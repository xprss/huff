import path from 'node:path';
import { MCPClient } from '@mastra/mcp';
import { createTool } from '@mastra/core/tools';
import type { RequestContext } from '@mastra/core/request-context';
import { z } from 'zod';

const games = ['all', 'hexaword', 'hexahack', 'hexasky', 'hexaflow', 'hexastar', 'hexaeco', 'hexatrap'] as const;
const singleGames = games.slice(1);
type Game = typeof games[number];
type StatsFilters = { users: string[]; games: string[]; from: string | null; to: string | null };
type ToolContext = { abortSignal?: AbortSignal; requestContext: RequestContext };

function postgresConnectionString() {
  const url = new URL('postgresql://localhost');
  url.username = process.env.POSTGRES_MCP_USER || 'huff_stats_agent';
  url.password = process.env.POSTGRES_MCP_PASSWORD || '';
  url.hostname = process.env.POSTGRES_MCP_HOST || process.env.POSTGRES_HOST || 'localhost';
  url.port = process.env.POSTGRES_MCP_PORT || process.env.POSTGRES_PORT || '5432';
  url.pathname = `/${process.env.POSTGRES_MCP_DATABASE || process.env.POSTGRES_DB || 'huff_hexaquot'}`;
  return url.toString();
}

const mcpClient = new MCPClient({
  id: 'hexaquot-game-statistics-postgres',
  servers: {
    postgres: {
      command: process.execPath,
      args: [path.resolve('node_modules/@microsoft/postgres-mcp/bin/postgres-mcp.js'), 'run'],
      env: {
        POSTGRES_MCP_CONNECTION_STRING: postgresConnectionString(),
        POSTGRES_MCP_DISABLE_CWD_ACCESS: '1',
        POSTGRES_MCP_QUERY_TIMEOUT_MS: '30000',
      },
      inheritDefaultEnv: false,
    },
  },
  timeout: 30_000,
  clientInfo: { name: 'hexaquot-game-statistics', version: '1.0.0' },
});

type McpTool = { execute(input: unknown, context?: unknown): Promise<unknown> };
type Toolsets = Record<string, Record<string, McpTool>>;
let toolsetsPromise: Promise<Toolsets> | undefined;
let connectionIdPromise: Promise<string> | undefined;

const gameRows: Record<Exclude<Game, 'all'>, string> = {
  hexaword: `SELECT 'hexaword' AS game, u.id AS user_id, u.nickname, g.puzzle_date AS played_on, g.created_at AS activity_at, g.status,
       (g.status IN ('WON', 'LOST')) AS completed,
       (g.status = 'WON') AS won, (g.status = 'LOST') AS lost
    FROM hexaword_games g JOIN users u ON u.id = g.user_id`,
  hexahack: `SELECT 'hexahack' AS game, u.id AS user_id, u.nickname, g.puzzle_date AS played_on, g.created_at AS activity_at, g.status,
       (g.status = 'COMPLETED') AS completed,
       (g.status = 'COMPLETED') AS won, false AS lost
    FROM hexahack_games g JOIN users u ON u.id = g.user_id`,
  hexasky: `SELECT 'hexasky' AS game, u.id AS user_id, u.nickname, g.puzzle_date AS played_on, g.created_at AS activity_at, g.status,
       (g.status IN ('WON', 'LOST')) AS completed,
       (g.status = 'WON') AS won, (g.status = 'LOST') AS lost
    FROM hexasky_games g JOIN users u ON u.id = g.user_id`,
  hexaflow: `SELECT 'hexaflow' AS game, u.id AS user_id, u.nickname, g.puzzle_date AS played_on, g.created_at AS activity_at, g.status,
       (g.status = 'COMPLETED') AS completed,
       (g.status = 'COMPLETED') AS won, false AS lost
    FROM hexaflow_games g JOIN users u ON u.id = g.user_id`,
  hexastar: `SELECT 'hexastar' AS game, u.id AS user_id, u.nickname, g.puzzle_date AS played_on, g.created_at AS activity_at, g.status,
       (g.status IN ('WON', 'LOST')) AS completed,
       (g.status = 'WON') AS won, (g.status = 'LOST') AS lost
    FROM hexastar_games g JOIN users u ON u.id = g.user_id`,
  hexaeco: `SELECT 'hexaeco' AS game, u.id AS user_id, u.nickname, g.puzzle_date AS played_on, g.created_at AS activity_at, g.status,
       true AS completed, true AS won, false AS lost
    FROM hexaeco_games g JOIN users u ON u.id = g.user_id`,
  hexatrap: `SELECT 'hexatrap' AS game, u.id AS user_id, u.nickname, g.puzzle_date AS played_on, g.created_at AS activity_at, g.status,
       (g.status = 'WON') AS completed,
       (g.status = 'WON') AS won, false AS lost
    FROM hexatrap_games g JOIN users u ON u.id = g.user_id`,
};

