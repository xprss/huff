package dev.huff.hexaquot.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.auth.UserService;
import dev.huff.hexaquot.game.GameMode;
import dev.huff.hexaquot.game.GameStatus;
import dev.huff.hexaquot.game.GuessResult;
import dev.huff.hexaquot.game.StatsDto;
import dev.huff.hexaquot.game.HexahackDtos;
import dev.huff.hexaquot.game.HexahackDtos.EventDto;
import dev.huff.hexaquot.game.OverallStatsService;
import dev.huff.hexaquot.persistence.AdminUserEntity;
import dev.huff.hexaquot.persistence.GameEntity;
import dev.huff.hexaquot.persistence.HexahackGameEntity;
import dev.huff.hexaquot.persistence.HexaflowGameEntity;
import dev.huff.hexaquot.persistence.HexaskyGameEntity;
import dev.huff.hexaquot.persistence.PushSubscriptionEntity;
import dev.huff.hexaquot.persistence.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@ApplicationScoped
public class AdminService {
    private static final int ADMIN_PLAYERS_PAGE_SIZE = 10;
    private static final String SORT_ALPHABETICAL = "alphabetical";
    private static final String SORT_RECENT_GAME = "recent-game";
    private static final String SORT_GAMES_PLAYED = "games-played";

    @Inject
    ObjectMapper objectMapper;

    @Inject
    OverallStatsService overallStatsService;

    @Inject
    UserService userService;

    @Inject
    EntityManager entityManager;

