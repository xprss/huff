package dev.huff.hexaquot.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "seen_alerts")
@IdClass(SeenAlertEntity.Key.class)
public class SeenAlertEntity extends PanacheEntityBase {
    @Id @Column(name = "user_id") public String userId;
    @Id @Column(name = "alert_id") public String alertId;
    @Column(name = "seen_at", nullable = false) public String seenAt;

    public static class Key implements Serializable {
        public String userId;
        public String alertId;
        public Key() {}
        public Key(String userId, String alertId) { this.userId = userId; this.alertId = alertId; }
        @Override public boolean equals(Object other) {
            return other instanceof Key key && java.util.Objects.equals(userId, key.userId) && java.util.Objects.equals(alertId, key.alertId);
        }
        @Override public int hashCode() { return java.util.Objects.hash(userId, alertId); }
    }
}
