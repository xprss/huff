import React from "react";
import { Gavel } from "lucide-react";
import type { HexatrapCaseDto, HexatrapGameDto } from "../../../types";
import { anyActionLeft, isClosed, verdictFacts, verdictReady, type VerdictDraft } from "../hexatrapModel";
import { Portrait } from "./CaseParts";

export function VerdictPanel({ caseFile, game, draft, busy, onChange, onSubmit }: {
  caseFile: HexatrapCaseDto; game: HexatrapGameDto | null; draft: VerdictDraft; busy: boolean;
  onChange: (draft: VerdictDraft) => void; onSubmit: () => void;
}) {
  const [confirming, setConfirming] = React.useState(false);
  const ready = verdictReady(caseFile, draft);
  const closed = isClosed(game);
  React.useEffect(() => { if (!ready) setConfirming(false); }, [ready]);
  return (
    <div className="trap-stack">
      <section className="trap-panel trap-verdict" aria-labelledby="trap-verdict-title">
        <h3 id="trap-verdict-title">Il verdetto</h3>
        <p className="trap-hint">
          Unico e definitivo. Vinci se indichi chi mente <strong>e</strong> ricostruisci i fatti richiesti; il tempo che avanza vale punti.
          {!closed && !anyActionLeft(caseFile, game) ? " Il tempo per indagare è finito: ora tocca a te." : ""}
        </p>
        <fieldset disabled={closed || busy}>
          <legend>Chi mente?</legend>
          <div className="trap-liar-choice">
            {caseFile.characters.map((character) => (
              <label key={character.id} className={`trap-choice${draft.liar === character.id ? " is-selected" : ""}`}>
                <input type="radio" name="trap-liar" value={character.id} checked={draft.liar === character.id}
                  onChange={() => onChange({ ...draft, liar: character.id })} />
                <Portrait character={character} size={22} />
                <span><strong>{character.name}</strong><small>{character.role}</small></span>
              </label>
            ))}
          </div>
        </fieldset>
        {verdictFacts(caseFile).map((fact) => (
          <fieldset key={fact.id} disabled={closed || busy}>
            <legend>{fact.label}</legend>
            <div className="trap-options">
              {fact.options.map((option) => (
                <label key={option.id} className={`trap-choice trap-choice--compact${draft.facts[fact.id] === option.id ? " is-selected" : ""}`}>
                  <input type="radio" name={`trap-fact-${fact.id}`} value={option.id} checked={draft.facts[fact.id] === option.id}
                    onChange={() => onChange({ ...draft, facts: { ...draft.facts, [fact.id]: option.id } })} />
                  <span>{option.label}</span>
                </label>
              ))}
            </div>
          </fieldset>
        ))}
        {closed ? null : confirming ? (
          <div className="trap-confirm" role="alertdialog" aria-labelledby="trap-confirm-text">
            <p id="trap-confirm-text">Il verdetto non si può cambiare. Sei sicuro?</p>
            <div>
              <button type="button" onClick={() => setConfirming(false)} disabled={busy}>Ricontrollo</button>
              <button type="button" className="trap-primary" onClick={onSubmit} disabled={busy}><Gavel size={18} aria-hidden="true" /> Confermo</button>
            </div>
          </div>
        ) : (
          <button type="button" className="trap-primary" disabled={!ready || busy} onClick={() => setConfirming(true)}>
            <Gavel size={18} aria-hidden="true" /> Emetti il verdetto
          </button>
        )}
      </section>
    </div>
  );
}
