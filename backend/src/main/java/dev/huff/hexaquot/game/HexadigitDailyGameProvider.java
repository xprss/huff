package dev.huff.hexaquot.game;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class HexadigitDailyGameProvider implements DailyGameProvider<HexadigitGuessResult> {
    public static final int CODE_LENGTH = 6;

    @ConfigProperty(name = "app.hexadigit.seed", defaultValue = "${app.word.seed}:hexadigit")
    String seed;

    @Override
    public String id() { return "hexadigit"; }

    @Override
    public int answerLength() { return CODE_LENGTH; }

    @Override
    public int maxAttempts() { return 6; }

    @Override
    public String normalize(String rawGuess) { return rawGuess == null ? "" : rawGuess.trim(); }

    @Override
    public void validate(String guess) {
        if (!guess.matches("[0-9]{6}")) {
            throw new BadRequestException("Inserisci esattamente 6 cifre.");
        }
    }

    @Override
    public String solutionFor(String puzzleDate) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest((seed + ":" + puzzleDate).getBytes(StandardCharsets.UTF_8));
            int value = new BigInteger(1, hash).mod(BigInteger.valueOf(1_000_000)).intValue();
            return String.format("%06d", value);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot choose daily Hexadigit solution", error);
        }
    }

    @Override
    public HexadigitGuessResult score(String guess, String solution) {
        List<TileState> states = new ArrayList<>();
        Map<Character, Integer> remaining = new HashMap<>();
        for (int index = 0; index < CODE_LENGTH; index++) {
            if (guess.charAt(index) == solution.charAt(index)) {
                states.add(TileState.CORRECT);
            } else {
                states.add(null);
                remaining.merge(solution.charAt(index), 1, Integer::sum);
            }
        }
        for (int index = 0; index < CODE_LENGTH; index++) {
            if (states.get(index) != null) continue;
            char digit = guess.charAt(index);
            int available = remaining.getOrDefault(digit, 0);
            states.set(index, available > 0 ? TileState.PRESENT : TileState.ABSENT);
            if (available > 0) remaining.put(digit, available - 1);
        }

        List<HexadigitTileResult> tiles = new ArrayList<>();
        for (int index = 0; index < CODE_LENGTH; index++) {
            int guessed = guess.charAt(index) - '0';
            int expected = solution.charAt(index) - '0';
            DigitComparison comparison = guessed == expected
                ? DigitComparison.EQUAL
                : guessed < expected ? DigitComparison.HIGHER : DigitComparison.LOWER;
            tiles.add(new HexadigitTileResult(String.valueOf(guess.charAt(index)), states.get(index), comparison));
        }
        return new HexadigitGuessResult(guess, List.copyOf(tiles));
    }
}
