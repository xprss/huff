import React from "react";
import { HEXATRAP_AVAILABLE } from "./constants";

export type AppRoute = "games" | "game" | "hexahack" | "hexasky" | "hexaflow" | "hexastar" | "hexatrap" | "hexaeco" | "profile" | "stats" | "admin" | "leaderboard" | "player";

function routeFromHash(hash: string): AppRoute {
  if (hash.startsWith("#/leaderboard/player/")) return "player";
  if (hash === "#/leaderboard") return "leaderboard";
  if (hash === "#/admin") return "admin";
  if (hash === "#/profile") return "profile";
  if (hash === "#/stats") return "stats";
  if (hash === "#/hexaword") return "game";
  if (hash === "#/hexahack") return "hexahack";
  if (hash === "#/hexasky") return "hexasky";
  if (hash === "#/hexaflow") return "hexaflow";
  if (hash === "#/hexastar") return "hexastar";
  if (hash === "#/hexatrap") return HEXATRAP_AVAILABLE ? "hexatrap" : "games";
  if (hash === "#/hexaeco") return "hexaeco";
  return "games";
}

function hashFromRoute(route: AppRoute) {
  if (route === "leaderboard" || route === "player") return "#/leaderboard";
  if (route === "admin") return "#/admin";
  if (route === "profile") return "#/profile";
  if (route === "stats") return "#/stats";
  if (route === "game") return "#/hexaword";
  if (route === "hexahack") return "#/hexahack";
  if (route === "hexasky") return "#/hexasky";
  if (route === "hexaflow") return "#/hexaflow";
  if (route === "hexastar") return "#/hexastar";
  if (route === "hexatrap") return HEXATRAP_AVAILABLE ? "#/hexatrap" : "#/";
  if (route === "hexaeco") return "#/hexaeco";
  return "#/";
}

export function playerNicknameFromHash(hash: string): string | null {
  const prefix = "#/leaderboard/player/";
  if (!hash.startsWith(prefix)) return null;
  try {
    const nickname = decodeURIComponent(hash.slice(prefix.length));
    return nickname || null;
  } catch {
    return null;
  }
}

export function playerHash(nickname: string) {
  return `#/leaderboard/player/${encodeURIComponent(nickname)}`;
}

export function useAppRoute() {
  const [route, setRouteState] = React.useState<AppRoute>(() => routeFromHash(window.location.hash));

  React.useEffect(() => {
    function syncRoute() {
      if (!HEXATRAP_AVAILABLE && window.location.hash === "#/hexatrap") {
        window.history.replaceState(null, "", "#/");
      }
      setRouteState(routeFromHash(window.location.hash));
    }

    syncRoute();
    window.addEventListener("hashchange", syncRoute);
    window.addEventListener("popstate", syncRoute);
    return () => {
      window.removeEventListener("hashchange", syncRoute);
      window.removeEventListener("popstate", syncRoute);
    };
  }, []);

  const setRoute = React.useCallback((nextRoute: AppRoute) => {
    const resolvedRoute = nextRoute === "hexatrap" && !HEXATRAP_AVAILABLE ? "games" : nextRoute;
    const nextHash = hashFromRoute(resolvedRoute);
    if (window.location.hash !== nextHash) {
      window.history.pushState(null, "", nextHash);
    }
    setRouteState(resolvedRoute);
  }, []);

  return [route, setRoute] as const;
}
