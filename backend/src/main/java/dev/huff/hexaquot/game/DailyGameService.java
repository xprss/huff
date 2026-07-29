package dev.huff.hexaquot.game;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.huff.hexaquot.auth.AppUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DailyGameService {
    public static final int MAX_ATTEMPTS = 6;

    @ConfigProperty(name = "app.game.timezone")
    String timezone;

    @ConfigProperty(name = "app.word.seed")
    String wordSeed;

    @Inject
    WordsProvider wordsProvider;

    @Inject
    GameRepository gameRepository;

    @Inject
    ObjectMapper objectMapper;

    public TodayGameDto today(AppUser user) {
        String date = todayDate();
        return new TodayGameDto(
            date,
            availableModes(),
            gameRepository.findByUserAndDate(user.id(), date)
                .map(record -> toDto(record, false))
                .orElse(null)
        );
    }

    @Transactional
    public GameDto selectMode(AppUser user, GameMode mode) {
        if (mode == null) {
            throw new BadRequestException("Scegli una modalità di gioco.");
        }

        String date = todayDate();
        GameRecord record = gameRepository.findByUserAndDate(user.id(), date)
            .orElseGet(() -> gameRepository.create(user.id(), date, solutionFor(date), mode));

        List<GuessResult> guesses = readGuesses(record);
        if (!guesses.isEmpty() && record.mode() != mode) {
            throw new WebApplicationException(
                "La modalità non puo' essere cambiata dopo il primo tentativo.",
                Response.Status.CONFLICT
            );
        }

        if (record.mode() == mode) {
            return toDto(record, false);
        }

        String now = java.time.Instant.now().toString();
        GameRecord updated = new GameRecord(
            record.id(),
            record.userId(),
            record.puzzleDate(),
            mode,
            record.solution(),
            record.guessesJson(),
            record.status(),
            null,
            mode == GameMode.CLASSIC,
            false,
            null,
            record.createdAt(),
            now,
            record.completedAt()
        );
        return toDto(gameRepository.update(updated), false);
    }

    @Transactional
    public GameDto guess(AppUser user, String rawGuess) {
        String guess = wordsProvider.normalize(rawGuess);
        if (guess.length() != WordsProvider.WORD_LENGTH) {
            throw new BadRequestException("La parola deve essere di 6 lettere.");
        }
        if (!wordsProvider.contains(guess)) {
            throw new BadRequestException("Parola non presente nella lista.");
        }

        GameRecord record = todayRecord(user);
        if (record.status() != GameStatus.IN_PROGRESS) {
            throw new WebApplicationException("La partita di oggi e' gia' conclusa.", Response.Status.CONFLICT);
        }

        List<GuessResult> guesses = readGuesses(record);
        if (guesses.size() >= MAX_ATTEMPTS) {
            throw new WebApplicationException("Tentativi esauriti.", Response.Status.CONFLICT);
        }
        if (record.mode() == GameMode.MISCHIEVOUS_MOUSE && alreadyGuessed(guesses, guess)) {
            throw new BadRequestException("Hai gia' inserito questa parola.");
        }

        GuessResult scoredGuess = score(guess, record.solution());
        Integer mouseTileIndex = record.mouseTileIndex();
        boolean mouseRevealed = record.mouseRevealed();
        boolean kittenUnlocked = record.kittenUnlocked();

        if (record.mode() == GameMode.MISCHIEVOUS_MOUSE && countCorrect(scoredGuess) >= 3) {
            kittenUnlocked = true;
        }

        if (record.mode() == GameMode.MISCHIEVOUS_MOUSE && guesses.isEmpty()) {
            mouseTileIndex = chooseMouseTileIndex(record);
            mouseRevealed = false;
            scoredGuess = hideTile(scoredGuess, mouseTileIndex);
        }

        guesses.add(scoredGuess);
        GameStatus status = GameStatus.IN_PROGRESS;
        if (guess.equals(record.solution())) {
            status = GameStatus.WON;
        } else if (guesses.size() == MAX_ATTEMPTS) {
            status = GameStatus.LOST;
        }

        String now = java.time.Instant.now().toString();
        GameRecord updated = new GameRecord(
            record.id(),
            record.userId(),
            record.puzzleDate(),
            record.mode(),
            record.solution(),
            writeGuesses(guesses),
            status,
            mouseTileIndex,
            mouseRevealed,
            kittenUnlocked,
            record.kittenUsedAt(),
            record.createdAt(),
            now,
            status == GameStatus.IN_PROGRESS ? null : now
        );
        return toDto(gameRepository.update(updated), status != GameStatus.IN_PROGRESS);
    }

    @Transactional
    public GameDto useKitten(AppUser user) {
        GameRecord record = todayRecord(user);
        if (record.mode() != GameMode.MISCHIEVOUS_MOUSE) {
            throw new WebApplicationException("Il gattino non e' disponibile in questa modalità.", Response.Status.CONFLICT);
        }
        if (!record.kittenUnlocked() || record.kittenUsedAt() != null || record.mouseRevealed()) {
            throw new WebApplicationException("Il gattino non puo' essere usato ora.", Response.Status.CONFLICT);
        }
        if (record.mouseTileIndex() == null) {
            throw new WebApplicationException("Nessun topolino da rimuovere.", Response.Status.CONFLICT);
        }

        List<GuessResult> guesses = readGuesses(record);
        if (guesses.isEmpty()) {
            throw new WebApplicationException("Nessun tentativo da rivelare.", Response.Status.CONFLICT);
        }

        GuessResult firstGuess = guesses.get(0);
        GuessResult revealedFirstGuess = revealHiddenTile(firstGuess, record.solution(), record.mouseTileIndex());
        List<GuessResult> updatedGuesses = new ArrayList<>(guesses);
        updatedGuesses.set(0, revealedFirstGuess);

        String now = java.time.Instant.now().toString();
        GameRecord updated = new GameRecord(
            record.id(),
            record.userId(),
            record.puzzleDate(),
            record.mode(),
            record.solution(),
            writeGuesses(updatedGuesses),
            record.status(),
            record.mouseTileIndex(),
            true,
            record.kittenUnlocked(),
            now,
            record.createdAt(),
            now,
            record.completedAt()
        );
        return toDto(gameRepository.update(updated), record.status() != GameStatus.IN_PROGRESS);
    }

    public StatsDto stats(AppUser user) {
        List<GameRecord> records = gameRepository.findCompletedByUser(user.id());
        int won = 0;
        Map<Integer, Integer> distribution = new LinkedHashMap<>();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            distribution.put(attempt, 0);
        }

        int currentStreak = 0;
        int maxStreak = 0;
        int runningStreak = 0;
        LocalDate expectedNextDate = null;

        for (GameRecord record : records) {
            if (record.status() == GameStatus.WON) {
                won++;
                int attempts = readGuesses(record).size();
                distribution.put(attempts, distribution.getOrDefault(attempts, 0) + 1);
            }

            LocalDate date = LocalDate.parse(record.puzzleDate());
            if (record.status() == GameStatus.WON && (expectedNextDate == null || date.equals(expectedNextDate))) {
                runningStreak++;
            } else if (record.status() == GameStatus.WON) {
                runningStreak = 1;
            } else {
                runningStreak = 0;
            }
            maxStreak = Math.max(maxStreak, runningStreak);
            currentStreak = runningStreak;
            expectedNextDate = date.plusDays(1);
        }

        return new StatsDto(records.size(), won, records.size() - won, currentStreak, maxStreak, distribution);
    }

    public GlobalStatsDto globalStats() {
        List<GameRecord> completedRecords = gameRepository.findCompleted();
        int won = 0;
        Map<Integer, Integer> distribution = new LinkedHashMap<>();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            distribution.put(attempt, 0);
        }

        for (GameRecord record : completedRecords) {
            if (record.status() == GameStatus.WON) {
                won++;
                int attempts = readGuesses(record).size();
                distribution.put(attempts, distribution.getOrDefault(attempts, 0) + 1);
            }
        }

        return new GlobalStatsDto(
            gameRepository.countPlayers(),
            gameRepository.countGamesStarted(),
            completedRecords.size(),
            won,
            completedRecords.size() - won,
            distribution
        );
    }

    public GuessResult score(String guess, String solution) {
        List<TileResult> tiles = new ArrayList<>();
        Map<Character, Integer> remaining = new HashMap<>();
        for (int index = 0; index < WordsProvider.WORD_LENGTH; index++) {
            char guessLetter = guess.charAt(index);
            char solutionLetter = solution.charAt(index);
            if (guessLetter == solutionLetter) {
                tiles.add(new TileResult(String.valueOf(guessLetter), TileState.CORRECT));
            } else {
                tiles.add(null);
                remaining.put(solutionLetter, remaining.getOrDefault(solutionLetter, 0) + 1);
            }
        }

        for (int index = 0; index < WordsProvider.WORD_LENGTH; index++) {
            if (tiles.get(index) != null) {
                continue;
            }
            char guessLetter = guess.charAt(index);
            int available = remaining.getOrDefault(guessLetter, 0);
            if (available > 0) {
                tiles.set(index, new TileResult(String.valueOf(guessLetter), TileState.PRESENT));
                remaining.put(guessLetter, available - 1);
            } else {
                tiles.set(index, new TileResult(String.valueOf(guessLetter), TileState.ABSENT));
            }
        }
        return new GuessResult(guess, List.copyOf(tiles));
    }

    private GameRecord todayRecord(AppUser user) {
        String date = todayDate();
        return gameRepository.findByUserAndDate(user.id(), date)
            .orElseThrow(() -> new BadRequestException("Scegli una modalità di gioco prima di iniziare."));
    }

    public String todayDate() {
        return LocalDate.now(ZoneId.of(timezone)).toString();
    }

    private List<GameModeDto> availableModes() {
        return Arrays.stream(GameMode.values())
            .map(mode -> new GameModeDto(mode, mode.label()))
            .toList();
    }

    private String solutionFor(String puzzleDate) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((wordSeed + ":" + puzzleDate).getBytes(StandardCharsets.UTF_8));
            int index = new BigInteger(1, hash).mod(BigInteger.valueOf(wordsProvider.words().size())).intValue();
            return wordsProvider.words().get(index);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot choose daily solution", error);
        }
    }

    private GameDto toDto(GameRecord record, boolean revealSolution) {
        List<GuessResult> guesses = readGuesses(record);
        return new GameDto(
            record.puzzleDate(),
            record.mode(),
            record.mode().label(),
            record.status(),
            MAX_ATTEMPTS,
            WordsProvider.WORD_LENGTH,
            guesses,
            revealSolution || record.status() != GameStatus.IN_PROGRESS ? record.solution() : null,
            guesses.isEmpty() && record.status() == GameStatus.IN_PROGRESS,
            kittenDto(record)
        );
    }

    private KittenDto kittenDto(GameRecord record) {
        if (record.mode() != GameMode.MISCHIEVOUS_MOUSE) {
            return new KittenDto(false, false, false);
        }
        boolean used = record.kittenUsedAt() != null || record.mouseRevealed();
        return new KittenDto(record.kittenUnlocked(), used, record.kittenUnlocked() && !used);
    }

    private GuessResult hideTile(GuessResult guess, int tileIndex) {
        List<TileResult> tiles = new ArrayList<>(guess.tiles());
        TileResult tile = tiles.get(tileIndex);
        tiles.set(tileIndex, new TileResult(tile.letter(), TileState.HIDDEN));
        return new GuessResult(guess.word(), List.copyOf(tiles));
    }

    private GuessResult revealHiddenTile(GuessResult hiddenGuess, String solution, int tileIndex) {
        GuessResult scoredGuess = score(hiddenGuess.word(), solution);
        List<TileResult> tiles = new ArrayList<>(hiddenGuess.tiles());
        tiles.set(tileIndex, scoredGuess.tiles().get(tileIndex));
        return new GuessResult(hiddenGuess.word(), List.copyOf(tiles));
    }

    private int countCorrect(GuessResult guess) {
        return (int) guess.tiles().stream()
            .filter(tile -> tile.state() == TileState.CORRECT)
            .count();
    }

    private boolean alreadyGuessed(List<GuessResult> guesses, String guess) {
        return guesses.stream()
            .anyMatch(submittedGuess -> submittedGuess.word().equals(guess));
    }

    private int chooseMouseTileIndex(GameRecord record) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                (wordSeed + ":mouse:" + record.userId() + ":" + record.puzzleDate() + ":" + record.solution())
                    .getBytes(StandardCharsets.UTF_8)
            );
            return new BigInteger(1, hash).mod(BigInteger.valueOf(WordsProvider.WORD_LENGTH)).intValue();
        } catch (Exception error) {
            throw new IllegalStateException("Cannot choose hidden tile", error);
        }
    }

    private List<GuessResult> readGuesses(GameRecord record) {
        try {
            return objectMapper.readValue(record.guessesJson(), new TypeReference<>() {
            });
        } catch (Exception error) {
            throw new IllegalStateException("Cannot parse guesses", error);
        }
    }

    private String writeGuesses(List<GuessResult> guesses) {
        try {
            return objectMapper.writeValueAsString(guesses);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot serialize guesses", error);
        }
    }
}
