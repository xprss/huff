package dev.huff.hexaquot.game;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@QuarkusTest
class ItalianSyllabifierTest {
    @Inject ItalianSyllabifier syllabifier;

    @Test
    void followsItalianPatternsForCommonGroupsAndDiphthongs() {
        assertEquals(List.of("ca", "sa", "ta"), syllabifier.syllabify("CASATA"));
        assertEquals(List.of("stra", "da"), syllabifier.syllabify("STRADA"));
        assertEquals(List.of("ba", "na", "na"), syllabifier.syllabify("BANANA"));
        assertEquals(List.of("chia", "ve"), syllabifier.syllabify("CHIAVE"));
        assertEquals(List.of("a", "iuo", "la"), syllabifier.syllabify("AIUOLA"));
    }

    @Test
    void appliesVersionedOverridesBeforePatterns() {
        assertEquals(List.of("a", "te", "ne", "o"), syllabifier.syllabify("ATENEO"));
    }

    @Test
    void excludesWordsOutsideTheSyllableContract() {
        List<String> boeing = syllabifier.syllabify("BOEING");
        assertFalse(syllabifier.isEligible("boeing", boeing));
        assertFalse(syllabifier.isEligible("casata", List.of("casata")));
        assertFalse(syllabifier.isEligible("casata", List.of("c", "asata")));
    }
}
