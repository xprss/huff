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

bump_repository="${temporary_directory}/bump-repository"
mkdir -p "${bump_repository}/scripts"
cp "${BUMP_VERSION_SCRIPT}" "${VERSION_SCRIPT}" "${bump_repository}/scripts/"
printf '3.2.1\n' > "${bump_repository}/VERSION"
git -C "${bump_repository}" init --quiet
git -C "${bump_repository}" config user.name 'Version test'
git -C "${bump_repository}" config user.email 'version-test@example.invalid'
git -C "${bump_repository}" add VERSION scripts
git -C "${bump_repository}" commit --quiet -m 'initial version'
printf 'must not be committed by a version bump\n' > "${bump_repository}/UNRELATED"
git -C "${bump_repository}" add UNRELATED

bump_version() {
  "${bump_repository}/scripts/bump-version.sh" "$@" >/dev/null
}

expect_commit() {
  [[ "$(git -C "${bump_repository}" log -1 --format=%s)" == "$1" ]]
  [[ "$(git -C "${bump_repository}" show --format= --name-only HEAD)" == VERSION ]]
}

bump_version
[[ "$(<"${bump_repository}/VERSION")" == 3.2.2 ]]
expect_commit 'bump patch 3.2.2'
[[ "$(git -C "${bump_repository}" diff --cached --name-only)" == UNRELATED ]]
bump_version --patch 4
[[ "$(<"${bump_repository}/VERSION")" == 3.2.6 ]]
expect_commit 'bump patch 3.2.6'
bump_version --minor
[[ "$(<"${bump_repository}/VERSION")" == 3.3.0 ]]
expect_commit 'bump minor 3.3.0'
bump_version --minor 7
[[ "$(<"${bump_repository}/VERSION")" == 3.10.0 ]]
expect_commit 'bump minor 3.10.0'
bump_version --major
[[ "$(<"${bump_repository}/VERSION")" == 4.0.0 ]]
expect_commit 'bump major 4.0.0'
bump_version --major 3
[[ "$(<"${bump_repository}/VERSION")" == 7.0.0 ]]
expect_commit 'bump major 7.0.0'
expect_failure "${bump_repository}/scripts/bump-version.sh" --patch 0
expect_failure "${bump_repository}/scripts/bump-version.sh" --patch 1 2
expect_failure "${bump_repository}/scripts/bump-version.sh" --invalid

echo "Version checks passed"
