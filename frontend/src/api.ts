import type { GameDto, GameMode, GlobalStatsDto, MeDto, StatsDto, TodayGameDto } from "./types";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {})
    },
    ...init
  });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.message ?? "Richiesta non riuscita");
  }
  return response.json() as Promise<T>;
}

export const api = {
  me: () => request<MeDto>("/api/me"),
  today: () => request<TodayGameDto>("/api/game/today"),
  selectMode: (mode: GameMode) =>
    request<GameDto>("/api/game/today/mode", {
      method: "POST",
      body: JSON.stringify({ mode })
    }),
  guess: (guess: string) =>
    request<GameDto>("/api/game/today/guesses", {
      method: "POST",
      body: JSON.stringify({ guess })
    }),
  useKitten: () =>
    request<GameDto>("/api/game/today/kitten", {
      method: "POST"
    }),
  stats: () => request<StatsDto>("/api/stats"),
  globalStats: () => request<GlobalStatsDto>("/api/stats/global")
};
