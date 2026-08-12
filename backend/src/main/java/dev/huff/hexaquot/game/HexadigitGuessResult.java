package dev.huff.hexaquot.game;

import java.util.List;

public record HexadigitGuessResult(String guess, List<HexadigitTileResult> tiles) {
}
