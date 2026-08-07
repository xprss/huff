package dev.huff.hexaquot.persistence;

import dev.huff.hexaquot.leaderboard.MedalType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "weekly_medals", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "week_start"}))
public class WeeklyMedalEntity extends PanacheEntityBase {
    @Id
    public String id;

    @Column(name = "user_id", nullable = false)
    public String userId;

    @Column(name = "week_start", nullable = false, length = 10)
    public String weekStart;

    @Enumerated(EnumType.STRING)
    @Column(name = "medal", nullable = false, length = 10)
    public MedalType medal;

    @Column(name = "awarded_at", nullable = false)
    public String awardedAt;
}
