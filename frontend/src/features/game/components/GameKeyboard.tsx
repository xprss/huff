import React from "react";
import { Delete } from "lucide-react";
import { KEY_ROWS } from "../../../app/constants";
import type { TileState } from "../../../types";

export function GameKeyboard({
  canPlay,
  keyStates,
  shouldHideKeyboardHints,
  onAddLetter,
  onSubmit,
  onBackspace
}: {
  canPlay: boolean;
  keyStates: Map<string, Exclude<TileState, "HIDDEN">>;
  shouldHideKeyboardHints: boolean;
  onAddLetter: (letter: string) => void;
  onSubmit: () => void;
  onBackspace: () => void;
}) {
  function pressVirtualKey(event: React.PointerEvent<HTMLButtonElement>, action: () => void) {
    if (event.button !== 0) return;
    event.preventDefault();
    action();
  }

  function clickVirtualKey(event: React.MouseEvent<HTMLButtonElement>, action: () => void) {
    if (event.detail !== 0) return;
    action();
  }

  return (
    <div className="keyboard" aria-label="Tastiera">
      {KEY_ROWS.map((row, index) => (
        <div className="key-row" key={row}>
          {index === 2 ? (
            <button
              className="key wide primary"
              type="button"
              disabled={!canPlay}
              onPointerDown={(event) => pressVirtualKey(event, onSubmit)}
              onClick={(event) => clickVirtualKey(event, onSubmit)}
            >
              Invio
            </button>
          ) : null}
          {row.split("").map((letter) => {
            const keyState = shouldHideKeyboardHints ? undefined : keyStates.get(letter);
            const keyClass =
              keyState === "CORRECT" || keyState === "PRESENT" || keyState === "ABSENT" ? keyState.toLowerCase() : "";
            const isAbsent = keyState === "ABSENT";

            return (
              <button
                className={`key ${keyClass}`}
                key={letter}
                type="button"
                disabled={!canPlay || isAbsent}
                onPointerDown={(event) => pressVirtualKey(event, () => onAddLetter(letter))}
                onClick={(event) => clickVirtualKey(event, () => onAddLetter(letter))}
              >
                {letter}
              </button>
            );
          })}
          {index === 2 ? (
            <button
              className="key wide"
              type="button"
              disabled={!canPlay}
              onPointerDown={(event) => pressVirtualKey(event, onBackspace)}
              onClick={(event) => clickVirtualKey(event, onBackspace)}
              title="Cancella"
            >
              <Delete size={19} />
            </button>
          ) : null}
        </div>
      ))}
    </div>
  );
}
