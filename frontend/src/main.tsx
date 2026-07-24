import React from "react";
import ReactDOM from "react-dom/client";
import confetti from "canvas-confetti";
import { BarChart3, Delete, ExternalLink, Github, Heart, Info, LogOut, Menu, Moon, Share2, Sun, X } from "lucide-react";
import { api } from "./api";
import { AppThemeProvider, applyThemeToDocument, baseAppTheme, useAppTheme } from "./theme";
import type { GameDto, GlobalStatsDto, MeDto, StatsDto, TileState } from "./types";
import { APP_VERSION } from "./version";
import "./styles.css";

const APP_NAME = "HexaQuot";
const KEY_ROWS = ["QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM"];
const STATE_RANK: Record<TileState, number> = { ABSENT: 1, PRESENT: 2, CORRECT: 3 };
const SHARE_EMOJI: Record<TileState, string> = { CORRECT: "🟩", PRESENT: "🟨", ABSENT: "⬛" };
const VICTORY_CONFETTI_COLORS = ["#25d7a1", "#ffd166", "#ff6b7a", "#5ab9ff", "#ffffff"];
const COUNTDOWN_INTERVAL_MS = 1000;
const NEXT_CHALLENGE_REFRESH_DELAY_MS = 2000;
const REPOSITORY_URL = "https://github.com/xprss/huff";
type BoardColumn = {
  feedback: Array<TileState | undefined>;
};
const LOGIN_DECOR_STATES = ["correct", "present", "absent"] as const;
type LoginDecorState = (typeof LOGIN_DECOR_STATES)[number];
type LoginDecorTile = {
  state: LoginDecorState;
  style: React.CSSProperties;
};
const LOGIN_DECOR_TILES = buildLoginDecorTiles();
const ZOOM_KEYS = new Set(["+", "-", "=", "_", "0"]);

