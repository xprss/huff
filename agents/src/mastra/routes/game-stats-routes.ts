import { registerApiRoute } from '@mastra/core/server';
import { z } from 'zod';
import { gameStatsAgent } from '../agents/game-stats-agent';

const streamRequest = z.object({
  message: z.string().trim().min(1).max(4000),
  threadId: z.string().uuid(),
  resourceId: z.string().regex(/^[a-f0-9]{64}$/),
  users: z.array(z.string().regex(/^@[a-z0-9._-]{1,30}$/)).max(10).default([]),
});

const encoder = new TextEncoder();

export const gameStatsRoutes = [
  registerApiRoute('/internal/health', {
    method: 'GET',
    requiresAuth: false,
    handler: async (c) => c.json({ ready: true }),
  }),
  registerApiRoute('/internal/game-stats/stream', {
    method: 'POST',
    handler: async (c) => {
      const parsed = streamRequest.safeParse(await c.req.json().catch(() => null));
      if (!parsed.success) return c.json({ message: 'Richiesta non valida.' }, 400);

      const { message, threadId, resourceId, users } = parsed.data;
      const userContext = users.length > 0
        ? `\nUtenti selezionati dall'amministratore: ${users.join(', ')}. Usa queste menzioni come filtro prioritario per le statistiche individuali.`
        : '';

      try {
        const result = await gameStatsAgent.stream(`${message}${userContext}`, {
          memory: { thread: threadId, resource: resourceId },
          abortSignal: c.req.raw.signal,
        });

        const body = new ReadableStream<Uint8Array>({
          async start(controller) {
            try {
              for await (const text of result.textStream) {
                controller.enqueue(encoder.encode(`event: token\ndata: ${JSON.stringify({ text })}\n\n`));
              }
              controller.enqueue(encoder.encode('event: done\ndata: {}\n\n'));
              controller.close();
            } catch {
              controller.enqueue(encoder.encode(`event: error\ndata: ${JSON.stringify({ message: 'Generazione interrotta.' })}\n\n`));
              controller.close();
            }
          },
        });

        return c.newResponse(body, 200, {
          'Content-Type': 'text/event-stream; charset=utf-8',
          'Cache-Control': 'no-cache, no-transform',
          Connection: 'keep-alive',
          'X-Accel-Buffering': 'no',
        });
      } catch {
        return c.json({ message: 'Agente statistiche non disponibile.' }, 503);
      }
    },
  }),
];
