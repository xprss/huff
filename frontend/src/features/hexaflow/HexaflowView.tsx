import React from "react";
import { Share2, Waves } from "lucide-react";
import type { HexaflowPathActionDto, HexaflowPathRequestDto, HexaflowTodayDto } from "../../types";

const AUTO_SUBMIT_DELAY_MS = 500;

export function HexaflowView({ today, busy, onPath, onUpdate, onComplete, onError }: {
  today: HexaflowTodayDto;
  busy: boolean;
  onPath: (request: HexaflowPathRequestDto) => Promise<HexaflowPathActionDto>;
  onUpdate: (action: HexaflowPathActionDto) => void;
  onComplete: () => void;
  onError: (message: string) => void;
}) {
  const [path, setPath] = React.useState<number[]>([]);
  const [dragging, setDragging] = React.useState(false);
  const pathRef = React.useRef<number[]>([]);
  const game = today.game;
  const completed = game?.status === "COMPLETED";
  const foundByCell = React.useMemo(() => {
    const map = new Map<number, "theme" | "flow">();
    game?.foundAnswers.forEach((answer) => answer.cells.forEach((cell) => map.set(cell, answer.type.toLowerCase() as "theme" | "flow")));
    return map;
  }, [game]);

  function addCell(index: number) {
    setPath((current) => {
      let next = current;
      if (current.length > 1 && current[current.length - 2] === index) next = current.slice(0, -1);
      else if (current.includes(index)) return current;
      const last = current[current.length - 1];
      if (next === current && last !== undefined && !adjacent(last, index)) return current;
      if (next === current) next = [...current, index];
      pathRef.current = next;
      return next;
    });
  }

  async function submit(selected: readonly number[]) {
    if (selected.length < 4 || busy || completed) return;
    try {
      const action = await onPath({ requestId: crypto.randomUUID(), cells: selected });
      onUpdate(action);
      pathRef.current = [];
      setPath([]);
      if (action.result.outcome === "DUPLICATE") onError("Sequenza già utilizzata.");
      if (action.game.status === "COMPLETED") onComplete();
    } catch (error) {
      onError(error instanceof Error ? error.message : "Percorso non accettato.");
    }
  }

  React.useEffect(() => {
    if (path.length < 4 || dragging || busy || completed) return;
    const timer = window.setTimeout(() => void submit(path), AUTO_SUBMIT_DELAY_MS);
    return () => window.clearTimeout(timer);
  }, [path, dragging, busy, completed]);

  if (!today.available) return <section className="hexaflow unavailable"><Waves size={42} /><h2>Hexaflow</h2><strong>Non disponibile oggi</strong><p>Il prossimo flusso apparirà qui quando sarà pubblicato.</p></section>;

  const currentWord = path.map((cell) => today.grid[cell]).join("");
  return <section className="hexaflow" aria-label="Hexaflow">
    <header className="hexaflow-head"><div><p className="eyebrow">Hexaflow · {today.puzzleDate}</p><h2>{today.themeClue}</h2></div><span>{game?.foundAnswers.length ?? 0}/{today.totalAnswers}</span></header>
    <p className="hexaflow-current" aria-live="polite">{currentWord || "Traccia una parola"}</p>
    <p className="hexaflow-instruction">Rilascia il percorso per inviarlo. Tocca la penultima cella per tornare indietro.</p>
    <div className="hexaflow-grid" onPointerMove={(event) => { if (!dragging) return; const target = document.elementFromPoint(event.clientX, event.clientY)?.closest<HTMLElement>("[data-cell]"); if (target?.dataset.cell) addCell(Number(target.dataset.cell)); }} onPointerUp={() => setDragging(false)} onPointerCancel={() => setDragging(false)}>
      {today.grid.map((letter, index) => <button
        type="button" key={index} data-cell={index}
        className={["hexaflow-cell", path.includes(index) ? "selected" : "", foundByCell.get(index) ?? ""].filter(Boolean).join(" ")}
        onPointerDown={(event) => { if (completed) return; event.preventDefault(); setDragging(true); if (pathRef.current.length === 0) { pathRef.current = [index]; setPath([index]); } else addCell(index); event.currentTarget.setPointerCapture(event.pointerId); }}
        aria-label={`Cella ${index + 1}: ${letter}`}>{letter}<small>{path.indexOf(index) >= 0 ? path.indexOf(index) + 1 : ""}</small></button>)}
    </div>
    {completed ? <div className="hexaflow-complete"><h3>Flusso completato!</h3><p>Hai connesso ogni percorso.</p><button type="button" onClick={() => void share(today)}><Share2 size={17}/> Condividi</button></div> : null}
  </section>;
}

function adjacent(a: number, b: number) { return a !== b && Math.abs(Math.floor(a / 6) - Math.floor(b / 6)) <= 1 && Math.abs(a % 6 - b % 6) <= 1; }

async function share(today: HexaflowTodayDto) {
  const text = `Hexaflow ${today.puzzleDate}\n🌊 ${today.game?.extraCount ?? 0} extra\n${location.origin}${location.pathname}#/hexaflow`;
  if (navigator.share) { try { await navigator.share({ text }); return; } catch { /* fallback */ } }
  await navigator.clipboard.writeText(text);
}