function filtersFrom(context: ToolContext): StatsFilters | undefined {
  const value = context.requestContext.getRaw('gameStatsFilters');
  if (!value || typeof value !== 'object') return undefined;
  const filters = value as Partial<StatsFilters>;
  return {
    users: Array.isArray(filters.users) ? filters.users : [],
    games: Array.isArray(filters.games) ? filters.games : [],
    from: typeof filters.from === 'string' ? filters.from : null,
    to: typeof filters.to === 'string' ? filters.to : null,
  };
}

function unionRows(game: Game, filters?: StatsFilters) {
  const selected = filters?.games.length
    ? singleGames.filter((name) => filters.games.includes(name))
    : game === 'all' ? singleGames : [game];
  return selected.map((name) => gameRows[name as Exclude<Game, 'all'>]).join('\nUNION ALL\n');
}

function dateRange(from?: string, to?: string, filters?: StatsFilters) {
  const clauses = [];
  const hasSelectedDates = filters?.from !== null && filters?.from !== undefined || filters?.to !== null && filters?.to !== undefined;
  const selectedFrom = hasSelectedDates ? filters?.from ?? undefined : from;
  const selectedTo = hasSelectedDates ? filters?.to ?? undefined : to;
  if (selectedFrom) clauses.push(`played_on >= '${selectedFrom}'`);
  if (selectedTo) clauses.push(`played_on <= '${selectedTo}'`);
  if (filters?.users.length) clauses.push(`nickname IN (${filters.users.map(sqlLiteral).join(', ')})`);
  return clauses.length ? `WHERE ${clauses.join(' AND ')}` : '';
}

function sqlLiteral(value: string) {
  return `'${value.replaceAll("'", "''")}'`;
}

function gameTimezone() {
  const configured = process.env.GAME_TIMEZONE || 'Europe/Rome';
  try {
    new Intl.DateTimeFormat('en', { timeZone: configured });
    return configured;
  } catch {
    return 'Europe/Rome';
  }
}

function hexawordFilters(filters: StatsFilters | undefined, options: {
  nickname?: string;
  from?: string;
  to?: string;
} = {}) {
  if (filters?.games.length && !filters.games.includes('hexaword')) return null;

  const clauses: string[] = [];
  const selectedFrom = filters?.from ?? options.from;
  const selectedTo = filters?.to ?? options.to;
  if (selectedFrom) clauses.push(`g.puzzle_date >= ${sqlLiteral(selectedFrom)}`);
  if (selectedTo) clauses.push(`g.puzzle_date <= ${sqlLiteral(selectedTo)}`);
  if (filters?.users.length) {
    clauses.push(`u.nickname IN (${filters.users.map(sqlLiteral).join(', ')})`);
  }
  if (options.nickname) clauses.push(`u.nickname = ${sqlLiteral(options.nickname)}`);

  return clauses.length ? `WHERE ${clauses.join(' AND ')}` : '';
}

async function getToolsets(): Promise<Toolsets> {
  if (!process.env.POSTGRES_MCP_PASSWORD) {
    throw new Error('POSTGRES_MCP_PASSWORD non configurata.');
  }
  toolsetsPromise ??= mcpClient.listToolsets() as Promise<Toolsets>;
  return toolsetsPromise;
}

async function getConnectionId(): Promise<string> {
  connectionIdPromise ??= (async () => {
    const tools = (await getToolsets()).postgres;
    if (!tools) throw new Error('Server MCP PostgreSQL non disponibile.');

    const profiles = await tools.postgres_mcp_list_connection_profiles.execute({}) as { profiles?: { profileId: string; profileName: string }[] };
    const profile = profiles.profiles?.find((item) => item.profileName.startsWith('default'));
    if (!profile) throw new Error('Profilo PostgreSQL read-only non trovato.');

    const connection = await tools.postgres_mcp_connect.execute({ profileId: profile.profileId }) as { connectionId?: string };
    if (!connection.connectionId) throw new Error('Connessione MCP PostgreSQL non riuscita.');
    return connection.connectionId;
  })().catch((error) => {
    connectionIdPromise = undefined;
    throw error;
  });
  return connectionIdPromise;
}

