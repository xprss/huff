#!/bin/sh

set -eu

: "${GAME_STATS_AGENT_TOKEN:?GAME_STATS_AGENT_TOKEN is required}"
: "${OPENAI_API_KEY:?OPENAI_API_KEY is required}"
: "${POSTGRES_MCP_PASSWORD:?POSTGRES_MCP_PASSWORD is required}"

cd /app/agents
node .mastra/output/index.mjs &
agent_pid=$!

cleanup() {
  status=$?
  trap - EXIT
  kill -TERM "$agent_pid" 2>/dev/null || true
  if [ -n "${app_pid:-}" ]; then kill -TERM "$app_pid" 2>/dev/null || true; fi
  wait "$agent_pid" 2>/dev/null || true
  if [ -n "${app_pid:-}" ]; then wait "$app_pid" 2>/dev/null || true; fi
  exit "$status"
}
trap cleanup EXIT
trap 'exit 143' TERM INT

agent_ready=false
attempt=0
while [ "$attempt" -lt 60 ]; do
  if ! kill -0 "$agent_pid" 2>/dev/null; then
    echo "Mastra exited before becoming ready." >&2
    exit 1
  fi
  if node -e 'fetch(`http://127.0.0.1:${process.env.GAME_STATS_AGENT_PORT || 4111}/internal/health`).then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))'; then
    agent_ready=true
    break
  fi
  attempt=$((attempt + 1))
  sleep 1
done

if [ "$agent_ready" != true ]; then
  echo "Mastra did not become ready in 60 seconds." >&2
  exit 1
fi

cd /app
java -XX:MaxRAMPercentage="${JAVA_MAX_RAM_PERCENTAGE:-60.0}" -jar /app/quarkus-run.jar &
app_pid=$!

set +e
wait "$app_pid"
app_status=$?
set -e
exit "$app_status"
