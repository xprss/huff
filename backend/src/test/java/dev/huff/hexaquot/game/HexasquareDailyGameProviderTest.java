package dev.huff.hexaquot.game;

import dev.huff.hexaquot.game.HexasquareDtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class HexasquareDailyGameProviderTest {
    private final HexasquareDailyGameProvider provider = new HexasquareDailyGameProvider();
    private final HexasquareSolver solver = new HexasquareSolver();

    @BeforeEach void seed() { provider.seed = "hexasquare-test-seed"; }

    @Test
    void generates365DeterministicSolvableDailyCities() {
        LocalDate date=LocalDate.of(2026,1,1);
        for(int day=0;day<365;day++) {
            String puzzleDate=date.plusDays(day).toString();
            var generated=provider.puzzleFor(puzzleDate);
            assertEquals(generated,provider.puzzleFor(puzzleDate));
            SnapshotDto puzzle=generated.snapshot();
            assertEquals(24,puzzle.size()); assertEquals(4,puzzle.quadrants().size());
            assertTrue(puzzle.characters().size()>=3&&puzzle.characters().size()<=6);
            assertTrue(puzzle.characters().stream().anyMatch(character->character.goal().type()==GoalType.EXACT_CELL));
            assertTrue(puzzle.characters().stream().anyMatch(character->character.goal().type()==GoalType.QUADRANT));
            assertTrue(puzzle.characters().stream().anyMatch(character->!character.forbiddenQuadrants().isEmpty()));
            assertFalse(puzzle.incompatiblePairs().isEmpty());
            assertTrue(puzzle.obstacles().size()>=87&&puzzle.obstacles().size()<=126);
            Map<RoadType,Long> used=new EnumMap<>(RoadType.class);
            generated.witness().forEach(tile->used.merge(tile.type(),1L,Long::sum));
            for(RoadType type:RoadType.values()) assertTrue(used.getOrDefault(type,0L)<=puzzle.inventory().getOrDefault(type,0));
            List<PlacementDto> validated=solver.validatePlacements(puzzle,generated.witness());
            assertTrue(solver.solve(puzzle,"witness",validated).success(),"Witness failed on "+puzzleDate);
        }
    }
}
