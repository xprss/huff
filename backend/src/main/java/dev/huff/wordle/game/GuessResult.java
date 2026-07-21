package dev.huff.wordle.game;

import java.util.List;

public record GuessResult(
    String word,
    List<TileResult> tiles
) {
}
