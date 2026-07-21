package dev.huff.wordle.persistence;

import dev.huff.wordle.auth.AppUser;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity extends PanacheEntityBase {
    @Id
    public String id;

    @Column(name = "google_subject", unique = true)
    public String googleSubject;

    @Column(name = "email")
    public String email;

    @Column(name = "display_name")
    public String displayName;

    @Column(name = "created_at", nullable = false)
    public String createdAt;

    public AppUser toAppUser(boolean authenticated) {
        return new AppUser(id, email, displayName, authenticated);
    }
}
