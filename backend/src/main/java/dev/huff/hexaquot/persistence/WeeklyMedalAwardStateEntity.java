package dev.huff.hexaquot.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "weekly_medal_award_state")
public class WeeklyMedalAwardStateEntity extends PanacheEntityBase {
    public static final String SINGLETON_ID = "weekly-medals";

    @Id
    public String id;

    @Column(name = "first_eligible_week_start", nullable = false, length = 10)
    public String firstEligibleWeekStart;

    @Column(name = "last_processed_week_start", nullable = false, length = 10)
    public String lastProcessedWeekStart;
}
