import React from "react";
import { CircleHelp, Share2, Waves, X } from "lucide-react";
import type { HexaflowFoundAnswerDto, HexaflowPathActionDto, HexaflowPathRequestDto, HexaflowTodayDto } from "../../types";

type Point = { x: number; y: number };

export function HexaflowView({ today, busy, onPath, onUpdate, onComplete, onError }: {
  today: HexaflowTodayDto;
  busy: boolean;
  onPath: (request: HexaflowPathRequestDto) => Promise<HexaflowPathActionDto>;
  onUpdate: (action: HexaflowPathActionDto) => void;
  onComplete: () => void;
  onError: (message: string) => void;
}) {
  const [path, setPath] = React.useState<number[]>([]);
  const [gridGeometry, setGridGeometry] = React.useState<{ width: number; height: number; centers: ReadonlyMap<number, Point> }>({ width: 0, height: 0, centers: new Map() });
  const gridRef = React.useRef<HTMLDivElement>(null);
  const helpRef = React.useRef<HTMLDialogElement>(null);
  const cellsRef = React.useRef(new Map<number, HTMLButtonElement>());
  const pathRef = React.useRef<number[]>([]);
  const gestureRef = React.useRef<{ pointerId: number; startCell: number; swiping: boolean } | null>(null);
  const submittingRef = React.useRef(false);
  const pointerFrame = React.useRef<number | null>(null);
  const pointerPosition = React.useRef<{ x: number; y: number } | null>(null);
  const game = today.game;
  const completed = game?.status === "COMPLETED";
  const foundByCell = React.useMemo(() => {
    const map = new Map<number, "theme" | "flow">();
    game?.foundAnswers.forEach((answer) => answer.cells.forEach((cell) => map.set(cell, answer.type.toLowerCase() as "theme" | "flow")));
    return map;
  }, [game]);
  const pathOrderByCell = React.useMemo(
    () => new Map(path.map((cell, index) => [cell, index + 1])),
    [path]
  );

  React.useEffect(() => () => {
    if (pointerFrame.current !== null) window.cancelAnimationFrame(pointerFrame.current);
  }, []);

  React.useEffect(() => {
    const cancelOutsideGrid = (event: PointerEvent) => {
      const target = event.target;
      if (target instanceof Node && !gridRef.current?.contains(target)) clearPath();
    };
    document.addEventListener("pointerdown", cancelOutsideGrid);
    return () => document.removeEventListener("pointerdown", cancelOutsideGrid);
  }, []);

  React.useLayoutEffect(() => {
    const grid = gridRef.current;
    if (!grid) return;
    const updateSize = () => {
      const gridBounds = grid.getBoundingClientRect();
      const centers = new Map<number, Point>();
      cellsRef.current.forEach((cell, index) => {
        const bounds = cell.getBoundingClientRect();
        centers.set(index, { x: bounds.left - gridBounds.left + bounds.width / 2, y: bounds.top - gridBounds.top + bounds.height / 2 });
      });
      setGridGeometry({ width: grid.clientWidth, height: grid.clientHeight, centers });
    };
    updateSize();
    const observer = new ResizeObserver(updateSize);
    observer.observe(grid);
    return () => observer.disconnect();
  }, [today.available]);

  function addCell(index: number) {
    const current = pathRef.current;
    let next = current;
    if (current.length > 1 && current[current.length - 2] === index) next = current.slice(0, -1);
    else if (current.includes(index)) return;
    const last = current[current.length - 1];
    if (next === current && last !== undefined && !adjacent(last, index)) return;
    if (next === current) next = [...current, index];
    pathRef.current = next;
    setPath(next);
  }

  function clearPath() {
    pathRef.current = [];
    setPath([]);
  }

  function cellAt(x: number, y: number) {
    const target = document.elementFromPoint(x, y)?.closest<HTMLElement>("[data-cell]");
    return target?.dataset.cell === undefined ? undefined : Number(target.dataset.cell);
  }

  function beginSwipe() {
    const gesture = gestureRef.current;
    if (!gesture || gesture.swiping) return;
    gesture.swiping = true;
    if (pathRef.current.length === 0) {
      pathRef.current = [gesture.startCell];
      setPath([gesture.startCell]);
    } else {
      addCell(gesture.startCell);
    }
  }

  function trackPointer(x: number, y: number) {
    pointerPosition.current = { x, y };
    if (pointerFrame.current !== null) return;
    pointerFrame.current = window.requestAnimationFrame(() => {
      pointerFrame.current = null;
      const position = pointerPosition.current;
      const gesture = gestureRef.current;
      if (!gesture || !position) return;
      const index = cellAt(position.x, position.y);
      if (index !== undefined && (gesture.swiping || index !== gesture.startCell)) {
        beginSwipe();
        addCell(index);
      }
    });
  }

  async function submit(selected: readonly number[]) {
    if (selected.length < 4 || busy || completed || submittingRef.current) return;
    submittingRef.current = true;
    pathRef.current = [];
    setPath([]);
    try {
      const action = await onPath({ requestId: crypto.randomUUID(), cells: selected });
      onUpdate(action);
      if (action.result.outcome === "DUPLICATE") onError("Sequenza già utilizzata.");
      if (action.game.status === "COMPLETED") onComplete();
    } catch (error) {
      onError(error instanceof Error ? error.message : "Percorso non accettato.");
    } finally {
      submittingRef.current = false;
    }
  }

  if (!today.available) return <section className="hexaflow unavailable"><Waves size={42} /><h2>Hexaflow</h2><strong>Non disponibile oggi</strong><p>Il prossimo flusso apparirà qui quando sarà pubblicato.</p></section>;

  const currentWord = path.map((cell) => today.grid[cell]).join("");
  return <section className="hexaflow" aria-label="Hexaflow" style={{ "--flow-rows": Math.ceil(today.grid.length / 6) } as React.CSSProperties}>
    <header className="hexaflow-head"><div><p className="eyebrow">Hexaflow <span className="hexaflow-progress" aria-label={`${game?.foundAnswers.length ?? 0} parole su ${today.totalAnswers}`}>{game?.foundAnswers.length ?? 0}/{today.totalAnswers}</span></p><h2>{today.themeClue}</h2></div><button className="hexaflow-help" type="button" onClick={() => helpRef.current?.showModal()} aria-label="Istruzioni e dettagli Hexaflow"><CircleHelp size={20} /></button></header>
    <div className={`hexaflow-current ${currentWord ? "is-tracing" : ""}`} aria-live="polite"><span>{completed ? "Flusso completato!" : currentWord || "Traccia una parola"}</span><b>{completed ? "✓" : currentWord ? `${path.length} celle` : "scorri e connetti"}</b></div>
    <div className="hexaflow-board-space">
    <div ref={gridRef} className="hexaflow-grid" onPointerDown={(event) => { const target = event.target; if (!(target instanceof Element) || !target.closest("[data-cell]")) clearPath(); }} onPointerMove={(event) => { if (gestureRef.current?.pointerId === event.pointerId) trackPointer(event.clientX, event.clientY); }} onPointerUp={(event) => {
      const gesture = gestureRef.current;
      if (!gesture || gesture.pointerId !== event.pointerId) return;
      if (pointerFrame.current !== null) { window.cancelAnimationFrame(pointerFrame.current); pointerFrame.current = null; }
      const index = cellAt(event.clientX, event.clientY);
      if (index !== undefined && (gesture.swiping || index !== gesture.startCell)) {
        beginSwipe();
        addCell(index);
      }
      gestureRef.current = null;
      if (gesture.swiping) void submit(pathRef.current);
      else if (pathRef.current[pathRef.current.length - 1] === gesture.startCell) void submit(pathRef.current);
      else addCell(gesture.startCell);
    }} onPointerCancel={() => { gestureRef.current = null; clearPath(); }}>
      <FoundPaths answers={game?.foundAnswers ?? []} centers={gridGeometry.centers} width={gridGeometry.width} height={gridGeometry.height} />
      <ActivePath cells={path} centers={gridGeometry.centers} width={gridGeometry.width} height={gridGeometry.height} />
      {today.grid.map((letter, index) => <button
        type="button" key={index} data-cell={index} ref={(element) => { if (element) cellsRef.current.set(index, element); else cellsRef.current.delete(index); }}
        className={["hexaflow-cell", pathOrderByCell.has(index) ? "selected" : "", foundByCell.get(index) ?? ""].filter(Boolean).join(" ")}
        onPointerDown={(event) => { if (completed || busy || submittingRef.current) return; event.preventDefault(); gestureRef.current = { pointerId: event.pointerId, startCell: index, swiping: false }; gridRef.current?.setPointerCapture(event.pointerId); }}
        aria-label={`Cella ${index + 1}: ${letter}`}><span className="hexaflow-cell-letter">{letter}</span><small>{pathOrderByCell.get(index) ?? ""}</small></button>)}
    </div>
    </div>
    <dialog ref={helpRef} className="modal hexaflow-details" aria-labelledby="hexaflow-details-title" onClick={(event) => { if (event.target === event.currentTarget) helpRef.current?.close(); }}>
      <header className="modal-head"><h2 id="hexaflow-details-title">Come giocare</h2><button className="close-button" type="button" onClick={() => helpRef.current?.close()} aria-label="Chiudi" autoFocus><X size={19} /></button></header>
      <p>Collega le lettere vicine in ogni direzione per trovare le parole del tema e il Flusso che attraversa la griglia.</p>
      <p>Scorri e rilascia per inviare. Se usi i tocchi, premi di nuovo l’ultima cella. Ogni parola deve avere almeno quattro lettere.</p>
      <p><strong>{today.themeClue}</strong><br />{formatGameDate(today.puzzleDate)}{today.authorUsername ? <><br />Autore: {today.authorUsername}</> : null}</p>
      <button className="hexaflow-share-details" type="button" onClick={() => void share(today).catch(() => onError("Condivisione non disponibile."))}><Share2 size={18} /> Condividi Hexaflow</button>
    </dialog>
  </section>;
}

