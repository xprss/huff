import React from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Heart } from "lucide-react";
import { api, isAuthRequiredError } from "../api";
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
import { globalStatsQueryOptions, meQueryOptions, queryKeys, statsQueryOptions, todayQueryOptions } from "./queries";
import { useAppRoute } from "./routing";
import { GameBoard } from "../features/game/components/GameBoard";
import { GameKeyboard } from "../features/game/components/GameKeyboard";
import { ModeSelection } from "../features/game/components/ModeSelection";
import { StarRevealModal } from "../features/game/components/StarRevealModal";
import { buildColumns, buildShareText, hasAlreadyGuessed } from "../features/game/gameUtils";
import { launchVictoryConfetti } from "../features/game/confetti";
import { InfoModal } from "../features/info/InfoModal";
import { LoginScreen } from "../features/login/LoginScreen";
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
import { StatsModal } from "../features/stats/StatsModal";
import { AdminView } from "../features/admin/AdminView";
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
  const [currentGuess, setCurrentGuess] = React.useState("");
  const [showStats, setShowStats] = React.useState(false);
  const [showInfo, setShowInfo] = React.useState(false);
  const [activeRoute, setActiveRoute] = useAppRoute();
  const [profileEditing, setProfileEditing] = React.useState(false);
  const [starReveal, setStarReveal] = React.useState<readonly GuessResult[] | null>(null);
  const [toast, setToast] = React.useState<ToastMessage | null>(null);
  const [showActionsMenu, setShowActionsMenu] = React.useState(false);
  const [refreshingChallenge, setRefreshingChallenge] = React.useState(false);
  const [darkMode, setDarkMode] = React.useState(() => localStorage.getItem("darkMode") !== "false");
  const [notificationsEnabled, setNotificationsEnabled] = React.useState(() => areGameNotificationsEnabled());
  const [notificationPermission, setNotificationPermission] = React.useState<NotificationPermission>(
    getNotificationPermission
  );
  const [nextChallengeCountdown, setNextChallengeCountdown] = React.useState(formatNextChallengeCountdown);
  const actionsMenuRef = React.useRef<HTMLDivElement | null>(null);
  const themeConditions = React.useMemo(() => ({ preferredMode: darkMode ? "dark" : "light" } as const), [darkMode]);
  const meQuery = useQuery(meQueryOptions());
  const me = meQuery.data ?? null;
  const isLoggedIn = Boolean(me?.loggedIn);
  const globalStatsQuery = useQuery(globalStatsQueryOptions());
  const todayQuery = useQuery({
    ...todayQueryOptions(),
    enabled: isLoggedIn
  });
  const statsQuery = useQuery({
    ...statsQueryOptions(),
    enabled: isLoggedIn
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
      setCurrentGuess("");
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

    queryClient.setQueryData<MeDto>(queryKeys.me, {
      loggedIn: false,
      user: null,
      loginUrl: error.loginUrl ?? "/api/login",
      logoutUrl: null,
      authEnabled: true
    });
    void queryClient.cancelQueries({ queryKey: queryKeys.today });
    void queryClient.cancelQueries({ queryKey: queryKeys.stats });
    void queryClient.cancelQueries({ queryKey: ["admin"] });
    queryClient.removeQueries({ queryKey: queryKeys.today });
    queryClient.removeQueries({ queryKey: queryKeys.stats });
    queryClient.removeQueries({ queryKey: ["admin"] });
    setCurrentGuess("");
    setShowStats(false);
    setShowInfo(false);
    setProfileEditing(false);
    setShowActionsMenu(false);
    setActiveRoute("game");
    setStarReveal(null);
    showToast("Sessione scaduta. Accedi di nuovo.", "warning");
    return true;
  }

  React.useEffect(() => {
    const error = meQuery.error ?? globalStatsQuery.error ?? todayQuery.error ?? statsQuery.error;
    if (!error) return;
    if (handleAuthRequired(error)) return;

    showToast(error instanceof Error ? error.message : "Errore imprevisto", "error");
  }, [meQuery.error, globalStatsQuery.error, todayQuery.error, statsQuery.error]);

  React.useEffect(() => {
    if (!me?.authEnabled || me.loggedIn) return;

    queryClient.removeQueries({ queryKey: queryKeys.today });
    queryClient.removeQueries({ queryKey: queryKeys.stats });
    queryClient.removeQueries({ queryKey: ["admin"] });
    setCurrentGuess("");
    setShowStats(false);
    setShowInfo(false);
    setProfileEditing(false);
    setShowActionsMenu(false);
    setStarReveal(null);
    setActiveRoute("game");
  }, [me?.authEnabled, me?.loggedIn, queryClient, setActiveRoute]);

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
        setCurrentGuess((value) => value.slice(0, -1));
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
  const showLoginScreen = Boolean(!loading && me?.authEnabled && !me.loggedIn);
  const canUseGameActions = Boolean(me && (!me.authEnabled || me.loggedIn));
  const notificationMenuLabel = getNotificationMenuLabel(notificationsEnabled, notificationPermission);
  const showStarButton = Boolean(game && game.status === "IN_PROGRESS" && game.guesses.length > 0);
  const lastGuess = game?.guesses[game.guesses.length - 1];
  const completedSolution =
    game?.status === "WON" || game?.status === "LOST" ? game.solution ?? lastGuess?.word ?? null : null;
  const terminalValue = completedSolution ?? currentGuess;
  const terminalResult = completedSolution ? (game?.status === "WON" ? "won" : "lost") : null;

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
    setCurrentGuess((value) => (value.length >= game.answerLength ? value : value + letter.toUpperCase()));
  }

  async function submitGuess() {
    if (!game || game.status !== "IN_PROGRESS") return;
    if (currentGuess.length !== game.answerLength) {
      showToast("Servono 6 lettere.", "warning");
      return;
    }
    if (game.mode === "MISCHIEVOUS_MOUSE" && hasAlreadyGuessed(game, currentGuess)) {
      showToast("Hai già inserito questa parola.", "warning");
      return;
    }

    try {
      const updated = await guessMutation.mutateAsync(currentGuess);
      setTodayGame(updated);
      setCurrentGuess("");
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
            logoutUrl={me?.logoutUrl ?? "/api/logout"}
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

          {showLoginScreen ? (
            <LoginScreen loginUrl={me?.loginUrl ?? "/api/login"} />
          ) : loading ? (
            <div className="play-area">
              <LoadingSpinner />
            </div>
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
                  terminalValue={terminalValue}
                  answerLength={answerLength}
                  canPlay={canPlay}
                  terminalResult={terminalResult}
                  completedSolution={completedSolution}
                  nextChallengeCountdown={nextChallengeCountdown}
                  onSelectMode={(mode) => void selectGameMode(mode)}
                  onUseKitten={() => void useKitten()}
                  onShareResult={() => void shareResult()}
                />
              </div>

              <GameKeyboard
                canPlay={canPlay}
                keyStates={keyStates}
                shouldHideKeyboardHints={shouldHideKeyboardHints}
                onAddLetter={addLetter}
                onSubmit={() => void submitGuess()}
                onBackspace={() => setCurrentGuess((value) => value.slice(0, -1))}
              />
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
