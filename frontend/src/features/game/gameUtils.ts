import { APP_NAME, SHARE_EMOJI } from "../../app/constants";
import type { GameDto, TileState } from "../../types";
import { formatPuzzleDate } from "../../shared/utils/date";

export type BoardColumn = {
  readonly feedback: readonly (TileState | undefined)[];
};

export function buildColumns(game: GameDto | null): BoardColumn[] {
  const answerLength = game?.answerLength ?? 6;
  const maxAttempts = game?.maxAttempts ?? 6;
  const submitted = game?.guesses ?? [];

  return Array.from({ length: answerLength }, (_, columnIndex) => ({
    feedback: Array.from({ length: maxAttempts }, (_, attemptIndex) => submitted[attemptIndex]?.tiles[columnIndex]?.state)
  }));
}

export function hasAlreadyGuessed(game: GameDto, guess: string) {
  const normalizedGuess = guess.trim().toUpperCase();
  return game.guesses.some((submittedGuess) => submittedGuess.word.toUpperCase() === normalizedGuess);
}

export function buildShareText(game: GameDto) {
  const result = game.status === "WON" ? String(game.guesses.length) : "X";
  const attempts = game.guesses
    .map((guess) => guess.tiles.map((tile) => SHARE_EMOJI[tile.state]).join(""))
    .join("\n");

  return `${APP_NAME} - ${formatPuzzleDate(game.puzzleDate)}
Modalità: ${game.modeLabel}
Risultato: ${result}/${game.maxAttempts}

${attempts}`;
}
