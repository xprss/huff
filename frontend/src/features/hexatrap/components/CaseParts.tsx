import { Hourglass, ScrollText } from "lucide-react";
import type { HexatrapCaseDto, HexatrapCharacterDto, HexatrapCharacterId, HexatrapGameDto, HexatrapReadingsDto } from "../../../types";
import { capitalize, characterName, difficultyLabel, minutesLeft, type Suspicion } from "../hexatrapModel";
import { characterIcon } from "./icons";

export function CaseClock({ caseFile, game }: { caseFile: HexatrapCaseDto; game: HexatrapGameDto | null }) {
  const left = minutesLeft(caseFile, game);
  const share = Math.max(0, Math.min(1, left / caseFile.budget));
  return (
    <div className="trap-clock" role="group" aria-label={`Tempo d'indagine: ${left} minuti rimasti su ${caseFile.budget}`}>
      <span className="trap-clock-label"><Hourglass size={16} aria-hidden="true" /> <strong>{left}</strong> min rimasti</span>
      <span className="trap-clock-track" aria-hidden="true"><span style={{ width: `${share * 100}%` }} className={share <= 0.25 ? "is-low" : undefined} /></span>
    </div>
  );
}

export function CaseMeta({ caseFile }: { caseFile: HexatrapCaseDto }) {
  return (
    <p className="trap-meta">
      <span className="trap-chip">{capitalize(caseFile.format)}</span>
      <span className="trap-chip" aria-label={`Difficoltà ${caseFile.difficulty} su 5: ${difficultyLabel(caseFile.difficulty)}`}>
        <span className="trap-dots" aria-hidden="true">{[1, 2, 3, 4, 5].map((n) => <i key={n} className={n <= caseFile.difficulty ? "on" : undefined} />)}</span>
        {difficultyLabel(caseFile.difficulty)}
      </span>
    </p>
  );
}

export function Portrait({ character, size = 26 }: { character: HexatrapCharacterDto; size?: number }) {
  const Icon = characterIcon(character.icon);
  return <span className={`trap-portrait trap-portrait--${character.id}`} aria-hidden="true"><Icon size={size} /></span>;
}

export function SuspicionBadge({ suspicion }: { suspicion: Suspicion }) {
  if (suspicion === "unknown") return null;
  return <span className={`trap-suspicion trap-suspicion--${suspicion}`}>{suspicion === "liar" ? "sospetto bugiardo" : "sospetto sincero"}</span>;
}

/** The official record: the authoritative wording of an item, written by the server. */
export function OfficialRecord({ text }: { text: string }) {
  return <p className="trap-record"><ScrollText size={14} aria-hidden="true" /><span><span className="trap-visually-hidden">Verbale: </span>{text}</span></p>;
}

export function Readings({ caseFile, readings }: { caseFile: HexatrapCaseDto; readings: HexatrapReadingsDto }) {
  const rows: [HexatrapCharacterId, string][] = [["A", readings.ifALies], ["B", readings.ifBLies]];
  return (
    <dl className="trap-readings">
      {rows.map(([liar, text]) => (
        <div key={liar} className={text.startsWith("Contraddizione") ? "is-contradiction" : undefined}>
          <dt>Se mente {characterName(caseFile, liar)}</dt>
          <dd>{text}</dd>
        </div>
      ))}
    </dl>
  );
}
