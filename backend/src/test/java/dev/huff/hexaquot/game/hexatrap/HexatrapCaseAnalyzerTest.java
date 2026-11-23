package dev.huff.hexaquot.game.hexatrap;

import dev.huff.hexaquot.game.hexatrap.HexatrapCaseAnalyzer.ActionClass;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HexatrapCaseAnalyzerTest {
    private final HexatrapCase.CaseDefinition definition = HexatrapFixtures.lanterna();
    private final HexatrapCaseAnalyzer analyzer = new HexatrapCaseAnalyzer(definition,
        HexatrapCaseValidator.validate(definition, 1, 7L).formulas());

    @Test
    void startsFromCertaintiesAndStatements() {
        assertEquals(108, analyzer.space().worlds());
        assertEquals(9, analyzer.initial().cardinality());
        assertEquals(2, analyzer.goal().cardinality());
        assertTrue(analyzer.initial().get(analyzer.truth()));
        assertFalse(analyzer.liarDetermined(analyzer.initial()));
        assertFalse(analyzer.determined(List.of()));
        assertTrue(analyzer.determined(analyzer.actions().keySet()));
    }

    @Test
    void answersAndClassifiesEveryAction() {
        var actions = analyzer.actions();
        assertTrue(actions.get("Q:q_ettore_stufa").answer());
        assertFalse(actions.get("Q:q_ettore_undici").answer());
        assertFalse(actions.get("Q:q_livia_dieci").answer());
        assertEquals(ActionClass.TRAP, actions.get("Q:q_ettore_sincero").classification());
        assertEquals(ActionClass.ORACLE, actions.get("Q:q_livia_cassa").classification());
        assertEquals(ActionClass.NORMAL, actions.get("C:c_impronte").classification());
        assertEquals(HexatrapRules.QUESTION_COST, actions.get("Q:q_ettore_stufa").cost());
        assertEquals(16, actions.get("C:c_maree").cost());
    }

    @Test
    void findsTheCheapestCertificateExhaustively() {
        var optimal = analyzer.optimalPath();
        assertTrue(optimal.found());
        assertEquals(10, optimal.cost());
        assertEquals(List.of("Q:q_ettore_stufa", "Q:q_ettore_undici"), optimal.keys());
        assertTrue(analyzer.determined(optimal.keys()));

        List<String> keys = new ArrayList<>(analyzer.actions().keySet());
        int best = Integer.MAX_VALUE;
        for (int mask = 0; mask < (1 << keys.size()); mask++) {
            List<String> subset = new ArrayList<>();
            int cost = 0;
            for (int i = 0; i < keys.size(); i++) if ((mask & (1 << i)) != 0) {
                subset.add(keys.get(i));
                cost += analyzer.actions().get(keys.get(i)).cost();
            }
            if (analyzer.determined(subset)) best = Math.min(best, cost);
        }
        assertEquals(best, optimal.cost());
        assertEquals(2, analyzer.fewestActions().cost());
    }

    @Test
    void provesTheLiarFromThePlayersOwnEvidence() {
        var proof = analyzer.proofOfLiar(List.of("k_chiave", "k_cane", "s_ettore_nove", "s_ettore_mozzo", "s_livia_cane",
            "s_livia_ettore", "q_ettore_undici"));
        assertTrue(proof.found());
        assertTrue(proof.keys().contains("q_ettore_undici"));
        assertTrue(proof.cost() <= 3, proof.toString());

        var none = analyzer.proofOfLiar(List.of("k_chiave", "k_cane", "s_ettore_nove", "s_ettore_mozzo", "s_livia_cane", "s_livia_ettore"));
        assertFalse(none.found());
    }

    @Test
    void simulatesExpertsAndNaivePlayersDeterministically() {
        assertEquals(List.of("Q:q_ettore_stufa", "Q:q_ettore_undici"), analyzer.expertPath());
        assertEquals(analyzer.naiveMedian(99L), analyzer.naiveMedian(99L));
        var metrics = analyzer.metrics(5L);
        assertEquals(78, metrics.totalCost());
        assertEquals(1, metrics.traps());
        assertEquals(1, metrics.oracles());
        assertEquals(18, metrics.guessOdds());
        assertEquals(0.333, metrics.roleBalance());
        assertTrue(metrics.naiveMedian() >= metrics.certificateCost());
    }

    @Test
    void judgesVerdicts() {
        assertTrue(analyzer.liarCorrect("B"));
        assertFalse(analyzer.liarCorrect("A"));
        assertTrue(analyzer.factsCorrect(java.util.Map.of("chi", "mozzo", "diario", "stufa")));
        assertFalse(analyzer.factsCorrect(java.util.Map.of("chi", "mozzo", "diario", "cassa")));
        assertFalse(analyzer.factsCorrect(java.util.Map.of("chi", "mozzo")));
    }
}
