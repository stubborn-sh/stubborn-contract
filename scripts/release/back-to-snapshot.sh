#!/usr/bin/env bash
#
# Restore the next -SNAPSHOT version after a release and push it to main.
#
# This reuses set-release-version.sh rather than a bare `versions:set` on the
# poms, so the SAME set of files stays in sync as during the release: the
# reactor, the out-of-reactor build parent, the two parent <version> refs, AND
# the Docker image's concrete project/gradle.properties. The Docker module's
# drift check (verifierVersion vs project.version) runs on the next main /
# publish-docker build, so leaving gradle.properties on the old release version
# here would break that build.
#
# Usage: back-to-snapshot.sh <next-snapshot-version>
#
set -euo pipefail

NEXT="${1:?Usage: back-to-snapshot.sh <next-snapshot-version>}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

"${SCRIPT_DIR}/set-release-version.sh" "${NEXT}"

git config user.name "github-actions[bot]"
git config user.email "github-actions[bot]@users.noreply.github.com"
git add -A
git commit -m "Back to ${NEXT}"
git push origin main
