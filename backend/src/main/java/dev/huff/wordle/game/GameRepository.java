package dev.huff.wordle.game;

import dev.huff.wordle.persistence.GameEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class GameRepository {
    public Optional<GameRecord> findByUserAndDate(String userId, String puzzleDate) {
        return GameEntity.<GameEntity>find("userId = ?1 and puzzleDate = ?2", userId, puzzleDate)
            .firstResultOptional()
            .map(GameEntity::toRecord);
    }

    @Transactional
    public GameRecord create(String userId, String puzzleDate, String solution) {
        String now = Instant.now().toString();
        GameRecord record = new GameRecord(
            UUID.randomUUID().toString(),
            userId,
            puzzleDate,
            solution,
            "[]",
            GameStatus.IN_PROGRESS,
            now,
            now,
            null
        );
        GameEntity.fromRecord(record).persist();
        return record;
    }

    @Transactional
    public GameRecord update(GameRecord record) {
        GameEntity entity = GameEntity.findById(record.id());
        if (entity == null) {
            throw new IllegalStateException("Cannot update missing game " + record.id());
        }
        entity.guessesJson = record.guessesJson();
        entity.status = record.status();
        entity.updatedAt = record.updatedAt();
        entity.completedAt = record.completedAt();
        return record;
    }

    public List<GameRecord> findCompletedByUser(String userId) {
        return GameEntity.<GameEntity>list(
                "userId = ?1 and status in ?2 order by puzzleDate asc",
                userId,
                List.of(GameStatus.WON, GameStatus.LOST)
            )
            .stream()
            .map(GameEntity::toRecord)
            .toList();
    }

    public List<GameRecord> findCompleted() {
        return GameEntity.<GameEntity>list(
                "status in ?1 order by puzzleDate asc",
                List.of(GameStatus.WON, GameStatus.LOST)
            )
            .stream()
            .map(GameEntity::toRecord)
            .toList();
    }

    public int countPlayers() {
        Long total = GameEntity.getEntityManager()
            .createQuery("SELECT COUNT(DISTINCT g.userId) FROM GameEntity g", Long.class)
            .getSingleResult();
        return total.intValue();
    }

    public int countGamesStarted() {
        return Math.toIntExact(GameEntity.count());
    }
}