function App() {
  const [me, setMe] = React.useState<MeDto | null>(null);
  const [game, setGame] = React.useState<GameDto | null>(null);
  const [stats, setStats] = React.useState<StatsDto | null>(null);
  const [globalStats, setGlobalStats] = React.useState<GlobalStatsDto | null>(null);
  const [currentGuess, setCurrentGuess] = React.useState("");
  const [message, setMessage] = React.useState("");
  const [showStats, setShowStats] = React.useState(false);
  const [showInfo, setShowInfo] = React.useState(false);
  const [showActionsMenu, setShowActionsMenu] = React.useState(false);
  const [loading, setLoading] = React.useState(true);
  const [darkMode, setDarkMode] = React.useState(() => localStorage.getItem("darkMode") !== "false");
  const [nextChallengeCountdown, setNextChallengeCountdown] = React.useState(formatNextChallengeCountdown);
  const actionsMenuRef = React.useRef<HTMLDivElement | null>(null);
  const themeConditions = React.useMemo(() => ({ preferredMode: darkMode ? "dark" : "light" } as const), [darkMode]);

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
    localStorage.setItem("darkMode", String(darkMode));
  }, [darkMode]);

  React.useEffect(() => {
    let lastTouchEnd = 0;
    const nonPassive = { passive: false };

    function preventDefault(event: Event) {
      event.preventDefault();
    }

    function preventWheelZoom(event: WheelEvent) {
      if (event.ctrlKey || event.metaKey) {
        event.preventDefault();
      }
    }

    function preventKeyboardZoom(event: KeyboardEvent) {
      if ((event.ctrlKey || event.metaKey) && ZOOM_KEYS.has(event.key)) {
        event.preventDefault();
      }
    }

    function preventDoubleTapZoom(event: TouchEvent) {
      const now = Date.now();
      if (now - lastTouchEnd <= 300) {
        event.preventDefault();
      }
      lastTouchEnd = now;
    }

    window.addEventListener("wheel", preventWheelZoom, nonPassive);
    window.addEventListener("keydown", preventKeyboardZoom);
    document.addEventListener("gesturestart", preventDefault, nonPassive);
    document.addEventListener("gesturechange", preventDefault, nonPassive);
    document.addEventListener("touchend", preventDoubleTapZoom, nonPassive);

    return () => {
      window.removeEventListener("wheel", preventWheelZoom);
      window.removeEventListener("keydown", preventKeyboardZoom);
      document.removeEventListener("gesturestart", preventDefault);
      document.removeEventListener("gesturechange", preventDefault);
      document.removeEventListener("touchend", preventDoubleTapZoom);
    };
  }, []);

  React.useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (showStats || showInfo) return;
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

  React.useEffect(() => {
    if (!showActionsMenu) return;

    function onPointerDown(event: PointerEvent) {
      if (actionsMenuRef.current?.contains(event.target as Node)) return;
      setShowActionsMenu(false);
    }

    function onEscape(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setShowActionsMenu(false);
      }
    }

    window.addEventListener("pointerdown", onPointerDown);
    window.addEventListener("keydown", onEscape);
    return () => {
      window.removeEventListener("pointerdown", onPointerDown);
      window.removeEventListener("keydown", onEscape);
    };
  }, [showActionsMenu]);

  function addLetter(letter: string) {
    if (!game || game.status !== "IN_PROGRESS") return;
    if (keyStates.get(letter.toUpperCase()) === "ABSENT") return;
    setMessage("");
    setCurrentGuess((value) =>
      value.length >= game.answerLength ? value : value + letter.toUpperCase()
    );
  }

  function pressVirtualKey(event: React.PointerEvent<HTMLButtonElement>, action: () => void) {
    if (event.button !== 0) return;
    event.preventDefault();
    action();
  }

  function clickVirtualKey(event: React.MouseEvent<HTMLButtonElement>, action: () => void) {
    if (event.detail !== 0) return;
    action();
  }

  async function submitGuess() {
    if (!game || game.status !== "IN_PROGRESS") return;
    if (currentGuess.length !== game.answerLength) {
      setMessage("Servono 6 lettere.");
      return;
    }

    try {
      const updated = await api.guess(currentGuess);
      setGame(updated);
      setCurrentGuess("");
      const statsRequest = Promise.all([api.stats(), api.globalStats()]);

      if (updated.status === "WON") {
        const [[statsResponse, globalStatsResponse]] = await Promise.all([statsRequest, launchVictoryConfetti()]);
        setStats(statsResponse);
        setGlobalStats(globalStatsResponse);
        setShowStats(true);
        return;
      }

      const [statsResponse, globalStatsResponse] = await statsRequest;
      setStats(statsResponse);
      setGlobalStats(globalStatsResponse);
      if (updated.status === "LOST") {
        setShowStats(true);
      }
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Tentativo non valido");
    }
  }

  async function shareResult() {
    if (!game || (game.status !== "WON" && game.status !== "LOST")) return;

    if (!navigator.share) {
      setMessage("Condivisione non disponibile su questo dispositivo.");
      return;
    }

    try {
      setMessage("");
      await navigator.share({
        title: APP_NAME,
        text: buildShareText(game),
        url: window.location.origin
      });
    } catch (error) {
      if (error instanceof DOMException && error.name === "AbortError") return;
      setMessage("Impossibile aprire la condivisione.");
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

  const columns = buildColumns(game);
  const canPlay = Boolean(game && game.status === "IN_PROGRESS");
  const answerLength = game?.answerLength ?? 6;
  const puzzleDate = formatPuzzleDate(game?.puzzleDate);
  const showLoginScreen = Boolean(!loading && me?.authEnabled && !me.loggedIn);
  const canUseGameActions = Boolean(me && (!me.authEnabled || me.loggedIn));
  const statusText = message;
  const lastGuess = game?.guesses[game.guesses.length - 1];
  const completedSolution =
    game?.status === "WON" || game?.status === "LOST"
      ? game.solution ?? lastGuess?.word ?? null
      : null;
  const terminalValue = completedSolution ?? currentGuess;
  const terminalResult = completedSolution ? (game?.status === "WON" ? "won" : "lost") : null;

  React.useEffect(() => {
    if (!completedSolution) return;

    const nextChallengeTime = getNextChallengeTime();
    let refreshTimer: number | undefined;

    function updateCountdown() {
      const remainingMilliseconds = getRemainingMilliseconds(nextChallengeTime);
      setNextChallengeCountdown(formatCountdownDuration(remainingMilliseconds));

      if (remainingMilliseconds > 0 || refreshTimer !== undefined) return;

      setLoading(true);
      setShowStats(false);
      refreshTimer = window.setTimeout(() => {
        window.location.reload();
      }, NEXT_CHALLENGE_REFRESH_DELAY_MS);
    }

    updateCountdown();
    const timer = window.setInterval(updateCountdown, COUNTDOWN_INTERVAL_MS);

    return () => {
      window.clearInterval(timer);
      if (refreshTimer !== undefined) {
        window.clearTimeout(refreshTimer);
      }
    };
  }, [completedSolution]);

  return (
    <AppThemeProvider conditions={themeConditions}>
      <main className="app-shell">
        <section className="game-surface" aria-busy={loading}>
          <header className="topbar">
            <div className="title-row">
              <div className="title-mark">
                <h1>{APP_NAME}</h1>
                <p className="date">{puzzleDate}</p>
              </div>
              <div className="actions" ref={actionsMenuRef}>
                <button
                  className="icon-button menu-trigger"
                  type="button"
                  onClick={() => setShowActionsMenu((value) => !value)}
                  aria-haspopup="menu"
                  aria-expanded={showActionsMenu}
                  aria-label="Apri menu"
                  title="Menu"
                >
                  <Menu size={21} />
                </button>
                {showActionsMenu ? (
                  <div className="action-menu" role="menu" aria-label="Azioni">
                    {canUseGameActions ? (
                      <button
                        className="menu-item"
                        type="button"
                        role="menuitem"
                        onClick={() => {
                          setShowStats(true);
                          setShowActionsMenu(false);
                        }}
                      >
                        <BarChart3 size={18} />
                        <span>Statistiche</span>
                      </button>
                    ) : null}
                    <button
                      className="menu-item"
                      type="button"
                      role="menuitem"
                      onClick={() => {
                        setShowInfo(true);
                        setShowActionsMenu(false);
                      }}
                    >
                      <Info size={18} />
                      <span>Info</span>
                    </button>
                    <button
                      className="menu-item"
                      type="button"
                      role="menuitem"
                      onClick={() => {
                        setDarkMode((value) => !value);
                        setShowActionsMenu(false);
                      }}
                    >
                      {darkMode ? <Sun size={18} /> : <Moon size={18} />}
                      <span>{darkMode ? "Tema chiaro" : "Tema scuro"}</span>
                    </button>
                    {me?.authEnabled && me.loggedIn ? (
                      <>
                        <div className="menu-divider" role="separator" />
                        <a
                          className="menu-item"
                          href={me.logoutUrl ?? "/api/logout"}
                          role="menuitem"
                          onClick={() => setShowActionsMenu(false)}
                        >
                          <LogOut size={18} />
                          <span>Esci</span>
                        </a>
                      </>
                    ) : null}
                  </div>
                ) : null}
              </div>
            </div>
          </header>
  
          <p className={`status-line ${message ? "error" : ""}`}>{statusText}</p>
  
          {showLoginScreen ? (
            <LoginScreen loginUrl={me?.loginUrl ?? "/api/login"} />
          ) : loading ? (
            <div className="play-area">
              <LoadingSpinner />
            </div>
          ) : (
            <>
              <div className="play-area">
                <div className="game-board-wrap">
                  {completedSolution ? (
                    <button className="share-button" type="button" onClick={() => void shareResult()}>
                      <Share2 size={18} />
                      <span>Condividi risultato</span>
                    </button>
                  ) : null}
                  <div className="board" aria-label="Griglia tentativi">
                    <TerminalInput
                      value={terminalValue}
                      answerLength={answerLength}
                      canPlay={canPlay}
                      result={terminalResult}
                    />
                    <div className="feedback-board">
                      {columns.map((column, columnIndex) => (
                        <div className="board-column" key={columnIndex}>
                          <div className="feedback-stack" aria-hidden="true">
                            {column.feedback.map((state, attemptIndex) => (
                              <span
                                className={`feedback-marker ${
                                  state === "CORRECT" || state === "PRESENT" || state === "ABSENT"
                                    ? state.toLowerCase()
                                    : "empty"
                                }`}
                                key={attemptIndex}
                              />
                            ))}
                          </div>
                        </div>
                      ))}
                    </div>
                    {completedSolution ? (
                      <p className="next-challenge" aria-live="polite">
                        Prossima sfida tra <time>{nextChallengeCountdown}</time>
                      </p>
                    ) : null}
                  </div>
                </div>
              </div>
  
              <div className="keyboard" aria-label="Tastiera">
                {KEY_ROWS.map((row, index) => (
                  <div className="key-row" key={row}>
                    {index === 2 ? (
                      <button
                        className="key wide primary"
                        type="button"
                        disabled={!canPlay}
                        onPointerDown={(event) => pressVirtualKey(event, () => void submitGuess())}
                        onClick={(event) => clickVirtualKey(event, () => void submitGuess())}
                      >
                        Invio
                      </button>
                    ) : null}
                    {row.split("").map((letter) => {
                      const keyState = keyStates.get(letter);
                      const keyClass =
                        keyState === "CORRECT" || keyState === "PRESENT" || keyState === "ABSENT"
                          ? keyState.toLowerCase()
                          : "";
                      const isAbsent = keyState === "ABSENT";
  
                      return (
                        <button
                          className={`key ${keyClass}`}
                          key={letter}
                          type="button"
                          disabled={!canPlay || isAbsent}
                          onPointerDown={(event) => pressVirtualKey(event, () => addLetter(letter))}
                          onClick={(event) => clickVirtualKey(event, () => addLetter(letter))}
                        >
                          {letter}
                        </button>
                      );
                    })}
                    {index === 2 ? (
                      <button
                        className="key wide"
                        type="button"
                        disabled={!canPlay}
                        onPointerDown={(event) =>
                          pressVirtualKey(event, () => setCurrentGuess((value) => value.slice(0, -1)))
                        }
                        onClick={(event) =>
                          clickVirtualKey(event, () => setCurrentGuess((value) => value.slice(0, -1)))
                        }
                        title="Cancella"
                      >
                        <Delete size={19} />
                      </button>
                    ) : null}
                  </div>
                ))}
              </div>
            </>
          )}
        </section>
  
        {showStats ? (
          <StatsModal game={game} stats={stats} globalStats={globalStats} onClose={() => setShowStats(false)} />
        ) : null}

        {showInfo ? <InfoModal onClose={() => setShowInfo(false)} /> : null}
  
        <footer className="app-footer">
          <span>Sviluppato con</span>
          <Heart className="footer-heart" aria-hidden="true" />
          <span>da xprss</span>
        </footer>
      </main>
    </AppThemeProvider>
  );
}

function InfoModal({ onClose }: { onClose: () => void }) {
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal info-modal" role="dialog" aria-modal="true" onMouseDown={(event) => event.stopPropagation()}>
        <header className="modal-head">
          <h2>Info</h2>
          <button className="close-button" type="button" onClick={onClose} aria-label="Chiudi">
            <X size={19} />
          </button>
        </header>

        <p className="app-version">Versione {APP_VERSION}</p>

        <div className="repo-panel">
          <Github size={26} aria-hidden="true" />
          <div>
            <h3>Repository GitHub</h3>
            <a className="repo-link" href={REPOSITORY_URL} target="_blank" rel="noreferrer">
              <span>github.com/xprss/huff</span>
              <ExternalLink size={16} aria-hidden="true" />
            </a>
          </div>
        </div>
      </section>
    </div>
  );
}

