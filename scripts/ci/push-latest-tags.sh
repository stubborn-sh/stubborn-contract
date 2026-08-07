#!/usr/bin/env bash
#
# Re-tag the just-built versioned Docker images as :latest and push them. Only
# meaningful on a release-tag build; on any other ref it is a no-op. Reads the
# version from $GITHUB_REF (refs/tags/vX.Y.Z).
#
# Usage: push-latest-tags.sh <docker-registry-organization>
#
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ORG="${1:?docker registry organization required}"
VERSION="$("$here/version-from-ref.sh")"

if [[ -z "$VERSION" ]]; then
	echo "Not a release tag ref (${GITHUB_REF:-unset}); nothing to tag as latest." >&2
	exit 0
fi

for image in stubborn-contract stubborn-contract-stub-runner; do
	docker tag "${ORG}/${image}:${VERSION}" "${ORG}/${image}:latest"
	docker push "${ORG}/${image}:latest"
done
