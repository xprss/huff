import React from "react";

export type AppRoute = "games" | "game" | "hexadigit" | "profile" | "admin" | "leaderboard" | "player";

function routeFromHash(hash: string): AppRoute {
  if (hash.startsWith("#/leaderboard/player/")) return "player";
  if (hash === "#/leaderboard") return "leaderboard";
  if (hash === "#/admin") return "admin";
  if (hash === "#/profile") return "profile";
  if (hash === "#/hexaword") return "game";
  if (hash === "#/hexadigit") return "hexadigit";
  return "games";
}

function hashFromRoute(route: AppRoute) {
  if (route === "leaderboard" || route === "player") return "#/leaderboard";
  if (route === "admin") return "#/admin";
  if (route === "profile") return "#/profile";
  if (route === "game") return "#/hexaword";
  if (route === "hexadigit") return "#/hexadigit";
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
