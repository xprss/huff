import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";
import { initialState, step, replay, hasWon, draftKey, parseDraft, compactSolution, destination } from "../src/features/hexaeco/hexaecoRules.ts";

const training = { walls: [], lightStart: 10, echoStart: 20, lightGoal: 6, echoGoal: 16 };
const moves = ["RIGHT", "UP", "WAIT", "WAIT"];
const examples = JSON.parse(readFileSync(new URL("../../backend/src/test/resources/hexaeco-cases.json", import.meta.url), "utf8"));

test("echo delays exactly two turns and waiting moves only the echo", () => {
  assert.deepEqual(replay(training, moves.slice(0, 2)), { light: 6, echo: 20, queue: ["RIGHT", "UP"] });
  assert.deepEqual(replay(training, moves.slice(0, 3)), { light: 6, echo: 21, queue: ["UP", "WAIT"] });
  assert.equal(hasWon(training, replay(training, moves)), true);
});

test("blocked commands consume a turn and walls do not block the other light", () => {
  const board = { walls: [5], lightStart: 10, echoStart: 20, lightGoal: 11, echoGoal: 16 };
  const first = step(board, initialState(board), "UP");
  assert.deepEqual(first, { light: 10, echo: 20, queue: ["WAIT", "UP"] });
  assert.equal(hasWon(board, replay(board, ["UP", "RIGHT", "WAIT", "WAIT"])), true);
  assert.deepEqual(destination(board, 10, "UP"), { position: 10, blocked: true, wall: 5 });
  assert.deepEqual(destination(board, 10, "LEFT"), { position: 10, blocked: true, wall: null });
  assert.equal(destination(board, 4, "RIGHT").position, 4);
  assert.equal(destination(board, 24, "DOWN").position, 24);
});

test("overlap, crossing and leaving a goal are legal", () => {
  const state = { light: 10, echo: 12, queue: ["LEFT", "WAIT"] };
  const together = step(training, state, "RIGHT");
  assert.equal(together.light, together.echo);
  const crossed = step(training, { light: 10, echo: 11, queue: ["LEFT", "WAIT"] }, "RIGHT");
  assert.equal(crossed.light, 11);
  assert.equal(crossed.echo, 10);
  assert.equal(step(training, { light: training.lightGoal, echo: 20, queue: ["WAIT", "WAIT"] }, "RIGHT").light, 7);
});

test("undo and restart restore the complete queue as well as positions", () => {
  const before = replay(training, moves.slice(0, 2));
  const history = [...moves.slice(0, 2), "DOWN"];
  assert.deepEqual(replay(training, history.slice(0, -1)), before);
  assert.deepEqual(replay(training, []), initialState(training));
});

test("all catalogue examples agree with the Java fixtures and terminate at the first win", () => {
  assert.equal(examples.length, 32);
  for (const example of examples) {
    let state = initialState(example.board);
    for (const command of example.moves) {
      assert.equal(hasWon(example.board, state), false);
      state = step(example.board, state, command);
    }
    assert.equal(hasWon(example.board, state), true);
  }
});

test("drafts are isolated by account, date and rules, and restored by replay", () => {
  const today = { puzzleDate: "2026-09-15", rulesVersion: 1 };
  assert.notEqual(draftKey("alice", today), draftKey("bob", today));
  assert.notEqual(draftKey("alice", today), draftKey("alice", { ...today, puzzleDate: "2026-09-16" }));
  assert.notEqual(draftKey("alice", today), draftKey("alice", { ...today, rulesVersion: 2 }));
  const raw = JSON.stringify({ board: JSON.stringify(training), moves });
  assert.deepEqual(parseDraft(raw, training), moves);
  assert.deepEqual(replay(training, parseDraft(raw, training)), replay(training, moves));
  assert.deepEqual(parseDraft(raw, { ...training, walls: [1] }), []);
  assert.deepEqual(parseDraft("not-json", training), []);
  assert.deepEqual(parseDraft(JSON.stringify({ board: JSON.stringify(training), moves: ["FLY"] }), training), []);
  assert.deepEqual(parseDraft(JSON.stringify({ board: JSON.stringify(training), moves: [...moves, "WAIT"] }), training), []);
});

test("transport removes cycles without changing either light or the delayed commands", () => {
  assert.deepEqual(compactSolution(training, ["RIGHT", "LEFT"]), ["RIGHT", "LEFT"]);
  assert.deepEqual(compactSolution(training, ["RIGHT", "LEFT", "WAIT", "WAIT", ...moves]), moves);
  for (const example of examples) {
    const long = [...Array(21000).fill("WAIT"), ...example.moves];
    const compact = compactSolution(example.board, long);
    assert.ok(compact.length < 20000);
    assert.deepEqual(replay(example.board, compact), replay(example.board, long));
    assert.equal(hasWon(example.board, replay(example.board, compact)), true);
    assert.equal(long.length, 21000 + example.moves.length);
  }
});
