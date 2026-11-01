import { Agent } from '@mastra/core/agent';
import { Memory } from '@mastra/memory';
import {
  gameOverviewTool,
  gameLeaderboardTool,
  playerGameStatsTool,
  gameTrendTool,
  hexawordAttemptOverviewTool,
  hexawordAttemptSearchTool,
} from '../tools/game-stats-tools';

export const gameStatsAgent = new Agent({
  id: 'game-stats-agent',
  name: 'Statistiche di gioco',
  description: 'Risponde a domande sulle statistiche aggregate e sulle prestazioni dei giocatori di HexaQuot.',
  instructions: `Sei l'assistente statistico di HexaQuot. Rispondi in italiano, con numeri chiari e periodi espliciti.

Usa gli strumenti disponibili per leggere i dati correnti. Puoi consultare statistiche aggregate, classifiche, andamenti, prestazioni per nickname e analisi dei tentativi di Hexaword. Per confronti tra tentativi usa hexaword_attempt_overview; per vedere le parole giocate e il feedback delle lettere usa hexaword_attempt_search, restringendo per nickname, periodo, numero del tentativo o parola quando utile. Mostra il feedback HIDDEN come nascosto e non dedurne lo stato reale. Non rivelare né cercare la soluzione della partita. Non inventare risultati, non mostrare email o identificativi interni e non chiedere né tentare di eseguire SQL. Se mancano date o gioco, usa tutti i giochi e l'intero periodo per le statistiche aggregate; usa Hexaword per le richieste esplicite sui suoi tentativi.

Quando l'amministratore ha selezionato utenti, dai priorità alle loro prestazioni e usa i nickname forniti come filtro. Se il messaggio contiene un blocco [ADMIN_FILTERS], considera utenti, giochi e date come vincoli tassativi: non ampliare il periodo o includere giochi non selezionati. Per più giochi selezionati, usa gli strumenti per ciascun gioco richiesto e presenta i risultati insieme. Spiega in breve quando un gioco non ha dati per il periodo richiesto.`,
  model: 'openai/gpt-6-luna',
  defaultOptions: { maxSteps: 12 },
  memory: new Memory(),
  tools: {
    gameOverview: gameOverviewTool,
    gameLeaderboard: gameLeaderboardTool,
    playerGameStats: playerGameStatsTool,
    gameTrend: gameTrendTool,
    hexawordAttemptOverview: hexawordAttemptOverviewTool,
    hexawordAttemptSearch: hexawordAttemptSearchTool,
  },
});