function FoundPaths({ answers, centers, width, height }: { answers: readonly HexaflowFoundAnswerDto[]; centers: ReadonlyMap<number, Point>; width: number; height: number }) {
  if (!width || !height || !answers.length) return null;
  return <svg className="hexaflow-paths" viewBox={`0 0 ${width} ${height}`} aria-hidden="true">
    {answers.flatMap((answer) => answer.cells.slice(1).map((cell, index) => {
      const from = centers.get(answer.cells[index]);
      const to = centers.get(cell);
      const kind = answer.type.toLowerCase();
      return from && to ? <line key={`${answer.id}-${index}`} className={`hexaflow-path ${kind}`} x1={from.x} y1={from.y} x2={to.x} y2={to.y} /> : null;
    }))}
  </svg>;
}

function ActivePath({ cells: path, centers, width, height }: { cells: readonly number[]; centers: ReadonlyMap<number, Point>; width: number; height: number }) {
  if (!width || !height || path.length < 2) return null;
  return <svg className="hexaflow-active-paths" viewBox={`0 0 ${width} ${height}`} aria-hidden="true">
    {path.slice(1).map((cell, index) => {
      const from = centers.get(path[index]);
      const to = centers.get(cell);
      return from && to ? <line className="hexaflow-active-line" key={`${path[index]}-${cell}`} x1={from.x} y1={from.y} x2={to.x} y2={to.y} /> : null;
    })}
  </svg>;
}

function adjacent(a: number, b: number) { return a !== b && Math.abs(Math.floor(a / 6) - Math.floor(b / 6)) <= 1 && Math.abs(a % 6 - b % 6) <= 1; }

function formatGameDate(date: string) {
  return new Intl.DateTimeFormat("it-IT", { day: "numeric", month: "long", year: "numeric", timeZone: "UTC" }).format(new Date(`${date}T12:00:00Z`));
}

async function share(today: HexaflowTodayDto) {
  const text = `Hexaflow ${formatGameDate(today.puzzleDate)}\n🌊 ${today.game?.extraCount ?? 0} extra\n${location.origin}${location.pathname}#/hexaflow`;
  if (navigator.share) { try { await navigator.share({ text }); return; } catch { /* fallback */ } }
  await navigator.clipboard.writeText(text);
}
