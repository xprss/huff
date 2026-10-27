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
    void dailyPuzzleIsDeterministicSudokuAndUniquelyIdentifiedByAllSixteenClues() {
        for (int day = 1; day <= 12; day++) {
            List<Integer> solution = provider.solutionFor("2026-08-%02d".formatted(day));
            assertEquals(solution, provider.solutionFor("2026-08-%02d".formatted(day)));
            assertEquals(1, matchingSudokuGrids(provider.cluesFor(solution)));
            for (int row = 0; row < 4; row++) assertEquals(4, new HashSet<>(solution.subList(row * 4, row * 4 + 4)).size());
            for (int row = 0; row < 4; row += 2) for (int column = 0; column < 4; column += 2) {
                Set<Integer> quadrant = new HashSet<>();
                for (int r = row; r < row + 2; r++) for (int c = column; c < column + 2; c++) quadrant.add(solution.get(r * 4 + c));
                assertEquals(4, quadrant.size());
            }
        }
    }

    @Test
    void rejectsIncompleteAndNonLatinProposals() {
        assertThrows(BadRequestException.class, () -> provider.validateSolution(List.of(1, 2)));
        assertThrows(BadRequestException.class, () -> provider.validateSolution(Arrays.asList(1,1,2,3, 2,3,4,1, 3,4,1,2, 4,2,3,1)));
        assertThrows(BadRequestException.class, () -> provider.validateSolution(List.of(1,2,3,4, 2,3,4,1, 3,4,1,2, 4,1,2,3)));
    }

    private int matchingSudokuGrids(VisibilityDto clues) { return count(new ArrayList<>(), clues); }
    private int count(List<Integer> cells, VisibilityDto clues) {
        if (cells.size() == 16) return provider.cluesFor(cells).equals(clues) ? 1 : 0;
        int row = cells.size() / 4, column = cells.size() % 4, total = 0;
        for (int value = 1; value <= 4; value++) {
            if (cells.subList(row * 4, cells.size()).contains(value) || inQuadrant(cells, row, column, value)) continue;
            boolean inColumn = false; for (int r = 0; r < row; r++) if (cells.get(r * 4 + column) == value) inColumn = true;
            if (!inColumn) { cells.add(value); total += count(cells, clues); cells.remove(cells.size() - 1); }
        }
        return total;
    }
    private boolean inQuadrant(List<Integer> cells, int row, int column, int value) { int firstRow = row / 2 * 2, firstColumn = column / 2 * 2; for (int r = firstRow; r <= row; r++) for (int c = firstColumn; c < firstColumn + 2; c++) { int index = r * 4 + c; if (index < cells.size() && cells.get(index) == value) return true; } return false; }
}
