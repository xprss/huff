package dev.huff.hexaquot.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "push_campaign_deliveries", uniqueConstraints = @UniqueConstraint(columnNames = {"subscription_id", "campaign"}))
public class PushCampaignDeliveryEntity extends PanacheEntityBase {
    @Id public String id;
    @Column(name = "subscription_id", nullable = false) public String subscriptionId;
    @Column(name = "campaign", nullable = false, length = 64) public String campaign;
    @Column(name = "created_at", nullable = false) public String createdAt;
    @Column(name = "sent_at") public String sentAt;
    @Column(name = "attempts", nullable = false) public Integer attempts = 0;
    @Column(name = "last_attempt_at") public String lastAttemptAt;
}
