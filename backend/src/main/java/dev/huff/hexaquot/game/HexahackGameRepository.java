package dev.huff.hexaquot.game;

import dev.huff.hexaquot.game.HexahackDtos.Status;
import dev.huff.hexaquot.persistence.HexahackGameEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class HexahackGameRepository {
    @Inject EntityManager entityManager;

    public void lockUser(String userId) {
        entityManager.createNativeQuery("SELECT id FROM users WHERE id = ?1 FOR UPDATE")
            .setParameter(1, userId)
            .getResultList();
    }

    public Optional<HexahackGameRecord> findByUserAndDate(String userId, String date) {
        return HexahackGameEntity.<HexahackGameEntity>find("userId = ?1 and puzzleDate = ?2", userId, date)
            .firstResultOptional().map(HexahackGameEntity::toRecord);
    }

    public Optional<HexahackGameRecord> findByUserAndDateForUpdate(String userId, String date) {
        return HexahackGameEntity.<HexahackGameEntity>find("userId = ?1 and puzzleDate = ?2", userId, date)
            .withLock(LockModeType.PESSIMISTIC_WRITE)
            .firstResultOptional().map(HexahackGameEntity::toRecord);
    }

    @Transactional
    public HexahackGameRecord create(String userId, String date, String solution) {
        String now = Instant.now().toString();
        HexahackGameRecord record = new HexahackGameRecord(
            UUID.randomUUID().toString(), userId, date, HexahackDailyGameProvider.RULES_VERSION, solution, "[]",
            0, 0, Status.IN_PROGRESS, null, null, now, now, null
        );
        HexahackGameEntity.fromRecord(record).persist();
        return record;
    }

    @Transactional
    public HexahackGameRecord update(HexahackGameRecord record) {
        HexahackGameEntity entity = HexahackGameEntity.findById(record.id());
        if (entity == null) throw new IllegalStateException("Cannot update missing Hexahack game " + record.id());
        entity.eventLogJson = record.eventLogJson();
        entity.totalCost = record.totalCost();
        entity.wrongSubmissions = record.wrongSubmissions();
        entity.status = record.status();
        entity.stealth = record.stealth();
        entity.rank = record.rank();
        entity.updatedAt = record.updatedAt();
        entity.completedAt = record.completedAt();
        return record;
    }

    public List<HexahackGameRecord> findCompletedByUser(String userId) {
        return HexahackGameEntity.<HexahackGameEntity>list(
            "userId = ?1 and status = ?2 order by puzzleDate asc", userId, Status.COMPLETED
        ).stream().map(HexahackGameEntity::toRecord).toList();
    }
}
