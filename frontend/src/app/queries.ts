import { queryOptions } from "@tanstack/react-query";
import { api } from "../api";

export const queryKeys = {
  me: ["me"] as const,
  today: ["game", "today"] as const,
  stats: ["stats", "personal"] as const,
  globalStats: ["stats", "global"] as const
};

export const meQueryOptions = () =>
  queryOptions({
    queryKey: queryKeys.me,
    queryFn: api.me
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
