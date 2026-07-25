package dev.huff.hexaquot.game;

import java.util.List;

public record TodayGameDto(
    String puzzleDate,
    List<GameModeDto> modes,
    GameDto game
) {
}
