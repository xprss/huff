#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ "$(id -u)" -ne 0 ]]; then
  echo "Run this installer as root." >&2
  exit 1
fi

apt-get update
apt-get install -y ca-certificates curl fontconfig gnupg jq nginx openjdk-21-jre

install -d -m 0755 /etc/apt/keyrings
curl --fail --silent --show-error \
  https://pkg.jenkins.io/debian-stable/jenkins.io-2026.key \
  -o /etc/apt/keyrings/jenkins-keyring.asc
printf '%s\n' \
  'deb [signed-by=/etc/apt/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/' \
  > /etc/apt/sources.list.d/jenkins.list

apt-get update
apt-get install -y jenkins

if getent group docker >/dev/null; then
  usermod -aG docker jenkins
else
  echo "Docker is not installed or its group is missing; install Docker before creating the deployment jobs." >&2
fi

install -d -m 0755 /etc/systemd/system/jenkins.service.d
install -m 0644 "${SCRIPT_DIR}/jenkins.service.override.conf" \
  /etc/systemd/system/jenkins.service.d/override.conf
install -d -o jenkins -g jenkins -m 0750 /var/lib/jenkins/init.groovy.d
install -o jenkins -g jenkins -m 0644 "${SCRIPT_DIR}/init.groovy.d/10-executors.groovy" \
  /var/lib/jenkins/init.groovy.d/10-executors.groovy
install -d -o jenkins -g jenkins -m 0750 /var/lib/jenkins/huff/state
install -d -o jenkins -g jenkins -m 0750 /var/lib/jenkins/huff/data/staging
install -d -o root -g jenkins -m 0750 /etc/huff

if command -v jenkins-plugin-cli >/dev/null 2>&1; then
  jenkins-plugin-cli --plugin-file "${SCRIPT_DIR}/plugins.txt"
else
  echo "jenkins-plugin-cli was not found; install the plugins in jenkins/plugins.txt after Jenkins starts." >&2
fi

systemctl daemon-reload
systemctl enable --now jenkins

echo "Jenkins LTS is running on 127.0.0.1:8092. Complete the security and Nginx steps in jenkins/README.md."
