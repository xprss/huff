import React from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Heart, X } from "lucide-react";
import { accessToken, api, clearAccessToken, isAuthRequiredError, storeAccessToken } from "../api";
import { AppThemeProvider } from "../theme";
import type {
  GameDto,
  GameMode,
  GuessResult,
  MeDto,
  ProfileUpdateDto,
  TodayGameDto,
  TileState
} from "../types";
import {
  APP_NAME,
  COUNTDOWN_INTERVAL_MS,
  NEXT_CHALLENGE_REFRESH_DELAY_MS,
  STAR_REVEAL_DURATION_MS,
  STATE_RANK,
  TOAST_DURATION_MS
} from "./constants";
import { AppHeader } from "./AppHeader";
import {
  globalStatsQueryOptions,
  leaderboardsQueryOptions,
  meQueryOptions,
  publicPlayerQueryOptions,
  queryKeys,
  statsQueryOptions,
  todayQueryOptions
} from "./queries";
import { playerHash, playerNicknameFromHash, useAppRoute } from "./routing";
import { GameBoard } from "../features/game/components/GameBoard";
import { GameKeyboard } from "../features/game/components/GameKeyboard";
import { ModeSelection } from "../features/game/components/ModeSelection";
import { StarRevealModal } from "../features/game/components/StarRevealModal";
import { buildColumns, buildShareText, hasAlreadyGuessed } from "../features/game/gameUtils";
import { launchVictoryConfetti } from "../features/game/confetti";
import { InfoModal } from "../features/info/InfoModal";
import {
  areGameNotificationsEnabled,
  disableGameNotifications,
  enableGameNotifications,
  getNotificationMenuLabel,
  getNotificationPermission,
  setGameNotificationsEnabled,
  syncPushSubscription
} from "../features/notifications/pushNotifications";
import { ProfileView } from "../features/profile/ProfileView";
import { PublicProfileView } from "../features/profile/PublicProfileView";
import { LeaderboardView } from "../features/leaderboard/LeaderboardView";
import { StatsModal } from "../features/stats/StatsModal";
import { AdminView } from "../features/admin/AdminView";
import { GoogleLoginScreen } from "../features/login/GoogleLoginScreen";
import { LoadingSpinner } from "../shared/components/LoadingSpinner";
import { useAppViewportHeight } from "../shared/hooks/useAppViewportHeight";
import { usePreventZoom } from "../shared/hooks/usePreventZoom";
import type { ToastMessage, ToastVariant } from "../shared/toast";
import {
  formatCountdownDuration,
  formatNextChallengeCountdown,
  formatPuzzleDate,
  getNextChallengeTime,
  getRemainingMilliseconds
} from "../shared/utils/date";

