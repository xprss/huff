package dev.huff.hexaquot.game;

import java.util.ArrayList;
import java.util.List;
import dev.huff.hexaquot.game.HexatrapDtos.*;

/** Version 1: hypothesis = configuration * 2 + loyal advisor (0=A, 1=B). */
public final class HexatrapRules {
    public static final int RULES_VERSION = 1;
    public static final int MAX_NUMBER = 9864;
    private HexatrapRules() {}
    public static boolean lit(int configuration, int seal) { return (configuration & (1 << seal)) != 0; }
    public static boolean allows(Dossier dossier, int configuration) {
        if (Integer.bitCount(configuration) != dossier.litCount()) return false;
        boolean a = lit(configuration, dossier.firstSeal()), b = lit(configuration, dossier.secondSeal());
        return switch (dossier.relation()) {
            case SAME -> a == b;
            case IMPLIES -> !a || b;
            case AT_LEAST_ONE -> a || b;
        };
    }
    public static List<Integer> hypotheses(Dossier dossier) {
        var result = new ArrayList<Integer>();
        for (int mask = 0; mask < 64; mask++) if (allows(dossier, mask)) {
            result.add(mask * 2); result.add(mask * 2 + 1);
        }
        return List.copyOf(result);
    }
    public static boolean answer(int hypothesis, int advisor, int seal) {
        return lit(hypothesis / 2, seal) == (hypothesis % 2 == advisor);
    }
    public static List<Integer> retain(List<Integer> hypotheses, int advisor, int seal, boolean answer) {
        return hypotheses.stream().filter(h -> answer(h, advisor, seal) == answer).toList();
    }
    public static boolean unlocked(List<Integer> hypotheses) {
        return !hypotheses.isEmpty() && hypotheses.stream().map(h -> h % 2).distinct().count() == 1;
    }
    public static long points(long questions, long wrong) { return 1000L - 100L * questions - 200L * wrong; }
}
