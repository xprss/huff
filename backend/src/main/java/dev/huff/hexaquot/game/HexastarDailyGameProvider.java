package dev.huff.hexaquot.game;

import dev.huff.hexaquot.game.HexastarDtos.SyllableResultDto;
import jakarta.annotation.PostConstruct;
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
public class HexastarDailyGameProvider {
    public static final int RULES_VERSION = 1;
    public static final int MAX_ATTEMPTS = 6;

    @ConfigProperty(name = "app.hexastar.seed")
    String seed;

    @Inject
    WordsProvider wordsProvider;

    @Inject
    ItalianSyllabifier syllabifier;

    private List<WordEntry> pool;

    @PostConstruct
    void buildPool() {
        pool = wordsProvider.words().stream()
            .map(word -> new WordEntry(word, syllabifier.syllabify(word)))
            .filter(entry -> syllabifier.isEligible(entry.word(), entry.syllables()))
            .toList();
        if (pool.isEmpty()) throw new IllegalStateException("Hexastar word pool is empty");
    }

    public WordEntry solutionFor(String puzzleDate) {
        return solutionFor(puzzleDate, seed);
    }

    WordEntry solutionFor(String puzzleDate, String selectionSeed) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest((selectionSeed + ":hexastar:" + puzzleDate).getBytes(StandardCharsets.UTF_8));
            int index = new BigInteger(1, hash).mod(BigInteger.valueOf(pool.size())).intValue();
            return pool.get(index);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot choose daily Hexastar solution", error);
        }
    }

    List<WordEntry> pool() {
        return pool;
    }

    public String validateRequestId(String rawRequestId) {
        String requestId = rawRequestId == null ? "" : rawRequestId.trim();
        if (!requestId.matches("[A-Za-z0-9_-]{8,100}")) {
            throw new BadRequestException("Identificativo richiesta non valido.");
        }
        return requestId;
    }

    public List<String> normalizeAndValidateGuess(List<String> rawSyllables, List<Integer> expectedLengths) {
        if (rawSyllables == null || rawSyllables.size() != expectedLengths.size()) {
            throw new BadRequestException("Completa tutti gli spazi delle sillabe.");
        }
        List<String> syllables = rawSyllables.stream().map(wordsProvider::normalize).toList();
        for (int index = 0; index < syllables.size(); index++) {
            if (!syllables.get(index).matches("[a-z]{" + expectedLengths.get(index) + "}")) {
                throw new BadRequestException("Completa ogni sillaba rispettando gli spazi.");
            }
        }
        String word = String.join("", syllables);
        if (!wordsProvider.contains(word)) {
            throw new BadRequestException("Parola non presente nella lista.");
        }
        List<String> canonical = syllabifier.syllabify(word);
        if (!canonical.equals(syllables)) {
            throw new BadRequestException("La divisione inserita non forma sillabe valide.");
        }
        return List.copyOf(syllables);
    }

    public List<SyllableResultDto> score(List<String> guess, List<String> solution) {
        List<SyllableResultDto> tiles = new ArrayList<>();
        Map<String, Integer> remaining = new HashMap<>();
        for (int index = 0; index < solution.size(); index++) {
            String syllable = guess.get(index);
            if (syllable.equals(solution.get(index))) {
                tiles.add(new SyllableResultDto(syllable, TileState.CORRECT));
            } else {
                tiles.add(null);
                remaining.merge(solution.get(index), 1, Integer::sum);
            }
        }
        for (int index = 0; index < guess.size(); index++) {
            if (tiles.get(index) != null) continue;
            String syllable = guess.get(index);
            int available = remaining.getOrDefault(syllable, 0);
            tiles.set(index, new SyllableResultDto(syllable, available > 0 ? TileState.PRESENT : TileState.ABSENT));
            if (available > 0) remaining.put(syllable, available - 1);
        }
        return List.copyOf(tiles);
    }

    public record WordEntry(String word, List<String> syllables) {
        public WordEntry {
            syllables = List.copyOf(syllables);
        }

        public List<Integer> lengths() {
            return syllables.stream().map(String::length).toList();
        }
    }
}
