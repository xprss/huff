package dev.huff.wordle.game;

public record TileResult(
    String letter,
    TileState state
) {
}
