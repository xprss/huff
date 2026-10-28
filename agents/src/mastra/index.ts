import { Mastra } from '@mastra/core/mastra';
import { LibSQLStore } from '@mastra/libsql';
import { MastraCompositeStore } from '@mastra/core/storage';
import { gameStatsAgent } from './agents/game-stats-agent';
import { internalAuth } from './auth/internal-auth';
import { gameStatsRoutes } from './routes/game-stats-routes';

export const mastra = new Mastra({
  agents: { gameStatsAgent },
  storage: new MastraCompositeStore({
    id: 'composite-storage',
    default: new LibSQLStore({
      id: 'mastra-storage',
      url: process.env.GAME_STATS_AGENT_DB_URL || 'file:./data/mastra.db',
      authToken: process.env.TURSO_AUTH_TOKEN || undefined,
    }),
  }),
  server: {
    host: process.env.GAME_STATS_AGENT_HOST || '127.0.0.1',
    port: Number(process.env.GAME_STATS_AGENT_PORT || 4111),
    auth: internalAuth,
    apiRoutes: gameStatsRoutes,
  },
});
