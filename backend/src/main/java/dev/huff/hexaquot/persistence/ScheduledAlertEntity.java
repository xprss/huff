package dev.huff.hexaquot.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "scheduled_alerts")
public class ScheduledAlertEntity extends PanacheEntityBase {
    @Id public String id;
    @Column(nullable = false, length = 10) public String type;
    @Column(nullable = false, length = 120) public String title;
    @Column(nullable = false, length = 1000) public String body;
    @Column(length = 255) public String url;
    @Column(name = "scheduled_at", nullable = false) public String scheduledAt;
    @Column(name = "created_at", nullable = false) public String createdAt;
    @Column(name = "dispatched_at") public String dispatchedAt;
}
