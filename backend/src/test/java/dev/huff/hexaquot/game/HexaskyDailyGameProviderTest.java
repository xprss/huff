package dev.huff.hexaquot.game;

import dev.huff.hexaquot.game.HexaskyDtos.VisibilityDto;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HexaskyDailyGameProviderTest {
    private final HexaskyDailyGameProvider provider = new HexaskyDailyGameProvider();
    @BeforeEach void seed() { provider.seed = "hexasky-test-seed"; }

    @Test
    void dailyPuzzleIsDeterministicLatinAndUniquelyIdentifiedByAllSixteenClues() {
        for (int day = 1; day <= 12; day++) {
            List<Integer> solution = provider.solutionFor("2026-08-%02d".formatted(day));
            assertEquals(solution, provider.solutionFor("2026-08-%02d".formatted(day)));
            assertEquals(1, matchingLatinSquares(provider.cluesFor(solution)));
            for (int row = 0; row < 4; row++) assertEquals(4, new HashSet<>(solution.subList(row * 4, row * 4 + 4)).size());
        }
    }

    @Test
    void rejectsIncompleteAndNonLatinProposals() {
        assertThrows(BadRequestException.class, () -> provider.validateSolution(List.of(1, 2)));
        assertThrows(BadRequestException.class, () -> provider.validateSolution(Arrays.asList(1,1,2,3, 2,3,4,1, 3,4,1,2, 4,2,3,1)));
    }

    private int matchingLatinSquares(VisibilityDto clues) { return count(new ArrayList<>(), clues); }
    private int count(List<Integer> cells, VisibilityDto clues) {
        if (cells.size() == 16) return provider.cluesFor(cells).equals(clues) ? 1 : 0;
        int row = cells.size() / 4, column = cells.size() % 4, total = 0;
        for (int value = 1; value <= 4; value++) {
            if (cells.subList(row * 4, cells.size()).contains(value)) continue;
            boolean inColumn = false; for (int r = 0; r < row; r++) if (cells.get(r * 4 + column) == value) inColumn = true;
            if (!inColumn) { cells.add(value); total += count(cells, clues); cells.remove(cells.size() - 1); }
        }
        return total;
    }
}
