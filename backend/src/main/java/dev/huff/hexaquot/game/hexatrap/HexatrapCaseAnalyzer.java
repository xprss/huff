package dev.huff.hexaquot.game.hexatrap;

import dev.huff.hexaquot.game.hexatrap.HexatrapCase.CaseDefinition;
import dev.huff.hexaquot.game.hexatrap.HexatrapCase.Item;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Exhaustive analysis of a structurally valid case. Knowledge is a set of hypotheses; every
 * statement, certainty and investigation action is a filter over it. The player's goal is
 * {@code good}: the true liar together with the true values of the verdict facts.
 */
public final class HexatrapCaseAnalyzer {
    public enum ActionKind { QUESTION, CLUE }

    /** TRAP actions carry no information at the start; ORACLE answers do not depend on who lies. */
    public enum ActionClass { NORMAL, TRAP, ORACLE }

    public record Action(String key, ActionKind kind, String itemId, String character, int cost,
                         boolean answer, ActionClass classification, BitSet filter, BitSet yes) {}

    /** A set of known items and its cost; {@code keys} is empty when no subset reaches the goal. */
    public record Certificate(List<String> keys, int cost, boolean found) {}

    public record Metrics(int worlds, int h0Size, double roleBalance, int totalCost, int certificateCost,
                          List<String> certificate, int minimumActions, int expertPar, List<String> expertPath,
                          int naiveMedian, int traps, int oracles, int guessOdds) {}

    private static final int SIMULATIONS = 200;

    private final CaseDefinition definition;
    private final Map<String, HexatrapFormula> formulas;
    private final HexatrapHypothesisSpace space;
    private final BitSet allowed;
    private final BitSet h0;
    private final BitSet good;
    private final BitSet trueLiar;
    private final int truth;
    private final Map<String, BitSet> itemSets = new LinkedHashMap<>();
    private final Map<String, Action> actions = new LinkedHashMap<>();

    public HexatrapCaseAnalyzer(CaseDefinition definition, Map<String, HexatrapFormula> formulas) {
        this.definition = definition;
        this.formulas = Map.copyOf(formulas);
        this.space = new HexatrapHypothesisSpace(definition.facts());

        allowed = space.all();
        for (Item certainty : definition.certainties()) {
            BitSet set = space.eval(formula(certainty));
            itemSets.put(certainty.id(), set);
            allowed.and(set);
        }
        h0 = (BitSet) allowed.clone();
        for (Item statement : definition.statements()) {
            BitSet set = space.asserted(formula(statement), statement.speaker());
            itemSets.put(statement.id(), set);
            h0.and(set);
        }

        int world = space.worldOf(definition.solution().facts());
        truth = world < 0 ? -1 : HexatrapHypothesisSpace.hypothesis(world, definition.solution().liar());

        trueLiar = new BitSet(space.size());
        good = new BitSet(space.size());
        for (int h = 0; h < space.size(); h++) {
            if (truth < 0 || !HexatrapHypothesisSpace.liar(h).equals(definition.solution().liar())) continue;
            trueLiar.set(h);
            boolean sameFacts = true;
            for (String fact : definition.verdictFacts()) {
                if (!space.optionId(HexatrapHypothesisSpace.world(h), fact).equals(definition.solution().facts().get(fact))) {
                    sameFacts = false;
                    break;
                }
            }
            if (sameFacts && h0.get(h)) good.set(h);
        }

        for (Item question : definition.questions()) {
            HexatrapFormula formula = formula(question);
            boolean answer = truth >= 0 && space.answer(formula, question.to(), truth);
            BitSet yes = space.answered(formula, question.to(), true);
            BitSet filter = answer ? yes : complement(yes);
            Action action = new Action("Q:" + question.id(), ActionKind.QUESTION, question.id(), question.to(),
                HexatrapRules.QUESTION_COST, answer, classifyQuestion(formula, question.to(), yes), filter, yes);
            actions.put(action.key(), action);
            itemSets.put(question.id(), filter);
        }
        for (Item clue : definition.clues()) {
            BitSet filter = space.eval(formula(clue));
            BitSet known = (BitSet) h0.clone();
            known.andNot(filter);
            ActionClass classification = known.isEmpty() ? ActionClass.TRAP : ActionClass.NORMAL;
            Action action = new Action("C:" + clue.id(), ActionKind.CLUE, clue.id(), null,
                clue.cost() == null ? 0 : clue.cost(), true, classification, filter, filter);
            actions.put(action.key(), action);
            itemSets.put(clue.id(), filter);
        }
    }

    private HexatrapFormula formula(Item item) {
        HexatrapFormula formula = formulas.get(item.id());
        if (formula == null) throw new IllegalArgumentException("Formula mancante per " + item.id());
        return formula;
    }

    private BitSet complement(BitSet set) {
        BitSet result = space.all();
        result.andNot(set);
        return result;
    }

