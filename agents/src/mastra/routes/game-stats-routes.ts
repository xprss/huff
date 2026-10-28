import { registerApiRoute } from '@mastra/core/server';
import { RequestContext } from '@mastra/core/request-context';
import { z } from 'zod';
import { gameStatsAgent } from '../agents/game-stats-agent';

const streamRequest = z.object({
  message: z.string().trim().min(1).max(4000),
  threadId: z.string().uuid(),
  resourceId: z.string().regex(/^[a-f0-9]{64}$/),
  users: z.array(z.string().regex(/^@[a-z0-9._-]{1,30}$/)).max(10).default([]),
  games: z.array(z.enum(['hexaword', 'hexahack', 'hexasky', 'hexaflow', 'hexastar', 'hexaeco', 'hexatrap'])).max(7).default([]),
  from: z.string().regex(/^\d{4}-\d{2}-\d{2}$/).optional(),
  to: z.string().regex(/^\d{4}-\d{2}-\d{2}$/).optional(),
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

      const { message, threadId, resourceId, users, games, from, to } = parsed.data;
      const filters = { users, games, from: from ?? null, to: to ?? null };
      const hasFilters = users.length > 0 || games.length > 0 || from !== undefined || to !== undefined;
      const filterContext = hasFilters
        ? `\n[ADMIN_FILTERS ${JSON.stringify(filters)}]\nPer questa richiesta applica i filtri selezionati: utenti ${users.join(', ') || 'tutti'}, giochi ${games.join(', ') || 'tutti'}, data iniziale ${from || 'senza limite'}, data finale ${to || 'senza limite'}. Non includere risultati fuori da questo ambito.`
        : '';
      const requestContext = new RequestContext();
      requestContext.setRaw('gameStatsFilters', filters);

      try {
        const result = await gameStatsAgent.stream(`${message}${filterContext}`, {
          memory: { thread: threadId, resource: resourceId },
          requestContext,
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
  registerApiRoute('/internal/game-stats/threads', {
    method: 'GET',
    handler: async (c) => {
      const resourceId = c.req.query('resourceId');
      if (!resourceId || !/^[a-f0-9]{64}$/.test(resourceId)) return c.json({ message: 'Richiesta non valida.' }, 400);
      const memory = await gameStatsAgent.getMemory();
      if (!memory) return c.json({ threads: [] });
      const result = await memory.listThreads({
        filter: { resourceId },
        orderBy: { field: 'updatedAt', direction: 'DESC' },
        page: 0,
        perPage: 100,
      });
      const threads = await Promise.all(result.threads.map(async (thread) => {
        const [history, opening] = await Promise.all([
          memory.recall({ threadId: thread.id, resourceId, page: 0, perPage: 1, orderBy: { field: 'createdAt', direction: 'DESC' }, includeTotal: false }),
          memory.recall({ threadId: thread.id, resourceId, page: 0, perPage: 1, orderBy: { field: 'createdAt', direction: 'ASC' }, includeTotal: false }),
        ]);
        const latest = history.messages.find((item) => item.role === 'user' || item.role === 'assistant');
        const initial = opening.messages.find((item) => item.role === 'user');
        const latestText = latest ? messageText(latest.content) : '';
        const preview = (latest?.role === 'user' ? visibleUserMessage(latestText) : latestText).trim();
        const initialPrompt = initial ? visibleUserMessage(messageText(initial.content)).trim() : '';
        const title = thread.title && thread.title !== 'New Thread' ? thread.title : initialPrompt.slice(0, 90) || 'Nuova conversazione';
        return {
          id: thread.id,
          title,
          updatedAt: thread.updatedAt,
          preview: preview.slice(0, 140),
        };
      }));
      return c.json({ threads });
    },
  }),
  registerApiRoute('/internal/game-stats/threads/:threadId', {
    method: 'GET',
    handler: async (c) => {
      const resourceId = c.req.query('resourceId');
      const threadId = c.req.param('threadId');
      if (!resourceId || !/^[a-f0-9]{64}$/.test(resourceId) || !z.string().uuid().safeParse(threadId).success) {
        return c.json({ message: 'Richiesta non valida.' }, 400);
      }
      const memory = await gameStatsAgent.getMemory();
      const thread = await memory?.getThreadById({ threadId, resourceId });
      if (!memory || !thread) return c.json({ message: 'Conversazione non trovata.' }, 404);
      const result = await memory.recall({
        threadId,
        resourceId,
        page: 0,
        perPage: 500,
        orderBy: { field: 'createdAt', direction: 'ASC' },
        includeTotal: false,
      });
      const messages = result.messages
        .filter((item) => item.role === 'user' || item.role === 'assistant')
        .map((item) => {
          const rawContent = messageText(item.content);
          const metadata = item.role === 'user' ? userMessageFilters(rawContent) : null;
          return {
            role: item.role,
            content: metadata ? rawContent.slice(0, metadata.start).trimEnd() : rawContent,
            ...(metadata ? { filters: metadata.filters } : {}),
            createdAt: item.createdAt,
          };
        })
        .filter((item) => item.content.trim());
      return c.json({ id: thread.id, title: thread.title || 'Nuova conversazione', messages });
    },
  }),
  registerApiRoute('/internal/game-stats/threads/:threadId', {
    method: 'DELETE',
    handler: async (c) => {
      const resourceId = c.req.query('resourceId');
      const threadId = c.req.param('threadId');
      if (!resourceId || !/^[a-f0-9]{64}$/.test(resourceId) || !z.string().uuid().safeParse(threadId).success) {
        return c.json({ message: 'Richiesta non valida.' }, 400);
      }
      const memory = await gameStatsAgent.getMemory();
      const thread = await memory?.getThreadById({ threadId, resourceId });
      if (!memory || !thread) return c.json({ message: 'Conversazione non trovata.' }, 404);
      await memory.deleteThread(threadId);
      return c.json({ deleted: true });
    },
  }),
];

function messageText(content: unknown): string {
  if (typeof content === 'string') return content;
  if (!content || typeof content !== 'object') return '';
  const value = content as { text?: unknown; parts?: unknown; content?: unknown };
  if (typeof value.text === 'string') return value.text;
  const parts = Array.isArray(value.parts) ? value.parts : Array.isArray(value.content) ? value.content : [];
  return parts.map((part) => {
    if (typeof part === 'string') return part;
    if (!part || typeof part !== 'object') return '';
    const item = part as { type?: unknown; text?: unknown };
    return (item.type === 'text' || item.type === undefined) && typeof item.text === 'string' ? item.text : '';
  }).filter(Boolean).join('\n');
}

function userMessageFilters(content: string): { start: number; filters: { users: string[]; games: string[]; from: string | null; to: string | null } } | null {
  const marker = content.lastIndexOf('\n[ADMIN_FILTERS ');
  const closing = content.indexOf(']\nPer questa richiesta applica i filtri selezionati:', marker);
  if (marker < 0 || closing < 0) return null;
  try {
    const filters = JSON.parse(content.slice(marker + '\n[ADMIN_FILTERS '.length, closing)) as Record<string, unknown>;
    if (!Array.isArray(filters.users) || !Array.isArray(filters.games)) return null;
    return {
      start: marker,
      filters: {
        users: filters.users.filter((value): value is string => typeof value === 'string'),
        games: filters.games.filter((value): value is string => typeof value === 'string'),
        from: typeof filters.from === 'string' ? filters.from : null,
        to: typeof filters.to === 'string' ? filters.to : null,
      },
    };
  } catch {
    return null;
  }
}

function visibleUserMessage(content: string): string {
  const metadata = userMessageFilters(content);
  return metadata ? content.slice(0, metadata.start).trimEnd() : content;
}
