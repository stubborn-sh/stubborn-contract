#!/usr/bin/env bash
#
# Print the release version encoded in a git ref of the form refs/tags/vX.Y.Z
# (echoes the part after the leading `v`). Prints nothing for any other ref, so
# callers can test with `[[ -n "$(version-from-ref.sh)" ]]`.
#
# Reads the ref from $1 when given, else $GITHUB_REF.
#
set -euo pipefail

ref="${1:-${GITHUB_REF:-}}"
if [[ "$ref" == refs/tags/v* ]]; then
	echo "${ref#refs/tags/v}"
fi
