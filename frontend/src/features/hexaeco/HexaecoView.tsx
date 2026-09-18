import React from "react";
import { ArrowDown, ArrowLeft, ArrowRight, ArrowUp, Check, CircleHelp, Pause, RotateCcw, Share2, Undo2 } from "lucide-react";
import { ApiError } from "../../api";
import type { HexaecoBoardDto, HexaecoCommand, HexaecoSubmitActionDto, HexaecoSubmitRequestDto, HexaecoTodayDto } from "../../types";
import { COMMAND_GLYPHS, COMMAND_LABELS, compactSolution, destination, draftKey, hasWon, parseDraft, positionLabel, replay, step, type EcoState } from "./hexaecoRules";
import "./hexaeco.css";

const TUTORIAL_KEY = "huff.hexaeco.tutorial.v1";
const ICONS = { UP: ArrowUp, RIGHT: ArrowRight, DOWN: ArrowDown, LEFT: ArrowLeft, WAIT: Pause };
const CONTROL_ORDER: readonly HexaecoCommand[] = ["UP", "LEFT", "WAIT", "RIGHT", "DOWN"];

interface Props {
  today: HexaecoTodayDto;
  userId: string;
  inputBlocked: boolean;
  onSubmit: (request: HexaecoSubmitRequestDto) => Promise<HexaecoSubmitActionDto>;
  onComplete: () => void;
  onError: (error: unknown) => void;
}

export function HexaecoView(props: Props) {
  const [tutorial, setTutorial] = React.useState(() => {
    try { return localStorage.getItem(TUTORIAL_KEY) !== "done"; } catch { return true; }
  });
  function closeTutorial() {
    try { localStorage.setItem(TUTORIAL_KEY, "done"); } catch { /* Training also works without storage. */ }
    setTutorial(false);
  }
  return <section className="eco-shell" aria-labelledby="eco-title" id="main-content">
    <header className="eco-heading">
      <div><p className="eyebrow">Logica e sincronia</p><h2 id="eco-title">Hexaeco<span aria-hidden="true"> ◇</span></h2></div>
      <button type="button" className="tutorial-help" aria-label="Come si gioca a Hexaeco" onClick={() => setTutorial(true)}><CircleHelp size={21}/></button>
    </header>
    {tutorial ? <Tutorial onClose={closeTutorial} inputBlocked={props.inputBlocked}/> : <PlaySession {...props} key={`${props.userId}:${props.today.puzzleDate}:${props.today.rulesVersion}`}/>}
  </section>;
}

