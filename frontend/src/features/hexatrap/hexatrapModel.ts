import type { HexatrapCaseDto, HexatrapCharacterId, HexatrapGameDto, HexatrapOutcome } from "../../types";

/** Pure helpers of the Hexatrap investigation, shared by the view and the tests. */

export type Mark = "unknown" | "no" | "yes";
export type Suspicion = "unknown" | "liar" | "truthful";
export interface Notebook {
  readonly facts: Readonly<Record<string, Readonly<Record<string, Mark>>>>;
  readonly suspicion: Readonly<Record<HexatrapCharacterId, Suspicion>>;
}
export interface VerdictDraft {
  readonly liar: HexatrapCharacterId | null;
  readonly facts: Readonly<Record<string, string>>;
}

export const EMPTY_NOTEBOOK: Notebook = { facts: {}, suspicion: { A: "unknown", B: "unknown" } };
export const EMPTY_VERDICT: VerdictDraft = { liar: null, facts: {} };

const DIFFICULTIES = ["", "Lieve", "Facile", "Media", "Ardua", "Diabolica"];

export function difficultyLabel(difficulty: number): string {
  return DIFFICULTIES[Math.min(5, Math.max(1, difficulty))];
}

export function capitalize(text: string): string {
  return text ? text.charAt(0).toLocaleUpperCase("it-IT") + text.slice(1) : text;
}

export function minutesUsed(game: HexatrapGameDto | null): number {
  return game?.minutesUsed ?? 0;
}

export function minutesLeft(caseFile: HexatrapCaseDto, game: HexatrapGameDto | null): number {
  return caseFile.budget - minutesUsed(game);
}

export function isClosed(game: HexatrapGameDto | null): boolean {
  return Boolean(game && game.status !== "IN_PROGRESS");
}

export function canAfford(caseFile: HexatrapCaseDto, game: HexatrapGameDto | null, cost: number): boolean {
  return !isClosed(game) && minutesLeft(caseFile, game) >= cost;
}

export function characterName(caseFile: HexatrapCaseDto, id: HexatrapCharacterId): string {
  return caseFile.characters.find((character) => character.id === id)?.name ?? id;
}

export function optionLabel(caseFile: HexatrapCaseDto, factId: string, optionId: string | undefined): string {
  const fact = caseFile.facts.find((candidate) => candidate.id === factId);
  return fact?.options.find((option) => option.id === optionId)?.label ?? "—";
}

export function verdictFacts(caseFile: HexatrapCaseDto) {
  return caseFile.facts.filter((fact) => fact.verdict);
}

export function verdictReady(caseFile: HexatrapCaseDto, draft: VerdictDraft): boolean {
  return draft.liar !== null && verdictFacts(caseFile).every((fact) => fact.options.some((option) => option.id === draft.facts[fact.id]));
}

/** The cheapest action still affordable, so the view can say when only the verdict is left. */
export function anyActionLeft(caseFile: HexatrapCaseDto, game: HexatrapGameDto | null): boolean {
  if (isClosed(game)) return false;
  const left = minutesLeft(caseFile, game);
  const asked = new Set(game?.asked.map((entry) => entry.questionId) ?? []);
  const revealed = new Set(game?.revealed.map((entry) => entry.clueId) ?? []);
  return caseFile.questions.some((question) => !asked.has(question.id) && caseFile.questionCost <= left)
    || caseFile.clues.some((clue) => !revealed.has(clue.id) && clue.cost <= left);
}

/** Cycles a notebook cell: unknown → excluded → confirmed → unknown. Confirming excludes the other options. */
export function cycleMark(notebook: Notebook, caseFile: HexatrapCaseDto, factId: string, optionId: string): Notebook {
  const current = notebook.facts[factId]?.[optionId] ?? "unknown";
  const next: Mark = current === "unknown" ? "no" : current === "no" ? "yes" : "unknown";
  const fact = caseFile.facts.find((candidate) => candidate.id === factId);
  const row: Record<string, Mark> = { ...(notebook.facts[factId] ?? {}) };
  row[optionId] = next;
  if (next === "yes" && fact) {
    for (const option of fact.options) if (option.id !== optionId && (row[option.id] ?? "unknown") === "unknown") row[option.id] = "no";
  }
  return { ...notebook, facts: { ...notebook.facts, [factId]: row } };
}

/** Cycles the suspicion on a character; since exactly one lies, the other gets the opposite role. */
export function cycleSuspicion(notebook: Notebook, id: HexatrapCharacterId): Notebook {
  const other: HexatrapCharacterId = id === "A" ? "B" : "A";
  const current = notebook.suspicion[id];
  const next: Suspicion = current === "unknown" ? "liar" : current === "liar" ? "truthful" : "unknown";
  const opposite: Suspicion = next === "liar" ? "truthful" : next === "truthful" ? "liar" : "unknown";
  return { ...notebook, suspicion: { ...notebook.suspicion, [id]: next, [other]: opposite } as Record<HexatrapCharacterId, Suspicion> };
}

export function outcomeTitle(outcome: HexatrapOutcome | null): string {
  if (outcome === "SOLVED") return "Caso risolto";
  if (outcome === "PARTIAL") return "Bugiardo smascherato, ma la ricostruzione non regge";
  if (outcome === "WRONG") return "Hai creduto alla persona sbagliata";
  return "Indagine in corso";
}

/** Keeps the newest server state when responses arrive out of order. */
export function newerGame(current: HexatrapGameDto | null | undefined, incoming: HexatrapGameDto): boolean {
  if (!current || current.puzzleDate !== incoming.puzzleDate) return true;
  if (current.status !== "IN_PROGRESS" && incoming.status === "IN_PROGRESS") return false;
  return incoming.sequence >= current.sequence;
}
