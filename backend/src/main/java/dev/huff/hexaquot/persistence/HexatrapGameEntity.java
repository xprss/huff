package dev.huff.hexaquot.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "hexatrap_games", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "puzzle_date"}))
public class HexatrapGameEntity extends PanacheEntityBase {
    @Id public String id;
    @Column(name="user_id", nullable=false) public String userId;
    @Column(name="puzzle_date", nullable=false, length=10) public String puzzleDate;
    @Column(name="rules_version", nullable=false) public int rulesVersion;
    @Column(name="puzzle_json", nullable=false, columnDefinition="TEXT") public String puzzleJson;
    @Column(name="hypotheses_json", nullable=false, columnDefinition="TEXT") public String hypothesesJson;
    @Column(name="evidence_json", nullable=false, columnDefinition="TEXT") public String evidenceJson = "[]";
    @Column(nullable=false, length=20) public String status;
    @Column(nullable=false) public long questions;
    @Column(name="wrong_submissions", nullable=false) public long wrongSubmissions;
    @Column(nullable=false) public long points;
    @Column(name="event_count", nullable=false) public long eventCount;
    @Column(name="number_requested", nullable=false) public boolean numberRequested;
    @Column(name="created_at", nullable=false) public String createdAt;
    @Column(name="updated_at", nullable=false) public String updatedAt;
    @Column(name="completed_at") public String completedAt;
}
