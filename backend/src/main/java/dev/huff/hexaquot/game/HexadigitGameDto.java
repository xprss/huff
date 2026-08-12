package dev.huff.hexaquot.game;

import java.util.List;

public record HexadigitGameDto(
    String puzzleDate,
    GameStatus status,
    int maxAttempts,
    int answerLength,
    List<HexadigitGuessResult> guesses,
    String solution
) {
}