function PlaySession({ today, userId, inputBlocked, onSubmit, onComplete, onError }: Props) {
  const key = draftKey(userId, today);
  const [moves, setMoves] = React.useState<readonly HexaecoCommand[]>(() => {
    if (today.game) return today.game.moves;
    try { return parseDraft(localStorage.getItem(key), today.board); } catch { return []; }
  });
  const [saveError, setSaveError] = React.useState<string | null>(null);
  const [expired, setExpired] = React.useState(false);
  const [saving, setSaving] = React.useState(false);
  const [saved, setSaved] = React.useState(Boolean(today.game));
  const [storageFailed, setStorageFailed] = React.useState(false);
  const [shareMessage, setShareMessage] = React.useState("");
  const [retry, setRetry] = React.useState(0);
  const sent = React.useRef(false);
  const requestId = React.useRef(crypto.randomUUID());
  const alive = React.useRef(true);
  React.useEffect(() => { alive.current = true; return () => { alive.current = false; }; }, []);
  const state = React.useMemo(() => replay(today.board, moves), [today.board, moves]);
  const won = hasWon(today.board, state);

  React.useEffect(() => {
    if (!today.game) return;
    setMoves(today.game.moves);
    setSaved(true);
    setSaveError(null);
  }, [today.game]);

  React.useEffect(() => {
    try {
      if (saved || today.game) localStorage.removeItem(key);
      else localStorage.setItem(key, JSON.stringify({ board: JSON.stringify(today.board), moves }));
      setStorageFailed(false);
    } catch { setStorageFailed(true); }
  }, [key, moves, saved, today.board, today.game]);

  React.useEffect(() => {
    if (!won || saved || today.game || sent.current) return;
    sent.current = true;
    setSaving(true);
    setSaveError(null);
    void onSubmit({ requestId: requestId.current, puzzleDate: today.puzzleDate, rulesVersion: today.rulesVersion, moves: compactSolution(today.board, moves) })
      .then(() => {
        try { localStorage.removeItem(key); } catch { /* Completion is safe on the server. */ }
        if (!alive.current) return;
        setSaved(true);
        onComplete();
      }).catch((error: unknown) => {
        if (!alive.current) return;
        setSaveError(error instanceof Error ? error.message : "Non riesco a salvare il risultato. Riprova appena sei online.");
        setExpired(error instanceof ApiError && error.status === 409);
        onError(error);
      }).finally(() => { if (alive.current) setSaving(false); });
  }, [won, saved, today, moves, onSubmit, onComplete, onError, key, retry]);

  function retrySave() { sent.current = false; setRetry((value) => value + 1); }
  React.useEffect(() => {
    if (!saveError || expired) return;
    window.addEventListener("online", retrySave);
    return () => window.removeEventListener("online", retrySave);
  }, [saveError, expired]);

  async function share() {
    const text = `Hexaeco · ${today.puzzleDate}\n● ◇ In perfetta sincronia.\nSfida completata!`;
    const url = `${window.location.origin}/#/hexaeco`;
    try {
      if (navigator.share) await navigator.share({ title: "Hexaeco", text, url });
      else if (navigator.clipboard) { await navigator.clipboard.writeText(`${text}\n${url}`); setShareMessage("Risultato copiato."); }
      else setShareMessage(`${text} ${url}`);
    } catch (error) {
      if (!(error instanceof DOMException && error.name === "AbortError")) setShareMessage(`${text} ${url}`);
    }
  }

  return <>
    <p className="eco-intro">Guida la luce. La sua eco ti segue <strong>due turni dopo.</strong></p>
    <Puzzle board={today.board} moves={moves} onMoves={setMoves} disabled={inputBlocked || won || saved} won={won}/>
    {won || saved ? <div className="eco-result" role="status">
      <span className="eco-result-symbol" aria-hidden="true"><Check size={24}/></span>
      <h3>In perfetta sincronia.</h3><p>Due luci, un solo arrivo. Ben giocato.</p>
      {saving ? <p>Salvo il tuo risultato…</p> : saved ? <><button className="eco-primary" type="button" onClick={() => void share()}><Share2 size={17}/> Condividi il risultato</button><p className="eco-next">Una nuova eco ti aspetta domani.</p></> : null}
    </div> : <p className="eco-footnote">Prenditi il tuo tempo. Puoi annullare e ricominciare quando vuoi.</p>}
    {saveError ? <div className="eco-save-error" role="alert"><p>{saveError}</p><button type="button" className="eco-primary" disabled={saving} onClick={expired ? () => window.location.reload() : retrySave}>{expired ? "Apri la sfida di oggi" : "Riprova a salvare"}</button></div> : null}
    {storageFailed && !saved ? <p className="eco-footnote" role="status">Il browser non permette di salvare la ripresa. Tieni aperta questa pagina per continuare.</p> : null}
    {shareMessage ? <p className="eco-share-message" role="status">{shareMessage}</p> : null}
  </>;
}

