export function TerminalInput({
  cells,
  answerLength,
  canPlay,
  result,
  selectedCellIndex,
  onSelectCell
}: {
  cells: readonly string[];
  answerLength: number;
  canPlay: boolean;
  result: "won" | "lost" | null;
  selectedCellIndex: number | null;
  onSelectCell: (index: number) => void;
}) {
  const displayCells = cells.map((cell) => cell.toUpperCase());

  return (
    <div className={`terminal-input ${result ?? ""}`} aria-label={`Input utente: ${displayCells.join("")}`}>
      {Array.from({ length: answerLength }, (_, index) => (
        <button
          className={`terminal-cell ${displayCells[index] ? "filled" : ""} ${
            canPlay && index === selectedCellIndex ? "selected" : ""
          } ${
            canPlay && index === selectedCellIndex && !displayCells[index] ? "cursor" : ""
          }`}
          key={index}
          type="button"
          disabled={!canPlay}
          onClick={() => onSelectCell(index)}
          aria-label={`Casella ${index + 1}${displayCells[index] ? `: ${displayCells[index]}` : ", vuota"}`}
        >
          <span>{displayCells[index] ?? ""}</span>
        </button>
      ))}
    </div>
  );
}
