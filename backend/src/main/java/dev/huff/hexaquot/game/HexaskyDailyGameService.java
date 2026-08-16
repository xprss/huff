package dev.huff.hexaquot.game;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.auth.AppUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.time.*;
import java.util.*;

@ApplicationScoped
public class HexaskyDailyGameService {
    @Inject DailyGameService dailyGameService;
    @Inject HexaskyDailyGameProvider provider;
    @Inject HexaskyGameRepository repository;
    @Inject ObjectMapper objectMapper;

    public HexaskyDtos.TodayDto today(AppUser user) { String date=dailyGameService.todayDate(); List<Integer> solution=provider.solutionFor(date); return new HexaskyDtos.TodayDto(date,HexaskyDailyGameProvider.RULES_VERSION,provider.cluesFor(solution),repository.findByUserAndDate(user.id(),date).map(this::toDto).orElse(null)); }
    @Transactional public HexaskyDtos.CheckActionDto check(AppUser user, HexaskyDtos.CheckRequest request) {
        String requestId=provider.validateRequestId(request==null?null:request.requestId());
        HexaskyGameRecord record=currentForUpdate(user); List<HexaskyDtos.EventDto> events=readEvents(record);
        HexaskyDtos.EventDto replay=events.stream().filter(event->requestId.equals(event.requestId())).findFirst().orElse(null);
        if(replay!=null) return new HexaskyDtos.CheckActionDto(toDto(record),replay.check(),true);
        if(record.status()!=HexaskyDtos.Status.IN_PROGRESS) throw new WebApplicationException("La partita di oggi è già conclusa.",Response.Status.CONFLICT);
        List<Integer> proposal=provider.validateSolution(request==null?null:request.solution()); List<Integer> solution=readGrid(record.solutionJson());
        boolean correct=proposal.equals(solution); int checks=record.checksUsed()+1; HexaskyDtos.Status status=correct?HexaskyDtos.Status.WON:checks>=2?HexaskyDtos.Status.LOST:HexaskyDtos.Status.IN_PROGRESS;
        String now=Instant.now().toString(); HexaskyDtos.CheckResultDto result=new HexaskyDtos.CheckResultDto(requestId,correct,checks,status,status==HexaskyDtos.Status.LOST?solution:null);
        events.add(new HexaskyDtos.EventDto(events.size()+1,HexaskyDtos.EventKind.CHECK,now,result));
        HexaskyGameRecord updated=new HexaskyGameRecord(record.id(),record.userId(),record.puzzleDate(),record.rulesVersion(),record.solutionJson(),writeGrid(proposal),writeEvents(events),checks,status,record.createdAt(),now,status==HexaskyDtos.Status.IN_PROGRESS?null:now);
        return new HexaskyDtos.CheckActionDto(toDto(repository.update(updated)),result,false);
    }
    public HexaskyDtos.StatsDto stats(AppUser user){return statsForUserId(user.id());}
    public HexaskyDtos.StatsDto statsForUserId(String userId) {
        List<HexaskyGameRecord> records=repository.findCompletedByUser(userId); int won=(int)records.stream().filter(r->r.status()==HexaskyDtos.Status.WON).count(); Map<Integer,Integer> dist=new LinkedHashMap<>();dist.put(1,0);dist.put(2,0);for(HexaskyGameRecord r:records)if(r.status()==HexaskyDtos.Status.WON)dist.merge(r.checksUsed(),1,Integer::sum);
        int running=0,max=0;LocalDate previous=null; for(HexaskyGameRecord r:records){LocalDate date=LocalDate.parse(r.puzzleDate());if(r.status()==HexaskyDtos.Status.WON)running=previous!=null&&date.equals(previous.plusDays(1))?running+1:1;else running=0;max=Math.max(max,running);previous=date;}
        return new HexaskyDtos.StatsDto(records.size(),won,records.size()-won,running,max,Map.copyOf(dist));
    }
    public List<StatsCalculator.CompletedGame> completedForUser(String userId) { return repository.findCompletedByUser(userId).stream().map(r->new StatsCalculator.CompletedGame(r.puzzleDate(),r.status()==HexaskyDtos.Status.WON?GameStatus.WON:GameStatus.LOST,r.checksUsed())).toList(); }
    private HexaskyGameRecord currentForUpdate(AppUser user){String date=dailyGameService.todayDate();repository.lockUser(user.id());return repository.findByUserAndDateForUpdate(user.id(),date).orElseGet(()->{List<Integer>s=provider.solutionFor(date);return repository.create(user.id(),date,s,writeGrid(s));});}
    private HexaskyDtos.GameDto toDto(HexaskyGameRecord r){return new HexaskyDtos.GameDto(r.puzzleDate(),r.rulesVersion(),r.status(),r.checksUsed(),r.proposalJson()==null?null:readGrid(r.proposalJson()),readEvents(r),r.status()==HexaskyDtos.Status.LOST?readGrid(r.solutionJson()):null,r.completedAt());}
    private List<HexaskyDtos.EventDto> readEvents(HexaskyGameRecord r){try{return new ArrayList<>(objectMapper.readValue(r.eventLogJson(),new TypeReference<List<HexaskyDtos.EventDto>>(){}));}catch(Exception e){throw new IllegalStateException("Cannot parse Hexasky events",e);}}
    private String writeEvents(List<HexaskyDtos.EventDto> e){try{return objectMapper.writeValueAsString(e);}catch(Exception x){throw new IllegalStateException("Cannot write Hexasky events",x);}}
    private List<Integer> readGrid(String json){try{return List.copyOf(objectMapper.readValue(json,new TypeReference<List<Integer>>(){}));}catch(Exception e){throw new IllegalStateException("Cannot parse Hexasky grid",e);}}
    private String writeGrid(List<Integer> grid){try{return objectMapper.writeValueAsString(grid);}catch(Exception e){throw new IllegalStateException("Cannot write Hexasky grid",e);}}
}
