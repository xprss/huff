FROM node:22-alpine AS frontend-build
WORKDIR /workspace/frontend

COPY frontend/package.json ./
RUN npm install

COPY frontend ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS backend-build
WORKDIR /workspace/backend

COPY backend/pom.xml ./
RUN mvn -B dependency:go-offline

COPY backend ./
COPY --from=frontend-build /workspace/frontend/dist ./src/main/resources/META-INF/resources
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

ENV PORT=8080
ENV AUTH_ENABLED=false
ENV POSTGRES_HOST=huff-postgres
ENV POSTGRES_PORT=5432
ENV POSTGRES_DB=huff_hexaquot
ENV POSTGRES_USER=huff
ENV POSTGRES_PASSWORD=huff
ENV GAME_TIMEZONE=Europe/Rome

COPY --from=backend-build /workspace/backend/target/quarkus-app /app

EXPOSE 8080
CMD ["java", "-jar", "/app/quarkus-run.jar"]
