#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

"${PROJECT_ROOT}/scripts/db-huff.sh" query "
ALTER TABLE users ADD COLUMN IF NOT EXISTS star_available boolean;
ALTER TABLE users ADD COLUMN IF NOT EXISTS star_awarded_at varchar(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS star_used_at varchar(255);

UPDATE users
SET star_available = COALESCE(star_available, false);

ALTER TABLE users ALTER COLUMN star_available SET DEFAULT false;
ALTER TABLE users ALTER COLUMN star_available SET NOT NULL;
"
