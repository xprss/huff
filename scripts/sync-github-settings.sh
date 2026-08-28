#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env.jenkins"
REPOSITORY=""

usage() {
  cat <<'EOF'
Usage: scripts/sync-github-settings.sh --repo OWNER/REPOSITORY [--env-file FILE]

Synchronizes the GitHub Actions secrets and variables used by delivery.yml.
Values are never printed. Authenticate first with `gh auth login`.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --repo)
      [[ $# -ge 2 ]] || { echo "--repo requires a value" >&2; exit 2; }
      REPOSITORY="$2"
      shift 2
      ;;
    --env-file)
      [[ $# -ge 2 ]] || { echo "--env-file requires a value" >&2; exit 2; }
      ENV_FILE="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

[[ "${REPOSITORY}" =~ ^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$ ]] || { usage >&2; exit 2; }
[[ -f "${ENV_FILE}" ]] || { echo "Settings file not found: ${ENV_FILE}" >&2; exit 1; }
command -v gh >/dev/null 2>&1 || { echo "GitHub CLI (gh) is required." >&2; exit 1; }
gh auth status >/dev/null

# Keep tracing disabled so values cannot be emitted by an invoking shell.
set +x
# shellcheck source=load-env-file.sh
. "${SCRIPT_DIR}/load-env-file.sh"
load_env_file "${ENV_FILE}"

secret_names=(
  DOCKERHUB_USERNAME
  DOCKERHUB_TOKEN
  JENKINS_TRIGGER_USER
  JENKINS_TRIGGER_TOKEN
)
variable_names=(
  DOCKERHUB_IMAGE
  JENKINS_STAGING_JOB
  GOOGLE_CLIENT_ID
)

for setting_name in "${secret_names[@]}" "${variable_names[@]}"; do
  [[ -n "${!setting_name:-}" ]] || { echo "Missing required setting: ${setting_name}" >&2; exit 1; }
done

for setting_name in "${secret_names[@]}"; do
  printf '%s' "${!setting_name}" | gh secret set "${setting_name}" --repo "${REPOSITORY}"
  echo "Updated GitHub secret: ${setting_name}"
done

for setting_name in "${variable_names[@]}"; do
  gh variable set "${setting_name}" --repo "${REPOSITORY}" --body "${!setting_name}"
  echo "Updated GitHub variable: ${setting_name}"
done

echo "GitHub Actions settings synchronized for ${REPOSITORY}"
