package dev.huff.hexaquot.game;

import dev.huff.hexaquot.game.HexastarDtos.Status;
import dev.huff.hexaquot.persistence.HexastarGameEntity;
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
public class HexastarGameRepository {
    @Inject EntityManager entityManager;

    public void lockUser(String userId) {
        entityManager.createNativeQuery("SELECT id FROM users WHERE id = ?1 FOR UPDATE")
            .setParameter(1, userId).getResultList();
    }

    public Optional<HexastarGameRecord> findByUserAndDate(String userId, String date) {
        return HexastarGameEntity.<HexastarGameEntity>find("userId = ?1 and puzzleDate = ?2", userId, date)
            .firstResultOptional().map(HexastarGameEntity::toRecord);
    }

    public Optional<HexastarGameRecord> findByUserAndDateForUpdate(String userId, String date) {
        return HexastarGameEntity.<HexastarGameEntity>find("userId = ?1 and puzzleDate = ?2", userId, date)
            .withLock(LockModeType.PESSIMISTIC_WRITE).firstResultOptional().map(HexastarGameEntity::toRecord);
    }

    @Transactional
    public HexastarGameRecord create(String userId, String date, String solution, String solutionSyllablesJson) {
        String now = Instant.now().toString();
        HexastarGameRecord record = new HexastarGameRecord(UUID.randomUUID().toString(), userId, date,
            HexastarDailyGameProvider.RULES_VERSION, solution, solutionSyllablesJson, "[]", Status.IN_PROGRESS,
            now, now, null);
        HexastarGameEntity.fromRecord(record).persist();
        return record;
    }

    @Transactional
    public HexastarGameRecord update(HexastarGameRecord record) {
        HexastarGameEntity entity = HexastarGameEntity.findById(record.id());
        if (entity == null) throw new IllegalStateException("Cannot update missing Hexastar game " + record.id());
        entity.attemptsJson = record.attemptsJson();
        entity.status = record.status();
        entity.updatedAt = record.updatedAt();
        entity.completedAt = record.completedAt();
        return record;
    }

    public List<HexastarGameRecord> findCompletedByUser(String userId) {
        return HexastarGameEntity.<HexastarGameEntity>list("userId = ?1 and status in ?2 order by puzzleDate asc",
                userId, List.of(Status.WON, Status.LOST))
            .stream().map(HexastarGameEntity::toRecord).toList();
    }
}
