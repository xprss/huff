import { Share2 } from "lucide-react";
import type { GameDto, GameMode, GameModeDto } from "../../../types";
import type { BoardColumn } from "../gameUtils";
import { ModeSelection } from "./ModeSelection";
import { TerminalInput } from "./TerminalInput";

export function GameBoard({
  game,
  modes,
  columns,
  terminalValue,
  answerLength,
  canPlay,
  terminalResult,
  completedSolution,
  nextChallengeCountdown,
  onSelectMode,
  onUseKitten,
  onShareResult
}: {
  game: GameDto;
  modes: readonly GameModeDto[];
  columns: readonly BoardColumn[];
  terminalValue: string;
  answerLength: number;
  canPlay: boolean;
  terminalResult: "won" | "lost" | null;
  completedSolution: string | null;
  nextChallengeCountdown: string;
  onSelectMode: (mode: GameMode) => void;
  onUseKitten: () => void;
  onShareResult: () => void;
}) {
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
      <div className="board" aria-label="Griglia tentativi">
        <TerminalInput value={terminalValue} answerLength={answerLength} canPlay={canPlay} result={terminalResult} />
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
                    }`}
                    key={attemptIndex}
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
