import { Binary, Check, SpellCheck2, Building2, Map } from "lucide-react";

export function GameSelector({
  hexawordCompleted,
  hexahackCompleted,
  hexaskyCompleted,
  hexasquareCompleted,
  onHexaword,
  onHexahack,
  onHexasky,
  onHexasquare
}: {
  hexawordCompleted: boolean;
  hexahackCompleted: boolean;
  hexaskyCompleted: boolean;
  hexasquareCompleted: boolean;
  onHexaword: () => void;
  onHexahack: () => void;
  onHexasky: () => void;
  onHexasquare: () => void;
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
        <button className={hexahackCompleted ? "completed" : ""} type="button" onClick={onHexahack}>
          <Binary className="game-selector-icon" aria-hidden="true" />
          <span className="game-selector-name">
            <strong>Hexahack</strong>
            {hexahackCompleted ? <small><Check aria-hidden="true" /> Accesso completato</small> : null}
          </span>
          <span>Viola il nodo senza perdere Stealth.</span>
        </button>
        <button className={hexaskyCompleted ? "completed" : ""} type="button" onClick={onHexasky}>
          <Building2 className="game-selector-icon" aria-hidden="true" />
          <span className="game-selector-name"><strong>Hexasky</strong>{hexaskyCompleted ? <small><Check aria-hidden="true" /> Completato</small> : null}</span>
          <span>Risolvi il grattacielo quotidiano 4×4.</span>
        </button>
        <button className={hexasquareCompleted ? "completed" : ""} type="button" onClick={onHexasquare}>
          <Map className="game-selector-icon" aria-hidden="true" />
          <span className="game-selector-name"><strong>Hexasquare</strong>{hexasquareCompleted ? <small><Check aria-hidden="true" /> Completato</small> : null}</span>
          <span>Progetta la rete urbana quotidiana 24×24.</span>
        </button>
      </div>
    </section>
  );
}
