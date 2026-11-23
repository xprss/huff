package dev.huff.hexaquot.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "hexatrap_generation_attempts")
public class HexatrapGenerationAttemptEntity extends PanacheEntityBase {
    @Id public String id;
    @Column(name = "puzzle_date", nullable = false, length = 10) public String puzzleDate;
    @Column(name = "run_id", nullable = false) public String runId;
    @Column(nullable = false) public int attempt;
    @Column(nullable = false, length = 20) public String phase;
    @Column(nullable = false) public boolean ok;
    @Column(name = "issues_json", nullable = false, columnDefinition = "TEXT") public String issuesJson = "[]";
    @Column(name = "metrics_json", columnDefinition = "TEXT") public String metricsJson;
    @Column(name = "draft_json", columnDefinition = "TEXT") public String draftJson;
    @Column(name = "duration_ms", nullable = false) public long durationMs;
    @Column(name = "created_at", nullable = false) public String createdAt;
}
