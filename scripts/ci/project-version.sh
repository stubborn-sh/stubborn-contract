#!/usr/bin/env bash
#
# Print the Maven project version of a POM — the tag the Docker images are
# built with, and so the tag the publish must be verified against.
#
# Usage: project-version.sh <pom-path>
#
# Emits version=<v> to $GITHUB_OUTPUT when set; always prints the bare version
# to stdout (the test seam).
#
# Environment:
#   MVN   maven wrapper to use (tests)
#
set -euo pipefail

POM="${1:?pom path required}"
MVN="${MVN:-./mvnw}"

version="$("$MVN" -q -N help:evaluate -Dexpression=project.version -DforceStdout -f "$POM")"
version="$(echo "$version" | tr -d '[:space:]')"

if [[ -z "$version" ]]; then
	echo "Could not resolve project.version from ${POM}" >&2
	exit 1
fi

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
	echo "version=${version}" >>"$GITHUB_OUTPUT"
fi

echo "$version"
