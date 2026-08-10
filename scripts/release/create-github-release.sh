#!/usr/bin/env bash
#
# Create the GitHub Release for a just-pushed release tag, with auto-generated
# notes. Requires the GH_TOKEN environment variable (set by the workflow).
#
# Usage: create-github-release.sh <version>
#
set -euo pipefail

VERSION="${1:?Usage: create-github-release.sh <version>}"

gh release create "v${VERSION}" \
	--title "v${VERSION}" \
	--generate-notes
