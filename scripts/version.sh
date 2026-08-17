#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
DEFAULT_VERSION_FILE="${PROJECT_ROOT}/VERSION"

BASE_PATTERN='^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$'
FULL_PATTERN='^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\+gha\.([1-9][0-9]*)\.([0-9a-f]{7})$'
REPOSITORY_PATTERN='^[a-z0-9]+([._-][a-z0-9]+)*/[a-z0-9]+([._-][a-z0-9]+)*$'
DIGEST_PATTERN='^sha256:[0-9a-f]{64}$'
COMMIT_PATTERN='^[0-9a-f]{40}$'

die() {
  echo "$*" >&2
  exit 1
}

validate_base() {
  local version="$1"
  [[ "${version}" =~ ${BASE_PATTERN} ]] || die "Invalid base SemVer: ${version}"
}

validate_full() {
  local version="$1"
  [[ "${version}" =~ ${FULL_PATTERN} ]] || die "Invalid CI SemVer: ${version}"
}

validate_repository() {
  local repository="$1"
  [[ "${repository}" =~ ${REPOSITORY_PATTERN} ]] || die "Invalid Docker Hub repository: ${repository}"
}

validate_digest() {
  local digest="$1"
  [[ "${digest}" =~ ${DIGEST_PATTERN} ]] || die "Invalid image digest: ${digest}"
}

validate_commit() {
  local commit="$1"
  [[ "${commit}" =~ ${COMMIT_PATTERN} ]] || die "Invalid Git commit: ${commit}"
}

read_version() {
  local version_file="${1:-${DEFAULT_VERSION_FILE}}"
  [[ -f "${version_file}" ]] || die "Version file not found: ${version_file}"

  local version actual_length expected_length last_byte
  version="$(<"${version_file}")"
  actual_length="$(wc -c < "${version_file}" | tr -d '[:space:]')"
  expected_length="${#version}"

  if [[ "${actual_length}" -eq $((expected_length + 1)) ]]; then
    last_byte="$(tail -c 1 "${version_file}" | od -An -t u1 | tr -d '[:space:]')"
    [[ "${last_byte}" == "10" ]] || die "${version_file} must contain only MAJOR.MINOR.PATCH"
  elif [[ "${actual_length}" -ne "${expected_length}" ]]; then
    die "${version_file} must contain only MAJOR.MINOR.PATCH"
  fi

  validate_base "${version}"
  printf '%s\n' "${version}"
}

