#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ENV_FILE:-${PROJECT_ROOT}/.env}"
VERSION_FILE="${VERSION_FILE:-${PROJECT_ROOT}/VERSION}"
FRONTEND_VERSION_FILE="${FRONTEND_VERSION_FILE:-${PROJECT_ROOT}/frontend/src/version.ts}"

usage() {
  printf 'Usage: %s [--patch|--minor|--major]\n' "$(basename "$0")"
  printf '\n'
  printf 'Version bump options:\n'
  printf '  --patch  Bump patch version. Default when no option is passed.\n'
  printf '  --minor  Bump minor version and reset patch to 0.\n'
  printf '  --major  Bump major version and reset minor and patch to 0.\n'
}

BUMP_KIND="patch"
BUMP_OPTION_SET="false"

set_bump_kind() {
  local next_kind="$1"
  if [[ "${BUMP_OPTION_SET}" == "true" && "${BUMP_KIND}" != "${next_kind}" ]]; then
    echo "Choose only one version bump option." >&2
    usage >&2
    exit 1
  fi
  BUMP_KIND="${next_kind}"
  BUMP_OPTION_SET="true"
}

while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --patch|patch)
      set_bump_kind "patch"
      ;;
    --minor|minor)
      set_bump_kind "minor"
      ;;
    --major|major)
      set_bump_kind "major"
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
  shift
done

bump_version() {
  local current_version="1.0.0"
  if [[ -f "${VERSION_FILE}" ]]; then
    current_version="$(tr -d '[:space:]' < "${VERSION_FILE}")"
  fi

  if [[ ! "${current_version}" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
    echo "Invalid version in ${VERSION_FILE}: ${current_version}" >&2
    exit 1
  fi

  local major="${BASH_REMATCH[1]}"
  local minor="${BASH_REMATCH[2]}"
  local patch="${BASH_REMATCH[3]}"

  case "${BUMP_KIND}" in
    patch)
      patch=$((patch + 1))
      ;;
    minor)
      minor=$((minor + 1))
      patch=0
      ;;
    major)
      major=$((major + 1))
      minor=0
      patch=0
      ;;
  esac

  APP_VERSION="${major}.${minor}.${patch}"
  printf '%s\n' "${APP_VERSION}" > "${VERSION_FILE}"
  printf 'export const APP_VERSION = "%s";\n' "${APP_VERSION}" > "${FRONTEND_VERSION_FILE}"
  echo "App version: ${current_version} -> ${APP_VERSION} (${BUMP_KIND})"
}

bump_version

if [[ -f "${ENV_FILE}" ]]; then
  set -a
  # shellcheck source=/dev/null
  . "${ENV_FILE}"
  set +a
fi

IMAGE_NAME="${IMAGE_NAME:-huff-italian-hexaquot}"
CONTAINER_NAME="${CONTAINER_NAME:-huff-hexaquot}"
HOST_PORT="${HOST_PORT:-8083}"
CONTAINER_PORT="${CONTAINER_PORT:-8080}"
DATA_DIR="${DATA_DIR:-${PROJECT_ROOT}/data}"
DOCKER_NETWORK="${DOCKER_NETWORK:-huff-hexaquot}"
POSTGRES_CONTAINER_NAME="${POSTGRES_CONTAINER_NAME:-huff-postgres}"
POSTGRES_DATA_DIR="${POSTGRES_DATA_DIR:-${DATA_DIR}/postgres}"
POSTGRES_IMAGE="${POSTGRES_IMAGE:-postgres:16-alpine}"
POSTGRES_DB="${POSTGRES_DB:-huff_hexaquot}"
POSTGRES_USER="${POSTGRES_USER:-huff}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-huff}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_HOST_BIND="${POSTGRES_HOST_BIND:-127.0.0.1}"
POSTGRES_HOST_PORT="${POSTGRES_HOST_PORT:-}"

POSTGRES_PORT_ARGS=()
if [[ -n "${POSTGRES_HOST_PORT}" ]]; then
  POSTGRES_PORT_ARGS=(-p "${POSTGRES_HOST_BIND}:${POSTGRES_HOST_PORT}:5432")
fi

mkdir -p "${DATA_DIR}" "${POSTGRES_DATA_DIR}"

if ! docker network inspect "${DOCKER_NETWORK}" >/dev/null 2>&1; then
  echo "Creating Docker network: ${DOCKER_NETWORK}"
  docker network create "${DOCKER_NETWORK}" >/dev/null
fi

postgres_publish_matches() {
  local published
  published="$(docker port "${POSTGRES_CONTAINER_NAME}" 5432/tcp 2>/dev/null || true)"

  if [[ -z "${POSTGRES_HOST_PORT}" ]]; then
    [[ -z "${published}" ]]
  else
    [[ "${published}" == *"${POSTGRES_HOST_BIND}:${POSTGRES_HOST_PORT}"* ]]
  fi
}

