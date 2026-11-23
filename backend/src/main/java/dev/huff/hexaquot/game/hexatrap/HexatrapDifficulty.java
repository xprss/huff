package dev.huff.hexaquot.game.hexatrap;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/** Weekly difficulty curve and deterministic rotation of case formats. */
public final class HexatrapDifficulty {
    public static final List<String> FORMATS = List.of(
        "furto", "alibi", "sabotaggio", "mappa", "eredità", "codice", "scomparsa", "falsario", "avvelenamento", "contrabbando");

    private HexatrapDifficulty() {}

    /** Monday is the gentlest case, the weekend the hardest. */
    public static int forDate(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return switch (day) {
            case MONDAY -> 1;
            case TUESDAY -> 2;
            case WEDNESDAY, THURSDAY -> 3;
            case FRIDAY -> 4;
            case SATURDAY, SUNDAY -> 5;
        };
    }

    /** A format chosen from the date and seed, never the same as the previous day's. */
    public static String format(LocalDate date, String seed) {
        int today = index(date, seed);
        int yesterday = index(date.minusDays(1), seed);
        return FORMATS.get(today == yesterday ? (today + 1) % FORMATS.size() : today);
    }

    private static int index(LocalDate date, String seed) {
        return (int) Math.floorMod(hash(seed + "|hexatrap|v2|format|" + date), FORMATS.size());
    }

    /** Stable 64-bit value for date-seeded simulations. */
    public static long hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(digest).getLong();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    /** Fewest investigation actions a case of this difficulty should require. */
    public static int minimumActions(int difficulty) {
        return difficulty <= 2 ? 2 : 3;
    }

    /** Upper bound of the expert's par, in minutes. */
    public static int maximumPar(int difficulty) {
        return (int) Math.round(HexatrapRules.TIME_BUDGET * (0.45 + 0.07 * difficulty));
    }

    /** Lower bound of the expert's par, in minutes. */
    public static int minimumPar(int difficulty) {
        return 10 + 4 * difficulty;
    }
}
