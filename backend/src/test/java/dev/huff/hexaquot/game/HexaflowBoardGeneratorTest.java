package dev.huff.hexaquot.game;

import jakarta.ws.rs.ServiceUnavailableException;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HexaflowBoardGeneratorTest {
    @Test void generatesBentNonCrossingPathsForShortAndLongWords() {
        List<List<Integer>> partitions = List.of(
            List.of(8, 8, 5, 6, 7, 8, 6),
            List.of(6, 6, 6, 6, 6, 6, 6, 6),
            List.of(7, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4),
            List.of(8, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4),
            List.of(7, 41), List.of(8, 40), List.of(24, 24), List.of(44, 4));
        var generator = new HexaflowBoardGenerator();
        var validator = new HexaflowPuzzleValidator();
        for (var lengths : partitions) {
            var themes = lengths.subList(1, lengths.size()).stream().map(length -> "A".repeat(length)).toList();
            var request = new HexaflowDtos.BoardGenerationRequest(themes, "B".repeat(lengths.get(0)));
            for (int seed = 0; seed < 30; seed++) {
                String context = "lengths=" + lengths + ", seed=" + seed;
                var board = assertDoesNotThrow(() -> generator.generate(request, new Random(context.hashCode())), context);
                var errors = validator.validate(new HexaflowDtos.PuzzleDraftDto("2026-09-06", "Tema", board.grid(), board.answers()));
                assertEquals(List.of(), errors, context);
                for (var answer : board.answers()) assertArticulated(answer.path(), context);
            }
        }
    }

    @Test void doesNotFallBackToAStraightFlowWhenArticulatedPathsCannotFit() {
        // A six-cell Flusso must advance a column at every step. Bending it splits the
        // remaining cells into regions that a single, non-crossing theme cannot cover.
        var request = new HexaflowDtos.BoardGenerationRequest(List.of("A".repeat(42)), "FLUSSO");
        assertThrows(ServiceUnavailableException.class,
            () -> new HexaflowBoardGenerator().generate(request, new Random(0)));
    }

    @Test void regenerationProducesDifferentLayouts() {
        var generator = new HexaflowBoardGenerator();
        var request = new HexaflowDtos.BoardGenerationRequest(
            List.of("Giardino", "Fiori", "Alberi", "Fontana", "Farfalle", "Piante"), "Corrente");
        Set<List<List<Integer>>> layouts = new HashSet<>();
        for (int seed = 0; seed < 10; seed++) {
            var board = generator.generate(request, new Random(seed));
            layouts.add(board.answers().stream().map(HexaflowDtos.AnswerDto::path).toList());
        }
        assertEquals(10, layouts.size());
    }

    private void assertArticulated(List<Integer> path, String context) {
        List<Integer> directions = new ArrayList<>();
        boolean diagonal = false;
        for (int index = 1; index < path.size(); index++) {
            int a = path.get(index - 1), b = path.get(index);
            directions.add(b - a);
            diagonal |= a / 6 != b / 6 && a % 6 != b % 6;
        }
        long turns = java.util.stream.IntStream.range(1, directions.size())
            .filter(index -> !directions.get(index).equals(directions.get(index - 1))).count();
        assertTrue(turns >= Math.max(2, (path.size() - 1) / 2), "Too few turns: " + path + "; " + context);
        assertTrue(diagonal, "No diagonal: " + path + "; " + context);
        for (int index = 0; index + 3 < directions.size(); index++) {
            assertTrue(new HashSet<>(directions.subList(index, index + 4)).size() > 1,
                "Four consecutive straight links: " + path + "; " + context);
        }
    }
}
