package dev.huff.hexaquot.game;

import java.util.List;

public record StarRevealDto(
    GameDto game,
    List<GuessResult> guesses
) {
}
