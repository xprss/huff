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
      <div className={`mode-options ${selectedMode ? "has-selection" : ""}`} role="group" aria-label="Scegli la modalità di gioco">
        {modes.map((mode) => {
          const isSelected = selectedMode === mode.mode;

          return (
            <button
              className={`mode-option ${isSelected ? "selected" : ""}`}
              type="button"
              key={mode.mode}
              onClick={() => onSelect(mode.mode)}
              aria-pressed={isSelected}
              aria-label={mode.label}
              title={mode.label}
            >
              {mode.mode === "MISCHIEVOUS_MOUSE" ? (
                <span aria-hidden="true">🐭</span>
              ) : mode.mode === "STUBBORN_CRAB" ? (
                <span aria-hidden="true">🦀</span>
              ) : (
                <span className="classic-mark" aria-hidden="true" />
              )}
              {isSelected ? <span>{mode.label}</span> : null}
            </button>
          );
        })}
      </div>
    </section>
  );
}
