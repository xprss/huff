package dev.huff.hexaquot.game.hexatrap;

import dev.huff.hexaquot.game.hexatrap.HexatrapFormula.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HexatrapFormulaParserTest {
    private static HexatrapFormula parse(String source) throws Exception {
        return HexatrapFormulaParser.parse(source);
    }

    private static FactIs atom(String fact, String option) {
        return new FactIs(fact, option, false);
    }

    @Test
    void appliesPrecedenceFromNotToIff() throws Exception {
        assertEquals(new Or(List.of(atom("a", "x"), new And(List.of(atom("b", "y"), new Not(atom("c", "z")))))),
            parse("a=x OR b=y AND NOT c=z"));
        assertEquals(new Or(List.of(new Xor(atom("a", "x"), atom("b", "y")), atom("c", "z"))), parse("a=x XOR b=y OR c=z"));
        assertEquals(new Iff(new Implies(atom("a", "x"), atom("b", "y")), atom("c", "z")), parse("a=x -> b=y <-> c=z"));
        assertEquals(new Implies(atom("a", "x"), new Implies(atom("b", "y"), atom("c", "z"))), parse("a=x -> b=y -> c=z"));
        assertEquals(new And(List.of(new Or(List.of(atom("a", "x"), atom("b", "y"))), atom("c", "z"))), parse("(a=x OR b=y) AND c=z"));
    }

    @Test
    void parsesCharactersCountsAndAliases() throws Exception {
        assertEquals(new Says("B", new Not(new Truthful("A"))), parse("SAYS(B, NOT TRUTHFUL(A))"));
        assertEquals(new Count(1, List.of(atom("a", "x"), new FactIs("b", "y", true), atom("c", "z"))), parse("COUNT(1, a=x, b!=y, c=z)"));
        assertEquals(parse("NOT a=x AND b=y OR c=z"), parse("!a=x && b=y || c=z"));
        assertEquals(new Const(true), parse(" TRUE "));
    }

    @Test
    void measuresFormulas() throws Exception {
        HexatrapFormula formula = parse("SAYS(A, a=x AND b=y) -> NOT c=z");
        assertEquals(7, formula.size());
        assertEquals(4, formula.depth());
        assertEquals(1, formula.saysDepth());
        assertFalse(formula.factsOnly());
        assertTrue(formula.mentionsSays("A"));
        assertFalse(formula.mentionsSays("B"));
        assertTrue(parse("a=x XOR b=y").factsOnly());
    }

    @Test
    void reportsItalianErrorsWithColumns() {
        assertError("a=x AND", 8, "Atteso un fatto");
        assertError("a=x)", 4, "Simbolo inatteso");
        assertError("Chi=x", 1, "Identificatore non valido");
        assertError("a x", 3, "serve = oppure !=");
        assertError("COUNT(3, a=x, b=y)", 7, "COUNT");
        assertError("SAYS(C, a=x)", 6, "Identificatore non valido");
        assertError("a=x # b=y", 5, "Carattere non valido");
        assertError("", 1, "vuota");
    }

    private static void assertError(String source, int column, String fragment) {
        var error = assertThrows(HexatrapFormulaParser.ParseException.class, () -> HexatrapFormulaParser.parse(source));
        assertEquals(column, error.column(), error.getMessage());
        assertTrue(error.getMessage().contains(fragment), error.getMessage());
    }
}
