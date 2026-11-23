package dev.huff.hexaquot.game.hexatrap;

import dev.huff.hexaquot.game.hexatrap.HexatrapCase.Fact;
import dev.huff.hexaquot.game.hexatrap.HexatrapCase.Option;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The classic knights-and-knaves identities follow from the semantics of the case language. */
class HexatrapLogicTest {
    private final HexatrapHypothesisSpace space = new HexatrapHypothesisSpace(List.of(
        new Fact("porta", "Porta", List.of(new Option("chiusa", "Chiusa", "chiusa", "non chiusa"), new Option("aperta", "Aperta", "aperta", "non aperta"))),
        new Fact("ora", "Ora", List.of(new Option("nove", "9", "9", "non 9"), new Option("dieci", "10", "10", "non 10"), new Option("undici", "11", "11", "non 11")))));

    private static HexatrapFormula parse(String source) throws Exception {
        return HexatrapFormulaParser.parse(source);
    }

    @Test
    void encodesWorldsAndLiars() {
        assertEquals(6, space.worlds());
        assertEquals(12, space.size());
        int world = space.worldOf(Map.of("porta", "aperta", "ora", "undici"));
        assertEquals("aperta", space.optionId(world, "porta"));
        assertEquals("undici", space.optionId(world, "ora"));
        assertEquals("B", HexatrapHypothesisSpace.liar(HexatrapHypothesisSpace.hypothesis(world, "B")));
        assertEquals(-1, space.worldOf(Map.of("porta", "socchiusa", "ora", "nove")));
    }

    @Test
    void askingTheOtherWhatTheyWouldSayAlwaysYieldsTheOpposite() throws Exception {
        HexatrapFormula fact = parse("porta=chiusa");
        for (int h = 0; h < space.size(); h++) {
            assertEquals(!space.holds(fact, h), space.answer(parse("SAYS(B, porta=chiusa)"), "A", h));
            assertEquals(!space.holds(fact, h), space.answer(parse("SAYS(A, porta=chiusa)"), "B", h));
            assertEquals(space.holds(fact, h), space.answer(parse("SAYS(A, porta=chiusa)"), "A", h));
        }
    }

    @Test
    void askingWhoIsTruthfulIsATrap() throws Exception {
        for (int h = 0; h < space.size(); h++) {
            assertTrue(space.answer(parse("TRUTHFUL(A)"), "A", h));
            assertTrue(space.answer(parse("TRUTHFUL(B)"), "B", h));
            assertFalse(space.answer(parse("TRUTHFUL(B)"), "A", h));
        }
    }

    @Test
    void theTwoCharactersAlwaysGiveOppositeAnswers() throws Exception {
        for (String source : List.of("porta=chiusa", "ora!=nove OR porta=aperta", "TRUTHFUL(A)", "SAYS(A, ora=dieci)")) {
            HexatrapFormula formula = parse(source);
            for (int h = 0; h < space.size(); h++) assertNotEquals(space.answer(formula, "A", h), space.answer(formula, "B", h), source);
            // Hence asking either character carries the same information.
            var yesFromA = space.answered(formula, "A", true);
            var noFromB = space.answered(formula, "B", false);
            assertEquals(yesFromA, noFromB, source);
        }
    }

    @Test
    void statementsAreTrueForTheTruthfulAndFalseForTheLiar() throws Exception {
        HexatrapFormula formula = parse("porta=aperta");
        var consistent = space.asserted(formula, "A");
        for (int h = 0; h < space.size(); h++) {
            boolean truthful = HexatrapHypothesisSpace.liar(h).equals("B");
            assertEquals(space.holds(formula, h) == truthful, consistent.get(h));
        }
    }

    @Test
    void countsExactlyKTrueOperands() throws Exception {
        HexatrapFormula formula = parse("COUNT(1, porta=aperta, ora=nove)");
        int both = HexatrapHypothesisSpace.hypothesis(space.worldOf(Map.of("porta", "aperta", "ora", "nove")), "A");
        int one = HexatrapHypothesisSpace.hypothesis(space.worldOf(Map.of("porta", "aperta", "ora", "dieci")), "A");
        assertFalse(space.holds(formula, both));
        assertTrue(space.holds(formula, one));
    }
}