function LoadingSpinner() {
  return (
    <div className="loading-spinner" role="status" aria-label="Caricamento">
      <span />
    </div>
  );
}

function TerminalInput({
  value,
  answerLength,
  canPlay,
  result
}: {
  value: string;
  answerLength: number;
  canPlay: boolean;
  result: "won" | "lost" | null;
}) {
  const displayValue = value.toUpperCase();
  const cursorIndex = Math.min(displayValue.length, answerLength);

  return (
    <div className={`terminal-input ${result ?? ""}`} aria-label={`Input utente: ${displayValue}`}>
      {Array.from({ length: answerLength }, (_, index) => (
        <span
          className={`terminal-cell ${displayValue[index] ? "filled" : ""} ${
            canPlay && index === cursorIndex ? "cursor" : ""
          }`}
          key={index}
        >
          <span>{displayValue[index] ?? ""}</span>
        </span>
      ))}
    </div>
  );
}

function LoginScreen({ loginUrl }: { loginUrl: string }) {
  return (
    <section className="login-screen" aria-labelledby="login-title">
      <div className="login-decor" aria-hidden="true">
        {LOGIN_DECOR_TILES.map((tile, index) => (
          <span className={`login-decor-tile ${tile.state}`} key={index} style={tile.style} />
        ))}
      </div>
      <div className="login-copy">
        <h2 id="login-title">{APP_NAME}</h2>
        <p>Accedi per giocare la partita del giorno.</p>
      </div>
      <a className="login-button" href={loginUrl}>
        <GoogleLogo />
        <span>Accedi con Google</span>
      </a>
    </section>
  );
}

