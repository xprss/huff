export type TileState = "CORRECT" | "PRESENT" | "ABSENT" | "HIDDEN";
export type GameStatus = "IN_PROGRESS" | "WON" | "LOST";
export type GameMode = "CLASSIC" | "MISCHIEVOUS_MOUSE" | "STUBBORN_CRAB";
export type IsoDateString = `${number}-${number}-${number}`;
export type GuessAttempt = "1" | "2" | "3" | "4" | "5" | "6";
export type GuessDistributionDto = Partial<Record<GuessAttempt, number>>;

export interface ApiErrorDto {
  readonly code?: string;
  readonly message?: string;
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
  readonly firstGuessSuggestion: string | null;
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
  readonly authEnabled: boolean;
  readonly pendingAnnouncements: readonly AnnouncementCampaign[];
}

export type AnnouncementCampaign = "HEXAHACK_LAUNCH";
export type HexahackStatus = "IN_PROGRESS" | "COMPLETED";
export type HexahackRank = "GHOST" | "SHADOW" | "BREACH" | "TRACED";
export type HexahackProbeType = "PING" | "BIT_SCAN" | "LINK_TRACE" | "CHECKSUM";
export type HexahackEventKind = "PROBE" | "SUBMISSION";
export type HexahackProbeComparison = "BELOW" | "EQUAL" | "ABOVE";
export type HexahackParity = "EVEN" | "ODD";

export interface HexahackProbeRequestDto {
  readonly requestId: string;
  readonly type: HexahackProbeType;
  readonly position: number;
  readonly threshold?: number;
  readonly otherPosition?: number;
}

export interface HexahackSubmissionRequestDto {
  readonly requestId: string;
  readonly code: string;
}

export interface HexahackProbeResultDto {
  readonly requestId: string;
  readonly type: HexahackProbeType;
  readonly cost: number;
  readonly position: number;
  readonly otherPosition: number | null;
  readonly threshold: number | null;
  readonly comparison: HexahackProbeComparison | null;
  readonly parity: HexahackParity | null;
  readonly sum: number | null;
  readonly summary: string;
}

export interface HexahackSubmissionResultDto {
  readonly requestId: string;
  readonly code: string;
  readonly correctPositions: number;
  readonly granted: boolean;
}

export interface HexahackEventDto {
  readonly sequence: number;
  readonly kind: HexahackEventKind;
  readonly occurredAt: string;
  readonly probe: HexahackProbeResultDto | null;
  readonly submission: HexahackSubmissionResultDto | null;
}

export interface HexahackGameDto {
  readonly puzzleDate: IsoDateString;
  readonly rulesVersion: number;
  readonly status: HexahackStatus;
  readonly answerLength: number;
  readonly log: readonly HexahackEventDto[];
  readonly totalCost: number;
  readonly wrongSubmissions: number;
  readonly currentStealth: number;
  readonly projectedRank: HexahackRank;
  readonly finalStealth: number | null;
  readonly rank: HexahackRank | null;
  readonly solution: string | null;
  readonly completedAt: string | null;
}

export interface HexahackTodayDto {
  readonly puzzleDate: IsoDateString;
  readonly rulesVersion: number;
  readonly answerLength: number;
  readonly freeClues: { readonly totalSum: number; readonly distinctDigits: number };
  readonly game: HexahackGameDto | null;
}

export interface HexahackProbeActionDto {
  readonly game: HexahackGameDto;
  readonly result: HexahackProbeResultDto;
  readonly replayed: boolean;
}

export interface HexahackSubmissionActionDto {
  readonly game: HexahackGameDto;
  readonly result: HexahackSubmissionResultDto;
  readonly replayed: boolean;
}

export interface HexahackCalendarNodeDto {
  readonly puzzleDate: IsoDateString;
  readonly completed: boolean;
  readonly stealth: number | null;
  readonly rank: HexahackRank | null;
  readonly totalCost: number | null;
  readonly wrongSubmissions: number | null;
}

export interface HexahackStatsDto {
  readonly completedAccesses: number;
  readonly averageStealth: number;
  readonly bestStealth: number;
  readonly rankDistribution: Readonly<Record<HexahackRank, number>>;
  readonly currentStreak: number;
  readonly maxStreak: number;
  readonly last30Nodes: readonly HexahackCalendarNodeDto[];
}

export type HexaskyStatus = "IN_PROGRESS" | "WON" | "LOST";
export interface HexaskyVisibilityDto { readonly top: readonly number[]; readonly right: readonly number[]; readonly bottom: readonly number[]; readonly left: readonly number[]; }
export interface HexaskyCheckRequestDto { readonly requestId: string; readonly solution: readonly number[]; }
export interface HexaskyCheckResultDto { readonly requestId: string; readonly correct: boolean; readonly checksUsed: number; readonly status: HexaskyStatus; readonly solution: readonly number[] | null; readonly incorrectCells?: readonly number[]; }
export interface HexaskyEventDto { readonly sequence: number; readonly kind: "CHECK"; readonly occurredAt: string; readonly check: HexaskyCheckResultDto; }
export interface HexaskyGameDto { readonly puzzleDate: IsoDateString; readonly rulesVersion: number; readonly status: HexaskyStatus; readonly checksUsed: number; readonly proposal: readonly number[] | null; readonly log: readonly HexaskyEventDto[]; readonly solution: readonly number[] | null; readonly completedAt: string | null; }
export interface HexaskyTodayDto { readonly puzzleDate: IsoDateString; readonly rulesVersion: number; readonly visibility: HexaskyVisibilityDto; readonly game: HexaskyGameDto | null; }
export interface HexaskyCheckActionDto { readonly game: HexaskyGameDto; readonly result: HexaskyCheckResultDto; readonly replayed: boolean; }
export interface HexaskyStatsDto { readonly played: number; readonly won: number; readonly lost: number; readonly currentStreak: number; readonly maxStreak: number; readonly checkDistribution: Readonly<Record<"1" | "2", number>>; }

