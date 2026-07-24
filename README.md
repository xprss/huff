# Indovena

Daily Italian word puzzle built with React, TypeScript, and a Java Quarkus backend.

## Local Development

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

The frontend uses the Vite proxy to reach Quarkus on `localhost:8080`.

## Configuration

Copy `.env.example` to `.env` for Docker deployments. Authentication is disabled by default:

```bash
AUTH_ENABLED=false
```

To enable Google OAuth:

```bash
AUTH_ENABLED=true
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
COOKIE_SECURE=true
```

In the Google OAuth client, use the `Web application` application type.

Authorized JavaScript origins:

```text
https://huff.ottonovembre.it
https://www.huff.ottonovembre.it
```

Authorized redirect URIs:

```text
https://huff.ottonovembre.it/auth/callback
https://www.huff.ottonovembre.it/auth/callback
```

Login starts at `/api/login`; local logout uses `/api/logout`.

The word list is stored in `backend/src/main/resources/words/it-words.json`; every entry must be 6 letters long.

## Deploy

```bash
scripts/redeploy-huff.sh
```

The script builds the Docker image, creates the Docker network if needed, starts PostgreSQL with persistent data in `POSTGRES_DATA_DIR`, and replaces the application container.

## Quarkus Logs

```bash
scripts/logs-huff.sh
```

Use `TAIL_LINES=500 scripts/logs-huff.sh` to change how many existing log lines are shown before following new output.

## Database

The backend uses Hibernate ORM/Panache with PostgreSQL. The live database runs in the `POSTGRES_CONTAINER_NAME` container and persists data in `POSTGRES_DATA_DIR`.

Useful commands:

```bash
scripts/db-huff.sh path
scripts/db-huff.sh tables
scripts/db-huff.sh schema
scripts/db-huff.sh query "SELECT * FROM users;"
scripts/db-huff.sh backup
scripts/db-huff.sh dump data/huff-hexaquot.sql
scripts/delete-player-huff.sh --email player@example.com
```

To reset the live database, delete the PostgreSQL data directory and any old residual SQLite files, then redeploy the app:

```bash
scripts/reset-db-huff.sh
```

You can also open an interactive `psql` shell:

```bash
scripts/db-huff.sh shell
```