function buildLoginDecorTiles(): LoginDecorTile[] {
  const columns = 6;
  const rows = 5;
  const xJitter = [-3.2, 1.8, -1.1, 3.4, -2.4, 2.6];
  const yJitter = [2.4, -3.1, 1.6, -1.8, 3.2];
  const widths = [54, 72, 44, 86, 62, 78, 50, 68];
  const drift = [
    [-16, -12],
    [14, -18],
    [-10, 16],
    [18, 10],
    [-20, 6],
    [12, 18]
  ];

  return Array.from({ length: columns * rows }, (_, index) => {
    const row = Math.floor(index / columns);
    const column = index % columns;
    const x = 8 + column * 16.8 + xJitter[(row + column) % xJitter.length];
    const y = 11 + row * 20 + yJitter[(column + row * 2) % yJitter.length];
    const [dx, dy] = drift[index % drift.length];
    const duration = 6800 + ((index * 467) % 3600);
    const delay = -1 * ((index * 719) % duration);
    const opacity = 0.11 + ((index * 7) % 7) * 0.012;
    const scale = 0.82 + ((index * 5) % 9) * 0.035;

    return {
      state: LOGIN_DECOR_STATES[index % LOGIN_DECOR_STATES.length],
      style: {
        "--tile-x": `${x.toFixed(1)}%`,
        "--tile-y": `${y.toFixed(1)}%`,
        "--tile-width": `${widths[index % widths.length]}px`,
        "--tile-delay": `${delay}ms`,
        "--tile-duration": `${duration}ms`,
        "--tile-start-x": `${(-dx * 0.65).toFixed(1)}px`,
        "--tile-start-y": `${(-dy * 0.65).toFixed(1)}px`,
        "--tile-dx": `${dx}px`,
        "--tile-dy": `${dy}px`,
        "--tile-opacity": opacity.toFixed(3),
        "--tile-scale": scale.toFixed(2),
        "--tile-scale-start": (scale * 0.56).toFixed(2),
        "--tile-scale-end": (scale * 0.72).toFixed(2)
      } as React.CSSProperties
    };
  });
}

