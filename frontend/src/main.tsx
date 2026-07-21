import React from "react";
import ReactDOM from "react-dom/client";
import { BarChart3, Delete, LogIn, LogOut, Moon, RotateCw, Sun } from "lucide-react";
import { api } from "./api";
import type { GameDto, GlobalStatsDto, MeDto, StatsDto, TileState } from "./types";
import "./styles.css";

const KEY_ROWS = ["QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM"];
const STATE_RANK: Record<TileState, number> = { ABSENT: 1, PRESENT: 2, CORRECT: 3 };

function App() {
  const [me, setMe] = React.useState<MeDto | null>(null);
  const [game, setGame] = React.useState<GameDto | null>(null);
  const [stats, setStats] = React.useState<StatsDto | null>(null);
  const [globalStats, setGlobalStats] = React.useState<GlobalStatsDto | null>(null);
  const [currentGuess, setCurrentGuess] = React.useState("");
  const [message, setMessage] = React.useState("");
  const [showStats, setShowStats] = React.useState(false);
  const [loading, setLoading] = React.useState(true);
  const [darkMode, setDarkMode] = React.useState(() => localStorage.getItem("darkMode") !== "false");

  const load = React.useCallback(async () => {
    setLoading(true);
    setMessage("");
    try {
      const meResponse = await api.me();
      setMe(meResponse);
      const globalStatsResponse = await api.globalStats();
      setGlobalStats(globalStatsResponse);
      if (meResponse.loggedIn) {
        const [gameResponse, statsResponse] = await Promise.all([
          api.today(),
          api.stats()
        ]);
        setGame(gameResponse);
        setStats(statsResponse);
      } else {
        setGame(null);
        setStats(null);
      }
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Errore imprevisto");
    } finally {
      setLoading(false);
    }
  }, []);

  React.useEffect(() => {
    void load();
  }, [load]);

  React.useEffect(() => {
    document.documentElement.dataset.theme = darkMode ? "dark" : "light";
    localStorage.setItem("darkMode", String(darkMode));
  }, [darkMode]);

  React.useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (showStats) return;
      if (event.key === "Enter") {
        void submitGuess();
      } else if (event.key === "Backspace") {
        setCurrentGuess((value) => value.slice(0, -1));
      } else if (/^[a-zA-Z]$/.test(event.key)) {
        addLetter(event.key);
      }
    }

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  });

  function addLetter(letter: string) {
    if (!game || game.status !== "IN_PROGRESS") return;
    setMessage("");
    setCurrentGuess((value) =>
      value.length >= game.wordLength ? value : value + letter.toUpperCase()
    );
  }

  async function submitGuess() {
    if (!game || game.status !== "IN_PROGRESS") return;
    if (currentGuess.length !== game.wordLength) {
      setMessage("Servono 6 lettere.");
      return;
    }

    try {
      const updated = await api.guess(currentGuess);
      setGame(updated);
      setCurrentGuess("");
      const [statsResponse, globalStatsResponse] = await Promise.all([api.stats(), api.globalStats()]);
      setStats(statsResponse);
      setGlobalStats(globalStatsResponse);
      if (updated.status === "WON" || updated.status === "LOST") {
        setShowStats(true);
      }
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Tentativo non valido");
    }
  }

  const keyStates = React.useMemo(() => {
    const states = new Map<string, TileState>();
    game?.guesses.forEach((guess) => {
      guess.tiles.forEach((tile) => {
        const letter = tile.letter.toUpperCase();
        const previous = states.get(letter);
        if (!previous || STATE_RANK[tile.state] > STATE_RANK[previous]) {
          states.set(letter, tile.state);
        }
      });
    });
    return states;
  }, [game]);

  const rows = buildRows(game, currentGuess);
  const canPlay = Boolean(game && game.status === "IN_PROGRESS");
  const puzzleDate = formatPuzzleDate(game?.puzzleDate);

  return (
    <main className="app-shell">
      <section className="game-surface" aria-busy={loading}>
        <header className="topbar">
          <div className="title-row">
            <h1>Wordolino</h1>
            <p className="date">{puzzleDate}</p>
          </div>
          <div className="actions">
            {me?.authEnabled && !me.loggedIn ? (
              <a className="icon-link" href={me.loginUrl ?? "/q/oidc/login"} title="Accedi con Google">
                <LogIn size={20} />
                <span>Google</span>
              </a>
            ) : null}
            {me?.authEnabled && me.loggedIn ? (
              <a className="icon-button" href={me.logoutUrl ?? "/api/logout"} title="Esci">
                <LogOut size={20} />
              </a>
            ) : null}
            <button className="icon-button" type="button" onClick={() => setShowStats(true)} title="Statistiche">
              <BarChart3 size={21} />
            </button>
            <button
              className="icon-button"
              type="button"
              onClick={() => setDarkMode((value) => !value)}
              title={darkMode ? "Tema chiaro" : "Tema scuro"}
            >
              {darkMode ? <Sun size={20} /> : <Moon size={20} />}
            </button>
            <button className="icon-button" type="button" onClick={() => void load()} title="Ricarica">
              <RotateCw size={20} />
            </button>
          </div>
        </header>

        {loading ? <p className="status-line">Caricamento...</p> : null}
        {!loading && me?.authEnabled && !me.loggedIn ? (
          <p className="status-line">Accedi per giocare.</p>
        ) : null}
        {message ? <p className="status-line error">{message}</p> : null}

        <div className="board" aria-label="Griglia tentativi">
          {rows.map((row, rowIndex) => (
            <div className="board-row" key={rowIndex}>
              {row.map((tile, tileIndex) => (
                <div
                  className={`tile ${tile.state ? tile.state.toLowerCase() : ""} ${tile.letter ? "filled" : ""}`}
                  key={tileIndex}
                >
                  {tile.letter}
                </div>
              ))}
            </div>
          ))}
        </div>

        <div className="keyboard" aria-label="Tastiera">
          {KEY_ROWS.map((row, index) => (
            <div className="key-row" key={row}>
              {index === 2 ? (
                <button className="key wide" type="button" disabled={!canPlay} onClick={() => void submitGuess()}>
                  Invio
                </button>
              ) : null}
              {row.split("").map((letter) => (
                <button
                  className={`key ${keyStates.get(letter)?.toLowerCase() ?? ""}`}
                  key={letter}
                  type="button"
                  disabled={!canPlay}
                  onClick={() => addLetter(letter)}
                >
                  {letter}
                </button>
              ))}
              {index === 2 ? (
                <button
                  className="key wide"
                  type="button"
                  disabled={!canPlay}
                  onClick={() => setCurrentGuess((value) => value.slice(0, -1))}
                  title="Cancella"
                >
                  <Delete size={19} />
                </button>
              ) : null}
            </div>
          ))}
        </div>
      </section>

      {showStats ? (
        <StatsModal game={game} stats={stats} globalStats={globalStats} onClose={() => setShowStats(false)} />
      ) : null}
    </main>
  );
}

