import React from "react";
import { Delete, Share2 } from "lucide-react";
import type { HexadigitGameDto, HexadigitTodayDto, InputHandPreference } from "../../types";
import { formatNextChallengeCountdown } from "../../shared/utils/date";
import { DailyGameIntro } from "../game/components/DailyGameIntro";
import { LetterNavigator } from "../game/components/LetterNavigator";

const COMPARISON_LABEL = { HIGHER: ">", LOWER: "<", EQUAL: "=" } as const;

export function HexadigitView({
  today,
  hand,
  submitting,
  onGuess,
  onError,
  onComplete
}: {
  today: HexadigitTodayDto;
  hand: InputHandPreference;
  submitting: boolean;
  onGuess: (guess: string) => Promise<HexadigitGameDto>;
  onError: (message: string) => void;
  onComplete: (game: HexadigitGameDto) => void;
}) {
  const [digits, setDigits] = React.useState<string[]>(emptyDigitCells);
  const [selectedCellIndex, setSelectedCellIndex] = React.useState<number | null>(0);
  const game = today.game;
  const canPlay = !game || game.status === "IN_PROGRESS";
  const guessComplete = digits.every(Boolean);
  const previousGuessCountRef = React.useRef(game?.guesses.length ?? 0);
  const [revealedAttemptIndex, setRevealedAttemptIndex] = React.useState<number | null>(null);

  React.useEffect(() => {
    const guessCount = game?.guesses.length ?? 0;
    const previousGuessCount = previousGuessCountRef.current;
    previousGuessCountRef.current = guessCount;

    if (guessCount <= previousGuessCount) return;

    setRevealedAttemptIndex(guessCount - 1);
    const revealTimer = window.setTimeout(() => setRevealedAttemptIndex(null), 920);
    return () => window.clearTimeout(revealTimer);
  }, [game?.guesses.length]);

  function addDigit(digit: string) {
    if (!canPlay || submitting) return;
    setDigits((value) => {
      const cells = [...value];
      const index = selectedCellIndex ?? cells.findIndex((cell) => !cell);
      if (index < 0) return cells;

      cells[index] = digit;
      setSelectedCellIndex(findNextEmptyCell(cells, index + 1));
      return cells;
    });
  }

  function removeDigit() {
    if (!canPlay || submitting) return;
    setDigits((value) => {
      const cells = [...value];
      const index =
        selectedCellIndex !== null && cells[selectedCellIndex]
          ? selectedCellIndex
          : findPreviousFilledCell(cells, selectedCellIndex ?? cells.length);
      if (index === null) return cells;

      cells[index] = "";
      setSelectedCellIndex(index);
      return cells;
    });
  }

  function selectDigitCell(index: number) {
    if (!canPlay || submitting) return;
    setSelectedCellIndex(index);
  }

  function pressVirtualKey(event: React.PointerEvent<HTMLButtonElement>, action: () => void) {
    if (event.button !== 0) return;
    event.preventDefault();
    action();
  }

  function clickVirtualKey(event: React.MouseEvent<HTMLButtonElement>, action: () => void) {
    if (event.detail !== 0) return;
    action();
  }

  async function submit() {
    if (!canPlay || submitting) return;
    if (!guessComplete) {
      onError("Inserisci esattamente 6 cifre.");
      return;
    }
    try {
      const updated = await onGuess(digits.join(""));
      setDigits(emptyDigitCells());
      setSelectedCellIndex(0);
      if (updated.status !== "IN_PROGRESS") onComplete(updated);
    } catch (error) {
      onError(error instanceof Error ? error.message : "Tentativo non valido.");
    }
  }

  React.useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (/^[0-9]$/.test(event.key)) addDigit(event.key);
      else if (event.key === "Backspace") removeDigit();
      else if (event.key === "Enter") void submit();
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  });

  async function share() {
    if (!game || game.status === "IN_PROGRESS" || !navigator.share) return;
    const rows = game.guesses.map((guess) => guess.tiles.map((tile) =>
      tile.state === "CORRECT" ? "🟩" : tile.state === "PRESENT" ? "🟨" : "⬛"
    ).join(""));
    try {
      await navigator.share({
        title: "HexaQuot · Hexadigit",
        text: `Hexadigit ${game.status === "WON" ? game.guesses.length : "X"}/6\n\n${rows.join("\n")}`,
        url: `${window.location.origin}/#/hexadigit`
      });
    } catch (error) {
      if (!(error instanceof DOMException && error.name === "AbortError")) onError("Condivisione non disponibile.");
    }
  }

  const rows = game?.guesses ?? [];
  return (
    <section className="hexadigit-view" aria-label="Hexadigit">
      <DailyGameIntro game="Hexadigit" title="Trova il codice">
        Il colore indica la presenza; <strong>&gt;</strong>, <strong>&lt;</strong> o <strong>=</strong> dice se la cifra corretta è più alta, più bassa o uguale.
      </DailyGameIntro>
      <div className={`hexadigit-board ${submitting ? "is-submitting" : ""}`} role="grid" aria-label="Tentativi Hexadigit">
        {Array.from({ length: 6 }, (_, rowIndex) => {
          const submitted = rows[rowIndex];
          const isCurrent = rowIndex === rows.length && canPlay;
          const pending = isCurrent ? digits : emptyDigitCells();
          const rowState = submitted ? "past" : isCurrent ? "current" : "future";
          return (
            <div className={`hexadigit-row ${rowState}`} role="row" key={rowIndex}>
              {Array.from({ length: 6 }, (__, columnIndex) => {
                const tile = submitted?.tiles[columnIndex];
                const isSelected = isCurrent && selectedCellIndex === columnIndex;
                const isJustRevealed = rowIndex === revealedAttemptIndex;
                return (
                  <button
                    className={`hexadigit-tile ${tile?.state.toLowerCase() ?? (pending[columnIndex] ? "filled" : "")} ${isSelected ? "selected" : ""} ${isSelected && !pending[columnIndex] ? "cursor" : ""} ${isJustRevealed ? "just-revealed" : ""}`}
                    role="gridcell"
                    type="button"
                    key={columnIndex}
                    disabled={!isCurrent || submitting}
                    onClick={() => selectDigitCell(columnIndex)}
                    aria-selected={isSelected}
                    aria-label={`Casella ${columnIndex + 1}${pending[columnIndex] ? `: ${pending[columnIndex]}` : tile ? `: ${tile.digit}` : ", vuota"}`}
                    style={
                      isJustRevealed
                        ? ({ "--feedback-reveal-delay": `${columnIndex * 78}ms` } as React.CSSProperties)
                        : undefined
                    }
                  >
                    <span>{tile?.digit ?? pending[columnIndex] ?? ""}</span>
                    {tile ? <small>{COMPARISON_LABEL[tile.comparison]}</small> : null}
                  </button>
                );
              })}
            </div>
          );
        })}
      </div>

      {game && game.status !== "IN_PROGRESS" ? (
        <div className={`hexadigit-result ${game.status.toLowerCase()}`}>
          <strong>{game.status === "WON" ? "Codice trovato!" : `Soluzione: ${game.solution}`}</strong>
          <span>Prossima sfida tra {formatNextChallengeCountdown()}</span>
          <button type="button" onClick={() => void share()}><Share2 size={18} /> Condividi</button>
        </div>
      ) : (
        <>
          <LetterNavigator
            answerLength={6}
            canPlay={canPlay && !submitting}
            hand={hand}
            label="Navigazione cifre"
            selectedCellIndex={selectedCellIndex}
            onSelectCell={selectDigitCell}
          />
          <div className="digit-keyboard" aria-label="Tastierino numerico">
            {["1", "2", "3", "4", "5", "6", "7", "8", "9"].map((digit) =>
              <button
                type="button"
                key={digit}
                onPointerDown={(event) => pressVirtualKey(event, () => addDigit(digit))}
                onClick={(event) => clickVirtualKey(event, () => addDigit(digit))}
                disabled={!canPlay || submitting}
              >
                {digit}
              </button>
            )}
            <button
              className="digit-submit"
              type="button"
              onPointerDown={(event) => pressVirtualKey(event, () => void submit())}
              onClick={(event) => clickVirtualKey(event, () => void submit())}
              disabled={!canPlay || submitting || !guessComplete}
            >
              Invia
            </button>
            <button
              type="button"
              onPointerDown={(event) => pressVirtualKey(event, () => addDigit("0"))}
              onClick={(event) => clickVirtualKey(event, () => addDigit("0"))}
              disabled={!canPlay || submitting}
            >
              0
            </button>
            <button
              className="digit-delete"
              type="button"
              onPointerDown={(event) => pressVirtualKey(event, removeDigit)}
              onClick={(event) => clickVirtualKey(event, removeDigit)}
              disabled={!canPlay || submitting}
              aria-label="Cancella"
            >
              <Delete />
            </button>
          </div>
        </>
      )}
    </section>
  );
}

function emptyDigitCells() {
  return Array.from({ length: 6 }, () => "");
}

function findNextEmptyCell(cells: readonly string[], startIndex: number) {
  for (let index = startIndex; index < cells.length; index += 1) {
    if (!cells[index]) return index;
  }
  for (let index = 0; index < startIndex; index += 1) {
    if (!cells[index]) return index;
  }
  return null;
}

function findPreviousFilledCell(cells: readonly string[], startIndex: number) {
  for (let index = Math.min(startIndex - 1, cells.length - 1); index >= 0; index -= 1) {
    if (cells[index]) return index;
  }
  for (let index = cells.length - 1; index >= startIndex; index -= 1) {
    if (cells[index]) return index;
  }
  return null;
}
