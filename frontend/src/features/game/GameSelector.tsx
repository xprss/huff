import React from "react";
import { ArrowUpRight, Check } from "lucide-react";

export function GameSelector({
  hexawordCompleted,
  hexahackCompleted,
  hexaskyCompleted,
  hexaflowCompleted,
  hexaflowAvailable,
  onHexaword,
  onHexahack,
  onHexasky,
  onHexaflow
}: {
  hexawordCompleted: boolean;
  hexahackCompleted: boolean;
  hexaskyCompleted: boolean;
  hexaflowCompleted: boolean;
  hexaflowAvailable: boolean;
  onHexaword: () => void;
  onHexahack: () => void;
  onHexasky: () => void;
  onHexaflow: () => void;
}) {
  const selectorRef = React.useRef<HTMLElement | null>(null);

  React.useEffect(() => {
    selectorRef.current?.scrollTo(0, 0);
  }, []);

  return (
    <section ref={selectorRef} className="game-selector" aria-labelledby="game-selector-title">
      <div>
        <p className="eyebrow">Oggi</p>
        <h2 id="game-selector-title">Cosa si gioca?</h2>
        <p>Quattro sfide indipendenti. Ognuna tiene i suoi punteggi.</p>
      </div>
      <div className="game-selector-grid">
        <button className={`game-selector-card game-selector-card--word${hexawordCompleted ? " completed" : ""}`} type="button" onClick={onHexaword}>
          <span className="game-selector-card-top">
            <span className="game-selector-icon"><span className="game-selector-index" aria-hidden="true">01</span></span>
          </span>
          <span className="game-selector-copy">
            <span className="game-selector-name">
              <strong>Hexaword</strong>
              {hexawordCompleted ? <small><Check aria-hidden="true" /> Risolta</small> : null}
            </span>
            <span className="game-selector-description">Sei lettere, sei tentativi.</span>
          </span>
          <span className="game-selector-open" aria-hidden="true"><ArrowUpRight /></span>
        </button>
        <button className={`game-selector-card game-selector-card--hack${hexahackCompleted ? " completed" : ""}`} type="button" onClick={onHexahack}>
          <span className="game-selector-card-top">
            <span className="game-selector-icon"><span className="game-selector-index" aria-hidden="true">02</span></span>
          </span>
          <span className="game-selector-copy">
            <span className="game-selector-name">
              <strong>Hexahack</strong>
              {hexahackCompleted ? <small><Check aria-hidden="true" /> Risolta</small> : null}
            </span>
            <span className="game-selector-description">Sei cifre, senza farti tracciare.</span>
          </span>
          <span className="game-selector-open" aria-hidden="true"><ArrowUpRight /></span>
        </button>
        <button className={`game-selector-card game-selector-card--sky${hexaskyCompleted ? " completed" : ""}`} type="button" onClick={onHexasky}>
          <span className="game-selector-card-top">
            <span className="game-selector-icon"><span className="game-selector-index" aria-hidden="true">03</span></span>
          </span>
          <span className="game-selector-copy">
            <span className="game-selector-name"><strong>Hexasky</strong>{hexaskyCompleted ? <small><Check aria-hidden="true" /> Completato</small> : null}</span>
            <span className="game-selector-description">Quattro altezze, quattro scorci.</span>
          </span>
          <span className="game-selector-open" aria-hidden="true"><ArrowUpRight /></span>
        </button>
        <button className={`game-selector-card game-selector-card--flow${hexaflowCompleted ? " completed" : ""}`} type="button" onClick={onHexaflow} disabled={!hexaflowAvailable}>
          <span className="game-selector-card-top">
            <span className="game-selector-icon"><span className="game-selector-index" aria-hidden="true">04</span></span>
          </span>
          <span className="game-selector-copy">
            <span className="game-selector-name"><strong>Hexaflow</strong>{hexaflowCompleted ? <small><Check aria-hidden="true" /> Completato</small> : null}</span>
            <span className="game-selector-description">{hexaflowAvailable ? "Collega le parole del tema." : "Torna domani."}</span>
          </span>
          <span className="game-selector-open" aria-hidden="true"><ArrowUpRight /></span>
        </button>
      </div>
    </section>
  );
}
