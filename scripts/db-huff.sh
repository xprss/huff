#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
DATA_DIR="${DATA_DIR:-${PROJECT_ROOT}/data}"
DB_FILE="${DB_FILE:-${DATA_DIR}/huff-wordle.sqlite}"

usage() {
  cat <<EOF
Usage:
  scripts/db-huff.sh path
  scripts/db-huff.sh tables
  scripts/db-huff.sh schema [table]
  scripts/db-huff.sh query "SELECT * FROM users;"
  scripts/db-huff.sh dump [output.sql]
  scripts/db-huff.sh backup [output.sqlite]
  scripts/db-huff.sh shell

Environment:
  DATA_DIR  Directory mounted as /data in the container. Default: ${PROJECT_ROOT}/data
  DB_FILE   SQLite database path on the host. Default: \${DATA_DIR}/huff-wordle.sqlite
EOF
}

require_db() {
  if [[ ! -f "${DB_FILE}" ]]; then
    echo "Database not found: ${DB_FILE}" >&2
    echo "Run scripts/redeploy-huff.sh once, or set DB_FILE=/path/to/huff-wordle.sqlite" >&2
    exit 1
  fi
}

run_python() {
  local action="$1"
  shift

  python3 - "${DB_FILE}" "${action}" "$@" <<'PY'
import csv
import datetime as dt
import os
import sqlite3
import sys

db_file = sys.argv[1]
action = sys.argv[2]
args = sys.argv[3:]

def connect():
    conn = sqlite3.connect(db_file)
    conn.row_factory = sqlite3.Row
    return conn

def print_rows(rows, headers):
    writer = csv.writer(sys.stdout, delimiter="\t", lineterminator="\n")
    if headers:
        writer.writerow(headers)
    for row in rows:
        writer.writerow([row[header] for header in headers])

if action == "tables":
    with connect() as conn:
        rows = conn.execute(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name"
        ).fetchall()
    for row in rows:
        print(row["name"])
elif action == "schema":
    table = args[0] if args else None
    sql = "SELECT name, sql FROM sqlite_master WHERE type IN ('table', 'index')"
    params = []
    if table:
        sql += " AND tbl_name = ?"
        params.append(table)
    sql += " ORDER BY type DESC, name"
    with connect() as conn:
        rows = conn.execute(sql, params).fetchall()
    for row in rows:
        if row["sql"]:
            print(f"{row['sql']};")
elif action == "query":
    query = args[0] if args else sys.stdin.read()
    if not query.strip():
        print("Missing SQL query", file=sys.stderr)
        sys.exit(2)
    with connect() as conn:
        cursor = conn.execute(query)
        if cursor.description:
            headers = [column[0] for column in cursor.description]
            print_rows(cursor.fetchall(), headers)
        else:
            conn.commit()
            print(f"{cursor.rowcount} row(s) affected")
elif action == "dump":
    output = args[0] if args else None
    with connect() as conn:
        lines = "\n".join(conn.iterdump()) + "\n"
    if output:
        os.makedirs(os.path.dirname(os.path.abspath(output)), exist_ok=True)
        with open(output, "w", encoding="utf-8") as handle:
            handle.write(lines)
        print(output)
    else:
        print(lines, end="")
elif action == "backup":
    output = args[0] if args else None
    if not output:
        stamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%d%H%M%S")
        output = os.path.join(os.path.dirname(db_file), "backups", f"huff-wordle-{stamp}.sqlite")
    os.makedirs(os.path.dirname(os.path.abspath(output)), exist_ok=True)
    with connect() as source, sqlite3.connect(output) as target:
        source.backup(target)
    print(output)
else:
    print(f"Unknown action: {action}", file=sys.stderr)
    sys.exit(2)
PY
}

command="${1:-help}"
shift || true

case "${command}" in
  help|-h|--help)
    usage
    ;;
  path)
    echo "${DB_FILE}"
    ;;
  shell)
    require_db
    if ! command -v sqlite3 >/dev/null 2>&1; then
      echo "sqlite3 CLI is not installed. Non-interactive commands still work:" >&2
      echo "  scripts/db-huff.sh tables" >&2
      echo "  scripts/db-huff.sh schema" >&2
      echo "  scripts/db-huff.sh query \"SELECT * FROM users;\"" >&2
      exit 1
    fi
    sqlite3 "${DB_FILE}"
    ;;
  tables|schema|query|dump|backup)
    require_db
    run_python "${command}" "$@"
    ;;
  *)
    usage >&2
    exit 2
    ;;
esac
