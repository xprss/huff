export type TileState = "CORRECT" | "PRESENT" | "ABSENT";
export type GameStatus = "IN_PROGRESS" | "WON" | "LOST";

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
  status: GameStatus;
  maxAttempts: number;
  wordLength: number;
  guesses: GuessResult[];
  solution: string | null;
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
  authEnabled: boolean;
}
