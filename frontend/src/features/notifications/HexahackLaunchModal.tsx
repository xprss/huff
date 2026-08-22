import { Map, RadioTower, X } from "lucide-react";
import type { AnnouncementCampaign } from "../../types";

const CAMPAIGNS = {
  HEXAHACK_LAUNCH: { title: "È arrivato Hexahack", eyebrow: "Un nuovo nodo è online", body: "Analizza il codice quotidiano con le sonde, proteggi lo Stealth e conquista il rango GHOST.", cta: "Entra nel nodo" },
  HEXASKY_LAUNCH: { title: "È arrivato Hexasky", eyebrow: "La skyline cambia", body: "Osserva gli indizi e risolvi il nuovo grattacielo quotidiano.", cta: "Sali sul tetto" },
  HEXASQUARE_LAUNCH: { title: "È arrivato Hexasquare", eyebrow: "La città ha bisogno di te", body: "Costruisci una rete stradale e porta ogni viaggiatore a destinazione, rispettando quartieri vietati e incompatibilità.", cta: "Apri il progetto" }
} satisfies Record<AnnouncementCampaign, { title: string; eyebrow: string; body: string; cta: string }>;

export function LaunchAnnouncementModal({ campaign, onPlay, onClose }: { campaign: AnnouncementCampaign; onPlay: () => void; onClose: () => void }) {
  const content=CAMPAIGNS[campaign];
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal launch-modal" role="dialog" aria-modal="true" aria-labelledby="launch-title" onMouseDown={(event) => event.stopPropagation()}>
        <header className="modal-head">
          <h2 id="launch-title">{content.title}</h2>
          <button className="close-button" type="button" onClick={onClose} aria-label="Chiudi"><X size={19} /></button>
        </header>
        {campaign==="HEXASQUARE_LAUNCH"?<Map className="launch-icon" aria-hidden="true" />:<RadioTower className="launch-icon" aria-hidden="true" />}
        <p className="eyebrow">{content.eyebrow}</p>
        <p>{content.body}</p>
        <button className="launch-cta" type="button" onClick={onPlay}>{content.cta}</button>
      </section>
    </div>
  );
}

export function HexahackLaunchModal({ onPlay, onClose }: { onPlay: () => void; onClose: () => void }) {
  return <LaunchAnnouncementModal campaign="HEXAHACK_LAUNCH" onPlay={onPlay} onClose={onClose}/>;
}