function GoogleLogo() {
  const theme = useAppTheme();

  return (
    <svg className="google-logo" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path
        fill={theme.colors.googleBlue}
        d="M23.49 12.27c0-.79-.07-1.54-.2-2.27H12v4.29h6.47a5.53 5.53 0 0 1-2.4 3.63v2.96h3.89c2.27-2.09 3.53-5.17 3.53-8.61Z"
      />
      <path
        fill={theme.colors.googleGreen}
        d="M12 24c3.24 0 5.96-1.07 7.95-2.91l-3.89-2.96c-1.08.72-2.45 1.15-4.06 1.15-3.12 0-5.77-2.11-6.71-4.95H1.28v3.05A12 12 0 0 0 12 24Z"
      />
      <path
        fill={theme.colors.googleYellow}
        d="M5.29 14.33a7.21 7.21 0 0 1 0-4.66V6.62H1.28a12.01 12.01 0 0 0 0 10.76l4.01-3.05Z"
      />
      <path
        fill={theme.colors.googleRed}
        d="M12 4.72c1.76 0 3.34.6 4.59 1.79l3.44-3.44C17.95 1.13 15.23 0 12 0A12 12 0 0 0 1.28 6.62l4.01 3.05C6.23 6.83 8.88 4.72 12 4.72Z"
      />
    </svg>
  );
}

