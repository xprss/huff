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
        List<Integer> route = serpentineRoute(flow.length());
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

    /** A randomized Hamiltonian route; the first run makes the Flusso touch two opposite sides. */
    private List<Integer> serpentineRoute(int flowLength) {
        boolean horizontal = flowLength < ROWS || ThreadLocalRandom.current().nextBoolean();
        int outer = horizontal ? ROWS : COLUMNS;
        int inner = horizontal ? COLUMNS : ROWS;
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
                route.add(horizontal ? outerIndex * COLUMNS + innerIndex : innerIndex * COLUMNS + outerIndex);
            }
        }
        return route;
    }
}
