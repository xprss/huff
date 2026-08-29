package dev.huff.hexaquot.persistence;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class FlywayV14MigrationTest {
    @Inject AgroalDataSource dataSource;

    @Test
    void migratesAnEmptyDatabaseDirectlyToTheNewV16() throws Exception {
        withSchema(schema -> {
            flyway(schema, null).migrate();
            assertEquals("16", scalar(schema, "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1"));
            assertTrue(tableExists(schema, "hexaword_games"));
            assertTrue(tableExists(schema, "hexahack_games"));
            assertTrue(tableExists(schema, "hexasky_games"));
            assertTrue(tableExists(schema, "hexaflow_puzzles"));
            assertTrue(tableExists(schema, "hexaflow_games"));
            assertTrue(columnExists(schema, "admin_users", "can_manage_hexaflow_puzzles"));
            assertFalse(columnExists(schema, "hexahack_games", "override_count"));
            assertFalse(tableExists(schema, "games"));
        });
    }

    @Test
    void removesOverrideHistoryWhenUpgradingFromV12() throws Exception {
        withSchema(schema -> {
            flyway(schema, MigrationVersion.fromVersion("12")).migrate();
            execute(schema, """
                INSERT INTO users (id, display_name, nickname, profile_emoji, created_at, star_available, input_hand_preference)
                VALUES ('v12-user', 'V12 User', '@v12-user', '😀', '2026-01-01T00:00:00Z', false, 'RIGHT');
                INSERT INTO hexahack_games (id, user_id, puzzle_date, rules_version, solution, event_log_json,
                  total_cost, wrong_submissions, override_count, status, created_at, updated_at)
                VALUES ('v12-hack', 'v12-user', '2026-01-02', 1, '012345',
                  '[{"sequence":1,"kind":"OVERRIDE","occurredAt":"2026-01-02T00:00:00Z","probe":null,"submission":null,"override":{"requestId":"old-override","position":1,"digit":"0","cost":6}},{"sequence":2,"kind":"PROBE","occurredAt":"2026-01-02T00:00:01Z","probe":{"requestId":"probe","type":"BIT_SCAN","cost":1,"position":1,"otherPosition":null,"threshold":null,"comparison":null,"parity":"EVEN","sum":null,"summary":"pari"},"submission":null,"override":null}]',
                  7, 0, 1, 'IN_PROGRESS', '2026-01-02T00:00:00Z', '2026-01-02T00:00:01Z');
                """);

            flyway(schema, null).migrate();

            assertFalse(columnExists(schema, "hexahack_games", "override_count"));
            String eventLog = scalar(schema, "SELECT event_log_json FROM hexahack_games WHERE id = 'v12-hack'");
            assertTrue(eventLog.contains("PROBE"));
            assertFalse(eventLog.contains("OVERRIDE"));
            assertFalse(eventLog.contains("override"));
        });
    }

    @Test
    void upgradesARealisticV11WithoutAliasesOrCompatibilityData() throws Exception {
        withSchema(schema -> {
            flyway(schema, MigrationVersion.fromVersion("11")).migrate();
            execute(schema, """
                INSERT INTO users (id, display_name, nickname, profile_emoji, created_at, star_available, input_hand_preference)
                VALUES ('v11-user', 'V11 User', '@v11-user', '😀', '2026-01-01T00:00:00Z', false, 'RIGHT');
                INSERT INTO games (id, user_id, puzzle_date, mode, solution, guesses_json, status,
                  mouse_revealed, kitten_unlocked, created_at, updated_at, completed_at)
                VALUES ('v11-game', 'v11-user', '2026-01-02', 'CLASSIC', 'abbaco', '[]', 'WON',
                  true, false, '2026-01-02T00:00:00Z', '2026-01-02T00:00:00Z', '2026-01-02T00:00:00Z');
                INSERT INTO push_subscriptions (id, user_id, endpoint, p256dh, auth, created_at, updated_at)
                VALUES ('v11-push', 'v11-user', 'https://example.test/v11', 'key', 'auth',
                  '2026-01-01T00:00:00Z', '2026-01-01T00:00:00Z');
                """);

            flyway(schema, null).migrate();

            assertFalse(tableExists(schema, "games"));
            assertEquals(1L, scalarLong(schema, "SELECT COUNT(*) FROM hexaword_games WHERE id = 'v11-game'"));
            assertEquals(0L, scalarLong(schema, "SELECT COUNT(*) FROM hexahack_games"));
            assertEquals(1L, scalarLong(schema, "SELECT COUNT(*) FROM user_announcements WHERE campaign = 'HEXAHACK_LAUNCH'"));
            assertEquals(1L, scalarLong(schema, "SELECT COUNT(*) FROM user_announcements WHERE campaign = 'HEXAFLOW_LAUNCH'"));
            assertEquals(1L, scalarLong(schema, "SELECT COUNT(*) FROM push_campaign_deliveries WHERE campaign = 'HEXAHACK_LAUNCH'"));
            assertEquals(0L, scalarLong(schema, "SELECT COUNT(*) FROM information_schema.views WHERE table_schema = current_schema()"));
        });
    }

    private Flyway flyway(String schema, MigrationVersion target) {
        var configuration = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .schemas(schema)
            .defaultSchema(schema)
            .placeholders(Map.of("game_timezone", "Europe/Rome"));
        if (target != null) configuration.target(target);
        return configuration.load();
    }

    private void withSchema(SchemaAssertion assertion) throws Exception {
        String schema = "v12_test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }
        try {
            assertion.run(schema);
        } finally {
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
                statement.execute("SET search_path TO public");
            }
        }
    }

    private void execute(String schema, String sql) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            try {
                statement.execute("SET search_path TO " + schema);
                statement.execute(sql);
            } finally {
                statement.execute("SET search_path TO public");
            }
        }
    }

    private boolean tableExists(String schema, String table) throws Exception {
        try (Connection connection = dataSource.getConnection(); var statement = connection.prepareStatement("""
            SELECT EXISTS (
              SELECT 1 FROM information_schema.tables
              WHERE table_schema = ? AND table_name = ? AND table_type = 'BASE TABLE'
            )
            """)) {
            statement.setString(1, schema);
            statement.setString(2, table);
            try (ResultSet result = statement.executeQuery()) { result.next(); return result.getBoolean(1); }
        }
    }

    private boolean columnExists(String schema, String table, String column) throws Exception {
        try (Connection connection = dataSource.getConnection(); var statement = connection.prepareStatement("""
            SELECT EXISTS (
              SELECT 1 FROM information_schema.columns
              WHERE table_schema = ? AND table_name = ? AND column_name = ?
            )
            """)) {
            statement.setString(1, schema);
            statement.setString(2, table);
            statement.setString(3, column);
            try (ResultSet result = statement.executeQuery()) { result.next(); return result.getBoolean(1); }
        }
    }

    private String scalar(String schema, String sql) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            try {
                statement.execute("SET search_path TO " + schema);
                try (ResultSet result = statement.executeQuery(sql)) { result.next(); return result.getString(1); }
            } finally {
                statement.execute("SET search_path TO public");
            }
        }
    }

    private long scalarLong(String schema, String sql) throws Exception {
        return Long.parseLong(scalar(schema, sql));
    }

    @FunctionalInterface
    private interface SchemaAssertion { void run(String schema) throws Exception; }
}
