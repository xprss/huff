#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
VERSION_SCRIPT="${PROJECT_ROOT}/scripts/version.sh"
RECORD_FILE="${1:-}"
EXPECTED_ENVIRONMENT="${2:-}"

[[ -f "${RECORD_FILE}" ]] || { echo "Deployment record not found: ${RECORD_FILE}" >&2; exit 1; }
[[ "${EXPECTED_ENVIRONMENT}" == "staging" || "${EXPECTED_ENVIRONMENT}" == "production" ]] || {
  echo "Expected environment must be staging or production" >&2
  exit 2
}
command -v jq >/dev/null 2>&1 || { echo "jq is required" >&2; exit 1; }

schema_version="$(jq -er '.schemaVersion' "${RECORD_FILE}")"
environment="$(jq -er '.environment' "${RECORD_FILE}")"
repository="$(jq -er '.imageRepository' "${RECORD_FILE}")"
digest="$(jq -er '.imageDigest' "${RECORD_FILE}")"
image_ref="$(jq -er '.imageRef' "${RECORD_FILE}")"
commit="$(jq -er '.gitCommit' "${RECORD_FILE}")"
version="$(jq -er '.appVersion' "${RECORD_FILE}")"
workflow_url="$(jq -er '.githubRunUrl' "${RECORD_FILE}")"
staged_at="$(jq -er '.stagedAt' "${RECORD_FILE}")"

[[ "${schema_version}" == "1" ]] || { echo "Unsupported deployment record schema" >&2; exit 1; }
[[ "${environment}" == "${EXPECTED_ENVIRONMENT}" ]] || { echo "Deployment environment mismatch" >&2; exit 1; }
"${VERSION_SCRIPT}" validate-repository "${repository}"
"${VERSION_SCRIPT}" validate-digest "${digest}"
"${VERSION_SCRIPT}" validate-commit "${commit}"
"${VERSION_SCRIPT}" validate-full "${version}"
[[ "${image_ref}" == "${repository}@${digest}" ]] || { echo "Deployment image reference mismatch" >&2; exit 1; }
[[ "${workflow_url}" =~ ^https://github\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+/actions/runs/[0-9]+$ ]] || {
  echo "Invalid workflow URL in deployment record" >&2
  exit 1
}
[[ "${staged_at}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z$ ]] || {
  echo "Invalid staging timestamp" >&2
  exit 1
}

echo "Deployment record is valid"
