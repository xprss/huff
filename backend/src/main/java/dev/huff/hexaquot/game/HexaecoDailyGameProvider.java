package dev.huff.hexaquot.game;

import dev.huff.hexaquot.game.HexaecoDtos.BoardDto;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * Version 1 catalogue: connected boards with independently tested 10–14 move solutions.
 * Keep this catalogue and its ordering immutable: saved submissions depend on it.
 * Each 32-day block uses another symmetry; the complete rotation lasts 256 days.
 */
@ApplicationScoped
public class HexaecoDailyGameProvider {
    // light start, echo start, light goal, echo goal, then walls.
    private static final int[][] CATALOGUE = {
        {10, 20, 22, 2, 3, 6, 12, 13, 16},
        {1, 23, 16, 5, 0, 7, 15, 17},
        {4, 20, 19, 8, 1, 7, 10, 13, 21, 22},
        {15, 23, 7, 1, 4, 16, 18, 19, 24},
        {3, 7, 20, 14, 16, 19, 22, 23, 24},
        {5, 14, 22, 2, 0, 4, 6, 13, 18},
        {10, 19, 20, 0, 11, 13, 18, 24},
        {22, 14, 23, 20, 0, 11, 12, 13, 17, 24},
        {24, 22, 1, 11, 7, 8, 9, 17, 18, 23},
        {14, 4, 2, 21, 6, 7, 8, 22, 23},
        {18, 11, 4, 24, 2, 6, 9, 17, 20},
        {5, 6, 17, 2, 7, 9, 10, 11, 15, 18, 24},
        {14, 3, 5, 24, 1, 2, 4, 7, 11, 18},
        {15, 8, 12, 21, 0, 6, 9, 16, 22, 24},
        {18, 5, 21, 19, 11, 17, 22, 24},
        {0, 3, 4, 10, 5, 8, 9, 17, 20, 22},
        {2, 19, 22, 3, 9, 12, 14, 21, 23},
        {7, 15, 17, 9, 4, 10, 12, 19},
        {24, 13, 7, 23, 6, 15, 17, 19, 20, 22},
        {19, 23, 24, 9, 4, 7, 12, 18, 21},
        {2, 9, 21, 1, 3, 10, 13, 16, 19, 24},
        {15, 4, 10, 1, 0, 3, 7, 8, 13},
        {1, 0, 24, 22, 5, 12, 15, 16, 17},
        {4, 10, 2, 21, 3, 7, 14, 17, 22},
        {1, 5, 0, 8, 4, 6, 11, 24},
        {24, 19, 20, 1, 0, 13, 14, 23},
        {22, 13, 0, 20, 9, 10, 11, 24},
        {2, 18, 24, 17, 1, 7, 9, 12, 20},
        {4, 2, 12, 23, 0, 8, 9, 10, 20, 24},
        {8, 22, 10, 2, 7, 9, 13, 14, 18, 20, 21},
        {4, 21, 8, 0, 5, 6, 10, 12, 20},
        {11, 22, 4, 13, 12, 14, 15, 18, 20}
    };

    public BoardDto boardFor(String date) {
        long day = LocalDate.parse(date).toEpochDay();
        return catalogueBoard(Math.floorMod(day, CATALOGUE.length),
            (int) Math.floorMod(Math.floorDiv(day, CATALOGUE.length), 8));
    }

    static int catalogueSize() { return CATALOGUE.length; }

    static BoardDto catalogueBoard(int index, int symmetry) {
        int[] cells = CATALOGUE[index];
        return new BoardDto(Arrays.stream(cells).skip(4).map(cell -> transform(cell, symmetry)).sorted().boxed().toList(),
            transform(cells[0], symmetry), transform(cells[1], symmetry),
            transform(cells[2], symmetry), transform(cells[3], symmetry));
    }

    private static int transform(int cell, int symmetry) {
        int x = cell % 5, y = cell / 5;
        if (symmetry >= 4) x = 4 - x;
        for (int turn = 0; turn < symmetry % 4; turn++) {
            int nextX = 4 - y;
            y = x;
            x = nextX;
        }
        return y * 5 + x;
    }
}