async function runQuery(query: string, abortSignal?: AbortSignal) {
  const queryTool = (await getToolsets()).postgres?.postgres_mcp_query;
  if (!queryTool) throw new Error('Tool di query PostgreSQL non disponibile.');
  const result = await queryTool.execute({
    connectionId: await getConnectionId(),
    query,
    maxResultRows: 100,
    validationQueries: [],
  }, { abortSignal }) as Record<string, unknown>;

  if (Array.isArray(result)) return result;
  if (Array.isArray(result.rows)) return result.rows;
  if (result.structuredContent && typeof result.structuredContent === 'object') {
    const structured = result.structuredContent as Record<string, unknown>;
    if (Array.isArray(structured.rows)) return structured.rows;
  }
  return result;
}

const gameSchema = z.enum(games).default('all');
const dateSchema = {
  from: z.string().regex(/^\d{4}-\d{2}-\d{2}$/).optional(),
  to: z.string().regex(/^\d{4}-\d{2}-\d{2}$/).optional(),
};
const outputSchema = z.unknown();

export const gameOverviewTool = createTool({
  id: 'game_overview',
  description: 'Conteggi di partite iniziate, completate, vinte e perse per gioco e periodo.',
  inputSchema: z.object({ game: gameSchema, ...dateSchema }),
  outputSchema,
  execute: async ({ game, from, to }, context) => {
    const filters = filtersFrom(context);
    return runQuery(`WITH games AS (${unionRows(game, filters)})
      SELECT game, COUNT(*) AS started,
        COUNT(*) FILTER (WHERE completed) AS completed,
        COUNT(*) FILTER (WHERE won) AS won,
        COUNT(*) FILTER (WHERE lost) AS lost,
        ROUND(100.0 * COUNT(*) FILTER (WHERE won) / NULLIF(COUNT(*) FILTER (WHERE completed), 0), 1) AS win_percentage
      FROM games ${dateRange(from, to, filters)} GROUP BY game ORDER BY game`, context.abortSignal);
  },
});

export const gameLeaderboardTool = createTool({
  id: 'game_leaderboard',
  description: 'Classifica dei giocatori per vittorie nel gioco e periodo selezionati.',
  inputSchema: z.object({ game: gameSchema, limit: z.number().int().min(1).max(50).default(10), ...dateSchema }),
  outputSchema,
  execute: async ({ game, limit, from, to }, context) => {
    const filters = filtersFrom(context);
    const scopeIsSelected = Boolean(filters?.games.length);
    const groupBy = scopeIsSelected ? 'game, nickname' : game === 'all' ? 'nickname' : 'game, nickname';
    const selectedGame = scopeIsSelected ? 'game' : game === 'all' ? "'all'" : 'game';
    return runQuery(`WITH games AS (${unionRows(game, filters)})
      SELECT ${selectedGame} AS game, nickname,
        COUNT(*) FILTER (WHERE won) AS wins,
        COUNT(*) FILTER (WHERE completed) AS completed,
        ROUND(100.0 * COUNT(*) FILTER (WHERE won) / NULLIF(COUNT(*) FILTER (WHERE completed), 0), 1) AS win_percentage
      FROM games ${dateRange(from, to, filters)}
      GROUP BY ${groupBy} ORDER BY wins DESC, completed DESC, nickname ASC LIMIT ${limit}`, context.abortSignal);
  },
});

