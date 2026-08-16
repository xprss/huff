package dev.huff.hexaquot.game;

import java.util.List;

/** Rules and deterministic content for one kind of daily game. */
public interface DailyGameProvider<G> {
    String id();

    int answerLength();

    int maxAttempts();

    String normalize(String rawGuess);

    void validate(String guess);

    String solutionFor(String puzzleDate);

    G score(String guess, String solution);

    default GameStatus statusAfter(String guess, String solution, List<G> guesses) {
        if (guess.equals(solution)) {
            return GameStatus.WON;
        }
        return guesses.size() >= maxAttempts() ? GameStatus.LOST : GameStatus.IN_PROGRESS;
    }
}
