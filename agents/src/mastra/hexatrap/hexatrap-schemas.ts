import { z } from 'zod';

/*
 * Schemas of the Hexatrap authoring and audit routes. Every field is required (nullable where
 * absent) so the model can use the provider's strict structured output. Counts, lengths and the
 * logic of the case are enforced by the Java validator, which sends specific feedback back.
 */

const id = z.string().describe('Identificatore: minuscole, cifre e _, al massimo 24 caratteri.');
const formula = z.string().describe('Formula nel linguaggio del caso.');
const character = z.enum(['A', 'B']);

export const hexatrapOptionSchema = z.object({
  id,
  label: z.string().describe('Etichetta breve per il taccuino, al massimo 40 caratteri.'),
  affirm: z.string().describe('Frase atomica affermativa, minuscola iniziale, senza punto finale.'),
  deny: z.string().describe('La stessa frase negata, minuscola iniziale, senza punto finale.'),
});

export const hexatrapDraftSchema = z.object({
  title: z.string(),
  intro: z.string(),
  epilogue: z.string(),
  format: z.string(),
  characters: z.array(z.object({ id: character, name: z.string(), role: z.string(), icon: z.string() })),
  facts: z.array(z.object({ id, label: z.string(), options: z.array(hexatrapOptionSchema) })),
  certainties: z.array(z.object({ id, text: z.string(), formula })),
  statements: z.array(z.object({ id, speaker: character, text: z.string(), formula })),
  questions: z.array(z.object({ id, to: character, text: z.string(), formula })),
  clues: z.array(z.object({ id, title: z.string(), text: z.string(), cost: z.number().int(), formula })),
  verdictFacts: z.array(id),
  solution: z.object({
    liar: character,
    facts: z.array(z.object({ fact: id, option: id })),
  }),
});

export type HexatrapDraft = z.infer<typeof hexatrapDraftSchema>;

/** The draft as the Java backend stores it: the solution's facts become a map. */
export function toCaseDefinition(draft: HexatrapDraft) {
  return {
    ...draft,
    solution: {
      liar: draft.solution.liar,
      facts: Object.fromEntries(draft.solution.facts.map((entry) => [entry.fact, entry.option])),
    },
  };
}

export const hexatrapDraftRequestSchema = z.object({
  brief: z.object({
    date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/),
    difficulty: z.number().int().min(1).max(5),
    format: z.string().min(1).max(30),
    budget: z.number().int().positive(),
    questionCost: z.number().int().positive(),
    clueCosts: z.array(z.number().int().positive()).min(1),
    icons: z.array(z.string()).min(2),
    avoid: z.object({
      titles: z.array(z.string()).max(60).default([]),
      names: z.array(z.string()).max(120).default([]),
      formats: z.array(z.string()).max(60).default([]),
    }),
  }),
  previousDraft: z.unknown().nullish(),
  feedback: z.array(z.string().max(2000)).max(40).default([]),
});

export const hexatrapAuditRequestSchema = z.object({
  facts: z.array(z.object({ id, label: z.string(), options: z.array(hexatrapOptionSchema) })).min(1).max(8),
  characters: z.array(z.object({ id: character, name: z.string() })).length(2),
  items: z.array(z.object({
    id,
    kind: z.enum(['CERTAINTY', 'STATEMENT', 'QUESTION', 'CLUE']),
    speaker: character.nullable(),
    text: z.string(),
  })).min(1).max(40),
});

export const hexatrapAuditSchema = z.object({
  items: z.array(z.object({ id, formula })),
});
