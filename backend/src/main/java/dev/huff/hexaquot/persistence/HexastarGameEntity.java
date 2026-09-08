package dev.huff.hexaquot.persistence;

import dev.huff.hexaquot.game.HexastarDtos.Status;
import dev.huff.hexaquot.game.HexastarGameRecord;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "hexastar_games", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "puzzle_date"}))
public class HexastarGameEntity extends PanacheEntityBase {
    @Id public String id;
    @Column(name = "user_id", nullable = false) public String userId;
    @Column(name = "puzzle_date", nullable = false) public String puzzleDate;
    @Column(name = "rules_version", nullable = false) public Integer rulesVersion;
    @Column(nullable = false) public String solution;
    @Column(name = "solution_syllables_json", nullable = false, columnDefinition = "TEXT") public String solutionSyllablesJson;
    @Column(name = "attempts_json", nullable = false, columnDefinition = "TEXT") public String attemptsJson;
    @Enumerated(EnumType.STRING) @Column(nullable = false) public Status status;
    @Column(name = "created_at", nullable = false) public String createdAt;
    @Column(name = "updated_at", nullable = false) public String updatedAt;
    @Column(name = "completed_at") public String completedAt;

    public static HexastarGameEntity fromRecord(HexastarGameRecord record) {
        HexastarGameEntity entity = new HexastarGameEntity();
        entity.id = record.id();
        entity.userId = record.userId();
        entity.puzzleDate = record.puzzleDate();
        entity.rulesVersion = record.rulesVersion();
        entity.solution = record.solution();
        entity.solutionSyllablesJson = record.solutionSyllablesJson();
        entity.attemptsJson = record.attemptsJson();
        entity.status = record.status();
        entity.createdAt = record.createdAt();
        entity.updatedAt = record.updatedAt();
        entity.completedAt = record.completedAt();
        return entity;
    }

    public HexastarGameRecord toRecord() {
        return new HexastarGameRecord(id, userId, puzzleDate, rulesVersion, solution, solutionSyllablesJson,
            attemptsJson, status, createdAt, updatedAt, completedAt);
    }
}
