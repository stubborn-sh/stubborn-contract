#!/usr/bin/env bash
# SNAPSHOT guard: fail if any -SNAPSHOT version remains in the release-reactor poms.
#
# Every release-reactor pom.xml must be fully finalized before deploying to the immutable
# Central Portal. This fails the release if any -SNAPSHOT survived version-set (e.g. a
# missed parent ref).
#
# Uses `git ls-files`, so it must run inside the repository work tree. Exclusions:
#   *.flattened-pom.xml   build-generated
#   */src/test/*          Maven-plugin test-fixture projects that carry intentional fixed
#                         sample versions (0.1.BUILD-SNAPSHOT, etc.) and are never published
#   docker/               the docker Maven tree is commented out of the reactor and is never
#                         built here; the released image is versioned via Maven-filtered
#                         gradle.properties (verifierVersion=${project.version}), not a literal
#                         SNAPSHOT, so its poms legitimately stay -SNAPSHOT
set -euo pipefail

remaining=$(git ls-files '*pom.xml' \
	':!:*.flattened-pom.xml' \
	':!:*/src/test/*' \
	':!:docker' \
	| xargs grep -l -- '-SNAPSHOT' 2>/dev/null || true)

if [ -n "$remaining" ]; then
	echo "::error::SNAPSHOT versions still present in release-reactor poms after version-set:" >&2
	echo "$remaining" >&2
	echo "$remaining" | xargs grep -n -- '-SNAPSHOT' >&2 || true
	exit 1
fi
echo "No -SNAPSHOT versions remain in release-reactor poms."
