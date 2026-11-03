package dev.huff.hexaquot.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "daily_game_summaries")
public class DailyGameSummaryEntity extends PanacheEntityBase {
    @Id
    @Column(name = "summary_date", nullable = false, length = 10)
    public String summaryDate;

    @Column(nullable = false, columnDefinition = "text")
    public String summary;

    @Column(name = "generated_at", nullable = false, length = 255)
    public String generatedAt;
}
