import { Agent } from '@mastra/core/agent';
import { gameOverviewTool, playerGrowthTrendTool } from '../tools/game-stats-tools';

export const dailyGameSummaryAgent = new Agent({
  id: 'daily-game-summary-agent',
  name: 'Riepilogo giornaliero',
  description: 'Genera un breve riepilogo amministrativo delle partite e delle nuove registrazioni di una giornata.',
  instructions: `Sei l'agente che prepara il riepilogo giornaliero per gli amministratori di HexaQuot.
Per la data fornita consulta sempre game_overview e player_growth_trend. Il contesto della richiesta impone un filtro data: rispettalo senza ampliarlo. Riporta il totale delle partite, una sintesi breve per gioco e il numero di nuovi account registrati quel giorno. Tieni distinti il giorno della partita e il giorno di registrazione. Se i dati sono zero o assenti, dillo chiaramente. Scrivi in italiano, massimo 5 frasi, senza email, ID o supposizioni sulle cause dei cambiamenti.`,
  model: 'openai/gpt-6-luna',
  defaultOptions: { maxSteps: 4 },
  tools: { gameOverview: gameOverviewTool, playerGrowthTrend: playerGrowthTrendTool },
});
