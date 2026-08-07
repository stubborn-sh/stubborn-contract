#!/usr/bin/env bash
#
# Compute the Docker image tag list for a build. On a refs/tags/vX.Y.Z ref the
# tags are "<version> latest"; on any other ref just "latest". Emits `tags=...`
# to $GITHUB_OUTPUT when set, else prints it to stdout (the BATS test seam).
#
# Reads the ref from $1 when given, else $GITHUB_REF.
#
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
version="$("$here/version-from-ref.sh" "${1:-${GITHUB_REF:-}}")"

if [[ -n "$version" ]]; then
	tags="$version latest"
else
	tags="latest"
fi

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
	echo "tags=$tags" >>"$GITHUB_OUTPUT"
else
	echo "tags=$tags"
fi
