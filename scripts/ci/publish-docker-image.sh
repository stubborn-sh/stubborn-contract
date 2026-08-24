#!/usr/bin/env bash
#
# Build one Docker module's image and push it to the registry.
#
# Runs `deploy`, not `package`: dockerfile-maven-plugin binds `build` to the
# package phase but `push` to the deploy phase, so `package` builds the image
# and silently pushes nothing. Artifact deployment is not wanted here and is
# skipped explicitly — the deploy phase is only being run to reach the push.
#
# Usage: publish-docker-image.sh <pom-path>
#
# Environment:
#   DOCKER_ORG            registry organization (required)
#   DOCKER_HUB_USERNAME   passed through to the plugin
#   DOCKER_HUB_PASSWORD   passed through to the plugin
#   MVN                   maven wrapper to use (tests)
#
set -euo pipefail

POM="${1:?pom path required}"
ORG="${DOCKER_ORG:?DOCKER_ORG is required}"
MVN="${MVN:-./mvnw}"

"$MVN" deploy \
	-f "$POM" \
	-Ddocker.registry.organization="$ORG" \
	-Dmaven.deploy.skip=true \
	-DskipTests \
	--no-transfer-progress
