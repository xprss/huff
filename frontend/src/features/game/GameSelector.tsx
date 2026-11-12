import React from "react";
import { ArrowRight, Check, Layers } from "lucide-react";
import { HEXATRAP_AVAILABLE } from "../../app/constants";
import { PuzzleArtwork, type PuzzleKind } from "./components/PuzzleArtwork";

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
    <section ref={selectorRef} className="game-selector" aria-labelledby="game-selector-title" tabIndex={-1} id="main-content">
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
            className={`puzzle-card puzzle-card--${game.id}${index === 0 ? " puzzle-card--featured" : ""}${game.completed ? " completed" : ""}`}
            type="button"
            onClick={game.onPlay}
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
