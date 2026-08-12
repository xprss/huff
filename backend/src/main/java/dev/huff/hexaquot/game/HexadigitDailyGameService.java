package dev.huff.hexaquot.game;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.auth.AppUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class HexadigitDailyGameService {
    @Inject DailyGameService dailyGameService;
    @Inject HexadigitDailyGameProvider provider;
    @Inject HexadigitGameRepository repository;
    @Inject ObjectMapper objectMapper;

    public HexadigitTodayDto today(AppUser user) {
        String date = dailyGameService.todayDate();
        return new HexadigitTodayDto(date, repository.findByUserAndDate(user.id(), date).map(this::toDto).orElse(null));
    }

    @Transactional
    public HexadigitGameDto guess(AppUser user, String rawGuess) {
        String guess = dailyGameService.normalizeAndValidate(provider, rawGuess);
        String date = dailyGameService.todayDate();
        HexadigitGameRecord record = repository.findByUserAndDate(user.id(), date)
            .orElseGet(() -> repository.create(user.id(), date, provider.solutionFor(date)));
        if (record.status() != GameStatus.IN_PROGRESS) {
            throw new WebApplicationException("La partita di oggi e' gia' conclusa.", Response.Status.CONFLICT);
        }
        List<HexadigitGuessResult> guesses = new ArrayList<>(readGuesses(record));
        if (guesses.size() >= provider.maxAttempts()) {
            throw new WebApplicationException("Tentativi esauriti.", Response.Status.CONFLICT);
        }
        guesses.add(dailyGameService.score(provider, guess, record.solution()));
        GameStatus status = dailyGameService.statusAfter(provider, guess, record.solution(), guesses);
        String now = Instant.now().toString();
        return toDto(repository.update(new HexadigitGameRecord(
            record.id(), record.userId(), record.puzzleDate(), record.solution(), writeGuesses(guesses), status,
            record.createdAt(), now, status == GameStatus.IN_PROGRESS ? null : now
        )));
    }

    public StatsDto stats(AppUser user) { return statsForUserId(user.id()); }

    public StatsDto statsForUserId(String userId) {
        return StatsCalculator.calculate(repository.findCompletedByUser(userId).stream().map(record ->
            new StatsCalculator.CompletedGame(record.puzzleDate(), record.status(), readGuesses(record).size())
        ).toList());
    }

    public List<StatsCalculator.CompletedGame> completedForUser(String userId) {
        return repository.findCompletedByUser(userId).stream().map(record ->
            new StatsCalculator.CompletedGame(record.puzzleDate(), record.status(), readGuesses(record).size())
        ).toList();
    }

    private HexadigitGameDto toDto(HexadigitGameRecord record) {
        return new HexadigitGameDto(record.puzzleDate(), record.status(), provider.maxAttempts(), provider.answerLength(),
            readGuesses(record), record.status() == GameStatus.IN_PROGRESS ? null : record.solution());
    }

    private List<HexadigitGuessResult> readGuesses(HexadigitGameRecord record) {
        try { return objectMapper.readValue(record.guessesJson(), new TypeReference<>() {}); }
        catch (Exception error) { throw new IllegalStateException("Cannot parse Hexadigit guesses", error); }
    }

    private String writeGuesses(List<HexadigitGuessResult> guesses) {
        try { return objectMapper.writeValueAsString(guesses); }
        catch (Exception error) { throw new IllegalStateException("Cannot serialize Hexadigit guesses", error); }
    }
}
