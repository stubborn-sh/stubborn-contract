#!/usr/bin/env bash
#
# Create the GitHub Release for a just-pushed release tag, with auto-generated
# notes. Requires the GH_TOKEN environment variable (set by the workflow).
#
# Usage: create-github-release.sh <version>
#
set -euo pipefail

VERSION="${1:?Usage: create-github-release.sh <version>}"

# Short "how to use" header prepended to the auto-generated notes. Stubborn Contract
# ships libraries consumed from Maven Central, so the release points there rather than
# attaching JARs — pin the BOM once and every module lines up.
NOTES_FILE="$(mktemp)"
cat >"$NOTES_FILE" <<EOF
## Use it

Pin the BOM once and every Stubborn Contract module lines up:

\`\`\`xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>sh.stubborn</groupId>
      <artifactId>stubborn-contract-dependencies</artifactId>
      <version>${VERSION}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
\`\`\`

All artifacts are on [Maven Central](https://central.sonatype.com/namespace/sh.stubborn).
EOF

# Idempotent: this script is also dispatchable on its own (publish-github-release.yml),
# so a re-run after a partial release must not fail on an existing release.
if gh release view "v${VERSION}" >/dev/null 2>&1; then
	echo "Release v${VERSION} already exists — nothing to do."
else
	gh release create "v${VERSION}" \
		--title "v${VERSION}" \
		--notes-file "$NOTES_FILE" \
		--generate-notes
fi