    private ActionClass classifyQuestion(HexatrapFormula formula, String character, BitSet yes) {
        BitSet yesInH0 = (BitSet) yes.clone();
        yesInH0.and(h0);
        if (yesInH0.isEmpty() || yesInH0.equals(h0)) return ActionClass.TRAP;
        for (int world = 0; world < space.worlds(); world++) {
            int a = HexatrapHypothesisSpace.hypothesis(world, "A");
            int b = HexatrapHypothesisSpace.hypothesis(world, "B");
            if (!allowed.get(a) && !allowed.get(b)) continue;
            if (space.answer(formula, character, a) != space.answer(formula, character, b)) return ActionClass.NORMAL;
        }
        return ActionClass.ORACLE;
    }

    public HexatrapHypothesisSpace space() { return space; }
    public BitSet allowed() { return (BitSet) allowed.clone(); }
    public BitSet initial() { return (BitSet) h0.clone(); }
    public BitSet goal() { return (BitSet) good.clone(); }
    public int truth() { return truth; }
    public Map<String, Action> actions() { return Collections.unmodifiableMap(actions); }
    public BitSet itemSet(String itemId) { return itemSets.get(itemId); }

    public static String questionKey(String questionId) { return "Q:" + questionId; }
    public static String clueKey(String clueId) { return "C:" + clueId; }

    /** What the player knows after the given actions, starting from certainties and statements. */
    public BitSet knowledge(Collection<String> actionKeys) {
        BitSet known = (BitSet) h0.clone();
        for (String key : actionKeys) {
            Action action = actions.get(key);
            if (action != null) known.and(action.filter());
        }
        return known;
    }

    public boolean determined(Collection<String> actionKeys) {
        return subset(knowledge(actionKeys), good);
    }

    /** Whether the given knowledge leaves only one possible liar. */
    public boolean liarDetermined(BitSet known) {
        return !known.isEmpty() && (subset(known, trueLiar) || !known.intersects(trueLiar));
    }

    private static boolean subset(BitSet set, BitSet of) {
        BitSet rest = (BitSet) set.clone();
        rest.andNot(of);
        return rest.isEmpty();
    }

    /** The cheapest set of investigation actions whose actual answers determine the verdict. */
    public Certificate optimalPath() {
        List<String> keys = new ArrayList<>(actions.keySet());
        List<BitSet> sets = keys.stream().map(k -> actions.get(k).filter()).toList();
        int[] costs = keys.stream().mapToInt(k -> actions.get(k).cost()).toArray();
        return smallest(keys, sets, costs, h0, good);
    }

    /** The fewest investigation actions that determine the verdict. */
    public Certificate fewestActions() {
        List<String> keys = new ArrayList<>(actions.keySet());
        List<BitSet> sets = keys.stream().map(k -> actions.get(k).filter()).toList();
        int[] costs = new int[keys.size()];
        java.util.Arrays.fill(costs, 1);
        return smallest(keys, sets, costs, h0, good);
    }

    /**
     * The smallest subset of what the player knows (certainties, statements, answers and
     * revealed clues, by item id) that rules out the wrong liar. Not found when the collected
     * evidence did not determine the liar.
     */
    public Certificate proofOfLiar(Collection<String> knownItemIds) {
        List<String> keys = new ArrayList<>(knownItemIds);
        List<BitSet> sets = keys.stream().map(itemSets::get).toList();
        int[] costs = new int[keys.size()];
        java.util.Arrays.fill(costs, 1);
        return smallest(keys, sets, costs, space.all(), trueLiar);
    }

    private Certificate smallest(List<String> keys, List<BitSet> sets, int[] costs, BitSet base, BitSet target) {
        BitSet everything = (BitSet) base.clone();
        sets.forEach(everything::and);
        if (!subset(everything, target)) return new Certificate(List.of(), 0, false);
        Search search = new Search(sets, costs, target);
        greedy(sets, costs, base, target, search);
        search.run(0, base, 0, new ArrayList<>());
        List<String> chosen = search.best.stream().map(keys::get).toList();
        return new Certificate(chosen, search.bestCost, true);
    }

    /** Upper bound for the exact search: repeatedly take the set that removes most wrong hypotheses per cost. */
    private static void greedy(List<BitSet> sets, int[] costs, BitSet base, BitSet target, Search search) {
        BitSet known = (BitSet) base.clone();
        List<Integer> chosen = new ArrayList<>();
        int cost = 0;
        while (!subset(known, target)) {
            BitSet wrong = (BitSet) known.clone();
            wrong.andNot(target);
            int bestIndex = -1;
            double bestScore = 0;
            for (int i = 0; i < sets.size(); i++) {
                if (chosen.contains(i)) continue;
                BitSet removed = (BitSet) wrong.clone();
                removed.andNot(sets.get(i));
                double score = (double) removed.cardinality() / Math.max(1, costs[i]);
                if (score > bestScore) { bestScore = score; bestIndex = i; }
            }
            if (bestIndex < 0) return;
            chosen.add(bestIndex);
            cost += costs[bestIndex];
            known.and(sets.get(bestIndex));
        }
        search.bestCost = cost;
        search.best = List.copyOf(chosen);
    }

