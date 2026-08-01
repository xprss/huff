export function TerminalInput({
  value,
  answerLength,
  canPlay,
  result
}: {
  value: string;
  answerLength: number;
  canPlay: boolean;
  result: "won" | "lost" | null;
}) {
  const displayValue = value.toUpperCase();
  const cursorIndex = Math.min(displayValue.length, answerLength);

  return (
    <div className={`terminal-input ${result ?? ""}`} aria-label={`Input utente: ${displayValue}`}>
      {Array.from({ length: answerLength }, (_, index) => (
        <span
          className={`terminal-cell ${displayValue[index] ? "filled" : ""} ${
            canPlay && index === cursorIndex ? "cursor" : ""
          }`}
          key={index}
        >
          <span>{displayValue[index] ?? ""}</span>
        </span>
      ))}
    </div>
  );
}
