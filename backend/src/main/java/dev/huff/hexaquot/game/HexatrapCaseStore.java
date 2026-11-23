package dev.huff.hexaquot.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.game.hexatrap.HexatrapCase.CaseDefinition;
import dev.huff.hexaquot.game.hexatrap.HexatrapCase.Item;
import dev.huff.hexaquot.game.hexatrap.HexatrapCaseAnalyzer;
import dev.huff.hexaquot.game.hexatrap.HexatrapCaseAnalyzer.Metrics;
import dev.huff.hexaquot.game.hexatrap.HexatrapFormula;
import dev.huff.hexaquot.game.hexatrap.HexatrapFormulaParser;
import dev.huff.hexaquot.game.hexatrap.HexatrapItalianRenderer;
import dev.huff.hexaquot.game.hexatrap.HexatrapRules;
import dev.huff.hexaquot.persistence.HexatrapCaseEntity;
import dev.huff.hexaquot.persistence.HexatrapGenerationAttemptEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Persistence of the daily cases and of their generation attempts. Ready cases are immutable and cached. */
@ApplicationScoped
public class HexatrapCaseStore {
    public static final String GENERATING = "GENERATING";
    public static final String READY = "READY";
    public static final String FAILED = "FAILED";
    private static final int MAX_DRAFT_LOG_CHARS = 60_000;

    @Inject ObjectMapper mapper;
    @Inject EntityManager entityManager;

    @ConfigProperty(name = "app.hexatrap.generation.lease-minutes", defaultValue = "30")
    int leaseMinutes;

    private final Map<String, ReadyCase> cache = new ConcurrentHashMap<>();

    /** A ready case with everything needed to play it, parsed once. */
    public record ReadyCase(String date, int difficulty, String format, CaseDefinition definition,
                            Map<String, HexatrapFormula> formulas, HexatrapCaseAnalyzer analyzer,
                            HexatrapItalianRenderer renderer, Metrics metrics) {
        public HexatrapFormula formula(String itemId) { return formulas.get(itemId); }
    }

    public Optional<ReadyCase> ready(String date) {
        ReadyCase cached = cache.get(date);
        if (cached != null) return Optional.of(cached);
        HexatrapCaseEntity entity = QuarkusTransaction.joiningExisting().call(() -> entityManager.find(HexatrapCaseEntity.class, date));
        if (entity == null || !READY.equals(entity.status)) return Optional.empty();
        ReadyCase parsed = parse(entity);
        cache.put(date, parsed);
        return Optional.of(parsed);
    }

    private ReadyCase parse(HexatrapCaseEntity entity) {
        try {
            CaseDefinition definition = mapper.readValue(entity.caseJson, CaseDefinition.class);
            Map<String, HexatrapFormula> formulas = new LinkedHashMap<>();
            for (Item item : definition.allItems()) formulas.put(item.id(), HexatrapFormulaParser.parse(item.formula()));
            HexatrapCaseAnalyzer analyzer = new HexatrapCaseAnalyzer(definition, formulas);
            Metrics metrics = mapper.readValue(entity.metricsJson, Metrics.class);
            return new ReadyCase(entity.puzzleDate, entity.difficulty, entity.format, definition, Map.copyOf(formulas),
                analyzer, new HexatrapItalianRenderer(definition), metrics);
        } catch (Exception error) {
            throw new IllegalStateException("Caso Hexatrap " + entity.puzzleDate + " non leggibile.", error);
        }
    }

    /**
     * Claims the generation of a date. Returns false when the case is already ready (unless
     * replacing it is requested) or another run holds an unexpired lease.
     */
    public boolean claim(String date, int difficulty, String format, boolean replaceReady) {
        return QuarkusTransaction.requiringNew().call(() -> {
            long now = System.currentTimeMillis();
            String timestamp = Instant.now().toString();
            int inserted = entityManager.createNativeQuery(
                    "INSERT INTO hexatrap_cases (puzzle_date, rules_version, status, lease_until_ms, difficulty, format, attempts, "
                        + "warnings_json, created_at, updated_at) VALUES (:date, :version, 'GENERATING', :lease, :difficulty, :format, 0, "
                        + "'[]', :now, :now) ON CONFLICT (puzzle_date) DO NOTHING")
                .setParameter("date", date)
                .setParameter("version", HexatrapRules.RULES_VERSION)
                .setParameter("lease", now + leaseMinutes * 60_000L)
                .setParameter("difficulty", difficulty)
                .setParameter("format", format)
                .setParameter("now", timestamp)
                .executeUpdate();
            if (inserted == 1) return true;
            HexatrapCaseEntity entity = entityManager.find(HexatrapCaseEntity.class, date, LockModeType.PESSIMISTIC_WRITE);
            if (entity == null) return false;
            if (READY.equals(entity.status) && !replaceReady) return false;
            if (GENERATING.equals(entity.status) && entity.leaseUntilMs != null && entity.leaseUntilMs > now) return false;
            if (READY.equals(entity.status) && games(date) > 0) return false;
            entity.status = GENERATING;
            entity.leaseUntilMs = now + leaseMinutes * 60_000L;
            entity.difficulty = difficulty;
            entity.format = format;
            entity.lastError = null;
            entity.updatedAt = timestamp;
            cache.remove(date);
            return true;
        });
    }