    public AdminPlayersPageDto listPlayers(AppUser admin, String query, String sort, Integer page) {
        requireViewPlayers(admin);
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String normalizedSort = normalizeSort(sort);
        int pageIndex = page == null || page < 0 ? 0 : page;
        int totalPlayers = Math.toIntExact(countPlayers(normalizedQuery));
        int totalPages = Math.max(1, (int) Math.ceil(totalPlayers / (double) ADMIN_PLAYERS_PAGE_SIZE));
        int boundedPage = Math.min(pageIndex, totalPages - 1);
        List<AdminPlayerSummaryDto> players = fetchPlayerPage(
            normalizedQuery,
            normalizedSort,
            boundedPage * ADMIN_PLAYERS_PAGE_SIZE
        );
        return new AdminPlayersPageDto(players, boundedPage, ADMIN_PLAYERS_PAGE_SIZE, totalPlayers, totalPages, normalizedSort);
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
        List<AdminHexahackGameDto> hexahackGames = HexahackGameEntity.<HexahackGameEntity>find(
                "userId = ?1 order by puzzleDate desc, updatedAt desc", userId
            ).list().stream().map(this::hexahackGameDto).toList();

        return new AdminPlayerDetailDto(
            summary(user),
            user.googleSubject,
            user.createdAt,
            Math.toIntExact(PushSubscriptionEntity.count("userId", user.id)),
            overallStatsService.statsForUserId(user.id),
            games,
            hexahackGames
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
            request.bio(),
            user.inputHandPreference,
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
        games += HexahackGameEntity.delete("userId = ?1", userId);
        games += HexaskyGameEntity.delete("userId = ?1", userId);
        games += HexaflowGameEntity.delete("userId = ?1", userId);
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
        long started = GameEntity.count("userId", user.id) + HexahackGameEntity.count("userId", user.id)
            + HexaskyGameEntity.count("userId", user.id) + HexaflowGameEntity.count("userId", user.id);
        long won = GameEntity.count("userId = ?1 and status = ?2", user.id, GameStatus.WON)
            + HexahackGameEntity.count("userId = ?1 and status = ?2", user.id, HexahackDtos.Status.COMPLETED)
            + HexaskyGameEntity.count("userId = ?1 and status = ?2", user.id, dev.huff.hexaquot.game.HexaskyDtos.Status.WON)
            + HexaflowGameEntity.count("userId = ?1 and status = ?2", user.id, dev.huff.hexaquot.game.HexaflowDtos.GameStatus.COMPLETED);
        long lost = GameEntity.count("userId = ?1 and status = ?2", user.id, GameStatus.LOST)
            + HexaskyGameEntity.count("userId = ?1 and status = ?2", user.id, dev.huff.hexaquot.game.HexaskyDtos.Status.LOST);
        long completed = won + lost;
        int winRate = completed == 0 ? 0 : Math.toIntExact(Math.round((won * 100.0) / completed));
        String wordActivity = GameEntity.<GameEntity>find("userId = ?1 order by updatedAt desc", user.id)
            .firstResultOptional()
            .map(game -> game.updatedAt)
            .orElse(user.createdAt);
        String hackActivity = HexahackGameEntity.<HexahackGameEntity>find("userId = ?1 order by updatedAt desc", user.id)
            .firstResultOptional().map(game -> game.updatedAt).orElse(user.createdAt);
        String skyActivity = HexaskyGameEntity.<HexaskyGameEntity>find("userId = ?1 order by updatedAt desc", user.id)
            .firstResultOptional().map(game -> game.updatedAt).orElse(user.createdAt);
        String flowActivity = HexaflowGameEntity.<HexaflowGameEntity>find("userId = ?1 order by updatedAt desc", user.id)
            .firstResultOptional().map(game -> game.updatedAt).orElse(user.createdAt);
        String lastActivityAt = java.util.stream.Stream.of(wordActivity,hackActivity,skyActivity,flowActivity).max(String::compareTo).orElse(user.createdAt);
        return new AdminPlayerSummaryDto(
            user.id,
            user.email,
            user.displayName,
            user.nickname,
            user.profileEmoji,
            user.bio,
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

    private AdminHexahackGameDto hexahackGameDto(HexahackGameEntity game) {
        try {
            List<EventDto> log = objectMapper.readValue(game.eventLogJson, new TypeReference<>() {});
            return new AdminHexahackGameDto(game.id, game.puzzleDate, game.rulesVersion, game.solution, log,
                game.totalCost, game.wrongSubmissions, game.status, game.stealth, game.rank,
                game.createdAt, game.updatedAt, game.completedAt);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot parse Hexahack event log for admin view", error);
        }
    }

    private long countPlayers(String query) {
        return ((Number) entityManager
            .createNativeQuery("""
                SELECT COUNT(*)
                FROM users u
                WHERE :query = ''
                   OR LOWER(u.id) LIKE :query_pattern
                   OR LOWER(COALESCE(u.email, '')) LIKE :query_pattern
                   OR LOWER(COALESCE(u.display_name, '')) LIKE :query_pattern
                   OR LOWER(COALESCE(u.nickname, '')) LIKE :query_pattern
                   OR LOWER(COALESCE(u.google_subject, '')) LIKE :query_pattern
                """)
            .setParameter("query", query)
            .setParameter("query_pattern", "%" + query + "%")
            .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    private List<AdminPlayerSummaryDto> fetchPlayerPage(String query, String sort, int offset) {
        return entityManager
            .createNativeQuery("""
                SELECT
                  u.id,
                  u.email,
                  u.display_name,
                  u.nickname,
                  u.profile_emoji,
                  u.bio,
                  (u.google_subject IS NOT NULL) AS authenticated,
                  (a.user_id IS NOT NULL) AS admin,
                  COALESCE(u.star_available, false) AS star_available,
                  u.star_awarded_at,
                  u.star_used_at,
                  CAST(COUNT(g.id) AS integer) AS games_started,
                  CAST(COUNT(g.id) FILTER (WHERE g.status = 'WON') AS integer) AS won,
                  CAST(COUNT(g.id) FILTER (WHERE g.status = 'LOST') AS integer) AS lost,
                  COALESCE(MAX(g.updated_at), u.created_at) AS last_activity_at
                FROM users u
                LEFT JOIN (
                  SELECT id, user_id, status, updated_at FROM hexaword_games
                  UNION ALL
                  SELECT id, user_id, CASE WHEN status = 'COMPLETED' THEN 'WON' ELSE status END AS status, updated_at FROM hexahack_games
                  UNION ALL
                  SELECT id, user_id, status, updated_at FROM hexasky_games
                  UNION ALL
                  SELECT id, user_id, CASE WHEN status = 'COMPLETED' THEN 'WON' ELSE status END AS status, updated_at FROM hexaflow_games
                ) g ON g.user_id = u.id
                LEFT JOIN admin_users a ON a.user_id = u.id
                WHERE :query = ''
                   OR LOWER(u.id) LIKE :query_pattern
                   OR LOWER(COALESCE(u.email, '')) LIKE :query_pattern
                   OR LOWER(COALESCE(u.display_name, '')) LIKE :query_pattern
                   OR LOWER(COALESCE(u.nickname, '')) LIKE :query_pattern
                   OR LOWER(COALESCE(u.google_subject, '')) LIKE :query_pattern
                GROUP BY
                  u.id,
                  u.email,
                  u.display_name,
                  u.nickname,
                  u.profile_emoji,
                  u.bio,
                  u.google_subject,
                  a.user_id,
                  u.star_available,
                  u.star_awarded_at,
                  u.star_used_at,
                  u.created_at
                ORDER BY
                """ + playerOrderBy(sort) + """
                """)
            .setParameter("query", query)
            .setParameter("query_pattern", "%" + query + "%")
            .setMaxResults(ADMIN_PLAYERS_PAGE_SIZE)
            .setFirstResult(offset)
            .getResultStream()
            .map(row -> summaryRow((Object[]) row))
            .toList();
    }

    private String playerOrderBy(String sort) {
        String alphabetical = "LOWER(COALESCE(u.display_name, '')), LOWER(COALESCE(u.nickname, '')), u.id";
        if (SORT_RECENT_GAME.equals(sort)) {
            return "CASE WHEN COUNT(g.id) > 0 THEN 0 ELSE 1 END, COALESCE(MAX(g.updated_at), u.created_at) DESC, " + alphabetical;
        }
        if (SORT_GAMES_PLAYED.equals(sort)) {
            return "COUNT(g.id) DESC, " + alphabetical;
        }
        return alphabetical;
    }

    private AdminPlayerSummaryDto summaryRow(Object[] row) {
        int gamesStarted = intValue(row[11]);
        int won = intValue(row[12]);
        int lost = intValue(row[13]);
        int completed = won + lost;
        int winRate = completed == 0 ? 0 : Math.toIntExact(Math.round((won * 100.0) / completed));
        return new AdminPlayerSummaryDto(
            (String) row[0],
            (String) row[1],
            (String) row[2],
            (String) row[3],
            (String) row[4],
            (String) row[5],
            booleanValue(row[6]),
            booleanValue(row[7]),
            booleanValue(row[8]),
            (String) row[9],
            (String) row[10],
            gamesStarted,
            completed,
            won,
            lost,
            winRate,
            (String) row[14]
        );
    }

    private String normalizeSort(String sort) {
        if (SORT_RECENT_GAME.equals(sort) || SORT_GAMES_PLAYED.equals(sort)) {
            return sort;
        }
        return SORT_ALPHABETICAL;
    }

    private int intValue(Object value) {
        return ((Number) value).intValue();
    }

    private boolean booleanValue(Object value) {
        return Boolean.TRUE.equals(value);
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
        String bio,
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

    public record AdminPlayersPageDto(
        List<AdminPlayerSummaryDto> players,
        int page,
        int pageSize,
        int totalPlayers,
        int totalPages,
        String sort
    ) {
    }

    public record AdminPlayerDetailDto(
        AdminPlayerSummaryDto player,
        String googleSubject,
        String createdAt,
        int pushSubscriptions,
        StatsDto stats,
        List<AdminGameDto> games,
        List<AdminHexahackGameDto> hexahackGames
    ) {
    }

    public record AdminHexahackGameDto(
        String id,
        String puzzleDate,
        int rulesVersion,
        String solution,
        List<EventDto> log,
        int totalCost,
        int wrongSubmissions,
        HexahackDtos.Status status,
        Integer stealth,
        HexahackDtos.Rank rank,
        String createdAt,
        String updatedAt,
        String completedAt
    ) {}

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
        String bio,
        Boolean starAvailable,
        String starAwardedAt,
        String starUsedAt
    ) {
    }

    public record AdminDeleteResultDto(int users, int games, int pushSubscriptions, int adminRows) {
    }
}
