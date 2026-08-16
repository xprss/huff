import React from "react";
import { createPortal } from "react-dom";
import { CircleHelp, X } from "lucide-react";
import type { HexaskyCheckActionDto, HexaskyCheckRequestDto, HexaskyTodayDto } from "../../types";

const TUTORIAL_KEY = "huff.hexasky.tutorial.closed";

export function HexaskyView({ today, busy, onCheck, onComplete, onError }: {
  today: HexaskyTodayDto; busy: boolean;
  onCheck: (request: HexaskyCheckRequestDto) => Promise<HexaskyCheckActionDto>;
  onComplete: (action: HexaskyCheckActionDto) => void; onError: (message: string) => void;
}) {
  const game = today.game;
  const completed = game?.status === "WON" || game?.status === "LOST";
  const draftKey = `huff.hexasky.draft.${today.puzzleDate}`;
  const [cells, setCells] = React.useState<number[]>(() => loadDraft(draftKey, game?.proposal));
  const displayedCells = game?.status === "LOST" && game.solution ? game.solution : cells;
  const [selected, setSelected] = React.useState(0);
  const [tutorialOpen, setTutorialOpen] = React.useState(() => localStorage.getItem(TUTORIAL_KEY) !== "true");

  React.useEffect(() => { setCells(loadDraft(draftKey, game?.proposal)); }, [draftKey, game?.proposal]);
  React.useEffect(() => { if (!completed) localStorage.setItem(draftKey, JSON.stringify(cells)); }, [cells, completed, draftKey]);
  React.useEffect(() => {
    function keyboard(event: KeyboardEvent) {
      if (event.key >= "1" && event.key <= "4") setValue(Number(event.key));
      if (event.key === "Backspace" || event.key === "Delete") clearValue();
      if (event.key === "ArrowLeft") setSelected((v) => Math.max(0, v - 1));
      if (event.key === "ArrowRight") setSelected((v) => Math.min(15, v + 1));
      if (event.key === "ArrowUp") setSelected((v) => Math.max(0, v - 4));
      if (event.key === "ArrowDown") setSelected((v) => Math.min(15, v + 4));
    }
    window.addEventListener("keydown", keyboard); return () => window.removeEventListener("keydown", keyboard);
  });

  function setValue(value: number) {
    if (completed || busy) return;
    setCells((previous) => {
      const next = [...previous]; next[selected] = value;
      setSelected(nextEmpty(next, selected + 1) ?? selected); return next;
    });
  }
  function press(event: React.PointerEvent<HTMLButtonElement>, action: () => void) {
    if (event.button !== 0) return;
    event.preventDefault();
    action();
  }
  function keyboardClick(event: React.MouseEvent<HTMLButtonElement>, action: () => void) {
    if (event.detail !== 0) return;
    action();
  }
  function clearValue() { if (completed || busy) return; setCells((previous) => { const next=[...previous]; next[selected]=0; return next; }); }
  async function check() {
    if (cells.some((value) => !value) || completed || busy) return;
    try {
      const action = await onCheck({ requestId: crypto.randomUUID(), solution: cells });
      if (action.result.status !== "IN_PROGRESS") { localStorage.removeItem(draftKey); onComplete(action); }
      else onError("Non è la soluzione. Hai ancora un ultimo controllo.");
    } catch (error) { onError(error instanceof Error ? error.message : "Impossibile verificare la griglia."); }
  }
  const checksLeft = Math.max(0, 2 - (game?.checksUsed ?? 0));
  return <section className="sky-shell" aria-label="Hexasky">
    <header className="sky-heading"><div><p className="eyebrow">GRATTACIELO / {today.puzzleDate}</p><h2>Hexasky</h2></div><button className="sky-help" type="button" onClick={() => setTutorialOpen(true)} aria-label="Come si gioca"><CircleHelp size={20} /></button></header>
    <p className="sky-intro">Ogni riga e colonna contiene 1–4 una sola volta. Gli indizi indicano quanti grattacieli sono visibili da quel lato.</p>
    <div className="sky-puzzle" aria-label="Griglia Hexasky e indizi di visibilità">
      <Clues className="top" clues={today.visibility.top} />
      <Clues className="bottom" clues={today.visibility.bottom} />
      <Clues className="left" clues={today.visibility.left} vertical />
      <Clues className="right" clues={today.visibility.right} vertical />
      <div className="sky-grid" role="grid" aria-label="Griglia 4 per 4">
        {displayedCells.map((value, index) => <button key={index} type="button" role="gridcell" className={`sky-cell${selected === index ? " selected" : ""}${game?.status === "LOST" ? " revealed" : ""}`} onPointerDown={(event) => press(event, () => setSelected(index))} onClick={(event) => keyboardClick(event, () => setSelected(index))} disabled={completed || busy} aria-label={`Riga ${Math.floor(index / 4) + 1}, colonna ${(index % 4) + 1}: ${value || "vuota"}`}>{value || ""}</button>)}
      </div>
    </div>
    {game?.status === "WON" ? <p className="sky-result won">Grattacielo risolto al controllo {game.checksUsed}.</p> : null}
    {game?.status === "LOST" ? <div className="sky-result lost"><strong>Partita conclusa.</strong></div> : null}
    {!completed ? <><div className="sky-keyboard" aria-label="Tastiera numerica">{[1,2,3,4].map((value) => <button key={value} type="button" onPointerDown={(event) => press(event, () => setValue(value))} onClick={(event) => keyboardClick(event, () => setValue(value))} disabled={busy}>{value}</button>)}<button className="sky-delete" type="button" onPointerDown={(event) => press(event, clearValue)} onClick={(event) => keyboardClick(event, clearValue)} disabled={busy} aria-label="Cancella cella">⌫</button></div><button className="sky-check" type="button" disabled={busy || cells.some((value) => !value)} onPointerDown={(event) => press(event, () => void check())} onClick={(event) => keyboardClick(event, () => void check())}>Verifica <span aria-label={`${checksLeft} controlli residui`}>{checksLeft}</span></button></> : null}
    {tutorialOpen ? <Tutorial onClose={() => { localStorage.setItem(TUTORIAL_KEY, "true"); setTutorialOpen(false); }} /> : null}
  </section>;
}

