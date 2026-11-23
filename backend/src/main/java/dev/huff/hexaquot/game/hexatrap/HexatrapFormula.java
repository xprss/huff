package dev.huff.hexaquot.game.hexatrap;

import java.util.List;
import java.util.Set;

/**
 * Abstract syntax of the Hexatrap case language. A hypothesis is a world (one option per fact)
 * plus the identity of the liar; formulas are evaluated by {@link HexatrapHypothesisSpace}.
 */
public sealed interface HexatrapFormula {
    record Const(boolean value) implements HexatrapFormula {}
    /** {@code fact=option}, or {@code fact!=option} when negated. */
    record FactIs(String fact, String option, boolean negated) implements HexatrapFormula {}
    record Not(HexatrapFormula operand) implements HexatrapFormula {}
    record And(List<HexatrapFormula> operands) implements HexatrapFormula {}
    record Or(List<HexatrapFormula> operands) implements HexatrapFormula {}
    record Xor(HexatrapFormula left, HexatrapFormula right) implements HexatrapFormula {}
    record Implies(HexatrapFormula premise, HexatrapFormula conclusion) implements HexatrapFormula {}
    record Iff(HexatrapFormula left, HexatrapFormula right) implements HexatrapFormula {}
    /** Exactly {@code k} operands are true. */
    record Count(int k, List<HexatrapFormula> operands) implements HexatrapFormula {}
    /** The character always tells the truth. */
    record Truthful(String character) implements HexatrapFormula {}
    /** What the character would answer if asked the operand: {@code operand XOR liar(character)}. */
    record Says(String character, HexatrapFormula operand) implements HexatrapFormula {}

    default List<HexatrapFormula> children() {
        if (this instanceof Not n) return List.of(n.operand());
        if (this instanceof And a) return a.operands();
        if (this instanceof Or o) return o.operands();
        if (this instanceof Xor x) return List.of(x.left(), x.right());
        if (this instanceof Implies i) return List.of(i.premise(), i.conclusion());
        if (this instanceof Iff i) return List.of(i.left(), i.right());
        if (this instanceof Count c) return c.operands();
        if (this instanceof Says s) return List.of(s.operand());
        return List.of();
    }

    default int size() {
        return 1 + children().stream().mapToInt(HexatrapFormula::size).sum();
    }

    default int depth() {
        return 1 + children().stream().mapToInt(HexatrapFormula::depth).max().orElse(0);
    }

    default int saysDepth() {
        int inner = children().stream().mapToInt(HexatrapFormula::saysDepth).max().orElse(0);
        return this instanceof Says ? inner + 1 : inner;
    }

    /** True when the formula mentions only facts (no TRUTHFUL or SAYS). */
    default boolean factsOnly() {
        if (this instanceof Truthful || this instanceof Says) return false;
        return children().stream().allMatch(HexatrapFormula::factsOnly);
    }

    default boolean mentionsSays(String character) {
        if (this instanceof Says s && s.character().equals(character)) return true;
        return children().stream().anyMatch(child -> child.mentionsSays(character));
    }

    default void collectCharacters(Set<String> into) {
        if (this instanceof Truthful t) into.add(t.character());
        if (this instanceof Says s) into.add(s.character());
        children().forEach(child -> child.collectCharacters(into));
    }

    default void collectAtoms(List<FactIs> into) {
        if (this instanceof FactIs f) into.add(f);
        children().forEach(child -> child.collectAtoms(into));
    }
}
