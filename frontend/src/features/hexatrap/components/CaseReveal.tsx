import React from "react";
import { Check, Copy, Route, Share2, X } from "lucide-react";
import type { HexatrapCaseDto, HexatrapEvidenceDto, HexatrapGameDto } from "../../../types";
import { characterName, optionLabel, outcomeTitle, verdictFacts } from "../hexatrapModel";

const KIND_LABEL: Record<HexatrapEvidenceDto["kind"], string> = {
  CERTAINTY: "Certezza", STATEMENT: "Deposizione", QUESTION: "Risposta", CLUE: "Reperto", UNKNOWN: "Elemento"
};

function EvidenceList({ items }: { items: readonly HexatrapEvidenceDto[] }) {
  return <ol className="trap-proof">{items.map((item) => <li key={item.id}><small>{KIND_LABEL[item.kind]}</small><span>{item.label}</span></li>)}</ol>;
}

export function CaseReveal({ caseFile, game }: { caseFile: HexatrapCaseDto; game: HexatrapGameDto }) {
  const reveal = game.reveal;
  const [copied, setCopied] = React.useState(false);
  if (!reveal || !game.verdict) return null;
  const verdict = game.verdict;
  async function share() {
    const text = reveal!.share;
    try {
      if (navigator.share) await navigator.share({ text });
      else { await navigator.clipboard.writeText(text); setCopied(true); }
    } catch {
      // Sharing was cancelled or is unavailable: nothing to do.
    }
  }
  return (
    <section className={`trap-panel trap-reveal trap-reveal--${game.outcome?.toLowerCase()}`} aria-labelledby="trap-reveal-title" tabIndex={-1}>
      <p className="eyebrow">Verdetto emesso</p>
      <h3 id="trap-reveal-title">{outcomeTitle(game.outcome)}</h3>
      {game.outcome === "SOLVED" ? <p className="trap-points"><strong>{game.points}</strong> punti · {game.minutesUsed} minuti d'indagine</p> : null}
      <ul className="trap-truth">
        <li className={reveal.liarCorrect ? "ok" : "ko"}>
          {reveal.liarCorrect ? <Check size={18} aria-hidden="true" /> : <X size={18} aria-hidden="true" />}
          <span>Mentiva <strong>{characterName(caseFile, reveal.liar)}</strong>{reveal.liarCorrect ? "" : ` (hai accusato ${characterName(caseFile, verdict.liar)})`}</span>
        </li>
        {verdictFacts(caseFile).map((fact) => {
          const right = reveal.facts[fact.id] === verdict.facts[fact.id];
          return (
            <li key={fact.id} className={right ? "ok" : "ko"}>
              {right ? <Check size={18} aria-hidden="true" /> : <X size={18} aria-hidden="true" />}
              <span>{fact.label} <strong>{optionLabel(caseFile, fact.id, reveal.facts[fact.id])}</strong>{right ? "" : ` (hai detto ${optionLabel(caseFile, fact.id, verdict.facts[fact.id])})`}</span>
            </li>
          );
        })}
      </ul>
      <h4>La prova</h4>
      {reveal.proofFound ? (
        <>
          <p className="trap-hint">Bastavano questi elementi fra quelli che avevi raccolto per escludere che mentisse {characterName(caseFile, reveal.liar === "A" ? "B" : "A")}:</p>
          <EvidenceList items={reveal.proof} />
        </>
      ) : <p className="trap-hint">Gli elementi raccolti non bastavano ancora a stabilire con certezza chi mentiva.</p>}
      <h4><Route size={16} aria-hidden="true" /> Il percorso più rapido</h4>
      <p className="trap-hint">Con {reveal.optimalMinutes} minuti si poteva chiudere il caso; un investigatore esperto ne impiega {reveal.expertMinutes}. Tu ne hai usati {game.minutesUsed}.</p>
      <EvidenceList items={reveal.optimalPath} />
      <h4>Com'è andata davvero</h4>
      <p className="trap-epilogue">{reveal.epilogue}</p>
      <button type="button" className="trap-action" onClick={() => void share()}>
        {copied ? <Copy size={18} aria-hidden="true" /> : <Share2 size={18} aria-hidden="true" />}{copied ? "Risultato copiato" : "Condividi il risultato"}
      </button>
      <p className="trap-hint">Domani un nuovo caso, scritto stanotte.</p>
    </section>
  );
}
