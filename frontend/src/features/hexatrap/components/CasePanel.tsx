import type { HexatrapCaseDto } from "../../../types";
import type { Notebook } from "../hexatrapModel";
import { OfficialRecord, Portrait, Readings, SuspicionBadge } from "./CaseParts";

export function CasePanel({ caseFile, notebook, lens }: { caseFile: HexatrapCaseDto; notebook: Notebook; lens: boolean }) {
  return (
    <div className="trap-stack">
      <section className="trap-panel" aria-labelledby="trap-dossier-title">
        <h3 id="trap-dossier-title">Il dossier</h3>
        <p className="trap-intro">{caseFile.intro}</p>
        <p className="trap-rule">Uno dei due dice <strong>sempre</strong> la verità, l'altro mente <strong>sempre</strong>, su ogni affermazione e ogni risposta.</p>
        {caseFile.certainties.length ? (
          <>
            <h4>Certezze</h4>
            <ul className="trap-list">
              {caseFile.certainties.map((certainty) => (
                <li key={certainty.id}><p>{certainty.text}</p><OfficialRecord text={certainty.record} /></li>
              ))}
            </ul>
          </>
        ) : null}
      </section>
      {caseFile.characters.map((character) => (
        <section key={character.id} className={`trap-panel trap-character trap-character--${character.id}`} aria-label={`Deposizioni di ${character.name}`}>
          <header className="trap-character-head">
            <Portrait character={character} />
            <div>
              <h3>{character.name}</h3>
              <p>{character.role}</p>
            </div>
            <SuspicionBadge suspicion={notebook.suspicion[character.id]} />
          </header>
          <ul className="trap-list">
            {caseFile.statements.filter((statement) => statement.speaker === character.id).map((statement) => (
              <li key={statement.id}>
                <blockquote>«{statement.text}»</blockquote>
                <OfficialRecord text={statement.record} />
                {lens ? <Readings caseFile={caseFile} readings={statement.readings} /> : null}
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}
