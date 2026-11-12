SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'stats_role', :'stats_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'stats_role')
\gexec

ALTER ROLE :"stats_role" WITH LOGIN PASSWORD :'stats_password'
  NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION;
ALTER ROLE :"stats_role" SET default_transaction_read_only = on;
REVOKE ALL PRIVILEGES ON SCHEMA public FROM :"stats_role";
REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM :"stats_role";
GRANT CONNECT ON DATABASE :"stats_database" TO :"stats_role";
GRANT USAGE ON SCHEMA public TO :"stats_role";
GRANT SELECT (id, nickname, display_name, created_at) ON TABLE users TO :"stats_role";
GRANT SELECT (user_id, puzzle_date, status, created_at) ON TABLE
  hexaword_games, hexahack_games, hexasky_games, hexaflow_games,
  hexastar_games, hexaeco_games, hexatrap_games TO :"stats_role";
GRANT SELECT (guesses_json) ON TABLE hexaword_games TO :"stats_role";
