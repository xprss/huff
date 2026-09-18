package dev.huff.hexaquot.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

/** Only validated completions are persisted; exploration stays on the player's device. */
@Entity
@Table(name = "hexaeco_games", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "puzzle_date"}))
public class HexaecoGameEntity extends PanacheEntityBase {
    @Id public String id;
    @Column(name = "user_id", nullable = false) public String userId;
    @Column(name = "puzzle_date", nullable = false, length = 10) public String puzzleDate;
    @Column(name = "rules_version", nullable = false) public int rulesVersion;
    @Column(name = "request_id", nullable = false, length = 128) public String requestId;
    @Column(name = "moves_json", nullable = false, columnDefinition = "TEXT") public String movesJson;
    @Column(name = "status", nullable = false, length = 20) public String status;
    @Column(name = "created_at", nullable = false) public String createdAt;
    @Column(name = "updated_at", nullable = false) public String updatedAt;
    @Column(name = "completed_at", nullable = false) public String completedAt;
}
