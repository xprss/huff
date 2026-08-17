#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JENKINS_URL="${JENKINS_URL:-https://ci.ottonovembre.it}"
JENKINS_USER="${JENKINS_USER:-}"
JENKINS_API_TOKEN="${JENKINS_API_TOKEN:-}"

usage() {
  cat <<'EOF'
Usage: JENKINS_USER=ADMIN JENKINS_API_TOKEN=TOKEN jenkins/install-plugins.sh

Optional: set JENKINS_URL. The token is passed only through the process
environment; command tracing remains disabled.
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

[[ -n "${JENKINS_USER}" && -n "${JENKINS_API_TOKEN}" ]] || { usage >&2; exit 2; }
command -v curl >/dev/null 2>&1 || { echo "curl is required" >&2; exit 1; }
command -v java >/dev/null 2>&1 || { echo "Java is required" >&2; exit 1; }

set +x
cli_jar="$(mktemp /tmp/huff-jenkins-cli.XXXXXX.jar)"
auth_file="$(mktemp /tmp/huff-jenkins-auth.XXXXXX)"
chmod 0600 "${auth_file}"
printf '%s:%s' "${JENKINS_USER}" "${JENKINS_API_TOKEN}" > "${auth_file}"
trap 'rm -f -- "${cli_jar}" "${auth_file}"' EXIT
curl --fail --silent --show-error \
  "${JENKINS_URL%/}/jnlpJars/jenkins-cli.jar" \
  -o "${cli_jar}"

mapfile -t plugins < <(sed -E '/^[[:space:]]*(#|$)/d' "${SCRIPT_DIR}/plugins.txt")
java -jar "${cli_jar}" \
  -s "${JENKINS_URL%/}/" \
  -auth "@${auth_file}" \
  install-plugin "${plugins[@]}" -deploy -restart

echo "Requested Jenkins plugin installation and safe restart"
