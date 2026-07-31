import type {
  GameDto,
  GameMode,
  GlobalStatsDto,
  MeDto,
  PushSettingsDto,
  PushSubscriptionDto,
  StatsDto,
  StarRevealDto,
  TodayGameDto
} from "./types";

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
  if (response.status === 204) {
    return undefined as T;
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
  useStar: () =>
    request<StarRevealDto>("/api/game/today/star", {
      method: "POST"
    }),
  stats: () => request<StatsDto>("/api/stats"),
  globalStats: () => request<GlobalStatsDto>("/api/stats/global"),
  pushSettings: () => request<PushSettingsDto>("/api/push/settings"),
  savePushSubscription: (subscription: PushSubscriptionDto) =>
    request<void>("/api/push/subscriptions", {
      method: "POST",
      body: JSON.stringify(subscription)
    }),
  deletePushSubscription: (subscription: PushSubscriptionDto) =>
    request<void>("/api/push/subscriptions", {
      method: "DELETE",
      body: JSON.stringify(subscription)
    })
};
