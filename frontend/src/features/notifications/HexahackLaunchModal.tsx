import { RadioTower, X } from "lucide-react";

export function HexahackLaunchModal({ onPlay, onClose }: { onPlay: () => void; onClose: () => void }) {
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal launch-modal" role="dialog" aria-modal="true" aria-labelledby="launch-title" onMouseDown={(event) => event.stopPropagation()}>
        <header className="modal-head">
          <h2 id="launch-title">È arrivato Hexahack</h2>
          <button className="close-button" type="button" onClick={onClose} aria-label="Chiudi"><X size={19} /></button>
        </header>
        <RadioTower className="launch-icon" aria-hidden="true" />
        <p className="eyebrow">Un nuovo nodo è online</p>
        <p>Analizza il codice quotidiano con le sonde, proteggi lo Stealth e conquista il rango GHOST.</p>
        <button className="launch-cta" type="button" onClick={onPlay}>Entra nel nodo</button>
      </section>
    </div>
  );
}
