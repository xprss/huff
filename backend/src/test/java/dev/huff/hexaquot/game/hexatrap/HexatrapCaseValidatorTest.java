package dev.huff.hexaquot.game.hexatrap;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static dev.huff.hexaquot.game.hexatrap.HexatrapFixtures.item;
import static org.junit.jupiter.api.Assertions.*;

class HexatrapCaseValidatorTest {
    private static HexatrapCaseValidator.Report validate(Consumer<ObjectNode> mutation) {
        return HexatrapCaseValidator.validate(HexatrapFixtures.lanterna(mutation), 1, 11L);
    }

    private static void assertRejected(String code, Consumer<ObjectNode> mutation) {
        var report = validate(mutation);
        assertFalse(report.valid(), code);
        assertTrue(report.hard().stream().anyMatch(issue -> issue.code().equals(code)), () -> code + " not in " + report.issues());
        assertTrue(report.hard().stream().allMatch(issue -> issue.message() != null && !issue.message().isBlank()));
    }

    @Test
    void acceptsTheHandWrittenCase() {
        var report = validate(json -> {});
        assertTrue(report.valid(), report.issues().toString());
        assertNotNull(report.metrics());
        assertEquals(10, report.metrics().certificateCost());
        // Difficulty preferences are soft: an easy case for a Monday is just a warning.
        assertTrue(report.issues().stream().allMatch(HexatrapCaseValidator.Issue::soft));
    }

    @Test
    void appliesTheDifficultyBandsAsSoftIssues() {
        var hard = HexatrapCaseValidator.validate(HexatrapFixtures.lanterna(), 5, 11L);
        assertTrue(hard.valid());
        assertTrue(hard.soft().stream().anyMatch(issue -> issue.code().equals("difficulty_actions")));
    }

    @Test
    void rejectsStructuralMistakes() {
        assertRejected("character_icon", json -> ((ObjectNode) json.withArray("characters").get(0)).put("icon", "skull"));
        assertRejected("characters", json -> ((ObjectNode) json.withArray("characters").get(1)).put("id", "C"));
        assertRejected("id", json -> item(json, "c_maree").put("id", "c_cenere"));
        assertRejected("formula_syntax", json -> item(json, "s_ettore_nove").put("formula", "ora!=nove AND"));
        assertRejected("formula_reference", json -> item(json, "s_ettore_nove").put("formula", "ora=mezzanotte"));
        assertRejected("formula_reference", json -> item(json, "s_ettore_nove").put("formula", "luna=piena"));
        assertRejected("formula_facts_only", json -> item(json, "c_maree").put("formula", "TRUTHFUL(A)"));
        assertRejected("formula_self_says", json -> item(json, "q_ettore_stufa").put("formula", "SAYS(A, diario=stufa)"));
        assertRejected("formula_says", json -> item(json, "q_livia_cassa").put("formula", "SAYS(A, SAYS(B, diario=cassa))"));
        assertRejected("clue_cost", json -> item(json, "c_maree").put("cost", 10));
        assertRejected("text_length", json -> json.put("title", "X"));
        assertRejected("questions", json -> json.withArray("questions").remove(0));
        assertRejected("verdict_facts", json -> json.putArray("verdictFacts").add("luna"));
        assertRejected("solution", json -> ((ObjectNode) json.path("solution")).put("liar", "C"));
    }

    @Test
    void rejectsLogicalMistakes() {
        assertRejected("solution_violates_statement", json -> ((ObjectNode) json.path("solution")).put("liar", "A"));
        assertRejected("solution_violates_clue", json -> item(json, "c_maree").put("formula", "ora=undici"));
        assertRejected("solution_violates_certainty", json -> item(json, "k_cane").put("formula", "cane=abbaiato"));
        assertRejected("trivial_certainty", json -> item(json, "k_cane").put("formula", "cane=abbaiato OR cane=silenzio"));
        assertRejected("trivial_statement", json -> item(json, "s_livia_cane").put("formula", "TRUTHFUL(B)"));
        assertRejected("liar_known_at_start", json -> item(json, "s_ettore_nove").put("formula", "porta=aperta -> ora!=nove"));
        assertRejected("undetermined", json -> {
            item(json, "q_ettore_stufa").put("formula", "porta=aperta");
            item(json, "q_livia_cassa_diretta").put("formula", "porta=chiusa");
            item(json, "q_livia_cassa").put("formula", "SAYS(A, porta=chiusa)");
            item(json, "c_cenere").put("formula", "cane=silenzio");
        });
        assertRejected("guessable", json -> json.putArray("verdictFacts").add("porta"));
        assertRejected("budget_too_generous", json -> {
            for (var clue : json.withArray("clues")) ((ObjectNode) clue).put("cost", 8);
        });
    }
}
