import type { GameMode, GameModeDto } from "../../../types";

export function ModeSelection({
  modes,
  selectedMode,
  compact = false,
  onSelect
}: {
  modes: readonly GameModeDto[];
  selectedMode: GameMode | null;
  compact?: boolean;
  onSelect: (mode: GameMode) => void;
}) {
  return (
    <section className={`mode-selection ${compact ? "compact" : ""}`} aria-label="Modalità di gioco">
      <h2>Modalità</h2>
      <div className="mode-options">
        {modes.map((mode) => (
          <button
            className={`mode-option ${selectedMode === mode.mode ? "selected" : ""}`}
            type="button"
            key={mode.mode}
            onClick={() => onSelect(mode.mode)}
            aria-pressed={selectedMode === mode.mode}
          >
            {mode.mode === "MISCHIEVOUS_MOUSE" ? <span>🐭</span> : <span className="classic-mark" aria-hidden="true" />}
            <span>{mode.label}</span>
          </button>
        ))}
      </div>
    </section>
  );
}
