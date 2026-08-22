#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSION_SCRIPT="${SCRIPT_DIR}/version.sh"
BUMP_VERSION_SCRIPT="${SCRIPT_DIR}/bump-version.sh"

expect_failure() {
  if "$@" >/dev/null 2>&1; then
    echo "Expected command to fail: $*" >&2
    exit 1
  fi
}

"${VERSION_SCRIPT}" validate-base 0.0.0
"${VERSION_SCRIPT}" validate-base 3.2.1
expect_failure "${VERSION_SCRIPT}" validate-base v3.2.1
expect_failure "${VERSION_SCRIPT}" validate-base 03.2.1
expect_failure "${VERSION_SCRIPT}" validate-base 3.2.1-extra
"${VERSION_SCRIPT}" validate-repository namespace/huff
"${VERSION_SCRIPT}" validate-digest sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
"${VERSION_SCRIPT}" validate-commit a1b2c3d000000000000000000000000000000000
expect_failure "${VERSION_SCRIPT}" validate-repository namespace/Huff
expect_failure "${VERSION_SCRIPT}" validate-digest sha256:abc

full_version="$("${VERSION_SCRIPT}" compose --run-number 184 --commit a1b2c3d000000000000000000000000000000000)"
[[ "${full_version}" == "$("${VERSION_SCRIPT}" read)+gha.184.a1b2c3d" ]]
"${VERSION_SCRIPT}" validate-full "${full_version}"
[[ "$("${VERSION_SCRIPT}" base "${full_version}")" == "$("${VERSION_SCRIPT}" read)" ]]

[[ "$("${VERSION_SCRIPT}" compare-base 3.2.1 3.2.1)" == 0 ]]
[[ "$("${VERSION_SCRIPT}" compare-base 3.2.0 3.2.1)" == -1 ]]
[[ "$("${VERSION_SCRIPT}" compare-base 4.0.0 3.99.99)" == 1 ]]

temporary_directory="$(mktemp -d)"
trap 'rm -rf -- "${temporary_directory}"' EXIT
printf '3.2.1\n\n' > "${temporary_directory}/VERSION"
expect_failure "${VERSION_SCRIPT}" read "${temporary_directory}/VERSION"

bump_version_file="${temporary_directory}/BUMP_VERSION"
printf '3.2.1\n' > "${bump_version_file}"

bump_version() {
  VERSION_FILE="${bump_version_file}" "${BUMP_VERSION_SCRIPT}" "$@" >/dev/null
}

bump_version --patch
[[ "$(<"${bump_version_file}")" == 3.2.2 ]]
bump_version --patch 4
[[ "$(<"${bump_version_file}")" == 3.2.6 ]]
bump_version --minor
[[ "$(<"${bump_version_file}")" == 3.3.0 ]]
bump_version --minor 7
[[ "$(<"${bump_version_file}")" == 3.10.0 ]]
bump_version --major
[[ "$(<"${bump_version_file}")" == 4.0.0 ]]
bump_version --major 3
[[ "$(<"${bump_version_file}")" == 7.0.0 ]]
expect_failure env VERSION_FILE="${bump_version_file}" "${BUMP_VERSION_SCRIPT}" --patch 0
expect_failure env VERSION_FILE="${bump_version_file}" "${BUMP_VERSION_SCRIPT}" --patch 1 2
expect_failure env VERSION_FILE="${bump_version_file}" "${BUMP_VERSION_SCRIPT}" --invalid

echo "Version checks passed"
