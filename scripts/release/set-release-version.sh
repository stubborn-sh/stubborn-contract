#!/usr/bin/env bash
# Set the release version across the entire build.
#
#   1. versions:set over the reactor.
#   2. versions:set over stubborn-contract-build (the build parent is NOT a reactor module).
#   3. Finalize the two out-of-reactor <parent> refs that point at stubborn-contract-build
#      (root pom.xml + the BOM) via finalize-parent-version.sh.
#
# NOTE: This script intentionally does NOT touch the Docker image versions. Those are
# Maven-filtered from gradle.properties at docker build time (verifierVersion=${project.version},
# camelVersion=${camel.version}), so there is nothing to sed here anymore.
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
