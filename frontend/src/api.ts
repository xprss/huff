import type {
  ApiEndpointMap,
  ApiErrorDto,
  HttpMethod,
  GameMode,
  AdminPlayerSortDto,
  AdminPlayerUpdateDto,
  HexahackProbeRequestDto,
  HexahackSubmissionRequestDto,
  HexaskyCheckRequestDto,
  LeaderboardGame,
  LeaderboardsDto,
  ProfileUpdateDto,
  PushSubscriptionDto
} from "./types";

type ApiPath = keyof ApiEndpointMap;
type ApiMethod<Path extends ApiPath> = keyof ApiEndpointMap[Path] & HttpMethod;
type ApiSpec<Path extends ApiPath, Method extends ApiMethod<Path>> = ApiEndpointMap[Path][Method];
type ApiResponse<Path extends ApiPath, Method extends ApiMethod<Path>> = ApiSpec<Path, Method> extends {
  response: infer Response;
}
  ? Response
  : never;
type ApiRequestBody<Path extends ApiPath, Method extends ApiMethod<Path>> = ApiSpec<Path, Method> extends {
  body: infer Body;
}
  ? Body
  : never;
type ApiRequestInit<Path extends ApiPath, Method extends ApiMethod<Path>> = Omit<RequestInit, "body" | "method"> & {
  method: Method;
} & ([ApiRequestBody<Path, Method>] extends [never]
    ? { body?: never }
    : { body: ApiRequestBody<Path, Method> });
type EndpointHandler<Path extends ApiPath, Method extends ApiMethod<Path>, Args extends readonly unknown[] = []> = (
  ...args: Args
) => Promise<ApiResponse<Path, Method>>;

const ACCESS_TOKEN_STORAGE_KEY = "huff.access-token";

export class ApiError extends Error {
  readonly status: number;
  readonly code: string | null;

  constructor(message: string, status: number, code?: string | null) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code ?? null;
  }
}

export function isAuthRequiredError(error: unknown): error is ApiError {
  return error instanceof ApiError && (error.status === 401 || error.code === "token_required");
}

export function accessToken(): string | null {
  const value = sessionStorage.getItem(ACCESS_TOKEN_STORAGE_KEY)?.trim();
  return value || null;
}

export function storeAccessToken(value: string) {
  const token = value.trim().replace(/^Bearer\s+/i, "");
  if (token) {
    sessionStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, token);
  }
}

export function clearAccessToken() {
  sessionStorage.removeItem(ACCESS_TOKEN_STORAGE_KEY);
}

export type ApiClient = {
  readonly logout: EndpointHandler<"/api/logout", "POST">;
  readonly me: EndpointHandler<"/api/me", "GET">;
  readonly markHexahackLaunchSeen: EndpointHandler<"/api/me/announcements/HEXAHACK_LAUNCH/seen", "POST">;
  readonly today: EndpointHandler<"/api/hexaword/today", "GET">;
  readonly selectMode: EndpointHandler<"/api/hexaword/today/mode", "POST", [mode: GameMode]>;
  readonly guess: EndpointHandler<"/api/hexaword/today/guesses", "POST", [guess: string]>;
  readonly useKitten: EndpointHandler<"/api/hexaword/today/kitten", "POST">;
  readonly useStar: EndpointHandler<"/api/hexaword/today/star", "POST">;
  readonly stats: EndpointHandler<"/api/hexaword/stats", "GET">;
  readonly hexahackToday: EndpointHandler<"/api/hexahack/today", "GET">;
  readonly hexahackProbe: EndpointHandler<"/api/hexahack/today/probes", "POST", [probe: HexahackProbeRequestDto]>;
  readonly hexahackSubmit: EndpointHandler<"/api/hexahack/today/submissions", "POST", [submission: HexahackSubmissionRequestDto]>;
  readonly hexahackStats: EndpointHandler<"/api/hexahack/stats", "GET">;
  readonly hexaskyToday: EndpointHandler<"/api/hexasky/today", "GET">;
  readonly hexaskyCheck: EndpointHandler<"/api/hexasky/today/checks", "POST", [request: HexaskyCheckRequestDto]>;
  readonly hexaskyStats: EndpointHandler<"/api/hexasky/stats", "GET">;
  readonly overallStats: EndpointHandler<"/api/overall/stats", "GET">;
  readonly updateProfile: EndpointHandler<"/api/me/profile", "PUT", [profile: ProfileUpdateDto]>;
  readonly globalStats: EndpointHandler<"/api/stats/global", "GET">;
  readonly leaderboards: (game: LeaderboardGame) => Promise<LeaderboardsDto>;
  readonly publicPlayer: EndpointHandler<"/api/player/:nickname", "GET", [nickname: string]>;
  readonly pushSettings: EndpointHandler<"/api/push/settings", "GET">;
  readonly savePushSubscription: EndpointHandler<"/api/push/subscriptions", "POST", [subscription: PushSubscriptionDto]>;
  readonly deletePushSubscription: EndpointHandler<
    "/api/push/subscriptions",
    "DELETE",
    [subscription: PushSubscriptionDto]
  >;
  readonly adminPlayers: EndpointHandler<
    "/api/admin/players",
    "GET",
    [query?: string, sort?: AdminPlayerSortDto, page?: number]
  >;
  readonly adminPlayer: EndpointHandler<"/api/admin/players/:userId", "GET", [userId: string]>;
  readonly updateAdminPlayer: EndpointHandler<
    "/api/admin/players/:userId",
    "PUT",
    [userId: string, player: AdminPlayerUpdateDto]
  >;
  readonly deleteAdminPlayer: EndpointHandler<"/api/admin/players/:userId", "DELETE", [userId: string]>;
};

