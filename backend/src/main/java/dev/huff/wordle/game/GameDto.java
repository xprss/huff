package dev.huff.wordle.game;

import java.util.List;

public record GameDto(
    String puzzleDate,
    GameStatus status,
    int maxAttempts,
    int wordLength,
    List<GuessResult> guesses,
    String solution
) {
}
