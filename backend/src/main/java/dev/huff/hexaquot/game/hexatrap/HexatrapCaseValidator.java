package dev.huff.hexaquot.game.hexatrap;

import dev.huff.hexaquot.game.hexatrap.HexatrapCase.CaseDefinition;
import dev.huff.hexaquot.game.hexatrap.HexatrapCase.Character;
import dev.huff.hexaquot.game.hexatrap.HexatrapCase.Fact;
import dev.huff.hexaquot.game.hexatrap.HexatrapCase.Item;
import dev.huff.hexaquot.game.hexatrap.HexatrapCase.Option;
import dev.huff.hexaquot.game.hexatrap.HexatrapCaseAnalyzer.Action;
import dev.huff.hexaquot.game.hexatrap.HexatrapCaseAnalyzer.ActionClass;
import dev.huff.hexaquot.game.hexatrap.HexatrapCaseAnalyzer.Metrics;
import dev.huff.hexaquot.game.hexatrap.HexatrapFormula.FactIs;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Authoritative validation of an authored case. Hard issues reject the case; soft issues are
 * difficulty preferences that the generator tolerates on its last attempt. Every message is
 * Italian and specific, because it becomes feedback for the next authoring attempt.
 */
public final class HexatrapCaseValidator {
    public static final Set<String> ICONS = Set.of(
        "anchor", "feather", "crown", "key", "compass", "lamp", "book", "hammer", "palette", "music", "flask",
        "ship", "train", "tree", "camera", "scroll", "shield", "star", "moon", "sun", "glasses", "briefcase",
        "bike", "chef", "telescope", "wrench", "stethoscope", "gem", "map", "clock");

    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_]{0,23}");
    private static final int MAX_FORMULA_SIZE = 16;
    private static final int MAX_FORMULA_DEPTH = 4;

    public record Issue(String code, String itemId, String message, boolean soft) {}

    public record Report(List<Issue> issues, Metrics metrics, Map<String, HexatrapFormula> formulas) {
        public boolean valid() { return issues.stream().noneMatch(i -> !i.soft()); }
        public boolean perfect() { return issues.isEmpty(); }
        public List<Issue> hard() { return issues.stream().filter(i -> !i.soft()).toList(); }
        public List<Issue> soft() { return issues.stream().filter(Issue::soft).toList(); }
    }

    private HexatrapCaseValidator() {}

    public static Report validate(CaseDefinition definition, int difficulty, long seed) {
        List<Issue> issues = new ArrayList<>();
        Map<String, HexatrapFormula> formulas = new LinkedHashMap<>();
        structure(definition, issues, formulas);
        if (!issues.isEmpty()) return new Report(List.copyOf(issues), null, Map.copyOf(formulas));

        HexatrapCaseAnalyzer analyzer = new HexatrapCaseAnalyzer(definition, formulas);
        logic(definition, analyzer, formulas, issues);
        Metrics metrics = null;
        if (issues.isEmpty()) {
            metrics = analyzer.metrics(seed);
            metricsChecks(metrics, analyzer, difficulty, issues);
        }
        return new Report(List.copyOf(issues), metrics, Map.copyOf(formulas));
    }

    private static void structure(CaseDefinition d, List<Issue> issues, Map<String, HexatrapFormula> formulas) {
        text(issues, "title", null, d.title(), 3, 80);
        text(issues, "intro", null, d.intro(), 60, 600);
        text(issues, "epilogue", null, d.epilogue(), 40, 700);
        text(issues, "format", null, d.format(), 1, 30);

        List<Character> characters = d.characters();
        if (characters.size() != 2 || d.character("A") == null || d.character("B") == null) {
            issues.add(hard("characters", null, "Servono esattamente due personaggi con id «A» e «B»."));
        } else {
            for (Character c : characters) {
                text(issues, "character_name", c.id(), c.name(), 2, 24);
                text(issues, "character_role", c.id(), c.role(), 2, 60);
                if (c.icon() == null || !ICONS.contains(c.icon()))
                    issues.add(hard("character_icon", c.id(), "Icona «" + c.icon() + "» non ammessa. Scegli fra: " + String.join(", ", ICONS.stream().sorted().toList()) + "."));
            }
            if (characters.get(0).name() != null && characters.get(0).name().equalsIgnoreCase(characters.get(1).name()))
                issues.add(hard("character_name", null, "I due personaggi devono avere nomi diversi."));
        }

        Set<String> ids = new HashSet<>();
        if (d.facts().size() < 3 || d.facts().size() > 6) issues.add(hard("facts", null, "Servono da 3 a 6 fatti; ne hai " + d.facts().size() + "."));
        long worlds = 1;
        for (Fact fact : d.facts()) {
            id(issues, ids, fact.id(), "fatto");
            text(issues, "fact_label", fact.id(), fact.label(), 5, 90);
            if (fact.options().size() < 2 || fact.options().size() > 4)
                issues.add(hard("fact_options", fact.id(), "Il fatto «" + fact.id() + "» deve avere da 2 a 4 opzioni."));
            Set<String> optionIds = new HashSet<>();
            for (Option option : fact.options()) {
                if (option.id() == null || !ID.matcher(option.id()).matches() || !optionIds.add(option.id()))
                    issues.add(hard("option_id", fact.id(), "Opzione «" + option.id() + "» del fatto «" + fact.id() + "»: id mancante, non valido o ripetuto."));
                text(issues, "option_label", fact.id(), option.label(), 1, 40);
                text(issues, "option_affirm", fact.id(), option.affirm(), 3, 120);
                text(issues, "option_deny", fact.id(), option.deny(), 3, 120);
            }
            worlds *= Math.max(1, fact.options().size());
        }
        if (worlds > HexatrapHypothesisSpace.MAX_WORLDS)
            issues.add(hard("worlds", null, "Troppi mondi possibili (" + worlds + "): al massimo " + HexatrapHypothesisSpace.MAX_WORLDS + "."));

        if (d.certainties().size() > 4) issues.add(hard("certainties", null, "Al massimo 4 certezze."));
        for (String character : HexatrapCase.CHARACTER_IDS) {
            long statements = d.statements().stream().filter(s -> character.equals(s.speaker())).count();
            if (statements < 2 || statements > 4)
                issues.add(hard("statements", null, "Il personaggio " + character + " deve fare da 2 a 4 deposizioni; ne fa " + statements + "."));
            long questions = d.questions().stream().filter(q -> character.equals(q.to())).count();
            if (questions < 3 || questions > 5)
                issues.add(hard("questions", null, "Servono da 3 a 5 domande per il personaggio " + character + "; ne ha " + questions + "."));
        }
        if (d.clues().size() < 3 || d.clues().size() > 6) issues.add(hard("clues", null, "Servono da 3 a 6 reperti; ne hai " + d.clues().size() + "."));

        for (Item item : d.certainties()) item(d, issues, ids, formulas, item, "certezza", true);
        for (Item item : d.statements()) {
            if (!HexatrapCase.CHARACTER_IDS.contains(item.speaker()))
                issues.add(hard("speaker", item.id(), "La deposizione «" + item.id() + "» deve avere speaker «A» o «B»."));
            item(d, issues, ids, formulas, item, "deposizione", false);
        }
        for (Item item : d.questions()) {
            if (!HexatrapCase.CHARACTER_IDS.contains(item.to()))
                issues.add(hard("question_to", item.id(), "La domanda «" + item.id() + "» deve avere to «A» o «B»."));
            item(d, issues, ids, formulas, item, "domanda", false);
        }
        for (Item item : d.clues()) {
            text(issues, "clue_title", item.id(), item.title(), 3, 50);
            if (item.cost() == null || !HexatrapRules.CLUE_COSTS.contains(item.cost()))
                issues.add(hard("clue_cost", item.id(), "Il reperto «" + item.id() + "» deve costare " + HexatrapRules.CLUE_COSTS + " minuti."));
            item(d, issues, ids, formulas, item, "reperto", true);
        }

        Set<String> verdict = new HashSet<>(d.verdictFacts());
        if (d.verdictFacts().isEmpty() || d.verdictFacts().size() > 2 || verdict.size() != d.verdictFacts().size())
            issues.add(hard("verdict_facts", null, "Indica 1 o 2 fatti del verdetto distinti."));
        for (String fact : d.verdictFacts())
            if (d.fact(fact) == null) issues.add(hard("verdict_facts", fact, "Il fatto del verdetto «" + fact + "» non esiste."));

        if (d.solution() == null || !HexatrapCase.CHARACTER_IDS.contains(d.solution().liar())) {
            issues.add(hard("solution", null, "La soluzione deve indicare il bugiardo, «A» o «B»."));
        } else {
            for (Fact fact : d.facts()) {
                String value = d.solution().facts().get(fact.id());
                if (fact.indexOf(value) < 0)
                    issues.add(hard("solution", fact.id(), "La soluzione deve dare un'opzione valida per il fatto «" + fact.id() + "»."));
            }
        }
    }

    private static void item(CaseDefinition d, List<Issue> issues, Set<String> ids, Map<String, HexatrapFormula> formulas,
                             Item item, String kind, boolean factsOnly) {
        id(issues, ids, item.id(), kind);
        text(issues, "item_text", item.id(), item.text(), 5, kind.equals("reperto") ? 220 : 200);
        HexatrapFormula formula;
        try {
            formula = HexatrapFormulaParser.parse(item.formula());
        } catch (HexatrapFormulaParser.ParseException error) {
            issues.add(hard("formula_syntax", item.id(), "Formula di «" + item.id() + "» non valida: " + error.getMessage() + "."));
            return;
        }
        List<FactIs> atoms = new ArrayList<>();
        formula.collectAtoms(atoms);
        for (FactIs atom : atoms) {
            Fact fact = d.fact(atom.fact());
            if (fact == null) issues.add(hard("formula_reference", item.id(), "La formula di «" + item.id() + "» usa il fatto inesistente «" + atom.fact() + "»."));
            else if (fact.indexOf(atom.option()) < 0)
                issues.add(hard("formula_reference", item.id(), "La formula di «" + item.id() + "» usa l'opzione «" + atom.option() + "», che non appartiene al fatto «" + atom.fact() + "»."));
        }
        if (formula.size() > MAX_FORMULA_SIZE || formula.depth() > MAX_FORMULA_DEPTH)
            issues.add(hard("formula_complexity", item.id(), "La formula di «" + item.id() + "» è troppo complessa: al massimo " + MAX_FORMULA_SIZE + " nodi e profondità " + MAX_FORMULA_DEPTH + "."));
        if (formula.saysDepth() > 1) issues.add(hard("formula_says", item.id(), "In «" + item.id() + "» SAYS non può contenere un altro SAYS."));
        if (factsOnly && !formula.factsOnly())
            issues.add(hard("formula_facts_only", item.id(), "La " + kind + " «" + item.id() + "» può parlare solo dei fatti: niente TRUTHFUL né SAYS."));
        String self = item.speaker() != null ? item.speaker() : item.to();
        if (self != null && formula.mentionsSays(self))
            issues.add(hard("formula_self_says", item.id(), "In «" + item.id() + "» il personaggio non può citare la propria risposta con SAYS(" + self + ", …)."));
        formulas.put(item.id(), formula);
    }

    private static void logic(CaseDefinition d, HexatrapCaseAnalyzer analyzer, Map<String, HexatrapFormula> formulas, List<Issue> issues) {
        HexatrapHypothesisSpace space = analyzer.space();
        int truth = analyzer.truth();
        BitSet allowed = analyzer.allowed();
        BitSet full = space.all();
        for (Item certainty : d.certainties()) {
            BitSet set = space.eval(formulas.get(certainty.id()));
            if (set.equals(full)) issues.add(hard("trivial_certainty", certainty.id(), "La certezza «" + certainty.id() + "» è sempre vera: non aggiunge nulla."));
            if (!set.get(truth)) issues.add(hard("solution_violates_certainty", certainty.id(), "La soluzione dichiarata contraddice la certezza «" + certainty.id() + "»."));
        }
        if (allowed.isEmpty()) {
            issues.add(hard("certainties_contradictory", null, "Le certezze si contraddicono: nessun mondo è possibile."));
            return;
        }
        for (Item statement : d.statements()) {
            BitSet set = analyzer.itemSet(statement.id());
            if (!set.get(truth)) {
                issues.add(hard("solution_violates_statement", statement.id(), "Con la soluzione dichiarata la deposizione «" + statement.id() + "» di "
                    + statement.speaker() + " non rispetta il suo ruolo: il sincero deve dire il vero e il bugiardo il falso."));
            }
            BitSet within = (BitSet) allowed.clone();
            within.andNot(set);
            if (within.isEmpty()) issues.add(hard("trivial_statement", statement.id(), "La deposizione «" + statement.id() + "» è compatibile con ogni ipotesi: rendila informativa."));
        }
        for (Item clue : d.clues()) {
            if (!space.holds(formulas.get(clue.id()), truth))
                issues.add(hard("solution_violates_clue", clue.id(), "Il reperto «" + clue.id() + "» è falso con la soluzione dichiarata: i reperti devono essere veri."));
        }
        if (!issues.isEmpty()) return;

        BitSet h0 = analyzer.initial();
        if (analyzer.liarDetermined(h0)) {
            issues.add(hard("liar_known_at_start", null, "Certezze e deposizioni bastano già a capire chi mente: l'indagine non serve. Indebolisci le deposizioni o le certezze."));
            return;
        }
        if (!analyzer.determined(analyzer.actions().keySet())) {
            BitSet known = analyzer.knowledge(analyzer.actions().keySet());
            StringBuilder message = new StringBuilder("Anche usando tutte le domande e tutti i reperti il verdetto resta incerto");
            if (!analyzer.liarDetermined(known)) message.append(": non si capisce chi mente");
            for (String fact : d.verdictFacts()) {
                Set<String> values = new java.util.TreeSet<>();
                for (int h = known.nextSetBit(0); h >= 0; h = known.nextSetBit(h + 1))
                    values.add(space.optionId(HexatrapHypothesisSpace.world(h), fact));
                if (values.size() > 1) message.append("; il fatto «").append(fact).append("» può ancora valere ").append(values);
            }
            issues.add(hard("undetermined", null, message.append(". Aggiungi o rafforza domande e reperti.").toString()));
        }
    }

    private static void metricsChecks(Metrics m, HexatrapCaseAnalyzer analyzer, int difficulty, List<Issue> issues) {
        int budget = HexatrapRules.TIME_BUDGET;
        if (m.guessOdds() < 12)
            issues.add(hard("guessable", null, "Il verdetto si indovina troppo facilmente (1 su " + m.guessOdds() + "): usa fatti del verdetto con più opzioni o un secondo fatto."));
        if (m.totalCost() < budget + 15)
            issues.add(hard("budget_too_generous", null, "Tutte le azioni costano " + m.totalCost() + " minuti: devono superare " + (budget + 15) + " così il giocatore deve scegliere. Aggiungi domande o reperti più costosi."));
        if (m.minimumActions() < 2)
            issues.add(hard("too_easy", m.certificate().isEmpty() ? null : m.certificate().get(0), "Basta una sola azione per risolvere il caso: serve combinare almeno due indizi."));
        if (m.certificateCost() > 40)
            issues.add(hard("too_expensive", null, "Il percorso ottimale costa " + m.certificateCost() + " minuti: deve stare entro 40."));
        if (m.expertPar() < 0 || m.expertPar() > budget)
            issues.add(hard("expert_cannot_finish", null, "Un investigatore esperto non chiude il caso entro " + budget + " minuti: rendi più informative domande e reperti."));
        for (Action action : analyzer.actions().values()) {
            if (action.classification() == ActionClass.TRAP && m.traps() > 2)
                issues.add(hard("too_many_traps", action.itemId(), "«" + action.itemId() + "» non dà alcuna informazione e ci sono già troppe trappole (massimo 2)."));
            if (action.classification() == ActionClass.ORACLE && m.oracles() > 2)
                issues.add(hard("too_many_oracles", action.itemId(), "«" + action.itemId() + "» rivela un fatto indipendentemente da chi mente e ci sono già troppi oracoli (massimo 2)."));
        }
        if (m.roleBalance() < 0.25 || m.roleBalance() > 0.75)
            issues.add(soft("role_balance", null, "All'inizio un ruolo è troppo più probabile dell'altro (" + Math.round(m.roleBalance() * 100) + "% di ipotesi con A bugiardo): bilancia le deposizioni."));
        int minimumActions = HexatrapDifficulty.minimumActions(difficulty);
        if (m.minimumActions() < minimumActions)
            issues.add(soft("difficulty_actions", null, "Per la difficoltà " + difficulty + " servono almeno " + minimumActions + " azioni combinate; ne bastano " + m.minimumActions() + "."));
        if (m.expertPar() > HexatrapDifficulty.maximumPar(difficulty))
            issues.add(soft("difficulty_too_hard", null, "Per la difficoltà " + difficulty + " il par esperto dovrebbe stare entro " + HexatrapDifficulty.maximumPar(difficulty) + " minuti; è " + m.expertPar() + "."));
        if (m.expertPar() < HexatrapDifficulty.minimumPar(difficulty))
            issues.add(soft("difficulty_too_easy", null, "Per la difficoltà " + difficulty + " il par esperto dovrebbe essere almeno " + HexatrapDifficulty.minimumPar(difficulty) + " minuti; è " + m.expertPar() + "."));
    }

    private static void id(List<Issue> issues, Set<String> ids, String id, String kind) {
        if (id == null || !ID.matcher(id).matches()) issues.add(hard("id", id, "Id «" + id + "» (" + kind + ") non valido: usa minuscole, cifre e _ (massimo 24)."));
        else if (!ids.add(id)) issues.add(hard("id", id, "Id «" + id + "» ripetuto: ogni fatto ed elemento deve avere un id unico."));
    }

    private static void text(List<Issue> issues, String code, String itemId, String value, int min, int max) {
        int length = value == null ? 0 : value.strip().length();
        if (length < min || length > max) {
            String where = itemId == null ? "«" + code + "»" : "«" + code + "» di «" + itemId + "»";
            issues.add(hard("text_length", itemId, "Il testo " + where + " deve avere da " + min + " a " + max + " caratteri; ne ha " + length + "."));
        }
    }

    private static Issue hard(String code, String itemId, String message) {
        return new Issue(code, itemId, message, false);
    }

    private static Issue soft(String code, String itemId, String message) {
        return new Issue(code, itemId, message, true);
    }
}