export const playerGameStatsTool = createTool({
  id: 'player_game_stats',
  description: 'Statistiche aggregate di un nickname, per gioco e periodo selezionati.',
  inputSchema: z.object({ nickname: z.string().regex(/^@[a-z0-9._-]{1,30}$/), game: gameSchema, ...dateSchema }),
  outputSchema,
  execute: async ({ nickname, game, from, to }, context) => {
    const filters = filtersFrom(context);
    const selectedUsers = filters?.users.length ? filters.users : [nickname];
    const dateAndSelectedUsers = dateRange(from, to, filters ? { ...filters, users: [] } : undefined);
    return runQuery(`WITH games AS (${unionRows(game, filters)})
      SELECT game, nickname, COUNT(*) AS started,
        COUNT(*) FILTER (WHERE completed) AS completed,
        COUNT(*) FILTER (WHERE won) AS won,
        COUNT(*) FILTER (WHERE lost) AS lost,
        ROUND(100.0 * COUNT(*) FILTER (WHERE won) / NULLIF(COUNT(*) FILTER (WHERE completed), 0), 1) AS win_percentage
      FROM games ${dateAndSelectedUsers}${dateAndSelectedUsers ? ' AND' : ' WHERE'} nickname IN (${selectedUsers.map(sqlLiteral).join(', ')})
      GROUP BY game, nickname ORDER BY game`, context.abortSignal);
  },
});

export const gameTrendTool = createTool({
  id: 'game_trend',
  description: 'Serie temporale di partite iniziate, completate e vinte per giorno, settimana o mese.',
  inputSchema: z.object({ game: gameSchema, bucket: z.enum(['day', 'week', 'month']).default('month'), ...dateSchema }),
  outputSchema,
  execute: async ({ game, bucket, from, to }, context) => {
    const filters = filtersFrom(context);
    return runQuery(`WITH games AS (${unionRows(game, filters)})
      SELECT game, date_trunc(${sqlLiteral(bucket)}, to_date(played_on, 'YYYY-MM-DD'))::date AS period,
        COUNT(*) AS started,
        COUNT(*) FILTER (WHERE completed) AS completed,
        COUNT(*) FILTER (WHERE won) AS won,
        COUNT(*) FILTER (WHERE lost) AS lost
      FROM games ${dateRange(from, to, filters)}
      GROUP BY game, period ORDER BY period, game`, context.abortSignal);
  },
});

export const hexawordAttemptOverviewTool = createTool({
  id: 'hexaword_attempt_overview',
  description: 'Confronta le partite Hexaword per numero di tentativo: quante arrivano a ciascun tentativo e quante terminano con vittoria o sconfitta proprio lì.',
  inputSchema: z.object({
    nickname: z.string().regex(/^@[a-z0-9._-]{1,30}$/).optional(),
    ...dateSchema,
  }),
  outputSchema,
  execute: async ({ nickname, from, to }, context) => {
    const filters = filtersFrom(context);
    const where = hexawordFilters(filters, { nickname, from, to });
    if (where === null) return [];
    return runQuery(`SELECT attempt.ordinality AS attempt_number,
        COUNT(*) AS games_with_attempt,
        COUNT(*) FILTER (
          WHERE g.status = 'WON'
            AND attempt.ordinality = jsonb_array_length(g.guesses_json::jsonb)
        ) AS games_won_on_attempt,
        COUNT(*) FILTER (
          WHERE g.status = 'LOST'
            AND attempt.ordinality = jsonb_array_length(g.guesses_json::jsonb)
        ) AS games_lost_on_attempt
      FROM hexaword_games g
      JOIN users u ON u.id = g.user_id
      CROSS JOIN LATERAL jsonb_array_elements(g.guesses_json::jsonb) WITH ORDINALITY AS attempt(guess, ordinality)
      ${where}
      GROUP BY attempt.ordinality
      ORDER BY attempt.ordinality`, context.abortSignal);
  },
});

