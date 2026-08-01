import { X } from "lucide-react";
import type { GuessResult } from "../../../types";

export function StarRevealModal({ guesses, onClose }: { guesses: readonly GuessResult[]; onClose: () => void }) {
  return (
    <div className="modal-backdrop star-reveal-backdrop" role="presentation" onMouseDown={onClose}>
      <section
        className="modal star-reveal-modal"
        role="dialog"
        aria-modal="true"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <header className="modal-head">
          <h2>Stella</h2>
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
