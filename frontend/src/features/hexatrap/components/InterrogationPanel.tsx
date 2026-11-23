import { MessageCircleQuestion } from "lucide-react";
import type { HexatrapCaseDto, HexatrapGameDto } from "../../../types";
import { canAfford, isClosed } from "../hexatrapModel";
import { OfficialRecord, Portrait, Readings } from "./CaseParts";

export function InterrogationPanel({ caseFile, game, lens, busy, onAsk }: {
  caseFile: HexatrapCaseDto; game: HexatrapGameDto | null; lens: boolean; busy: boolean; onAsk: (questionId: string) => void;
}) {
  const asked = new Map(game?.asked.map((entry) => [entry.questionId, entry]) ?? []);
  return (
    <div className="trap-stack">
      <p className="trap-hint">Ogni domanda costa <strong>{caseFile.questionCost} minuti</strong> e si può porre una sola volta. Il sincero risponde il vero, il bugiardo il contrario.</p>
      {caseFile.characters.map((character) => (
        <section key={character.id} className={`trap-panel trap-character--${character.id}`} aria-label={`Domande per ${character.name}`}>
          <header className="trap-character-head">
            <Portrait character={character} size={22} />
            <h3>Interroga {character.name}</h3>
          </header>
          <ul className="trap-list">
            {caseFile.questions.filter((question) => question.to === character.id).map((question) => {
              const answer = asked.get(question.id);
              const affordable = canAfford(caseFile, game, caseFile.questionCost);
              return (
                <li key={question.id} className={answer ? "is-answered" : undefined}>
                  <p className="trap-question">{question.text}</p>
                  <OfficialRecord text={question.record} />
                  {answer ? (
                    <div className="trap-answer">
                      <span className={`trap-answer-word trap-answer-word--${answer.answer ? "yes" : "no"}`}>{character.name}: «{answer.answer ? "Sì" : "No"}»</span>
                      {lens ? <Readings caseFile={caseFile} readings={answer.readings} /> : null}
                    </div>
                  ) : (
                    <button type="button" className="trap-action" disabled={busy || !affordable} onClick={() => onAsk(question.id)}>
                      <MessageCircleQuestion size={18} aria-hidden="true" />
                      {isClosed(game) ? "Indagine chiusa" : affordable ? `Chiedi · ${caseFile.questionCost} min` : "Tempo insufficiente"}
                    </button>
                  )}
                </li>
              );
            })}
          </ul>
        </section>
      ))}
    </div>
  );
}
