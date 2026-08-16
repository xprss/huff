package dev.huff.hexaquot.game;

import dev.huff.hexaquot.game.HexaskyDtos.Status;
import dev.huff.hexaquot.persistence.HexaskyGameEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class HexaskyGameRepository {
    @Inject EntityManager entityManager;
    public void lockUser(String userId) { entityManager.createNativeQuery("SELECT id FROM users WHERE id = ?1 FOR UPDATE").setParameter(1, userId).getResultList(); }
    public Optional<HexaskyGameRecord> findByUserAndDate(String userId, String date) { return HexaskyGameEntity.<HexaskyGameEntity>find("userId = ?1 and puzzleDate = ?2", userId,date).firstResultOptional().map(HexaskyGameEntity::toRecord); }
    public Optional<HexaskyGameRecord> findByUserAndDateForUpdate(String userId, String date) { return HexaskyGameEntity.<HexaskyGameEntity>find("userId = ?1 and puzzleDate = ?2",userId,date).withLock(LockModeType.PESSIMISTIC_WRITE).firstResultOptional().map(HexaskyGameEntity::toRecord); }
    @Transactional public HexaskyGameRecord create(String userId, String date, List<Integer> solution, String solutionJson) {
        String now=Instant.now().toString(); HexaskyGameRecord r=new HexaskyGameRecord(UUID.randomUUID().toString(),userId,date,HexaskyDailyGameProvider.RULES_VERSION,solutionJson,null,"[]",0,Status.IN_PROGRESS,now,now,null); HexaskyGameEntity.fromRecord(r).persist(); return r;
    }
    @Transactional public HexaskyGameRecord update(HexaskyGameRecord r) { HexaskyGameEntity e=HexaskyGameEntity.findById(r.id()); if(e==null) throw new IllegalStateException("Cannot update missing Hexasky game "+r.id()); e.proposalJson=r.proposalJson(); e.eventLogJson=r.eventLogJson(); e.checksUsed=r.checksUsed(); e.status=r.status(); e.updatedAt=r.updatedAt(); e.completedAt=r.completedAt(); return r; }
    public List<HexaskyGameRecord> findCompletedByUser(String userId) { return HexaskyGameEntity.<HexaskyGameEntity>list("userId = ?1 and status in ?2 order by puzzleDate asc",userId,List.of(Status.WON,Status.LOST)).stream().map(HexaskyGameEntity::toRecord).toList(); }
}
