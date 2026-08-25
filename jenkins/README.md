# Jenkins deployment setup

The repository defines three Pipeline jobs:

- `huff-staging-deploy`, using `jenkins/Jenkinsfile.staging`;
- `huff-production-deploy`, using `jenkins/Jenkinsfile.production`;
- `huff-container-restart`, using `jenkins/Jenkinsfile.restart`.

All jobs must load their Pipeline from SCM on the `master` branch. Before enabling
them, review and commit every local change and merge that commit into `master`.
Production never accepts an image parameter: it copies `deployment.json` from the
last successful staging build and fingerprints that artifact before approval.

## Host installation

On a supported Debian/Ubuntu host, install Docker first, then run as root:

```bash
jenkins/install-debian.sh
```

The installer follows the Jenkins LTS Debian repository, installs Java 21, sets
one executor, binds Jenkins to `127.0.0.1:8092`, limits the Java heap to 384 MiB,
and grants the `jenkins` account access to the existing Docker group. If the
package exposes `jenkins-plugin-cli`, it also installs the required plugins.
Otherwise, after creating the administrator API token, run:

```bash
JENKINS_USER=huff-admin JENKINS_API_TOKEN=... jenkins/install-plugins.sh
```

The Pipelines keep 30 builds. Deployment does not prune pulled images, so the
last five deployed digests for each environment remain on the host; do not
schedule an unrestricted `docker image prune`.

Install runtime configuration without exposing it in the repository:

```bash
sudo install -o root -g jenkins -m 0640 .env.jenkins /etc/huff/.env.jenkins
sudo install -o root -g jenkins -m 0640 staging.env /etc/huff/staging.env
sudo install -o root -g jenkins -m 0640 production.env /etc/huff/production.env
```

Create `.env.jenkins` from `.env.jenkins.example`. The staging and production
files contain only application runtime settings. Docker Hub is public, so the
Jenkins host has no Docker Hub credential and pulls by digest anonymously.
In Docker Hub, configure tag immutability for the `sha-*` pattern while leaving
only `staging` mutable; the pipelines never use either tag as a deploy target.

After obtaining the TLS certificate, install the Nginx virtual host:

```bash
sudo install -m 0644 jenkins/nginx-ci.hexaquot.it.conf \
  /etc/nginx/conf.d/ci.hexaquot.it.conf
sudo install -m 0644 jenkins/nginx-ci.ottonovembre.it.conf \
  /etc/nginx/conf.d/ci.ottonovembre.it.conf
sudo nginx -t
sudo systemctl reload nginx
```

Set the Jenkins URL to `https://ci.hexaquot.it/`. Keep the default CSRF crumb
issuer enabled. Jenkins API tokens authenticate remote build requests without
disabling CSRF protection.

Before requesting the certificate, create an `A`/`AAAA` record for
`ci.hexaquot.it` pointing to this host. The former `ci.ottonovembre.it` virtual
host remains only as a redirect to the new canonical URL.

## Security configuration

Complete the setup wizard and apply Matrix Authorization with these rules:

1. Create the local administrator named in `JENKINS_PRODUCTION_APPROVERS`.
2. Create `github-actions` (or the value of `JENKINS_TRIGGER_USER`) and generate
   an API token for it.
3. Grant no permissions to anonymous users.
4. Grant the administrator `Overall/Administer`.
5. Grant the technical account global `Overall/Read`, then only `Job/Read` and
   `Job/Build` on `huff-staging-deploy` with project-based matrix authorization.
6. Grant production and restart build permissions only to production admins.
7. On `huff-staging-deploy`, allow `huff-production-deploy` to copy artifacts.

Do not give the technical account build permission on production or restart.
Use its API token as `JENKINS_TRIGGER_TOKEN`, never its password.

## Job behavior

Staging validates all five GitHub parameters, pulls
`IMAGE_REPOSITORY@IMAGE_DIGEST`, verifies OCI version and revision labels,
deploys `huff-hexaquot-staging`, checks local and public readiness, and only then
archives `deployment.json`. Failed jobs archive sanitized inspect state, health
responses, and the last 500 application log lines without container environment
variables.

Production pins the newest successful staging artifact at job start, compares it
with `/var/lib/jenkins/huff/state/production-deployment.json`, displays its
SemVer, commit, digest, staging time and GitHub workflow, then requires a reason,
and the exact text `DEPLOY-PRODUCTION`. It refuses downgrades and refuses reuse
of the same base SemVer with another digest. The first successful run bootstraps
the production state. Repeating the same version/digest is idempotent.

Restart exposes only `staging` (`huff-hexaquot-staging`) and `production`
(`huff-hexaquot`). Production requires a reason, `RESTART-PRODUCTION`, and final
approval. PostgreSQL is never a restart target. Deploy and restart use the same
per-environment locks.

## GitHub settings and smoke test

Synchronize the four Secrets and four Variables without printing values:

```bash
scripts/sync-github-settings.sh --repo OWNER/REPOSITORY \
  --env-file /etc/huff/.env.jenkins
```

Before enabling delivery, manually run the staging job once with a known image
and verify the archived diagnostics. Then exercise the policy tests with:

```bash
scripts/test-version.sh
scripts/jenkins/test-production-policy.sh
```

The production state file is updated only after image validation, deployment,
local readiness, and public URL checks all succeed. There is deliberately no
automatic rollback; use the archived diagnostics to decide the next operation.
