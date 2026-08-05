#!/usr/bin/env bash

load_env_file() {
  local env_file="$1"
  local line key value

  while IFS= read -r line || [[ -n "${line}" ]]; do
    line="${line%$'\r'}"
    if [[ -z "${line//[[:space:]]/}" || "${line}" =~ ^[[:space:]]*# ]]; then
      continue
    fi
    if [[ ! "${line}" =~ ^([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
      echo "Invalid environment entry in ${env_file}: ${line}" >&2
      return 2
    fi

    key="${BASH_REMATCH[1]}"
    value="${BASH_REMATCH[2]}"
    export "${key}=${value}"
  done < "${env_file}"
}
