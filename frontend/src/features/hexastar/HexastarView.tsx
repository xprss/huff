import React from "react";
import { CircleHelp, Share2 } from "lucide-react";
import { GameKeyboard } from "../game/components/GameKeyboard";
import { SHARE_EMOJI } from "../../app/constants";
import { formatPuzzleDate } from "../../shared/utils/date";
import type { HexastarAttemptDto, HexastarGuessActionDto, HexastarSyllableResultDto, HexastarTodayDto } from "../../types";
import { HexastarTutorial } from "./HexastarTutorial";

type AnimationPhase = "idle" | "assembling" | "separating" | "won" | "rejected";

export function HexastarView({
  today,
  busy,
  onGuess,
  onUpdate,
  onError,
  onComplete
}: {
  today: HexastarTodayDto;
  busy: boolean;
  onGuess: (requestId: string, syllables: readonly string[]) => Promise<HexastarGuessActionDto>;
  onUpdate: (action: HexastarGuessActionDto) => void;
  onError: (message: string) => void;
  onComplete: (action: HexastarGuessActionDto) => void;
}) {
  const game = today.game;
  const lengths = game?.syllableLengths ?? today.syllableLengths;
  const canPlay = !busy && (game?.status ?? "IN_PROGRESS") === "IN_PROGRESS";
  const [syllables, setSyllables] = React.useState<string[]>(() => lengths.map(() => ""));
  const [selected, setSelected] = React.useState(0);
  const [phase, setPhase] = React.useState<AnimationPhase>("idle");
  const [pendingAttempt, setPendingAttempt] = React.useState<HexastarAttemptDto | null>(null);
  const [tutorialOpen, setTutorialOpen] = React.useState(() => localStorage.getItem("huff.hexastar.tutorial.v2.closed") !== "true");
  const boardRef = React.useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    setSyllables(lengths.map(() => ""));
    setSelected(0);
    setPendingAttempt(null);
  }, [today.puzzleDate, lengths.join(",")]);

  React.useLayoutEffect(() => {
    const board = boardRef.current;
    if (board) board.scrollTop = board.scrollHeight;
  }, [today.puzzleDate, game?.attempts.length]);

  React.useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (!canPlay || tutorialOpen || phase !== "idle") return;
      if (event.key === "Enter") void submit();
      else if (event.key === "Backspace") removeLetter();
      else if (/^[a-zA-Z]$/.test(event.key)) addLetter(event.key);
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  });

  const complete = syllables.every((syllable, index) => syllable.length === lengths[index]);

  function addLetter(rawLetter: string) {
    if (!canPlay || phase !== "idle") return;
    const letter = rawLetter.toUpperCase();
    setSyllables((current) => {
      const next = [...current];
      let index = selected;
      if (next[index]?.length >= lengths[index]) index = next.findIndex((value, candidate) => candidate > index && value.length < lengths[candidate]);
      if (index < 0) return current;
      next[index] = `${next[index]}${letter}`.slice(0, lengths[index]);
      if (next[index].length === lengths[index] && index < lengths.length - 1) setSelected(index + 1);
      return next;
    });
  }

  function removeLetter() {
    if (!canPlay || phase !== "idle") return;
    setSyllables((current) => {
      const next = [...current];
      let index = selected;
      if (!next[index] && index > 0) index -= 1;
      if (!next[index]) return current;
      next[index] = next[index].slice(0, -1);
      setSelected(index);
      return next;
    });
  }

  async function submit() {
    if (!canPlay || phase !== "idle") return;
    if (!complete) {
      onError("Completa tutte le sillabe.");
      return;
    }
    setPhase("assembling");
    const minimumAnimation = animationDelay(420);
    try {
      const [action] = await Promise.all([onGuess(crypto.randomUUID(), syllables), minimumAnimation]);
      if (action.game.status === "WON") {
        setPendingAttempt(action.attempt);
        setPhase("won");
        onUpdate(action);
        await animationDelay(650);
        setPhase("idle");
        onComplete(action);
        return;
      }
      setPendingAttempt(action.attempt);
      setPhase("separating");
      await animationDelay(520);
      onUpdate(action);
      setPendingAttempt(null);
      setSyllables(lengths.map(() => ""));
      setSelected(0);
      setPhase("idle");
      if (action.game.status === "LOST") onComplete(action);
    } catch (error) {
      await minimumAnimation;
      setPhase("rejected");
      onError(error instanceof Error ? error.message : "Tentativo non valido.");
      await animationDelay(420);
      setPhase("idle");
    }
  }

  async function share() {
    if (!game || game.status === "IN_PROGRESS" || !navigator.share) {
      onError("Condivisione non disponibile su questo dispositivo.");
      return;
    }
    const result = game.status === "WON" ? String(game.attempts.length) : "X";
    const rows = game.attempts.map((attempt) => attempt.tiles.map((tile) => SHARE_EMOJI[tile.state]).join("")).join("\n");
    try {
      await navigator.share({ title: "HexaQuot", text: `Hexastar - ${formatPuzzleDate(game.puzzleDate)}\nRisultato: ${result}/${game.maxAttempts}\n\n${rows}`, url: window.location.origin });
    } catch (error) {
      if (!(error instanceof DOMException && error.name === "AbortError")) onError("Impossibile aprire la condivisione.");
    }
  }

  return <section className="hexastar-shell" aria-label="Hexastar">
    <header className="sky-heading">
      <div><p className="eyebrow">Hexastar</p><h2>Componi la parola</h2></div>
      <button className="tutorial-help" type="button" onClick={() => setTutorialOpen(true)} aria-label="Come si gioca"><CircleHelp size={20}/></button>
    </header>
    <p className="sky-intro">Inserisci sillabe complete. I colori indicano posizione corretta, presenza altrove o assenza.</p>

    <div className="hexastar-board" ref={boardRef} role="region" aria-label="Tentativi e soluzione" aria-live="polite" tabIndex={0}>
      <div className="hexastar-attempts" aria-label="Tentativi precedenti">
        {(game?.attempts ?? []).map((attempt) => <AttemptRow attempt={attempt} lengths={lengths} key={attempt.requestId}/>) }
      </div>

      {(game?.status ?? "IN_PROGRESS") === "IN_PROGRESS" || phase !== "idle" ? (
        <div className={`hexastar-entry phase-${phase}`} aria-busy={busy || phase !== "idle"}>
          <div className="hexastar-syllables">
            {lengths.map((length, index) => <button
              type="button"
              className={`hexastar-syllable-input${selected === index ? " selected" : ""}${pendingAttempt?.tiles[index] ? ` ${pendingAttempt.tiles[index].state.toLowerCase()}` : ""}`}
              style={{ flexGrow: length }}
              onClick={() => phase === "idle" && setSelected(index)}
              disabled={!canPlay || phase !== "idle"}
              aria-label={`Sillaba ${index + 1} di ${lengths.length}, ${length} lettere`}
              key={`${index}-${length}`}
            >
              <span className="hexastar-syllable-label">Sillaba {index + 1}</span>
              <SyllableLetters syllable={syllables[index] ?? ""} length={length} feedback={pendingAttempt?.tiles[index]}/>
            </button>)}
          </div>
          <small>{complete ? "Parola pronta" : `Sillaba ${selected + 1}: ${syllables[selected]?.length ?? 0}/${lengths[selected]}`}</small>
        </div>
      ) : (
        <div className={`hexastar-terminal ${game?.status.toLowerCase()}`}>
          <strong>{game?.solutionSyllables?.join("")}</strong>
          <span>{game?.solutionSyllables?.join(" · ")}</span>
        </div>
      )}

    </div>

    {game && game.status !== "IN_PROGRESS" && phase === "idle" ? <button className="share-button" type="button" onClick={() => void share()}><Share2 size={18}/><span>Condividi risultato</span></button> : null}

    <div className="keyboard-zone">
      <GameKeyboard canPlay={canPlay && phase === "idle"} canSubmit={canPlay && phase === "idle" && complete} keyStates={new Map()} shouldHideKeyboardHints onAddLetter={addLetter} onBackspace={removeLetter} onSubmit={() => void submit()}/>
    </div>

    {tutorialOpen ? <HexastarTutorial onClose={() => {
      localStorage.setItem("huff.hexastar.tutorial.v2.closed", "true");
      setTutorialOpen(false);
    }}/> : null}
  </section>;
}

