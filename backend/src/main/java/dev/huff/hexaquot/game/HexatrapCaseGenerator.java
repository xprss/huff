package dev.huff.hexaquot.game;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.game.HexatrapAgentClient.AgentException;
import dev.huff.hexaquot.game.hexatrap.HexatrapCase.CaseDefinition;
import dev.huff.hexaquot.game.hexatrap.HexatrapCase.Fact;
import dev.huff.hexaquot.game.hexatrap.HexatrapCase.Item;
import dev.huff.hexaquot.game.hexatrap.HexatrapCaseAnalyzer;
import dev.huff.hexaquot.game.hexatrap.HexatrapCaseValidator;
import dev.huff.hexaquot.game.hexatrap.HexatrapCaseValidator.Issue;
import dev.huff.hexaquot.game.hexatrap.HexatrapCaseValidator.Report;
import dev.huff.hexaquot.game.hexatrap.HexatrapDifficulty;
import dev.huff.hexaquot.game.hexatrap.HexatrapFormula;
import dev.huff.hexaquot.game.hexatrap.HexatrapFormulaParser;
import dev.huff.hexaquot.game.hexatrap.HexatrapItalianRenderer;
import dev.huff.hexaquot.game.hexatrap.HexatrapRules;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Writes the case of a date with the agents: draft, exhaustive validation, blind audit of every
 * sentence against its formula, and specific feedback to the author until a case passes. No
 * database transaction is held during the agent calls.
 */
@ApplicationScoped
public class HexatrapCaseGenerator {
    private static final Logger LOG = Logger.getLogger(HexatrapCaseGenerator.class);
    private static final int RECENT_DAYS = 21;
    private static final int AUDIT_TRIES = 2;

    public enum Outcome { READY, FAILED, SKIPPED }

    @Inject HexatrapAgentClient client;
    @Inject HexatrapCaseStore store;
    @Inject ObjectMapper mapper;

    @ConfigProperty(name = "app.hexatrap.seed", defaultValue = "huff-it-hexaquott-hexatrap")
    String seed;

    @ConfigProperty(name = "app.hexatrap.generation.max-attempts", defaultValue = "4")
    int maxAttempts;

    @ConfigProperty(name = "app.hexatrap.generation.audit", defaultValue = "true")
    boolean audit;

    private record Candidate(CaseDefinition definition, Report report, String model) {}

