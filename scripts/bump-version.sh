#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
VERSION_SCRIPT="${SCRIPT_DIR}/version.sh"
VERSION_FILE="${VERSION_FILE:-${PROJECT_ROOT}/VERSION}"

usage() {
  cat <<'EOF'
Usage: scripts/bump-version.sh [--patch|--minor|--major] [DELTA]

Updates VERSION according to Semantic Versioning. DELTA must be a positive
integer and defaults to 1. With no arguments, increments PATCH by 1.
After updating VERSION, creates a commit containing only that file.

Examples:
  scripts/bump-version.sh               # 3.0.8 -> 3.0.9
  scripts/bump-version.sh --patch       # 3.0.8 -> 3.0.9
  scripts/bump-version.sh --minor 2     # 3.0.8 -> 3.2.0
  scripts/bump-version.sh --major       # 3.0.8 -> 4.0.0
EOF
}

die() {
  echo "$*" >&2
  exit 2
}

add_decimal() {
  local left="$1" right="$2" carry=0 result=""
  local left_index=$(( ${#left} - 1 )) right_index=$(( ${#right} - 1 ))
  local left_digit right_digit sum

  while (( left_index >= 0 || right_index >= 0 || carry > 0 )); do
    left_digit=0
    right_digit=0
    if (( left_index >= 0 )); then
      left_digit=$((10#${left:left_index:1}))
      left_index=$((left_index - 1))
    fi
    if (( right_index >= 0 )); then
      right_digit=$((10#${right:right_index:1}))
      right_index=$((right_index - 1))
    fi

    sum=$((left_digit + right_digit + carry))
    result="$((sum % 10))${result}"
    carry=$((sum / 10))
  done

  printf '%s\n' "${result}"
}

if [[ $# -eq 0 ]]; then
  bump_type="--patch"
else
  bump_type="$1"
  shift
fi
case "${bump_type}" in
  --patch|--minor|--major) ;;
  -h|--help|help)
    [[ $# -eq 0 ]] || die "Help does not accept arguments"
    usage
    exit 0
    ;;
  *)
    usage >&2
    die "Choose exactly one of --patch, --minor, or --major"
    ;;
esac

[[ $# -le 1 ]] || die "Only one optional DELTA is allowed"
delta="${1:-1}"
[[ "${delta}" =~ ^[1-9][0-9]*$ ]] || die "DELTA must be a positive integer"

current_version="$("${VERSION_SCRIPT}" read "${VERSION_FILE}")"
IFS=. read -r major minor patch <<< "${current_version}"

case "${bump_type}" in
  --patch)
    next_version="${major}.${minor}.$(add_decimal "${patch}" "${delta}")"
    ;;
  --minor)
    next_version="${major}.$(add_decimal "${minor}" "${delta}").0"
    ;;
  --major)
    next_version="$(add_decimal "${major}" "${delta}").0.0"
    ;;
esac

"${VERSION_SCRIPT}" validate-base "${next_version}"

temporary_file="$(mktemp "${VERSION_FILE}.tmp.XXXXXX")"
trap 'rm -f -- "${temporary_file}"' EXIT
printf '%s\n' "${next_version}" > "${temporary_file}"
chmod --reference="${VERSION_FILE}" "${temporary_file}"
mv -f -- "${temporary_file}" "${VERSION_FILE}"
trap - EXIT

commit_type="${bump_type#--}"
git -C "${PROJECT_ROOT}" add -- "${VERSION_FILE}"
git -C "${PROJECT_ROOT}" commit --only \
  -m "bump ${commit_type} ${next_version}" -- "${VERSION_FILE}"

printf 'Bumped version: %s -> %s\n' "${current_version}" "${next_version}"
