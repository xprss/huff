package dev.huff.wordle.game;

import dev.huff.wordle.persistence.Database;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class GameRepository {
    @Inject
    Database database;

    public Optional<GameRecord> findByUserAndDate(String userId, String puzzleDate) {
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement("""
                 SELECT id, user_id, puzzle_date, solution, guesses_json, status, created_at, updated_at, completed_at
                 FROM games
                 WHERE user_id = ? AND puzzle_date = ?
                 """)) {
            statement.setString(1, userId);
            statement.setString(2, puzzleDate);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (Exception error) {
            throw new IllegalStateException("Cannot load game", error);
        }
    }

    public GameRecord create(String userId, String puzzleDate, String solution) {
        String now = Instant.now().toString();
        GameRecord record = new GameRecord(
            UUID.randomUUID().toString(),
            userId,
            puzzleDate,
            solution,
            "[]",
            GameStatus.IN_PROGRESS,
            now,
            now,
            null
        );
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement("""
                 INSERT INTO games (id, user_id, puzzle_date, solution, guesses_json, status, created_at, updated_at, completed_at)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                 """)) {
            bind(record, statement);
            statement.executeUpdate();
            return record;
        } catch (Exception error) {
            return findByUserAndDate(userId, puzzleDate)
                .orElseThrow(() -> new IllegalStateException("Cannot create game", error));
        }
    }

    public GameRecord update(GameRecord record) {
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement("""
                 UPDATE games
                 SET guesses_json = ?, status = ?, updated_at = ?, completed_at = ?
                 WHERE id = ?
                 """)) {
            statement.setString(1, record.guessesJson());
            statement.setString(2, record.status().name());
            statement.setString(3, record.updatedAt());
            statement.setString(4, record.completedAt());
            statement.setString(5, record.id());
            statement.executeUpdate();
            return record;
        } catch (Exception error) {
            throw new IllegalStateException("Cannot update game", error);
        }
    }

    public List<GameRecord> findCompletedByUser(String userId) {
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement("""
                 SELECT id, user_id, puzzle_date, solution, guesses_json, status, created_at, updated_at, completed_at
                 FROM games
                 WHERE user_id = ? AND status IN ('WON', 'LOST')
                 ORDER BY puzzle_date ASC
                 """)) {
            statement.setString(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<GameRecord> records = new ArrayList<>();
                while (resultSet.next()) {
                    records.add(map(resultSet));
                }
                return records;
            }
        } catch (Exception error) {
            throw new IllegalStateException("Cannot load completed games", error);
        }
    }

    private void bind(GameRecord record, PreparedStatement statement) throws Exception {
        statement.setString(1, record.id());
        statement.setString(2, record.userId());
        statement.setString(3, record.puzzleDate());
        statement.setString(4, record.solution());
        statement.setString(5, record.guessesJson());
        statement.setString(6, record.status().name());
        statement.setString(7, record.createdAt());
        statement.setString(8, record.updatedAt());
        statement.setString(9, record.completedAt());
    }

    private GameRecord map(ResultSet resultSet) throws Exception {
        return new GameRecord(
            resultSet.getString("id"),
            resultSet.getString("user_id"),
            resultSet.getString("puzzle_date"),
            resultSet.getString("solution"),
            resultSet.getString("guesses_json"),
            GameStatus.valueOf(resultSet.getString("status")),
            resultSet.getString("created_at"),
            resultSet.getString("updated_at"),
            resultSet.getString("completed_at")
        );
    }
}
