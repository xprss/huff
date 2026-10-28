import path from 'node:path';
import { MCPClient } from '@mastra/mcp';
import { createTool } from '@mastra/core/tools';
import { z } from 'zod';

const games = ['all', 'hexaword', 'hexahack', 'hexasky', 'hexaflow', 'hexastar', 'hexaeco', 'hexatrap'] as const;
const singleGames = games.slice(1);
type Game = typeof games[number];

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
  hexaword: `SELECT 'hexaword' AS game, u.nickname, g.puzzle_date AS played_on, g.status,
       (g.status IN ('WON', 'LOST')) AS completed,
       (g.status = 'WON') AS won, (g.status = 'LOST') AS lost
    FROM hexaword_games g JOIN users u ON u.id = g.user_id`,
  hexahack: `SELECT 'hexahack' AS game, u.nickname, g.puzzle_date AS played_on, g.status,
       (g.status = 'COMPLETED') AS completed,
       (g.status = 'COMPLETED') AS won, false AS lost
    FROM hexahack_games g JOIN users u ON u.id = g.user_id`,
  hexasky: `SELECT 'hexasky' AS game, u.nickname, g.puzzle_date AS played_on, g.status,
       (g.status IN ('WON', 'LOST')) AS completed,
       (g.status = 'WON') AS won, (g.status = 'LOST') AS lost
    FROM hexasky_games g JOIN users u ON u.id = g.user_id`,
  hexaflow: `SELECT 'hexaflow' AS game, u.nickname, g.puzzle_date AS played_on, g.status,
       (g.status = 'COMPLETED') AS completed,
       (g.status = 'COMPLETED') AS won, false AS lost
    FROM hexaflow_games g JOIN users u ON u.id = g.user_id`,
  hexastar: `SELECT 'hexastar' AS game, u.nickname, g.puzzle_date AS played_on, g.status,
       (g.status IN ('WON', 'LOST')) AS completed,
       (g.status = 'WON') AS won, (g.status = 'LOST') AS lost
    FROM hexastar_games g JOIN users u ON u.id = g.user_id`,
  hexaeco: `SELECT 'hexaeco' AS game, u.nickname, g.puzzle_date AS played_on, g.status,
       true AS completed, true AS won, false AS lost
    FROM hexaeco_games g JOIN users u ON u.id = g.user_id`,
  hexatrap: `SELECT 'hexatrap' AS game, u.nickname, g.puzzle_date AS played_on, g.status,
       (g.status = 'WON') AS completed,
       (g.status = 'WON') AS won, false AS lost
    FROM hexatrap_games g JOIN users u ON u.id = g.user_id`,
};

function unionRows(game: Game) {
  const selected = game === 'all' ? singleGames : [game];
  return selected.map((name) => gameRows[name as Exclude<Game, 'all'>]).join('\nUNION ALL\n');
}

function dateRange(from?: string, to?: string) {
  const clauses = [];
  if (from) clauses.push(`played_on >= '${from}'`);
  if (to) clauses.push(`played_on <= '${to}'`);
  return clauses.length ? `WHERE ${clauses.join(' AND ')}` : '';
}

function sqlLiteral(value: string) {
  return `'${value.replaceAll("'", "''")}'`;
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
  execute: async ({ game, from, to }, { abortSignal }) => {
    return runQuery(`WITH games AS (${unionRows(game)})
      SELECT game, COUNT(*) AS started,
        COUNT(*) FILTER (WHERE completed) AS completed,
        COUNT(*) FILTER (WHERE won) AS won,
        COUNT(*) FILTER (WHERE lost) AS lost,
        ROUND(100.0 * COUNT(*) FILTER (WHERE won) / NULLIF(COUNT(*) FILTER (WHERE completed), 0), 1) AS win_percentage
      FROM games ${dateRange(from, to)} GROUP BY game ORDER BY game`, abortSignal);
  },
});

export const gameLeaderboardTool = createTool({
  id: 'game_leaderboard',
  description: 'Classifica dei giocatori per vittorie nel gioco e periodo selezionati.',
  inputSchema: z.object({ game: gameSchema, limit: z.number().int().min(1).max(50).default(10), ...dateSchema }),
  outputSchema,
  execute: async ({ game, limit, from, to }, { abortSignal }) => {
    const groupBy = game === 'all' ? 'nickname' : 'game, nickname';
    const selectedGame = game === 'all' ? "'all'" : 'game';
    return runQuery(`WITH games AS (${unionRows(game)})
      SELECT ${selectedGame} AS game, nickname,
        COUNT(*) FILTER (WHERE won) AS wins,
        COUNT(*) FILTER (WHERE completed) AS completed,
        ROUND(100.0 * COUNT(*) FILTER (WHERE won) / NULLIF(COUNT(*) FILTER (WHERE completed), 0), 1) AS win_percentage
      FROM games ${dateRange(from, to)}
      GROUP BY ${groupBy} ORDER BY wins DESC, completed DESC, nickname ASC LIMIT ${limit}`, abortSignal);
  },
});

export const playerGameStatsTool = createTool({
  id: 'player_game_stats',
  description: 'Statistiche aggregate di un nickname, per gioco e periodo selezionati.',
  inputSchema: z.object({ nickname: z.string().regex(/^@[a-z0-9._-]{1,30}$/), game: gameSchema, ...dateSchema }),
  outputSchema,
  execute: async ({ nickname, game, from, to }, { abortSignal }) => {
    const clauses = [`nickname = ${sqlLiteral(nickname)}`];
    if (from) clauses.push(`played_on >= '${from}'`);
    if (to) clauses.push(`played_on <= '${to}'`);
    return runQuery(`WITH games AS (${unionRows(game)})
      SELECT game, nickname, COUNT(*) AS started,
        COUNT(*) FILTER (WHERE completed) AS completed,
        COUNT(*) FILTER (WHERE won) AS won,
        COUNT(*) FILTER (WHERE lost) AS lost,
        ROUND(100.0 * COUNT(*) FILTER (WHERE won) / NULLIF(COUNT(*) FILTER (WHERE completed), 0), 1) AS win_percentage
      FROM games WHERE ${clauses.join(' AND ')} GROUP BY game, nickname ORDER BY game`, abortSignal);
  },
});

export const gameTrendTool = createTool({
  id: 'game_trend',
  description: 'Serie temporale di partite iniziate, completate e vinte per giorno, settimana o mese.',
  inputSchema: z.object({ game: gameSchema, bucket: z.enum(['day', 'week', 'month']).default('month'), ...dateSchema }),
  outputSchema,
  execute: async ({ game, bucket, from, to }, { abortSignal }) => {
    return runQuery(`WITH games AS (${unionRows(game)})
      SELECT game, date_trunc(${sqlLiteral(bucket)}, to_date(played_on, 'YYYY-MM-DD'))::date AS period,
        COUNT(*) AS started,
        COUNT(*) FILTER (WHERE completed) AS completed,
        COUNT(*) FILTER (WHERE won) AS won,
        COUNT(*) FILTER (WHERE lost) AS lost
      FROM games ${dateRange(from, to)}
      GROUP BY game, period ORDER BY period, game`, abortSignal);
  },
});
