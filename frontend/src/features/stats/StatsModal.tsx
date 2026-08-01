import { X } from "lucide-react";
import { Distribution } from "../../shared/components/Distribution";
import { Metric } from "../../shared/components/Metric";
import type { GameDto, GlobalStatsDto, StatsDto } from "../../types";

export function StatsModal({
  game,
  stats,
  globalStats,
  onClose
}: {
  game: GameDto | null;
  stats: StatsDto | null;
  globalStats: GlobalStatsDto | null;
  onClose: () => void;
}) {
  const winRate = stats && stats.played > 0 ? Math.round((stats.won / stats.played) * 100) : 0;
  const globalWinRate =
    globalStats && globalStats.completed > 0 ? Math.round((globalStats.won / globalStats.completed) * 100) : 0;

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal" role="dialog" aria-modal="true" onMouseDown={(event) => event.stopPropagation()}>
        <header className="modal-head">
          <h2>Statistiche</h2>
          <button className="close-button" type="button" onClick={onClose} aria-label="Chiudi">
            <X size={19} />
          </button>
        </header>

        <h3>Personali</h3>
        <div className="stat-grid">
          <Metric label="Giocate" value={stats?.played ?? 0} />
          <Metric label="Vinte" value={stats?.won ?? 0} />
          <Metric label="Vittorie" value={`${winRate}%`} />
          <Metric label="Serie" value={stats?.currentStreak ?? 0} />
        </div>

        <Distribution distribution={stats?.guessDistribution ?? {}} />

        <h3>Globali</h3>
        <div className="stat-grid global">
          <Metric label="Giocatori" value={globalStats?.players ?? 0} />
          <Metric label="Iniziate" value={globalStats?.gamesStarted ?? 0} />
          <Metric label="Concluse" value={globalStats?.completed ?? 0} />
          <Metric label="Vittorie" value={`${globalWinRate}%`} />
        </div>

        <div className="stat-grid compact">
          <Metric label="Vinte" value={globalStats?.won ?? 0} />
          <Metric label="Perse" value={globalStats?.lost ?? 0} />
        </div>

        <Distribution distribution={globalStats?.guessDistribution ?? {}} />

        {game?.status === "WON" ? <p className="result won">Risolta.</p> : null}
        {game?.status === "LOST" ? <p className="result lost">Soluzione: {game.solution?.toUpperCase()}</p> : null}
      </section>
    </div>
  );
}
