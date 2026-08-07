# HexaQuot

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

To build and run the complete Docker image locally with PostgreSQL:

```bash
scripts/run-docker-local.sh
```

The application is available at `http://localhost:8080`. Press Ctrl-C to stop
the application container; the local PostgreSQL container and its named volume
are retained for the next run. Use `--no-build` to reuse the existing image.

## Configuration

The Quarkus development profile runs anonymously by default for local work. Docker and
production builds instead fail closed and require an OIDC provider configuration:

```bash
AUTH_ENABLED=true
GOOGLE_CLIENT_ID=...
```

Quarkus OIDC runs in `service` mode: every `/api/*` endpoint accepts only a
token issued by the configured OIDC provider:

```text
Authorization: Bearer <token>
```

Quarkus validates the token before the request reaches the API. No authorization
code flow, OIDC session, or cookie fallback is enabled. A missing or invalid
token returns `401`, a `WWW-Authenticate: Bearer` challenge, and clears legacy
session cookies. The web client uses Google OAuth to obtain an access token,
keeps it only in `sessionStorage`, adds it to every request, and removes it
automatically after `401`. Quarkus validates this opaque Google token through
Google UserInfo. Configure the Google client as a **Web application** and add
each site origin to its Authorized JavaScript
origins. For `npm run dev`, configure the
same public value as `VITE_GOOGLE_CLIENT_ID` in `frontend/.env.local`; the
Docker deployment scripts pass `GOOGLE_CLIENT_ID` to the frontend build.

The production and staging deployment scripts refuse to start when authentication
or OIDC configuration is missing. Every `/api/*` call requires a verified Bearer
identity at the server boundary and is never downgraded to an anonymous player.

Push notifications use standard Web Push with VAPID keys. Generate a stable key pair and put it in `.env` before deploying:

```bash
node scripts/generate-vapid-keys.mjs
```

```bash
VAPID_PUBLIC_KEY=...
VAPID_PRIVATE_KEY=...
VAPID_SUBJECT=mailto:notifications@huff.ottonovembre.it
PUSH_NEW_GAME_CRON=0 2 0 * * ?
PUSH_DAILY_REMINDER_CRON=0 0 23 * * ?
```

The crons run in `GAME_TIMEZONE`. `PUSH_NEW_GAME_CRON` sends one notification per subscribed browser for each new daily puzzle. `PUSH_DAILY_REMINDER_CRON` reminds subscribed browsers whose user has not submitted any guess for the current puzzle date.

The word list is stored in `backend/src/main/resources/words/it-words.json`; every entry must be 6 letters long.

## Deploy

```bash
scripts/redeploy-huff.sh
```

The script builds the Docker image, creates the Docker network if needed, starts PostgreSQL with persistent data in `POSTGRES_DATA_DIR`, and replaces the application container.

## Staging Deploy

Copy `.env.staging.example` to `.env.staging`, set `GOOGLE_CLIENT_ID` (and the
remaining staging secrets), then run:

```bash
scripts/redeploy-huff-staging.sh
```

The staging deploy uses isolated Docker resources by default: `huff-hexaquot-staging`, `huff-postgres-staging`, `huff-hexaquot-staging` network, and `127.0.0.1:8084`.

## Quarkus Logs

```bash
scripts/logs-huff.sh
```

Use `TAIL_LINES=500 scripts/logs-huff.sh` to change how many existing log lines are shown before following new output.
Console colors are forced on, including when the output is read through Docker,
so `INFO`, `WARN`, `ERROR` and stack traces remain visually distinct.

Every `/api/*` call produces an `API_REQUEST_STARTED` and an
`API_REQUEST_COMPLETED` entry. Match the two with `requestId`; the completion
entry includes HTTP status, outcome, elapsed time and response length. API logs
include only safe diagnostic metadata (method, path, query parameter names,
content metadata, forwarded client IP, origin and user agent): never cookies,
authorization headers, request bodies or query values. The request identifier
is used only to correlate the two container-log entries.

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

Production backups can be scheduled with the included systemd timer. It runs
`scripts/db-huff.sh backup` every day at 03:00 UTC and removes
`data/backups/huff-hexaquot-*.sql` files older than 30 days:

```bash
sudo install -m 0644 systemd/huff-db-backup.service /etc/systemd/system/huff-db-backup.service
sudo install -m 0644 systemd/huff-db-backup.timer /etc/systemd/system/huff-db-backup.timer
sudo systemctl daemon-reload
sudo systemctl enable --now huff-db-backup.timer
systemctl list-timers --all huff-db-backup.timer
```

To run one backup immediately:

```bash
sudo systemctl start huff-db-backup.service
```

To seed three consecutive completed games for every player, run:

```bash
scripts/seed-star-streak-huff.sh
```

The script inserts completed games for yesterday through three days ago for all
players.

To make every staging user able to use a star from the frontend, run:

```bash
scripts/grant-staging-stars-huff.sh
```

The script can be run again after testing to restore consumed stars.

Schema updates that add game or user bonus columns can be applied with:

```bash
scripts/migrate-game-modes-huff.sh
scripts/migrate-user-stars-huff.sh
scripts/migrate-user-profile-huff.sh
```

User profile nicknames can be changed directly in PostgreSQL when needed. The command is a dry-run by default and only writes with `--yes`:

```bash
scripts/set-user-nickname-huff.sh --id USER_ID --nickname nickname
scripts/set-user-nickname-huff.sh --id USER_ID --nickname @nickname --yes
```

To reset the live database, delete the PostgreSQL data directory and any old residual SQLite files, then redeploy the app:

```bash
scripts/reset-db-huff.sh
```

You can also open an interactive `psql` shell:

```bash
scripts/db-huff.sh shell
```
