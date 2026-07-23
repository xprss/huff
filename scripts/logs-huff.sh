#!/usr/bin/env bash

set -euo pipefail

CONTAINER_NAME="${CONTAINER_NAME:-huff-hexaquot}"
TAIL_LINES="${TAIL_LINES:-200}"

docker logs --tail="${TAIL_LINES}" -f "${CONTAINER_NAME}"
