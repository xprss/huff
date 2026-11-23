package dev.huff.hexaquot.game.hexatrap;

import java.util.List;

/** Fixed rules of Hexatrap version 2, "Il caso del giorno". */
public final class HexatrapRules {
    public static final int RULES_VERSION = 2;
    /** Minutes of investigation available to the player. */
    public static final int TIME_BUDGET = 60;
    public static final int QUESTION_COST = 5;
    public static final List<Integer> CLUE_COSTS = List.of(8, 12, 16);

    private HexatrapRules() {}

    /** Points of a solved case: harder days and faster investigations are worth more. */
    public static long points(int difficulty, int minutesLeft) {
        return 300L + 100L * difficulty + 10L * Math.max(0, minutesLeft);
    }
}