    private static final class Search {
        static final int MAX_NODES = 2_000_000;
        final List<BitSet> sets;
        final int[] costs;
        final BitSet target;
        int bestCost = Integer.MAX_VALUE;
        List<Integer> best = List.of();
        int nodes;

        Search(List<BitSet> sets, int[] costs, BitSet target) {
            this.sets = sets;
            this.costs = costs;
            this.target = target;
        }

        void run(int index, BitSet known, int cost, List<Integer> chosen) {
            if (cost >= bestCost || ++nodes > MAX_NODES) return;
            if (subset(known, target)) {
                bestCost = cost;
                best = List.copyOf(chosen);
                return;
            }
            if (index >= sets.size()) return;
            BitSet narrowed = (BitSet) known.clone();
            narrowed.and(sets.get(index));
            if (!narrowed.equals(known)) {
                chosen.add(index);
                run(index + 1, narrowed, cost + costs[index], chosen);
                chosen.remove(chosen.size() - 1);
            }
            run(index + 1, known, cost, chosen);
        }
    }

    /**
     * An expert's greedy investigation: each step picks the action with the best expected
     * reduction of the remaining hypotheses per minute, then applies its actual result.
     * Returns the path; its cost is the par shown after the game.
     */
    public List<String> expertPath() {
        BitSet known = (BitSet) h0.clone();
        List<String> path = new ArrayList<>();
        while (!subset(known, good)) {
            String bestKey = null;
            double bestScore = 0;
            int size = known.cardinality();
            for (Action action : actions.values()) {
                if (path.contains(action.key())) continue;
                BitSet yes = (BitSet) known.clone();
                yes.and(action.yes());
                int y = yes.cardinality();
                int n = size - y;
                double expected = ((double) y * y + (double) n * n) / size;
                double score = (size - expected) / Math.max(1, action.cost());
                if (score > bestScore + 1e-9) { bestScore = score; bestKey = action.key(); }
            }
            if (bestKey == null) return null;
            path.add(bestKey);
            known.and(actions.get(bestKey).filter());
        }
        return path;
    }

    /** Median cost of investigating in a random order until the verdict is certain. */
    public int naiveMedian(long seed) {
        List<Action> list = new ArrayList<>(actions.values());
        SplittableRandom random = new SplittableRandom(seed);
        int[] costs = new int[SIMULATIONS];
        for (int run = 0; run < SIMULATIONS; run++) {
            List<Action> order = new ArrayList<>(list);
            for (int i = order.size() - 1; i > 0; i--) Collections.swap(order, i, random.nextInt(i + 1));
            BitSet known = (BitSet) h0.clone();
            int cost = 0;
            for (Action action : order) {
                if (subset(known, good)) break;
                known.and(action.filter());
                cost += action.cost();
            }
            costs[run] = cost;
        }
        java.util.Arrays.sort(costs);
        return costs[SIMULATIONS / 2];
    }

    public Metrics metrics(long seed) {
        int h0Size = h0.cardinality();
        BitSet aLies = new BitSet(space.size());
        for (int h = h0.nextSetBit(0); h >= 0; h = h0.nextSetBit(h + 1)) if ((h & 1) == 0) aLies.set(h);
        double balance = h0Size == 0 ? 0 : (double) aLies.cardinality() / h0Size;
        int totalCost = actions.values().stream().mapToInt(Action::cost).sum();
        Certificate optimal = optimalPath();
        Certificate fewest = fewestActions();
        List<String> expert = expertPath();
        int expertCost = expert == null ? -1 : expert.stream().mapToInt(k -> actions.get(k).cost()).sum();
        int traps = (int) actions.values().stream().filter(a -> a.classification() == ActionClass.TRAP).count();
        int oracles = (int) actions.values().stream().filter(a -> a.classification() == ActionClass.ORACLE).count();
        int guessOdds = 2;
        for (String fact : definition.verdictFacts()) guessOdds *= definition.fact(fact).options().size();
        return new Metrics(space.worlds(), h0Size, Math.round(balance * 1000) / 1000.0, totalCost,
            optimal.found() ? optimal.cost() : -1, optimal.keys(), fewest.found() ? fewest.cost() : -1,
            expertCost, expert == null ? List.of() : expert, optimal.found() ? naiveMedian(seed) : -1,
            traps, oracles, guessOdds);
    }

    /** Whether the verdict names the true liar and the true values of every verdict fact. */
    public boolean liarCorrect(String liar) {
        return definition.solution().liar().equals(liar);
    }

    public boolean factsCorrect(Map<String, String> facts) {
        for (String fact : definition.verdictFacts()) {
            if (facts == null || !definition.solution().facts().get(fact).equals(facts.get(fact))) return false;
        }
        return true;
    }
}
