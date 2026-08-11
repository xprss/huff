import { useEffect, useRef, useState, type CSSProperties } from "react";
import { Share2 } from "lucide-react";
import type { GameDto, GameMode, GameModeDto } from "../../../types";
import type { BoardColumn } from "../gameUtils";
import { ModeSelection } from "./ModeSelection";
import { TerminalInput } from "./TerminalInput";

export function GameBoard({
  game,
  modes,
  columns,
  terminalCells,
  answerLength,
  canPlay,
  isSubmitting,
  terminalResult,
  selectedCellIndex,
  completedSolution,
  nextChallengeCountdown,
  onSelectMode,
  onUseKitten,
  onShareResult,
  onSelectCell
}: {
  game: GameDto;
  modes: readonly GameModeDto[];
  columns: readonly BoardColumn[];
  terminalCells: readonly string[];
  answerLength: number;
  canPlay: boolean;
  isSubmitting: boolean;
  terminalResult: "won" | "lost" | null;
  selectedCellIndex: number | null;
  completedSolution: string | null;
  nextChallengeCountdown: string;
  onSelectMode: (mode: GameMode) => void;
  onUseKitten: () => void;
  onShareResult: () => void;
  onSelectCell: (index: number) => void;
}) {
  const previousGuessCountRef = useRef(game.guesses.length);
  const [revealedAttemptIndex, setRevealedAttemptIndex] = useState<number | null>(null);

  useEffect(() => {
    const previousGuessCount = previousGuessCountRef.current;
    previousGuessCountRef.current = game.guesses.length;

    if (game.guesses.length <= previousGuessCount) return;

    setRevealedAttemptIndex(game.guesses.length - 1);
    const revealTimer = window.setTimeout(() => setRevealedAttemptIndex(null), 920);
    return () => window.clearTimeout(revealTimer);
  }, [game.guesses.length]);

  return (
    <div className="game-board-wrap">
      {game.canChangeMode ? (
        <ModeSelection modes={modes} selectedMode={game.mode} compact onSelect={onSelectMode} />
      ) : (
        <div className="mode-badge">{game.modeLabel}</div>
      )}
      {game.kitten.canUse ? (
        <button className="kitten-button" type="button" onClick={onUseKitten}>
          <span>🐱 Usa gattino</span>
        </button>
      ) : null}
      {completedSolution ? (
        <button className="share-button" type="button" onClick={onShareResult}>
          <Share2 size={18} />
          <span>Condividi risultato</span>
        </button>
      ) : null}
      <div className={`board ${isSubmitting ? "is-submitting" : ""}`} aria-label="Griglia tentativi">
        <TerminalInput
          cells={terminalCells}
          answerLength={answerLength}
          canPlay={canPlay}
          result={terminalResult}
          selectedCellIndex={selectedCellIndex}
          onSelectCell={onSelectCell}
        />
        <div className="feedback-board">
          {columns.map((column, columnIndex) => (
            <div className="board-column" key={columnIndex}>
              <div className="feedback-stack" aria-hidden="true">
                {column.feedback.map((state, attemptIndex) => (
                  <span
                    className={`feedback-marker ${
                      state === "CORRECT" || state === "PRESENT" || state === "ABSENT" || state === "HIDDEN"
                        ? state.toLowerCase()
                        : "empty"
                    } ${attemptIndex === revealedAttemptIndex ? "just-revealed" : ""}`}
                    key={attemptIndex}
                    style={
                      attemptIndex === revealedAttemptIndex
                        ? ({ "--feedback-reveal-delay": `${columnIndex * 78}ms` } as CSSProperties)
                        : undefined
                    }
                  >
                    {state === "HIDDEN" ? <span className="rat-in-guess">🐭</span> : null}
                  </span>
                ))}
              </div>
            </div>
          ))}
        </div>
        {completedSolution ? (
          <p className="next-challenge" aria-live="polite">
            Prossima sfida tra <time>{nextChallengeCountdown}</time>
          </p>
        ) : null}
      </div>
    </div>
  );
}
