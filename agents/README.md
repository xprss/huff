# Agente statistiche HexaQuot

Questo progetto ospita gli agenti Mastra usati dal backend HexaQuot: statistiche dei giochi, spiegazioni delle parole e casi quotidiani di Hexatrap. L'agente accede a PostgreSQL tramite `@microsoft/postgres-mcp`; gli unici strumenti visibili al modello eseguono query statistiche predefinite in sola lettura.

## Configurazione locale

Copiare `.env.example` in `.env` e impostare:

- `OPENAI_API_KEY` per il modello;
- `POSTGRES_MCP_USER` e `POSTGRES_MCP_PASSWORD` per un utente PostgreSQL dedicato in sola lettura;
- `GAME_STATS_AGENT_TOKEN`, condiviso con il backend;
- `GAME_STATS_AGENT_DB_URL` per la memoria Mastra persistente.

Avviare il server dalla cartella `agents/`:

```sh
npm ci
npm run dev
```

Il server ascolta su `127.0.0.1:4111` in sviluppo. Le API Mastra e la rotta di chat richiedono il bearer token interno; la sola rotta health non restituisce dati.

Per creare il ruolo locale, configurare `POSTGRES_MCP_PASSWORD` nell'ambiente del repository e lanciare `scripts/provision-game-stats-role.sh`. Lo script usa l'utente proprietario del database e concede `SELECT` soltanto alle colonne statistiche delle partite, a `guesses_json` di Hexaword e a nickname, date di registrazione/avvio e ID necessari per analisi aggregate e join. L'agente usa `guesses_json` per analizzare i tentativi; non riceve accesso alla soluzione. Non concedere l'utente proprietario al processo MCP.

Gli strumenti includono statistiche aggregate, andamento delle registrazioni e delle attività, coinvolgimento per gioco e un gruppo di monitoraggio dei nuovi giocatori. Quest'ultimo considera gli account registrati negli ultimi 14 giorni per impostazione predefinita e misura attivazione, partite, giochi scelti, ritorno e tempo alla prima partita. L'agente `dailyGameSummaryAgent` viene invocato ogni giorno dal backend e produce il riepilogo del giorno precedente visibile nella sezione admin hexAI; gli amministratori possono anche generarlo o aggiornarlo con il pulsante della sezione. Gli strumenti di elenco e inattività restituiscono nickname e date, mai email o ID interni.

L'agente `wordExplanationAgent`, senza strumenti né accesso al database, scrive la spiegazione della parola del giorno di Hexaword e Hexastar tramite `POST /internal/words/explanation` (`{ "word": "abbaia" }` → `{ "word", "explanation" }`). Il backend lo invoca una sola volta per parola, alla prima partita conclusa, e memorizza il risultato.

Gli agenti `hexatrapAuthorAgent` e `hexatrapAuditorAgent`, senza strumenti né accesso al database, scrivono e revisionano il caso quotidiano di Hexatrap:

- `POST /internal/hexatrap/draft` riceve `{ brief: { date, difficulty, format, budget, questionCost, clueCosts, icons, avoid }, previousDraft, feedback }` e restituisce `{ draft, model }`. La bozza contiene ambientazione, personaggi, fatti con frasi atomiche, certezze, deposizioni, domande, reperti, fatti del verdetto e soluzione, con ogni elemento accompagnato dalla sua formula.
- `POST /internal/hexatrap/audit` riceve il catalogo dei fatti, i personaggi e i soli testi degli elementi e restituisce `{ items: [{ id, formula }], model }`: il revisore traduce i testi senza conoscere le formule dell'autore.

Il backend Java valida ogni bozza in modo esaustivo, confronta le traduzioni del revisore con le formule dell'autore e rimanda all'autore i problemi trovati; nessuna logica del caso è affidata al modello. Le regole complete sono in `docs/hexatrap.md`.

Le route `/internal/*` sono destinate esclusivamente al backend; non esporre la porta Mastra all'esterno.
