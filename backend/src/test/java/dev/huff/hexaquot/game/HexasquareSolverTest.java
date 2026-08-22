package dev.huff.hexaquot.game;

import dev.huff.hexaquot.game.HexasquareDtos.*;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class HexasquareSolverTest {
    private final HexasquareSolver solver=new HexasquareSolver();

    @Test void supportsCurvesExactAndQuadrantGoalsAndCompatibleSharing() {
        SnapshotDto puzzle=puzzle(false,List.of());
        List<PlacementDto> roads=List.of(
            new PlacementDto(1,1,RoadType.STRAIGHT,90),
            new PlacementDto(1,2,RoadType.CURVE,180),
            new PlacementDto(2,2,RoadType.CURVE,0),
            new PlacementDto(2,3,RoadType.STRAIGHT,90),
            new PlacementDto(2,4,RoadType.STRAIGHT,90),
            new PlacementDto(2,5,RoadType.STRAIGHT,90),
            new PlacementDto(2,6,RoadType.STRAIGHT,90),
            new PlacementDto(2,7,RoadType.STRAIGHT,90),
            new PlacementDto(2,8,RoadType.STRAIGHT,90),
            new PlacementDto(2,9,RoadType.STRAIGHT,90),
            new PlacementDto(2,10,RoadType.STRAIGHT,90),
            new PlacementDto(2,11,RoadType.STRAIGHT,90),
            new PlacementDto(2,12,RoadType.STRAIGHT,90),
            new PlacementDto(2,13,RoadType.STRAIGHT,90),
            new PlacementDto(2,14,RoadType.STRAIGHT,90),
            new PlacementDto(2,15,RoadType.STRAIGHT,90),
            new PlacementDto(2,16,RoadType.STRAIGHT,90),
            new PlacementDto(2,17,RoadType.STRAIGHT,90),
            new PlacementDto(2,18,RoadType.STRAIGHT,90),
            new PlacementDto(2,19,RoadType.STRAIGHT,90),
            new PlacementDto(2,20,RoadType.STRAIGHT,90),
            new PlacementDto(2,21,RoadType.STRAIGHT,90),
            new PlacementDto(2,22,RoadType.STRAIGHT,90)
        );
        assertTrue(solver.solve(puzzle,"ok",solver.validatePlacements(puzzle,roads)).success());
    }

    @Test void distinguishesForbiddenQuadrantAndValidatesInventoryAndDuplicates() {
        SnapshotDto puzzle=puzzle(true,List.of());
        List<PlacementDto> roads=new ArrayList<>(); for(int column=1;column<=22;column++) roads.add(new PlacementDto(1,column,RoadType.STRAIGHT,90));
        SimulationResultDto result=solver.solve(puzzle,"forbidden",solver.validatePlacements(puzzle,roads));
        assertFalse(result.success());
        assertTrue(result.characters().stream().anyMatch(outcome->outcome.result()==CharacterResult.FORBIDDEN_QUADRANT));
        assertThrows(BadRequestException.class,()->solver.validatePlacements(puzzle,List.of(roads.get(0),roads.get(0))));
    }

    private SnapshotDto puzzle(boolean forbidSolaria,List<IncompatiblePairDto> pairs) {
        CharacterDto exact=new CharacterDto("a","A","🚲",new CoordinateDto(1,0),
            new GoalDto(GoalType.EXACT_CELL,new CoordinateDto(2,23),null),List.of());
        CharacterDto zone=new CharacterDto("b","B","🚌",new CoordinateDto(1,0),
            new GoalDto(GoalType.QUADRANT,null,Quadrant.SOLARIA),forbidSolaria?List.of(Quadrant.SOLARIA):List.of());
        EnumMap<RoadType,Integer> inventory=new EnumMap<>(RoadType.class); for(RoadType type:RoadType.values())inventory.put(type,100);
        return new SnapshotDto(24,List.of(),List.of(),List.of(
            new TerminalDto("as","a",TerminalKind.START,new CoordinateDto(1,0)),
            new TerminalDto("ad","a",TerminalKind.DESTINATION,new CoordinateDto(2,23)),
            new TerminalDto("bs","b",TerminalKind.START,new CoordinateDto(1,0))),List.of(exact,zone),pairs,inventory);
    }
}
