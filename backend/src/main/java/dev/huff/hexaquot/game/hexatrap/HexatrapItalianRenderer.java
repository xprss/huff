package dev.huff.hexaquot.game.hexatrap;

import dev.huff.hexaquot.game.hexatrap.HexatrapCase.CaseDefinition;
import dev.huff.hexaquot.game.hexatrap.HexatrapCase.Character;
import dev.huff.hexaquot.game.hexatrap.HexatrapCase.Fact;
import dev.huff.hexaquot.game.hexatrap.HexatrapCase.Option;
import dev.huff.hexaquot.game.hexatrap.HexatrapFormula.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Renders formulas as fixed-template Italian built from the authored atom phrases. The rendering
 * ("verbale") is authoritative over the narrative text, so a case can never be ambiguous.
 */
public final class HexatrapItalianRenderer {
    private final CaseDefinition definition;

    public HexatrapItalianRenderer(CaseDefinition definition) {
        this.definition = definition;
    }

    public String formal(HexatrapFormula formula) {
        if (formula instanceof Const c) return c.value() ? "nessun vincolo" : "è impossibile";
        if (formula instanceof FactIs f) return atom(f.fact(), f.option(), f.negated());
        if (formula instanceof Truthful t) return name(t.character()) + " dice sempre la verità";
        if (formula instanceof Says s) return name(s.character()) + " risponderebbe «sì» a: «" + capitalize(formal(s.operand())) + "?»";
        if (formula instanceof Not n) {
            if (n.operand() instanceof FactIs f) return atom(f.fact(), f.option(), !f.negated());
            if (n.operand() instanceof Truthful t) return name(t.character()) + " mente sempre";
            if (n.operand() instanceof Count c) return count(c, true);
            return "non è vero che " + group(n.operand());
        }
        if (formula instanceof And a) return join(a.operands().stream().map(this::group).toList(), " e ");
        if (formula instanceof Or o) {
            if (o.operands().size() == 2) return group(o.operands().get(0)) + " oppure " + group(o.operands().get(1));
            return "almeno una di queste è vera: " + o.operands().stream().map(this::formal).collect(Collectors.joining("; "));
        }
        if (formula instanceof Xor x) return "o " + group(x.left()) + ", o " + group(x.right()) + ", ma non entrambe le cose";
        if (formula instanceof Implies i) return "se " + group(i.premise()) + ", allora " + group(i.conclusion());
        if (formula instanceof Iff i) return group(i.left()) + " se e solo se " + group(i.right());
        if (formula instanceof Count c) return count(c, false);
        throw new IllegalArgumentException("Formula sconosciuta: " + formula);
    }

    /** The question as the player reads it in the official record. */
    public String question(HexatrapFormula formula) {
        String text = capitalize(formal(formula));
        // A SAYS question already ends with the quoted inner question.
        return text.endsWith("?»") ? text : text + "?";
    }

    /** What a statement implies about the world if the given character is the liar. */
    public String statementReading(HexatrapFormula formula, String speaker, String liar) {
        HexatrapFormula content = speaker.equals(liar) ? new Not(formula) : formula;
        return reading(content, liar);
    }

    /** What an answer implies about the world if the given character is the liar. */
    public String answerReading(HexatrapFormula formula, String asked, boolean answer, String liar) {
        boolean truthful = !asked.equals(liar);
        HexatrapFormula content = answer == truthful ? formula : new Not(formula);
        return reading(content, liar);
    }

    private String reading(HexatrapFormula content, String liar) {
        HexatrapFormula simplified = simplify(content, liar);
        if (simplified instanceof Const c) return c.value() ? "nessuna informazione sui fatti" : "contraddizione: questa ipotesi è impossibile";
        return capitalize(formal(simplified));
    }

    /** Removes TRUTHFUL and SAYS for a fixed liar, folds constants and pushes negations to the atoms. */
    public HexatrapFormula simplify(HexatrapFormula formula, String liar) {
        if (formula instanceof Truthful t) return new Const(!t.character().equals(liar));
        if (formula instanceof Says s) {
            HexatrapFormula inner = simplify(s.operand(), liar);
            return s.character().equals(liar) ? negate(inner) : inner;
        }
        if (formula instanceof Not n) return negate(simplify(n.operand(), liar));
        if (formula instanceof And a) return and(a.operands().stream().map(f -> simplify(f, liar)).toList());
        if (formula instanceof Or o) return or(o.operands().stream().map(f -> simplify(f, liar)).toList());
        if (formula instanceof Xor x) return xor(simplify(x.left(), liar), simplify(x.right(), liar));
        if (formula instanceof Iff i) return negate(xor(simplify(i.left(), liar), simplify(i.right(), liar)));
        if (formula instanceof Implies i) {
            HexatrapFormula premise = simplify(i.premise(), liar);
            HexatrapFormula conclusion = simplify(i.conclusion(), liar);
            if (premise instanceof Const c) return c.value() ? conclusion : new Const(true);
            if (conclusion instanceof Const c) return c.value() ? new Const(true) : negate(premise);
            return new Implies(premise, conclusion);
        }
        if (formula instanceof Count c) {
            List<HexatrapFormula> operands = new ArrayList<>();
            int k = c.k();
            for (HexatrapFormula operand : c.operands()) {
                HexatrapFormula simple = simplify(operand, liar);
                if (simple instanceof Const constant) { if (constant.value()) k--; }
                else operands.add(simple);
            }
            if (k < 0 || k > operands.size()) return new Const(false);
            if (operands.isEmpty()) return new Const(true);
            if (k == 0) return and(operands.stream().map(this::negate).toList());
            if (k == operands.size()) return and(operands);
            if (operands.size() == 1) return operands.get(0);
            return new Count(k, List.copyOf(operands));
        }
        return formula;
    }

