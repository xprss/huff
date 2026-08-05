#!/usr/bin/env bash

set -euo pipefail

CONTAINER_NAME="${CONTAINER_NAME:-huff-hexaquot}"
TAIL_LINES="${TAIL_LINES:-200}"

if [[ -t 1 ]]; then
  reset_terminal_colors() {
    printf '\033[0m' > /dev/tty 2>/dev/null || true
  }
  trap reset_terminal_colors EXIT
fi

docker logs --tail="${TAIL_LINES}" -f "${CONTAINER_NAME}"
