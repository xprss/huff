package dev.huff.hexaquot.game;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ServiceUnavailableException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@ApplicationScoped
public class HexaflowBoardGenerator {
    private static final int ROWS = 8;
    private static final int COLUMNS = 6;
    private static final int CELLS = ROWS * COLUMNS;
    private static final int SEARCH_RESTARTS = 96;
    private static final int SEARCH_STEPS = 12_000;

    public HexaflowDtos.GeneratedBoardDto generate(HexaflowDtos.BoardGenerationRequest request) {
        return generate(request, ThreadLocalRandom.current());
    }

    HexaflowDtos.GeneratedBoardDto generate(HexaflowDtos.BoardGenerationRequest request, Random random) {
        if (request == null) throw new BadRequestException("Inserisci le parole tema e il Flusso.");
        String flow = word(request.flowWord(), "Il Flusso");
        List<String> themes = new ArrayList<>();
        for (String value : request.themeWords() == null ? List.<String>of() : request.themeWords()) {
            if (value != null && !value.isBlank()) themes.add(word(value, "Ogni parola tema"));
        }
        if (themes.isEmpty()) throw new BadRequestException("Inserisci almeno una parola tema.");
        int total = flow.length() + themes.stream().mapToInt(String::length).sum();
        if (total != CELLS) throw new BadRequestException("Le parole devono usare esattamente 48 lettere: ora ne usano " + total + ".");

        Collections.shuffle(themes, random);
        List<Integer> lengths = new ArrayList<>();
        lengths.add(flow.length());
        themes.forEach(theme -> lengths.add(theme.length()));
        List<Integer> route = intricateRoute(lengths, random);
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
     * Searches cell permutations using endpoint and internal reversals, plus cell swaps.
     * Each move preserves full, disjoint coverage; only adjacent, non-crossing answer links
     * are accepted. Annealing allows temporary regressions in shape and Flusso span, but
     * only an arrangement meeting every word's complexity requirements is returned.
     */
    private List<Integer> intricateRoute(List<Integer> lengths, Random random) {
        for (int restart = 0; restart < SEARCH_RESTARTS; restart++) {
            List<Integer> route = serpentineRoute(random);
            int penalty = complexityPenalty(route, lengths);
            for (int attempt = 0; attempt < SEARCH_STEPS; attempt++) {
                int start, end;
                if (random.nextBoolean()) {
                    boolean head = random.nextBoolean();
                    int target = head ? random.nextInt(2, CELLS) : random.nextInt(CELLS - 2);
                    start = head ? 0 : target + 1;
                    end = head ? target : CELLS;
                } else {
                    start = random.nextInt(CELLS - 1);
                    end = random.nextInt(start + 2, CELLS + 1);
                }
                List<Integer> reversed = route.subList(start, end);
                boolean swap = random.nextInt(4) == 0;
                rearrange(reversed, swap);
                if (!hasValidLinks(route, lengths)) {
                    rearrange(reversed, swap);
                    continue;
                }
                int candidatePenalty = complexityPenalty(route, lengths);
                if (candidatePenalty == 0) return route;
                double temperature = 0.15 + 3.0 * (1.0 - (double) attempt / SEARCH_STEPS);
                if (candidatePenalty <= penalty || random.nextDouble() < Math.exp((penalty - candidatePenalty) / temperature)) {
                    penalty = candidatePenalty;
                } else {
                    rearrange(reversed, swap);
                }
            }
        }
        throw new ServiceUnavailableException("Non è stato trovato un incastro senza incroci con percorsi abbastanza articolati. Riprova oppure cambia le parole o la lunghezza del Flusso.");
    }

    private void rearrange(List<Integer> cells, boolean swap) {
        if (swap) Collections.swap(cells, 0, cells.size() - 1);
        else Collections.reverse(cells);
    }

    private boolean hasValidLinks(List<Integer> route, List<Integer> lengths) {
        // Only opposite diagonals of the same square can cross. Connections between
        // words are not drawn, so they must not constrain the arrangement of the answers.
        long descending = 0, ascending = 0;
        int offset = 0;
        for (int length : lengths) {
            for (int index = offset + 1; index < offset + length; index++) {
                int from = route.get(index - 1), to = route.get(index);
                if (!HexaflowPuzzleValidator.adjacent(from, to)) return false;
                int row = from / COLUMNS, column = from % COLUMNS;
                int nextRow = to / COLUMNS, nextColumn = to % COLUMNS;
                if (row == nextRow || column == nextColumn) continue;
                long square = 1L << (Math.min(row, nextRow) * (COLUMNS - 1) + Math.min(column, nextColumn));
                if ((nextRow - row) * (nextColumn - column) > 0) descending |= square;
                else ascending |= square;
                if ((descending & ascending) != 0) return false;
            }
            offset += length;
        }
        return true;
    }

    private int complexityPenalty(List<Integer> route, List<Integer> lengths) {
        int penalty = 12 * missingSides(route, lengths.get(0));
        int offset = 0;
        for (int length : lengths) {
            int turns = 0, run = 0, diagonals = 0;
            int previousRow = 0, previousColumn = 0;
            for (int index = offset + 1; index < offset + length; index++) {
                int from = route.get(index - 1), to = route.get(index);
                int row = to / COLUMNS - from / COLUMNS;
                int column = to % COLUMNS - from % COLUMNS;
                if (row != 0 && column != 0) diagonals++;
                if (index > offset + 1 && (row != previousRow || column != previousColumn)) {
                    turns++;
                    run = 1;
                } else {
                    run++;
                }
                if (run > 3) penalty += 4;
                previousRow = row;
                previousColumn = column;
            }
            // Even the shortest words must bend twice; longer ones must keep changing
            // direction. Score each word separately, excluding links between answers.
            penalty += 4 * Math.max(0, Math.max(2, (length - 1) / 2) - turns);
            if (diagonals == 0) penalty += 2;
            offset += length;
        }
        return penalty;
    }

    private int missingSides(List<Integer> route, int length) {
        boolean top = false, bottom = false, left = false, right = false;
        for (int index = 0; index < length; index++) {
            int cell = route.get(index);
            top |= cell < COLUMNS;
            bottom |= cell >= CELLS - COLUMNS;
            left |= cell % COLUMNS == 0;
            right |= cell % COLUMNS == COLUMNS - 1;
        }
        return Math.max(0, 2 - ((top ? 1 : 0) + (bottom ? 1 : 0) + (left ? 1 : 0) + (right ? 1 : 0)));
    }

    private List<Integer> serpentineRoute(Random random) {
        int outer = ROWS;
        int inner = COLUMNS;
        int outerStart = random.nextBoolean() ? 0 : outer - 1;
        int outerStep = outerStart == 0 ? 1 : -1;
        int innerStart = random.nextBoolean() ? 0 : inner - 1;
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
