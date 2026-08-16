package dev.huff.hexaquot.push;

import dev.huff.hexaquot.persistence.UserAnnouncementEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class AnnouncementService {
    public static final String HEXAHACK_LAUNCH = "HEXAHACK_LAUNCH";

    public List<String> pending(String userId) {
        return UserAnnouncementEntity.<UserAnnouncementEntity>list(
            "userId = ?1 and seenAt is null order by createdAt", userId
        ).stream().map(row -> row.campaign).toList();
    }

    @Transactional
    public void markSeen(String userId, String campaign) {
        if (!HEXAHACK_LAUNCH.equals(campaign)) throw new NotFoundException("Annuncio non trovato.");
        UserAnnouncementEntity row = UserAnnouncementEntity.<UserAnnouncementEntity>find(
            "userId = ?1 and campaign = ?2", userId, campaign
        ).firstResult();
        // Missing means either already outside the launch audience or removed: the operation remains idempotent.
        if (row != null && row.seenAt == null) row.seenAt = Instant.now().toString();
    }
}
