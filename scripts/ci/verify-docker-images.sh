#!/usr/bin/env bash
#
# Assert that the Docker images this build was supposed to publish are actually
# in the registry.
#
# This exists because they were not, for 139 consecutive green runs: the push
# goal was bound to a lifecycle phase the workflow never ran, so Maven built the
# images, pushed nothing, and exited 0. A publish workflow that cannot fail is
# not a publish workflow, so the run now ends by asking the registry.
#
# `docker manifest inspect` queries the registry, not the local daemon, so a
# locally-built image cannot make this pass.
#
# Usage: verify-docker-images.sh <docker-registry-organization> <tag> [tag...]
#
# Environment:
#   DOCKER   docker binary to use (tests)
#
set -euo pipefail

ORG="${1:?docker registry organization required}"
shift
TAGS=("$@")
DOCKER="${DOCKER:-docker}"
IMAGES=(stubborn-contract stubborn-contract-stub-runner)

if [[ ${#TAGS[@]} -eq 0 ]]; then
	echo "At least one tag to verify is required." >&2
	exit 1
fi

missing=()
for image in "${IMAGES[@]}"; do
	for tag in "${TAGS[@]}"; do
		reference="${ORG}/${image}:${tag}"
		if "$DOCKER" manifest inspect "$reference" >/dev/null 2>&1; then
			echo "found in registry: ${reference}"
		else
			echo "MISSING from registry: ${reference}" >&2
			missing+=("$reference")
		fi
	done
done

if [[ ${#missing[@]} -gt 0 ]]; then
	echo "" >&2
	echo "The build reported success but ${#missing[@]} image(s) are not published:" >&2
	printf '  %s\n' "${missing[@]}" >&2
	exit 1
fi

echo "All ${#IMAGES[@]} image(s) published for: ${TAGS[*]}"
