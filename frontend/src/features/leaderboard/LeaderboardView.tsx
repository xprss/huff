import React from "react";
import { Trophy } from "lucide-react";
import { Avatar } from "../../shared/components/Avatar";
import type { LeaderboardGame, LeaderboardPeriodDto, LeaderboardsDto } from "../../types";

type LeaderboardTab = "allTime" | "yearly" | "monthly" | "weekly";

const TABS: ReadonlyArray<{ id: LeaderboardTab; label: string }> = [
  { id: "allTime", label: "Sempre" },
  { id: "yearly", label: "Anno" },
  { id: "monthly", label: "Mese" },
  { id: "weekly", label: "Settimana" }
];

const GAME_TABS: ReadonlyArray<{ id: LeaderboardGame; label: string }> = [
  { id: "overall", label: "Tutti" },
  { id: "hexaword", label: "Hexaword" },
  { id: "hexahack", label: "Hexahack" },
  { id: "hexasky", label: "Hexasky" },
  { id: "hexaflow", label: "Hexaflow" },
  { id: "hexastar", label: "Hexastar" },
  { id: "hexaeco", label: "Hexaeco" },
  { id: "hexatrap", label: "Hexatrap" }
];

export function LeaderboardView({
  leaderboards,
  activeGame,
  onChangeGame,
  onOpenPlayer
}: {
  leaderboards: LeaderboardsDto;
  activeGame: LeaderboardGame;
  onChangeGame: (game: LeaderboardGame) => void;
  onOpenPlayer: (nickname: string) => void;
}) {
  const [activeTab, setActiveTab] = React.useState<LeaderboardTab>("weekly");
  const period = leaderboards[activeTab];

  return (
    <section className="leaderboard-view" aria-label="Classifica">
      <div className="leaderboard-games" role="tablist" aria-label="Gioco della classifica">
        {GAME_TABS.map((game) => (
          <button
            key={game.id}
            type="button"
            role="tab"
            aria-selected={activeGame === game.id}
            className={activeGame === game.id ? "selected" : ""}
            onClick={() => onChangeGame(game.id)}
          >
            {game.label}
          </button>
        ))}
      </div>

      <div className="leaderboard-tabs" role="tablist" aria-label="Periodo classifica">
        {TABS.map((tab) => (
          <button
            key={tab.id}
            type="button"
            role="tab"
            aria-selected={activeTab === tab.id}
            className={activeTab === tab.id ? "selected" : ""}
            onClick={() => setActiveTab(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </div>
      <p className="leaderboard-period">{periodLabel(period, activeTab)}</p>

      {period.entries.length === 0 ? (
        <div className="leaderboard-empty">
          <Trophy size={28} aria-hidden="true" />
          <p>Nessuna vittoria in questo periodo.</p>
        </div>
      ) : (
        <ol className="leaderboard-list" aria-label="Giocatori in classifica">
          {period.entries.map((entry) => (
            <li key={entry.nickname}>
              <button type="button" onClick={() => onOpenPlayer(entry.nickname)} aria-label={`Apri il profilo di ${entry.nickname}`}>
                <span className={`leaderboard-rank rank-${entry.rank}`}>{entry.rank}</span>
                <Avatar className="leaderboard-avatar" name={entry.displayName || entry.nickname} avatarUrl={entry.avatarUrl} size={36} />
                <span className="leaderboard-player">
                  <strong>{entry.displayName}</strong>
                  <small>{entry.nickname}</small>
                </span>
                <span className="leaderboard-wins"><strong>{entry.wins}</strong> vittorie{entry.points != null ? <small> · {entry.points} punti</small> : null}</span>
              </button>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}

function periodLabel(period: LeaderboardPeriodDto, tab: LeaderboardTab) {
  if (tab === "allTime") return "Tutte le vittorie";
  if (!period.startDate || !period.endDate) return "";
  const start = new Intl.DateTimeFormat("it-IT", { day: "numeric", month: "short", year: "numeric" }).format(
    new Date(`${period.startDate}T12:00:00`)
  );
  if (tab === "yearly") return `Dal 1 gennaio ${period.startDate.slice(0, 4)}`;
  if (tab === "monthly") return start;
  const end = new Intl.DateTimeFormat("it-IT", { day: "numeric", month: "short" }).format(
    new Date(`${period.endDate}T12:00:00`)
  );
  return `${start} – ${end}`;
}
