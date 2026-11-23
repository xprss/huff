package dev.huff.hexaquot.game.hexatrap;

import dev.huff.hexaquot.game.hexatrap.HexatrapCase.Fact;
import dev.huff.hexaquot.game.hexatrap.HexatrapFormula.*;

import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * Every hypothesis of a case: a world (one option per fact, mixed radix) and the liar.
 * Hypothesis {@code h = world * 2 + liar}, where liar 0 means A lies and 1 means B lies.
 */
public final class HexatrapHypothesisSpace {
    public static final int MAX_WORLDS = 4096;

    private final List<Fact> facts;
    private final int[] radix;
    private final int worlds;

    public HexatrapHypothesisSpace(List<Fact> facts) {
        this.facts = List.copyOf(facts);
        this.radix = new int[facts.size()];
        long product = 1;
        for (int i = 0; i < facts.size(); i++) {
            radix[i] = facts.get(i).options().size();
            product *= Math.max(1, radix[i]);
            if (product > MAX_WORLDS) throw new IllegalArgumentException("Troppi mondi possibili: massimo " + MAX_WORLDS + ".");
        }
        this.worlds = (int) product;
    }

    public int worlds() { return worlds; }

    public int size() { return worlds * 2; }

    public static int world(int hypothesis) { return hypothesis >> 1; }

    /** "A" or "B": who lies in the hypothesis. */
    public static String liar(int hypothesis) { return (hypothesis & 1) == 0 ? "A" : "B"; }

    public static int hypothesis(int world, String liar) { return world * 2 + ("A".equals(liar) ? 0 : 1); }

    /** Option index of the given fact in the given world. */
    public int option(int world, int factIndex) {
        int value = world;
        for (int i = facts.size() - 1; i > factIndex; i--) value /= radix[i];
        return value % radix[factIndex];
    }

    public int factIndex(String factId) {
        for (int i = 0; i < facts.size(); i++) if (facts.get(i).id().equals(factId)) return i;
        return -1;
    }

    public int worldOf(Map<String, String> assignment) {
        int world = 0;
        for (int i = 0; i < facts.size(); i++) {
            int option = facts.get(i).indexOf(assignment.get(facts.get(i).id()));
            if (option < 0) return -1;
            world = world * radix[i] + option;
        }
        return world;
    }

    public String optionId(int world, String factId) {
        int index = factIndex(factId);
        return facts.get(index).options().get(option(world, index)).id();
    }

    public BitSet all() {
        BitSet result = new BitSet(size());
        result.set(0, size());
        return result;
    }

    /** Hypotheses where the formula holds. */
    public BitSet eval(HexatrapFormula formula) {
        BitSet result = new BitSet(size());
        for (int h = 0; h < size(); h++) if (holds(formula, h)) result.set(h);
        return result;
    }

    /** Hypotheses where the character's assertion of the formula is consistent with their role. */
    public BitSet asserted(HexatrapFormula formula, String speaker) {
        BitSet result = new BitSet(size());
        for (int h = 0; h < size(); h++) if (holds(formula, h) == !liar(h).equals(speaker)) result.set(h);
        return result;
    }

    /** The character's yes/no answer to the formula in the hypothesis. */
    public boolean answer(HexatrapFormula formula, String character, int hypothesis) {
        return holds(formula, hypothesis) == !liar(hypothesis).equals(character);
    }

    /** Hypotheses where the character answers the formula as given. */
    public BitSet answered(HexatrapFormula formula, String character, boolean answer) {
        BitSet result = new BitSet(size());
        for (int h = 0; h < size(); h++) if (answer(formula, character, h) == answer) result.set(h);
        return result;
    }

    public boolean holds(HexatrapFormula formula, int hypothesis) {
        if (formula instanceof Const c) return c.value();
        if (formula instanceof FactIs f) {
            int index = factIndex(f.fact());
            boolean equal = index >= 0 && facts.get(index).indexOf(f.option()) == option(world(hypothesis), index);
            return equal != f.negated();
        }
        if (formula instanceof Not n) return !holds(n.operand(), hypothesis);
        if (formula instanceof And a) {
            for (HexatrapFormula operand : a.operands()) if (!holds(operand, hypothesis)) return false;
            return true;
        }
        if (formula instanceof Or o) {
            for (HexatrapFormula operand : o.operands()) if (holds(operand, hypothesis)) return true;
            return false;
        }
        if (formula instanceof Xor x) return holds(x.left(), hypothesis) != holds(x.right(), hypothesis);
        if (formula instanceof Implies i) return !holds(i.premise(), hypothesis) || holds(i.conclusion(), hypothesis);
        if (formula instanceof Iff i) return holds(i.left(), hypothesis) == holds(i.right(), hypothesis);
        if (formula instanceof Count c) {
            int count = 0;
            for (HexatrapFormula operand : c.operands()) if (holds(operand, hypothesis)) count++;
            return count == c.k();
        }
        if (formula instanceof Truthful t) return !liar(hypothesis).equals(t.character());
        if (formula instanceof Says s) return holds(s.operand(), hypothesis) == !liar(hypothesis).equals(s.character());
        throw new IllegalArgumentException("Formula sconosciuta: " + formula);
    }
}
