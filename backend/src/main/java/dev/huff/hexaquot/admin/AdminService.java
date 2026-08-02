package dev.huff.hexaquot.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.auth.UserService;
import dev.huff.hexaquot.game.DailyGameService;
import dev.huff.hexaquot.game.GameMode;
import dev.huff.hexaquot.game.GameStatus;
import dev.huff.hexaquot.game.GuessResult;
import dev.huff.hexaquot.game.StatsDto;
import dev.huff.hexaquot.persistence.AdminUserEntity;
import dev.huff.hexaquot.persistence.GameEntity;
import dev.huff.hexaquot.persistence.PushSubscriptionEntity;
import dev.huff.hexaquot.persistence.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@ApplicationScoped
public class AdminService {
    @Inject
    ObjectMapper objectMapper;

    @Inject
    DailyGameService dailyGameService;

    @Inject
    UserService userService;

    public List<AdminPlayerSummaryDto> listPlayers(AppUser admin, String query) {
        requireViewPlayers(admin);
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return UserEntity.<UserEntity>listAll()
            .stream()
            .filter(user -> matches(user, normalizedQuery))
            .sorted(Comparator
                .comparing((UserEntity user) -> safeLower(user.displayName))
                .thenComparing(user -> safeLower(user.nickname))
                .thenComparing(user -> user.id))
            .map(this::summary)
            .toList();
    }

    public AdminPlayerDetailDto playerDetail(AppUser admin, String userId) {
        requireViewPlayerDetails(admin);
        UserEntity user = findUser(userId);
        List<AdminGameDto> games = GameEntity.<GameEntity>find(
                "userId = ?1 order by puzzleDate desc, updatedAt desc",
                userId
            )
            .list()
            .stream()
            .map(this::gameDto)
            .toList();

        return new AdminPlayerDetailDto(
            summary(user),
            user.googleSubject,
            user.createdAt,
            Math.toIntExact(PushSubscriptionEntity.count("userId", user.id)),
            dailyGameService.stats(new AppUser(
                user.id,
                user.email,
                user.displayName,
                user.nickname,
                user.profileEmoji,
                user.googleSubject != null
            )),
            games
        );
    }

    @Transactional
    public AdminPlayerDetailDto updatePlayer(AppUser admin, String userId, AdminPlayerUpdateRequest request) {
        requireManagePlayers(admin);
        if (request == null) {
            throw new BadRequestException("Dati giocatore mancanti.");
        }
        UserEntity user = findUser(userId);
        userService.updateProfile(
            userId,
            request.displayName(),
            request.nickname(),
            request.profileEmoji(),
            user.googleSubject != null
        );

        UserEntity updated = findUser(userId);
        updated.starAvailable = Boolean.TRUE.equals(request.starAvailable());
        updated.starAwardedAt = blankToNull(request.starAwardedAt());
        updated.starUsedAt = blankToNull(request.starUsedAt());
        return playerDetail(admin, userId);
    }

    @Transactional
    public AdminDeleteResultDto deletePlayer(AppUser admin, String userId) {
        requireManagePlayers(admin);
        if (Objects.equals(admin.id(), userId)) {
            throw new WebApplicationException("Non puoi eliminare l'admin corrente.", Response.Status.CONFLICT);
        }
        findUser(userId);
        long games = GameEntity.delete("userId = ?1", userId);
        long subscriptions = PushSubscriptionEntity.delete("userId = ?1", userId);
        long adminRows = AdminUserEntity.delete("userId = ?1", userId);
        long users = UserEntity.delete("id = ?1", userId);
        return new AdminDeleteResultDto(Math.toIntExact(users), Math.toIntExact(games), Math.toIntExact(subscriptions), Math.toIntExact(adminRows));
    }

    private void requireViewPlayers(AppUser admin) {
        if (admin == null || admin.admin() == null || !admin.admin().canViewPlayers()) {
            throw new ForbiddenException("Privilegio admin richiesto.");
        }
    }

    private void requireViewPlayerDetails(AppUser admin) {
        if (admin == null || admin.admin() == null || !admin.admin().canViewPlayerDetails()) {
            throw new ForbiddenException("Privilegio admin richiesto.");
        }
    }

