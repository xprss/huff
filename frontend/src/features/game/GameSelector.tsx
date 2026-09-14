import React from "react";
import { ArrowRight, Check, Clock3, Sparkles } from "lucide-react";
import { PuzzleArtwork, type PuzzleKind } from "./components/PuzzleArtwork";

export function GameSelector({
  hexawordCompleted, hexahackCompleted, hexaskyCompleted, hexaflowCompleted,
  hexaflowAvailable, hexastarCompleted, onHexaword, onHexahack, onHexasky, onHexaflow, onHexastar
}: {
  hexawordCompleted: boolean;
  hexahackCompleted: boolean;
  hexaskyCompleted: boolean;
  hexaflowCompleted: boolean;
  hexaflowAvailable: boolean;
  hexastarCompleted: boolean;
  onHexaword: () => void;
  onHexahack: () => void;
  onHexasky: () => void;
  onHexaflow: () => void;
  onHexastar: () => void;
}) {
  const selectorRef = React.useRef<HTMLElement | null>(null);
  React.useEffect(() => { selectorRef.current?.scrollTo(0, 0); }, []);

  const games: { id: PuzzleKind; name: string; category: string; description: string; completed: boolean; available: boolean; onPlay: () => void }[] = [
    { id: "word", name: "Hexaword", category: "Parole · 6 tentativi", description: "Sei lettere. Una parola. Segui gli indizi e trova quella giusta.", completed: hexawordCompleted, available: true, onPlay: onHexaword },
    { id: "hack", name: "Hexahack", category: "Codici e deduzione", description: "Decifra il codice. Resta invisibile.", completed: hexahackCompleted, available: true, onPlay: onHexahack },
    { id: "sky", name: "Hexasky", category: "Logica e prospettiva", description: "Ogni indizio cambia il tuo skyline.", completed: hexaskyCompleted, available: true, onPlay: onHexasky },
    { id: "flow", name: "Hexaflow", category: "Parole da collegare", description: "Unisci le lettere. Trova il Flusso.", completed: hexaflowCompleted, available: hexaflowAvailable, onPlay: onHexaflow },
    { id: "star", name: "Hexastar", category: "Sillabe e intuizione", description: "Le sillabe giuste, al posto giusto.", completed: hexastarCompleted, available: true, onPlay: onHexastar }
  ];
  const availableGames = games.filter((game) => game.available);
  const completed = availableGames.filter((game) => game.completed).length;

  return (
    <section ref={selectorRef} className="game-selector" aria-labelledby="game-selector-title" tabIndex={-1} id="main-content">
      <div className="daily-welcome">
        <div className="daily-welcome-copy">
          <p className="eyebrow"><span className="live-dot" /> Un piccolo rituale, ogni giorno</p>
          <h2 id="game-selector-title">Fai spazio<br />a un bel <span>rompicapo.</span></h2>
          <p>Stacca un momento. Accendi la mente.</p>
        </div>
        <div className="daily-progress" aria-label={`${completed} sfide completate su ${availableGames.length} disponibili oggi`}>
          <span className="daily-progress-icon"><Sparkles size={20} aria-hidden="true" /></span>
          <div><strong>{completed === availableGames.length ? "Tutte fatte. Ben giocato!" : "Il tuo percorso di oggi"}</strong><span>{completed} di {availableGames.length} sfide completate</span></div>
          <div className="daily-progress-track" aria-hidden="true">{availableGames.map((game) => <i key={game.id} className={game.completed ? "filled" : ""} />)}</div>
        </div>
      </div>
      <div className="collection-heading"><h3>La tua dose di sfida</h3><span>Ogni giorno, nuove soluzioni</span></div>
      <div className="game-selector-grid">
        {games.map((game, index) => (
          <button key={game.id} className={`puzzle-card puzzle-card--${game.id}${index === 0 ? " puzzle-card--featured" : ""}${game.completed ? " completed" : ""}`} type="button" onClick={game.onPlay} disabled={!game.available} aria-label={`${game.name}: ${!game.available ? "non disponibile oggi" : game.completed ? "completato, vedi risultato" : game.description}`}>
            <span className="puzzle-card-heading"><span className="puzzle-category">{game.category}</span>{game.completed ? <span className="puzzle-status"><Check size={13} aria-hidden="true" /> Fatto</span> : index === 0 ? <span className="puzzle-tag">Il classico</span> : null}</span>
            <PuzzleArtwork kind={game.id} />
            <span className="puzzle-card-copy"><strong>{game.name}</strong><span>{game.available ? game.description : "Una nuova sfida è in arrivo."}</span></span>
            <span className="puzzle-card-footer"><span>{!game.available ? "Non disponibile oggi" : game.completed ? "Vedi risultato" : "Giochiamo"}</span><ArrowRight size={18} aria-hidden="true" /></span>
          </button>
        ))}
      </div>
      <div className="daily-note"><Clock3 size={17} aria-hidden="true" /><span>Sviluppato con ❤️ da vins e yumi.</span></p></div>
    </section>
  );
}
