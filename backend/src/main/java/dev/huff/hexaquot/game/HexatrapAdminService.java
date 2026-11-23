package dev.huff.hexaquot.game;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.game.HexatrapDtos.*;
import dev.huff.hexaquot.game.hexatrap.HexatrapCase.CaseDefinition;
import dev.huff.hexaquot.game.hexatrap.HexatrapCase.Item;
import dev.huff.hexaquot.game.hexatrap.HexatrapFormulaParser;
import dev.huff.hexaquot.game.hexatrap.HexatrapItalianRenderer;
import dev.huff.hexaquot.persistence.HexatrapCaseEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Admin view of the generated cases: metrics, full solution, attempt log and regeneration. */
@ApplicationScoped
public class HexatrapAdminService {
    private static final int MAX_REGENERATION_DAYS = 7;

    @Inject HexatrapCaseStore store;
    @Inject HexatrapCaseScheduler scheduler;
    @Inject HexatrapAgentClient client;
    @Inject DailyGameService clock;
    @Inject ObjectMapper mapper;

    @ConfigProperty(name = "app.hexatrap.generation.lookahead-days", defaultValue = "1")
    int lookaheadDays;

    @Transactional
    public List<CaseSummaryDto> list(AppUser user, String from, String to) {
        require(user);
        LocalDate today = LocalDate.parse(clock.todayDate());
        String start = from == null ? today.minusDays(14).toString() : date(from).toString();
        String end = to == null ? today.plusDays(Math.max(lookaheadDays, 1)).toString() : date(to).toString();
        return store.list(start, end).stream().map(this::summary).toList();
    }

    @Transactional
    public CaseDetailDto detail(AppUser user, String rawDate) {
        require(user);
        HexatrapCaseEntity entity = store.find(date(rawDate).toString());
        if (entity == null) throw new NotFoundException("Nessun caso per questa data.");
        Map<String, String> records = new LinkedHashMap<>();
        JsonNode caseFile = null;
        if (entity.caseJson != null) {
            caseFile = readTree(entity.caseJson);
            try {
                CaseDefinition definition = mapper.readValue(entity.caseJson, CaseDefinition.class);
                HexatrapItalianRenderer renderer = new HexatrapItalianRenderer(definition);
                for (Item item : definition.allItems())
                    records.put(item.id(), HexatrapItalianRenderer.capitalize(renderer.formal(HexatrapFormulaParser.parse(item.formula()))));
            } catch (Exception ignored) {
                // The raw case is still shown when a record cannot be rendered.
            }
        }
        List<AttemptDto> attempts = store.attempts(entity.puzzleDate).stream().map(a -> new AttemptDto(a.runId, a.attempt, a.phase, a.ok,
            readTree(a.issuesJson), a.metricsJson == null ? null : readTree(a.metricsJson), a.durationMs, a.createdAt)).toList();
        return new CaseDetailDto(summary(entity), caseFile, records, attempts);
    }

    public GenerationQueuedDto generate(AppUser user, String rawDate) {
        require(user);
        LocalDate date = date(rawDate);
        LocalDate today = LocalDate.parse(clock.todayDate());
        if (date.isBefore(today) || date.isAfter(today.plusDays(MAX_REGENERATION_DAYS)))
            throw new WebApplicationException("Si possono generare solo i casi da oggi ai prossimi " + MAX_REGENERATION_DAYS + " giorni.", 409);
        if (store.games(date.toString()) > 0)
            throw new WebApplicationException("Il caso è già stato giocato: non può essere rigenerato.", 409);
        if (!client.configured()) throw new WebApplicationException("L'agente AI non è configurato.", 503);
        boolean queued = scheduler.enqueue(date.toString(), true);
        return new GenerationQueuedDto(date.toString(), queued || scheduler.queued(date.toString()));
    }

    private CaseSummaryDto summary(HexatrapCaseEntity entity) {
        long games = store.games(entity.puzzleDate);
        LocalDate today = LocalDate.parse(clock.todayDate());
        LocalDate date = LocalDate.parse(entity.puzzleDate);
        boolean regenerable = games == 0 && !date.isBefore(today) && !date.isAfter(today.plusDays(MAX_REGENERATION_DAYS))
            && !HexatrapCaseStore.GENERATING.equals(entity.status);
        return new CaseSummaryDto(entity.puzzleDate, entity.status, entity.difficulty, entity.format, entity.title, entity.attempts,
            entity.model, entity.generatedAt, entity.lastError, games,
            entity.metricsJson == null ? null : readTree(entity.metricsJson), readTree(entity.warningsJson), regenerable);
    }

    private JsonNode readTree(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception error) {
            return null;
        }
    }

    private static LocalDate date(String value) {
        try {
            return LocalDate.parse(value);
        } catch (Exception error) {
            throw new BadRequestException("Data non valida.");
        }
    }

    private static void require(AppUser user) {
        if (user == null || user.admin() == null || !user.admin().canManageHexatrapCases())
            throw new ForbiddenException("Privilegio casi Hexatrap richiesto.");
    }
}
