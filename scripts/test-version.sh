#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSION_SCRIPT="${SCRIPT_DIR}/version.sh"

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

echo "Version checks passed"