    public Outcome generate(String date, boolean replaceReady) {
        LocalDate day = LocalDate.parse(date);
        int difficulty = HexatrapDifficulty.forDate(day);
        String format = HexatrapDifficulty.format(day, seed);
        if (!store.claim(date, difficulty, format, replaceReady)) return Outcome.SKIPPED;
        int attempts = 0;
        try {
            String runId = UUID.randomUUID().toString();
            long simulationSeed = HexatrapDifficulty.hash(seed + "|hexatrap|v2|simulation|" + date);
            HexatrapCaseStore.RecentContext recent = store.recent(date, RECENT_DAYS);
            HexatrapAgentClient.Brief brief = new HexatrapAgentClient.Brief(date, difficulty, format, HexatrapRules.TIME_BUDGET,
                HexatrapRules.QUESTION_COST, HexatrapRules.CLUE_COSTS, HexatrapCaseValidator.ICONS.stream().sorted().toList(),
                new HexatrapAgentClient.Avoid(recent.titles(), recent.names(), recent.formats()));

            JsonNode previous = null;
            List<String> feedback = List.of();
            Candidate fallback = null;
            String lastError = null;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                attempts = attempt;
                long started = System.currentTimeMillis();
                HexatrapAgentClient.Draft draft;
                try {
                    draft = client.draft(brief, previous, feedback);
                } catch (AgentException error) {
                    lastError = "Bozza non generata: " + error.getMessage();
                    store.logAttempt(date, runId, attempt, "DRAFT", false, List.of(lastError), null, null, elapsed(started));
                    continue;
                }
                String draftJson = mapper.writeValueAsString(draft.draft());
                CaseDefinition definition;
                try {
                    definition = mapper.treeToValue(draft.draft(), CaseDefinition.class);
                } catch (Exception error) {
                    lastError = "La bozza non rispetta lo schema: " + error.getMessage();
                    feedback = List.of(lastError);
                    previous = draft.draft();
                    store.logAttempt(date, runId, attempt, "VALIDATE", false, feedback, null, draftJson, elapsed(started));
                    continue;
                }
                Report report = HexatrapCaseValidator.validate(definition, difficulty, simulationSeed);
                store.logAttempt(date, runId, attempt, "VALIDATE", report.valid(), report.issues(), report.metrics(), draftJson, elapsed(started));
                if (!report.valid()) {
                    feedback = messages(report.issues());
                    previous = draft.draft();
                    lastError = report.hard().get(0).message();
                    continue;
                }
                if (audit) {
                    long auditStarted = System.currentTimeMillis();
                    List<String> mismatches = null;
                    for (int tryCount = 0; tryCount < AUDIT_TRIES && mismatches == null; tryCount++) {
                        try {
                            mismatches = audit(definition, report.formulas());
                        } catch (AgentException error) {
                            lastError = "Revisione non riuscita: " + error.getMessage();
                        }
                    }
                    if (mismatches == null) {
                        store.logAttempt(date, runId, attempt, "AUDIT", false, List.of(lastError), null, null, elapsed(auditStarted));
                        previous = draft.draft();
                        feedback = messages(report.soft());
                        continue;
                    }
                    store.logAttempt(date, runId, attempt, "AUDIT", mismatches.isEmpty(), mismatches, null, null, elapsed(auditStarted));
                    if (!mismatches.isEmpty()) {
                        List<String> next = new ArrayList<>(mismatches);
                        next.addAll(messages(report.soft()));
                        feedback = next;
                        previous = draft.draft();
                        lastError = mismatches.get(0);
                        continue;
                    }
                }
                Candidate candidate = new Candidate(definition, report, draft.model());
                if (report.perfect()) {
                    accept(date, runId, attempt, candidate, attempts);
                    return Outcome.READY;
                }
                if (fallback == null) fallback = candidate;
                feedback = messages(report.soft());
                previous = draft.draft();
            }
            if (fallback != null) {
                accept(date, runId, attempts, fallback, attempts);
                return Outcome.READY;
            }
            store.markFailed(date, lastError, attempts);
            LOG.errorf("Hexatrap case for %s was not generated: %s", date, lastError);
            return Outcome.FAILED;
        } catch (Exception error) {
            LOG.errorf(error, "Hexatrap case generation failed for %s.", date);
            store.markFailed(date, "Errore interno: " + error.getMessage(), attempts);
            return Outcome.FAILED;
        }
    }

    private void accept(String date, String runId, int attempt, Candidate candidate, int attempts) {
        List<String> warnings = messages(candidate.report().soft());
        store.logAttempt(date, runId, attempt, "ACCEPTED", true, warnings, candidate.report().metrics(), null, 0);
        store.markReady(date, candidate.definition(), candidate.report().metrics(), warnings, attempts, candidate.model());
        LOG.infof("Hexatrap case for %s generated after %d attempts%s.", date, attempts, warnings.isEmpty() ? "" : " with warnings");
    }

    /**
     * Sends only the texts to the auditor and compares each back-translation with the author's
     * formula over every hypothesis allowed by the certainties. Returns one feedback line per
     * sentence that does not say what its formula says.
     */
    List<String> audit(CaseDefinition definition, Map<String, HexatrapFormula> formulas) throws AgentException {
        List<Map<String, Object>> items = new ArrayList<>();
        addItems(items, definition.certainties(), "CERTAINTY");
        addItems(items, definition.statements(), "STATEMENT");
        addItems(items, definition.questions(), "QUESTION");
        addItems(items, definition.clues(), "CLUE");
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("facts", definition.facts());
        request.put("characters", definition.characters().stream().map(c -> Map.of("id", c.id(), "name", c.name())).toList());
        request.put("items", items);
        Map<String, String> translated = client.audit(request);

        HexatrapCaseAnalyzer analyzer = new HexatrapCaseAnalyzer(definition, formulas);
        BitSet allowed = analyzer.allowed();
        HexatrapItalianRenderer renderer = new HexatrapItalianRenderer(definition);
        List<String> mismatches = new ArrayList<>();
        for (Item item : definition.allItems()) {
            HexatrapFormula expected = formulas.get(item.id());
            String source = translated.get(item.id());
            HexatrapFormula reading = null;
            if (source != null) {
                try {
                    reading = HexatrapFormulaParser.parse(source);
                    if (!references(definition, reading)) reading = null;
                } catch (HexatrapFormulaParser.ParseException ignored) {
                    reading = null;
                }
            }
            if (reading == null) {
                mismatches.add("Il testo di «" + item.id() + "» («" + item.text() + "») non è comprensibile senza ambiguità: riscrivilo in modo che dica "
                    + "esattamente «" + renderer.formal(expected) + "».");
                continue;
            }
            BitSet a = analyzer.space().eval(expected);
            BitSet b = analyzer.space().eval(reading);
            a.and(allowed);
            b.and(allowed);
            if (!a.equals(b)) {
                mismatches.add("Il testo di «" + item.id() + "» («" + item.text() + "») non dice la stessa cosa della sua formula: un lettore lo intende come «"
                    + renderer.formal(reading) + "», mentre la formula dice «" + renderer.formal(expected) + "». Riscrivi il testo perché coincida esattamente.");
            }
        }
        return mismatches;
    }

    private static void addItems(List<Map<String, Object>> into, List<Item> items, String kind) {
        for (Item item : items) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", item.id());
            entry.put("kind", kind);
            entry.put("speaker", item.speaker() != null ? item.speaker() : item.to());
            entry.put("text", item.text());
            into.add(entry);
        }
    }

    private static boolean references(CaseDefinition definition, HexatrapFormula formula) {
        List<HexatrapFormula.FactIs> atoms = new ArrayList<>();
        formula.collectAtoms(atoms);
        for (HexatrapFormula.FactIs atom : atoms) {
            Fact fact = definition.fact(atom.fact());
            if (fact == null || fact.indexOf(atom.option()) < 0) return false;
        }
        return true;
    }

    private static List<String> messages(List<Issue> issues) {
        return issues.stream().map(Issue::message).toList();
    }

    private static long elapsed(long started) {
        return System.currentTimeMillis() - started;
    }
}
