package dev.huff.hexaquot.game;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.persistence.HexaflowPuzzleEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.time.*;
import java.util.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class HexaflowPuzzleService {
    @Inject ObjectMapper objectMapper;
    @Inject HexaflowPuzzleValidator validator;
    @Inject HexaflowBoardGenerator boardGenerator;
    @ConfigProperty(name="app.game.timezone") String timezone;

    public HexaflowDtos.PuzzleMonthDto month(AppUser admin, String month) {
        require(admin); String normalized=validateMonth(month);
        List<HexaflowDtos.PuzzleSummaryDto> puzzles=HexaflowPuzzleEntity.<HexaflowPuzzleEntity>list(
            "puzzleDate like ?1 order by puzzleDate",normalized+"-%").stream().map(this::summary).toList();
        return new HexaflowDtos.PuzzleMonthDto(normalized,puzzles);
    }
    public HexaflowDtos.PuzzleAdminDto detail(AppUser admin,String date){require(admin);return toAdmin(find(date));}
    public HexaflowDtos.GeneratedBoardDto generate(AppUser admin,String date,HexaflowDtos.BoardGenerationRequest request){require(admin);parseDate(date);return boardGenerator.generate(request);}

    @Transactional
    public HexaflowDtos.PuzzleAdminDto save(AppUser admin,String date,HexaflowDtos.PuzzleDraftDto request){
        require(admin); parseDate(date); if(request==null)throw new BadRequestException("Bozza mancante.");
        if(request.puzzleDate()!=null&&!request.puzzleDate().equals(date))throw new BadRequestException("La data del corpo non coincide con l'URL.");
        HexaflowPuzzleEntity entity=HexaflowPuzzleEntity.<HexaflowPuzzleEntity>find("puzzleDate",date).firstResult();
        if(entity!=null&&entity.status==HexaflowDtos.PuzzleStatus.PUBLISHED) immutable();
        String now=Instant.now().toString();
        if(entity==null){entity=new HexaflowPuzzleEntity();entity.id=UUID.randomUUID().toString();entity.puzzleDate=date;entity.status=HexaflowDtos.PuzzleStatus.DRAFT;entity.createdBy=admin.id();entity.createdAt=now;}
        entity.themeClue=request.themeClue()==null?"":request.themeClue().trim();
        entity.gridJson=write(request.grid()==null?List.of():request.grid().stream().map(v->v==null?"":v.trim().toUpperCase(Locale.ROOT)).toList());
        entity.answersJson=write(request.answers()==null?List.of():request.answers());
        entity.updatedBy=admin.id();entity.updatedAt=now;
        if(!entity.isPersistent())entity.persist();
        return toAdmin(entity);
    }

    @Transactional
    public HexaflowDtos.PuzzleAdminDto publish(AppUser admin,String date){
        require(admin);HexaflowPuzzleEntity entity=find(date);
        if(entity.status==HexaflowDtos.PuzzleStatus.PUBLISHED)return toAdmin(entity);
        List<HexaflowDtos.ValidationErrorDto> errors=validator.validate(toDraft(entity));
        if(!errors.isEmpty())throw new WebApplicationException(Response.status(422).entity(new ValidationResponse("invalid_puzzle","Il puzzle non è pubblicabile.",errors)).build());
        String now=Instant.now().toString();entity.status=HexaflowDtos.PuzzleStatus.PUBLISHED;entity.publishedBy=admin.id();entity.publishedAt=now;entity.updatedBy=admin.id();entity.updatedAt=now;return toAdmin(entity);
    }

    @Transactional
    public HexaflowDtos.PuzzleAdminDto unpublish(AppUser admin,String date){
        require(admin);HexaflowPuzzleEntity entity=find(date);if(!parseDate(date).isAfter(today()))immutable();
        entity.status=HexaflowDtos.PuzzleStatus.DRAFT;entity.publishedBy=null;entity.publishedAt=null;entity.updatedBy=admin.id();entity.updatedAt=Instant.now().toString();return toAdmin(entity);
    }

    @Transactional
    public void delete(AppUser admin,String date){require(admin);HexaflowPuzzleEntity entity=find(date);if(entity.status!=HexaflowDtos.PuzzleStatus.DRAFT||!parseDate(date).isAfter(today()))immutable();entity.delete();}

    public HexaflowDtos.PuzzleDraftDto toDraft(HexaflowPuzzleEntity e){return new HexaflowDtos.PuzzleDraftDto(e.puzzleDate,e.themeClue,readGrid(e.gridJson),readAnswers(e.answersJson));}
    public List<String> readGrid(String json){return read(json,new TypeReference<List<String>>(){});}
    public List<HexaflowDtos.AnswerDto> readAnswers(String json){return read(json,new TypeReference<List<HexaflowDtos.AnswerDto>>(){});}
    public String write(Object value){try{return objectMapper.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException("Cannot serialize Hexaflow data",e);}}
    private <T>T read(String json,TypeReference<T> type){try{return objectMapper.readValue(json,type);}catch(Exception e){throw new IllegalStateException("Cannot parse Hexaflow data",e);}}
    private HexaflowDtos.PuzzleAdminDto toAdmin(HexaflowPuzzleEntity e){var draft=toDraft(e);return new HexaflowDtos.PuzzleAdminDto(e.id,e.puzzleDate,e.status,e.themeClue,draft.grid(),draft.answers(),validator.validate(draft),e.createdBy,e.updatedBy,e.publishedBy,e.createdAt,e.updatedAt,e.publishedAt,!parseDate(e.puzzleDate).isAfter(today())&&e.status==HexaflowDtos.PuzzleStatus.PUBLISHED);}
    private HexaflowDtos.PuzzleSummaryDto summary(HexaflowPuzzleEntity e){var d=toDraft(e);var errors=validator.validate(d);int covered=(int)d.answers().stream().flatMap(a->a.path()==null?java.util.stream.Stream.empty():a.path().stream()).distinct().count();return new HexaflowDtos.PuzzleSummaryDto(e.puzzleDate,e.status,e.themeClue,d.answers().size(),covered,errors.isEmpty(),e.status==HexaflowDtos.PuzzleStatus.PUBLISHED&&!parseDate(e.puzzleDate).isAfter(today()));}
    private HexaflowPuzzleEntity find(String date){parseDate(date);HexaflowPuzzleEntity e=HexaflowPuzzleEntity.<HexaflowPuzzleEntity>find("puzzleDate",date).firstResult();if(e==null)throw new NotFoundException("Puzzle Hexaflow non trovato.");return e;}
    private void require(AppUser a){if(a==null||a.admin()==null||!a.admin().canManageHexaflowPuzzles())throw new ForbiddenException("Privilegio CMS Hexaflow richiesto.");}
    private String validateMonth(String m){if(m==null||!m.matches("[0-9]{4}-[0-9]{2}"))throw new BadRequestException("Mese non valido.");try{YearMonth.parse(m);return m;}catch(Exception e){throw new BadRequestException("Mese non valido.");}}
    private LocalDate parseDate(String d){try{return LocalDate.parse(d);}catch(Exception e){throw new BadRequestException("Data non valida.");}}
    private LocalDate today(){return LocalDate.now(ZoneId.of(timezone));}
    private void immutable(){throw new WebApplicationException("I puzzle pubblicati per oggi o nel passato sono immutabili.",Response.Status.CONFLICT);}
    public record ValidationResponse(String code,String message,List<HexaflowDtos.ValidationErrorDto> errors){}
}
