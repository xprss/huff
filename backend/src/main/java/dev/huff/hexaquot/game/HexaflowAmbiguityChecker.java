package dev.huff.hexaquot.game;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;

/**
 * Finds answers whose word can also be traced on the board along a path different from the intended one.
 * Gameplay accepts only the stored path (or its reverse when it spells the same word, i.e. palindromes),
 * so an alternative path would be counted as an extra.
 * The check is advisory: it never blocks saving or publishing.
 */
@ApplicationScoped
public class HexaflowAmbiguityChecker {
    static final int CELLS = 48;
    static final int COLUMNS = 6;
    /** Stop exploring a word after this many alternative paths, so degenerate grids stay fast. */
    static final int MAX_ALTERNATIVES = 50;
    /** Number of alternative paths sent back for each word. */
    static final int MAX_RETURNED_PATHS = 5;

    public HexaflowDtos.AmbiguityReportDto check(HexaflowDtos.PuzzleDraftDto draft) {
        List<String> grid = draft == null || draft.grid() == null ? List.of() : draft.grid();
        List<HexaflowDtos.AnswerDto> answers = draft == null || draft.answers() == null ? List.of() : draft.answers();
        if (grid.size() != CELLS) return new HexaflowDtos.AmbiguityReportDto(0, List.of());
        String[] letters = new String[CELLS];
        for (int i = 0; i < CELLS; i++) letters[i] = grid.get(i) == null ? "" : grid.get(i).trim().toUpperCase(Locale.ROOT);

        int checked = 0;
        List<HexaflowDtos.AmbiguousAnswerDto> ambiguous = new ArrayList<>();
        for (int index = 0; index < answers.size(); index++) {
            HexaflowDtos.AnswerDto answer = answers.get(index);
            if (answer == null) continue;
            List<Integer> path = validPath(answer.path());
            String word = path.isEmpty() ? HexaflowPuzzleValidator.normalize(answer.label()) : spell(letters, path);
            if (word.isEmpty() || !word.matches("[A-Z]+")) continue;
            checked++;
            Search search = new Search(letters, word, path);
            search.run();
            if (search.alternatives.isEmpty()) continue;
            ambiguous.add(new HexaflowDtos.AmbiguousAnswerDto(answer.id(), index, word, answer.type(),
                    search.alternatives.size(), search.truncated,
                    search.alternatives.stream().limit(MAX_RETURNED_PATHS).toList()));
        }
        return new HexaflowDtos.AmbiguityReportDto(checked, List.copyOf(ambiguous));
    }

    private static List<Integer> validPath(List<Integer> path) {
        if (path == null) return List.of();
        for (Integer cell : path) if (cell == null || cell < 0 || cell >= CELLS) return List.of();
        return path;
    }

    private static String spell(String[] letters, List<Integer> path) {
        StringBuilder word = new StringBuilder();
        for (int cell : path) word.append(letters[cell]);
        return word.toString();
    }

    private static final class Search {
        final String[] letters;
        final String word;
        final List<Integer> intended;
        final List<List<Integer>> alternatives = new ArrayList<>();
        final Set<List<Integer>> seen = new HashSet<>();
        final boolean[] used = new boolean[CELLS];
        final int[] current;
        boolean truncated;

        Search(String[] letters, String word, List<Integer> intended) {
            this.letters = letters;
            this.word = word;
            this.intended = intended;
            this.current = new int[word.length()];
        }

        void run() {
            String first = word.substring(0, 1);
            for (int cell = 0; cell < CELLS && !truncated; cell++) if (letters[cell].equals(first)) visit(cell, 0);
        }

        private void visit(int cell, int depth) {
            if (truncated) return;
            current[depth] = cell;
            if (depth == word.length() - 1) {
                record();
                return;
            }
            used[cell] = true;
            String next = word.substring(depth + 1, depth + 2);
            int row = cell / COLUMNS, column = cell % COLUMNS;
            for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int r = row + dr, c = column + dc;
                if (r < 0 || c < 0 || c >= COLUMNS || r * COLUMNS + c >= CELLS) continue;
                int neighbour = r * COLUMNS + c;
                if (!used[neighbour] && letters[neighbour].equals(next)) visit(neighbour, depth + 1);
            }
            used[cell] = false;
        }

        private void record() {
            List<Integer> path = new ArrayList<>(current.length);
            for (int cell : current) path.add(cell);
            List<Integer> reversed = new ArrayList<>(path);
            Collections.reverse(reversed);
            // Gameplay also accepts the reverse of the intended path when it spells the same word (palindromes).
            if (path.equals(intended) || reversed.equals(intended)) return;
            if (seen.contains(reversed) || !seen.add(path)) return;
            alternatives.add(List.copyOf(path));
            if (alternatives.size() >= MAX_ALTERNATIVES) truncated = true;
        }
    }
}
