import React from "react";
import { Share2, Waves } from "lucide-react";
import type { HexaflowFoundAnswerDto, HexaflowPathActionDto, HexaflowPathRequestDto, HexaflowTodayDto } from "../../types";

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
  const [gridSize, setGridSize] = React.useState({ width: 0, height: 0 });
  const gridRef = React.useRef<HTMLDivElement>(null);
  const cellsRef = React.useRef(new Map<number, HTMLButtonElement>());
  const pathRef = React.useRef<number[]>([]);
  const knownAnswerIds = React.useRef<Set<string> | null>(null);
  const rejectedTimer = React.useRef<number | null>(null);
  const [newAnswerIds, setNewAnswerIds] = React.useState<readonly string[]>([]);
  const [rejectedCells, setRejectedCells] = React.useState<readonly number[]>([]);
  const game = today.game;
  const completed = game?.status === "COMPLETED";
  const foundByCell = React.useMemo(() => {
    const map = new Map<number, "theme" | "flow">();
    game?.foundAnswers.forEach((answer) => answer.cells.forEach((cell) => map.set(cell, answer.type.toLowerCase() as "theme" | "flow")));
    return map;
  }, [game]);

  React.useEffect(() => {
    const answerIds = new Set((game?.foundAnswers ?? []).map((answer) => answer.id));
    if (knownAnswerIds.current === null) {
      knownAnswerIds.current = answerIds;
      return;
    }
    const added = [...answerIds].filter((id) => !knownAnswerIds.current?.has(id));
    knownAnswerIds.current = answerIds;
    if (!added.length) return;
    setNewAnswerIds(added);
    const timer = window.setTimeout(() => setNewAnswerIds([]), 1400);
    return () => window.clearTimeout(timer);
  }, [game?.foundAnswers]);

  React.useEffect(() => () => {
    if (rejectedTimer.current !== null) window.clearTimeout(rejectedTimer.current);
  }, []);

  React.useLayoutEffect(() => {
    const grid = gridRef.current;
    if (!grid) return;
    const updateSize = () => setGridSize({ width: grid.clientWidth, height: grid.clientHeight });
    updateSize();
    const observer = new ResizeObserver(updateSize);
    observer.observe(grid);
    return () => observer.disconnect();
  }, [today.available]);

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
      if (action.result.outcome === "EXTRA" || action.result.outcome === "DUPLICATE") {
        showRejectedPath(selected);
        if (action.result.outcome === "DUPLICATE") onError("Sequenza già utilizzata.");
      }
      if (action.game.status === "COMPLETED") onComplete();
    } catch (error) {
      onError(error instanceof Error ? error.message : "Percorso non accettato.");
    }
  }

  function showRejectedPath(cells: readonly number[]) {
    if (rejectedTimer.current !== null) window.clearTimeout(rejectedTimer.current);
    setRejectedCells(cells);
    rejectedTimer.current = window.setTimeout(() => {
      setRejectedCells([]);
      rejectedTimer.current = null;
    }, 520);
  }

  React.useEffect(() => {
    if (path.length < 4 || dragging || busy || completed) return;
    const timer = window.setTimeout(() => void submit(path), AUTO_SUBMIT_DELAY_MS);
    return () => window.clearTimeout(timer);
  }, [path, dragging, busy, completed]);

  if (!today.available) return <section className="hexaflow unavailable"><Waves size={42} /><h2>Hexaflow</h2><strong>Non disponibile oggi</strong><p>Il prossimo flusso apparirà qui quando sarà pubblicato.</p></section>;

  const currentWord = path.map((cell) => today.grid[cell]).join("");
  return <section className="hexaflow" aria-label="Hexaflow">
    <header className="hexaflow-head"><div><p className="eyebrow">Hexaflow · {today.puzzleDate}</p><h2>{today.themeClue}</h2></div><span aria-label={`${game?.foundAnswers.length ?? 0} parole su ${today.totalAnswers}`}>{game?.foundAnswers.length ?? 0}<i>/</i>{today.totalAnswers}</span></header>
    <div className={`hexaflow-current ${currentWord ? "is-tracing" : ""}`} aria-live="polite"><span>{currentWord || "Traccia una parola"}</span><b>{currentWord ? `${path.length} celle` : "scorri e connetti"}</b></div>
    <p className="hexaflow-instruction"><span className="hexaflow-gesture" aria-hidden="true">↗</span> Scorri sulle celle. Rilascia per inviare.</p>
    <div ref={gridRef} className="hexaflow-grid" onPointerMove={(event) => { if (!dragging) return; const target = document.elementFromPoint(event.clientX, event.clientY)?.closest<HTMLElement>("[data-cell]"); if (target?.dataset.cell) addCell(Number(target.dataset.cell)); }} onPointerUp={() => setDragging(false)} onPointerCancel={() => setDragging(false)}>
      <FoundPaths answers={game?.foundAnswers ?? []} newAnswerIds={newAnswerIds} cells={cellsRef.current} width={gridSize.width} height={gridSize.height} />
      <ActivePath cells={path} cellElements={cellsRef.current} width={gridSize.width} height={gridSize.height} />
      {today.grid.map((letter, index) => <button
        type="button" key={index} data-cell={index} ref={(element) => { if (element) cellsRef.current.set(index, element); else cellsRef.current.delete(index); }}
        className={["hexaflow-cell", path.includes(index) ? "selected" : "", rejectedCells.includes(index) ? "rejected" : "", foundByCell.get(index) ?? ""].filter(Boolean).join(" ")}
        onPointerDown={(event) => { if (completed) return; event.preventDefault(); setDragging(true); if (pathRef.current.length === 0) { pathRef.current = [index]; setPath([index]); } else addCell(index); event.currentTarget.setPointerCapture(event.pointerId); }}
        aria-label={`Cella ${index + 1}: ${letter}`}><span className="hexaflow-cell-letter">{letter}</span><small>{path.indexOf(index) >= 0 ? path.indexOf(index) + 1 : ""}</small><span className="hexaflow-cell-glint" aria-hidden="true" /></button>)}
    </div>
    {completed ? <div className="hexaflow-complete"><h3>Flusso completato!</h3><p>Hai connesso ogni percorso.</p><button type="button" onClick={() => void share(today)}><Share2 size={17}/> Condividi</button></div> : null}
  </section>;
}

