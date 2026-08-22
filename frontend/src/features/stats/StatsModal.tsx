import React from "react";
import { X } from "lucide-react";
import { Distribution } from "../../shared/components/Distribution";
import { Metric } from "../../shared/components/Metric";
import type { GameDto, HexahackRank, HexahackStatsDto, HexaskyStatsDto, HexasquareStatsDto, StatsDto, StatsSetDto } from "../../types";

export type StatsGame = "overall" | "hexaword" | "hexahack" | "hexasky" | "hexasquare";

const RANK_LABELS: Readonly<Record<HexahackRank, string>> = {
  GHOST: "Ghost",
  SHADOW: "Shadow",
  BREACH: "Breach",
  TRACED: "Traced"
};

export function StatsTabs({ active, onChange }: { active: StatsGame; onChange: (game: StatsGame) => void }) {
  return (
    <div className="game-stats-tabs" role="tablist" aria-label="Gioco">
      {([['overall', 'Overall'], ['hexaword', 'Hexaword'], ['hexahack', 'Hexahack'], ['hexasky', 'Hexasky'], ['hexasquare', 'Hexasquare']] as const).map(([id, label]) =>
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

export function HexahackStatsPanel({ stats }: { stats: HexahackStatsDto }) {
  return <section className="hack-mastery" aria-label="Maestria Hexahack">
    <header><strong>MAESTRIA</strong><span>ultimi 30 nodi</span></header>
    <div className="hack-metrics">
      <div><strong>{stats.completedAccesses}</strong><span>Accessi</span></div>
      <div><strong>{stats.averageStealth}</strong><span>Media</span></div>
      <div><strong>{stats.bestStealth}</strong><span>Migliore</span></div>
      <div><strong>{stats.currentStreak}</strong><span>Serie</span></div>
    </div>
    <div className="hack-ranks" aria-label="Distribuzione ranghi">
      {(Object.keys(RANK_LABELS) as HexahackRank[]).map((rank) => <span key={rank}>{RANK_LABELS[rank]} <strong>{stats.rankDistribution[rank] ?? 0}</strong></span>)}
    </div>
    <div className="hack-calendar">
      {stats.last30Nodes.map((node) => <span className={node.completed ? `rank-${node.rank?.toLowerCase()}` : "empty"} title={`${node.puzzleDate}${node.completed ? ` · ${node.rank} · ${node.stealth}` : " · non completato"}`} aria-label={`${node.puzzleDate}${node.completed ? `, rango ${node.rank}, Stealth ${node.stealth}` : ", non completato"}`} key={node.puzzleDate} />)}
    </div>
  </section>;
}

export function HexaskyStatsPanel({ stats }: { stats: HexaskyStatsDto }) {
  const winRate = stats.played ? Math.round((stats.won / stats.played) * 100) : 0;
  return <><div className="stat-grid"><Metric label="Giocate" value={stats.played} /><Metric label="Vinte" value={stats.won} /><Metric label="Perse" value={stats.lost} /><Metric label="Vittorie" value={`${winRate}%`} /></div><div className="stat-grid compact"><Metric label="Serie" value={stats.currentStreak} /><Metric label="Record" value={stats.maxStreak} /></div><div className="distribution"><h3>Controlli per vittoria</h3>{([1, 2] as const).map((check) => <div className="distribution-row" key={check}><span>{check}</span><div><i style={{ width: `${Math.min(100, (stats.checkDistribution[String(check) as "1" | "2"] ?? 0) * 10)}%` }} /></div><strong>{stats.checkDistribution[String(check) as "1" | "2"] ?? 0}</strong></div>)}</div></>;
}

export function HexasquareStatsPanel({ stats }: { stats: HexasquareStatsDto }) {
  return <><div className="stat-grid"><Metric label="Iniziate" value={stats.gamesStarted} /><Metric label="Completate" value={stats.completed} /><Metric label="Completamento" value={`${stats.completionPercentage}%`} /><Metric label="Serie" value={stats.currentStreak} /></div><div className="stat-grid compact"><Metric label="Record serie" value={stats.maxStreak} /><Metric label="Caselle medie" value={stats.averageCellsUsed} /><Metric label="Risparmio medio" value={stats.averageCellsSaved} /><Metric label="Record risparmio" value={stats.bestCellsSaved} /></div><p className="stats-note">Simulazioni medie per vittoria: <strong>{stats.averageSimulationsPerWin}</strong></p></>;
}

export function StatsModal({ game, stats, initialGame = "overall", onClose }: {
  game: GameDto | null;
  stats: StatsSetDto;
  initialGame?: StatsGame;
  onClose: () => void;
}) {
  const [active, setActive] = React.useState<StatsGame>(initialGame);
  const selected = active === "hexahack" || active === "hexasky" || active === "hexasquare" ? null : stats[active];
  const completed = active === "hexaword" ? game : null;
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal" role="dialog" aria-modal="true" onMouseDown={(event) => event.stopPropagation()}>
        <header className="modal-head"><h2>Statistiche</h2><button className="close-button" type="button" onClick={onClose} aria-label="Chiudi"><X size={19} /></button></header>
        <StatsTabs active={active} onChange={setActive} />
        {active === "hexahack" ? <HexahackStatsPanel stats={stats.hexahack} /> : active === "hexasky" ? <HexaskyStatsPanel stats={stats.hexasky} /> : active === "hexasquare" ? <HexasquareStatsPanel stats={stats.hexasquare} /> : <StatsPanel stats={selected} />}
        {completed?.status === "WON" ? <p className="result won">Risolta.</p> : null}
        {completed?.status === "LOST" ? <p className="result lost">Soluzione: {completed.solution?.toUpperCase()}</p> : null}
      </section>
    </div>
  );
}
