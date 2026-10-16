package dev.huff.hexaquot.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.game.HexatrapDtos.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HexatrapRulesTest {
    @Test
    void everyTemplatePairConfigurationAndRoleIsConsistentAndSolvableInThreeToFiveQuestions() {
        for (int count : List.of(2, 4)) for (Relation relation : Relation.values())
            for (int first = 0; first < 6; first++) for (int second = 0; second < 6; second++) if (first != second) {
                var dossier = new Dossier(count, relation, first, second);
                var initial = HexatrapRules.hypotheses(dossier);
                assertFalse(initial.isEmpty());
                assertFalse(HexatrapRules.unlocked(initial));
                for (int seal = 0; seal < 6; seal++) {
                    final int s = seal;
                    assertEquals(2, initial.stream().map(h -> HexatrapRules.lit(h / 2, s)).distinct().count());
                }
                Map<List<Integer>, Integer> cache = new HashMap<>();
                int depth = optimalDepth(initial, cache);
                assertTrue(depth >= 3 && depth <= 5, dossier + ": " + depth);
                for (int actual : initial) {
                    assertTrue(HexatrapRules.allows(dossier, actual / 2));
                    for (int seal = 0; seal < 6; seal++) for (int advisor = 0; advisor < 2; advisor++)
                        assertEquals(HexatrapRules.lit(actual / 2, seal), HexatrapRules.answer(actual, advisor, seal) == (actual % 2 == advisor));
                    var remaining = initial;
                    int used = 0;
                    while (!HexatrapRules.unlocked(remaining)) {
                        int bestSeal = -1, best = 99;
                        for (int seal = 0; seal < 6; seal++) {
                            var yes = HexatrapRules.retain(remaining, 0, seal, true);
                            var no = HexatrapRules.retain(remaining, 0, seal, false);
                            if (yes.isEmpty() || no.isEmpty()) continue;
                            int cost = 1 + Math.max(optimalDepth(yes, cache), optimalDepth(no, cache));
                            if (cost < best) { best = cost; bestSeal = seal; }
                        }
                        assertTrue(bestSeal >= 0);
                        remaining = HexatrapRules.retain(remaining, 0, bestSeal, HexatrapRules.answer(actual, 0, bestSeal));
                        assertTrue(remaining.contains(actual));
                        assertTrue(++used <= 5);
                    }
                    assertEquals(actual % 2, remaining.get(0) % 2);
                }
            }
    }
    private int optimalDepth(List<Integer> candidates, Map<List<Integer>, Integer> cache) {
        if (HexatrapRules.unlocked(candidates)) return 0;
        if (cache.containsKey(candidates)) return cache.get(candidates);
        int best = 99;
        // Asking B produces the complementary partition, so A's six questions cover all strategies.
        for (int seal = 0; seal < 6; seal++) {
            var yes = HexatrapRules.retain(candidates, 0, seal, true);
            var no = HexatrapRules.retain(candidates, 0, seal, false);
            if (yes.isEmpty() || no.isEmpty()) continue;
            best = Math.min(best, 1 + Math.max(optimalDepth(yes, cache), optimalDepth(no, cache)));
        }
        cache.put(candidates, best);
        return best;
    }
    @Test
    void javaAndTypescriptShareDeductionVectors() throws Exception {
        var mapper = new ObjectMapper();
        var cases = mapper.readTree(getClass().getResourceAsStream("/hexatrap-v1-cases.json"));
        assertEquals(12, cases.size());
        for (var example : cases) {
            var dossier = mapper.treeToValue(example.get("dossier"), Dossier.class);
            var remaining = HexatrapRules.hypotheses(dossier);
            for (var step : example.get("steps")) {
                remaining = HexatrapRules.retain(remaining, step.get("advisor").asInt(), step.get("seal").asInt(), step.get("answer").asBoolean());
                assertEquals(step.get("remaining"), mapper.valueToTree(remaining));
                assertEquals(step.get("unlocked").asBoolean(), HexatrapRules.unlocked(remaining));
            }
        }
        assertFalse(HexatrapRules.unlocked(List.of()));
        assertEquals(-600, HexatrapRules.points(12, 2));
    }
    @Test
    void dailyPuzzlesAreStableIndependentAndFalseNumberAlwaysDiffers() {
        var provider = new HexatrapDailyGameProvider(); provider.seed = "test-seed";
        Set<Dossier> dossiers = new HashSet<>(); Set<Integer> roles = new HashSet<>(); Set<Integer> numbers = new HashSet<>();
        for (int day = 0; day < 2000; day++) {
            String date = java.time.LocalDate.of(2026,1,1).plusDays(day).toString();
            var puzzle = provider.puzzleFor(date);
            assertEquals(puzzle, provider.puzzleFor(date));
            assertTrue(HexatrapRules.allows(puzzle.dossier(), puzzle.configuration()));
            assertTrue(puzzle.secretNumber() >= 1 && puzzle.secretNumber() <= 9864);
            assertTrue(puzzle.falseNumber() >= 1 && puzzle.falseNumber() <= 9864);
            assertNotEquals(puzzle.secretNumber(), puzzle.falseNumber());
            dossiers.add(puzzle.dossier()); roles.add(puzzle.loyalAdvisor()); numbers.add(puzzle.secretNumber());
        }
        assertTrue(dossiers.size() > 100); assertEquals(2, roles.size()); assertTrue(numbers.size() > 1000);
    }
}
