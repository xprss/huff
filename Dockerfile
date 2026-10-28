# syntax=docker/dockerfile:1.7

FROM node:22-alpine AS frontend-build
WORKDIR /workspace/frontend

ARG GOOGLE_CLIENT_ID
ARG APP_VERSION
ENV VITE_GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
ENV VITE_APP_VERSION=${APP_VERSION}

COPY VERSION /workspace/VERSION
COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm,sharing=locked npm ci

COPY frontend ./
RUN test -n "${APP_VERSION}" && npm run build

FROM maven:3.9-eclipse-temurin-17 AS backend-build
WORKDIR /workspace/backend

COPY backend/pom.xml ./
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B dependency:go-offline

COPY backend ./
COPY --from=frontend-build /workspace/frontend/dist ./src/main/resources/META-INF/resources
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B package -Dmaven.test.skip=true

FROM node:24-bookworm-slim AS agents-build
WORKDIR /workspace/agents

COPY agents/package.json agents/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm,sharing=locked npm ci

COPY agents .
RUN npm run build

FROM node:24-bookworm-slim AS runtime
WORKDIR /app

ARG APP_VERSION
ARG GIT_COMMIT

RUN test -n "${APP_VERSION}" && test -n "${GIT_COMMIT}" \
    && apt-get update \
    && apt-get install -y --no-install-recommends openjdk-17-jre-headless tini \
    && rm -rf /var/lib/apt/lists/*

LABEL org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.revision="${GIT_COMMIT}"

ENV PORT=8080
ENV AUTH_ENABLED=true
ENV POSTGRES_HOST=huff-postgres
ENV POSTGRES_PORT=5432
ENV POSTGRES_DB=huff_hexaquot
ENV POSTGRES_USER=huff
ENV POSTGRES_PASSWORD=huff
ENV GAME_TIMEZONE=Europe/Rome
ENV GAME_STATS_AGENT_HOST=127.0.0.1
ENV GAME_STATS_AGENT_PORT=4111
ENV GAME_STATS_AGENT_DB_URL=file:./data/mastra.db

COPY --from=backend-build /workspace/backend/target/quarkus-app /app
COPY --from=agents-build /workspace/agents/.mastra/output /app/agents/.mastra/output
COPY --from=agents-build /workspace/agents/node_modules/@microsoft /app/agents/node_modules/@microsoft
COPY scripts/start-huff-app.sh /app/start-huff-app.sh
RUN chmod +x /app/start-huff-app.sh && mkdir -p /app/agents/data

EXPOSE 8080
ENTRYPOINT ["/usr/bin/tini", "--", "/app/start-huff-app.sh"]
