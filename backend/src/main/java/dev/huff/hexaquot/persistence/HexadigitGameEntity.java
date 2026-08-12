package dev.huff.hexaquot.persistence;

import dev.huff.hexaquot.game.GameStatus;
import dev.huff.hexaquot.game.HexadigitGameRecord;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "hexadigit_games", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "puzzle_date"}))
public class HexadigitGameEntity extends PanacheEntityBase {
    @Id public String id;
    @Column(name = "user_id", nullable = false) public String userId;
    @Column(name = "puzzle_date", nullable = false, length = 10) public String puzzleDate;
    @Column(name = "solution", nullable = false, length = 6) public String solution;
    @Column(name = "guesses_json", nullable = false, columnDefinition = "TEXT") public String guessesJson;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false) public GameStatus status;
    @Column(name = "created_at", nullable = false) public String createdAt;
    @Column(name = "updated_at", nullable = false) public String updatedAt;
    @Column(name = "completed_at") public String completedAt;

    public static HexadigitGameEntity fromRecord(HexadigitGameRecord record) {
        HexadigitGameEntity entity = new HexadigitGameEntity();
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

    public HexadigitGameRecord toRecord() {
        return new HexadigitGameRecord(id, userId, puzzleDate, solution, guessesJson, status, createdAt, updatedAt, completedAt);
    }
}
