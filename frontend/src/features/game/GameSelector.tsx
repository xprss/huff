import React from "react";
import { ArrowRight } from "lucide-react";
import { HEXATRAP_AVAILABLE } from "../../app/constants";
import { PuzzleArtwork, type PuzzleKind } from "./components/PuzzleArtwork";

// The card swells and fades out; the route switches when it is almost gone and
// the game view then fades in (route-enter). Keep in sync with interface.css.
const LAUNCH_SWITCH_MS = 300;

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

  // Clicking a card swells it while it fades out and the rest of the selector
  // recedes; the game view then fades in. Reduced motion navigates at once.
  const [launching, setLaunching] = React.useState<PuzzleKind | null>(null);
  const launchTimer = React.useRef<number | undefined>(undefined);
  React.useEffect(() => () => window.clearTimeout(launchTimer.current), []);
  const launch = (id: PuzzleKind, onPlay: () => void) => {
    if (launching) return;
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      onPlay();
      return;
    }
    setLaunching(id);
    launchTimer.current = window.setTimeout(onPlay, LAUNCH_SWITCH_MS);
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

  return (
    <section ref={selectorRef} className={`game-selector${launching ? " is-launching" : ""}`} aria-label="Giochi di oggi" tabIndex={-1} id="main-content">
      <div className="game-selector-grid">
        {games.map((game) => (
          <button
            key={game.id}
            className={`puzzle-card puzzle-card--${game.id}${game.completed ? " completed" : ""}${launching === game.id ? " is-launching" : ""}`}
            type="button"
            onClick={() => launch(game.id, game.onPlay)}
            disabled={!game.available}
            aria-label={`${game.name}: ${!game.available ? game.unavailableLabel ?? "non disponibile oggi" : game.completed ? "completato, vedi risultato" : game.description}`}
          >
            <PuzzleArtwork kind={game.id} />
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
