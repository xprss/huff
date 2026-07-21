package dev.huff.wordle.persistence;

import dev.huff.wordle.game.GameRecord;
import dev.huff.wordle.game.GameStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "games",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "puzzle_date"}),
    indexes = {
        @Index(name = "idx_games_user_date", columnList = "user_id,puzzle_date"),
        @Index(name = "idx_games_user_status", columnList = "user_id,status")
    }
)
public class GameEntity extends PanacheEntityBase {
    @Id
    public String id;

    @Column(name = "user_id", nullable = false)
    public String userId;

    @Column(name = "puzzle_date", nullable = false)
    public String puzzleDate;

    @Column(name = "solution", nullable = false)
    public String solution;

    @Column(name = "guesses_json", nullable = false, columnDefinition = "TEXT")
    public String guessesJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public GameStatus status;

    @Column(name = "created_at", nullable = false)
    public String createdAt;

    @Column(name = "updated_at", nullable = false)
    public String updatedAt;

    @Column(name = "completed_at")
    public String completedAt;

    public static GameEntity fromRecord(GameRecord record) {
        GameEntity entity = new GameEntity();
        entity.id = record.id();
        entity.userId = record.userId();
        entity.puzzleDate = record.puzzleDate();
        entity.solution = record.solution();
        entity.guessesJson = record.guessesJson();
        entity.status = record.status();
        entity.createdAt = record.createdAt();
        entity.updatedAt = record.updatedAt();
        entity.completedAt = record.completedAt();
        return entity;
    }

    public GameRecord toRecord() {
        return new GameRecord(
            id,
            userId,
            puzzleDate,
            solution,
            guessesJson,
            status,
            createdAt,
            updatedAt,
            completedAt
        );
    }
}
