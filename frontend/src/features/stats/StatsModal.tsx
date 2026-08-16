import React from "react";
import { X } from "lucide-react";
import { Distribution } from "../../shared/components/Distribution";
import { Metric } from "../../shared/components/Metric";
import type { GameDto, StatsDto, StatsSetDto } from "../../types";

export type StatsGame = "overall" | "hexaword";

export function StatsTabs({ active, onChange }: { active: StatsGame; onChange: (game: StatsGame) => void }) {
  return (
    <div className="game-stats-tabs" role="tablist" aria-label="Gioco">
      {([['overall', 'Overall'], ['hexaword', 'Hexaword']] as const).map(([id, label]) =>
        <button key={id} type="button" role="tab" aria-selected={active === id} className={active === id ? "selected" : ""} onClick={() => onChange(id)}>{label}</button>
      )}
    </div>
  );
}

export function StatsPanel({ stats }: { stats: StatsDto | null }) {
  const winRate = stats && stats.played > 0 ? Math.round((stats.won / stats.played) * 100) : 0;
  return <>
    <div className="stat-grid">
      <Metric label="Giocate" value={stats?.played ?? 0} />
      <Metric label="Vinte" value={stats?.won ?? 0} />
      <Metric label="Perse" value={stats?.lost ?? 0} />
      <Metric label="Vittorie" value={`${winRate}%`} />
    </div>
    <div className="stat-grid compact">
      <Metric label="Serie" value={stats?.currentStreak ?? 0} />
      <Metric label="Record" value={stats?.maxStreak ?? 0} />
    </div>
    <Distribution distribution={stats?.guessDistribution ?? {}} />
  </>;
}

export function StatsModal({ game, stats, initialGame = "overall", onClose }: {
  game: GameDto | null;
  stats: StatsSetDto;
  initialGame?: StatsGame;
  onClose: () => void;
}) {
  const [active, setActive] = React.useState<StatsGame>(initialGame);
  const selected = stats[active];
  const completed = active === "hexaword" ? game : null;
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal" role="dialog" aria-modal="true" onMouseDown={(event) => event.stopPropagation()}>
        <header className="modal-head"><h2>Statistiche</h2><button className="close-button" type="button" onClick={onClose} aria-label="Chiudi"><X size={19} /></button></header>
        <StatsTabs active={active} onChange={setActive} />
        <StatsPanel stats={selected} />
        {completed?.status === "WON" ? <p className="result won">Risolta.</p> : null}
        {completed?.status === "LOST" ? <p className="result lost">Soluzione: {completed.solution?.toUpperCase()}</p> : null}
      </section>
    </div>
  );
}
