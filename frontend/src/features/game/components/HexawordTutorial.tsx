import { createPortal } from "react-dom";
import { X } from "lucide-react";

export function HexawordTutorial({ onClose }: { onClose: () => void }) {
  return createPortal(
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal word-tutorial" role="dialog" aria-modal="true" aria-labelledby="word-tutorial-title" onMouseDown={(event) => event.stopPropagation()}>
        <button className="close-button" type="button" onClick={onClose} aria-label="Chiudi"><X size={19} /></button>
        <h2 id="word-tutorial-title">Come si gioca a Hexaword</h2>
        <p>Trova la parola italiana di sei lettere in sei tentativi. Ogni parola inserita deve essere valida.</p>
        <div className="word-tutorial-examples" aria-label="Esempi di indizi">
          <div><span className="correct">A</span><p><strong>Verde</strong>: la lettera è nella posizione giusta.</p></div>
          <div><span className="present">R</span><p><strong>Giallo</strong>: la lettera c'è, ma va spostata.</p></div>
          <div><span className="absent">T</span><p><strong>Grigio</strong>: la lettera non è nella parola.</p></div>
        </div>
        <p>Puoi scegliere la modalità prima del primo tentativo. In <strong>Topolino dispettoso</strong>, il primo risultato nasconde una casella: ottieni almeno tre verdi per sbloccare il gattino e rivelarla.</p>
        <button className="sky-check" type="button" onClick={onClose}>Ho capito</button>
      </section>
    </div>,
    document.body
  );
}
