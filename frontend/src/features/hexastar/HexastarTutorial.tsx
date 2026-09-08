import { createPortal } from "react-dom";
import { X } from "lucide-react";

export function HexastarTutorial({ onClose }: { onClose: () => void }) {
  return createPortal(
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal word-tutorial" role="dialog" aria-modal="true" aria-labelledby="star-tutorial-title" onMouseDown={(event) => event.stopPropagation()}>
        <header className="modal-head">
          <h2 id="star-tutorial-title">Come si gioca a Hexastar</h2>
          <button className="close-button" type="button" onClick={onClose} aria-label="Chiudi"><X size={19} /></button>
        </header>
        <p>Trova la parola italiana di sei lettere in sei tentativi, inserendola attraverso le sue sillabe.</p>
        <div className="hexastar-tutorial-example" aria-label="Esempio: CASATA divisa in CA, SA, TA">
          <span>CA</span><span>SA</span><span>TA</span>
        </div>
        <p>Gli spazi mostrano quante lettere contiene ogni sillaba. La parola viene validata soltanto quando tutte le sillabe sono complete.</p>
        <div className="word-tutorial-examples" aria-label="Esempi di indizi">
          <div><span className="correct">CA</span><p><strong>Verde</strong>: la sillaba è nella posizione giusta.</p></div>
          <div><span className="present">SA</span><p><strong>Giallo</strong>: la sillaba c’è, ma va spostata.</p></div>
          <div><span className="absent">TA</span><p><strong>Grigio</strong>: la sillaba non è nella parola.</p></div>
        </div>
        <button className="sky-check" type="button" onClick={onClose}>Ho capito</button>
      </section>
    </div>,
    document.body
  );
}