function buildColumns(game: GameDto | null): BoardColumn[] {
  const answerLength = game?.answerLength ?? 6;
  const maxAttempts = game?.maxAttempts ?? 6;
  const submitted = game?.guesses ?? [];

  return Array.from({ length: answerLength }, (_, columnIndex) => ({
    feedback: Array.from({ length: maxAttempts }, (_, attemptIndex) => submitted[attemptIndex]?.tiles[columnIndex]?.state)
  }));
}

function buildShareText(game: GameDto) {
  const result = game.status === "WON" ? String(game.guesses.length) : "X";
  const attempts = game.guesses
    .map((guess) => guess.tiles.map((tile) => SHARE_EMOJI[tile.state]).join(""))
    .join("\n");

  return `${APP_NAME} - ${formatPuzzleDate(game.puzzleDate)}
Risultato: ${result}/${game.maxAttempts}

${attempts}`;
}

async function launchVictoryConfetti() {
  if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;

  const burst = confetti({
    colors: VICTORY_CONFETTI_COLORS,
    disableForReducedMotion: true,
    gravity: 0.82,
    origin: { x: 0.5, y: 0.45 },
    particleCount: 100,
    scalar: 1.1,
    spread: 95,
    startVelocity: 48,
    ticks: 240
  });

  if (burst) await burst;
}

function formatPuzzleDate(value: string | undefined) {
  if (!value) {
    return formatItalianDate(new Date());
  }
  const [year, month, day] = value.split("-").map(Number);
  if (!year || !month || !day) {
    return value;
  }
  return formatItalianDate(new Date(year, month - 1, day));
}

function formatItalianDate(date: Date) {
  return new Intl.DateTimeFormat("it-IT", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric"
  }).format(date);
}

function getNextChallengeTime() {
  const now = new Date();
  const nextMidnight = new Date(now);
  nextMidnight.setHours(24, 0, 0, 0);
  return nextMidnight.getTime();
}

function getRemainingMilliseconds(targetTime: number) {
  return Math.max(0, targetTime - Date.now());
}

function formatNextChallengeCountdown() {
  return formatCountdownDuration(getRemainingMilliseconds(getNextChallengeTime()));
}

function formatCountdownDuration(remainingMilliseconds: number) {
  const totalSeconds = Math.max(0, Math.ceil(remainingMilliseconds / 1000));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  return `${formatTimeUnit(hours, "ora", "ore")}, ${formatTimeUnit(minutes, "minuto", "minuti")} e ${formatTimeUnit(
    seconds,
    "secondo",
    "secondi"
  )}`;
}

function formatTimeUnit(value: number, singular: string, plural: string) {
  return `${value} ${value === 1 ? singular : plural}`;
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

applyThemeToDocument(baseAppTheme);

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);

if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    void navigator.serviceWorker.register("/sw.js").then((registration) => registration.update());
  });
}
