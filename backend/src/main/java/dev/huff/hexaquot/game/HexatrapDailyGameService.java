package dev.huff.hexaquot.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.game.HexatrapCaseStore.ReadyCase;
import dev.huff.hexaquot.game.HexatrapDtos.*;
import dev.huff.hexaquot.game.HexatrapDtos.GameDto;
import dev.huff.hexaquot.game.HexatrapDtos.StatsDto;
import dev.huff.hexaquot.game.hexatrap.HexatrapCase.CaseDefinition;
import dev.huff.hexaquot.game.hexatrap.HexatrapCase.Character;
import dev.huff.hexaquot.game.hexatrap.HexatrapCase.Item;
import dev.huff.hexaquot.game.hexatrap.HexatrapCaseAnalyzer;
import dev.huff.hexaquot.game.hexatrap.HexatrapItalianRenderer;
import dev.huff.hexaquot.game.hexatrap.HexatrapRules;
import dev.huff.hexaquot.persistence.HexatrapActionEntity;
import dev.huff.hexaquot.persistence.HexatrapGameEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Plays the daily Hexatrap case. The server answers questions and reveals clues from the stored
 * case; the player spends minutes from a fixed budget and gives a single, final verdict.
 * Requests are idempotent by request id, and each account's actions are serialized.
 */
@ApplicationScoped
public class HexatrapDailyGameService {
    @Inject DailyGameService clock;
    @Inject HexatrapCaseStore cases;
    @Inject HexatrapAccess access;
    @Inject ObjectMapper mapper;
    @Inject EntityManager entityManager;

    @Transactional
    public TodayDto today(AppUser user) {
        requireAccess(user);
        lock(user);
        String date = clock.todayDate();
        var ready = cases.ready(date);
        if (ready.isEmpty()) return new TodayDto(date, HexatrapRules.RULES_VERSION, false, null, null);
        HexatrapGameEntity game = find(user.id(), date);
        return new TodayDto(date, HexatrapRules.RULES_VERSION, true, caseDto(ready.get()), game == null ? null : toDto(game, ready.get()));
    }

    @Transactional
    public ActionDto question(AppUser user, QuestionRequest request) {
        requireAccess(user);
        if (request == null) throw bad();
        validateEnvelope(request.requestId(), request.puzzleDate());
        lock(user);
        String fingerprint = "question:" + json(request);
        ActionDto replay = replay(user.id(), request.requestId(), fingerprint);
        if (replay != null) return replay;
        validateDay(request.puzzleDate(), request.rulesVersion());
        ReadyCase ready = readyCase(request.puzzleDate());
        Item question = request.questionId() == null ? null : ready.definition().question(request.questionId());
        if (question == null) throw bad();
        HexatrapGameEntity game = gameFor(user.id(), request.puzzleDate());
        String key = HexatrapCaseAnalyzer.questionKey(question.id());
        spend(game, key, HexatrapRules.QUESTION_COST);
        return record(game, ready, user.id(), request.requestId(), fingerprint, "QUESTION", key);
    }

    @Transactional
    public ActionDto clue(AppUser user, ClueRequest request) {
        requireAccess(user);
        if (request == null) throw bad();
        validateEnvelope(request.requestId(), request.puzzleDate());
        lock(user);
        String fingerprint = "clue:" + json(request);
        ActionDto replay = replay(user.id(), request.requestId(), fingerprint);
        if (replay != null) return replay;
        validateDay(request.puzzleDate(), request.rulesVersion());
        ReadyCase ready = readyCase(request.puzzleDate());
        Item clue = request.clueId() == null ? null : ready.definition().clue(request.clueId());
        if (clue == null) throw bad();
        HexatrapGameEntity game = gameFor(user.id(), request.puzzleDate());
        String key = HexatrapCaseAnalyzer.clueKey(clue.id());
        spend(game, key, clue.cost());
        return record(game, ready, user.id(), request.requestId(), fingerprint, "CLUE", key);
    }

