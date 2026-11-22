package dev.huff.hexaquot.game;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HexaflowAnswerMatchTest {
    @Test void acceptsTheAnswerTracedInItsReadingDirection() {
        List<String> grid = grid(Map.of(0, "E", 1, "N", 2, "O", 3, "T", 4, "E", 5, "C", 11, "A"));
        assertTrue(HexaflowDailyGameService.matchesAnswer(List.of(0, 1, 2, 3, 4, 5, 11), List.of(0, 1, 2, 3, 4, 5, 11), grid));
    }

    @Test void rejectsTheAnswerTracedBackwards() {
        // ENOTECA traced from the last cell spells ACETONE: a different word, not the answer.
        List<String> grid = grid(Map.of(0, "E", 1, "N", 2, "O", 3, "T", 4, "E", 5, "C", 11, "A"));
        assertFalse(HexaflowDailyGameService.matchesAnswer(List.of(0, 1, 2, 3, 4, 5, 11), List.of(11, 5, 4, 3, 2, 1, 0), grid));
    }

    @Test void acceptsAPalindromeTracedBackwards() {
        List<String> grid = grid(Map.of(0, "A", 1, "N", 2, "N", 3, "A"));
        assertTrue(HexaflowDailyGameService.matchesAnswer(List.of(0, 1, 2, 3), List.of(3, 2, 1, 0), grid));
    }

    @Test void rejectsADifferentPath() {
        List<String> grid = grid(Map.of(0, "A", 1, "N", 2, "N", 3, "A"));
        assertFalse(HexaflowDailyGameService.matchesAnswer(List.of(0, 1, 2, 3), List.of(0, 1, 2), grid));
        assertFalse(HexaflowDailyGameService.matchesAnswer(List.of(0, 1, 2, 3), List.of(0, 2, 1, 3), grid));
        assertFalse(HexaflowDailyGameService.matchesAnswer(null, List.of(0, 1, 2, 3), grid));
    }

    private static List<String> grid(Map<Integer, String> letters) {
        List<String> grid = new ArrayList<>();
        for (int i = 0; i < 48; i++) grid.add(letters.getOrDefault(i, "Z"));
        return grid;
    }
}