async function request<Path extends ApiPath, Method extends ApiMethod<Path>>(
  path: Path,
  init: ApiRequestInit<Path, Method>
): Promise<ApiResponse<Path, Method>> {
  const { body, headers, ...requestInit } = init;
  const isMutation = init.method !== "GET";
  const token = accessToken();
  const response = await fetch(path, {
    credentials: "same-origin",
    headers: {
      "Content-Type": "application/json",
      ...(isMutation ? { "X-Huff-Request": "1" } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(headers ?? {})
    },
    ...requestInit,
    body: body === undefined ? undefined : JSON.stringify(body)
  });

  if (!response.ok) {
    const errorBody = (await response.json().catch(() => null)) as ApiErrorDto | null;
    if (response.status === 401) {
      clearAccessToken();
    }
    throw new ApiError(
      errorBody?.message ?? (response.status === 401 ? "Token Bearer valido richiesto." : "Richiesta non riuscita"),
      response.status,
      errorBody?.code
    );
  }
  if (response.status === 204) {
    return undefined as ApiResponse<Path, Method>;
  }
  return response.json() as Promise<ApiResponse<Path, Method>>;
}

export const api = {
  logout: () => request("/api/logout", { method: "POST" }),
  me: () => request("/api/me", { method: "GET" }),
  markHexahackLaunchSeen: () => request("/api/me/announcements/HEXAHACK_LAUNCH/seen", { method: "POST" }),
  today: () => request("/api/hexaword/today", { method: "GET" }),
  selectMode: (mode: GameMode) =>
    request("/api/hexaword/today/mode", {
      method: "POST",
      body: { mode }
    }),
  guess: (guess: string) =>
    request("/api/hexaword/today/guesses", {
      method: "POST",
      body: { guess }
    }),
  useKitten: () =>
    request("/api/hexaword/today/kitten", {
      method: "POST"
    }),
  useStar: () =>
    request("/api/hexaword/today/star", {
      method: "POST"
    }),
  stats: () => request("/api/hexaword/stats", { method: "GET" }),
  hexahackToday: () => request("/api/hexahack/today", { method: "GET" }),
  hexahackProbe: (probe: HexahackProbeRequestDto) => request("/api/hexahack/today/probes", { method: "POST", body: probe }),
  hexahackSubmit: (submission: HexahackSubmissionRequestDto) => request("/api/hexahack/today/submissions", { method: "POST", body: submission }),
  hexahackStats: () => request("/api/hexahack/stats", { method: "GET" }),
  hexaskyToday: () => request("/api/hexasky/today", { method: "GET" }),
  hexaskyCheck: (checkRequest: HexaskyCheckRequestDto) => request("/api/hexasky/today/checks", { method: "POST", body: checkRequest }),
  hexaskyStats: () => request("/api/hexasky/stats", { method: "GET" }),
  overallStats: () => request("/api/overall/stats", { method: "GET" }),
  updateProfile: (profile: ProfileUpdateDto) =>
    request("/api/me/profile", {
      method: "PUT",
      body: profile
    }),
  globalStats: () => request("/api/stats/global", { method: "GET" }),
  leaderboards: (game: LeaderboardGame) => {
    const paths = {
      overall: "/api/leaderboards/overall",
      hexaword: "/api/hexaword/leaderboards",
      hexahack: "/api/hexahack/leaderboards",
      hexasky: "/api/hexasky/leaderboards"
    } as const;
    return request(paths[game], { method: "GET" });
  },
  publicPlayer: (nickname: string) =>
    request(`/api/player/${encodeURIComponent(nickname)}` as "/api/player/:nickname", { method: "GET" }),
  pushSettings: () => request("/api/push/settings", { method: "GET" }),
  savePushSubscription: (subscription: PushSubscriptionDto) =>
    request("/api/push/subscriptions", {
      method: "POST",
      body: subscription
    }),
  deletePushSubscription: (subscription: PushSubscriptionDto) =>
    request("/api/push/subscriptions", {
      method: "DELETE",
      body: subscription
    }),
  adminPlayers: (query?: string, sort?: AdminPlayerSortDto, page?: number) => {
    const search = query?.trim();
    const params = new URLSearchParams();
    if (search) params.set("q", search);
    if (sort) params.set("sort", sort);
    if (page && page > 0) params.set("page", String(page));
    const queryString = params.toString();
    const path = queryString ? (`/api/admin/players?${queryString}` as "/api/admin/players") : "/api/admin/players";
    return request(path, {
      method: "GET"
    });
  },
  adminPlayer: (userId: string) =>
    request(`/api/admin/players/${encodeURIComponent(userId)}` as "/api/admin/players/:userId", {
      method: "GET"
    }),
  updateAdminPlayer: (userId: string, player: AdminPlayerUpdateDto) =>
    request(`/api/admin/players/${encodeURIComponent(userId)}` as "/api/admin/players/:userId", {
      method: "PUT",
      body: player
    }),
  deleteAdminPlayer: (userId: string) =>
    request(`/api/admin/players/${encodeURIComponent(userId)}` as "/api/admin/players/:userId", {
      method: "DELETE"
    })
} satisfies ApiClient;
