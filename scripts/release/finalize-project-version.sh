#!/usr/bin/env bash
# Finalize a pom's OWN <version> (the direct child of <project>) to a fixed version.
#
# Companion to finalize-parent-version.sh, and like it a pure text transform (no Maven,
# no network) so it runs offline and is unit-tested. It exists because `versions:set` only
# rewrites poms it reaches through the reactor. The Docker modules live OUTSIDE the root
# reactor (they are commented out of the root <modules> so an ordinary build never drags in
# the Docker/Confluent toolchain), so versions:set never touches their <version>, which would
# otherwise stay at the previous value while the rest of the build moves on.
#
# The project version is the FIRST <version> that appears after the </parent> block: Maven
# orders <parent> before the project's own groupId/artifactId/version, and dependency/plugin
# <version>s come later still, so they are left untouched. Each target pom therefore MUST
# declare a <parent>.
#
# Usage: finalize-project-version.sh <version> <pom> [<pom>...]
set -euo pipefail

VERSION="${1:?Usage: finalize-project-version.sh <version> <pom> [<pom>...]}"
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
	if ! grep -q "</parent>" "$pom"; then
		echo "error: $pom has no <parent> block; cannot locate the project <version>" >&2
		exit 1
	fi
	awk -v ver="$VERSION" '
		/<\/parent>/ { after_parent = 1 }
		after_parent && !done && /<version>[^<]*<\/version>/ {
			sub(/<version>[^<]*<\/version>/, "<version>" ver "</version>")
			done = 1
		}
		{ print }
	' "$pom" > "$pom.tmp"
	mv "$pom.tmp" "$pom"
done
