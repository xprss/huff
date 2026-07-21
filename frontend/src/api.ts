import type { GameDto, MeDto, StatsDto } from "./types";

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
  today: () => request<GameDto>("/api/game/today"),
  guess: (guess: string) =>
    request<GameDto>("/api/game/today/guesses", {
      method: "POST",
      body: JSON.stringify({ guess })
    }),
  stats: () => request<StatsDto>("/api/stats")
};
