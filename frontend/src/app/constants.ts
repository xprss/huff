import type { TileState } from "../types";

export const APP_NAME = "HexaQuot";
export const KEY_ROWS = ["QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM"];
export const STATE_RANK: Record<Exclude<TileState, "HIDDEN">, number> = {
  ABSENT: 1,
  PRESENT: 2,
  CORRECT: 3
};
export const SHARE_EMOJI: Record<TileState, string> = {
  CORRECT: "🟩",
  PRESENT: "🟨",
  ABSENT: "⬛",
  HIDDEN: "🐭"
};
export const VICTORY_CONFETTI_COLORS = ["#25d7a1", "#ffd166", "#ff6b7a", "#5ab9ff", "#ffffff"];
export const COUNTDOWN_INTERVAL_MS = 1000;
export const NEXT_CHALLENGE_REFRESH_DELAY_MS = 2000;
export const STAR_REVEAL_DURATION_MS = 15000;
export const TOAST_DURATION_MS = 3800;
export const REPOSITORY_URL = "https://github.com/xprss/huff";
export const VIEWPORT_SYNC_DELAYS_MS = [40, 120, 280, 600, 1200];
export const PROFILE_EMOJIS = ["😀", "😄", "😎", "🤓", "🥳", "😇", "🤠", "😴", "😤", "😍", "🙃", "😌"];
export const ZOOM_KEYS = new Set(["+", "-", "=", "_", "0"]);