    public void markReady(String date, CaseDefinition definition, Metrics metrics, List<String> warnings, int attempts, String model) {
        QuarkusTransaction.requiringNew().run(() -> {
            HexatrapCaseEntity entity = entityManager.find(HexatrapCaseEntity.class, date, LockModeType.PESSIMISTIC_WRITE);
            entity.status = READY;
            entity.leaseUntilMs = null;
            entity.title = definition.title();
            entity.caseJson = json(definition);
            entity.metricsJson = json(metrics);
            entity.warningsJson = json(warnings);
            entity.attempts += attempts;
            entity.model = model;
            entity.lastError = null;
            entity.generatedAt = Instant.now().toString();
            entity.updatedAt = entity.generatedAt;
        });
        cache.remove(date);
    }

    /** Marks a failed run. A replaced ready case stays ready when its regeneration fails. */
    public void markFailed(String date, String error, int attempts) {
        QuarkusTransaction.requiringNew().run(() -> {
            HexatrapCaseEntity entity = entityManager.find(HexatrapCaseEntity.class, date, LockModeType.PESSIMISTIC_WRITE);
            if (entity == null) return;
            entity.status = entity.caseJson != null ? READY : FAILED;
            entity.leaseUntilMs = null;
            entity.attempts += attempts;
            entity.lastError = error == null ? "Generazione non riuscita." : truncate(error, 2000);
            entity.updatedAt = Instant.now().toString();
        });
        cache.remove(date);
    }

    public void logAttempt(String date, String runId, int attempt, String phase, boolean ok, Object issues, Object metrics,
                           String draftJson, long durationMs) {
        QuarkusTransaction.requiringNew().run(() -> {
            HexatrapGenerationAttemptEntity row = new HexatrapGenerationAttemptEntity();
            row.id = UUID.randomUUID().toString();
            row.puzzleDate = date;
            row.runId = runId;
            row.attempt = attempt;
            row.phase = phase;
            row.ok = ok;
            row.issuesJson = json(issues == null ? List.of() : issues);
            row.metricsJson = metrics == null ? null : json(metrics);
            row.draftJson = draftJson == null ? null : truncate(draftJson, MAX_DRAFT_LOG_CHARS);
            row.durationMs = durationMs;
            row.createdAt = Instant.now().toString();
            row.persist();
        });
    }

    /** Titles, character names and formats of the ready cases before the given date. */
    public RecentContext recent(String date, int days) {
        return QuarkusTransaction.joiningExisting().call(() -> {
            String from = LocalDate.parse(date).minusDays(days).toString();
            List<HexatrapCaseEntity> rows = HexatrapCaseEntity.list(
                "status = ?1 and puzzleDate >= ?2 and puzzleDate < ?3 order by puzzleDate desc", READY, from, date);
            List<String> titles = new ArrayList<>();
            Set<String> names = new LinkedHashSet<>();
            List<String> formats = new ArrayList<>();
            for (HexatrapCaseEntity row : rows) {
                titles.add(row.title);
                formats.add(row.format);
                try {
                    CaseDefinition definition = mapper.readValue(row.caseJson, CaseDefinition.class);
                    definition.characters().forEach(c -> names.add(c.name()));
                } catch (Exception ignored) {
                    // An unreadable old case only weakens the variety hint.
                }
            }
            return new RecentContext(titles, List.copyOf(names), formats);
        });
    }

    public record RecentContext(List<String> titles, List<String> names, List<String> formats) {}

    public long games(String date) {
        return QuarkusTransaction.joiningExisting().call(() -> ((Number) entityManager
            .createNativeQuery("SELECT COUNT(*) FROM hexatrap_games WHERE puzzle_date = :date")
            .setParameter("date", date).getSingleResult()).longValue());
    }

    public List<HexatrapCaseEntity> list(String from, String to) {
        return HexatrapCaseEntity.list("puzzleDate >= ?1 and puzzleDate <= ?2 order by puzzleDate desc", from, to);
    }

    public HexatrapCaseEntity find(String date) {
        return HexatrapCaseEntity.findById(date);
    }

    public List<HexatrapGenerationAttemptEntity> attempts(String date) {
        return HexatrapGenerationAttemptEntity.list("puzzleDate = ?1 order by createdAt desc", date);
    }

    public String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
