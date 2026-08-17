#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEST_DIRECTORY="$(mktemp -d)"
trap 'rm -rf -- "${TEST_DIRECTORY}"' EXIT

write_record() {
  local output_file="$1" environment="$2" version="$3" digest_character="$4"
  local digest="sha256:$(printf '%064d' 0 | tr 0 "${digest_character}")"
  jq -n \
    --arg environment "${environment}" \
    --arg digest "${digest}" \
    --arg version "${version}" \
    '{
      schemaVersion: 1,
      environment: $environment,
      imageRepository: "namespace/huff",
      imageDigest: $digest,
      imageRef: ("namespace/huff@" + $digest),
      gitCommit: "a1b2c3d000000000000000000000000000000000",
      appVersion: $version,
      githubRunUrl: "https://github.com/example/huff/actions/runs/1",
      stagedAt: "2026-08-17T12:00:00Z"
    }' > "${output_file}"
}

expect_failure() {
  if "$@" >/dev/null 2>&1; then
    echo "Expected policy check to fail: $*" >&2
    exit 1
  fi
}

candidate="${TEST_DIRECTORY}/candidate.json"
current="${TEST_DIRECTORY}/current.json"

write_record "${candidate}" staging 3.2.1+gha.10.a1b2c3d a
"${SCRIPT_DIR}/check-production-policy.sh" "${candidate}" "${TEST_DIRECTORY}/missing.json" >/dev/null

write_record "${current}" production 3.2.0+gha.9.a1b2c3d b
"${SCRIPT_DIR}/check-production-policy.sh" "${candidate}" "${current}" >/dev/null

write_record "${current}" production 3.2.1+gha.8.a1b2c3d a
"${SCRIPT_DIR}/check-production-policy.sh" "${candidate}" "${current}" >/dev/null

write_record "${current}" production 3.2.1+gha.8.a1b2c3d b
expect_failure "${SCRIPT_DIR}/check-production-policy.sh" "${candidate}" "${current}"

write_record "${current}" production 4.0.0+gha.8.a1b2c3d a
expect_failure "${SCRIPT_DIR}/check-production-policy.sh" "${candidate}" "${current}"

echo "Production policy checks passed"
