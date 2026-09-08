package dev.huff.hexaquot.game;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.game.HexastarDailyGameProvider.WordEntry;
import dev.huff.hexaquot.game.HexastarDtos.AttemptDto;
import dev.huff.hexaquot.game.HexastarDtos.GameDto;
import dev.huff.hexaquot.game.HexastarDtos.GuessActionDto;
import dev.huff.hexaquot.game.HexastarDtos.GuessRequest;
import dev.huff.hexaquot.game.HexastarDtos.Status;
import dev.huff.hexaquot.game.HexastarDtos.TodayDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class HexastarDailyGameService {
    @Inject DailyGameService dailyGameService;
    @Inject HexastarDailyGameProvider provider;
    @Inject HexastarGameRepository repository;
    @Inject ObjectMapper objectMapper;

    public TodayDto today(AppUser user) {
        String date = dailyGameService.todayDate();
        WordEntry daily = provider.solutionFor(date);
        return new TodayDto(date, HexastarDailyGameProvider.RULES_VERSION, HexastarDailyGameProvider.MAX_ATTEMPTS, daily.lengths(),
            repository.findByUserAndDate(user.id(), date).map(this::toDto).orElse(null));
    }

    @Transactional
    public GuessActionDto guess(AppUser user, GuessRequest request) {
        String requestId = provider.validateRequestId(request == null ? null : request.requestId());
        String date = dailyGameService.todayDate();
        repository.lockUser(user.id());
        HexastarGameRecord existing = repository.findByUserAndDateForUpdate(user.id(), date).orElse(null);
        List<AttemptDto> attempts = existing == null ? new ArrayList<>() : readAttempts(existing);
        AttemptDto replay = attempts.stream().filter(attempt -> requestId.equals(attempt.requestId())).findFirst().orElse(null);
        if (replay != null) return new GuessActionDto(toDto(existing), replay, true);
        if (existing != null && existing.status() != Status.IN_PROGRESS) {
            throw new WebApplicationException("La partita di oggi è già conclusa.", Response.Status.CONFLICT);
        }

        WordEntry daily = existing == null
            ? provider.solutionFor(date)
            : new WordEntry(existing.solution(), readSyllables(existing.solutionSyllablesJson()));
        List<String> syllables = provider.normalizeAndValidateGuess(
            request == null ? null : request.syllables(), daily.lengths());
        if (attempts.stream().anyMatch(attempt -> attempt.syllables().equals(syllables))) {
            throw new BadRequestException("Hai già inserito questa parola.");
        }

        HexastarGameRecord record = existing == null
            ? repository.create(user.id(), date, daily.word(), writeSyllables(daily.syllables()))
            : existing;
        String now = Instant.now().toString();
        AttemptDto attempt = new AttemptDto(attempts.size() + 1, requestId, syllables,
            provider.score(syllables, daily.syllables()), now);
        attempts.add(attempt);
        Status status = syllables.equals(daily.syllables())
            ? Status.WON
            : attempts.size() >= HexastarDailyGameProvider.MAX_ATTEMPTS ? Status.LOST : Status.IN_PROGRESS;
        HexastarGameRecord updated = new HexastarGameRecord(record.id(), record.userId(), record.puzzleDate(),
            record.rulesVersion(), record.solution(), record.solutionSyllablesJson(), writeAttempts(attempts), status,
            record.createdAt(), now, status == Status.IN_PROGRESS ? null : now);
        updated = repository.update(updated);
        return new GuessActionDto(toDto(updated), attempt, false);
    }

    public StatsDto stats(AppUser user) {
        return statsForUserId(user.id());
    }

    public StatsDto statsForUserId(String userId) {
        return StatsCalculator.calculate(completedForUser(userId));
    }

    public List<StatsCalculator.CompletedGame> completedForUser(String userId) {
        return repository.findCompletedByUser(userId).stream()
            .map(record -> new StatsCalculator.CompletedGame(record.puzzleDate(),
                record.status() == Status.WON ? GameStatus.WON : GameStatus.LOST,
                readAttempts(record).size()))
            .toList();
    }

    private GameDto toDto(HexastarGameRecord record) {
        List<String> solution = readSyllables(record.solutionSyllablesJson());
        return new GameDto(record.puzzleDate(), record.rulesVersion(), record.status(),
            HexastarDailyGameProvider.MAX_ATTEMPTS, solution.stream().map(String::length).toList(),
            readAttempts(record), record.status() == Status.IN_PROGRESS ? null : solution, record.completedAt());
    }

    private List<AttemptDto> readAttempts(HexastarGameRecord record) {
        try {
            return new ArrayList<>(objectMapper.readValue(record.attemptsJson(), new TypeReference<List<AttemptDto>>() {}));
        } catch (Exception error) {
            throw new IllegalStateException("Cannot parse Hexastar attempts", error);
        }
    }

    private List<String> readSyllables(String json) {
        try {
            return List.copyOf(objectMapper.readValue(json, new TypeReference<List<String>>() {}));
        } catch (Exception error) {
            throw new IllegalStateException("Cannot parse Hexastar syllables", error);
        }
    }

    private String writeAttempts(List<AttemptDto> attempts) {
        try {
            return objectMapper.writeValueAsString(attempts);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot write Hexastar attempts", error);
        }
    }

    private String writeSyllables(List<String> syllables) {
        try {
            return objectMapper.writeValueAsString(syllables);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot write Hexastar syllables", error);
        }
    }
}
