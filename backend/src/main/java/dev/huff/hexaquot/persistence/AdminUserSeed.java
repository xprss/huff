package dev.huff.hexaquot.persistence;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

import java.time.Instant;

@ApplicationScoped
public class AdminUserSeed {
    public static final String FIRST_ADMIN_ID =
        "google:VincenzoSagristano:2399532d11393886cfb8d77c1207d666933aa31cedd6b10f92247b79e5d74e96";

    @Transactional
    void onStart(@Observes StartupEvent event) {
        AdminUserEntity admin = AdminUserEntity.findById(FIRST_ADMIN_ID);
        String now = Instant.now().toString();
        if (admin == null) {
            admin = new AdminUserEntity();
            admin.userId = FIRST_ADMIN_ID;
            admin.createdAt = now;
            admin.canViewPlayers = true;
            admin.canViewPlayerDetails = true;
            admin.canManagePlayers = true;
            admin.canManageAdmins = true;
            admin.updatedAt = now;
            admin.persist();
            return;
        }

        admin.canViewPlayers = true;
        admin.canViewPlayerDetails = true;
        admin.canManagePlayers = true;
        admin.canManageAdmins = true;
        admin.updatedAt = now;
    }
}
