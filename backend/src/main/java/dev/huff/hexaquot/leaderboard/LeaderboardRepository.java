package dev.huff.hexaquot.leaderboard;

import dev.huff.hexaquot.game.GameStatus;
import dev.huff.hexaquot.persistence.UserEntity;
import io.quarkus.hibernate.orm.panache.Panache;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class LeaderboardRepository {
    public enum Board { HEXAWORD, HEXADIGIT, OVERALL }

    public List<PlayerScore> winnerScores(String startDate, String endDate) {
        return winnerScores(Board.HEXAWORD, startDate, endDate);
    }

    public List<PlayerScore> winnerScores(Board board, String startDate, String endDate) {
        Map<String, MutableScore> scores = new HashMap<>();
        if (board == Board.HEXAWORD || board == Board.OVERALL) merge(scores, rows("GameEntity", startDate, endDate), true);
        if (board == Board.HEXADIGIT || board == Board.OVERALL) merge(scores, rows("HexadigitGameEntity", startDate, endDate), false);
        if (scores.isEmpty()) return List.of();
        Set<String> ids = scores.keySet();
        Map<String, UserEntity> users = new HashMap<>();
        UserEntity.<UserEntity>list("id in ?1", ids).forEach(user -> users.put(user.id, user));
        return scores.entrySet().stream()
            .filter(entry -> users.containsKey(entry.getKey()))
            .map(entry -> new PlayerScore(users.get(entry.getKey()), entry.getValue().hexawordWins,
                entry.getValue().hexadigitWins, entry.getValue().lastWinAt))
            .toList();
    }

    private List<Object[]> rows(String entity, String startDate, String endDate) {
        String query = "SELECT g.userId, COUNT(g), MAX(COALESCE(g.completedAt, g.updatedAt, g.createdAt)) "
            + "FROM " + entity + " g WHERE g.status = ?1";
        if (startDate != null) query += " AND g.puzzleDate >= ?2 AND g.puzzleDate < ?3";
        query += " GROUP BY g.userId";
        var typed = Panache.getEntityManager().createQuery(query, Object[].class).setParameter(1, GameStatus.WON);
        if (startDate != null) typed.setParameter(2, startDate).setParameter(3, endDate);
        return typed.getResultList();
    }

    private void merge(Map<String, MutableScore> scores, List<Object[]> rows, boolean hexaword) {
        for (Object[] row : rows) {
            MutableScore score = scores.computeIfAbsent((String) row[0], ignored -> new MutableScore());
            int wins = ((Long) row[1]).intValue();
            if (hexaword) score.hexawordWins += wins; else score.hexadigitWins += wins;
            String timestamp = (String) row[2];
            if (score.lastWinAt == null || timestamp != null && timestamp.compareTo(score.lastWinAt) > 0) score.lastWinAt = timestamp;
        }
    }

    private static class MutableScore { int hexawordWins; int hexadigitWins; String lastWinAt; }

    public record PlayerScore(UserEntity user, int hexawordWins, int hexadigitWins, String lastWinAt) {
        public int wins() { return hexawordWins + hexadigitWins; }
    }
}
