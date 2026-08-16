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

The Quarkus development profile runs anonymously by default for local work. Docker local,
staging, and production builds use the OIDC configuration in `.env` and require:

```bash
AUTH_ENABLED=true
GOOGLE_CLIENT_ID=...
```

`scripts/run-docker-local.sh` loads `.env` before building the image, so the
Google client ID is also embedded in the frontend build. It keeps cookies
non-secure for `http://localhost`; set `LOCAL_AUTH_ENABLED=false` only when an
anonymous local run is intentional.

Quarkus OIDC runs in `service` mode. At the first access, every `/api/*`
endpoint accepts a token issued by the configured OIDC provider:

```text
Authorization: Bearer <token>
```

Quarkus validates the token before the request reaches the API and creates an
application session backed by the database. Its opaque identifier is stored in
a persistent, `HttpOnly`, `Secure`, `SameSite=Lax` cookie, so closing and
reopening the browser does not require another login. The session is revoked
when the user selects logout (and expires after 30 days). The Google access
token remains in `sessionStorage` and is used only to establish the session.
Quarkus validates this opaque Google token through Google UserInfo. Configure
the Google client as a **Web application** and add each site origin to its Authorized JavaScript
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
PUSH_WEEKLY_AWARDS_REMINDER_CRON=0 10 20 ? * MON
LEADERBOARD_WEEKLY_AWARDS_CRON=0 5 0 ? * MON
```

The crons run in `GAME_TIMEZONE`. `PUSH_NEW_GAME_CRON` sends one notification per subscribed browser for each new daily puzzle. `PUSH_DAILY_REMINDER_CRON` reminds subscribed browsers whose user has not submitted any guess for the current puzzle date. `PUSH_WEEKLY_AWARDS_REMINDER_CRON` sends every Monday at 20:10 a reminder that the previous week has ended; tapping it opens the user's profile to view the updated medals.

### Hexahack rollout

Hexahack can be withheld from the public frontend by setting `VITE_HIDE_HEXAHACK=true` in the deployment environment. The value is compiled into the client image, so redeploy after changing it. While enabled, the game selector, direct `/#/hexahack` route, launch modal, related requests, and the admin history are hidden; the launch announcement remains pending.

The launch push campaign is disabled by default. Set `PUSH_HEXAHACK_LAUNCH_ENABLED=true` only when Hexahack is released, together with `VITE_HIDE_HEXAHACK=false`; pending campaign deliveries are then sent by the existing scheduler.

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

Database schema changes are managed by Flyway. Versioned SQL migrations live in
`backend/src/main/resources/db/migration` and run automatically before the
application starts. Hibernate validates the migrated schema instead of changing
it at runtime.

Existing production deployments are baselined automatically and upgrade from
V11 to `V12__add_hexahack_and_launch_campaign.sql`. This V12 replaces an
unreleased experiment: do not repair its checksum and do not add a conversion
migration. Local test databases are disposable; recreate any local database
that recorded the experimental V12 with
`scripts/run-docker-local.sh --fresh-db`. Recreate staging with:

```bash
scripts/reset-db-huff-staging.sh
```

The staging reset moves the previous data directory under `data/backups` before
running Flyway V1-V12 on a fresh database. Production must be at V11 before this
release and must not be reset. After V12 ships, resume the normal rule: every
schema change gets a new `V<n>__description.sql` migration. The legacy
`scripts/migrate-*.sh` files remain only for emergency/manual use.

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
