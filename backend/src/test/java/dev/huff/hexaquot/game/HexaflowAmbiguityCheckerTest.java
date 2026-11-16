package dev.huff.hexaquot.game;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HexaflowAmbiguityCheckerTest {
    private final HexaflowAmbiguityChecker checker = new HexaflowAmbiguityChecker();

    @Test void reportsWordBuildableFromASecondNeighbouringLetter() {
        List<String> grid = grid(Map.of(0, "O", 1, "L", 2, "I", 3, "V", 4, "A", 6, "O"));
        var report = checker.check(draft(grid, answer("oliva", HexaflowDtos.AnswerType.THEME, List.of(0, 1, 2, 3, 4))));
        assertEquals(1, report.checkedAnswers());
        assertEquals(1, report.ambiguousAnswers().size());
        var ambiguous = report.ambiguousAnswers().get(0);
        assertEquals("oliva", ambiguous.answerId());
        assertEquals(0, ambiguous.answerIndex());
        assertEquals("OLIVA", ambiguous.word());
        assertEquals(1, ambiguous.alternativeCount());
        assertFalse(ambiguous.truncated());
        assertEquals(List.of(List.of(6, 1, 2, 3, 4)), ambiguous.alternativePaths());
    }

    @Test void reportsNothingWhenTheWordHasASinglePath() {
        List<String> grid = grid(Map.of(0, "O", 1, "L", 2, "I", 3, "V", 4, "A", 12, "O"));
        var report = checker.check(draft(grid, answer("oliva", HexaflowDtos.AnswerType.THEME, List.of(0, 1, 2, 3, 4))));
        assertEquals(1, report.checkedAnswers());
        assertTrue(report.ambiguousAnswers().isEmpty());
    }

    @Test void ignoresTheReversedIntendedPathOfAPalindrome() {
        List<String> grid = grid(Map.of(0, "A", 1, "N", 2, "N", 3, "A"));
        var report = checker.check(draft(grid, answer("anna", HexaflowDtos.AnswerType.THEME, List.of(0, 1, 2, 3))));
        assertTrue(report.ambiguousAnswers().isEmpty());
    }

    @Test void findsAlternativesCrossingOtherAnswersCells() {
        // OLIVA on row 0, a second LIVA on row 1 under it: O(0) can continue to L(7).
        List<String> grid = grid(Map.of(0, "O", 1, "L", 2, "I", 3, "V", 4, "A", 7, "L", 8, "I", 9, "V", 10, "A"));
        var report = checker.check(draft(grid,
                answer("oliva", HexaflowDtos.AnswerType.THEME, List.of(0, 1, 2, 3, 4)),
                answer("flow", HexaflowDtos.AnswerType.FLOW, List.of(7, 8, 9, 10))));
        assertEquals(2, report.checkedAnswers());
        var oliva = report.ambiguousAnswers().stream().filter(a -> a.answerId().equals("oliva")).findFirst().orElseThrow();
        // Each of L, I, V, A has two adjacent choices: 2^4 paths, minus the intended one.
        assertEquals(15, oliva.alternativeCount());
        assertEquals(HexaflowAmbiguityChecker.MAX_RETURNED_PATHS, oliva.alternativePaths().size());
        assertTrue(report.ambiguousAnswers().stream().anyMatch(a -> a.answerId().equals("flow")));
    }

    @Test void capsDegenerateGrids() {
        List<String> grid = Collections.nCopies(48, "A");
        var report = checker.check(draft(grid, answer("a", HexaflowDtos.AnswerType.THEME, List.of(0, 1, 2, 3, 4, 5))));
        var ambiguous = report.ambiguousAnswers().get(0);
        assertTrue(ambiguous.truncated());
        assertEquals(HexaflowAmbiguityChecker.MAX_ALTERNATIVES, ambiguous.alternativeCount());
        assertEquals(HexaflowAmbiguityChecker.MAX_RETURNED_PATHS, ambiguous.alternativePaths().size());
    }

    @Test void skipsIncompleteBoards() {
        var report = checker.check(draft(List.of("A"), answer("a", HexaflowDtos.AnswerType.THEME, List.of(0))));
        assertEquals(0, report.checkedAnswers());
        assertTrue(report.ambiguousAnswers().isEmpty());
    }

    private static List<String> grid(Map<Integer, String> letters) {
        List<String> grid = new ArrayList<>(Collections.nCopies(48, "Z"));
        letters.forEach(grid::set);
        return grid;
    }

    private static HexaflowDtos.AnswerDto answer(String id, HexaflowDtos.AnswerType type, List<Integer> path) {
        return new HexaflowDtos.AnswerDto(id, "", type, path);
    }

    private static HexaflowDtos.PuzzleDraftDto draft(List<String> grid, HexaflowDtos.AnswerDto... answers) {
        return new HexaflowDtos.PuzzleDraftDto("2026-12-01", "Tema", grid, List.of(answers));
    }
}
