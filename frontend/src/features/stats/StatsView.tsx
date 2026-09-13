import React from "react";
import { BarChart3 } from "lucide-react";
import { Distribution } from "../../shared/components/Distribution";
import { Metric } from "../../shared/components/Metric";
import type { HexahackRank, HexahackStatsDto, HexaskyStatsDto, HexaflowStatsDto, StatsDto, StatsSetDto } from "../../types";

export type StatsGame = "overall" | "hexaword" | "hexahack" | "hexasky" | "hexaflow" | "hexastar";

const RANK_LABELS: Readonly<Record<HexahackRank, string>> = {
  GHOST: "Ghost",
  SHADOW: "Shadow",
  BREACH: "Breach",
  TRACED: "Traced"
};

export function StatsTabs({ active, onChange }: { active: StatsGame; onChange: (game: StatsGame) => void }) {
  return (
    <div className="game-stats-tabs" role="tablist" aria-label="Gioco">
      {([['overall', 'Tutti'], ['hexaword', 'Hexaword'], ['hexahack', 'Hexahack'], ['hexasky', 'Hexasky'], ['hexaflow', 'Hexaflow'], ['hexastar', 'Hexastar']] as const).map(([id, label]) =>
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

export function HexaflowStatsPanel({ stats }: { stats: HexaflowStatsDto }) {
  return <div className="stat-grid"><Metric label="Iniziate" value={stats.started}/><Metric label="Completate" value={stats.completed}/><Metric label="Serie" value={stats.currentStreak}/><Metric label="Record" value={stats.maxStreak}/></div>;
}

export function StatsView({ stats, active, onChangeGame }: {
  stats: StatsSetDto;
  active: StatsGame;
  onChangeGame: (game: StatsGame) => void;
}) {
  const selected = active === "hexahack" || active === "hexasky" || active === "hexaflow" ? null : stats[active];
  const titles = { overall: "Il quadro completo", hexaword: "Le tue parole vincenti", hexahack: "La tua maestria nei codici", hexasky: "La tua prospettiva", hexaflow: "Il tuo Flusso", hexastar: "Le tue intuizioni" };
  return (
    <section className="stats-view" aria-labelledby="stats-page-title">
      <header className="account-page-heading"><span className="account-page-icon"><BarChart3 size={23} aria-hidden="true" /></span><div><p className="eyebrow">Un passo alla volta</p><h2 id="stats-page-title">Le tue statistiche</h2><p>Ogni sfida racconta un po’ dei tuoi progressi.</p></div></header>
      <StatsTabs active={active} onChange={onChangeGame} />
      <section className="account-panel stats-detail" aria-label={titles[active]}>
        <h3>{titles[active]}</h3>
        {active === "hexahack" ? <HexahackStatsPanel stats={stats.hexahack} /> : active === "hexasky" ? <HexaskyStatsPanel stats={stats.hexasky} /> : active === "hexaflow" ? <HexaflowStatsPanel stats={stats.hexaflow}/> : <StatsPanel stats={selected} />}
      </section>
      <p className="account-note">Le statistiche si aggiornano dopo ogni partita. Torna quando vuoi per scoprire come stai andando.</p>
    </section>
  );
}
