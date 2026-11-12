import React from "react";
import { flushSync } from "react-dom";
import { ArrowRight, Check, Layers } from "lucide-react";
import { HEXATRAP_AVAILABLE } from "../../app/constants";
import { PuzzleArtwork, type PuzzleKind } from "./components/PuzzleArtwork";

// Swell phase before the route switch; the switch itself is a cross-fade.
const LAUNCH_SWELL_MS = 200;
const LAUNCH_FALLBACK_MS = 300;

type ViewTransitionDocument = Document & {
  startViewTransition?: (update: () => void) => { finished: Promise<void> };
};

export function GameSelector({
  hexawordCompleted, hexahackCompleted, hexaskyCompleted, hexaflowCompleted,
  hexaflowAvailable, hexastarCompleted, hexaecoCompleted, hexatrapCompleted,
  onHexatrap, onHexaword, onHexahack, onHexasky, onHexaflow, onHexastar, onHexaeco
}: {
  hexawordCompleted: boolean;
  hexahackCompleted: boolean;
  hexaskyCompleted: boolean;
  hexaflowCompleted: boolean;
  hexaflowAvailable: boolean;
  hexastarCompleted: boolean;
  hexaecoCompleted: boolean;
  hexatrapCompleted: boolean;
  onHexatrap: () => void;
  onHexaword: () => void;
  onHexahack: () => void;
  onHexasky: () => void;
  onHexaflow: () => void;
  onHexastar: () => void;
  onHexaeco: () => void;
}) {
  const selectorRef = React.useRef<HTMLElement | null>(null);
  React.useEffect(() => { selectorRef.current?.scrollTo(0, 0); }, []);

  // Clicking a card swells it while the rest of the selector recedes, then the
  // route switch runs inside a View Transition so the selector and the game view
  // cross-fade with no blank frame. Without View Transitions the card fades out
  // and the game view fades in (route-enter). Reduced motion navigates at once.
  const [launching, setLaunching] = React.useState<PuzzleKind | null>(null);
  const launchTimer = React.useRef<number | undefined>(undefined);
  React.useEffect(() => () => window.clearTimeout(launchTimer.current), []);
  const launch = (id: PuzzleKind, onPlay: () => void) => {
    if (launching) return;
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      onPlay();
      return;
    }
    const doc = document as ViewTransitionDocument;
    if (!doc.startViewTransition) {
      setLaunching(id);
      launchTimer.current = window.setTimeout(onPlay, LAUNCH_FALLBACK_MS);
      return;
    }
    setLaunching(id);
    launchTimer.current = window.setTimeout(() => {
      const root = document.documentElement;
      root.dataset.transition = "game-launch";
      const transition = doc.startViewTransition!(() => flushSync(onPlay));
      void transition.finished.finally(() => {
        delete root.dataset.transition;
        // Re-enabling route-enter would replay it on the view that just faded in.
        document.querySelectorAll(".game-surface > *").forEach((element) => {
          element.getAnimations().forEach((animation) => {
            if ((animation as CSSAnimation).animationName === "route-enter") animation.finish();
          });
        });
      });
    }, LAUNCH_SWELL_MS);
  };

  const games: { id: PuzzleKind; name: string; description: string; completed: boolean; available: boolean; unavailableLabel?: string; onPlay: () => void }[] = [
    { id: "word", name: "Hexaword", description: "Indovina la parola segreta di 6 lettere con gli indizi di colore.", completed: hexawordCompleted, available: true, onPlay: onHexaword },
    { id: "hack", name: "Hexahack", description: "Decifra il codice a 4 cifre massimizzando il tuo stealth.", completed: hexahackCompleted, available: true, onPlay: onHexahack },
    { id: "sky", name: "Hexasky", description: "Costruisci lo skyline rispettando le visibilità prospettiche.", completed: hexaskyCompleted, available: true, onPlay: onHexasky },
    { id: "flow", name: "Hexaflow", description: "Collega le lettere adiacenti per completare il flusso continuo.", completed: hexaflowCompleted, available: hexaflowAvailable, onPlay: onHexaflow },
    { id: "star", name: "Hexastar", description: "Combina le tessere sillabiche per ricostruire la griglia.", completed: hexastarCompleted, available: true, onPlay: onHexastar },
    { id: "eco", name: "Hexaeco", description: "Pianifica la traiettoria e sincronizza la luce con la sua eco.", completed: hexaecoCompleted, available: true, onPlay: onHexaeco },
    { id: "trap", name: "Hexatrap", description: "Interroga i due consiglieri e smaschera l'inganno.", completed: hexatrapCompleted, available: HEXATRAP_AVAILABLE, unavailableLabel: "In sviluppo", onPlay: onHexatrap }
  ];
  const availableGames = games.filter((game) => game.available);
  const completed = availableGames.filter((game) => game.completed).length;

  return (
    <section ref={selectorRef} className={`game-selector${launching ? ` is-launching${"startViewTransition" in document ? "" : " is-launching--fade"}` : ""}`} aria-labelledby="game-selector-title" tabIndex={-1} id="main-content">
      <div className="daily-welcome">
        <div className="daily-progress" aria-label={`${completed} sfide completate su ${availableGames.length} disponibili oggi`}>
          <span className="daily-progress-icon"><Layers size={20} aria-hidden="true" /></span>
          <div>
            <strong>{completed === availableGames.length ? "Tutte le sfide completate!" : "Sfide di oggi"}</strong>
            <span>{completed} di {availableGames.length} completate</span>
          </div>
          <div className="daily-progress-track" aria-hidden="true">
            {availableGames.map((game) => <i key={game.id} className={game.completed ? "filled" : ""} />)}
          </div>
        </div>
      </div>
      <div className="game-selector-grid">
        {games.map((game, index) => (
          <button
            key={game.id}
            className={`puzzle-card puzzle-card--${game.id}${index === 0 ? " puzzle-card--featured" : ""}${game.completed ? " completed" : ""}${launching === game.id ? " is-launching" : ""}`}
            type="button"
            onClick={() => launch(game.id, game.onPlay)}
            disabled={!game.available}
            aria-label={`${game.name}: ${!game.available ? game.unavailableLabel ?? "non disponibile oggi" : game.completed ? "completato, vedi risultato" : game.description}`}
          >
            <PuzzleArtwork kind={game.id} />
            {game.completed ? (
              <span className="puzzle-status"><Check size={12} strokeWidth={3} aria-hidden="true" /> Fatto</span>
            ) : null}
            <span className="puzzle-card-copy">
              <strong>{game.name}</strong>
              <span>{game.available ? game.description : "Nuova sfida in arrivo."}</span>
            </span>
            <span className="puzzle-card-cta">
              {!game.available ? game.unavailableLabel ?? "In arrivo" : game.completed ? "Risultato" : "Gioca"}
              {game.available ? <ArrowRight size={15} strokeWidth={2.5} aria-hidden="true" /> : null}
            </span>
          </button>
        ))}
      </div>
      <div className="daily-note"><span>Sviluppato con amore da Vincenzo e Noemi</span></div>
    </section>
  );
}