export const hexawordAttemptSearchTool = createTool({
  id: 'hexaword_attempt_search',
  description: 'Cerca i singoli tentativi Hexaword mostrando nickname, data, numero del tentativo, parola e feedback delle lettere. Può filtrare per nickname, periodo, numero del tentativo e parola; non restituisce la soluzione della partita.',
  inputSchema: z.object({
    nickname: z.string().regex(/^@[a-z0-9._-]{1,30}$/).optional(),
    attemptNumber: z.number().int().positive().optional(),
    word: z.string().trim().min(1).max(30).optional(),
    limit: z.number().int().min(1).max(100).default(25),
    ...dateSchema,
  }),
  outputSchema,
  execute: async ({ nickname, attemptNumber, word, limit, from, to }, context) => {
    const filters = filtersFrom(context);
    const where = hexawordFilters(filters, { nickname, from, to });
    if (where === null) return [];

    const clauses = where ? [where.slice('WHERE '.length)] : [];
    if (attemptNumber !== undefined) clauses.push(`attempt.ordinality = ${attemptNumber}`);
    if (word) clauses.push(`LOWER(attempt.guess->>'word') = LOWER(${sqlLiteral(word)})`);
    const attemptWhere = clauses.length ? `WHERE ${clauses.join(' AND ')}` : '';

    return runQuery(`SELECT u.nickname, g.puzzle_date AS played_on,
        attempt.ordinality AS attempt_number,
        attempt.guess->>'word' AS word,
        attempt.guess->'tiles' AS feedback,
        g.status
      FROM hexaword_games g
      JOIN users u ON u.id = g.user_id
      CROSS JOIN LATERAL jsonb_array_elements(g.guesses_json::jsonb) WITH ORDINALITY AS attempt(guess, ordinality)
      ${attemptWhere}
      ORDER BY g.puzzle_date DESC, u.nickname, attempt.ordinality
      LIMIT ${limit}`, context.abortSignal);
  },
});

const cohortAgeSchema = z.number().int().min(1).max(90).default(14);
const boundedLimitSchema = z.number().int().min(1).max(100).default(50);

function playerScope(filters: StatsFilters | undefined, days: number, prefix = 'u') {
  const clauses = [`${prefix}.created_at::timestamptz >= now() - make_interval(days => ${days})`, `${prefix}.created_at::timestamptz <= now()`];
  if (filters?.users.length) clauses.push(`${prefix}.nickname IN (${filters.users.map(sqlLiteral).join(', ')})`);
  const localCreatedAt = `(${prefix}.created_at::timestamptz AT TIME ZONE ${sqlLiteral(gameTimezone())})`;
  if (filters?.from) clauses.push(`${localCreatedAt} >= ${sqlLiteral(filters.from)}::date`);
  if (filters?.to) clauses.push(`${localCreatedAt} < (${sqlLiteral(filters.to)}::date + interval '1 day')`);
  return `WHERE ${clauses.join(' AND ')}`;
}

function activityDateRange(from?: string, to?: string, filters?: StatsFilters) {
  const selectedFrom = filters?.from ?? from;
  const selectedTo = filters?.to ?? to;
  const clauses = [];
  if (selectedFrom) clauses.push(`played_on >= ${sqlLiteral(selectedFrom)}`);
  if (selectedTo) clauses.push(`played_on <= ${sqlLiteral(selectedTo)}`);
  if (filters?.users.length) clauses.push(`nickname IN (${filters.users.map(sqlLiteral).join(', ')})`);
  return clauses.length ? `WHERE ${clauses.join(' AND ')}` : '';
}

export const newPlayerOverviewTool = createTool({
  id: 'new_player_overview',
  description: 'Riepilogo dei giocatori registrati negli ultimi N giorni (14 per impostazione predefinita): registrazioni, attivazione, partite e ritorno. Usa questo strumento per monitorare i nuovi giocatori.',
  inputSchema: z.object({ days: cohortAgeSchema }),
  outputSchema,
  execute: async ({ days }, context) => {
    const filters = filtersFrom(context);
    const activities = unionRows('all', filters);
    return runQuery(`WITH cohort AS (SELECT u.id, u.nickname, u.created_at::timestamptz AS joined_at FROM users u ${playerScope(filters, days)}),
      activity AS (${activities}), player_activity AS (
        SELECT c.id, COUNT(a.user_id) AS games_started,
          COUNT(a.user_id) FILTER (WHERE a.completed) AS games_completed,
          COUNT(DISTINCT a.played_on) AS active_days,
          MAX(a.played_on) AS last_active
        FROM cohort c LEFT JOIN activity a ON a.user_id = c.id GROUP BY c.id
      )
      SELECT ${days} AS cohort_days, COUNT(*) AS registered,
        COUNT(*) FILTER (WHERE p.games_started > 0) AS activated,
        COUNT(*) FILTER (WHERE p.active_days > 1) AS returned,
        COALESCE(SUM(p.games_started), 0) AS games_started,
        COALESCE(SUM(p.games_completed), 0) AS games_completed,
        ROUND(100.0 * COUNT(*) FILTER (WHERE p.games_started > 0) / NULLIF(COUNT(*), 0), 1) AS activation_percentage,
        ROUND(100.0 * COUNT(*) FILTER (WHERE p.active_days > 1) / NULLIF(COUNT(*) FILTER (WHERE p.games_started > 0), 0), 1) AS return_percentage
      FROM cohort c JOIN player_activity p ON p.id = c.id`, context.abortSignal);
  },
});

