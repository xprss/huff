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

FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

ARG APP_VERSION
ARG GIT_COMMIT

RUN test -n "${APP_VERSION}" && test -n "${GIT_COMMIT}"

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

COPY --from=backend-build /workspace/backend/target/quarkus-app /app

EXPOSE 8080
CMD ["java", "-jar", "/app/quarkus-run.jar"]
