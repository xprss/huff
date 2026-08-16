package dev.huff.hexaquot.game;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
public class HexawordDailyGameProvider implements DailyGameProvider<GuessResult> {
    @ConfigProperty(name = "app.word.seed")
    String seed;

    @Inject
    WordsProvider wordsProvider;

    @Override
    public String id() {
        return "hexaword";
    }

    @Override
    public int answerLength() {
        return WordsProvider.WORD_LENGTH;
    }

    @Override
    public int maxAttempts() {
        return DailyGameService.MAX_ATTEMPTS;
    }

    @Override
    public String normalize(String rawGuess) {
        return wordsProvider.normalize(rawGuess);
    }

    @Override
    public void validate(String guess) {
        if (!guess.matches("[a-z]{" + answerLength() + "}")) {
            throw new BadRequestException("Completa tutte e 6 le caselle.");
        }
        if (!wordsProvider.contains(guess)) {
            throw new BadRequestException("Parola non presente nella lista.");
        }
    }

    @Override
    public String solutionFor(String puzzleDate) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest((seed + ":" + puzzleDate).getBytes(StandardCharsets.UTF_8));
            int index = new BigInteger(1, hash).mod(BigInteger.valueOf(wordsProvider.words().size())).intValue();
            return wordsProvider.words().get(index);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot choose daily Hexaword solution", error);
        }
    }

    @Override
    public GuessResult score(String guess, String solution) {
        List<TileResult> tiles = new ArrayList<>();
        Map<Character, Integer> remaining = new HashMap<>();
        for (int index = 0; index < answerLength(); index++) {
            char guessLetter = guess.charAt(index);
            char solutionLetter = solution.charAt(index);
            if (guessLetter == solutionLetter) {
                tiles.add(new TileResult(String.valueOf(guessLetter), TileState.CORRECT));
            } else {
                tiles.add(null);
                remaining.merge(solutionLetter, 1, Integer::sum);
            }
        }
        for (int index = 0; index < answerLength(); index++) {
            if (tiles.get(index) != null) continue;
            char letter = guess.charAt(index);
            int available = remaining.getOrDefault(letter, 0);
            tiles.set(index, new TileResult(String.valueOf(letter), available > 0 ? TileState.PRESENT : TileState.ABSENT));
            if (available > 0) remaining.put(letter, available - 1);
        }
        return new GuessResult(guess, List.copyOf(tiles));
    }
}
