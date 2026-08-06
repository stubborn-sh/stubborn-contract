#!/usr/bin/env bash
# Echo "true" if the current project version ends in -SNAPSHOT, otherwise "false".
#
# Used by publish-snapshots.yml: the Release workflow pushes a transient non-SNAPSHOT
# "Release x.y.z" commit to main before restoring the next SNAPSHOT. On that commit a
# snapshot publish is a no-op, so the workflow records this result and SKIPS the publish
# job rather than failing red.
#
# Reads the first <version> in the pom (default ./pom.xml, matching the reactor root).
#
# Usage: is-snapshot.sh [<pom>]
set -euo pipefail

POM="${1:-pom.xml}"

VERSION=$(grep -m1 '<version>' "$POM" | sed 's/.*<version>\(.*\)<\/version>.*/\1/')

if [[ "$VERSION" == *-SNAPSHOT ]]; then
	echo "true"
else
	echo "false"
fi
