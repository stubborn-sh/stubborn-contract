#!/usr/bin/env bash
# Finalize the <version> inside each given pom's <parent> block to a fixed version.
#
# This is a pure text transform (no Maven, no network) so it can run offline and be
# unit-tested. It exists because `versions:set` only rewrites <parent> refs that live
# INSIDE the reactor. The root pom.xml and the BOM (stubborn-contract-dependencies)
# both declare stubborn-contract-build as their parent, and that parent lives OUTSIDE
# the reactor, so their <parent><version> would otherwise stay at the SNAPSHOT and the
# tagged build would resolve a stale snapshot parent (non-reproducible).
#
# `versions:update-parent` cannot be used here: it needs to read/resolve the
# not-yet-installed release parent + imported BOMs first, which fails. So we edit the
# single <parent> block in each pom directly with a scoped sed.
#
# Usage: finalize-parent-version.sh <version> <pom> [<pom>...]
set -euo pipefail

VERSION="${1:?Usage: finalize-parent-version.sh <version> <pom> [<pom>...]}"
shift
if [ "$#" -lt 1 ]; then
	echo "error: at least one pom path is required" >&2
	exit 2
fi

for pom in "$@"; do
	if [ ! -f "$pom" ]; then
		echo "error: pom not found: $pom" >&2
		exit 1
	fi
	# Only touch the <version> that sits between <parent> and </parent>.
	sed -i -E "/<parent>/,/<\/parent>/ s#<version>[^<]*</version>#<version>${VERSION}</version>#" "$pom"
done