export interface StatsSetDto {
  readonly overall: StatsDto;
  readonly hexaword: StatsDto;
  readonly hexahack: HexahackStatsDto;
  readonly hexasky: HexaskyStatsDto;
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
  readonly inputHandPreference: InputHandPreference;
  readonly authenticated: boolean;
  readonly admin: AdminPrivilegesDto | null;
  readonly medals: MedalCountsDto;
}

export type InputHandPreference = "LEFT" | "RIGHT";

export interface MedalCountsDto {
  readonly gold: number;
  readonly silver: number;
  readonly bronze: number;
}

export interface LeaderboardEntryDto {
  readonly rank: number;
  readonly displayName: string;
  readonly nickname: string;
  readonly profileEmoji: string;
  readonly wins: number;
}

export interface LeaderboardPeriodDto {
  readonly startDate: IsoDateString | null;
  readonly endDate: IsoDateString | null;
  readonly entries: readonly LeaderboardEntryDto[];
}

export interface LeaderboardsDto {
  readonly allTime: LeaderboardPeriodDto;
  readonly yearly: LeaderboardPeriodDto;
  readonly monthly: LeaderboardPeriodDto;
  readonly weekly: LeaderboardPeriodDto;
}

export type LeaderboardGame = "overall" | "hexaword" | "hexahack" | "hexasky";

export interface PublicPlayerProfileDto {
  readonly displayName: string;
  readonly nickname: string;
  readonly profileEmoji: string;
  readonly bio: string | null;
  readonly stats: StatsDto;
  readonly overallStats: StatsDto;
  readonly hexawordStats: StatsDto;
  readonly hexahackStats: HexahackStatsDto;
  readonly hexaskyStats: HexaskyStatsDto;
  readonly medals: MedalCountsDto;
}

export interface ProfileUpdateDto {
  readonly displayName: string;
  readonly nickname: string;
  readonly profileEmoji: string;
  readonly bio: string | null;
  readonly inputHandPreference: InputHandPreference;
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
  readonly hexahackGames: readonly AdminHexahackGameDto[];
}

export interface AdminHexahackGameDto {
  readonly id: string;
  readonly puzzleDate: IsoDateString;
  readonly rulesVersion: number;
  readonly solution: string;
  readonly log: readonly HexahackEventDto[];
  readonly totalCost: number;
  readonly wrongSubmissions: number;
  readonly status: HexahackStatus;
  readonly stealth: number | null;
  readonly rank: HexahackRank | null;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly completedAt: string | null;
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
  "/api/logout": {
    POST: {
      response: void;
    };
  };
  "/api/me": {
    GET: {
      response: MeDto;
    };
  };
  "/api/me/announcements/HEXAHACK_LAUNCH/seen": {
    POST: { response: void };
  };
  "/api/hexaword/today": { GET: { response: TodayGameDto } };
  "/api/hexaword/today/mode": { POST: { body: ModeRequestDto; response: GameDto } };
  "/api/hexaword/today/guesses": { POST: { body: GuessRequestDto; response: GameDto } };
  "/api/hexaword/today/kitten": { POST: { response: GameDto } };
  "/api/hexaword/today/star": { POST: { response: StarRevealDto } };
  "/api/hexaword/stats": { GET: { response: StatsDto } };
  "/api/hexahack/today": { GET: { response: HexahackTodayDto } };
  "/api/hexahack/today/probes": { POST: { body: HexahackProbeRequestDto; response: HexahackProbeActionDto } };
  "/api/hexahack/today/submissions": { POST: { body: HexahackSubmissionRequestDto; response: HexahackSubmissionActionDto } };
  "/api/hexahack/stats": { GET: { response: HexahackStatsDto } };
  "/api/hexasky/today": { GET: { response: HexaskyTodayDto } };
  "/api/hexasky/today/checks": { POST: { body: HexaskyCheckRequestDto; response: HexaskyCheckActionDto } };
  "/api/hexasky/stats": { GET: { response: HexaskyStatsDto } };
  "/api/overall/stats": { GET: { response: StatsDto } };
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
  "/api/leaderboards": {
    GET: {
      response: LeaderboardsDto;
    };
  };
  "/api/hexaword/leaderboards": { GET: { response: LeaderboardsDto } };
  "/api/hexahack/leaderboards": { GET: { response: LeaderboardsDto } };
  "/api/hexasky/leaderboards": { GET: { response: LeaderboardsDto } };
  "/api/leaderboards/overall": { GET: { response: LeaderboardsDto } };
  "/api/player/:nickname": {
    GET: {
      response: PublicPlayerProfileDto;
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
