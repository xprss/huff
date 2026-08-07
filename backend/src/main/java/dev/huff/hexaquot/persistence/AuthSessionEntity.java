package dev.huff.hexaquot.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "auth_sessions")
public class AuthSessionEntity extends PanacheEntityBase {
    @Id
    public String id;

    @Column(name = "user_id", nullable = false)
    public String userId;

    @Column(name = "expires_at", nullable = false)
    public String expiresAt;
}
