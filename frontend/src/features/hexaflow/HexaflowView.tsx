import React from "react";
import { Lightbulb, RotateCcw, Send, Share2, Waves } from "lucide-react";
import type { HexaflowHintActionDto, HexaflowPathActionDto, HexaflowPathRequestDto, HexaflowStatsDto, HexaflowTodayDto } from "../../types";

export function HexaflowView({ today, stats, busy, onPath, onHint, onUpdate, onComplete, onError }: {
  today: HexaflowTodayDto;
  stats: HexaflowStatsDto;
  busy: boolean;
  onPath: (request: HexaflowPathRequestDto) => Promise<HexaflowPathActionDto>;
  onHint: () => Promise<HexaflowHintActionDto>;
  onUpdate: (action: HexaflowPathActionDto | HexaflowHintActionDto) => void;
  onComplete: () => void;
  onError: (message: string) => void;
}) {
  const [path, setPath] = React.useState<number[]>([]);
  const [dragging, setDragging] = React.useState(false);
  const pathRef = React.useRef<number[]>([]);
  const dragMovedRef = React.useRef(false);
  const game = today.game;
  const foundByCell = React.useMemo(() => {
    const map = new Map<number, "theme" | "flow">();
    game?.foundAnswers.forEach((answer) => answer.cells.forEach((cell) => map.set(cell, answer.type.toLowerCase() as "theme" | "flow")));
    return map;
  }, [game]);
  const hinted = React.useMemo(() => new Set(game?.hintedCells.flat() ?? []), [game]);

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

  async function submit(selected = path) {
    if (selected.length < 4 || busy) return;
    try {
      const action = await onPath({ requestId: crypto.randomUUID(), cells: selected });
      onUpdate(action);
      pathRef.current = []; setPath([]);
      if (action.result.outcome === "DUPLICATE") onError("Sequenza già utilizzata.");
      if (action.game.status === "COMPLETED") onComplete();
    } catch (error) { onError(error instanceof Error ? error.message : "Percorso non accettato."); }
  }

  async function useHint() {
    try { onUpdate(await onHint()); }
    catch (error) { onError(error instanceof Error ? error.message : "Suggerimento non disponibile."); }
  }

  if (!today.available) return <section className="hexaflow unavailable"><Waves size={42} /><h2>Hexaflow</h2><strong>Non disponibile oggi</strong><p>Il prossimo flusso apparirà qui quando sarà pubblicato.</p></section>;
  const completed = game?.status === "COMPLETED";
  const currentWord = path.map((cell) => today.grid[cell]).join("");
  return <section className="hexaflow" aria-label="Hexaflow">
    <header className="hexaflow-head"><div><p className="eyebrow">Hexaflow · {today.puzzleDate}</p><h2>{today.themeClue}</h2></div><span>{game?.foundAnswers.length ?? 0}/{today.totalAnswers}</span></header>
    <div className="hexaflow-progress" aria-label="Progresso suggerimento"><span style={{ width: `${((game?.extraCount ?? 0) % 3) / 3 * 100}%` }} /></div>
    <p className="hexaflow-current" aria-live="polite">{currentWord || "Traccia una parola"}</p>
    <div className="hexaflow-grid" onPointerMove={(event) => { if (!dragging) return; const target = document.elementFromPoint(event.clientX, event.clientY)?.closest<HTMLElement>("[data-cell]"); if (target?.dataset.cell) { const cell=Number(target.dataset.cell); if (pathRef.current[pathRef.current.length-1] !== cell) dragMovedRef.current=true; addCell(cell); } }} onPointerUp={() => { if (dragging && dragMovedRef.current) void submit(pathRef.current); setDragging(false); }} onPointerCancel={() => setDragging(false)}>
      {today.grid.map((letter, index) => <button
        type="button" key={index} data-cell={index}
        className={["hexaflow-cell", path.includes(index) ? "selected" : "", foundByCell.get(index) ?? "", hinted.has(index) ? "hinted" : ""].filter(Boolean).join(" ")}
        onPointerDown={(event) => { if (completed) return; event.preventDefault(); dragMovedRef.current=false; setDragging(true); if (pathRef.current.length===0) { pathRef.current=[index]; setPath([index]); } else addCell(index); event.currentTarget.setPointerCapture(event.pointerId); }}
        onPointerEnter={() => { if (dragging) addCell(index); }}
        onClick={() => { if (!dragging && !completed) addCell(index); }}
        aria-label={`Cella ${index + 1}: ${letter}`}>{letter}<small>{path.indexOf(index) >= 0 ? path.indexOf(index) + 1 : ""}</small></button>)}
    </div>
    <div className="hexaflow-actions">
      <button type="button" onClick={() => { pathRef.current=[]; setPath([]); }} disabled={!path.length || busy}><RotateCcw size={17}/> Azzera</button>
      <button type="button" onClick={() => void submit()} disabled={path.length < 4 || busy || completed}><Send size={17}/> Invia</button>
      <button type="button" onClick={() => void useHint()} disabled={!game?.hintCredits || busy || completed}><Lightbulb size={17}/> Suggerimento ({game?.hintCredits ?? 0})</button>
    </div>
    <div className="hexaflow-answers">
      <div><strong>Trovate</strong>{game?.foundAnswers.length ? game.foundAnswers.map((answer) => <span className={answer.type === "FLOW" ? "flow" : ""} key={answer.id}>{answer.type === "FLOW" ? "Flusso · " : ""}{answer.label}</span>) : <small>Nessuna parola trovata</small>}</div>
      <div className="hexaflow-counters"><span><strong>{game?.extraCount ?? 0}</strong> extra</span><span><strong>{game?.hintsUsed ?? 0}</strong> suggerimenti</span></div>
    </div>
    {completed ? <div className="hexaflow-complete"><h3>Flusso completato!</h3><p>{stats.completed + (stats.started === 0 ? 1 : 0)} completamenti · serie {stats.currentStreak}</p><button type="button" onClick={() => void share(today)}><Share2 size={17}/> Condividi</button></div> : null}
  </section>;
}

function adjacent(a: number, b: number) { return a !== b && Math.abs(Math.floor(a / 6) - Math.floor(b / 6)) <= 1 && Math.abs(a % 6 - b % 6) <= 1; }
async function share(today: HexaflowTodayDto) {
  const text = `Hexaflow ${today.puzzleDate}\n🌊 ${today.game?.extraCount ?? 0} extra · 💡 ${today.game?.hintsUsed ?? 0}\n${location.origin}${location.pathname}#/hexaflow`;
  if (navigator.share) { try { await navigator.share({ text }); return; } catch { /* fallback */ } }
  await navigator.clipboard.writeText(text);
}
