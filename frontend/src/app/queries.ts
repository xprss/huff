import { queryOptions } from "@tanstack/react-query";
import { api } from "../api";
import type { AdminPlayerSortDto } from "../types";

export const queryKeys = {
  me: ["me"] as const,
  today: ["game", "today"] as const,
  stats: ["stats", "personal"] as const,
  globalStats: ["stats", "global"] as const,
  leaderboards: ["leaderboards"] as const,
  publicPlayer: (nickname: string) => ["player", nickname] as const,
  adminPlayers: (query: string, sort: AdminPlayerSortDto, page: number) => ["admin", "players", query, sort, page] as const,
  adminPlayer: (userId: string) => ["admin", "players", "detail", userId] as const
};

export const meQueryOptions = () =>
  queryOptions({
    queryKey: queryKeys.me,
    queryFn: api.me,
    refetchOnMount: "always",
    refetchOnReconnect: "always",
    refetchOnWindowFocus: "always",
    refetchInterval: 4 * 60 * 1000,
    refetchIntervalInBackground: true,
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

export const leaderboardsQueryOptions = () =>
  queryOptions({
    queryKey: queryKeys.leaderboards,
    queryFn: api.leaderboards
  });

export const publicPlayerQueryOptions = (nickname: string) =>
  queryOptions({
    queryKey: queryKeys.publicPlayer(nickname),
    queryFn: () => api.publicPlayer(nickname)
  });

export const adminPlayersQueryOptions = (query: string, sort: AdminPlayerSortDto, page: number) =>
  queryOptions({
    queryKey: queryKeys.adminPlayers(query, sort, page),
    queryFn: () => api.adminPlayers(query, sort, page)
  });

export const adminPlayerQueryOptions = (userId: string) =>
  queryOptions({
    queryKey: queryKeys.adminPlayer(userId),
    queryFn: () => api.adminPlayer(userId)
  });
