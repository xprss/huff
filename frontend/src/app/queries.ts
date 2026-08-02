import { queryOptions } from "@tanstack/react-query";
import { api } from "../api";

export const queryKeys = {
  me: ["me"] as const,
  today: ["game", "today"] as const,
  stats: ["stats", "personal"] as const,
  globalStats: ["stats", "global"] as const,
  adminPlayers: (query: string) => ["admin", "players", query] as const,
  adminPlayer: (userId: string) => ["admin", "players", "detail", userId] as const
};

export const meQueryOptions = () =>
  queryOptions({
    queryKey: queryKeys.me,
    queryFn: api.me,
    refetchOnMount: "always",
    refetchOnReconnect: "always",
    refetchOnWindowFocus: "always",
    staleTime: 0
  });

export const todayQueryOptions = () =>
  queryOptions({
    queryKey: queryKeys.today,
    queryFn: api.today
  });

export const statsQueryOptions = () =>
  queryOptions({
    queryKey: queryKeys.stats,
    queryFn: api.stats
  });

export const globalStatsQueryOptions = () =>
  queryOptions({
    queryKey: queryKeys.globalStats,
    queryFn: api.globalStats
  });

export const adminPlayersQueryOptions = (query: string) =>
  queryOptions({
    queryKey: queryKeys.adminPlayers(query),
    queryFn: () => api.adminPlayers(query)
  });

export const adminPlayerQueryOptions = (userId: string) =>
  queryOptions({
    queryKey: queryKeys.adminPlayer(userId),
    queryFn: () => api.adminPlayer(userId)
  });
