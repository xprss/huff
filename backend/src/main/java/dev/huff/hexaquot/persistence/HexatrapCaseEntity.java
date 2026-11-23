package dev.huff.hexaquot.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "hexatrap_cases")
public class HexatrapCaseEntity extends PanacheEntityBase {
    @Id @Column(name = "puzzle_date", length = 10) public String puzzleDate;
    @Column(name = "rules_version", nullable = false) public int rulesVersion;
    @Column(nullable = false, length = 20) public String status;
    @Column(name = "lease_until_ms") public Long leaseUntilMs;
    @Column(nullable = false) public int difficulty;
    @Column(nullable = false, length = 30) public String format;
    @Column public String title;
    @Column(name = "case_json", columnDefinition = "TEXT") public String caseJson;
    @Column(name = "metrics_json", columnDefinition = "TEXT") public String metricsJson;
    @Column(name = "warnings_json", nullable = false, columnDefinition = "TEXT") public String warningsJson = "[]";
    @Column(nullable = false) public int attempts;
    @Column(length = 80) public String model;
    @Column(name = "last_error", columnDefinition = "TEXT") public String lastError;
    @Column(name = "generated_at") public String generatedAt;
    @Column(name = "created_at", nullable = false) public String createdAt;
    @Column(name = "updated_at", nullable = false) public String updatedAt;
}
