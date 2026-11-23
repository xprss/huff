import { registerApiRoute } from '@mastra/core/server';
import { hexatrapAuthorAgent } from '../agents/hexatrap-author-agent';
import { hexatrapAuditorAgent } from '../agents/hexatrap-auditor-agent';
import {
  hexatrapAuditRequestSchema,
  hexatrapAuditSchema,
  hexatrapDraftRequestSchema,
  hexatrapDraftSchema,
  toCaseDefinition,
} from '../hexatrap/hexatrap-schemas';

const MODEL = 'openai/gpt-6-luna';

/*
 * Internal routes used only by the Java backend, which validates every draft exhaustively and
 * drives the repair loop. Nothing here is trusted for logic.
 */
export const hexatrapRoutes = [
  registerApiRoute('/internal/hexatrap/draft', {
    method: 'POST',
    handler: async (c) => {
      const parsed = hexatrapDraftRequestSchema.safeParse(await c.req.json().catch(() => null));
      if (!parsed.success) return c.json({ message: 'Richiesta non valida.' }, 400);

      const { brief, previousDraft, feedback } = parsed.data;
      const prompt = [
        `Scrivi il caso Hexatrap del ${brief.date}.`,
        `Difficoltà: ${brief.difficulty} su 5. Formato del giorno: ${brief.format}.`,
        `Budget del giocatore: ${brief.budget} minuti. Ogni domanda costa ${brief.questionCost} minuti; i reperti possono costare ${brief.clueCosts.join(', ')} minuti.`,
        `Icone ammesse per i personaggi: ${brief.icons.join(', ')}.`,
        brief.avoid.titles.length ? `Titoli recenti da non ripetere né imitare: ${brief.avoid.titles.join(' | ')}.` : '',
        brief.avoid.names.length ? `Nomi di personaggi recenti da non riusare: ${brief.avoid.names.join(', ')}.` : '',
        brief.avoid.formats.length ? `Formati dei giorni precedenti: ${brief.avoid.formats.join(', ')}.` : '',
        previousDraft ? `Bozza precedente da correggere:\n${JSON.stringify(previousDraft)}` : '',
        feedback.length ? `Problemi trovati dal verificatore, da correggere tutti:\n- ${feedback.join('\n- ')}` : '',
      ].filter(Boolean).join('\n\n');

      try {
        const result = await hexatrapAuthorAgent.generate(prompt, {
          structuredOutput: { schema: hexatrapDraftSchema },
          abortSignal: c.req.raw.signal,
        });
        const draft = result.object;
        if (!draft) return c.json({ message: 'La bozza generata è vuota.' }, 502);
        return c.json({ draft: toCaseDefinition(draft), model: MODEL });
      } catch {
        return c.json({ message: 'Generazione del caso non riuscita.' }, 503);
      }
    },
  }),
  registerApiRoute('/internal/hexatrap/audit', {
    method: 'POST',
    handler: async (c) => {
      const parsed = hexatrapAuditRequestSchema.safeParse(await c.req.json().catch(() => null));
      if (!parsed.success) return c.json({ message: 'Richiesta non valida.' }, 400);

      try {
        const result = await hexatrapAuditorAgent.generate(
          `Traduci in formule questi testi.\n${JSON.stringify(parsed.data)}`,
          { structuredOutput: { schema: hexatrapAuditSchema }, abortSignal: c.req.raw.signal },
        );
        const audit = result.object;
        if (!audit || audit.items.length === 0) return c.json({ message: 'La revisione generata è vuota.' }, 502);
        return c.json({ items: audit.items, model: MODEL });
      } catch {
        return c.json({ message: 'Revisione del caso non riuscita.' }, 503);
      }
    },
  }),
];
