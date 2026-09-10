package dev.huff.hexaquot.game;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class HexastarDailyGameProviderTest {
    @Inject HexastarDailyGameProvider provider;
    @Inject ItalianSyllabifier syllabifier;

    @Test
    void buildsOnlyEligibleSixLetterWords() {
        assertTrue(provider.pool().size() > 1_000);
        assertTrue(provider.pool().stream().allMatch(entry ->
            entry.word().length() == WordsProvider.WORD_LENGTH
                && syllabifier.isEligible(entry.word(), entry.syllables())));
    }

    @Test
    void selectionIsDeterministicAndUsesAnIndependentSeed() {
        List<String> first = IntStream.rangeClosed(1, 24)
            .mapToObj(day -> provider.solutionFor("2026-09-%02d".formatted(day), "hexastar-seed").word()).toList();
        assertEquals(first, IntStream.rangeClosed(1, 24)
            .mapToObj(day -> provider.solutionFor("2026-09-%02d".formatted(day), "hexastar-seed").word()).toList());
        List<String> second = IntStream.rangeClosed(1, 24)
            .mapToObj(day -> provider.solutionFor("2026-09-%02d".formatted(day), "hexaword-seed").word()).toList();
        assertNotEquals(first, second);
    }

    @Test
    void validatesDictionaryMembershipCanonicalBoundariesAndShape() {
        assertEquals(List.of("ca", "sa", "ta"), provider.normalizeAndValidateGuess(
            List.of("CA", "SA", "TA"), List.of(2, 2, 2)));
        assertThrows(BadRequestException.class, () -> provider.normalizeAndValidateGuess(
            List.of("xx", "xx", "xx"), List.of(2, 2, 2)));
        assertThrows(BadRequestException.class, () -> provider.normalizeAndValidateGuess(
            List.of("cas", "a", "ta"), List.of(3, 1, 2)));
        assertThrows(BadRequestException.class, () -> provider.normalizeAndValidateGuess(
            List.of("ca", "s", "ata"), List.of(2, 2, 2)));
    }

    @Test
    void scoresDuplicateSyllablesWithTwoPassAccounting() {
        var tiles = provider.score(List.of("ba", "ba", "na"), List.of("ba", "na", "na"));
        assertEquals(List.of(TileState.CORRECT, TileState.ABSENT, TileState.CORRECT),
            tiles.stream().map(HexastarDtos.SyllableResultDto::state).toList());
    }

    @Test
    void marksPresentLettersAndIdentifiesTheirSolutionSyllable() {
        var tiles = provider.score(List.of("ba", "sa", "na"), List.of("ca", "sa", "ta"));

        assertEquals(List.of(TileState.ABSENT, TileState.PRESENT), tiles.get(0).letters().stream()
            .map(HexastarDtos.LetterResultDto::state).toList());
        assertEquals(List.of(null, 1), tiles.get(0).letters().stream()
            .map(HexastarDtos.LetterResultDto::solutionSyllableIndex).toList());
        assertEquals(List.of(TileState.CORRECT, TileState.CORRECT), tiles.get(1).letters().stream()
            .map(HexastarDtos.LetterResultDto::state).toList());
    }
}