function Puzzle({ board, moves, onMoves, disabled, won, expected }: {
  board: HexaecoBoardDto; moves: readonly HexaecoCommand[]; onMoves: (moves: readonly HexaecoCommand[]) => void;
  disabled: boolean; won: boolean; expected?: HexaecoCommand;
}) {
  const state = React.useMemo(() => replay(board, moves), [board, moves]);
  const [preview, setPreview] = React.useState<HexaecoCommand | null>(null);
  const [animating, setAnimating] = React.useState(false);
  const [announcement, setAnnouncement] = React.useState("");
  const timer = React.useRef<ReturnType<typeof setTimeout>>();
  const locked = React.useRef(false);
  React.useEffect(() => () => clearTimeout(timer.current), []);
  React.useEffect(() => {
    if (!disabled) return;
    clearTimeout(timer.current);
    locked.current = false;
    setAnimating(false);
    setPreview(null);
  }, [disabled]);

  function play(command: HexaecoCommand) {
    if (disabled || locked.current || (expected && expected !== command)) return;
    locked.current = true;
    setAnimating(true);
    setPreview(command);
    const next = step(board, state, command);
    // Give touch and keyboard users the same landing preview as pointer users.
    timer.current = setTimeout(() => {
      onMoves([...moves, command]);
      setPreview(null);
      const blockedLight = destination(board, state.light, command).blocked;
      const blockedEcho = destination(board, state.echo, state.queue[0]).blocked;
      setAnnouncement(`Luce: ${positionLabel(next.light)}${blockedLight ? ", ostacolo: resta ferma" : ""}. Eco: ${positionLabel(next.echo)}${blockedEcho ? ", ostacolo: resta ferma" : ""}. Prossima eco: ${COMMAND_LABELS[next.queue[0]]}.`);
      timer.current = setTimeout(() => { locked.current = false; setAnimating(false); }, 230);
    }, window.matchMedia("(prefers-reduced-motion: reduce)").matches ? 0 : 180);
  }

  function undo() {
    if (disabled || locked.current || !moves.length) return;
    onMoves(moves.slice(0, -1));
    setPreview(null);
    setAnnouncement("Mossa annullata. Anche i due comandi dell’eco sono stati ripristinati.");
  }

  React.useEffect(() => {
    function keydown(event: KeyboardEvent) {
      if (disabled || event.repeat || event.altKey || event.ctrlKey || event.metaKey || document.querySelector('[role="dialog"], dialog[open]')) return;
      const target = event.target;
      if (target instanceof HTMLElement && (target.isContentEditable || ["INPUT", "TEXTAREA", "SELECT"].includes(target.tagName))) return;
      const keys: Record<string, HexaecoCommand> = { ArrowUp: "UP", ArrowRight: "RIGHT", ArrowDown: "DOWN", ArrowLeft: "LEFT", " ": "WAIT" };
      if (event.key === " " && target instanceof HTMLButtonElement) return;
      if (keys[event.key]) { event.preventDefault(); play(keys[event.key]); }
      if (event.key === "Backspace" || event.key.toLowerCase() === "z") { event.preventDefault(); undo(); }
    }
    window.addEventListener("keydown", keydown);
    return () => window.removeEventListener("keydown", keydown);
  });

  return <>
    <div className="eco-legend"><span><i className="eco-light-mark"/> Luce · cerchio</span><span><i className="eco-echo-mark"/> Eco · rombo</span></div>
    <Board board={board} state={state} preview={disabled ? null : preview} won={won}/>
    <div className="eco-queue" aria-label={`Comandi in attesa: prossimo turno ${COMMAND_LABELS[state.queue[0]]}, fra due turni ${COMMAND_LABELS[state.queue[1]]}`}>
      <div><strong>L’eco ricorda</strong><span>Questi sono i suoi prossimi passi</span></div>
      {state.queue.map((command, index) => <div className={`eco-queued ${index === 0 ? "next" : ""}`} key={index}><small>{index === 0 ? "Prossimo" : "Poi"}</small><span aria-hidden="true">{COMMAND_GLYPHS[command]}</span><b>{COMMAND_LABELS[command]}</b></div>)}
    </div>
    {!won ? <>
      <div className="eco-controls" role="group" aria-label="Muovi la luce; l’eco segue con due turni di ritardo">
        {CONTROL_ORDER.map((command) => {
          const Icon = ICONS[command];
          return <button type="button" className={`eco-command eco-command--${command.toLowerCase()}${expected === command ? " expected" : ""}`} key={command} aria-label={COMMAND_LABELS[command]}
            disabled={disabled || animating || Boolean(expected && expected !== command)}
            onPointerDown={() => setPreview(command)} onPointerCancel={() => setPreview(null)}
            onMouseEnter={() => setPreview(command)} onMouseLeave={() => { if (!locked.current) setPreview(null); }}
            onFocus={() => setPreview(command)} onBlur={() => { if (!locked.current) setPreview(null); }} onClick={() => play(command)}>
            <Icon size={22} aria-hidden="true"/>{command === "WAIT" ? <span>Aspetta</span> : null}
          </button>;
        })}
      </div>
      <div className="eco-tools"><button type="button" disabled={disabled || animating || !moves.length} onClick={undo}><Undo2 size={17}/> Annulla</button><button type="button" disabled={disabled || animating || !moves.length} onClick={() => { onMoves([]); setPreview(null); setAnnouncement("Si ricomincia. Entrambe le luci e la coda sono tornate all’inizio."); }}><RotateCcw size={16}/> Ricomincia</button></div>
    </> : null}
    <p className="eco-sr-only" role="status" aria-live="polite" aria-atomic="true">{announcement}</p>
  </>;
}