    private HexatrapFormula negate(HexatrapFormula formula) {
        if (formula instanceof Const c) return new Const(!c.value());
        if (formula instanceof FactIs f) return new FactIs(f.fact(), f.option(), !f.negated());
        if (formula instanceof Not n) return n.operand();
        if (formula instanceof And a) return or(a.operands().stream().map(this::negate).toList());
        if (formula instanceof Or o) return and(o.operands().stream().map(this::negate).toList());
        if (formula instanceof Xor x) return new Iff(x.left(), x.right());
        if (formula instanceof Iff i) return new Xor(i.left(), i.right());
        if (formula instanceof Implies i) return and(List.of(i.premise(), negate(i.conclusion())));
        return new Not(formula);
    }

    private HexatrapFormula and(List<HexatrapFormula> operands) {
        List<HexatrapFormula> kept = new ArrayList<>();
        for (HexatrapFormula operand : operands) {
            if (operand instanceof Const c) { if (!c.value()) return c; continue; }
            if (operand instanceof And nested) kept.addAll(nested.operands());
            else kept.add(operand);
        }
        if (kept.isEmpty()) return new Const(true);
        return kept.size() == 1 ? kept.get(0) : new And(List.copyOf(kept));
    }

    private HexatrapFormula or(List<HexatrapFormula> operands) {
        List<HexatrapFormula> kept = new ArrayList<>();
        for (HexatrapFormula operand : operands) {
            if (operand instanceof Const c) { if (c.value()) return c; continue; }
            if (operand instanceof Or nested) kept.addAll(nested.operands());
            else kept.add(operand);
        }
        if (kept.isEmpty()) return new Const(false);
        return kept.size() == 1 ? kept.get(0) : new Or(List.copyOf(kept));
    }

    private HexatrapFormula xor(HexatrapFormula left, HexatrapFormula right) {
        if (left instanceof Const c) return c.value() ? negate(right) : right;
        if (right instanceof Const c) return c.value() ? negate(left) : left;
        return new Xor(left, right);
    }

    private String count(Count c, boolean negated) {
        String items = c.operands().stream().map(this::formal).collect(Collectors.joining("; "));
        String quantity = c.k() == 0 ? "nessuna" : c.k() == 1 ? "esattamente una" : "esattamente " + c.k();
        return (negated ? "non è vero che " + quantity : capitalizeIfNeeded(quantity, false)) + " di queste è vera: " + items;
    }

    private String group(HexatrapFormula formula) {
        boolean simple = formula instanceof FactIs || formula instanceof Const || formula instanceof Truthful
            || formula instanceof Says || (formula instanceof Not n && (n.operand() instanceof FactIs || n.operand() instanceof Truthful));
        return simple ? formal(formula) : "(" + formal(formula) + ")";
    }

    private String atom(String factId, String optionId, boolean negated) {
        Fact fact = definition.fact(factId);
        Option option = fact == null ? null : fact.options().stream().filter(o -> o.id().equals(optionId)).findFirst().orElse(null);
        if (option == null) return factId + (negated ? " ≠ " : " = ") + optionId;
        String phrase = negated ? option.deny() : option.affirm();
        return phrase == null ? option.label() : stripFinalPeriod(phrase.trim());
    }

    private String name(String characterId) {
        Character character = definition.character(characterId);
        return character == null || character.name() == null ? characterId : character.name();
    }

    private static String join(List<String> parts, String last) {
        if (parts.size() <= 1) return String.join("", parts);
        return String.join(", ", parts.subList(0, parts.size() - 1)) + last + parts.get(parts.size() - 1);
    }

    private static String stripFinalPeriod(String text) {
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }

    private static String capitalizeIfNeeded(String text, boolean capitalize) {
        return capitalize ? capitalize(text) : text;
    }

    public static String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        int first = text.codePointAt(0);
        return new String(java.lang.Character.toChars(java.lang.Character.toUpperCase(first))) + text.substring(java.lang.Character.charCount(first));
    }
}
