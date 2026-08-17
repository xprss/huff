#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
VERSION_SCRIPT="${PROJECT_ROOT}/scripts/version.sh"
CANDIDATE_FILE="${1:-}"
CURRENT_FILE="${2:-}"

"${SCRIPT_DIR}/validate-deployment-record.sh" "${CANDIDATE_FILE}" staging >/dev/null

if [[ -z "${CURRENT_FILE}" || ! -f "${CURRENT_FILE}" ]]; then
  echo "No Jenkins production record exists; accepting bootstrap deployment"
  exit 0
fi

"${SCRIPT_DIR}/validate-deployment-record.sh" "${CURRENT_FILE}" production >/dev/null

candidate_version="$(jq -er '.appVersion' "${CANDIDATE_FILE}")"
candidate_digest="$(jq -er '.imageDigest' "${CANDIDATE_FILE}")"
current_version="$(jq -er '.appVersion' "${CURRENT_FILE}")"
current_digest="$(jq -er '.imageDigest' "${CURRENT_FILE}")"
candidate_base="$("${VERSION_SCRIPT}" base "${candidate_version}")"
current_base="$("${VERSION_SCRIPT}" base "${current_version}")"
comparison="$("${VERSION_SCRIPT}" compare-base "${candidate_base}" "${current_base}")"

if [[ "${comparison}" == "-1" ]]; then
  echo "Production downgrade refused: ${candidate_base} is older than ${current_base}" >&2
  exit 1
fi

if [[ "${comparison}" == "0" && "${candidate_digest}" != "${current_digest}" ]]; then
  echo "Production deployment refused: base version ${candidate_base} is already associated with a different digest" >&2
  exit 1
fi

if [[ "${comparison}" == "0" ]]; then
  echo "Idempotent production deployment accepted for ${candidate_base} and ${candidate_digest}"
else
  echo "Production upgrade accepted: ${current_base} -> ${candidate_base}"
fi