function buildRows(game: GameDto | null, currentGuess: string) {
  const wordLength = game?.wordLength ?? 6;
  const maxAttempts = game?.maxAttempts ?? 6;
  const submitted = game?.guesses ?? [];
  const rows: Array<Array<{ letter: string; state?: TileState }>> = submitted.map((guess) =>
    guess.tiles.map((tile) => ({ letter: tile.letter.toUpperCase(), state: tile.state }))
  );

  if (rows.length < maxAttempts && game?.status !== "WON" && game?.status !== "LOST") {
    rows.push(
      Array.from({ length: wordLength }, (_, index) => ({
        letter: currentGuess[index] ?? ""
      }))
    );
  }

  while (rows.length < maxAttempts) {
    rows.push(Array.from({ length: wordLength }, () => ({ letter: "" })));
  }
  return rows;
}

function formatPuzzleDate(value: string | undefined) {
  if (!value) {
    return "Oggi";
  }
  const [year, month, day] = value.split("-").map(Number);
  if (!year || !month || !day) {
    return value;
  }
  return new Intl.DateTimeFormat("it-IT", {
    day: "numeric",
    month: "long",
    year: "numeric"
  }).format(new Date(year, month - 1, day));
}

function StatsModal({
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
            x
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

function Distribution({ distribution }: { distribution: Record<string, number> }) {
  const maxDistribution = Math.max(1, ...Object.values(distribution));

  return (
    <div className="distribution">
      {[1, 2, 3, 4, 5, 6].map((attempt) => {
        const value = Number(distribution[String(attempt)] ?? 0);
        return (
          <div className="distribution-row" key={attempt}>
            <span>{attempt}</span>
            <div>
              <b style={{ width: `${Math.max(8, (value / maxDistribution) * 100)}%` }}>{value}</b>
            </div>
          </div>
        );
      })}
    </div>
  );
}

function Metric({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="metric">
      <strong>{value}</strong>
      <span>{label}</span>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);

if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    void navigator.serviceWorker.register("/sw.js");
  });
}
