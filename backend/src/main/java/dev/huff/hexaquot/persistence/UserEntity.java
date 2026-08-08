package dev.huff.hexaquot.persistence;

import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.auth.AdminPrivileges;
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

    @Column(name = "nickname", length = 30)
    public String nickname;

    @Column(name = "profile_emoji", length = 16)
    public String profileEmoji;

    @Column(name = "bio", length = 200)
    public String bio;

    @Column(name = "input_hand_preference", length = 5)
    public String inputHandPreference = "RIGHT";

    @Column(name = "created_at", nullable = false)
    public String createdAt;

    @Column(name = "star_available", nullable = false, columnDefinition = "boolean default false")
    public Boolean starAvailable = false;

    @Column(name = "star_awarded_at")
    public String starAwardedAt;

    @Column(name = "star_used_at")
    public String starUsedAt;

    public AppUser toAppUser(boolean authenticated) {
        return toAppUser(authenticated, null);
    }

    public AppUser toAppUser(boolean authenticated, AdminPrivileges admin) {
        return new AppUser(id, email, displayName, nickname, profileEmoji, bio, inputHandPreference, authenticated, admin);
    }
}
