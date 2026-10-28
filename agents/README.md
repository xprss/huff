# Agente statistiche HexaQuot

Questo progetto ospita l'agente Mastra usato dal backend HexaQuot per rispondere a domande sulle statistiche dei giochi. L'agente accede a PostgreSQL tramite `@microsoft/postgres-mcp`; gli unici strumenti visibili al modello eseguono query statistiche predefinite in sola lettura.

## Configurazione locale

Copiare `.env.example` in `.env` e impostare:

- `OPENAI_API_KEY` per il modello;
- `POSTGRES_MCP_CONNECTION_STRING` con un utente PostgreSQL dedicato in sola lettura;
- `GAME_STATS_AGENT_TOKEN`, condiviso con il backend;
- `GAME_STATS_AGENT_DB_URL` per la memoria Mastra persistente.

Avviare il server dalla cartella `agents/`:

```sh
npm ci
npm run dev
```

Il server ascolta su `127.0.0.1:4111` in sviluppo. Le API Mastra e la rotta di chat richiedono il bearer token interno; la sola rotta health non restituisce dati.

Per creare il ruolo locale, configurare `POSTGRES_STATS_PASSWORD` nell'ambiente del repository e lanciare `scripts/provision-game-stats-role.sh`. Lo script usa l'utente proprietario del database e concede `SELECT` soltanto alle tabelle di gioco e alle colonne nickname necessarie per le join. Non concedere l'utente proprietario al processo MCP.

La route `POST /internal/game-stats/stream` è destinata esclusivamente al backend; non esporre la porta Mastra all'esterno.
