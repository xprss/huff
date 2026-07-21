#!/usr/bin/env bash

set -euo pipefail

BRANCH="${BRANCH:-master}"
PROJECT_ROOT="${PROJECT_ROOT:-/root/huff}"
REMOTE_NAME="${REMOTE_NAME:-origin}"

cd "${PROJECT_ROOT}"

echo "Fetching ${REMOTE_NAME}/${BRANCH}"
git fetch "${REMOTE_NAME}" "${BRANCH}"
git checkout "${BRANCH}"
git pull --ff-only "${REMOTE_NAME}" "${BRANCH}"

echo "Redeploying Huff Wordle container"
"${PROJECT_ROOT}/scripts/redeploy-huff.sh"
