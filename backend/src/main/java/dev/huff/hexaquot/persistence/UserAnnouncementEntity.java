package dev.huff.hexaquot.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "user_announcements", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "campaign"}))
public class UserAnnouncementEntity extends PanacheEntityBase {
    @Id public String id;
    @Column(name = "user_id", nullable = false) public String userId;
    @Column(name = "campaign", nullable = false, length = 64) public String campaign;
    @Column(name = "created_at", nullable = false) public String createdAt;
    @Column(name = "seen_at") public String seenAt;
}
