#!/usr/bin/env bash
#
# Commit the release version bump (already applied to the working tree by
# set-release-version.sh), tag it vX.Y.Z, and push both main and the tag.
# Run by the Release workflow only on a real (non-dry-run) release.
#
# Usage: tag-and-push.sh <version>
#
set -euo pipefail

VERSION="${1:?Usage: tag-and-push.sh <version>}"

git config user.name "github-actions[bot]"
git config user.email "github-actions[bot]@users.noreply.github.com"
git add -A
git commit -m "Release ${VERSION}"
git tag "v${VERSION}"
git push origin main "v${VERSION}"
