import { Search } from "lucide-react";
import type { HexatrapCaseDto, HexatrapGameDto } from "../../../types";
import { canAfford, isClosed } from "../hexatrapModel";
import { OfficialRecord } from "./CaseParts";

export function EvidencePanel({ caseFile, game, busy, onReveal }: {
  caseFile: HexatrapCaseDto; game: HexatrapGameDto | null; busy: boolean; onReveal: (clueId: string) => void;
}) {
  const revealed = new Map(game?.revealed.map((entry) => [entry.clueId, entry]) ?? []);
  return (
    <div className="trap-stack">
      <p className="trap-hint">I reperti dicono sempre il vero, ma esaminarli richiede tempo. Il titolo non rivela il contenuto.</p>
      <ul className="trap-evidence">
        {caseFile.clues.map((clue) => {
          const content = revealed.get(clue.id);
          const affordable = canAfford(caseFile, game, clue.cost);
          return (
            <li key={clue.id} className={`trap-card${content ? " is-revealed" : ""}`}>
              <div className="trap-card-head">
                <h3>{clue.title}</h3>
                <span className="trap-chip">{clue.cost} min</span>
              </div>
              {content ? (
                <>
                  <p>{content.text}</p>
                  <OfficialRecord text={content.record} />
                </>
              ) : (
                <button type="button" className="trap-action" disabled={busy || !affordable} onClick={() => onReveal(clue.id)}>
                  <Search size={18} aria-hidden="true" />
                  {isClosed(game) ? "Indagine chiusa" : affordable ? `Esamina · ${clue.cost} min` : "Tempo insufficiente"}
                </button>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
