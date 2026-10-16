package dev.huff.hexaquot.game;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.game.HexatrapDtos.*;
import dev.huff.hexaquot.game.HexatrapDtos.GameDto;
import dev.huff.hexaquot.game.HexatrapDtos.StatsDto;
import dev.huff.hexaquot.persistence.HexatrapGameEntity;
import dev.huff.hexaquot.persistence.HexatrapEventEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class HexatrapDailyGameService {
    @Inject DailyGameService clock;
    @Inject HexatrapDailyGameProvider provider;
    @Inject ObjectMapper mapper;
    @Inject EntityManager entityManager;

    @Transactional
    public TodayDto today(AppUser user) {
        // Read counts and the latest events from the same serialized account state.
        lock(user);
        String date = clock.todayDate();
        var game = find(user.id(), date);
        return new TodayDto(date, game == null ? HexatrapRules.RULES_VERSION : game.rulesVersion,
            game == null ? provider.puzzleFor(date).dossier() : puzzle(game).dossier(), game == null ? null : toDto(game));
    }

    @Transactional
    public ActionDto question(AppUser user, QuestionRequest request) {
        if (request == null) throw bad();
        validateEnvelope(request.requestId(), request.puzzleDate());
        lock(user);
        String fingerprint = "question:" + json(request);
        var replay = replay(user.id(), request.requestId(), fingerprint);
        if (replay != null) return replay;
        validateDay(request.puzzleDate(), request.rulesVersion());
        if (request.type() == null || request.advisor() == null || request.advisor() < 0 || request.advisor() > 1
            || (request.type() == QuestionType.SEAL && (request.seal() == null || request.seal() < 0 || request.seal() > 5))
            || (request.type() == QuestionType.NUMBER && request.seal() != null)) throw bad();
        var game = find(user.id(), request.puzzleDate());
        if (game == null) game = newGame(user.id(), request.puzzleDate());
        if (game.status.equals("WON")) throw conflict("La sfida è già completata.");
        var hypotheses = hypotheses(game);
        if (request.type() == QuestionType.NUMBER && !HexatrapRules.unlocked(hypotheses))
            throw conflict("Raccogli altre risposte prima di chiedere il numero.");
        var puzzle = puzzle(game);
        Boolean answer = null;
        Integer number = null;
        if (request.type() == QuestionType.SEAL) {
            answer = HexatrapRules.answer(puzzle.configuration() * 2 + puzzle.loyalAdvisor(), request.advisor(), request.seal());
            game.hypothesesJson = json(HexatrapRules.retain(hypotheses, request.advisor(), request.seal(), answer));
        } else {
            number = request.advisor() == puzzle.loyalAdvisor() ? puzzle.secretNumber() : puzzle.falseNumber();
            game.numberRequested = true;
        }
        game.questions++;
        return record(game, user.id(), request.requestId(), fingerprint,
            new EventDto(game.eventCount + 1, request.type().name(), request.advisor(), request.seal(), answer, number, null, Instant.now().toString()));
    }

    @Transactional
    public ActionDto submit(AppUser user, SubmissionRequest request) {
        if (request == null) throw bad();
        validateEnvelope(request.requestId(), request.puzzleDate());
        lock(user);
        String fingerprint = "submission:" + json(request);
        var replay = replay(user.id(), request.requestId(), fingerprint);
        if (replay != null) return replay;
        validateDay(request.puzzleDate(), request.rulesVersion());
        if (request.number() == null || request.number() < 1 || request.number() > HexatrapRules.MAX_NUMBER) throw bad();
        var game = find(user.id(), request.puzzleDate());
        if (game == null || !game.numberRequested || !HexatrapRules.unlocked(hypotheses(game)))
            throw conflict("Prima deduci il leale e chiedi il numero a un consigliere.");
        if (game.status.equals("WON")) throw conflict("La sfida è già completata.");
        boolean correct = request.number() == puzzle(game).secretNumber();
        if (correct) { game.status = "WON"; game.completedAt = Instant.now().toString(); }
        else game.wrongSubmissions++;
        return record(game, user.id(), request.requestId(), fingerprint,
            new EventDto(game.eventCount + 1, "SUBMISSION", null, null, null, request.number(), correct, Instant.now().toString()));
    }

    public EventsDto events(AppUser user, String date, Long before, int limit) {
        if (date == null) date = clock.todayDate();
        validateEnvelope("events", date);
        if (before != null && before <= 0 || limit < 1 || limit > 100) throw bad();
        var game = find(user.id(), date);
        return game == null ? new EventsDto(List.of(), null) : page(game, before, limit);
    }

    private EventsDto page(HexatrapGameEntity game, Long before, int limit) {
        var rows = HexatrapEventEntity.<HexatrapEventEntity>find(
            "gameId = ?1 and sequence < ?2 order by sequence desc", game.id, before == null ? Long.MAX_VALUE : before)
            .range(0, limit).list(); // One extra row determines whether another page exists.
        var visible = rows.stream().limit(limit).map(e -> read(e.eventJson, EventDto.class)).toList();
        return new EventsDto(visible, rows.size() > limit ? visible.get(visible.size() - 1).sequence() : null);
    }

    private ActionDto record(HexatrapGameEntity game, String userId, String requestId, String fingerprint, EventDto event) {
        if (event.type().equals("SEAL")) {
            var evidence = new java.util.ArrayList<>(evidence(game));
            if (evidence.stream().noneMatch(e -> e.seal().equals(event.seal()))) {
                evidence.add(event);
                game.evidenceJson = json(evidence);
            }
        }
        game.eventCount++;
        game.points = HexatrapRules.points(game.questions, game.wrongSubmissions);
        game.updatedAt = event.createdAt();
        if (!game.isPersistent()) game.persist();
        var row = new HexatrapEventEntity();
        row.id = UUID.randomUUID().toString(); row.gameId = game.id; row.userId = userId;
        row.sequence = event.sequence(); row.requestId = requestId; row.fingerprint = fingerprint; row.eventJson = json(event);
        // Store this operation's original state for retries, even after other devices have moved on.
        row.responseJson = "{}"; row.persist();
        var response = new ActionDto(toDto(game), event, false);
        row.responseJson = json(response);
        return response;
    }

    private ActionDto replay(String userId, String requestId, String fingerprint) {
        var row = HexatrapEventEntity.<HexatrapEventEntity>find("userId = ?1 and requestId = ?2", userId, requestId).firstResult();
        if (row == null) return null;
        if (!row.fingerprint.equals(fingerprint)) throw conflict("Identificatore già usato con contenuto diverso.");
        var response = read(row.responseJson, ActionDto.class);
        return new ActionDto(response.game(), response.event(), true);
    }

    private GameDto toDto(HexatrapGameEntity game) {
        Reveal reveal = null;
        if (game.status.equals("WON")) {
            var p = puzzle(game);
            int falseLoyal = 1 - p.loyalAdvisor();
            String[] names = {"Sole", "Luna", "Stella", "Fiamma", "Onda", "Foglia"};
            String counterfactual = evidence(game).stream().map(e ->
                names[e.seal()] + " " + (e.answer() == (e.advisor() == falseLoyal) ? "acceso" : "spento")
                    + " (risposta #" + e.sequence() + ")").collect(java.util.stream.Collectors.joining(", "));
            reveal = new Reveal(p.secretNumber(), p.loyalAdvisor(), p.configuration(),
                "Se il consigliere " + (falseLoyal == 0 ? "A" : "B") + " fosse leale, le risposte implicherebbero: "
                    + counterfactual + ". Nessuna configurazione con questi stati soddisfa il dossier. Il consigliere "
                    + (p.loyalAdvisor() == 0 ? "A" : "B") + " è quindi leale.");
        }
        return new GameDto(game.puzzleDate, game.rulesVersion, game.status, game.questions, game.wrongSubmissions,
            game.points, HexatrapRules.unlocked(hypotheses(game)), game.numberRequested, page(game, null, 20), reveal);
    }

    private HexatrapGameEntity newGame(String userId, String date) {
        var game = new HexatrapGameEntity();
        game.id = UUID.randomUUID().toString(); game.userId = userId; game.puzzleDate = date;
        game.rulesVersion = HexatrapRules.RULES_VERSION;
        var puzzle = provider.puzzleFor(date);
        game.puzzleJson = json(puzzle); game.hypothesesJson = json(HexatrapRules.hypotheses(puzzle.dossier()));
        game.status = "IN_PROGRESS"; game.points = 1000; game.createdAt = game.updatedAt = Instant.now().toString();
        return game;
    }
    private HexatrapGameEntity find(String userId, String date) {
        return HexatrapGameEntity.<HexatrapGameEntity>find("userId = ?1 and puzzleDate = ?2", userId, date).firstResult();
    }
    private void lock(AppUser user) {
        entityManager.createNativeQuery("SELECT id FROM users WHERE id = ?1 FOR UPDATE").setParameter(1, user.id()).getResultList();
    }
    private void validateEnvelope(String id, String date) {
        if (id == null || id.isBlank() || id.length() > 128 || date == null || !date.matches("\\d{4}-\\d{2}-\\d{2}")) throw bad();
        try { LocalDate.parse(date); } catch (java.time.DateTimeException e) { throw bad(); }
    }
    private void validateDay(String date, int version) {
        if (version != HexatrapRules.RULES_VERSION) throw conflict("Le regole sono cambiate. Ricarica la sfida.");
        if (!clock.todayDate().equals(date)) throw conflict("È iniziata una nuova sfida. Ricarica per giocare a quella di oggi.");
    }
    private Puzzle puzzle(HexatrapGameEntity game) { return read(game.puzzleJson, Puzzle.class); }
    private List<EventDto> evidence(HexatrapGameEntity game) {
        try { return mapper.readValue(game.evidenceJson, new TypeReference<>() {}); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    private List<Integer> hypotheses(HexatrapGameEntity game) {
        try { return mapper.readValue(game.hypothesesJson, new TypeReference<>() {}); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    private String json(Object value) {
        try { return mapper.writeValueAsString(value); } catch (Exception e) { throw new IllegalStateException(e); }
    }
    private <T> T read(String json, Class<T> type) {
        try { return mapper.readValue(json, type); } catch (Exception e) { throw new IllegalStateException(e); }
    }
    private BadRequestException bad() { return new BadRequestException("Richiesta Hexatrap non valida."); }
    private WebApplicationException conflict(String message) { return new WebApplicationException(message, 409); }

    public List<StatsCalculator.CompletedGame> completedForUser(String userId) {
        return games(userId).stream().filter(g -> g.status.equals("WON"))
            .map(g -> new StatsCalculator.CompletedGame(g.puzzleDate, GameStatus.WON, (int)Math.min(Integer.MAX_VALUE, g.questions))).toList();
    }
    private List<HexatrapGameEntity> games(String userId) {
        return HexatrapGameEntity.list("userId = ?1 order by puzzleDate asc", userId);
    }
    public StatsDto statsForUserId(String userId) {
        var games = games(userId);
        var wins = games.stream().filter(g -> g.status.equals("WON")).toList();
        var streak = StatsCalculator.calculate(completedForUser(userId));
        boolean active = !wins.isEmpty() && wins.get(wins.size()-1).puzzleDate.compareTo(LocalDate.parse(clock.todayDate()).minusDays(1).toString()) >= 0;
        return new StatsDto(games.size(), wins.size(), active ? streak.currentStreak() : 0, streak.maxStreak(),
            wins.stream().mapToLong(g -> g.points).average().orElse(0),
            wins.isEmpty() ? null : wins.stream().mapToLong(g -> g.points).max().getAsLong(),
            wins.stream().mapToLong(g -> g.questions).average().orElse(0));
    }
}