    @Transactional
    public ActionDto verdict(AppUser user, VerdictRequest request) {
        requireAccess(user);
        if (request == null) throw bad();
        validateEnvelope(request.requestId(), request.puzzleDate());
        lock(user);
        String fingerprint = "verdict:" + json(request);
        ActionDto replay = replay(user.id(), request.requestId(), fingerprint);
        if (replay != null) return replay;
        validateDay(request.puzzleDate(), request.rulesVersion());
        ReadyCase ready = readyCase(request.puzzleDate());
        CaseDefinition definition = ready.definition();
        if (!"A".equals(request.liar()) && !"B".equals(request.liar())) throw bad();
        Map<String, String> facts = request.facts() == null ? Map.of() : request.facts();
        if (facts.size() != definition.verdictFacts().size()) throw bad();
        for (String fact : definition.verdictFacts()) {
            if (definition.fact(fact).indexOf(facts.get(fact)) < 0) throw bad();
        }
        HexatrapGameEntity game = gameFor(user.id(), request.puzzleDate());
        if (!"IN_PROGRESS".equals(game.status)) throw conflict("Il verdetto è già stato emesso.");
        boolean liarCorrect = ready.analyzer().liarCorrect(request.liar());
        boolean factsCorrect = ready.analyzer().factsCorrect(facts);
        String now = Instant.now().toString();
        game.outcome = liarCorrect && factsCorrect ? "SOLVED" : liarCorrect ? "PARTIAL" : "WRONG";
        game.status = "SOLVED".equals(game.outcome) ? "WON" : "LOST";
        game.points = "SOLVED".equals(game.outcome) ? HexatrapRules.points(ready.difficulty(), HexatrapRules.TIME_BUDGET - game.minutesUsed) : 0;
        game.verdictJson = json(new VerdictDto(request.liar(), new LinkedHashMap<>(facts)));
        game.completedAt = now;
        return record(game, ready, user.id(), request.requestId(), fingerprint, "VERDICT", "V");
    }

    private void spend(HexatrapGameEntity game, String key, int cost) {
        if (!"IN_PROGRESS".equals(game.status)) throw conflict("Il caso è già chiuso.");
        if (game.isPersistent() && HexatrapActionEntity.count("gameId = ?1 and actionKey = ?2", game.id, key) > 0)
            throw conflict(key.startsWith("Q:") ? "Hai già posto questa domanda." : "Hai già esaminato questo reperto.");
        if (game.minutesUsed + cost > HexatrapRules.TIME_BUDGET)
            throw conflict("Non hai abbastanza tempo per questa azione: emetti il verdetto con ciò che sai.");
        game.minutesUsed += cost;
        game.actions++;
    }

    private ActionDto record(HexatrapGameEntity game, ReadyCase ready, String userId, String requestId, String fingerprint,
                             String kind, String key) {
        String now = Instant.now().toString();
        game.updatedAt = now;
        if (!game.isPersistent()) game.persist();
        HexatrapActionEntity row = new HexatrapActionEntity();
        row.id = UUID.randomUUID().toString();
        row.gameId = game.id;
        row.userId = userId;
        row.sequence = (int) HexatrapActionEntity.count("gameId", game.id) + 1;
        row.requestId = requestId;
        row.fingerprint = fingerprint;
        row.kind = kind;
        row.actionKey = key;
        row.createdAt = now;
        row.responseJson = "{}";
        row.persist();
        entityManager.flush();
        ActionDto response = new ActionDto(toDto(game, ready), false);
        // Keep this operation's original response for retries, even after other devices move on.
        row.responseJson = json(response);
        return response;
    }

    private ActionDto replay(String userId, String requestId, String fingerprint) {
        HexatrapActionEntity row = HexatrapActionEntity.<HexatrapActionEntity>find("userId = ?1 and requestId = ?2", userId, requestId).firstResult();
        if (row == null) return null;
        if (!row.fingerprint.equals(fingerprint)) throw conflict("Identificatore già usato con contenuto diverso.");
        ActionDto response = read(row.responseJson, ActionDto.class);
        return new ActionDto(response.game(), true);
    }

    CaseDto caseDto(ReadyCase ready) {
        CaseDefinition d = ready.definition();
        HexatrapItalianRenderer renderer = ready.renderer();
        return new CaseDto(d.title(), d.format(), ready.difficulty(), d.intro(), HexatrapRules.TIME_BUDGET, HexatrapRules.QUESTION_COST,
            d.characters().stream().map(c -> new CharacterDto(c.id(), c.name(), c.role(), c.icon())).toList(),
            d.facts().stream().map(f -> new FactDto(f.id(), f.label(),
                f.options().stream().map(o -> new OptionDto(o.id(), o.label())).toList(), d.verdictFacts().contains(f.id()))).toList(),
            d.certainties().stream().map(c -> new CertaintyDto(c.id(), c.text(),
                HexatrapItalianRenderer.capitalize(renderer.formal(ready.formula(c.id()))))).toList(),
            d.statements().stream().map(s -> new StatementDto(s.id(), s.speaker(), s.text(),
                HexatrapItalianRenderer.capitalize(renderer.formal(ready.formula(s.id()))),
                new ReadingsDto(renderer.statementReading(ready.formula(s.id()), s.speaker(), "A"),
                    renderer.statementReading(ready.formula(s.id()), s.speaker(), "B")))).toList(),
            d.questions().stream().map(q -> new QuestionDto(q.id(), q.to(), q.text(), renderer.question(ready.formula(q.id())))).toList(),
            d.clues().stream().map(c -> new ClueCardDto(c.id(), c.title(), c.cost())).toList());
    }

