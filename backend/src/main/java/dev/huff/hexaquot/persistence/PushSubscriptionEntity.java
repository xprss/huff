package dev.huff.hexaquot.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "push_subscriptions",
    uniqueConstraints = @UniqueConstraint(columnNames = "endpoint")
)
public class PushSubscriptionEntity extends PanacheEntityBase {
    @Id
    public String id;

    @Column(name = "user_id", nullable = false)
    public String userId;

    @Column(name = "endpoint", nullable = false, length = 2048)
    public String endpoint;

    @Column(name = "p256dh", nullable = false, length = 512)
    public String p256dh;

    @Column(name = "auth", nullable = false, length = 256)
    public String auth;

    @Column(name = "last_notified_puzzle_date")
    public String lastNotifiedPuzzleDate;

    @Column(name = "last_reminded_puzzle_date")
    public String lastRemindedPuzzleDate;

    @Column(name = "created_at", nullable = false)
    public String createdAt;

    @Column(name = "updated_at", nullable = false)
    public String updatedAt;
}
