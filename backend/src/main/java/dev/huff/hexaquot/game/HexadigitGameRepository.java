package dev.huff.hexaquot.game;

import dev.huff.hexaquot.persistence.HexadigitGameEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class HexadigitGameRepository {
    public Optional<HexadigitGameRecord> findByUserAndDate(String userId, String date) {
        return HexadigitGameEntity.<HexadigitGameEntity>find("userId = ?1 and puzzleDate = ?2", userId, date)
            .firstResultOptional().map(HexadigitGameEntity::toRecord);
    }

    @Transactional
    public HexadigitGameRecord create(String userId, String date, String solution) {
        String now = Instant.now().toString();
        HexadigitGameRecord record = new HexadigitGameRecord(
            UUID.randomUUID().toString(), userId, date, solution, "[]", GameStatus.IN_PROGRESS, now, now, null
        );
        HexadigitGameEntity.fromRecord(record).persist();
        return record;
    }

    @Transactional
    public HexadigitGameRecord update(HexadigitGameRecord record) {
        HexadigitGameEntity entity = HexadigitGameEntity.findById(record.id());
        if (entity == null) throw new IllegalStateException("Cannot update missing Hexadigit game " + record.id());
        entity.guessesJson = record.guessesJson();
        entity.status = record.status();
        entity.updatedAt = record.updatedAt();
        entity.completedAt = record.completedAt();
        return record;
    }

    public List<HexadigitGameRecord> findCompletedByUser(String userId) {
        return HexadigitGameEntity.<HexadigitGameEntity>list(
            "userId = ?1 and status in ?2 order by puzzleDate asc", userId, List.of(GameStatus.WON, GameStatus.LOST)
        ).stream().map(HexadigitGameEntity::toRecord).toList();
    }

    public List<HexadigitGameRecord> findCompleted() {
        return HexadigitGameEntity.<HexadigitGameEntity>list(
            "status in ?1 order by puzzleDate asc", List.of(GameStatus.WON, GameStatus.LOST)
        ).stream().map(HexadigitGameEntity::toRecord).toList();
    }
}