    private GameDto toDto(HexatrapGameEntity game, ReadyCase ready) {
        CaseDefinition definition = ready.definition();
        HexatrapItalianRenderer renderer = ready.renderer();
        List<HexatrapActionEntity> actions = game.isPersistent()
            ? HexatrapActionEntity.list("gameId = ?1 order by sequence", game.id) : List.of();
        List<AskedDto> asked = new ArrayList<>();
        List<RevealedDto> revealed = new ArrayList<>();
        List<String> knownItems = new ArrayList<>();
        definition.certainties().forEach(i -> knownItems.add(i.id()));
        definition.statements().forEach(i -> knownItems.add(i.id()));
        for (HexatrapActionEntity action : actions) {
            var analyzed = ready.analyzer().actions().get(action.actionKey);
            if (analyzed == null) continue;
            if ("QUESTION".equals(action.kind)) {
                Item question = definition.question(analyzed.itemId());
                var formula = ready.formula(question.id());
                asked.add(new AskedDto(action.sequence, question.id(), question.to(), analyzed.answer(), new ReadingsDto(
                    renderer.answerReading(formula, question.to(), analyzed.answer(), "A"),
                    renderer.answerReading(formula, question.to(), analyzed.answer(), "B"))));
            } else if ("CLUE".equals(action.kind)) {
                Item clue = definition.clue(analyzed.itemId());
                revealed.add(new RevealedDto(action.sequence, clue.id(), clue.title(), clue.text(),
                    HexatrapItalianRenderer.capitalize(renderer.formal(ready.formula(clue.id())))));
            }
            knownItems.add(analyzed.itemId());
        }
        VerdictDto verdict = game.verdictJson == null ? null : read(game.verdictJson, VerdictDto.class);
        RevealDto reveal = verdict == null ? null : reveal(game, ready, verdict, knownItems);
        int sequence = actions.isEmpty() ? 0 : actions.get(actions.size() - 1).sequence;
        return new GameDto(game.puzzleDate, game.rulesVersion, game.status, game.outcome, game.minutesUsed,
            HexatrapRules.TIME_BUDGET - game.minutesUsed, sequence, asked, revealed, verdict, game.points, reveal, game.completedAt);
    }

    private RevealDto reveal(HexatrapGameEntity game, ReadyCase ready, VerdictDto verdict, List<String> knownItems) {
        CaseDefinition definition = ready.definition();
        HexatrapCaseAnalyzer analyzer = ready.analyzer();
        var proof = analyzer.proofOfLiar(knownItems);
        var optimal = analyzer.optimalPath();
        Map<String, String> truth = new LinkedHashMap<>();
        for (String fact : definition.verdictFacts()) truth.put(fact, definition.solution().facts().get(fact));
        List<EvidenceDto> proofItems = proof.keys().stream().map(id -> evidence(ready, id)).toList();
        List<EvidenceDto> path = optimal.keys().stream().map(key -> evidence(ready, analyzer.actions().get(key).itemId())).toList();
        String outcome = switch (game.outcome) {
            case "SOLVED" -> "✅ Caso risolto";
            case "PARTIAL" -> "🟨 Bugiardo smascherato, ricostruzione sbagliata";
            default -> "❌ Ho creduto al bugiardo";
        };
        String share = "Hexatrap " + LocalDate.parse(game.puzzleDate).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"))
            + " · " + outcome + " · ⏱️ " + game.minutesUsed + "/" + HexatrapRules.TIME_BUDGET + " min · 🔎 " + game.actions + " azioni"
            + ("SOLVED".equals(game.outcome) ? " · " + game.points + " punti" : "");
        return new RevealDto(definition.solution().liar(), truth, definition.epilogue(), analyzer.liarCorrect(verdict.liar()),
            analyzer.factsCorrect(verdict.facts()), proof.found(), proofItems, path, optimal.cost(),
            ready.metrics().expertPar(), share);
    }

