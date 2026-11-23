import assert from "node:assert/strict";
import test from "node:test";
import {
  EMPTY_NOTEBOOK, EMPTY_VERDICT, anyActionLeft, canAfford, characterName, cycleMark, cycleSuspicion, difficultyLabel,
  isClosed, minutesLeft, newerGame, optionLabel, outcomeTitle, verdictFacts, verdictReady
} from "../src/features/hexatrap/hexatrapModel.ts";

const caseFile = {
  title: "La lanterna spenta", format: "sabotaggio", difficulty: 3, intro: "…", budget: 60, questionCost: 5,
  characters: [{ id: "A", name: "Ettore", role: "guardiano", icon: "lamp" }, { id: "B", name: "Livia", role: "cartografa", icon: "map" }],
  facts: [
    { id: "chi", label: "Chi?", verdict: true, options: [{ id: "ettore", label: "Ettore" }, { id: "livia", label: "Livia" }, { id: "mozzo", label: "Il mozzo" }] },
    { id: "ora", label: "Quando?", verdict: false, options: [{ id: "nove", label: "Alle nove" }, { id: "dieci", label: "Alle dieci" }] }
  ],
  certainties: [], statements: [],
  questions: [{ id: "q1", to: "A", text: "?", record: "?" }, { id: "q2", to: "B", text: "?", record: "?" }],
  clues: [{ id: "c1", title: "Cenere", cost: 16 }]
};
const game = (overrides = {}) => ({
  puzzleDate: "2026-11-24", rulesVersion: 2, status: "IN_PROGRESS", outcome: null, minutesUsed: 0, minutesLeft: 60, sequence: 0,
  asked: [], revealed: [], verdict: null, points: 0, reveal: null, completedAt: null, ...overrides
});

test("tracks the investigation budget", () => {
  assert.equal(minutesLeft(caseFile, null), 60);
  assert.equal(minutesLeft(caseFile, game({ minutesUsed: 47 })), 13);
  assert.equal(canAfford(caseFile, game({ minutesUsed: 47 }), 12), true);
  assert.equal(canAfford(caseFile, game({ minutesUsed: 47 }), 16), false);
  assert.equal(canAfford(caseFile, game({ status: "WON", outcome: "SOLVED" }), 5), false);
  assert.equal(isClosed(null), false);
  assert.equal(isClosed(game({ status: "LOST" })), true);
});

test("knows when only the verdict is left", () => {
  assert.equal(anyActionLeft(caseFile, game()), true);
  const asked = [{ sequence: 1, questionId: "q1", to: "A", answer: true, readings: { ifALies: "", ifBLies: "" } }];
  assert.equal(anyActionLeft(caseFile, game({ minutesUsed: 55, asked })), true);
  assert.equal(anyActionLeft(caseFile, game({ minutesUsed: 56, asked })), false);
  assert.equal(anyActionLeft(caseFile, game({ status: "WON" })), false);
});

test("requires the liar and every verdict fact", () => {
  assert.deepEqual(verdictFacts(caseFile).map((fact) => fact.id), ["chi"]);
  assert.equal(verdictReady(caseFile, EMPTY_VERDICT), false);
  assert.equal(verdictReady(caseFile, { liar: "B", facts: {} }), false);
  assert.equal(verdictReady(caseFile, { liar: null, facts: { chi: "mozzo" } }), false);
  assert.equal(verdictReady(caseFile, { liar: "B", facts: { chi: "tetto" } }), false);
  assert.equal(verdictReady(caseFile, { liar: "B", facts: { chi: "mozzo" } }), true);
});

test("cycles notebook marks and confirming one option excludes the others", () => {
  let notebook = cycleMark(EMPTY_NOTEBOOK, caseFile, "chi", "ettore");
  assert.equal(notebook.facts.chi.ettore, "no");
  notebook = cycleMark(notebook, caseFile, "chi", "mozzo");
  notebook = cycleMark(notebook, caseFile, "chi", "mozzo");
  assert.deepEqual(notebook.facts.chi, { ettore: "no", mozzo: "yes", livia: "no" });
  notebook = cycleMark(notebook, caseFile, "chi", "mozzo");
  assert.equal(notebook.facts.chi.mozzo, "unknown");
  assert.equal(EMPTY_NOTEBOOK.facts.chi, undefined);
});

test("suspecting one character gives the other the opposite role", () => {
  let notebook = cycleSuspicion(EMPTY_NOTEBOOK, "A");
  assert.deepEqual(notebook.suspicion, { A: "liar", B: "truthful" });
  notebook = cycleSuspicion(notebook, "A");
  assert.deepEqual(notebook.suspicion, { A: "truthful", B: "liar" });
  notebook = cycleSuspicion(notebook, "A");
  assert.deepEqual(notebook.suspicion, { A: "unknown", B: "unknown" });
});

test("never lets an older response overwrite a newer state", () => {
  assert.equal(newerGame(null, game({ sequence: 1 })), true);
  assert.equal(newerGame(game({ sequence: 3 }), game({ sequence: 2 })), false);
  assert.equal(newerGame(game({ sequence: 3 }), game({ sequence: 4 })), true);
  assert.equal(newerGame(game({ sequence: 4, status: "WON" }), game({ sequence: 4 })), false);
  assert.equal(newerGame(game({ puzzleDate: "2026-11-23", sequence: 9 }), game({ sequence: 1 })), true);
});

test("labels characters, options, difficulty and outcomes in Italian", () => {
  assert.equal(characterName(caseFile, "B"), "Livia");
  assert.equal(optionLabel(caseFile, "chi", "mozzo"), "Il mozzo");
  assert.equal(optionLabel(caseFile, "chi", undefined), "—");
  assert.deepEqual([1, 3, 5, 9].map(difficultyLabel), ["Lieve", "Media", "Diabolica", "Diabolica"]);
  assert.equal(outcomeTitle("SOLVED"), "Caso risolto");
  assert.match(outcomeTitle("PARTIAL"), /ricostruzione/);
  assert.match(outcomeTitle("WRONG"), /sbagliata/);
});