function Clues({ clues, className, vertical = false }: { clues: readonly number[]; className: string; vertical?: boolean }) { return <div className={`sky-clues ${className}${vertical ? " vertical" : ""}`}>{clues.map((clue, index) => <span key={index}>{clue}</span>)}</div>; }
function Tutorial({ onClose }: { onClose: () => void }) { return createPortal(<div className="modal-backdrop" role="presentation" onMouseDown={onClose}><section className="modal sky-tutorial" role="dialog" aria-modal="true" aria-labelledby="sky-tutorial-title" onMouseDown={(event) => event.stopPropagation()}><button className="close-button" type="button" onClick={onClose} aria-label="Chiudi"><X size={19} /></button><h2 id="sky-tutorial-title">Come si gioca a Hexasky</h2><p>Gli indizi ai quattro lati dicono quanti grattacieli vedi guardando quella riga o colonna: un edificio più alto nasconde quelli più bassi dietro di sé.</p><p>Inserisci i numeri da 1 a 4 senza ripetizioni in ogni riga e colonna. Tocca una cella, usa la tastiera virtuale e la selezione avanza automaticamente.</p><p>Hai due verifiche: il primo errore non mostra dettagli; il secondo chiude la partita e rivela la soluzione.</p><button className="sky-check" type="button" onClick={onClose}>Ho capito</button></section></div>, document.body); }
function loadDraft(key: string, proposal: readonly number[] | null | undefined) { try { const stored=JSON.parse(localStorage.getItem(key) ?? "null"); if(Array.isArray(stored)&&stored.length===16&&stored.every((value)=>Number.isInteger(value)&&value>=0&&value<=4)) return stored; } catch { /* ignore malformed local state */ } return proposal ? [...proposal] : Array(16).fill(0); }
function nextEmpty(cells: readonly number[], start: number) { for(let index=start;index<cells.length;index++)if(!cells[index])return index; for(let index=0;index<start;index++)if(!cells[index])return index; return null; }
