# Indovena

Wordle italiano con React, TypeScript e backend Java Quarkus.

## Sviluppo locale

Backend:

```bash
cd backend
mvn quarkus:dev
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

Il frontend usa il proxy Vite verso Quarkus su `localhost:8080`.

## Configurazione

Copia `.env.example` in `.env` per il deploy Docker. L'autenticazione e' disattivata di default:

```bash
AUTH_ENABLED=false
```

Per abilitare Google OAuth:

```bash
AUTH_ENABLED=true
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
COOKIE_SECURE=true
```

Nel client OAuth Google usa tipo applicazione `Web application`.

Origini JavaScript autorizzate:

```text
https://huff.ottonovembre.it
https://www.huff.ottonovembre.it
```

URI di reindirizzamento autorizzati:

```text
https://huff.ottonovembre.it/auth/callback
https://www.huff.ottonovembre.it/auth/callback
```

Il login parte da `/api/login`; il logout locale dell'app passa da `/api/logout`.

Le parole sono in `backend/src/main/resources/words/it-words.json` e devono essere tutte di 6 lettere.

## Deploy

```bash
scripts/redeploy-huff.sh
```

Lo script costruisce l'immagine Docker, crea la rete Docker se manca, avvia PostgreSQL con dati persistenti in `POSTGRES_DATA_DIR` e rimpiazza il container applicativo.

## Log Quarkus

```bash
scripts/logs-huff.sh
```

Usa `TAIL_LINES=500 scripts/logs-huff.sh` per cambiare quante righe iniziali mostrare prima del follow.

## Database

Il backend usa Hibernate ORM/Panache su PostgreSQL. Il database live gira nel container `POSTGRES_CONTAINER_NAME` e persiste i dati in `POSTGRES_DATA_DIR`.

Comandi utili:

```bash
scripts/db-huff.sh path
scripts/db-huff.sh tables
scripts/db-huff.sh schema
scripts/db-huff.sh query "SELECT * FROM users;"
scripts/db-huff.sh backup
scripts/db-huff.sh dump data/huff-wordle.sql
```

Per azzerare il database live, eliminando la directory dati PostgreSQL e gli eventuali vecchi file SQLite residui, poi ridistribuendo l'app:

```bash
scripts/reset-db-huff.sh
```

Puoi aprire anche una shell `psql` interattiva:

```bash
scripts/db-huff.sh shell
```
