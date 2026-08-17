package dev.huff.hexaquot.persistence;

import dev.huff.hexaquot.game.HexahackDtos.Rank;
import dev.huff.hexaquot.game.HexahackDtos.Status;
import dev.huff.hexaquot.game.HexahackGameRecord;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "hexahack_games", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "puzzle_date"}))
public class HexahackGameEntity extends PanacheEntityBase {
    @Id public String id;
    @Column(name = "user_id", nullable = false) public String userId;
    @Column(name = "puzzle_date", nullable = false, length = 10) public String puzzleDate;
    @Column(name = "rules_version", nullable = false) public int rulesVersion;
    @Column(name = "solution", nullable = false, length = 6) public String solution;
    @Column(name = "event_log_json", nullable = false, columnDefinition = "TEXT") public String eventLogJson;
    @Column(name = "total_cost", nullable = false) public int totalCost;
    @Column(name = "wrong_submissions", nullable = false) public int wrongSubmissions;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 20) public Status status;
    @Column(name = "stealth") public Integer stealth;
    @Enumerated(EnumType.STRING) @Column(name = "rank", length = 20) public Rank rank;
    @Column(name = "created_at", nullable = false) public String createdAt;
    @Column(name = "updated_at", nullable = false) public String updatedAt;
    @Column(name = "completed_at") public String completedAt;

    public static HexahackGameEntity fromRecord(HexahackGameRecord record) {
        HexahackGameEntity entity = new HexahackGameEntity();
        entity.id = record.id();
        entity.userId = record.userId();
        entity.puzzleDate = record.puzzleDate();
        entity.rulesVersion = record.rulesVersion();
        entity.solution = record.solution();
        entity.eventLogJson = record.eventLogJson();
        entity.totalCost = record.totalCost();
        entity.wrongSubmissions = record.wrongSubmissions();
        entity.status = record.status();
        entity.stealth = record.stealth();
        entity.rank = record.rank();
        entity.createdAt = record.createdAt();
        entity.updatedAt = record.updatedAt();
        entity.completedAt = record.completedAt();
        return entity;
    }

    public HexahackGameRecord toRecord() {
        return new HexahackGameRecord(id, userId, puzzleDate, rulesVersion, solution, eventLogJson, totalCost,
            wrongSubmissions, status, stealth, rank, createdAt, updatedAt, completedAt);
    }
}