function FoundPaths({ answers, newAnswerIds, cells, width, height }: { answers: readonly HexaflowFoundAnswerDto[]; newAnswerIds: readonly string[]; cells: Map<number, HTMLButtonElement>; width: number; height: number }) {
  if (!width || !height || !answers.length) return null;
  const grid = cells.values().next().value?.parentElement?.getBoundingClientRect();
  if (!grid) return null;
  const center = centerForCell(cells, grid);
  return <svg className="hexaflow-paths" viewBox={`0 0 ${width} ${height}`} aria-hidden="true">
    <defs>
      <filter id="hexaflow-theme-glow" x="-60%" y="-60%" width="220%" height="220%"><feGaussianBlur stdDeviation="5" /></filter>
      <filter id="hexaflow-flow-glow" x="-60%" y="-60%" width="220%" height="220%"><feGaussianBlur stdDeviation="5" /></filter>
      <linearGradient id="hexaflow-theme-line" x1="0" y1="0" x2="1" y2="1"><stop stopColor="#a6fff4" /><stop offset=".48" stopColor="#45d8c8" /><stop offset="1" stopColor="#159f98" /></linearGradient>
      <linearGradient id="hexaflow-flow-line" x1="0" y1="0" x2="1" y2="1"><stop stopColor="#fff0a6" /><stop offset=".45" stopColor="#ffc24d" /><stop offset="1" stopColor="#ff8f35" /></linearGradient>
    </defs>
    {answers.flatMap((answer) => answer.cells.slice(1).map((cell, index) => {
      const from = center(answer.cells[index]);
      const to = center(cell);
      const kind = answer.type.toLowerCase();
      const isNew = newAnswerIds.includes(answer.id);
      return from && to ? <g key={`${answer.id}-${index}`} className={`hexaflow-connection ${kind} ${isNew ? "is-new" : ""}`}>
        <path className="hexaflow-path-glow" d={curvedLine(from, to, answer.cells[index] + cell)} />
        <path className="hexaflow-path" d={curvedLine(from, to, answer.cells[index] + cell)} />
      </g> : null;
    }))}
  </svg>;
}

function ActivePath({ cells: path, cellElements, width, height }: { cells: readonly number[]; cellElements: Map<number, HTMLButtonElement>; width: number; height: number }) {
  if (!width || !height || path.length < 2) return null;
  const grid = cellElements.values().next().value?.parentElement?.getBoundingClientRect();
  if (!grid) return null;
  const center = centerForCell(cellElements, grid);
  return <svg className="hexaflow-active-paths" viewBox={`0 0 ${width} ${height}`} aria-hidden="true">
    <defs><filter id="hexaflow-active-glow" x="-60%" y="-60%" width="220%" height="220%"><feGaussianBlur stdDeviation="5" /></filter></defs>
    {path.slice(1).map((cell, index) => {
      const from = center(path[index]);
      const to = center(cell);
      const line = from && to ? curvedLine(from, to, path[index] + cell) : "";
      return line ? <g className="hexaflow-active-connection" key={`${path[index]}-${cell}`}><path className="hexaflow-active-glow" d={line} /><path className="hexaflow-active-line" d={line} /></g> : null;
    })}
  </svg>;
}

function centerForCell(cells: Map<number, HTMLButtonElement>, grid: DOMRect) {
  return (index: number) => {
    const cell = cells.get(index)?.getBoundingClientRect();
    return cell ? { x: cell.left - grid.left + cell.width / 2, y: cell.top - grid.top + cell.height / 2 } : null;
  };
}

function curvedLine(from: { x: number; y: number }, to: { x: number; y: number }, seed: number) {
  const dx = to.x - from.x;
  const dy = to.y - from.y;
  const distance = Math.hypot(dx, dy);
  if (!distance) return "";
  const normal = { x: -dy / distance, y: dx / distance };
  const curve = ((seed % 3) - 1) * Math.min(7, distance * .12);
  const middle = { x: (from.x + to.x) / 2 + normal.x * curve, y: (from.y + to.y) / 2 + normal.y * curve };
  return `M ${from.x.toFixed(2)} ${from.y.toFixed(2)} Q ${middle.x.toFixed(2)} ${middle.y.toFixed(2)} ${to.x.toFixed(2)} ${to.y.toFixed(2)}`;
}

function adjacent(a: number, b: number) { return a !== b && Math.abs(Math.floor(a / 6) - Math.floor(b / 6)) <= 1 && Math.abs(a % 6 - b % 6) <= 1; }

async function share(today: HexaflowTodayDto) {
  const text = `Hexaflow ${today.puzzleDate}\n🌊 ${today.game?.extraCount ?? 0} extra\n${location.origin}${location.pathname}#/hexaflow`;
  if (navigator.share) { try { await navigator.share({ text }); return; } catch { /* fallback */ } }
  await navigator.clipboard.writeText(text);
}
