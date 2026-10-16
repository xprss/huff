import type { HexatrapDossierDto } from "../../types";

// Used by the uncounted tutorial and shared contract vectors. The server authorizes play.
export function hypotheses(dossier: HexatrapDossierDto): number[] {
  const result: number[] = [];
  for (let mask = 0; mask < 64; mask++) {
    const count = mask.toString(2).replace(/0/g, "").length;
    const a = Boolean(mask & (1 << dossier.firstSeal)), b = Boolean(mask & (1 << dossier.secondSeal));
    const relation = dossier.relation === "SAME" ? a === b : dossier.relation === "IMPLIES" ? !a || b : a || b;
    if (count === dossier.litCount && relation) result.push(mask * 2, mask * 2 + 1);
  }
  return result;
}
export function answer(hypothesis: number, advisor: number, seal: number): boolean {
  return Boolean((hypothesis >> 1) & (1 << seal)) === (hypothesis % 2 === advisor);
}
export function retain(candidates: readonly number[], advisor: number, seal: number, response: boolean): number[] {
  return candidates.filter(h => answer(h, advisor, seal) === response);
}
export function unlocked(candidates: readonly number[]): boolean {
  return candidates.length > 0 && candidates.every(h => h % 2 === candidates[0] % 2);
}
