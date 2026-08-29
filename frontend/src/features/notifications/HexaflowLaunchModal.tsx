import { Waves, X } from "lucide-react";

export function HexaflowLaunchModal({ onPlay, onClose }: { onPlay: () => void; onClose: () => void }) {
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal launch-modal hexaflow-launch-modal" role="dialog" aria-modal="true" aria-labelledby="hexaflow-launch-title" onMouseDown={(event) => event.stopPropagation()}>
        <header className="modal-head">
          <h2 id="hexaflow-launch-title">È arrivato Hexaflow</h2>
          <button className="close-button" type="button" onClick={onClose} aria-label="Chiudi"><X size={19} /></button>
        </header>
        <Waves className="launch-icon" aria-hidden="true" />
        <p className="eyebrow">Segui il flusso delle parole</p>
        <p>Collega le lettere in ogni direzione, scopri le parole del tema e trova il Flusso che attraversa tutta la griglia.</p>
        <p className="launch-modal-note">Ogni tre sequenze extra ottieni un suggerimento.</p>
        <button className="launch-cta" type="button" onClick={onPlay}>Gioca a Hexaflow</button>
      </section>
    </div>
  );
}
