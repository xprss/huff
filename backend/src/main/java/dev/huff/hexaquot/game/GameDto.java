package dev.huff.hexaquot.game;

import java.util.List;

public record GameDto(
    String puzzleDate,
    GameStatus status,
    int maxAttempts,
    int answerLength,
    List<GuessResult> guesses,
    String solution
) {
}
