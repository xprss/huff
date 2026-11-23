package dev.huff.hexaquot.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "hexatrap_games", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "puzzle_date"}))
public class HexatrapGameEntity extends PanacheEntityBase {
    @Id public String id;
    @Column(name = "user_id", nullable = false) public String userId;
    @Column(name = "puzzle_date", nullable = false, length = 10) public String puzzleDate;
    @Column(name = "rules_version", nullable = false) public int rulesVersion;
    @Column(nullable = false, length = 20) public String status;
    @Column(length = 20) public String outcome;
    @Column(name = "minutes_used", nullable = false) public int minutesUsed;
    @Column(nullable = false) public int actions;
    @Column(nullable = false) public long points;
    @Column(name = "verdict_json", columnDefinition = "TEXT") public String verdictJson;
    @Column(name = "created_at", nullable = false) public String createdAt;
    @Column(name = "updated_at", nullable = false) public String updatedAt;
    @Column(name = "completed_at") public String completedAt;
}
