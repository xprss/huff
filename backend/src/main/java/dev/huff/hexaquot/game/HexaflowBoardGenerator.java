package dev.huff.hexaquot.game;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@ApplicationScoped
public class HexaflowBoardGenerator {
    private static final int ROWS = 8;
    private static final int COLUMNS = 6;
    private static final int CELLS = ROWS * COLUMNS;

    public HexaflowDtos.GeneratedBoardDto generate(HexaflowDtos.BoardGenerationRequest request) {
        if (request == null) throw new BadRequestException("Inserisci le parole tema e il Flusso.");
        String flow = word(request.flowWord(), "Il Flusso");
        List<String> themes = new ArrayList<>();
        for (String value : request.themeWords() == null ? List.<String>of() : request.themeWords()) {
            if (value != null && !value.isBlank()) themes.add(word(value, "Ogni parola tema"));
        }
        if (themes.isEmpty()) throw new BadRequestException("Inserisci almeno una parola tema.");
        if (flow.length() < COLUMNS) throw new BadRequestException("Il Flusso deve avere almeno 6 lettere per collegare due lati opposti.");
        int total = flow.length() + themes.stream().mapToInt(String::length).sum();
        if (total != CELLS) throw new BadRequestException("Le parole devono usare esattamente 48 lettere: ora ne usano " + total + ".");

        Collections.shuffle(themes);
        List<Integer> route = intricateRoute(flow.length());
        List<String> grid = new ArrayList<>(Collections.nCopies(CELLS, ""));
        List<HexaflowDtos.AnswerDto> answers = new ArrayList<>();
        int offset = addAnswer(answers, grid, route, 0, flow, HexaflowDtos.AnswerType.FLOW);
        for (String theme : themes) offset = addAnswer(answers, grid, route, offset, theme, HexaflowDtos.AnswerType.THEME);
        return new HexaflowDtos.GeneratedBoardDto(List.copyOf(grid), List.copyOf(answers));
    }

    private String word(String value, String field) {
        String normalized = HexaflowPuzzleValidator.normalize(value);
        if (normalized.length() < 4) throw new BadRequestException(field + " deve avere almeno 4 lettere.");
        if (!normalized.matches("[A-Z]+")) throw new BadRequestException(field + " può contenere solo lettere, spazi, apostrofi o trattini.");
        return normalized;
    }

    private int addAnswer(List<HexaflowDtos.AnswerDto> answers, List<String> grid, List<Integer> route, int offset, String word, HexaflowDtos.AnswerType type) {
        List<Integer> path = new ArrayList<>();
        for (int index = 0; index < word.length(); index++) {
            int cell = route.get(offset + index);
            path.add(cell);
            grid.set(cell, String.valueOf(word.charAt(index)));
        }
        answers.add(new HexaflowDtos.AnswerDto(UUID.randomUUID().toString(), word, type, List.copyOf(path)));
        return offset + word.length();
    }

    /**
     * Builds a Hamiltonian route and repeatedly rewires two of its links.  Starting from a
     * horizontal snake guarantees that the first six cells already span the board, while the
     * rewiring makes the words weave through the grid instead of following predictable rows.
     */
    private List<Integer> intricateRoute(int flowLength) {
        List<Integer> route = serpentineRoute();
        int rewires = 0;
        for (int attempts = 0; attempts < 2_000 && rewires < 72; attempts++) {
            int firstEdge = ThreadLocalRandom.current().nextInt(CELLS - 3);
            int secondEdge = ThreadLocalRandom.current().nextInt(firstEdge + 2, CELLS - 1);
            int first = route.get(firstEdge);
            int next = route.get(firstEdge + 1);
            int last = route.get(secondEdge);
            int afterLast = route.get(secondEdge + 1);

            // A 2-opt reversal preserves every cell exactly once.  It is valid only when the
            // two new links are neighbours in the hex grid.
            if (!HexaflowPuzzleValidator.adjacent(first, last)
                    || !HexaflowPuzzleValidator.adjacent(next, afterLast)) continue;
            Collections.reverse(route.subList(firstEdge + 1, secondEdge + 1));
            if (touchesOppositeSides(route, flowLength)) rewires++;
            else Collections.reverse(route.subList(firstEdge + 1, secondEdge + 1));
        }
        return route;
    }

    private boolean touchesOppositeSides(List<Integer> route, int length) {
        boolean top = false, bottom = false, left = false, right = false;
        for (int index = 0; index < length; index++) {
            int cell = route.get(index);
            top |= cell < COLUMNS;
            bottom |= cell >= CELLS - COLUMNS;
            left |= cell % COLUMNS == 0;
            right |= cell % COLUMNS == COLUMNS - 1;
        }
        return (top && bottom) || (left && right);
    }

    private List<Integer> serpentineRoute() {
        int outer = ROWS;
        int inner = COLUMNS;
        int outerStart = ThreadLocalRandom.current().nextBoolean() ? 0 : outer - 1;
        int outerStep = outerStart == 0 ? 1 : -1;
        int innerStart = ThreadLocalRandom.current().nextBoolean() ? 0 : inner - 1;
        int innerStep = innerStart == 0 ? 1 : -1;
        List<Integer> route = new ArrayList<>(CELLS);
        for (int outerOffset = 0; outerOffset < outer; outerOffset++) {
            int outerIndex = outerStart + outerOffset * outerStep;
            int direction = outerOffset % 2 == 0 ? innerStep : -innerStep;
            int start = outerOffset % 2 == 0 ? innerStart : innerStart == 0 ? inner - 1 : 0;
            for (int innerOffset = 0; innerOffset < inner; innerOffset++) {
                int innerIndex = start + innerOffset * direction;
                route.add(outerIndex * COLUMNS + innerIndex);
            }
        }
        return route;
    }
}
