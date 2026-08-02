import React from "react";

export type AppRoute = "game" | "profile" | "admin";

function routeFromHash(hash: string): AppRoute {
  if (hash === "#/admin") return "admin";
  return hash === "#/profile" ? "profile" : "game";
}

function hashFromRoute(route: AppRoute) {
  if (route === "admin") return "#/admin";
  return route === "profile" ? "#/profile" : "#/";
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
