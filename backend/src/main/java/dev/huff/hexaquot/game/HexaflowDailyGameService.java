package dev.huff.hexaquot.game;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.persistence.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class HexaflowDailyGameService {
    @Inject DailyGameService dailyGameService;
    @Inject HexaflowPuzzleService puzzleService;
    @Inject ObjectMapper objectMapper;
    @Inject EntityManager entityManager;

    public HexaflowDtos.TodayDto today(AppUser user){
        String date=dailyGameService.todayDate();HexaflowPuzzleEntity puzzle=published(date);
        if(puzzle==null)return new HexaflowDtos.TodayDto(date,false,null,List.of(),0,null);
        var answers=puzzleService.readAnswers(puzzle.answersJson);HexaflowGameEntity game=findGame(user.id(),date,false);
        return new HexaflowDtos.TodayDto(date,true,puzzle.themeClue,puzzleService.readGrid(puzzle.gridJson),answers.size(),game==null?null:toDto(game,answers));
    }

    @Transactional
    public HexaflowDtos.PathActionDto submitPath(AppUser user,HexaflowDtos.PathRequest request){
        String requestId=requestId(request==null?null:request.requestId());List<Integer> cells=validatePath(request==null?null:request.cells());
        HexaflowPuzzleEntity puzzle=requireTodayPuzzle();lockUser(user.id());HexaflowGameEntity game=current(user,puzzle);
        List<HexaflowDtos.EventDto> events=events(game);HexaflowDtos.EventDto replay=events.stream().filter(e->requestId.equals(e.requestId())).findFirst().orElse(null);
        var answers=puzzleService.readAnswers(puzzle.answersJson);if(replay!=null){if(replay.path()==null)throw duplicateRequest();return new HexaflowDtos.PathActionDto(toDto(game,answers),replay.path(),true);}
        if(game.status==HexaflowDtos.GameStatus.COMPLETED)throw new WebApplicationException("La partita di oggi è già conclusa.",Response.Status.CONFLICT);
        List<String> found=readStrings(game.foundAnswersJson),extras=readStrings(game.extraSequencesJson);List<String> grid=puzzleService.readGrid(puzzle.gridJson);
        HexaflowDtos.AnswerDto matched=answers.stream().filter(a->samePath(a.path(),cells)).findFirst().orElse(null);
        HexaflowDtos.PathOutcome outcome;HexaflowDtos.FoundAnswerDto foundDto=null;String sequence=word(grid,cells);
        if(matched!=null){
            if(found.contains(matched.id()))outcome=HexaflowDtos.PathOutcome.DUPLICATE;
            else{found.add(matched.id());outcome=matched.type()==HexaflowDtos.AnswerType.FLOW?HexaflowDtos.PathOutcome.FLOW:HexaflowDtos.PathOutcome.THEME;foundDto=new HexaflowDtos.FoundAnswerDto(matched.id(),matched.label(),matched.type(),List.copyOf(matched.path()));}
        }else{String normalized=HexaflowPuzzleValidator.normalize(sequence);if(extras.contains(normalized))outcome=HexaflowDtos.PathOutcome.DUPLICATE;else{extras.add(normalized);outcome=HexaflowDtos.PathOutcome.EXTRA;}}
        String now=Instant.now().toString();
        var result=new HexaflowDtos.PathResultDto(requestId,outcome,foundDto,sequence,extras.size());events.add(new HexaflowDtos.EventDto(events.size()+1,HexaflowDtos.EventKind.PATH,requestId,now,result));
        game.foundAnswersJson=write(found);game.extraSequencesJson=write(extras);game.eventLogJson=write(events);game.updatedAt=now;
        if(found.size()==answers.size()){game.status=HexaflowDtos.GameStatus.COMPLETED;game.completedAt=now;}
        return new HexaflowDtos.PathActionDto(toDto(game,answers),result,false);
    }

    public HexaflowDtos.StatsDto stats(AppUser user){return statsForUserId(user.id());}
    public HexaflowDtos.StatsDto statsForUserId(String userId){
        List<HexaflowGameEntity> all=HexaflowGameEntity.<HexaflowGameEntity>list("userId = ?1 order by puzzleDate",userId);List<HexaflowGameEntity> complete=all.stream().filter(g->g.status==HexaflowDtos.GameStatus.COMPLETED).toList();
        String today=dailyGameService.todayDate();List<String> published=HexaflowPuzzleEntity.<HexaflowPuzzleEntity>list("status = ?1 and puzzleDate <= ?2 order by puzzleDate",HexaflowDtos.PuzzleStatus.PUBLISHED,today).stream().map(p->p.puzzleDate).toList();
        Set<String> won=complete.stream().map(g->g.puzzleDate).collect(java.util.stream.Collectors.toSet());int running=0,max=0;for(String date:published){if(won.contains(date))running++;else running=0;max=Math.max(max,running);}
        return new HexaflowDtos.StatsDto(all.size(),complete.size(),running,max);
    }
    public List<StatsCalculator.CompletedGame> completedForUser(String userId){return HexaflowGameEntity.<HexaflowGameEntity>list("userId = ?1 and status = ?2 order by puzzleDate",userId,HexaflowDtos.GameStatus.COMPLETED).stream().map(g->new StatsCalculator.CompletedGame(g.puzzleDate,GameStatus.WON,1)).toList();}

    private HexaflowDtos.GameDto toDto(HexaflowGameEntity g,List<HexaflowDtos.AnswerDto> answers){List<String> ids=readStrings(g.foundAnswersJson);Map<String,HexaflowDtos.AnswerDto> byId=new LinkedHashMap<>();answers.forEach(a->byId.put(a.id(),a));List<HexaflowDtos.FoundAnswerDto> found=ids.stream().map(byId::get).filter(Objects::nonNull).map(a->new HexaflowDtos.FoundAnswerDto(a.id(),a.label(),a.type(),a.path())).toList();int extras=readStrings(g.extraSequencesJson).size();return new HexaflowDtos.GameDto(g.puzzleDate,g.status,found,extras,g.completedAt);}
    private HexaflowGameEntity current(AppUser u,HexaflowPuzzleEntity p){HexaflowGameEntity g=findGame(u.id(),p.puzzleDate,true);if(g!=null)return g;String now=Instant.now().toString();g=new HexaflowGameEntity();g.id=UUID.randomUUID().toString();g.userId=u.id();g.puzzleId=p.id;g.puzzleDate=p.puzzleDate;g.foundAnswersJson="[]";g.extraSequencesJson="[]";g.eventLogJson="[]";g.status=HexaflowDtos.GameStatus.IN_PROGRESS;g.createdAt=now;g.updatedAt=now;g.persist();return g;}
    private HexaflowGameEntity findGame(String uid,String d,boolean lock){var q=HexaflowGameEntity.<HexaflowGameEntity>find("userId = ?1 and puzzleDate = ?2",uid,d);if(lock)q=q.withLock(LockModeType.PESSIMISTIC_WRITE);return q.firstResult();}
    private HexaflowPuzzleEntity published(String d){return HexaflowPuzzleEntity.<HexaflowPuzzleEntity>find("puzzleDate = ?1 and status = ?2",d,HexaflowDtos.PuzzleStatus.PUBLISHED).firstResult();}
    private HexaflowPuzzleEntity requireTodayPuzzle(){HexaflowPuzzleEntity p=published(dailyGameService.todayDate());if(p==null)throw new NotFoundException("Hexaflow non disponibile oggi.");return p;}
    private void lockUser(String id){entityManager.createNativeQuery("SELECT id FROM users WHERE id = ?1 FOR UPDATE").setParameter(1,id).getResultList();}
    private List<Integer> validatePath(List<Integer> p){if(p==null||p.size()<4||p.size()>48)throw new BadRequestException("Il percorso deve contenere da 4 a 48 celle.");Set<Integer>s=new HashSet<>();for(int i=0;i<p.size();i++){Integer c=p.get(i);if(c==null||c<0||c>=48)throw new BadRequestException("Indice cella non valido.");if(!s.add(c))throw new BadRequestException("Una cella non può essere riutilizzata.");if(i>0&&!HexaflowPuzzleValidator.adjacent(p.get(i-1),c))throw new BadRequestException("Il percorso contiene un salto.");}return List.copyOf(p);}
    private String requestId(String id){if(id==null||!id.matches("[A-Za-z0-9._:-]{1,100}"))throw new BadRequestException("requestId non valido.");return id;}
    private boolean samePath(List<Integer>a,List<Integer>b){if(a==null||a.size()!=b.size())return false;if(a.equals(b))return true;for(int i=0;i<a.size();i++)if(!a.get(i).equals(b.get(b.size()-1-i)))return false;return true;}
    private String word(List<String>grid,List<Integer>p){StringBuilder b=new StringBuilder();p.forEach(i->b.append(grid.get(i)));return b.toString();}
    private List<String> readStrings(String j){return read(j,new TypeReference<List<String>>(){});}
    private List<HexaflowDtos.EventDto> events(HexaflowGameEntity g){return new ArrayList<>(read(g.eventLogJson,new TypeReference<List<HexaflowDtos.EventDto>>(){}));}
    private <T>T read(String j,TypeReference<T>t){try{return objectMapper.readValue(j,t);}catch(Exception e){throw new IllegalStateException("Cannot parse Hexaflow game",e);}}
    private String write(Object o){try{return objectMapper.writeValueAsString(o);}catch(Exception e){throw new IllegalStateException("Cannot serialize Hexaflow game",e);}}
    private WebApplicationException duplicateRequest(){return new WebApplicationException("requestId già usato per un'altra azione.",Response.Status.CONFLICT);}
}
