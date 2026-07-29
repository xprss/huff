export type TileState = "CORRECT" | "PRESENT" | "ABSENT" | "HIDDEN";
export type GameStatus = "IN_PROGRESS" | "WON" | "LOST";
export type GameMode = "CLASSIC" | "MISCHIEVOUS_MOUSE";

export interface TileResult {
  letter: string;
  state: TileState;
}

export interface GuessResult {
  word: string;
  tiles: TileResult[];
}

export interface GameDto {
  puzzleDate: string;
  mode: GameMode;
  modeLabel: string;
  status: GameStatus;
  maxAttempts: number;
  answerLength: number;
  guesses: GuessResult[];
  solution: string | null;
  canChangeMode: boolean;
  kitten: {
    unlocked: boolean;
    used: boolean;
    canUse: boolean;
  };
}

export interface GameModeDto {
  mode: GameMode;
  label: string;
}

export interface TodayGameDto {
  puzzleDate: string;
  modes: GameModeDto[];
  game: GameDto | null;
}

export interface StatsDto {
  played: number;
  won: number;
  lost: number;
  currentStreak: number;
  maxStreak: number;
  guessDistribution: Record<string, number>;
}

export interface GlobalStatsDto {
  players: number;
  gamesStarted: number;
  completed: number;
  won: number;
  lost: number;
  guessDistribution: Record<string, number>;
}

export interface MeDto {
  loggedIn: boolean;
  user: null | {
    email: string | null;
    displayName: string | null;
    authenticated: boolean;
  };
  loginUrl: string | null;
  logoutUrl: string | null;
  authEnabled: boolean;
}

export interface PushSettingsDto {
  supported: boolean;
  publicKey: string | null;
}

export interface PushSubscriptionDto {
  endpoint: string;
  keys: {
    p256dh: string;
    auth: string;
  };
}
