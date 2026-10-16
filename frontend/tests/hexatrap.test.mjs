import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";
import { hypotheses, retain, unlocked, answer } from "../src/features/hexatrap/hexatrapRules.ts";
const examples = JSON.parse(readFileSync(new URL("../../backend/src/test/resources/hexatrap-v1-cases.json", import.meta.url), "utf8"));

test("shared Java/TypeScript vectors agree on every retained hypothesis and unlock", () => {
  assert.equal(examples.length, 12);
  for (const example of examples) {
    let candidates = hypotheses(example.dossier);
    assert.equal(unlocked(candidates), false);
    for (const step of example.steps) {
      const actual = 2 * example.configuration + example.loyalAdvisor;
      assert.equal(answer(actual, step.advisor, step.seal), step.answer);
      candidates = retain(candidates, step.advisor, step.seal, step.answer);
      assert.deepEqual(candidates, step.remaining);
      assert.equal(unlocked(candidates), step.unlocked);
      assert.deepEqual(retain(candidates, step.advisor, step.seal, step.answer), candidates);
    }
    assert.equal(unlocked(candidates), true);
  }
  assert.equal(unlocked([]), false);
});

test("all dossier models leave every seal uncertain, and the two advisors always disagree", () => {
  for (const litCount of [2,4]) for (const relation of ["SAME", "IMPLIES", "AT_LEAST_ONE"])
    for (let firstSeal = 0; firstSeal < 6; firstSeal++) for (let secondSeal = 0; secondSeal < 6; secondSeal++) if (firstSeal !== secondSeal) {
      const candidates = hypotheses({litCount, relation, firstSeal, secondSeal});
      assert.ok(candidates.length > 0);
      assert.equal(unlocked(candidates), false);
      for (let seal = 0; seal < 6; seal++) {
        assert.equal(new Set(candidates.map(h => Boolean((h >> 1) & (1 << seal)))).size, 2);
        for (const h of candidates) assert.notEqual(answer(h, 0, seal), answer(h, 1, seal));
      }
    }
});
