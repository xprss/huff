import type {
  ApiEndpointMap,
  ApiErrorDto,
  HttpMethod,
  GameMode,
  ProfileUpdateDto,
  PushSubscriptionDto,
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

export class ApiError extends Error {
  readonly status: number;
  readonly code: string | null;
  readonly loginUrl: string | null;

  constructor(message: string, status: number, code?: string | null, loginUrl?: string | null) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code ?? null;
    this.loginUrl = loginUrl ?? null;
  }
}

export function isAuthRequiredError(error: unknown): error is ApiError {
  return error instanceof ApiError && (error.status === 401 || error.code === "auth_required");
}

export type ApiClient = {
  readonly me: EndpointHandler<"/api/me", "GET">;
  readonly today: EndpointHandler<"/api/game/today", "GET">;
  readonly selectMode: EndpointHandler<"/api/game/today/mode", "POST", [mode: GameMode]>;
  readonly guess: EndpointHandler<"/api/game/today/guesses", "POST", [guess: string]>;
  readonly useKitten: EndpointHandler<"/api/game/today/kitten", "POST">;
  readonly useStar: EndpointHandler<"/api/game/today/star", "POST">;
  readonly stats: EndpointHandler<"/api/stats", "GET">;
  readonly updateProfile: EndpointHandler<"/api/me/profile", "PUT", [profile: ProfileUpdateDto]>;
  readonly globalStats: EndpointHandler<"/api/stats/global", "GET">;
  readonly pushSettings: EndpointHandler<"/api/push/settings", "GET">;
  readonly savePushSubscription: EndpointHandler<"/api/push/subscriptions", "POST", [subscription: PushSubscriptionDto]>;
  readonly deletePushSubscription: EndpointHandler<
    "/api/push/subscriptions",
    "DELETE",
    [subscription: PushSubscriptionDto]
  >;
};

async function request<Path extends ApiPath, Method extends ApiMethod<Path>>(
  path: Path,
  init: ApiRequestInit<Path, Method>
): Promise<ApiResponse<Path, Method>> {
  const { body, headers, ...requestInit } = init;
  const response = await fetch(path, {
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(headers ?? {})
    },
    ...requestInit,
    body: body === undefined ? undefined : JSON.stringify(body)
  });

  if (!response.ok) {
    const errorBody = (await response.json().catch(() => null)) as ApiErrorDto | null;
    throw new ApiError(
      errorBody?.message ?? (response.status === 401 ? "Accesso Google richiesto." : "Richiesta non riuscita"),
      response.status,
      errorBody?.code,
      errorBody?.loginUrl
    );
  }
  if (response.status === 204) {
    return undefined as ApiResponse<Path, Method>;
  }
  return response.json() as Promise<ApiResponse<Path, Method>>;
}

export const api = {
  me: () => request("/api/me", { method: "GET" }),
  today: () => request("/api/game/today", { method: "GET" }),
  selectMode: (mode: GameMode) =>
    request("/api/game/today/mode", {
      method: "POST",
      body: { mode }
    }),
  guess: (guess: string) =>
    request("/api/game/today/guesses", {
      method: "POST",
      body: { guess }
    }),
  useKitten: () =>
    request("/api/game/today/kitten", {
      method: "POST"
    }),
  useStar: () =>
    request("/api/game/today/star", {
      method: "POST"
    }),
  stats: () => request("/api/stats", { method: "GET" }),
  updateProfile: (profile: ProfileUpdateDto) =>
    request("/api/me/profile", {
      method: "PUT",
      body: profile
    }),
  globalStats: () => request("/api/stats/global", { method: "GET" }),
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
    })
} satisfies ApiClient;
