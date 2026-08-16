package dev.huff.hexaquot.game;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.auth.AppUser;
import dev.huff.hexaquot.game.HexahackDtos.CalendarNodeDto;
import dev.huff.hexaquot.game.HexahackDtos.EventDto;
import dev.huff.hexaquot.game.HexahackDtos.EventKind;
import dev.huff.hexaquot.game.HexahackDtos.GameDto;
import dev.huff.hexaquot.game.HexahackDtos.OverrideActionDto;
import dev.huff.hexaquot.game.HexahackDtos.OverrideRequest;
import dev.huff.hexaquot.game.HexahackDtos.OverrideResultDto;
import dev.huff.hexaquot.game.HexahackDtos.ProbeActionDto;
import dev.huff.hexaquot.game.HexahackDtos.ProbeRequest;
import dev.huff.hexaquot.game.HexahackDtos.ProbeResultDto;
import dev.huff.hexaquot.game.HexahackDtos.Rank;
import dev.huff.hexaquot.game.HexahackDtos.StatsDto;
import dev.huff.hexaquot.game.HexahackDtos.Status;
import dev.huff.hexaquot.game.HexahackDtos.SubmissionActionDto;
import dev.huff.hexaquot.game.HexahackDtos.SubmissionRequest;
import dev.huff.hexaquot.game.HexahackDtos.SubmissionResultDto;
import dev.huff.hexaquot.game.HexahackDtos.TodayDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class HexahackDailyGameService {
    @Inject DailyGameService dailyGameService;
    @Inject HexahackDailyGameProvider provider;
    @Inject HexahackGameRepository repository;
    @Inject ObjectMapper objectMapper;

    public TodayDto today(AppUser user) {
        String date = dailyGameService.todayDate();
        String solution = provider.solutionFor(date);
        return new TodayDto(date, HexahackDailyGameProvider.RULES_VERSION, HexahackDailyGameProvider.CODE_LENGTH,
            provider.freeClues(solution), repository.findByUserAndDate(user.id(), date).map(this::toDto).orElse(null));
    }

    @Transactional
    public ProbeActionDto probe(AppUser user, ProbeRequest request) {
        String requestId = provider.validateRequestId(request == null ? null : request.requestId());
        HexahackGameRecord record = currentForUpdate(user);
        List<EventDto> events = readEvents(record);
        EventDto replay = findRequest(events, requestId);
        if (replay != null) {
            if (replay.kind() != EventKind.PROBE) throw reusedRequestId();
            return new ProbeActionDto(toDto(record), replay.probe(), true);
        }
        requireInProgress(record);
        ProbeResultDto result = provider.probe(request, record.solution());
        String now = Instant.now().toString();
        events.add(new EventDto(events.size() + 1, EventKind.PROBE, now, result, null, null));
        HexahackGameRecord updated = updateProgress(record, events, record.totalCost() + result.cost(),
            record.wrongSubmissions(), record.overrideCount(), now);
        return new ProbeActionDto(toDto(repository.update(updated)), result, false);
    }

    @Transactional
    public SubmissionActionDto submit(AppUser user, SubmissionRequest request) {
        String requestId = provider.validateRequestId(request == null ? null : request.requestId());
        String code = provider.validateCode(request == null ? null : request.code());
        HexahackGameRecord record = currentForUpdate(user);
        List<EventDto> events = readEvents(record);
        EventDto replay = findRequest(events, requestId);
        if (replay != null) {
            if (replay.kind() != EventKind.SUBMISSION) throw reusedRequestId();
            return new SubmissionActionDto(toDto(record), replay.submission(), true);
        }
        requireInProgress(record);
        int correct = correctPositions(code, record.solution());
        boolean granted = correct == HexahackDailyGameProvider.CODE_LENGTH;
        SubmissionResultDto result = new SubmissionResultDto(requestId, code, correct, granted);
        String now = Instant.now().toString();
        events.add(new EventDto(events.size() + 1, EventKind.SUBMISSION, now, null, result, null));
        int errors = record.wrongSubmissions() + (granted ? 0 : 1);
        HexahackGameRecord updated;
        if (granted) {
            int stealth = stealth(record.totalCost(), errors);
            Rank rank = rank(stealth, errors, record.overrideCount());
            updated = new HexahackGameRecord(record.id(), record.userId(), record.puzzleDate(), record.rulesVersion(),
                record.solution(), writeEvents(events), record.totalCost(), errors, record.overrideCount(),
                Status.COMPLETED, stealth, rank, record.createdAt(), now, now);
        } else {
            updated = updateProgress(record, events, record.totalCost(), errors, record.overrideCount(), now);
        }
        return new SubmissionActionDto(toDto(repository.update(updated)), result, false);
    }

    @Transactional
    public OverrideActionDto override(AppUser user, OverrideRequest request) {
        String requestId = provider.validateRequestId(request == null ? null : request.requestId());
        int position = validateOverridePosition(request == null ? null : request.position());
        HexahackGameRecord record = currentForUpdate(user);
        List<EventDto> events = readEvents(record);
        EventDto replay = findRequest(events, requestId);
        if (replay != null) {
            if (replay.kind() != EventKind.OVERRIDE) throw reusedRequestId();
            return new OverrideActionDto(toDto(record), replay.override(), true);
        }
        requireInProgress(record);
        OverrideResultDto result = new OverrideResultDto(requestId, position,
            String.valueOf(record.solution().charAt(position - 1)), HexahackDailyGameProvider.OVERRIDE_COST);
        String now = Instant.now().toString();
        events.add(new EventDto(events.size() + 1, EventKind.OVERRIDE, now, null, null, result));
        HexahackGameRecord updated = updateProgress(record, events,
            record.totalCost() + HexahackDailyGameProvider.OVERRIDE_COST, record.wrongSubmissions(),
            record.overrideCount() + 1, now);
        return new OverrideActionDto(toDto(repository.update(updated)), result, false);
    }

    public StatsDto stats(AppUser user) {
        return statsForUserId(user.id());
    }

    public StatsDto statsForUserId(String userId) {
        List<HexahackGameRecord> records = repository.findCompletedByUser(userId);
        EnumMap<Rank, Integer> distribution = new EnumMap<>(Rank.class);
        for (Rank rank : Rank.values()) distribution.put(rank, 0);
        long totalStealth = 0;
        int bestStealth = records.isEmpty() ? 0 : Integer.MIN_VALUE;
        for (HexahackGameRecord record : records) {
            totalStealth += record.stealth();
            bestStealth = Math.max(bestStealth, record.stealth());
            distribution.merge(record.rank(), 1, Integer::sum);
        }
        double average = records.isEmpty() ? 0.0 : BigDecimal.valueOf(totalStealth)
            .divide(BigDecimal.valueOf(records.size()), 1, RoundingMode.HALF_UP).doubleValue();
        int maxStreak = maxDailyStreak(records);
        int currentStreak = currentDailyStreak(records, LocalDate.parse(dailyGameService.todayDate()));
        int maxNoOverride = maxNoOverrideStreak(records);
        int currentNoOverride = currentNoOverrideStreak(records, LocalDate.parse(dailyGameService.todayDate()));
        return new StatsDto(records.size(), average, bestStealth, Map.copyOf(distribution), currentStreak, maxStreak,
            currentNoOverride, maxNoOverride, calendar(records, LocalDate.parse(dailyGameService.todayDate())));
    }

    public static int stealth(int totalCost, int wrongSubmissions) {
        return 100 - 2 * totalCost - 5 * wrongSubmissions;
    }

    public static Rank rank(int stealth, int wrongSubmissions, int overrideCount) {
        if (overrideCount > 0) return Rank.TRACED;
        if (stealth >= 70 && wrongSubmissions == 0) return Rank.GHOST;
        if (stealth >= 55) return Rank.SHADOW;
        if (stealth >= 35) return Rank.BREACH;
        return Rank.TRACED;
    }

    private HexahackGameRecord currentForUpdate(AppUser user) {
        String date = dailyGameService.todayDate();
        repository.lockUser(user.id());
        return repository.findByUserAndDateForUpdate(user.id(), date)
            .orElseGet(() -> repository.create(user.id(), date, provider.solutionFor(date)));
    }

    private HexahackGameRecord updateProgress(HexahackGameRecord record, List<EventDto> events, int cost,
                                                int errors, int overrides, String now) {
        return new HexahackGameRecord(record.id(), record.userId(), record.puzzleDate(), record.rulesVersion(),
            record.solution(), writeEvents(events), cost, errors, overrides, Status.IN_PROGRESS, null, null,
            record.createdAt(), now, null);
    }

    private GameDto toDto(HexahackGameRecord record) {
        int currentStealth = record.status() == Status.COMPLETED
            ? record.stealth() : stealth(record.totalCost(), record.wrongSubmissions());
        Rank projected = record.status() == Status.COMPLETED
            ? record.rank() : rank(currentStealth, record.wrongSubmissions(), record.overrideCount());
        return new GameDto(record.puzzleDate(), record.rulesVersion(), record.status(),
            HexahackDailyGameProvider.CODE_LENGTH, readEvents(record), record.totalCost(), record.wrongSubmissions(),
            record.overrideCount(), currentStealth, projected, record.stealth(), record.rank(),
            record.status() == Status.COMPLETED ? record.solution() : null, record.completedAt());
    }

    private List<EventDto> readEvents(HexahackGameRecord record) {
        try {
            return new ArrayList<>(objectMapper.readValue(record.eventLogJson(), new TypeReference<List<EventDto>>() {}));
        } catch (Exception error) {
            throw new IllegalStateException("Cannot parse Hexahack event log", error);
        }
    }

    private String writeEvents(List<EventDto> events) {
        try { return objectMapper.writeValueAsString(events); }
        catch (Exception error) { throw new IllegalStateException("Cannot serialize Hexahack event log", error); }
    }

    private EventDto findRequest(List<EventDto> events, String requestId) {
        return events.stream().filter(event -> requestId.equals(event.requestId())).findFirst().orElse(null);
    }

    private void requireInProgress(HexahackGameRecord record) {
        if (record.status() != Status.IN_PROGRESS) {
            throw new WebApplicationException("L'accesso di oggi è già completato.", Response.Status.CONFLICT);
        }
    }

    private WebApplicationException reusedRequestId() {
        return new WebApplicationException("requestId già usato per un'altra operazione.", Response.Status.CONFLICT);
    }

    private int validateOverridePosition(Integer position) {
        if (position == null || position < 1 || position > HexahackDailyGameProvider.CODE_LENGTH) {
            throw new BadRequestException("Posizione non valida: usa un valore da 1 a 6.");
        }
        return position;
    }

    private int correctPositions(String code, String solution) {
        int correct = 0;
        for (int index = 0; index < HexahackDailyGameProvider.CODE_LENGTH; index++) {
            if (code.charAt(index) == solution.charAt(index)) correct++;
        }
        return correct;
    }

    private int maxDailyStreak(List<HexahackGameRecord> records) {
        int max = 0;
        int current = 0;
        LocalDate previous = null;
        for (HexahackGameRecord record : records) {
            LocalDate date = LocalDate.parse(record.puzzleDate());
            current = previous != null && ChronoUnit.DAYS.between(previous, date) == 1 ? current + 1 : 1;
            max = Math.max(max, current);
            previous = date;
        }
        return max;
    }

    private int currentDailyStreak(List<HexahackGameRecord> records, LocalDate today) {
        if (records.isEmpty()) return 0;
        LocalDate last = LocalDate.parse(records.get(records.size() - 1).puzzleDate());
        if (last.isBefore(today.minusDays(1))) return 0;
        int streak = 1;
        for (int index = records.size() - 2; index >= 0; index--) {
            LocalDate previous = LocalDate.parse(records.get(index).puzzleDate());
            if (ChronoUnit.DAYS.between(previous, last) != 1) break;
            streak++;
            last = previous;
        }
        return streak;
    }

    private int maxNoOverrideStreak(List<HexahackGameRecord> records) {
        int max = 0;
        int current = 0;
        LocalDate previous = null;
        for (HexahackGameRecord record : records) {
            LocalDate date = LocalDate.parse(record.puzzleDate());
            boolean consecutive = previous != null && ChronoUnit.DAYS.between(previous, date) == 1;
            current = record.overrideCount() == 0 ? (consecutive ? current + 1 : 1) : 0;
            max = Math.max(max, current);
            previous = date;
        }
        return max;
    }

    private int currentNoOverrideStreak(List<HexahackGameRecord> records, LocalDate today) {
        if (records.isEmpty()) return 0;
        int index = records.size() - 1;
        LocalDate last = LocalDate.parse(records.get(index).puzzleDate());
        if (last.isBefore(today.minusDays(1)) || records.get(index).overrideCount() > 0) return 0;
        int streak = 1;
        for (index -= 1; index >= 0; index--) {
            HexahackGameRecord previousRecord = records.get(index);
            LocalDate previous = LocalDate.parse(previousRecord.puzzleDate());
            if (previousRecord.overrideCount() > 0 || ChronoUnit.DAYS.between(previous, last) != 1) break;
            streak++;
            last = previous;
        }
        return streak;
    }

    private List<CalendarNodeDto> calendar(List<HexahackGameRecord> records, LocalDate today) {
        Map<String, HexahackGameRecord> byDate = new HashMap<>();
        records.forEach(record -> byDate.put(record.puzzleDate(), record));
        List<CalendarNodeDto> nodes = new ArrayList<>();
        for (int daysAgo = 29; daysAgo >= 0; daysAgo--) {
            String date = today.minusDays(daysAgo).toString();
            HexahackGameRecord record = byDate.get(date);
            nodes.add(record == null
                ? new CalendarNodeDto(date, false, null, null, null, null, null)
                : new CalendarNodeDto(date, true, record.stealth(), record.rank(), record.totalCost(),
                    record.wrongSubmissions(), record.overrideCount() > 0));
        }
        return List.copyOf(nodes);
    }
}
