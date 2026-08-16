import { Binary, Check, SpellCheck2 } from "lucide-react";

export function GameSelector({
  hexawordCompleted,
  showHexahack,
  hexahackCompleted,
  onHexaword,
  onHexahack
}: {
  hexawordCompleted: boolean;
  showHexahack: boolean;
  hexahackCompleted: boolean;
  onHexaword: () => void;
  onHexahack: () => void;
}) {
  return (
    <section className="game-selector" aria-labelledby="game-selector-title">
      <div>
        <p className="eyebrow">La sfida quotidiana</p>
        <h2 id="game-selector-title">A cosa giochiamo?</h2>
        <p>Scegli il gioco di oggi. Le partite e le statistiche restano indipendenti.</p>
      </div>
      <div className="game-selector-grid">
        <button className={hexawordCompleted ? "completed" : ""} type="button" onClick={onHexaword}>
          <SpellCheck2 className="game-selector-icon" aria-hidden="true" />
          <span className="game-selector-name">
            <strong>Hexaword</strong>
            {hexawordCompleted ? <small><Check aria-hidden="true" /> Completato</small> : null}
          </span>
          <span>Trova la parola italiana di sei lettere.</span>
        </button>
        {showHexahack ? (
          <button className={hexahackCompleted ? "completed" : ""} type="button" onClick={onHexahack}>
            <Binary className="game-selector-icon" aria-hidden="true" />
            <span className="game-selector-name">
              <strong>Hexahack</strong>
              {hexahackCompleted ? <small><Check aria-hidden="true" /> Accesso completato</small> : null}
            </span>
            <span>Viola il nodo senza perdere Stealth.</span>
          </button>
        ) : null}
      </div>
    </section>
  );
}
