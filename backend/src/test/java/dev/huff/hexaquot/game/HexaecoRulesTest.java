package dev.huff.hexaquot.game;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.game.HexaecoDtos.BoardDto;
import dev.huff.hexaquot.game.HexaecoDtos.Command;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import static dev.huff.hexaquot.game.HexaecoDtos.Command.*;
import static org.junit.jupiter.api.Assertions.*;

class HexaecoRulesTest {
    record Example(BoardDto board, List<Command> moves) {}

    @Test
    void echoWaitsTwoTurnsAndWaitLetsItCatchUp() {
        BoardDto board = new BoardDto(List.of(), 10, 20, 6, 16);
        var first = HexaecoRules.step(board, HexaecoRules.initial(board), RIGHT);
        assertEquals(new HexaecoRules.State(11, 20, WAIT, RIGHT), first);
        var second = HexaecoRules.step(board, first, UP);
        assertEquals(new HexaecoRules.State(6, 20, RIGHT, UP), second);
        var third = HexaecoRules.step(board, second, WAIT);
        assertEquals(new HexaecoRules.State(6, 21, UP, WAIT), third);
        assertFalse(HexaecoRules.won(board, third));
        assertTrue(HexaecoRules.solves(board, List.of(RIGHT, UP, WAIT, WAIT)));
    }

    @Test
    void wallsAndEdgesBlockOnlyTheAffectedLightAndConsumeTheTurn() {
        BoardDto board = new BoardDto(List.of(5), 10, 20, 11, 16);
        var first = HexaecoRules.step(board, HexaecoRules.initial(board), UP);
        assertEquals(10, first.light());
        assertEquals(UP, first.later());
        assertTrue(HexaecoRules.solves(board, List.of(UP, RIGHT, WAIT, WAIT)));
        assertEquals(10, HexaecoRules.step(board, HexaecoRules.initial(board), LEFT).light());
        assertEquals(4, HexaecoRules.step(board, new HexaecoRules.State(4, 24, DOWN, UP), RIGHT).light());
        assertEquals(24, HexaecoRules.step(board, new HexaecoRules.State(4, 24, DOWN, UP), RIGHT).echo());
    }

    @Test
    void lightsMayOverlapAndCrossAndGoalsDoNotLockThem() {
        BoardDto board = new BoardDto(List.of(), 10, 12, 11, 13);
        var together = HexaecoRules.step(board, new HexaecoRules.State(10, 12, LEFT, WAIT), RIGHT);
        assertEquals(11, together.light());
        assertEquals(11, together.echo());
        var crossing = HexaecoRules.step(board, new HexaecoRules.State(10, 11, LEFT, WAIT), RIGHT);
        assertEquals(11, crossing.light());
        assertEquals(10, crossing.echo());
        assertEquals(12, HexaecoRules.step(board, together, RIGHT).light());
    }

    @Test
    void simultaneousArrivalEndsPlayEvenWithQueuedCommands() {
        BoardDto board = new BoardDto(List.of(), 10, 20, 11, 16);
        assertTrue(HexaecoRules.solves(board, List.of(UP, RIGHT, DOWN, WAIT)));
        assertFalse(HexaecoRules.solves(board, List.of(UP, RIGHT, DOWN, WAIT, WAIT)));
        assertFalse(HexaecoRules.solves(board, List.of(UP, RIGHT)));
    }

    @Test
    void sharedClientServerExamplesMatchTheImmutableCatalogue() throws Exception {
        try (var stream = getClass().getResourceAsStream("/hexaeco-cases.json")) {
            List<Example> examples = new ObjectMapper().readValue(stream, new TypeReference<>() {});
            assertEquals(HexaecoDailyGameProvider.catalogueSize(), examples.size());
            for (int index = 0; index < examples.size(); index++) {
                Example example = examples.get(index);
                assertEquals(example.board(), HexaecoDailyGameProvider.catalogueBoard(index, 0));
                assertTrue(HexaecoRules.solves(example.board(), example.moves()), "Example " + index);
            }
        }
    }

    @Test
    void everyCatalogueSymmetryIsConnectedAndHasATenToFourteenMoveShortestSolution() {
        Set<BoardDto> unique = new HashSet<>();
        for (int index = 0; index < HexaecoDailyGameProvider.catalogueSize(); index++) {
            for (int symmetry = 0; symmetry < 8; symmetry++) {
                BoardDto board = HexaecoDailyGameProvider.catalogueBoard(index, symmetry);
                assertTrue(unique.add(board));
                Set<Integer> markers = Set.of(board.lightStart(), board.echoStart(), board.lightGoal(), board.echoGoal());
                assertEquals(4, markers.size());
                assertTrue(Collections.disjoint(markers, board.walls()));
                Set<Integer> reachable = new HashSet<>();
                ArrayDeque<Integer> cells = new ArrayDeque<>();
                cells.add(board.lightStart());
                while (!cells.isEmpty()) {
                    int cell = cells.remove();
                    if (!reachable.add(cell)) continue;
                    for (Command command : Command.values()) cells.add(HexaecoRules.step(board,
                        new HexaecoRules.State(cell, board.echoStart(), WAIT, WAIT), command).light());
                }
                assertEquals(25 - board.walls().size(), reachable.size());
                List<Command> solution = shortestSolution(board);
                assertTrue(solution.size() >= 10 && solution.size() <= 14, "Board " + index + ", symmetry " + symmetry);
                assertTrue(HexaecoRules.solves(board, solution));
            }
        }
        assertEquals(256, unique.size());
    }

    @Test
    void dailySelectionIsDeterministicAndDoesNotRepeatFor256Days() {
        var provider = new HexaecoDailyGameProvider();
        Set<BoardDto> boards = new HashSet<>();
        LocalDate start = LocalDate.of(2026, 9, 15);
        for (int day = 0; day < 256; day++) {
            String date = start.plusDays(day).toString();
            assertEquals(provider.boardFor(date), provider.boardFor(date));
            assertTrue(boards.add(provider.boardFor(date)));
        }
        assertEquals(provider.boardFor(start.toString()), provider.boardFor(start.plusDays(256).toString()));
        assertNotNull(provider.boardFor("1969-12-31"));
    }

    static List<Command> shortestSolution(BoardDto board) {
        record Node(HexaecoRules.State state, List<Command> moves) {}
        ArrayDeque<Node> queue = new ArrayDeque<>();
        Set<HexaecoRules.State> seen = new HashSet<>();
        var initial = HexaecoRules.initial(board);
        queue.add(new Node(initial, List.of()));
        seen.add(initial);
        while (!queue.isEmpty()) {
            Node node = queue.remove();
            if (HexaecoRules.won(board, node.state())) return node.moves();
            for (Command command : Command.values()) {
                var next = HexaecoRules.step(board, node.state(), command);
                if (!seen.add(next)) continue;
                List<Command> moves = new ArrayList<>(node.moves());
                moves.add(command);
                queue.add(new Node(next, List.copyOf(moves)));
            }
        }
        throw new AssertionError("Unsolvable Hexaeco board");
    }
}
