import type { HexatrapCaseDto } from "../../../types";
import type { Mark, Notebook } from "../hexatrapModel";
import { Portrait } from "./CaseParts";

const MARK_LABEL: Record<Mark, string> = { unknown: "da verificare", no: "escluso", yes: "confermato" };
const MARK_SIGN: Record<Mark, string> = { unknown: "·", no: "✕", yes: "✓" };

export function NotebookPanel({ caseFile, notebook, onMark, onSuspect, onReset }: {
  caseFile: HexatrapCaseDto; notebook: Notebook;
  onMark: (factId: string, optionId: string) => void; onSuspect: (id: "A" | "B") => void; onReset: () => void;
}) {
  return (
    <div className="trap-stack">
      <section className="trap-panel" aria-labelledby="trap-notebook-title">
        <h3 id="trap-notebook-title">Il taccuino</h3>
        <p className="trap-hint">Appunti personali, salvati solo su questo dispositivo: non cambiano risposte né punteggio. Tocca una casella per passare da da verificare a escluso a confermato.</p>
        <div className="trap-suspects" role="group" aria-label="Chi sospetti?">
          {caseFile.characters.map((character) => {
            const suspicion = notebook.suspicion[character.id];
            return (
              <button key={character.id} type="button" className={`trap-suspect is-${suspicion}`} onClick={() => onSuspect(character.id)}
                aria-label={`${character.name}: ${suspicion === "unknown" ? "nessun sospetto" : suspicion === "liar" ? "sospetto bugiardo" : "sospetto sincero"}`}>
                <Portrait character={character} size={20} />
                <span>{character.name}</span>
                <small>{suspicion === "unknown" ? "?" : suspicion === "liar" ? "mente" : "sincero"}</small>
              </button>
            );
          })}
        </div>
        {caseFile.facts.map((fact) => (
          <fieldset key={fact.id} className="trap-grid-row">
            <legend>{fact.label}{fact.verdict ? <span className="trap-chip trap-chip--verdict">verdetto</span> : null}</legend>
            <div>
              {fact.options.map((option) => {
                const mark = notebook.facts[fact.id]?.[option.id] ?? "unknown";
                return (
                  <button key={option.id} type="button" className={`trap-mark is-${mark}`} onClick={() => onMark(fact.id, option.id)}
                    aria-label={`${option.label}: ${MARK_LABEL[mark]}`}>
                    <span aria-hidden="true">{MARK_SIGN[mark]}</span>{option.label}
                  </button>
                );
              })}
            </div>
          </fieldset>
        ))}
        <button type="button" className="trap-link" onClick={onReset}>Svuota il taccuino</button>
      </section>
    </div>
  );
}
