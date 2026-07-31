package dev.huff.hexaquot.game;

public record StarDto(
    boolean available,
    boolean used,
    boolean canUse,
    boolean justAwarded
) {
}
