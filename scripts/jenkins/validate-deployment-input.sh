#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
VERSION_SCRIPT="${PROJECT_ROOT}/scripts/version.sh"

required_names=(IMAGE_REPOSITORY IMAGE_DIGEST GIT_COMMIT APP_VERSION GITHUB_RUN_URL)
for variable_name in "${required_names[@]}"; do
  [[ -n "${!variable_name:-}" ]] || { echo "Missing deployment parameter: ${variable_name}" >&2; exit 1; }
done

"${VERSION_SCRIPT}" validate-repository "${IMAGE_REPOSITORY}"
"${VERSION_SCRIPT}" validate-digest "${IMAGE_DIGEST}"
"${VERSION_SCRIPT}" validate-commit "${GIT_COMMIT}"
"${VERSION_SCRIPT}" validate-full "${APP_VERSION}"
[[ "${GITHUB_RUN_URL}" =~ ^https://github\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+/actions/runs/[0-9]+$ ]] || {
  echo "Invalid GitHub workflow URL" >&2
  exit 1
}

echo "Deployment parameters are valid"
