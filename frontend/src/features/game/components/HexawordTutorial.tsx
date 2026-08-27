import { createPortal } from "react-dom";
import { X } from "lucide-react";

export function HexawordTutorial({ onClose }: { onClose: () => void }) {
  return createPortal(
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal word-tutorial" role="dialog" aria-modal="true" aria-labelledby="word-tutorial-title" onMouseDown={(event) => event.stopPropagation()}>
        <header className="modal-head">
          <h2 id="word-tutorial-title">Come si gioca a Hexaword</h2>
          <button className="close-button" type="button" onClick={onClose} aria-label="Chiudi"><X size={19} /></button>
        </header>
        <p>Trova la parola italiana di sei lettere in sei tentativi. Ogni parola inserita deve essere valida.</p>
        <div className="word-tutorial-examples" aria-label="Esempi di indizi">
          <div><span className="correct">A</span><p><strong>Verde</strong>: la lettera è nella posizione giusta.</p></div>
          <div><span className="present">R</span><p><strong>Giallo</strong>: la lettera c'è, ma va spostata.</p></div>
          <div><span className="absent">T</span><p><strong>Grigio</strong>: la lettera non è nella parola.</p></div>
        </div>
        <div className="word-mode-tutorials">
          <section>
            <h3><span className="classic-mark" aria-hidden="true" /> Classica</h3>
            <p>Usa liberamente gli indizi ottenuti per trovare la soluzione.</p>
          </section>
          <section>
            <h3><span aria-hidden="true">🐭</span> Topolino dispettoso</h3>
            <p>Il primo risultato nasconde una casella. Ottieni almeno tre verdi per sbloccare il gattino e rivelarla.</p>
          </section>
          <section className="crab-tutorial">
            <h3><span aria-hidden="true">🦀</span> Granchio testardo</h3>
            <p>Dal tentativo successivo, ogni indizio scoperto diventa obbligatorio e continua a valere fino alla fine:</p>
            <ul>
              <li><strong>Verde:</strong> la lettera resta bloccata nella stessa casella.</li>
              <li><strong>Giallo:</strong> devi riutilizzare la lettera, spostandola dalle caselle in cui è risultata gialla.</li>
              <li><strong>Copie:</strong> due lettere confermate richiedono due copie nei tentativi successivi.</li>
              <li><strong>Grigio:</strong> non aggiunge un nuovo obbligo.</li>
            </ul>
            <div className="crab-tutorial-example" aria-label="Esempio: A verde bloccata e due R gialle obbligatorie">
              <span className="correct">A</span>
              <span className="present">R</span>
              <span className="present">R</span>
              <small>→ A bloccata, R ×2 da spostare</small>
            </div>
          </section>
        </div>
        <p>Puoi scegliere e cambiare modalità soltanto prima del primo tentativo.</p>
        <button className="sky-check" type="button" onClick={onClose}>Ho capito</button>
      </section>
    </div>,
    document.body
  );
}
