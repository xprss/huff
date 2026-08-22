package dev.huff.hexaquot.game;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.game.HexasquareDtos.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@ApplicationScoped
public class HexasquareDailyGameService {
    @Inject DailyGameService dailyGameService;
    @Inject HexasquareDailyGameProvider provider;
    @Inject HexasquareSolver solver;
    @Inject HexasquareGameRepository repository;
    @Inject ObjectMapper objectMapper;

    public TodayDto today(AppUser user) {
        String date=dailyGameService.todayDate();
        HexasquareGameRecord record=repository.findByUserAndDate(user.id(),date).orElse(null);
        SnapshotDto puzzle=record==null?provider.puzzleFor(date).snapshot():readSnapshot(record.puzzleJson());
        return todayDto(date,record==null?HexasquareDailyGameProvider.RULES_VERSION:record.rulesVersion(),puzzle,record==null?null:toDto(record));
    }

    @Transactional
    public SimulationActionDto simulate(AppUser user,SimulationRequestDto request) {
        String requestId=validateRequestId(request==null?null:request.requestId());
        String date=dailyGameService.todayDate();
        repository.lockUser(user.id());
        HexasquareGameRecord record=repository.findByUserAndDateForUpdate(user.id(),date).orElseGet(()-> {
            SnapshotDto generated=provider.puzzleFor(date).snapshot();
            return repository.create(user.id(),date,write(generated));
        });
        Optional<HexasquareSimulationRecord> replay=repository.findSimulation(record.id(),requestId);
        if(replay.isPresent()) return new SimulationActionDto(toDto(record),readResult(replay.get().outcomeJson()),true);
        if(record.status()==Status.COMPLETED) throw new WebApplicationException("La rete di oggi è già completata.",Response.Status.CONFLICT);

        SnapshotDto puzzle=readSnapshot(record.puzzleJson());
        List<PlacementDto> placements=solver.validatePlacements(puzzle,request==null?null:request.placements());
        SimulationResultDto result=solver.solve(puzzle,requestId,placements);
        String now=Instant.now().toString();
        repository.createSimulation(record.id(),requestId,write(placements),write(result),result.success(),now);
        HexasquareGameRecord updated=new HexasquareGameRecord(record.id(),record.userId(),record.puzzleDate(),record.rulesVersion(),
            record.puzzleJson(),write(placements),result.success()?write(result.paths()):null,
            result.success()?Status.COMPLETED:Status.IN_PROGRESS,record.simulationsCount()+1,
            result.success()?result.usedCells():null,result.success()?result.remainingCells():null,
            record.createdAt(),now,result.success()?now:null);
        updated=repository.update(updated);
        return new SimulationActionDto(toDto(updated),result,false);
    }

    public HexasquareDtos.StatsDto stats(AppUser user) { return statsForUserId(user.id()); }
    public HexasquareDtos.StatsDto statsForUserId(String userId) {
        List<HexasquareGameRecord> all=repository.findByUser(userId);
        List<HexasquareGameRecord> completed=all.stream().filter(record->record.status()==Status.COMPLETED).toList();
        int running=0,max=0; LocalDate previous=null;
        for(HexasquareGameRecord record:completed) {
            LocalDate date=LocalDate.parse(record.puzzleDate());
            running=previous!=null&&date.equals(previous.plusDays(1))?running+1:1; max=Math.max(max,running); previous=date;
        }
        double completion=all.isEmpty()?0:round(100d*completed.size()/all.size());
        double averageUsed=average(completed.stream().map(HexasquareGameRecord::usedCells).filter(Objects::nonNull).mapToInt(Integer::intValue).toArray());
        double averageSaved=average(completed.stream().map(HexasquareGameRecord::remainingCells).filter(Objects::nonNull).mapToInt(Integer::intValue).toArray());
        double averageSimulations=average(completed.stream().mapToInt(HexasquareGameRecord::simulationsCount).toArray());
        int bestSaved=completed.stream().map(HexasquareGameRecord::remainingCells).filter(Objects::nonNull).mapToInt(Integer::intValue).max().orElse(0);
        return new HexasquareDtos.StatsDto(all.size(),completed.size(),completion,running,max,averageUsed,averageSaved,bestSaved,averageSimulations);
    }

    public List<StatsCalculator.CompletedGame> completedForUser(String userId) {
        return repository.findCompletedByUser(userId).stream()
            .map(record->new StatsCalculator.CompletedGame(record.puzzleDate(),GameStatus.WON,record.simulationsCount())).toList();
    }

    private TodayDto todayDto(String date,int rulesVersion,SnapshotDto puzzle,HexasquareDtos.GameDto game) {
        return new TodayDto(date,rulesVersion,puzzle.size(),puzzle.quadrants(),puzzle.obstacles(),
            puzzle.terminals(),puzzle.characters(),puzzle.incompatiblePairs(),puzzle.inventory(),game);
    }
    private HexasquareDtos.GameDto toDto(HexasquareGameRecord record) {
        List<PlacementDto> placements=record.placementsJson()==null?List.of():readPlacements(record.placementsJson());
        List<PathDto> paths=record.canonicalPathsJson()==null?List.of():readPaths(record.canonicalPathsJson());
        return new HexasquareDtos.GameDto(record.puzzleDate(),record.rulesVersion(),record.status(),placements,record.simulationsCount(),
            record.usedCells(),record.remainingCells(),paths,record.completedAt());
    }
    private String validateRequestId(String requestId) {
        if(requestId==null||requestId.isBlank()||requestId.length()>128) throw new BadRequestException("requestId non valido.");
        return requestId.trim();
    }
    private double average(int[] values) { return values.length==0?0:round(Arrays.stream(values).average().orElse(0)); }
    private double round(double value) { return Math.round(value*10d)/10d; }
    private SnapshotDto readSnapshot(String json) { return read(json,SnapshotDto.class,"puzzle"); }
    private SimulationResultDto readResult(String json) { return read(json,SimulationResultDto.class,"simulation result"); }
    private List<PlacementDto> readPlacements(String json) { try{return List.copyOf(objectMapper.readValue(json,new TypeReference<List<PlacementDto>>(){}));}catch(Exception e){throw new IllegalStateException("Cannot parse Hexasquare placements",e);} }
    private List<PathDto> readPaths(String json) { try{return List.copyOf(objectMapper.readValue(json,new TypeReference<List<PathDto>>(){}));}catch(Exception e){throw new IllegalStateException("Cannot parse Hexasquare paths",e);} }
    private <T>T read(String json,Class<T> type,String label) { try{return objectMapper.readValue(json,type);}catch(Exception e){throw new IllegalStateException("Cannot parse Hexasquare "+label,e);} }
    private String write(Object value) { try{return objectMapper.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException("Cannot serialize Hexasquare data",e);} }
}
