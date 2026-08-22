package dev.huff.hexaquot.game;

import dev.huff.hexaquot.game.HexasquareDtos.Status;
import dev.huff.hexaquot.persistence.HexasquareGameEntity;
import dev.huff.hexaquot.persistence.HexasquareSimulationEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class HexasquareGameRepository {
    @Inject EntityManager entityManager;

    public void lockUser(String userId) {
        entityManager.createNativeQuery("SELECT id FROM users WHERE id = ?1 FOR UPDATE").setParameter(1,userId).getResultList();
    }
    public Optional<HexasquareGameRecord> findByUserAndDate(String userId,String date) {
        return HexasquareGameEntity.<HexasquareGameEntity>find("userId = ?1 and puzzleDate = ?2",userId,date)
            .firstResultOptional().map(HexasquareGameEntity::toRecord);
    }
    public Optional<HexasquareGameRecord> findByUserAndDateForUpdate(String userId,String date) {
        return HexasquareGameEntity.<HexasquareGameEntity>find("userId = ?1 and puzzleDate = ?2",userId,date)
            .withLock(LockModeType.PESSIMISTIC_WRITE).firstResultOptional().map(HexasquareGameEntity::toRecord);
    }
    public HexasquareGameRecord create(String userId,String date,String puzzleJson) {
        String now=Instant.now().toString();
        HexasquareGameEntity entity=new HexasquareGameEntity();
        entity.id=UUID.randomUUID().toString(); entity.userId=userId; entity.puzzleDate=date;
        entity.rulesVersion=HexasquareDailyGameProvider.RULES_VERSION; entity.puzzleJson=puzzleJson;
        entity.status=Status.IN_PROGRESS; entity.simulationsCount=0; entity.createdAt=now; entity.updatedAt=now;
        entity.persist(); return entity.toRecord();
    }
    public HexasquareGameRecord update(HexasquareGameRecord record) {
        HexasquareGameEntity entity=HexasquareGameEntity.findById(record.id());
        if(entity==null) throw new IllegalStateException("Cannot update missing Hexasquare game "+record.id());
        entity.placementsJson=record.placementsJson(); entity.canonicalPathsJson=record.canonicalPathsJson();
        entity.status=record.status(); entity.simulationsCount=record.simulationsCount(); entity.usedCells=record.usedCells();
        entity.remainingCells=record.remainingCells(); entity.updatedAt=record.updatedAt(); entity.completedAt=record.completedAt();
        return entity.toRecord();
    }
    public Optional<HexasquareSimulationRecord> findSimulation(String gameId,String requestId) {
        return HexasquareSimulationEntity.<HexasquareSimulationEntity>find("gameId = ?1 and requestId = ?2",gameId,requestId)
            .firstResultOptional().map(HexasquareSimulationEntity::toRecord);
    }
    public HexasquareSimulationRecord createSimulation(String gameId,String requestId,String placementsJson,
                                                        String outcomeJson,boolean successful,String createdAt) {
        HexasquareSimulationEntity entity=new HexasquareSimulationEntity(); entity.id=UUID.randomUUID().toString();
        entity.gameId=gameId; entity.requestId=requestId; entity.placementsJson=placementsJson;
        entity.outcomeJson=outcomeJson; entity.successful=successful; entity.createdAt=createdAt; entity.persist();
        return entity.toRecord();
    }
    public List<HexasquareGameRecord> findByUser(String userId) {
        return HexasquareGameEntity.<HexasquareGameEntity>list("userId = ?1 order by puzzleDate asc",userId)
            .stream().map(HexasquareGameEntity::toRecord).toList();
    }
    public List<HexasquareGameRecord> findCompletedByUser(String userId) {
        return HexasquareGameEntity.<HexasquareGameEntity>list("userId = ?1 and status = ?2 order by puzzleDate asc",userId,Status.COMPLETED)
            .stream().map(HexasquareGameEntity::toRecord).toList();
    }
}
