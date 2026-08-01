import type { CSSProperties } from "react";
import { X } from "lucide-react";
import type { GuessResult } from "../../../types";

type StarRevealTimerStyle = CSSProperties & {
  "--star-reveal-duration": string;
};

export function StarRevealModal({
  durationMs,
  guesses,
  onClose
}: {
  durationMs: number;
  guesses: readonly GuessResult[];
  onClose: () => void;
}) {
  const timerStyle: StarRevealTimerStyle = {
    "--star-reveal-duration": `${durationMs}ms`
  };

  return (
    <div className="modal-backdrop star-reveal-backdrop" role="presentation" onMouseDown={onClose}>
      <section
        className="modal star-reveal-modal"
        role="dialog"
        aria-modal="true"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <header className="modal-head star-reveal-head">
          <div
            className="star-reveal-timer"
            role="progressbar"
            aria-label="Tempo rimanente"
            aria-valuemin={0}
            aria-valuemax={Math.round(durationMs / 1000)}
            style={timerStyle}
          >
            <span />
          </div>
          <button className="close-button" type="button" onClick={onClose} aria-label="Chiudi">
            <X size={19} />
          </button>
        </header>

        <div className="star-reveal-grid" aria-label="Lettere rivelate">
          {guesses.map((guess, guessIndex) => (
            <div className="star-reveal-row" key={`${guess.word}-${guessIndex}`}>
              {guess.tiles.map((tile, tileIndex) => (
                <span className={`star-reveal-tile ${tile.state.toLowerCase()}`} key={`${tile.letter}-${tileIndex}`}>
                  {tile.letter.toUpperCase()}
                </span>
              ))}
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