if docker ps -a --format '{{.Names}}' | grep -Fxq "${POSTGRES_CONTAINER_NAME}"; then
  if ! postgres_publish_matches; then
    echo "Recreating PostgreSQL container with updated port publishing: ${POSTGRES_CONTAINER_NAME}"
    docker rm -f "${POSTGRES_CONTAINER_NAME}" >/dev/null
  fi
fi

if docker ps -a --format '{{.Names}}' | grep -Fxq "${POSTGRES_CONTAINER_NAME}"; then
  if ! docker ps --format '{{.Names}}' | grep -Fxq "${POSTGRES_CONTAINER_NAME}"; then
    echo "Starting existing PostgreSQL container: ${POSTGRES_CONTAINER_NAME}"
    docker start "${POSTGRES_CONTAINER_NAME}" >/dev/null
  else
    echo "PostgreSQL container already running: ${POSTGRES_CONTAINER_NAME}"
  fi
else
  echo "Starting PostgreSQL container: ${POSTGRES_CONTAINER_NAME}"
  docker run -d \
    --name "${POSTGRES_CONTAINER_NAME}" \
    --restart unless-stopped \
    --network "${DOCKER_NETWORK}" \
    -e "POSTGRES_DB=${POSTGRES_DB}" \
    -e "POSTGRES_USER=${POSTGRES_USER}" \
    -e "POSTGRES_PASSWORD=${POSTGRES_PASSWORD}" \
    -v "${POSTGRES_DATA_DIR}:/var/lib/postgresql/data" \
    "${POSTGRES_PORT_ARGS[@]}" \
    "${POSTGRES_IMAGE}" >/dev/null
fi

echo "Waiting for PostgreSQL readiness"
for attempt in $(seq 1 45); do
  if docker exec "${POSTGRES_CONTAINER_NAME}" pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" >/dev/null 2>&1; then
    break
  fi
  if [[ "${attempt}" -eq 45 ]]; then
    echo "PostgreSQL did not become ready in time" >&2
    exit 1
  fi
  sleep 1
done

echo "Building ${IMAGE_NAME}:latest from ${PROJECT_ROOT}"
PREVIOUS_IMAGE_ID=""
if docker ps -a --format '{{.Names}}' | grep -Fxq "${CONTAINER_NAME}"; then
  PREVIOUS_IMAGE_ID="$(docker inspect --format '{{.Image}}' "${CONTAINER_NAME}")"
fi

docker build -t "${IMAGE_NAME}:latest" "${PROJECT_ROOT}"
NEW_IMAGE_ID="$(docker image inspect --format '{{.Id}}' "${IMAGE_NAME}:latest")"

if docker ps -a --format '{{.Names}}' | grep -Fxq "${CONTAINER_NAME}"; then
  echo "Stopping and removing existing container: ${CONTAINER_NAME}"
  docker rm -f "${CONTAINER_NAME}"
fi

ENV_ARGS=()
if [[ -f "${ENV_FILE}" ]]; then
  ENV_ARGS+=(--env-file "${ENV_FILE}")
fi

echo "Starting ${CONTAINER_NAME} on 127.0.0.1:${HOST_PORT}->${CONTAINER_PORT}"
docker run -d \
  --name "${CONTAINER_NAME}" \
  --restart unless-stopped \
  --network "${DOCKER_NETWORK}" \
  "${ENV_ARGS[@]}" \
  -e "PORT=${CONTAINER_PORT}" \
  -e "POSTGRES_HOST=${POSTGRES_CONTAINER_NAME}" \
  -e "POSTGRES_PORT=${POSTGRES_PORT}" \
  -e "POSTGRES_DB=${POSTGRES_DB}" \
  -e "POSTGRES_USER=${POSTGRES_USER}" \
  -e "POSTGRES_PASSWORD=${POSTGRES_PASSWORD}" \
  -p "127.0.0.1:${HOST_PORT}:${CONTAINER_PORT}" \
  "${IMAGE_NAME}:latest"

if [[ -n "${PREVIOUS_IMAGE_ID}" && "${PREVIOUS_IMAGE_ID}" != "${NEW_IMAGE_ID}" ]]; then
  echo "Removing previously used image: ${PREVIOUS_IMAGE_ID}"
  docker image rm "${PREVIOUS_IMAGE_ID}" >/dev/null 2>&1 || true
fi

echo "Deployment complete"
docker ps --filter "name=^(${CONTAINER_NAME}|${POSTGRES_CONTAINER_NAME})$" --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
