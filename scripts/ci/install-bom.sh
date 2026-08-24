#!/usr/bin/env bash
#
# Install the consumer BOM into the local repository.
#
# The .lastUpdated sweep first: once Maven has recorded a failed resolution for
# an sh.stubborn artifact it will not retry within the update interval, and the
# reactor build that follows then fails to resolve modules it is about to build.
#
# Usage: install-bom.sh
#
# Environment:
#   MVN         maven wrapper to use (tests)
#   M2_REPO     local repository path (tests); defaults to ~/.m2/repository
#
set -euo pipefail

MVN="${MVN:-./mvnw}"
M2_REPO="${M2_REPO:-${HOME}/.m2/repository}"

if [[ -d "${M2_REPO}/sh/stubborn" ]]; then
	find "${M2_REPO}/sh/stubborn" -name "*.lastUpdated" -delete
fi

"$MVN" install -N -f stubborn-contract-dependencies/pom.xml -DskipTests --no-transfer-progress
