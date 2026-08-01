import type { IsoDateString } from "../../types";

export function formatPuzzleDate(value: IsoDateString | undefined) {
  if (!value) {
    return formatItalianDate(new Date());
  }
  const [year, month, day] = value.split("-").map(Number);
  if (!year || !month || !day) {
    return value;
  }
  return formatItalianDate(new Date(year, month - 1, day));
}

function formatItalianDate(date: Date) {
  return new Intl.DateTimeFormat("it-IT", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric"
  }).format(date);
}

export function getNextChallengeTime() {
  const now = new Date();
  const nextMidnight = new Date(now);
  nextMidnight.setHours(24, 0, 0, 0);
  return nextMidnight.getTime();
}

export function getRemainingMilliseconds(targetTime: number) {
  return Math.max(0, targetTime - Date.now());
}

export function formatNextChallengeCountdown() {
  return formatCountdownDuration(getRemainingMilliseconds(getNextChallengeTime()));
}

export function formatCountdownDuration(remainingMilliseconds: number) {
  const totalSeconds = Math.max(0, Math.ceil(remainingMilliseconds / 1000));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  return `${formatTimeUnit(hours, "ora", "ore")}, ${formatTimeUnit(minutes, "minuto", "minuti")} e ${formatTimeUnit(
    seconds,
    "secondo",
    "secondi"
  )}`;
}

function formatTimeUnit(value: number, singular: string, plural: string) {
  return `${value} ${value === 1 ? singular : plural}`;
}
