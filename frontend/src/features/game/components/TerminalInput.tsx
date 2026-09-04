export function TerminalInput({
  cells,
  answerLength,
  canPlay,
  result,
  lockedCellIndices,
  selectedCellIndex,
  onSelectCell
}: {
  cells: readonly string[];
  answerLength: number;
  canPlay: boolean;
  result: "won" | "lost" | null;
  lockedCellIndices: readonly number[];
  selectedCellIndex: number | null;
  onSelectCell: (index: number) => void;
}) {
  const displayCells = cells.map((cell) => cell.toUpperCase());
  const lockedCells = new Set(lockedCellIndices);

  return (
    <div className={`terminal-input ${result ?? ""}`} aria-label={`Input utente: ${displayCells.join("")}`}>
      {Array.from({ length: answerLength }, (_, index) => (
        <button
          className={`terminal-cell ${displayCells[index] ? "filled" : ""} ${lockedCells.has(index) ? "locked" : ""} ${
            canPlay && index === selectedCellIndex ? "selected" : ""
          } ${
            canPlay && index === selectedCellIndex && !displayCells[index] ? "cursor" : ""
          }`}
          key={index}
          type="button"
          disabled={!canPlay || lockedCells.has(index)}
          onClick={() => onSelectCell(index)}
          aria-label={`Casella ${index + 1}${displayCells[index] ? `: ${displayCells[index]}` : ", vuota"}${
            lockedCells.has(index) ? ", salvia bloccata dal granchio" : ""
          }`}
        >
          <span>{displayCells[index] ?? ""}</span>
        </button>
      ))}
    </div>
  );
}
