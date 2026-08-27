import { APP_NAME, SHARE_EMOJI } from "../../app/constants";
import type { GameDto, TileState } from "../../types";
import { formatPuzzleDate } from "../../shared/utils/date";

export type BoardColumn = {
  readonly feedback: readonly (TileState | undefined)[];
};

export type CrabYellowRequirement = {
  readonly letter: string;
  readonly count: number;
};

export type CrabConstraints = {
  readonly lockedLetters: readonly (string | null)[];
  readonly forbiddenYellowPositions: ReadonlyMap<string, ReadonlySet<number>>;
  readonly requiredLetterCounts: ReadonlyMap<string, number>;
  readonly yellowRequirements: readonly CrabYellowRequirement[];
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

export function buildCrabConstraints(game: GameDto | null): CrabConstraints {
  const lockedLetters = Array.from<string | null>({ length: game?.answerLength ?? 6 }).fill(null);
  const forbiddenYellowPositions = new Map<string, Set<number>>();
  const requiredLetterCounts = new Map<string, number>();
  const yellowLetters = new Set<string>();

  if (game?.mode !== "STUBBORN_CRAB") {
    return { lockedLetters, forbiddenYellowPositions, requiredLetterCounts, yellowRequirements: [] };
  }

  game.guesses.forEach((guess) => {
    const confirmedInGuess = new Map<string, number>();
    guess.tiles.forEach((tile, index) => {
      if (tile.state !== "CORRECT" && tile.state !== "PRESENT") return;

      const letter = tile.letter.toUpperCase();
      confirmedInGuess.set(letter, (confirmedInGuess.get(letter) ?? 0) + 1);
      if (tile.state === "CORRECT") {
        lockedLetters[index] = letter;
      } else {
        yellowLetters.add(letter);
        const positions = forbiddenYellowPositions.get(letter) ?? new Set<number>();
        positions.add(index);
        forbiddenYellowPositions.set(letter, positions);
      }
    });
    confirmedInGuess.forEach((count, letter) => {
      requiredLetterCounts.set(letter, Math.max(requiredLetterCounts.get(letter) ?? 0, count));
    });
  });

  const yellowRequirements = Array.from(yellowLetters)
    .sort((left, right) => left.localeCompare(right, "it"))
    .map((letter) => ({ letter, count: requiredLetterCounts.get(letter) ?? 1 }));

  return { lockedLetters, forbiddenYellowPositions, requiredLetterCounts, yellowRequirements };
}

export function crabConstraintViolation(constraints: CrabConstraints, rawGuess: string) {
  const guess = rawGuess.toUpperCase();
  for (let index = 0; index < constraints.lockedLetters.length; index += 1) {
    const lockedLetter = constraints.lockedLetters[index];
    if (lockedLetter && guess[index] !== lockedLetter) {
      return "Il granchio non lascia spostare le lettere verdi.";
    }
  }

  for (const [letter, positions] of constraints.forbiddenYellowPositions) {
    for (const position of positions) {
      if (guess[position] === letter) return "Le lettere gialle devono cambiare posizione.";
    }
  }

  const guessCounts = new Map<string, number>();
  for (const letter of guess) guessCounts.set(letter, (guessCounts.get(letter) ?? 0) + 1);
  for (const [letter, requiredCount] of constraints.requiredLetterCounts) {
    if ((guessCounts.get(letter) ?? 0) < requiredCount) {
      return "Il granchio vuole che tu riutilizzi tutte le lettere gialle.";
    }
  }
  return null;
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