function Board({ board, state, preview, won }: { board: HexaecoBoardDto; state: EcoState; preview: HexaecoCommand | null; won: boolean }) {
  const lightPreview = preview ? destination(board, state.light, preview) : null;
  const echoPreview = preview ? destination(board, state.echo, state.queue[0]) : null;
  const tokenStyle = (position: number): React.CSSProperties => ({ left: `${position % 5 * 20}%`, top: `${Math.floor(position / 5) * 20}%` });
  return <div className={`eco-board${won ? " solved" : ""}`}>
    <div className="eco-grid" role="grid" aria-label="Labirinto 5 per 5. Porta il cerchio e il rombo sui rispettivi traguardi.">
      {Array.from({ length: 5 }, (_, row) => <div role="row" className="eco-row" key={row}>
        {Array.from({ length: 5 }, (_, col) => {
          const cell = row * 5 + col, wall = board.walls.includes(cell), lightGoal = board.lightGoal === cell, echoGoal = board.echoGoal === cell;
          const hit = lightPreview?.wall === cell || echoPreview?.wall === cell;
          const description = [positionLabel(cell), wall ? "muro" : "passaggio", lightGoal && "traguardo del cerchio", echoGoal && "traguardo del rombo", state.light === cell && "luce", state.echo === cell && "eco"].filter(Boolean).join(", ");
          return <div key={cell} role="gridcell" aria-label={description} className={`eco-cell${wall ? " wall" : ""}${hit ? " hit" : ""}`}>
            {lightGoal ? <span className="eco-goal light" aria-hidden="true"/> : null}{echoGoal ? <span className="eco-goal echo" aria-hidden="true"/> : null}
          </div>;
        })}
      </div>)}
    </div>
    {lightPreview ? <span className="eco-token light preview" style={tokenStyle(lightPreview.position)} aria-hidden="true"><i/></span> : null}
    {echoPreview ? <span className="eco-token echo preview" style={tokenStyle(echoPreview.position)} aria-hidden="true"><i/></span> : null}
    <span className={`eco-token light${lightPreview?.blocked ? " blocked" : ""}`} style={tokenStyle(state.light)} aria-hidden="true"><i/></span>
    <span className={`eco-token echo${echoPreview?.blocked ? " blocked" : ""}${state.light === state.echo ? " shared" : ""}`} style={tokenStyle(state.echo)} aria-hidden="true"><i/></span>
  </div>;
}

const LESSONS = [
  { board: { walls: [], lightStart: 10, echoStart: 20, lightGoal: 6, echoGoal: 16 }, commands: ["RIGHT", "UP", "WAIT", "WAIT"], title: "Una mossa, due momenti", instructions: ["Premi Destra. Si muove solo il cerchio.", "Ora Su. L’eco aspetta ancora: ricorda due comandi.", "Premi Aspetta. Il cerchio si ferma e l’eco va a destra.", "Aspetta ancora. L’eco sale: entrambe le luci arrivano!"] },
  { board: { walls: [5], lightStart: 10, echoStart: 20, lightGoal: 11, echoGoal: 16 }, commands: ["UP", "RIGHT", "WAIT", "WAIT"], title: "Anche un muro ti aiuta", instructions: ["Premi Su. Il muro ferma il cerchio, ma il comando resta nella coda.", "Premi Destra per raggiungere il traguardo del cerchio.", "Aspetta. L’eco esegue Su: davanti a lei il passaggio è libero.", "Aspetta ancora. Ora anche il rombo trova il suo traguardo."] }
] satisfies { board: HexaecoBoardDto; commands: HexaecoCommand[]; title: string; instructions: string[] }[];

function Tutorial({ onClose, inputBlocked }: { onClose: () => void; inputBlocked: boolean }) {
  const [lessonIndex, setLessonIndex] = React.useState(0);
  const [moves, setMoves] = React.useState<readonly HexaecoCommand[]>([]);
  const lesson = LESSONS[lessonIndex];
  const won = hasWon(lesson.board, replay(lesson.board, moves));
  return <div className="eco-tutorial">
    <div className="eco-lesson-heading"><span>Piccola prova · {lessonIndex + 1} di 2</span><button type="button" onClick={onClose}>Salta la prova</button></div>
    <h3>{lesson.title}</h3>
    <p className="eco-lesson-instruction" role="status">{won ? "Ecco la sincronia. Hai portato entrambe le luci a destinazione." : lesson.instructions[moves.length]}</p>
    <Puzzle key={lessonIndex} board={lesson.board} moves={moves} onMoves={setMoves} disabled={inputBlocked || won} won={won} expected={lesson.commands[moves.length]}/>
    {won ? <button type="button" className="eco-primary" onClick={() => { if (lessonIndex === 0) { setLessonIndex(1); setMoves([]); } else onClose(); }}>{lessonIndex === 0 ? "Prova con un muro" : "Gioca la sfida di oggi"}<ArrowRight size={17}/></button> : null}
    <p className="eco-footnote">Questa è una prova libera. La sfida di oggi ti aspetta subito dopo.</p>
  </div>;
}
