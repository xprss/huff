package dev.huff.hexaquot.game.hexatrap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The case as written by the authoring agent and stored in {@code hexatrap_cases.case_json}.
 * Field names mirror the agent's structured output, so a draft deserializes directly.
 */
public final class HexatrapCase {
    private HexatrapCase() {}

    public static final List<String> CHARACTER_IDS = List.of("A", "B");

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CaseDefinition(
        String title,
        String intro,
        String epilogue,
        String format,
        List<Character> characters,
        List<Fact> facts,
        List<Item> certainties,
        List<Item> statements,
        List<Item> questions,
        List<Item> clues,
        List<String> verdictFacts,
        Solution solution
    ) {
        public List<Character> characters() { return characters == null ? List.of() : characters; }
        public List<Fact> facts() { return facts == null ? List.of() : facts; }
        public List<Item> certainties() { return certainties == null ? List.of() : certainties; }
        public List<Item> statements() { return statements == null ? List.of() : statements; }
        public List<Item> questions() { return questions == null ? List.of() : questions; }
        public List<Item> clues() { return clues == null ? List.of() : clues; }
        public List<String> verdictFacts() { return verdictFacts == null ? List.of() : verdictFacts; }

        public Character character(String id) {
            return characters().stream().filter(c -> id.equals(c.id())).findFirst().orElse(null);
        }

        public Fact fact(String id) {
            return facts().stream().filter(f -> id.equals(f.id())).findFirst().orElse(null);
        }

        public Item question(String id) {
            return questions().stream().filter(q -> id.equals(q.id())).findFirst().orElse(null);
        }

        public Item clue(String id) {
            return clues().stream().filter(c -> id.equals(c.id())).findFirst().orElse(null);
        }

        /** Every item that carries a formula, in a stable order. */
        public List<Item> allItems() {
            List<Item> all = new ArrayList<>();
            all.addAll(certainties());
            all.addAll(statements());
            all.addAll(questions());
            all.addAll(clues());
            return all;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Character(String id, String name, String role, String icon) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Fact(String id, String label, List<Option> options) {
        public List<Option> options() { return options == null ? List.of() : options; }

        public int indexOf(String option) {
            for (int i = 0; i < options().size(); i++) if (options().get(i).id().equals(option)) return i;
            return -1;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Option(String id, String label, String affirm, String deny) {}

    /**
     * A certainty, statement, question or clue. {@code speaker} is set on statements, {@code to} on
     * questions (the character who is asked), {@code title} and {@code cost} on clues.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String id, String speaker, String to, String title, String text, Integer cost, String formula) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Solution(String liar, Map<String, String> facts) {
        public Map<String, String> facts() { return facts == null ? Map.of() : facts; }
    }

    public enum ItemKind { CERTAINTY, STATEMENT, QUESTION, CLUE }

    public static String other(String character) {
        return "A".equals(character) ? "B" : "A";
    }
}