export const newPlayerListTool = createTool({
  id: 'new_player_list',
  description: 'Elenco dei giocatori registrati negli ultimi N giorni con data di iscrizione, giorni trascorsi, partite, giorni attivi e ultima attività. Non restituisce email né ID.',
  inputSchema: z.object({ days: cohortAgeSchema, limit: boundedLimitSchema }),
  outputSchema,
  execute: async ({ days, limit }, context) => {
    const filters = filtersFrom(context);
    const activities = unionRows('all', filters);
    return runQuery(`WITH cohort AS (SELECT u.id, u.nickname, u.created_at::timestamptz AS joined_at FROM users u ${playerScope(filters, days)}),
      activity AS (${activities})
      SELECT c.nickname, c.joined_at, FLOOR(EXTRACT(EPOCH FROM (now() - c.joined_at)) / 86400)::integer AS days_since_signup,
        COUNT(a.user_id) AS games_started, COUNT(a.user_id) FILTER (WHERE a.completed) AS games_completed,
        COUNT(DISTINCT a.played_on) AS active_days, MAX(a.played_on) AS last_active,
        COALESCE(array_agg(DISTINCT a.game) FILTER (WHERE a.game IS NOT NULL), ARRAY[]::text[]) AS games_played
      FROM cohort c LEFT JOIN activity a ON a.user_id = c.id
      GROUP BY c.id, c.nickname, c.joined_at ORDER BY c.joined_at DESC, c.nickname LIMIT ${limit}`, context.abortSignal);
  },
});

export const newPlayerGameBreakdownTool = createTool({
  id: 'new_player_game_breakdown',
  description: 'Confronta l uso dei singoli giochi tra chi si è registrato negli ultimi N giorni, mostrando partite e giocatori attivi per gioco.',
  inputSchema: z.object({ days: cohortAgeSchema, game: gameSchema }),
  outputSchema,
  execute: async ({ days, game }, context) => {
    const filters = filtersFrom(context);
    const activities = unionRows(game, filters);
    return runQuery(`WITH cohort AS (SELECT u.id FROM users u ${playerScope(filters, days)}), activity AS (${activities})
      SELECT a.game, COUNT(*) AS games_started, COUNT(*) FILTER (WHERE a.completed) AS games_completed,
        COUNT(DISTINCT a.user_id) AS active_players,
        ROUND(AVG(CASE WHEN a.completed THEN 1.0 ELSE 0.0 END) * 100, 1) AS completion_percentage
      FROM activity a JOIN cohort c ON c.id = a.user_id GROUP BY a.game ORDER BY active_players DESC, a.game`, context.abortSignal);
  },
});

export const newPlayerRetentionTool = createTool({
  id: 'new_player_retention',
  description: 'Misura per i nuovi iscritti quanti hanno giocato nei giorni 1, 3, 7 e 14 dopo la registrazione. La coorte include gli ultimi N giorni (massimo 90).',
  inputSchema: z.object({ days: cohortAgeSchema, game: gameSchema }),
  outputSchema,
  execute: async ({ days, game }, context) => {
    const filters = filtersFrom(context);
    const activities = unionRows(game, filters);
    return runQuery(`WITH cohort AS (SELECT u.id, u.created_at::timestamptz AS joined_at FROM users u ${playerScope(filters, days)}),
      activity AS (${activities}), ages AS (
        SELECT c.id, FLOOR(EXTRACT(EPOCH FROM (to_date(a.played_on, 'YYYY-MM-DD')::timestamp - c.joined_at::date)) / 86400)::integer AS day_after_signup
        FROM cohort c JOIN activity a ON a.user_id = c.id WHERE a.played_on IS NOT NULL
      ), milestones AS (SELECT unnest(ARRAY[1,3,7,14]) AS day_after_signup)
      SELECT m.day_after_signup, COUNT(DISTINCT c.id) FILTER (WHERE c.joined_at <= now() - make_interval(days => m.day_after_signup)) AS eligible_players,
        COUNT(DISTINCT a.id) AS active_players,
        ROUND(100.0 * COUNT(DISTINCT a.id) / NULLIF(COUNT(DISTINCT c.id) FILTER (WHERE c.joined_at <= now() - make_interval(days => m.day_after_signup)), 0), 1) AS retention_percentage
      FROM milestones m CROSS JOIN cohort c
      LEFT JOIN ages a ON a.id = c.id AND a.day_after_signup = m.day_after_signup
      GROUP BY m.day_after_signup ORDER BY m.day_after_signup`, context.abortSignal);
  },
});

