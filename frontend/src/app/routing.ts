import React from "react";
import { isHexahackHidden } from "./features";

export type AppRoute = "games" | "game" | "hexahack" | "profile" | "admin" | "leaderboard" | "player";

function routeFromHash(hash: string): AppRoute {
  if (hash.startsWith("#/leaderboard/player/")) return "player";
  if (hash === "#/leaderboard") return "leaderboard";
  if (hash === "#/admin") return "admin";
  if (hash === "#/profile") return "profile";
  if (hash === "#/hexaword") return "game";
  if (hash === "#/hexahack" && !isHexahackHidden) return "hexahack";
  return "games";
}

function hashFromRoute(route: AppRoute) {
  if (route === "leaderboard" || route === "player") return "#/leaderboard";
  if (route === "admin") return "#/admin";
  if (route === "profile") return "#/profile";
  if (route === "game") return "#/hexaword";
  if (route === "hexahack" && !isHexahackHidden) return "#/hexahack";
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
  const [route, setRouteState] = React.useState<AppRoute>(() => {
    if (isHexahackHidden && window.location.hash === "#/hexahack") {
      window.history.replaceState(null, "", "#/");
    }
    return routeFromHash(window.location.hash);
  });

  React.useEffect(() => {
    function syncRoute() {
      if (isHexahackHidden && window.location.hash === "#/hexahack") {
        window.history.replaceState(null, "", "#/");
      }
      setRouteState(routeFromHash(window.location.hash));
    }

    window.addEventListener("hashchange", syncRoute);
    window.addEventListener("popstate", syncRoute);
    return () => {
      window.removeEventListener("hashchange", syncRoute);
      window.removeEventListener("popstate", syncRoute);
    };
  }, []);

  const setRoute = React.useCallback((nextRoute: AppRoute) => {
    const nextHash = hashFromRoute(nextRoute);
    if (window.location.hash !== nextHash) {
      window.history.pushState(null, "", nextHash);
    }
    setRouteState(nextRoute);
  }, []);

  return [route, setRoute] as const;
}