    private void requireManagePlayers(AppUser admin) {
        if (admin == null || admin.admin() == null || !admin.admin().canManagePlayers()) {
            throw new ForbiddenException("Privilegio admin richiesto.");
        }
    }

    private UserEntity findUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException("User id mancante.");
        }
        UserEntity user = UserEntity.findById(userId);
        if (user == null) {
            throw new NotFoundException("Giocatore non trovato.");
        }
        return user;
    }

    private AdminPlayerSummaryDto summary(UserEntity user) {
        long started = GameEntity.count("userId", user.id);
        long won = GameEntity.count("userId = ?1 and status = ?2", user.id, GameStatus.WON);
        long lost = GameEntity.count("userId = ?1 and status = ?2", user.id, GameStatus.LOST);
        long completed = won + lost;
        int winRate = completed == 0 ? 0 : Math.toIntExact(Math.round((won * 100.0) / completed));
        String lastActivityAt = GameEntity.<GameEntity>find("userId = ?1 order by updatedAt desc", user.id)
            .firstResultOptional()
            .map(game -> game.updatedAt)
            .orElse(user.createdAt);
        return new AdminPlayerSummaryDto(
            user.id,
            user.email,
            user.displayName,
            user.nickname,
            user.profileEmoji,
            user.googleSubject != null,
            AdminUserEntity.findById(user.id) != null,
            Boolean.TRUE.equals(user.starAvailable),
            user.starAwardedAt,
            user.starUsedAt,
            Math.toIntExact(started),
            Math.toIntExact(completed),
            Math.toIntExact(won),
            Math.toIntExact(lost),
            winRate,
            lastActivityAt
        );
    }

    private AdminGameDto gameDto(GameEntity game) {
        return new AdminGameDto(
            game.id,
            game.puzzleDate,
            game.mode,
            game.mode.label(),
            game.solution,
            readGuesses(game),
            game.status,
            game.mouseTileIndex,
            game.mouseRevealed,
            game.kittenUnlocked,
            game.kittenUsedAt,
            game.createdAt,
            game.updatedAt,
            game.completedAt
        );
    }

    private List<GuessResult> readGuesses(GameEntity game) {
        try {
            return objectMapper.readValue(game.guessesJson, new TypeReference<>() {
            });
        } catch (Exception error) {
            throw new IllegalStateException("Cannot parse guesses for admin view", error);
        }
    }

    private boolean matches(UserEntity user, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return safeLower(user.id).contains(query) ||
            safeLower(user.email).contains(query) ||
            safeLower(user.displayName).contains(query) ||
            safeLower(user.nickname).contains(query) ||
            safeLower(user.googleSubject).contains(query);
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record AdminPlayerSummaryDto(
        String id,
        String email,
        String displayName,
        String nickname,
        String profileEmoji,
        boolean authenticated,
        boolean admin,
        boolean starAvailable,
        String starAwardedAt,
        String starUsedAt,
        int gamesStarted,
        int completed,
        int won,
        int lost,
        int winRate,
        String lastActivityAt
    ) {
    }

    public record AdminPlayerDetailDto(
        AdminPlayerSummaryDto player,
        String googleSubject,
        String createdAt,
        int pushSubscriptions,
        StatsDto stats,
        List<AdminGameDto> games
    ) {
    }

    public record AdminGameDto(
        String id,
        String puzzleDate,
        GameMode mode,
        String modeLabel,
        String solution,
        List<GuessResult> guesses,
        GameStatus status,
        Integer mouseTileIndex,
        Boolean mouseRevealed,
        Boolean kittenUnlocked,
        String kittenUsedAt,
        String createdAt,
        String updatedAt,
        String completedAt
    ) {
    }

    public record AdminPlayerUpdateRequest(
        String displayName,
        String nickname,
        String profileEmoji,
        Boolean starAvailable,
        String starAwardedAt,
        String starUsedAt
    ) {
    }

    public record AdminDeleteResultDto(int users, int games, int pushSubscriptions, int adminRows) {
    }
}