export const playerGrowthTrendTool = createTool({
  id: 'player_growth_trend',
  description: 'Serie temporale delle nuove registrazioni per giorno, settimana o mese nel periodo richiesto.',
  inputSchema: z.object({ bucket: z.enum(['day', 'week', 'month']).default('week'), ...dateSchema }),
  outputSchema,
  execute: async ({ bucket, from, to }, context) => {
    const filters = filtersFrom(context);
    const clauses = [];
    const selectedFrom = filters?.from ?? from;
    const selectedTo = filters?.to ?? to;
    const localCreatedAt = `(u.created_at::timestamptz AT TIME ZONE ${sqlLiteral(gameTimezone())})`;
    if (selectedFrom) clauses.push(`${localCreatedAt} >= ${sqlLiteral(selectedFrom)}::date`);
    if (selectedTo) clauses.push(`${localCreatedAt} < (${sqlLiteral(selectedTo)}::date + interval '1 day')`);
    if (filters?.users.length) clauses.push(`u.nickname IN (${filters.users.map(sqlLiteral).join(', ')})`);
    const where = clauses.length ? `WHERE ${clauses.join(' AND ')}` : '';
    return runQuery(`SELECT date_trunc(${sqlLiteral(bucket)}, ${localCreatedAt})::date AS period,
        COUNT(*) AS new_players FROM users u ${where}
      GROUP BY period ORDER BY period`, context.abortSignal);
  },
});

export const dailyActivePlayersTool = createTool({
  id: 'daily_active_players',
  description: 'Giocatori attivi e partite per data, giorno, settimana o mese, filtrabili per gioco e periodo.',
  inputSchema: z.object({ game: gameSchema, bucket: z.enum(['day', 'week', 'month']).default('day'), ...dateSchema }),
  outputSchema,
  execute: async ({ game, bucket, from, to }, context) => {
    const filters = filtersFrom(context);
    const activities = unionRows(game, filters);
    return runQuery(`WITH activity AS (${activities})
      SELECT date_trunc(${sqlLiteral(bucket)}, to_date(played_on, 'YYYY-MM-DD'))::date AS period, game,
        COUNT(DISTINCT user_id) AS active_players, COUNT(*) AS games_started,
        COUNT(*) FILTER (WHERE completed) AS games_completed
      FROM activity ${activityDateRange(from, to, filters)} GROUP BY period, game ORDER BY period, game`, context.abortSignal);
  },
});

export const playerActivityProfileTool = createTool({
  id: 'player_activity_profile',
  description: 'Profilo di attività di un nickname: partite, completamenti, giorni attivi, giochi utilizzati, prima e ultima attività nel periodo selezionato.',
  inputSchema: z.object({ nickname: z.string().regex(/^@[a-z0-9._-]{1,30}$/), game: gameSchema, ...dateSchema }),
  outputSchema,
  execute: async ({ nickname, game, from, to }, context) => {
    const filters = filtersFrom(context);
    const activities = unionRows(game, filters);
    const where = activityDateRange(from, to, filters);
    const clauses = where ? [where.slice('WHERE '.length)] : [];
    clauses.push(`nickname = ${sqlLiteral(nickname)}`);
    return runQuery(`WITH activity AS (${activities})
      SELECT nickname, COUNT(*) AS games_started, COUNT(*) FILTER (WHERE completed) AS games_completed,
        COUNT(DISTINCT played_on) AS active_days, COUNT(DISTINCT game) AS games_used,
        MIN(played_on) AS first_activity, MAX(played_on) AS last_activity
      FROM activity WHERE ${clauses.join(' AND ')} GROUP BY nickname`, context.abortSignal);
  },
});