function AttemptRow({ attempt, lengths }: { attempt: HexastarAttemptDto; lengths: readonly number[] }) {
  return <div className="hexastar-attempt-row" aria-label={`Tentativo ${attempt.sequence}`}>
    {attempt.tiles.map((tile, index) => <div className={`hexastar-attempt-tile ${tile.state.toLowerCase()}`} style={{ flexGrow: lengths[index] }} key={index}>
      <span className="hexastar-syllable-label">Sillaba {index + 1}</span>
      <SyllableLetters syllable={tile.syllable} length={lengths[index]} feedback={tile}/>
    </div>)}
  </div>;
}

function SyllableLetters({ syllable, length, feedback }: {
  syllable: string;
  length: number;
  feedback?: HexastarSyllableResultDto;
}) {
  return <span className="hexastar-letters">
    {Array.from({ length }, (_, index) => {
      const result = feedback?.letters?.[index];
      const letter = syllable[index] ?? "";
      return <span className={`hexastar-letter-slot${result ? ` ${result.state.toLowerCase()}` : ""}`} key={index}>
        <strong>{letter}</strong>
        {result?.solutionSyllableIndex ? <small title={`Nella sillaba ${result.solutionSyllableIndex}`}>S{result.solutionSyllableIndex}</small> : null}
      </span>;
    })}
  </span>;
}

function delay(milliseconds: number) {
  return new Promise<void>((resolve) => window.setTimeout(resolve, milliseconds));
}

function animationDelay(milliseconds: number) {
  return delay(window.matchMedia("(prefers-reduced-motion: reduce)").matches ? 0 : milliseconds);
}
