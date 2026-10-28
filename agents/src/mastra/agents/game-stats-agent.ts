import { Agent } from '@mastra/core/agent';
import { Memory } from '@mastra/memory';
import {
  gameOverviewTool,
  gameLeaderboardTool,
  playerGameStatsTool,
  gameTrendTool,
} from '../tools/game-stats-tools';

export const gameStatsAgent = new Agent({
  id: 'game-stats-agent',
  name: 'Statistiche di gioco',
  description: 'Risponde a domande sulle statistiche aggregate e sulle prestazioni dei giocatori di HexaQuot.',
  instructions: `Sei l'assistente statistico di HexaQuot. Rispondi in italiano, con numeri chiari e periodi espliciti.

Usa gli strumenti disponibili per leggere i dati correnti. Puoi consultare solo statistiche aggregate, classifiche, andamenti e prestazioni per nickname. Non inventare risultati, non mostrare email o identificativi interni e non chiedere né tentare di eseguire SQL. Se mancano date o gioco, usa tutti i giochi e l'intero periodo. Distingui partite iniziate, completate, vinte e perse in base ai dati restituiti.

Quando l'amministratore ha selezionato utenti, dai priorità alle loro prestazioni e usa i nickname forniti come filtro. Spiega in breve quando un gioco non ha dati per il periodo richiesto.`,
  model: 'openai/gpt-6.1-sol',
  defaultOptions: { maxSteps: 12 },
  memory: new Memory(),
  tools: {
    gameOverview: gameOverviewTool,
    gameLeaderboard: gameLeaderboardTool,
    playerGameStats: playerGameStatsTool,
    gameTrend: gameTrendTool,
  },
});
