import React from "react";
import { ChevronLeft, Trophy } from "lucide-react";
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
  { id: "hexasky", label: "Hexasky" }
];

export function LeaderboardView({
  leaderboards,
  activeGame,
  onChangeGame,
  onBack,
  onOpenPlayer
}: {
  leaderboards: LeaderboardsDto;
  activeGame: LeaderboardGame;
  onChangeGame: (game: LeaderboardGame) => void;
  onBack: () => void;
  onOpenPlayer: (nickname: string) => void;
}) {
  const [activeTab, setActiveTab] = React.useState<LeaderboardTab>("weekly");
  const period = leaderboards[activeTab];

  return (
    <section className="leaderboard-view" aria-label="Leaderboard">
      <header className="leaderboard-head">
        <button className="icon-button profile-back" type="button" onClick={onBack} aria-label="Torna al gioco" title="Torna">
          <ChevronLeft size={23} />
        </button>
        <div>
          <h2><Trophy size={21} aria-hidden="true" /> Leaderboard</h2>
          <p>{periodLabel(period, activeTab)}</p>
        </div>
      </header>

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
                <span className="leaderboard-emoji" aria-hidden="true">{entry.profileEmoji}</span>
                <span className="leaderboard-player">
                  <strong>{entry.displayName}</strong>
                  <small>{entry.nickname}</small>
                </span>
                <span className="leaderboard-wins"><strong>{entry.wins}</strong> vittorie</span>
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