compose_version() {
  local run_number="" commit="" version_file="${DEFAULT_VERSION_FILE}"

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --run-number)
        [[ $# -ge 2 ]] || die "--run-number requires a value"
        run_number="$2"
        shift 2
        ;;
      --commit)
        [[ $# -ge 2 ]] || die "--commit requires a value"
        commit="$2"
        shift 2
        ;;
      --version-file)
        [[ $# -ge 2 ]] || die "--version-file requires a value"
        version_file="$2"
        shift 2
        ;;
      *)
        die "Unknown compose option: $1"
        ;;
    esac
  done

  [[ "${run_number}" =~ ^[1-9][0-9]*$ ]] || die "Invalid GitHub run number: ${run_number}"
  [[ "${commit}" =~ ^[0-9a-f]{7,40}$ ]] || die "Invalid Git commit: ${commit}"

  local base_version full_version
  base_version="$(read_version "${version_file}")"
  full_version="${base_version}+gha.${run_number}.${commit:0:7}"
  validate_full "${full_version}"
  printf '%s\n' "${full_version}"
}

base_from_full() {
  local version="$1"
  validate_full "${version}"
  printf '%s\n' "${version%%+*}"
}

compare_decimal() {
  local left="$1" right="$2"
  if [[ ${#left} -lt ${#right} ]]; then
    echo -1
  elif [[ ${#left} -gt ${#right} ]]; then
    echo 1
  elif [[ "${left}" < "${right}" ]]; then
    echo -1
  elif [[ "${left}" > "${right}" ]]; then
    echo 1
  else
    echo 0
  fi
}

compare_base() {
  local left="$1" right="$2" index comparison
  validate_base "${left}"
  validate_base "${right}"

  local IFS=.
  read -r -a left_parts <<< "${left}"
  read -r -a right_parts <<< "${right}"
  for index in 0 1 2; do
    comparison="$(compare_decimal "${left_parts[$index]}" "${right_parts[$index]}")"
    if [[ "${comparison}" != "0" ]]; then
      printf '%s\n' "${comparison}"
      return
    fi
  done
  echo 0
}

verify_image() {
  local image_ref="" app_version="" git_commit=""

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --image-ref)
        [[ $# -ge 2 ]] || die "--image-ref requires a value"
        image_ref="$2"
        shift 2
        ;;
      --app-version)
        [[ $# -ge 2 ]] || die "--app-version requires a value"
        app_version="$2"
        shift 2
        ;;
      --git-commit)
        [[ $# -ge 2 ]] || die "--git-commit requires a value"
        git_commit="$2"
        shift 2
        ;;
      *)
        die "Unknown verify-image option: $1"
        ;;
    esac
  done

  local repository="${image_ref%@*}" digest="${image_ref#*@}"
  [[ "${image_ref}" == *@* && "${repository}" != "${digest}" ]] || die "Image reference must be repository@sha256:digest"
  validate_repository "${repository}"
  validate_digest "${digest}"
  validate_full "${app_version}"
  validate_commit "${git_commit}"

  command -v docker >/dev/null 2>&1 || die "Docker is required"
  docker pull --quiet "${image_ref}" >/dev/null

  local image_version image_revision
  image_version="$(docker image inspect --format '{{index .Config.Labels "org.opencontainers.image.version"}}' "${image_ref}")"
  image_revision="$(docker image inspect --format '{{index .Config.Labels "org.opencontainers.image.revision"}}' "${image_ref}")"

  [[ "${image_version}" == "${app_version}" ]] || die "Image version label mismatch: expected ${app_version}, found ${image_version}"
  [[ "${image_revision}" == "${git_commit}" ]] || die "Image revision label mismatch: expected ${git_commit}, found ${image_revision}"

  printf 'Verified %s (version %s, revision %s)\n' "${image_ref}" "${app_version}" "${git_commit}"
}

usage() {
  cat <<'EOF'
Usage:
  scripts/version.sh read [VERSION_FILE]
  scripts/version.sh validate-base VERSION
  scripts/version.sh validate-full VERSION
  scripts/version.sh validate-repository NAMESPACE/REPOSITORY
  scripts/version.sh validate-digest SHA256_DIGEST
  scripts/version.sh validate-commit FULL_SHA
  scripts/version.sh compose --run-number NUMBER --commit SHA [--version-file FILE]
  scripts/version.sh base FULL_VERSION
  scripts/version.sh compare-base LEFT RIGHT
  scripts/version.sh verify-image --image-ref REPOSITORY@DIGEST --app-version VERSION --git-commit SHA
EOF
}

command_name="${1:-}"
[[ -n "${command_name}" ]] || { usage >&2; exit 2; }
shift

case "${command_name}" in
  read)
    [[ $# -le 1 ]] || die "read accepts at most one file"
    read_version "${1:-${DEFAULT_VERSION_FILE}}"
    ;;
  validate-base)
    [[ $# -eq 1 ]] || die "validate-base requires one version"
    validate_base "$1"
    ;;
  validate-full)
    [[ $# -eq 1 ]] || die "validate-full requires one version"
    validate_full "$1"
    ;;
  validate-repository)
    [[ $# -eq 1 ]] || die "validate-repository requires one repository"
    validate_repository "$1"
    ;;
  validate-digest)
    [[ $# -eq 1 ]] || die "validate-digest requires one digest"
    validate_digest "$1"
    ;;
  validate-commit)
    [[ $# -eq 1 ]] || die "validate-commit requires one commit"
    validate_commit "$1"
    ;;
  compose)
    compose_version "$@"
    ;;
  base)
    [[ $# -eq 1 ]] || die "base requires one full version"
    base_from_full "$1"
    ;;
  compare-base)
    [[ $# -eq 2 ]] || die "compare-base requires two base versions"
    compare_base "$1" "$2"
    ;;
  verify-image)
    verify_image "$@"
    ;;
  -h|--help|help)
    usage
    ;;
  *)
    usage >&2
    die "Unknown command: ${command_name}"
    ;;
esac
