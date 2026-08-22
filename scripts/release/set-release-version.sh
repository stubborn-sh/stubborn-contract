#!/usr/bin/env bash
# Set the release version across the entire build.
#
#   1. versions:set over the reactor.
#   2. versions:set over stubborn-contract-build (the build parent is NOT a reactor module).
#   3. Finalize the two out-of-reactor <parent> refs that point at stubborn-contract-build
#      (root pom.xml + the BOM) via finalize-parent-version.sh.
#   4. Version-set the Docker reactor poms. The docker modules are ALSO out of the root
#      reactor (commented out of the root <modules> so an ordinary build never drags in the
#      Docker/Confluent toolchain), so the versions:set calls above never reach them. Their
#      own <version> and their <parent> refs would otherwise drift and leave a stale SNAPSHOT
#      parent that fails "Non-resolvable parent POM" once the reactor moves on. Rewrite both
#      with the offline finalize-project-version.sh / finalize-parent-version.sh transforms.
#   5. Rewrite verifierVersion in the Docker image's project/gradle.properties. That file
#      carries CONCRETE versions (not Maven placeholders) and the docker module's Maven
#      drift check (scripts/docker/check-gradle-versions.sh) fails the build if verifierVersion
#      no longer matches project.version, so it must track the release version here.
#
# Must be run from the repository root (uses ./mvnw and repo-relative pom paths).
#
# Usage: set-release-version.sh <release-version>
set -euo pipefail

RELEASE_VERSION="${1:?Usage: set-release-version.sh <release-version>}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 1. Reactor.
./mvnw versions:set -DnewVersion="${RELEASE_VERSION}" \
	-DprocessAllModules=true -DgenerateBackupPoms=false --no-transfer-progress

# 2. stubborn-contract-build (out-of-reactor build parent).
./mvnw versions:set -DnewVersion="${RELEASE_VERSION}" \
	-DprocessAllModules=true -DgenerateBackupPoms=false --no-transfer-progress \
	-f stubborn-contract-build/pom.xml

# 3. Finalize the <parent> refs pointing at stubborn-contract-build. These are the only
#    two poms whose parent is the build parent (root + the BOM).
"${SCRIPT_DIR}/finalize-parent-version.sh" "${RELEASE_VERSION}" \
	pom.xml \
	stubborn-contract-dependencies/pom.xml

# 4. Docker reactor poms (out of the root reactor; see header). Rewrite each pom's own
#    <version> and its <parent> ref with the same offline text transforms used for the other
#    out-of-reactor poms, so the docker parent chain tracks the release version too.
DOCKER_POMS=(
	docker/pom.xml
	docker/stubborn-contract-docker/pom.xml
	docker/stubborn-contract-stub-runner-docker/pom.xml
)
"${SCRIPT_DIR}/finalize-project-version.sh" "${RELEASE_VERSION}" "${DOCKER_POMS[@]}"
"${SCRIPT_DIR}/finalize-parent-version.sh" "${RELEASE_VERSION}" "${DOCKER_POMS[@]}"

# 5. Rewrite verifierVersion in the Docker image's concrete gradle.properties so it tracks
#    project.version (the docker module's Maven drift check enforces the match).
GRADLE_PROPS="docker/stubborn-contract-docker/project/gradle.properties"
sed -i -E "s#^verifierVersion=.*#verifierVersion=${RELEASE_VERSION}#" "${GRADLE_PROPS}"
if ! grep -q "^verifierVersion=${RELEASE_VERSION}$" "${GRADLE_PROPS}"; then
	echo "error: failed to rewrite verifierVersion to ${RELEASE_VERSION} in ${GRADLE_PROPS}" >&2
	exit 1
fi
