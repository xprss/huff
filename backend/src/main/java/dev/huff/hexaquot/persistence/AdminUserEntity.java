package dev.huff.hexaquot.persistence;

import dev.huff.hexaquot.auth.AdminPrivileges;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_users")
public class AdminUserEntity extends PanacheEntityBase {
    @Id
    @Column(name = "user_id")
    public String userId;

    @Column(name = "can_view_players", nullable = false, columnDefinition = "boolean default false")
    public Boolean canViewPlayers = false;

    @Column(name = "can_view_player_details", nullable = false, columnDefinition = "boolean default false")
    public Boolean canViewPlayerDetails = false;

    @Column(name = "can_manage_players", nullable = false, columnDefinition = "boolean default false")
    public Boolean canManagePlayers = false;

    @Column(name = "can_manage_admins", nullable = false, columnDefinition = "boolean default false")
    public Boolean canManageAdmins = false;

    @Column(name = "can_manage_hexaflow_puzzles", nullable = false, columnDefinition = "boolean default false")
    public Boolean canManageHexaflowPuzzles = false;

    @Column(name = "created_at", nullable = false)
    public String createdAt;

    @Column(name = "updated_at", nullable = false)
    public String updatedAt;

    public AdminPrivileges toPrivileges() {
        return new AdminPrivileges(
            Boolean.TRUE.equals(canViewPlayers),
            Boolean.TRUE.equals(canViewPlayerDetails),
            Boolean.TRUE.equals(canManagePlayers),
            Boolean.TRUE.equals(canManageAdmins),
            Boolean.TRUE.equals(canManageHexaflowPuzzles)
        );
    }
}
