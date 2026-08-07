import type { MedalCountsDto } from "../../types";

export function MedalCounts({ medals }: { medals: MedalCountsDto }) {
  return (
    <section className="medal-counts" aria-label="Medaglie conquistate">
      <div className="medal-count gold">
        <span aria-hidden="true">🥇</span>
        <strong>{medals.gold}</strong>
        <small>Oro</small>
      </div>
      <div className="medal-count silver">
        <span aria-hidden="true">🥈</span>
        <strong>{medals.silver}</strong>
        <small>Argento</small>
      </div>
      <div className="medal-count bronze">
        <span aria-hidden="true">🥉</span>
        <strong>{medals.bronze}</strong>
        <small>Bronzo</small>
      </div>
    </section>
  );
}