export function App() {
  const queryClient = useQueryClient();
  const [currentGuess, setCurrentGuess] = React.useState<string[]>([]);
  const [selectedCellIndex, setSelectedCellIndex] = React.useState<number | null>(0);
  const [showStats, setShowStats] = React.useState(false);
  const [showInfo, setShowInfo] = React.useState(false);
  const [activeRoute, setActiveRoute] = useAppRoute();
  const publicPlayerNickname = activeRoute === "player" ? playerNicknameFromHash(window.location.hash) : null;
  const [profileEditing, setProfileEditing] = React.useState(false);
  const [starReveal, setStarReveal] = React.useState<readonly GuessResult[] | null>(null);
  const [toast, setToast] = React.useState<ToastMessage | null>(null);
  const [showActionsMenu, setShowActionsMenu] = React.useState(false);
  const [refreshingChallenge, setRefreshingChallenge] = React.useState(false);
  const [dismissedFirstGuessSuggestionPuzzleDate, setDismissedFirstGuessSuggestionPuzzleDate] = React.useState<
    string | null
  >(null);
  const [darkMode, setDarkMode] = React.useState(() => localStorage.getItem("darkMode") !== "false");
  const [notificationsEnabled, setNotificationsEnabled] = React.useState(() => areGameNotificationsEnabled());
  const [notificationPermission, setNotificationPermission] = React.useState<NotificationPermission>(
    getNotificationPermission
  );
  const [activeAccessToken, setActiveAccessToken] = React.useState(accessToken);
  const [nextChallengeCountdown, setNextChallengeCountdown] = React.useState(formatNextChallengeCountdown);
  const actionsMenuRef = React.useRef<HTMLDivElement | null>(null);
  const themeConditions = React.useMemo(() => ({ preferredMode: darkMode ? "dark" : "light" } as const), [darkMode]);
  const meQuery = useQuery(meQueryOptions());
  const me = meQuery.data ?? null;
  const isLoggedIn = Boolean(me?.loggedIn);
  const globalStatsQuery = useQuery({
    ...globalStatsQueryOptions(),
    enabled: isLoggedIn
  });
  const todayQuery = useQuery({
    ...todayQueryOptions(),
    enabled: isLoggedIn
  });
  const statsQuery = useQuery({
    ...statsQueryOptions(),
    enabled: isLoggedIn
  });
  const leaderboardsQuery = useQuery({
    ...leaderboardsQueryOptions(),
    enabled: isLoggedIn && (activeRoute === "leaderboard" || activeRoute === "player")
  });
  const publicPlayerQuery = useQuery({
    ...publicPlayerQueryOptions(publicPlayerNickname ?? ""),
    enabled: isLoggedIn && activeRoute === "player" && publicPlayerNickname !== null
  });
  const today = todayQuery.data;
  const game = today?.game ?? null;
  const modes = today?.modes ?? [];
  const todayPuzzleDate = today?.puzzleDate ?? null;
  const stats = isLoggedIn ? statsQuery.data ?? null : null;
  const globalStats = globalStatsQuery.data ?? null;
  const canViewAdmin = Boolean(me?.user?.admin?.canViewPlayers);
  const canManagePlayers = Boolean(me?.user?.admin?.canManagePlayers);
  const loading =
    refreshingChallenge ||
    meQuery.isPending ||
    isAuthRequiredError(meQuery.error) ||
    globalStatsQuery.isPending ||
    (isLoggedIn && (todayQuery.isPending || statsQuery.isPending));

  function setTodayGame(gameUpdate: GameDto | null) {
    queryClient.setQueryData<TodayGameDto | undefined>(queryKeys.today, (current) =>
      current
        ? {
            ...current,
            game: gameUpdate
          }
        : current
    );
  }

  function refreshStats() {
    return Promise.all([queryClient.fetchQuery(statsQueryOptions()), queryClient.fetchQuery(globalStatsQueryOptions())]);
  }

  const selectModeMutation = useMutation({
    mutationFn: api.selectMode,
    onSuccess: (updated) => {
      setTodayGame(updated);
      setCurrentGuess([]);
      setSelectedCellIndex(0);
    }
  });

  const guessMutation = useMutation({
    mutationFn: api.guess
  });

  const useKittenMutation = useMutation({
    mutationFn: api.useKitten,
    onSuccess: (updated) => {
      setTodayGame(updated);
    }
  });

  const useStarMutation = useMutation({
    mutationFn: api.useStar,
    onSuccess: (reveal) => {
      setTodayGame(reveal.game);
      setStarReveal(reveal.guesses);
    }
  });

  const updateProfileMutation = useMutation({
    mutationFn: api.updateProfile,
    onSuccess: (updatedUser) => {
      queryClient.setQueryData<MeDto | undefined>(queryKeys.me, (current) =>
        current?.user ? { ...current, user: updatedUser } : current
      );
    }
  });

  useAppViewportHeight(me?.loggedIn);
  usePreventZoom();

  function clearToast() {
    setToast(null);
  }

  function showToast(text: string, variant: ToastVariant = "neutral") {
    setToast({ id: Date.now(), text, variant });
  }

  function handleAuthRequired(error: unknown) {
    if (!isAuthRequiredError(error)) return false;
    void queryClient.cancelQueries({ queryKey: queryKeys.today });
    void queryClient.cancelQueries({ queryKey: queryKeys.stats });
    void queryClient.cancelQueries({ queryKey: ["admin"] });
    void queryClient.cancelQueries({ queryKey: queryKeys.leaderboards });
    void queryClient.cancelQueries({ queryKey: ["player"] });
    queryClient.removeQueries({ queryKey: queryKeys.today });
    queryClient.removeQueries({ queryKey: queryKeys.stats });
    queryClient.removeQueries({ queryKey: ["admin"] });
    queryClient.removeQueries({ queryKey: queryKeys.leaderboards });
    queryClient.removeQueries({ queryKey: ["player"] });
    setCurrentGuess([]);
    setSelectedCellIndex(0);
    setShowStats(false);
    setShowInfo(false);
    setProfileEditing(false);
    setShowActionsMenu(false);
    setActiveRoute("game");
    setStarReveal(null);
    clearAccessToken();
    setActiveAccessToken(null);
    return true;
  }

  React.useEffect(() => {
    const error =
      meQuery.error ??
      globalStatsQuery.error ??
      todayQuery.error ??
      statsQuery.error ??
      leaderboardsQuery.error ??
      publicPlayerQuery.error;
    if (!error) return;
    if (handleAuthRequired(error)) return;

    showToast(error instanceof Error ? error.message : "Errore imprevisto", "error");
  }, [meQuery.error, globalStatsQuery.error, todayQuery.error, statsQuery.error, leaderboardsQuery.error, publicPlayerQuery.error]);

  React.useEffect(() => {
    localStorage.setItem("darkMode", String(darkMode));
  }, [darkMode]);

  React.useEffect(() => {
    function refreshNotificationState() {
      setNotificationPermission(getNotificationPermission());
      setNotificationsEnabled(areGameNotificationsEnabled());
    }

    window.addEventListener("focus", refreshNotificationState);
    document.addEventListener("visibilitychange", refreshNotificationState);
    return () => {
      window.removeEventListener("focus", refreshNotificationState);
      document.removeEventListener("visibilitychange", refreshNotificationState);
    };
  }, []);

  React.useEffect(() => {
    if (!toast) return;

    const timer = window.setTimeout(() => setToast(null), TOAST_DURATION_MS);
    return () => window.clearTimeout(timer);
  }, [toast]);

  React.useEffect(() => {
    if (!starReveal) return;

    const timer = window.setTimeout(() => setStarReveal(null), STAR_REVEAL_DURATION_MS);
    return () => window.clearTimeout(timer);
  }, [starReveal]);

  React.useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (activeRoute !== "game" || showStats || showInfo || starReveal) return;
      if (event.key === "Enter") {
        void submitGuess();
      } else if (event.key === "Backspace") {
        removeLetter();
      } else if (/^[a-zA-Z]$/.test(event.key)) {
        addLetter(event.key);
      }
    }

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  });

  React.useEffect(() => {
    if (activeRoute !== "profile") {
      setProfileEditing(false);
    }
  }, [activeRoute]);

  React.useEffect(() => {
    if (activeRoute === "admin" && me && !canViewAdmin) {
      setActiveRoute("game");
    }
  }, [activeRoute, canViewAdmin, me, setActiveRoute]);

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

  const keyStates = React.useMemo(() => {
    const states = new Map<string, Exclude<TileState, "HIDDEN">>();
    game?.guesses.forEach((guess) => {
      guess.tiles.forEach((tile) => {
        if (tile.state === "HIDDEN") return;
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
  const shouldHideKeyboardHints = Boolean(game?.mode === "MISCHIEVOUS_MOUSE" && !game.kitten.used);
  const answerLength = game?.answerLength ?? 6;
  const puzzleDate = formatPuzzleDate(game?.puzzleDate ?? todayPuzzleDate ?? undefined);
  const canUseGameActions = Boolean(me && (!me.authEnabled || me.loggedIn));
  const notificationMenuLabel = getNotificationMenuLabel(notificationsEnabled, notificationPermission);
  const showStarButton = Boolean(game && game.status === "IN_PROGRESS" && game.guesses.length > 0);
  const lastGuess = game?.guesses[game.guesses.length - 1];
  const completedSolution =
    game?.status === "WON" || game?.status === "LOST" ? game.solution ?? lastGuess?.word ?? null : null;
  const terminalCells = completedSolution ? completedSolution.split("") : currentGuess;
  const terminalResult = completedSolution ? (game?.status === "WON" ? "won" : "lost") : null;
  const firstGuessSuggestion = game?.firstGuessSuggestion ?? null;
  const showFirstGuessSuggestion = Boolean(
    canPlay &&
      game &&
      game.guesses.length === 0 &&
      firstGuessSuggestion &&
      dismissedFirstGuessSuggestionPuzzleDate !== game.puzzleDate
  );

  React.useEffect(() => {
    if (!canUseGameActions || !notificationsEnabled) return;

    void syncPushSubscription().catch((error) => {
      if (handleAuthRequired(error)) return;
      setNotificationsEnabled(false);
      setGameNotificationsEnabled(false);
      showToast(error instanceof Error ? error.message : "Impossibile attivare le notifiche.", "error");
    });
  }, [canUseGameActions, notificationsEnabled]);

  React.useEffect(() => {
    if (!completedSolution) return;

    const nextChallengeTime = getNextChallengeTime();
    let refreshTimer: number | undefined;

    function updateCountdown() {
      const remainingMilliseconds = getRemainingMilliseconds(nextChallengeTime);
      setNextChallengeCountdown(formatCountdownDuration(remainingMilliseconds));

      if (remainingMilliseconds > 0 || refreshTimer !== undefined) return;

      setRefreshingChallenge(true);
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

  function addLetter(letter: string) {
    if (!game || game.status !== "IN_PROGRESS") return;
    if (!shouldHideKeyboardHints && keyStates.get(letter.toUpperCase()) === "ABSENT") return;
    clearToast();
    setCurrentGuess((value) => {
      const cells = normalizeGuessCells(value, game.answerLength);
      const index = selectedCellIndex ?? cells.findIndex((cell) => !cell);
      if (index < 0) return cells;

      cells[index] = letter.toUpperCase();
      setSelectedCellIndex(findNextEmptyCell(cells, index + 1));
      return cells;
    });
  }

  function removeLetter() {
    if (!game || game.status !== "IN_PROGRESS") return;
    clearToast();
    setCurrentGuess((value) => {
      const cells = normalizeGuessCells(value, game.answerLength);
      // A manually selected filled cell is the explicit deletion target.  When
      // the cursor is on an empty cell, retain the usual Backspace behavior.
      const index =
        selectedCellIndex !== null && cells[selectedCellIndex]
          ? selectedCellIndex
          : findPreviousFilledCell(cells, selectedCellIndex ?? game.answerLength);
      if (index === null) return cells;

      cells[index] = "";
      setSelectedCellIndex(index);
      return cells;
    });
  }

  function selectGuessCell(index: number) {
    if (!game || game.status !== "IN_PROGRESS") return;
    clearToast();
    setSelectedCellIndex(index);
  }

  async function submitGuess() {
    if (!game || game.status !== "IN_PROGRESS") return;
    if (currentGuess.length !== game.answerLength || currentGuess.some((cell) => !cell)) {
      showToast(`Completa tutte e ${game.answerLength} le caselle.`, "warning");
      return;
    }
    const guess = currentGuess.join("");
    if (game.mode === "MISCHIEVOUS_MOUSE" && hasAlreadyGuessed(game, guess)) {
      showToast("Hai già inserito questa parola.", "warning");
      return;
    }

    try {
      const updated = await guessMutation.mutateAsync(guess);
      setTodayGame(updated);
      setCurrentGuess([]);
      setSelectedCellIndex(0);
      if (updated.star.justAwarded) {
        showToast("Hai conquistato una stella.", "success");
      }
      const statsRequest = refreshStats();

      if (updated.status === "WON") {
        await Promise.all([statsRequest, launchVictoryConfetti()]);
        setShowStats(true);
        return;
      }

      await statsRequest;
      if (updated.status === "LOST") {
        setShowStats(true);
      }
    } catch (error) {
      if (handleAuthRequired(error)) return;
      showToast(error instanceof Error ? error.message : "Tentativo non valido", "warning");
    }
  }

  async function selectGameMode(mode: GameMode) {
    try {
      clearToast();
      await selectModeMutation.mutateAsync(mode);
    } catch (error) {
      if (handleAuthRequired(error)) return;
      showToast(error instanceof Error ? error.message : "Impossibile selezionare la modalità", "error");
    }
  }

  async function useKitten() {
    if (!game?.kitten.canUse) return;

    try {
      clearToast();
      await useKittenMutation.mutateAsync();
    } catch (error) {
      if (handleAuthRequired(error)) return;
      showToast(error instanceof Error ? error.message : "Impossibile usare il gattino", "error");
    }
  }

  async function useStar() {
    setShowActionsMenu(false);
    if (!game || game.status !== "IN_PROGRESS" || game.guesses.length === 0) return;
    if (!game.star.canUse) {
      showToast("Completa 3 partite di fila per ottenere una stella.");
      return;
    }

    try {
      clearToast();
      await useStarMutation.mutateAsync();
    } catch (error) {
      if (handleAuthRequired(error)) return;
      showToast(error instanceof Error ? error.message : "Impossibile usare la stella.", "error");
    }
  }

  async function shareResult() {
    if (!game || (game.status !== "WON" && game.status !== "LOST")) return;

    if (!navigator.share) {
      showToast("Condivisione non disponibile su questo dispositivo.", "neutral");
      return;
    }

    try {
      clearToast();
      await navigator.share({
        title: APP_NAME,
        text: buildShareText(game),
        url: window.location.origin
      });
    } catch (error) {
      if (error instanceof DOMException && error.name === "AbortError") return;
      showToast("Impossibile aprire la condivisione.", "error");
    }
  }

  async function toggleGameNotifications() {
    setShowActionsMenu(false);

    if (notificationsEnabled) {
      await disableGameNotifications();
      setNotificationsEnabled(false);
      setNotificationPermission(getNotificationPermission());
      showToast("Notifiche disattivate.", "neutral");
      return;
    }

    try {
      await enableGameNotifications();
      setNotificationsEnabled(true);
      setNotificationPermission(getNotificationPermission());
      showToast("Notifiche attive.", "success");
    } catch (error) {
      if (handleAuthRequired(error)) return;
      setNotificationsEnabled(false);
      setGameNotificationsEnabled(false);
      setNotificationPermission(getNotificationPermission());
      showToast(error instanceof Error ? error.message : "Impossibile attivare le notifiche.", "error");
    }
  }

  async function updateProfile(profile: ProfileUpdateDto) {
    try {
      return await updateProfileMutation.mutateAsync(profile);
    } catch (error) {
      if (handleAuthRequired(error)) {
        throw new Error("Sessione scaduta. Accedi di nuovo.");
      }
      throw error;
    }
  }

  function autofillFirstGuessSuggestion() {
    if (!firstGuessSuggestion || !game || game.status !== "IN_PROGRESS" || game.guesses.length > 0) return;
    clearToast();
    setCurrentGuess(firstGuessSuggestion.toUpperCase().split(""));
    setSelectedCellIndex(null);
  }

  if (!activeAccessToken && (me?.authEnabled || isAuthRequiredError(meQuery.error))) {
    return (
      <AppThemeProvider conditions={themeConditions}>
        <main className="app-shell">
          <GoogleLoginScreen
            onAccessToken={(token) => {
              storeAccessToken(token);
              setActiveAccessToken(accessToken());
              window.location.reload();
            }}
          />
        </main>
      </AppThemeProvider>
    );
  }

  return (
    <AppThemeProvider conditions={themeConditions}>
      <main className="app-shell">
        <section className="game-surface" aria-busy={loading}>
          <AppHeader
            puzzleDate={puzzleDate}
            toast={toast}
            showStarButton={showStarButton}
            starCanUse={Boolean(game?.star.canUse)}
            showProfileEdit={Boolean(activeRoute === "profile" && me?.user && !profileEditing)}
            showActionsMenu={showActionsMenu}
            actionsMenuRef={actionsMenuRef}
            canUseGameActions={canUseGameActions}
            showAdmin={canViewAdmin}
            notificationsEnabled={notificationsEnabled}
            notificationMenuLabel={notificationMenuLabel}
            darkMode={darkMode}
            showLogout={Boolean(me?.authEnabled && me.loggedIn)}
            onLogout={() => {
              clearAccessToken();
              setActiveAccessToken(null);
              queryClient.clear();
            }}
            onUseStar={() => void useStar()}
            onEditProfile={() => setProfileEditing(true)}
            onToggleMenu={() => setShowActionsMenu((value) => !value)}
            onOpenProfile={() => {
              setActiveRoute("profile");
              setProfileEditing(false);
              setShowActionsMenu(false);
            }}
            onOpenStats={() => {
              setShowStats(true);
              setShowActionsMenu(false);
            }}
            onOpenLeaderboard={() => {
              setActiveRoute("leaderboard");
              setShowActionsMenu(false);
            }}
            onOpenAdmin={() => {
              setActiveRoute("admin");
              setShowActionsMenu(false);
            }}
            onOpenInfo={() => {
              setShowInfo(true);
              setShowActionsMenu(false);
            }}
            onToggleNotifications={() => void toggleGameNotifications()}
            onToggleTheme={() => {
              setDarkMode((value) => !value);
              setShowActionsMenu(false);
            }}
            onCloseMenu={() => setShowActionsMenu(false)}
          />

          {loading ? (
            <div className="play-area">
              <LoadingSpinner />
            </div>
          ) : activeRoute === "leaderboard" ? (
            leaderboardsQuery.isPending ? (
              <div className="play-area"><LoadingSpinner /></div>
            ) : leaderboardsQuery.data ? (
              <LeaderboardView
                leaderboards={leaderboardsQuery.data}
                onBack={() => setActiveRoute("game")}
                onOpenPlayer={(nickname) => {
                  window.location.hash = playerHash(nickname);
                }}
              />
            ) : (
              <div className="play-area">Impossibile caricare la leaderboard.</div>
            )
          ) : activeRoute === "player" ? (
            publicPlayerQuery.isPending ? (
              <div className="play-area"><LoadingSpinner /></div>
            ) : publicPlayerQuery.data ? (
              <PublicProfileView profile={publicPlayerQuery.data} onBack={() => setActiveRoute("leaderboard")} />
            ) : (
              <div className="play-area">Giocatore non trovato.</div>
            )
          ) : activeRoute === "profile" && me?.user ? (
            <ProfileView
              user={me.user}
              stats={stats}
              editing={profileEditing}
              onEditingChange={setProfileEditing}
              onBack={() => {
                setProfileEditing(false);
                setActiveRoute("game");
              }}
              onSave={(profile) => updateProfile(profile)}
              onSuccess={(message) => showToast(message, "success")}
              onError={(message) => showToast(message, "error")}
            />
          ) : activeRoute === "admin" && canViewAdmin ? (
            <AdminView
              canManagePlayers={canManagePlayers}
              onAuthRequired={handleAuthRequired}
              onSuccess={(message) => showToast(message, "success")}
              onError={(message) => showToast(message, "error")}
            />
          ) : !game ? (
            <div className="play-area">
              <ModeSelection modes={modes} selectedMode={null} onSelect={(mode) => void selectGameMode(mode)} />
            </div>
          ) : (
            <>
              <div className="play-area">
                <GameBoard
                  game={game}
                  modes={modes}
                  columns={columns}
                  terminalCells={terminalCells}
                  answerLength={answerLength}
                  canPlay={canPlay}
                  terminalResult={terminalResult}
                  selectedCellIndex={selectedCellIndex}
                  completedSolution={completedSolution}
                  nextChallengeCountdown={nextChallengeCountdown}
                  onSelectMode={(mode) => void selectGameMode(mode)}
                  onUseKitten={() => void useKitten()}
                  onShareResult={() => void shareResult()}
                  onSelectCell={selectGuessCell}
                />
              </div>

              <div className="keyboard-zone">
                {showFirstGuessSuggestion && game && firstGuessSuggestion ? (
                  <div className="first-guess-suggestion">
                    <span>Di solito inizi con</span>
                    <button
                      className="first-guess-chip"
                      type="button"
                      onClick={autofillFirstGuessSuggestion}
                      aria-label={`Compila ${firstGuessSuggestion.toUpperCase()}`}
                    >
                      {firstGuessSuggestion.toUpperCase()}
                    </button>
                    <button
                      className="first-guess-dismiss"
                      type="button"
                      onClick={() => setDismissedFirstGuessSuggestionPuzzleDate(game.puzzleDate)}
                      aria-label="Chiudi suggerimento"
                      title="Chiudi"
                    >
                      <X size={15} />
                    </button>
                  </div>
                ) : null}

                <GameKeyboard
                  canPlay={canPlay}
                  keyStates={keyStates}
                  shouldHideKeyboardHints={shouldHideKeyboardHints}
                  onAddLetter={addLetter}
                  onSubmit={() => void submitGuess()}
                  onBackspace={removeLetter}
                />
              </div>
            </>
          )}
        </section>

        {showStats ? (
          <StatsModal game={game} stats={stats} globalStats={globalStats} onClose={() => setShowStats(false)} />
        ) : null}

        {showInfo ? <InfoModal onClose={() => setShowInfo(false)} /> : null}

        {starReveal ? (
          <StarRevealModal
            durationMs={STAR_REVEAL_DURATION_MS}
            guesses={starReveal}
            onClose={() => setStarReveal(null)}
          />
        ) : null}

        <footer className="app-footer">
          <span>Sviluppato con</span>
          <Heart className="footer-heart" aria-hidden="true" />
          <span>da xprss</span>
        </footer>
      </main>
    </AppThemeProvider>
  );
}

function normalizeGuessCells(cells: readonly string[], answerLength: number) {
  return Array.from({ length: answerLength }, (_, index) => cells[index] ?? "");
}

function findNextEmptyCell(cells: readonly string[], startIndex: number) {
  for (let index = startIndex; index < cells.length; index += 1) {
    if (!cells[index]) return index;
  }
  for (let index = 0; index < startIndex; index += 1) {
    if (!cells[index]) return index;
  }
  return null;
}

function findPreviousFilledCell(cells: readonly string[], startIndex: number) {
  for (let index = Math.min(startIndex - 1, cells.length - 1); index >= 0; index -= 1) {
    if (cells[index]) return index;
  }

  // The cursor can wrap to an earlier empty cell after a user fills a later one.
  // Continue from the end so Backspace still removes the character just entered.
  for (let index = cells.length - 1; index >= startIndex; index -= 1) {
    if (cells[index]) return index;
  }

  return null;
}
