#!/usr/bin/env bash
#
# Re-tag the just-built versioned Docker images as :latest and push them.
#
# Usage: push-latest-tags.sh <docker-registry-organization> <version>
#
# An empty version is a no-op, so a snapshot build can call this unconditionally
# without :latest drifting onto an unreleased image.
#
# Environment:
#   DOCKER   docker binary to use (tests)
#
set -euo pipefail

ORG="${1:?docker registry organization required}"
VERSION="${2-}"
DOCKER="${DOCKER:-docker}"

if [[ -z "$VERSION" ]]; then
	echo "No release version given; nothing to tag as latest." >&2
	exit 0
fi

for image in stubborn-contract stubborn-contract-stub-runner; do
	"$DOCKER" tag "${ORG}/${image}:${VERSION}" "${ORG}/${image}:latest"
	"$DOCKER" push "${ORG}/${image}:latest"
done