export const playerActivationTimeTool = createTool({
  id: 'player_activation_time',
  description: 'Calcola il tempo tra registrazione e prima partita per i nuovi iscritti, con distribuzione per fascia oraria e tempi medi/mediani.',
  inputSchema: z.object({ days: cohortAgeSchema, game: gameSchema }),
  outputSchema,
  execute: async ({ days, game }, context) => {
    const filters = filtersFrom(context);
    const activities = unionRows(game, filters);
    return runQuery(`WITH cohort AS (SELECT u.id, u.created_at::timestamptz AS joined_at FROM users u ${playerScope(filters, days)}),
      activity AS (${activities}), first_game AS (
        SELECT c.id, MIN(a.activity_at::timestamptz) AS first_played
        FROM cohort c JOIN activity a ON a.user_id = c.id GROUP BY c.id
      ), delays AS (SELECT EXTRACT(EPOCH FROM (first_played - joined_at)) / 3600.0 AS hours_to_first_game FROM cohort c JOIN first_game f ON f.id = c.id)
      SELECT COUNT(*) AS activated_players,
        ROUND(AVG(hours_to_first_game)::numeric, 1) AS average_hours_to_first_game,
        ROUND((percentile_cont(0.5) WITHIN GROUP (ORDER BY hours_to_first_game))::numeric, 1) AS median_hours_to_first_game,
        COUNT(*) FILTER (WHERE hours_to_first_game <= 24) AS played_within_24_hours,
        COUNT(*) FILTER (WHERE hours_to_first_game <= 168) AS played_within_7_days
      FROM delays`, context.abortSignal);
  },
});

export const playerInactivityTool = createTool({
  id: 'player_inactivity',
  description: 'Trova giocatori con registrazione o ultima partita più vecchia di N giorni, mostrando data di iscrizione, ultima attività e giochi svolti.',
  inputSchema: z.object({ inactiveDays: z.number().int().min(1).max(365).default(7), limit: boundedLimitSchema, game: gameSchema }),
  outputSchema,
  execute: async ({ inactiveDays, limit, game }, context) => {
    const filters = filtersFrom(context);
    const activities = unionRows(game, filters);
    return runQuery(`WITH activity AS (${activities}), recent AS (
        SELECT user_id, MAX(played_on) AS last_activity, COUNT(*) AS games_started,
          array_agg(DISTINCT game ORDER BY game) AS games_played FROM activity GROUP BY user_id
      )
      SELECT u.nickname, u.created_at AS joined_at, r.last_activity, COALESCE(r.games_started, 0) AS games_started,
        COALESCE(r.games_played, ARRAY[]::text[]) AS games_played,
        FLOOR(EXTRACT(EPOCH FROM (now() - COALESCE(to_date(r.last_activity, 'YYYY-MM-DD')::timestamptz, u.created_at::timestamptz))) / 86400)::integer AS inactive_days
      FROM users u LEFT JOIN recent r ON r.user_id = u.id
      ${filters?.users.length ? `WHERE u.nickname IN (${filters.users.map(sqlLiteral).join(', ')})` : ''}
      ${filters?.users.length ? 'AND' : 'WHERE'} COALESCE(to_date(r.last_activity, 'YYYY-MM-DD')::timestamptz, u.created_at::timestamptz) < now() - make_interval(days => ${inactiveDays})
      ORDER BY inactive_days DESC, u.nickname LIMIT ${limit}`, context.abortSignal);
  },
});

export const gameEngagementTool = createTool({
  id: 'game_engagement',
  description: 'Confronta per gioco giocatori unici, partite iniziate, completate, tasso di completamento e partite medie per giocatore.',
  inputSchema: z.object({ game: gameSchema, ...dateSchema }),
  outputSchema,
  execute: async ({ game, from, to }, context) => {
    const filters = filtersFrom(context);
    const activities = unionRows(game, filters);
    return runQuery(`WITH activity AS (${activities})
      SELECT game, COUNT(DISTINCT user_id) AS unique_players, COUNT(*) AS games_started,
        COUNT(*) FILTER (WHERE completed) AS games_completed,
        ROUND(100.0 * COUNT(*) FILTER (WHERE completed) / NULLIF(COUNT(*), 0), 1) AS completion_percentage,
        ROUND(COUNT(*)::numeric / NULLIF(COUNT(DISTINCT user_id), 0), 2) AS games_per_player
      FROM activity ${activityDateRange(from, to, filters)} GROUP BY game ORDER BY unique_players DESC, game`, context.abortSignal);
  },
});
