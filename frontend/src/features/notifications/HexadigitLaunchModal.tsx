import { Binary, X } from "lucide-react";

export function HexadigitLaunchModal({ onPlay, onClose }: { onPlay: () => void; onClose: () => void }) {
  return (
    <div className="modal-backdrop" role="presentation">
      <section className="modal launch-modal" role="dialog" aria-modal="true" aria-labelledby="launch-title">
        <button className="close-button" type="button" onClick={onClose} aria-label="Chiudi"><X size={19} /></button>
        <Binary className="launch-icon" aria-hidden="true" />
        <p className="eyebrow">Nuovo gioco</p>
        <h2 id="launch-title">È arrivato Hexadigit</h2>
        <p>Hai sei tentativi per trovare il codice quotidiano. I colori mostrano posizione e presenza; i simboli indicano se cercare più in alto o più in basso.</p>
        <button className="launch-cta" type="button" onClick={onPlay}>Gioca a Hexadigit</button>
      </section>
    </div>
  );
}
