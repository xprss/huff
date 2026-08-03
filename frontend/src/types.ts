export type TileState = "CORRECT" | "PRESENT" | "ABSENT" | "HIDDEN";
export type GameStatus = "IN_PROGRESS" | "WON" | "LOST";
export type GameMode = "CLASSIC" | "MISCHIEVOUS_MOUSE";
export type IsoDateString = `${number}-${number}-${number}`;
export type GuessAttempt = "1" | "2" | "3" | "4" | "5" | "6";
export type GuessDistributionDto = Partial<Record<GuessAttempt, number>>;

export interface ApiErrorDto {
  readonly code?: string;
  readonly message?: string;
  readonly loginUrl?: string | null;
}

export interface TileResult {
  readonly letter: string;
  readonly state: TileState;
}

export interface GuessResult {
  readonly word: string;
  readonly tiles: readonly TileResult[];
}

export interface KittenDto {
  readonly unlocked: boolean;
  readonly used: boolean;
  readonly canUse: boolean;
}

export interface StarDto {
  readonly available: boolean;
  readonly used: boolean;
  readonly canUse: boolean;
  readonly justAwarded: boolean;
}

export interface GameDto {
  readonly puzzleDate: IsoDateString;
  readonly mode: GameMode;
  readonly modeLabel: string;
  readonly status: GameStatus;
  readonly maxAttempts: number;
  readonly answerLength: number;
  readonly guesses: readonly GuessResult[];
  readonly solution: string | null;
  readonly canChangeMode: boolean;
  readonly kitten: KittenDto;
  readonly star: StarDto;
}

export interface StarRevealDto {
  readonly game: GameDto;
  readonly guesses: readonly GuessResult[];
}

export interface GameModeDto {
  readonly mode: GameMode;
  readonly label: string;
}

export interface TodayGameDto {
  readonly puzzleDate: IsoDateString;
  readonly modes: readonly GameModeDto[];
  readonly game: GameDto | null;
}

export interface StatsDto {
  readonly played: number;
  readonly won: number;
  readonly lost: number;
  readonly currentStreak: number;
  readonly maxStreak: number;
  readonly guessDistribution: GuessDistributionDto;
}

export interface GlobalStatsDto {
  readonly players: number;
  readonly gamesStarted: number;
  readonly completed: number;
  readonly won: number;
  readonly lost: number;
  readonly guessDistribution: GuessDistributionDto;
}

export interface MeDto {
  readonly loggedIn: boolean;
  readonly user: UserDto | null;
  readonly loginUrl: string | null;
  readonly logoutUrl: string | null;
  readonly authEnabled: boolean;
}

export interface AdminPrivilegesDto {
  readonly canViewPlayers: boolean;
  readonly canViewPlayerDetails: boolean;
  readonly canManagePlayers: boolean;
  readonly canManageAdmins: boolean;
}

export interface UserDto {
  readonly email: string | null;
  readonly displayName: string | null;
  readonly nickname: string;
  readonly profileEmoji: string;
  readonly bio: string | null;
  readonly authenticated: boolean;
  readonly admin: AdminPrivilegesDto | null;
}

export interface ProfileUpdateDto {
  readonly displayName: string;
  readonly nickname: string;
  readonly profileEmoji: string;
  readonly bio: string | null;
}

export interface ModeRequestDto {
  readonly mode: GameMode;
}

export interface GuessRequestDto {
  readonly guess: string;
}

export interface PushSettingsDto {
  readonly supported: boolean;
  readonly publicKey: string | null;
}

export interface PushSubscriptionKeysDto {
  readonly p256dh: string;
  readonly auth: string;
}

export interface PushSubscriptionDto {
  readonly endpoint: string;
  readonly keys: PushSubscriptionKeysDto;
}

export interface AdminPlayerSummaryDto {
  readonly id: string;
  readonly email: string | null;
  readonly displayName: string | null;
  readonly nickname: string;
  readonly profileEmoji: string;
  readonly bio: string | null;
  readonly authenticated: boolean;
  readonly admin: boolean;
  readonly starAvailable: boolean;
  readonly starAwardedAt: string | null;
  readonly starUsedAt: string | null;
  readonly gamesStarted: number;
  readonly completed: number;
  readonly won: number;
  readonly lost: number;
  readonly winRate: number;
  readonly lastActivityAt: string | null;
}

export type AdminPlayerSortDto = "alphabetical" | "recent-game" | "games-played";

export interface AdminPlayersPageDto {
  readonly players: readonly AdminPlayerSummaryDto[];
  readonly page: number;
  readonly pageSize: number;
  readonly totalPlayers: number;
  readonly totalPages: number;
  readonly sort: AdminPlayerSortDto;
}

export interface AdminGameDto {
  readonly id: string;
  readonly puzzleDate: IsoDateString;
  readonly mode: GameMode;
  readonly modeLabel: string;
  readonly solution: string;
  readonly guesses: readonly GuessResult[];
  readonly status: GameStatus;
  readonly mouseTileIndex: number | null;
  readonly mouseRevealed: boolean | null;
  readonly kittenUnlocked: boolean | null;
  readonly kittenUsedAt: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly completedAt: string | null;
}

export interface AdminPlayerDetailDto {
  readonly player: AdminPlayerSummaryDto;
  readonly googleSubject: string | null;
  readonly createdAt: string;
  readonly pushSubscriptions: number;
  readonly stats: StatsDto;
  readonly games: readonly AdminGameDto[];
}

export interface AdminPlayerUpdateDto {
  readonly displayName: string;
  readonly nickname: string;
  readonly profileEmoji: string;
  readonly bio: string | null;
  readonly starAvailable: boolean;
  readonly starAwardedAt: string | null;
  readonly starUsedAt: string | null;
}

export interface AdminDeleteResultDto {
  readonly users: number;
  readonly games: number;
  readonly pushSubscriptions: number;
  readonly adminRows: number;
}

export type HttpMethod = "GET" | "POST" | "PUT" | "DELETE";

export type ApiEndpointMap = {
  "/api/me": {
    GET: {
      response: MeDto;
    };
  };
  "/api/game/today": {
    GET: {
      response: TodayGameDto;
    };
  };
  "/api/game/today/mode": {
    POST: {
      body: ModeRequestDto;
      response: GameDto;
    };
  };
  "/api/game/today/guesses": {
    POST: {
      body: GuessRequestDto;
      response: GameDto;
    };
  };
  "/api/game/today/kitten": {
    POST: {
      response: GameDto;
    };
  };
  "/api/game/today/star": {
    POST: {
      response: StarRevealDto;
    };
  };
  "/api/stats": {
    GET: {
      response: StatsDto;
    };
  };
  "/api/me/profile": {
    PUT: {
      body: ProfileUpdateDto;
      response: UserDto;
    };
  };
  "/api/stats/global": {
    GET: {
      response: GlobalStatsDto;
    };
  };
  "/api/push/settings": {
    GET: {
      response: PushSettingsDto;
    };
  };
  "/api/push/subscriptions": {
    POST: {
      body: PushSubscriptionDto;
      response: void;
    };
    DELETE: {
      body: PushSubscriptionDto;
      response: void;
    };
  };
  "/api/admin/players": {
    GET: {
      response: AdminPlayersPageDto;
    };
  };
  "/api/admin/players/:userId": {
    GET: {
      response: AdminPlayerDetailDto;
    };
    PUT: {
      body: AdminPlayerUpdateDto;
      response: AdminPlayerDetailDto;
    };
    DELETE: {
      response: AdminDeleteResultDto;
    };
  };
};
