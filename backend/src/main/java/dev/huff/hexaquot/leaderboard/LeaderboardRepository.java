package dev.huff.hexaquot.leaderboard;

import dev.huff.hexaquot.game.GameStatus;
import dev.huff.hexaquot.persistence.GameEntity;
import dev.huff.hexaquot.persistence.UserEntity;
import io.quarkus.hibernate.orm.panache.Panache;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class LeaderboardRepository {
    public List<PlayerScore> winnerScores(String startDate, String endDate) {
        String query = "SELECT g.userId, COUNT(g), MAX(COALESCE(g.completedAt, g.updatedAt, g.createdAt)) "
            + "FROM GameEntity g WHERE g.status = ?1";
        if (startDate != null) {
            query += " AND g.puzzleDate >= ?2 AND g.puzzleDate < ?3";
        }
        query += " GROUP BY g.userId";

        var typedQuery = Panache.getEntityManager().createQuery(query, Object[].class)
            .setParameter(1, GameStatus.WON);
        if (startDate != null) {
            typedQuery.setParameter(2, startDate);
            typedQuery.setParameter(3, endDate);
        }
        List<Object[]> rows = typedQuery.getResultList();
        if (rows.isEmpty()) {
            return List.of();
        }

        Set<String> userIds = rows.stream().map(row -> (String) row[0]).collect(Collectors.toSet());
        Map<String, UserEntity> users = new HashMap<>();
        UserEntity.<UserEntity>list("id in ?1", userIds).forEach(user -> users.put(user.id, user));
        return rows.stream()
            .map(row -> new PlayerScore(users.get((String) row[0]), ((Long) row[1]).intValue(), (String) row[2]))
            .filter(score -> score.user() != null)
            .toList();
    }

    public record PlayerScore(UserEntity user, int wins, String lastWinAt) {
    }
}
