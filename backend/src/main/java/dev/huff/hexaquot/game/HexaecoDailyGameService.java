package dev.huff.hexaquot.game;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.game.HexaecoDtos.Command;
import dev.huff.hexaquot.game.HexaecoDtos.GameDto;
import dev.huff.hexaquot.game.HexaecoDtos.StatsDto;
import dev.huff.hexaquot.game.HexaecoDtos.SubmitActionDto;
import dev.huff.hexaquot.game.HexaecoDtos.SubmitRequest;
import dev.huff.hexaquot.game.HexaecoDtos.TodayDto;
import dev.huff.hexaquot.persistence.HexaecoGameEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class HexaecoDailyGameService {
    @Inject DailyGameService clock;
    @Inject HexaecoDailyGameProvider provider;
    @Inject ObjectMapper mapper;
    @Inject EntityManager entityManager;

    public TodayDto today(AppUser user) {
        String date = clock.todayDate();
        HexaecoGameEntity game = find(user.id(), date);
        return new TodayDto(date, HexaecoRules.RULES_VERSION, provider.boardFor(date), game == null ? null : toDto(game));
    }

    @Transactional
    public SubmitActionDto submit(AppUser user, SubmitRequest request) {
        if (request == null || request.requestId() == null || request.requestId().isBlank() || request.requestId().length() > 128
            || request.puzzleDate() == null || !request.puzzleDate().matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new BadRequestException("Richiesta Hexaeco non valida.");
        }
        try { LocalDate.parse(request.puzzleDate()); }
        catch (java.time.DateTimeException error) { throw new BadRequestException("Data della sfida non valida."); }
        if (request.rulesVersion() != HexaecoRules.RULES_VERSION) {
            throw new WebApplicationException("Le regole sono cambiate. Ricarica la sfida.", Response.Status.CONFLICT);
        }
        // Lock the user before checking/inserting, including concurrent requests from multiple devices.
        entityManager.createNativeQuery("SELECT id FROM users WHERE id = ?1 FOR UPDATE")
            .setParameter(1, user.id()).getResultList();
        HexaecoGameEntity existing = find(user.id(), request.puzzleDate());
        if (existing != null) return new SubmitActionDto(toDto(existing), true);
        if (!clock.todayDate().equals(request.puzzleDate())) {
            throw new WebApplicationException("È iniziata una nuova sfida. Ricarica per giocare a quella di oggi.", Response.Status.CONFLICT);
        }
        List<Command> moves = request.moves();
        if (moves == null || moves.isEmpty() || moves.size() > HexaecoRules.MAX_MOVES || moves.stream().anyMatch(java.util.Objects::isNull)) {
            throw new BadRequestException("Sequenza di comandi non valida.");
        }
        if (!HexaecoRules.solves(provider.boardFor(request.puzzleDate()), moves)) {
            throw new BadRequestException("Le due luci devono raggiungere insieme i rispettivi traguardi.");
        }
        HexaecoGameEntity game = new HexaecoGameEntity();
        game.id = UUID.randomUUID().toString();
        game.userId = user.id();
        game.puzzleDate = request.puzzleDate();
        game.rulesVersion = HexaecoRules.RULES_VERSION;
        game.requestId = request.requestId();
        try { game.movesJson = mapper.writeValueAsString(moves); }
        catch (Exception error) { throw new IllegalStateException("Cannot serialize Hexaeco moves", error); }
        game.status = "WON";
        game.createdAt = game.updatedAt = game.completedAt = Instant.now().toString();
        game.persist();
        return new SubmitActionDto(toDto(game), false);
    }

    public StatsDto statsForUserId(String userId) {
        List<StatsCalculator.CompletedGame> completed = completedForUser(userId);
        var stats = StatsCalculator.calculate(completed);
        String today = clock.todayDate();
        boolean active = !completed.isEmpty() && completed.get(completed.size() - 1).date()
            .compareTo(LocalDate.parse(today).minusDays(1).toString()) >= 0;
        return new StatsDto(stats.won(), active ? stats.currentStreak() : 0, stats.maxStreak());
    }

    public List<StatsCalculator.CompletedGame> completedForUser(String userId) {
        return HexaecoGameEntity.<HexaecoGameEntity>list("userId = ?1 order by puzzleDate asc", userId).stream()
            .map(game -> new StatsCalculator.CompletedGame(game.puzzleDate, GameStatus.WON, 1)).toList();
    }

    private HexaecoGameEntity find(String userId, String date) {
        return HexaecoGameEntity.<HexaecoGameEntity>find("userId = ?1 and puzzleDate = ?2", userId, date).firstResult();
    }

    private GameDto toDto(HexaecoGameEntity game) {
        try {
            List<Command> moves = mapper.readValue(game.movesJson, new TypeReference<>() {});
            return new GameDto(game.puzzleDate, game.rulesVersion, game.status, List.copyOf(moves), game.completedAt);
        } catch (Exception error) { throw new IllegalStateException("Cannot read Hexaeco moves", error); }
    }
}