    private EvidenceDto evidence(ReadyCase ready, String itemId) {
        CaseDefinition d = ready.definition();
        for (Item item : d.certainties()) if (item.id().equals(itemId)) return new EvidenceDto("CERTAINTY", itemId, item.text());
        for (Item item : d.statements()) if (item.id().equals(itemId))
            return new EvidenceDto("STATEMENT", itemId, name(d, item.speaker()) + ": «" + item.text() + "»");
        for (Item item : d.questions()) if (item.id().equals(itemId)) {
            boolean answer = ready.analyzer().actions().get(HexatrapCaseAnalyzer.questionKey(itemId)).answer();
            return new EvidenceDto("QUESTION", itemId, item.text() + " → " + name(d, item.to()) + ": «" + (answer ? "Sì" : "No") + "»");
        }
        for (Item item : d.clues()) if (item.id().equals(itemId)) return new EvidenceDto("CLUE", itemId, item.title() + ": " + item.text());
        return new EvidenceDto("UNKNOWN", itemId, itemId);
    }

    private static String name(CaseDefinition definition, String id) {
        Character character = definition.character(id);
        return character == null ? id : character.name();
    }

    private ReadyCase readyCase(String date) {
        return cases.ready(date).orElseThrow(() -> conflict("Il caso di oggi è ancora in preparazione. Riprova tra poco."));
    }

    private HexatrapGameEntity gameFor(String userId, String date) {
        HexatrapGameEntity game = find(userId, date);
        if (game != null) return game;
        game = new HexatrapGameEntity();
        game.id = UUID.randomUUID().toString();
        game.userId = userId;
        game.puzzleDate = date;
        game.rulesVersion = HexatrapRules.RULES_VERSION;
        game.status = "IN_PROGRESS";
        game.createdAt = game.updatedAt = Instant.now().toString();
        return game;
    }

    private HexatrapGameEntity find(String userId, String date) {
        return HexatrapGameEntity.<HexatrapGameEntity>find("userId = ?1 and puzzleDate = ?2", userId, date).firstResult();
    }

    private void requireAccess(AppUser user) {
        if (!access.available(user)) throw new NotFoundException("Hexatrap non è ancora disponibile.");
    }

    private void lock(AppUser user) {
        entityManager.createNativeQuery("SELECT id FROM users WHERE id = ?1 FOR UPDATE").setParameter(1, user.id()).getResultList();
    }

    private void validateEnvelope(String id, String date) {
        if (id == null || id.isBlank() || id.length() > 128 || date == null || !date.matches("\\d{4}-\\d{2}-\\d{2}")) throw bad();
        try {
            LocalDate.parse(date);
        } catch (java.time.DateTimeException error) {
            throw bad();
        }
    }

    private void validateDay(String date, int version) {
        if (version != HexatrapRules.RULES_VERSION) throw conflict("Le regole sono cambiate. Ricarica il caso.");
        if (!clock.todayDate().equals(date)) throw conflict("È iniziato un nuovo caso. Ricarica per giocare a quello di oggi.");
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    private BadRequestException bad() {
        return new BadRequestException("Richiesta Hexatrap non valida.");
    }

    private WebApplicationException conflict(String message) {
        return new WebApplicationException(message, 409);
    }

    public List<StatsCalculator.CompletedGame> completedForUser(String userId) {
        return games(userId).stream().filter(g -> !"IN_PROGRESS".equals(g.status))
            .map(g -> new StatsCalculator.CompletedGame(g.puzzleDate, GameStatus.valueOf(g.status), 1)).toList();
    }

    private List<HexatrapGameEntity> games(String userId) {
        return HexatrapGameEntity.list("userId = ?1 order by puzzleDate asc", userId);
    }

    public StatsDto statsForUserId(String userId) {
        List<HexatrapGameEntity> games = games(userId);
        List<HexatrapGameEntity> finished = games.stream().filter(g -> !"IN_PROGRESS".equals(g.status)).toList();
        List<HexatrapGameEntity> wins = finished.stream().filter(g -> "WON".equals(g.status)).toList();
        var streak = StatsCalculator.calculate(completedForUser(userId));
        boolean active = !finished.isEmpty() && finished.get(finished.size() - 1).puzzleDate
            .compareTo(LocalDate.parse(clock.todayDate()).minusDays(1).toString()) >= 0;
        return new StatsDto(games.size(), wins.size(), finished.size() - wins.size(),
            (int) finished.stream().filter(g -> "PARTIAL".equals(g.outcome)).count(),
            active ? streak.currentStreak() : 0, streak.maxStreak(),
            wins.stream().mapToLong(g -> g.points).average().orElse(0),
            wins.isEmpty() ? null : wins.stream().mapToLong(g -> g.points).max().getAsLong(),
            finished.stream().mapToInt(g -> g.minutesUsed).average().orElse(0));
    }
}
