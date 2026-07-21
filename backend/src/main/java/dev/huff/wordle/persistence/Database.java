package dev.huff.wordle.persistence;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@ApplicationScoped
public class Database {
    @ConfigProperty(name = "app.db.path")
    String dbPath;

    @PostConstruct
    void init() {
        try {
            Class.forName("org.sqlite.JDBC");
            Path path = Path.of(dbPath);
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Connection connection = connection(); Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL");
                statement.execute("PRAGMA foreign_keys=ON");
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                      id TEXT PRIMARY KEY,
                      google_subject TEXT UNIQUE,
                      email TEXT,
                      display_name TEXT,
                      created_at TEXT NOT NULL
                    )
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS games (
                      id TEXT PRIMARY KEY,
                      user_id TEXT NOT NULL REFERENCES users(id),
                      puzzle_date TEXT NOT NULL,
                      solution TEXT NOT NULL,
                      guesses_json TEXT NOT NULL,
                      status TEXT NOT NULL,
                      created_at TEXT NOT NULL,
                      updated_at TEXT NOT NULL,
                      completed_at TEXT,
                      UNIQUE(user_id, puzzle_date)
                    )
                    """);
                statement.execute("CREATE INDEX IF NOT EXISTS idx_games_user_date ON games(user_id, puzzle_date)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_games_user_status ON games(user_id, status)");
            }
        } catch (Exception error) {
            throw new IllegalStateException("Cannot initialize SQLite database at " + dbPath, error);
        }
    }

    public Connection connection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=ON");
        }
        return connection;
    }
}
