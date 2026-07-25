package dev.huff.hexaquot.persistence;

import dev.huff.hexaquot.game.GameRecord;
import dev.huff.hexaquot.game.GameMode;
import dev.huff.hexaquot.game.GameStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "games",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "puzzle_date"})
)
public class GameEntity extends PanacheEntityBase {
    @Id
    public String id;

    @Column(name = "user_id", nullable = false)
    public String userId;

    @Column(name = "puzzle_date", nullable = false)
    public String puzzleDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode")
    public GameMode mode;

    @Column(name = "solution", nullable = false)
    public String solution;

    @Column(name = "guesses_json", nullable = false, columnDefinition = "TEXT")
    public String guessesJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public GameStatus status;

    @Column(name = "mouse_tile_index")
    public Integer mouseTileIndex;

    @Column(name = "mouse_revealed")
    public Boolean mouseRevealed;

    @Column(name = "kitten_unlocked")
    public Boolean kittenUnlocked;

    @Column(name = "kitten_used_at")
    public String kittenUsedAt;

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
        entity.mode = record.mode();
        entity.solution = record.solution();
        entity.guessesJson = record.guessesJson();
        entity.status = record.status();
        entity.mouseTileIndex = record.mouseTileIndex();
        entity.mouseRevealed = record.mouseRevealed();
        entity.kittenUnlocked = record.kittenUnlocked();
        entity.kittenUsedAt = record.kittenUsedAt();
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
            mode == null ? GameMode.CLASSIC : mode,
            solution,
            guessesJson,
            status,
            mouseTileIndex,
            mouseRevealed == null || mouseRevealed,
            kittenUnlocked != null && kittenUnlocked,
            kittenUsedAt,
            createdAt,
            updatedAt,
            completedAt
        );
    }
}
