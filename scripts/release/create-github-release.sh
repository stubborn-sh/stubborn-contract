#!/usr/bin/env bash
#
# Create the GitHub Release for a just-pushed release tag, with auto-generated
# notes. Requires the GH_TOKEN environment variable (set by the workflow).
#
# Usage: create-github-release.sh <version>
#
# The libraries are consumed from Maven Central (pin the BOM once). The one thing
# that does NOT belong on Central is the standalone stub-runner application's
# executable ("fat") jar — it bundles Spring Boot plus every binder and would blow
# the Central per-deployment quota. That jar is built at
#   stubborn-contract-stub-runner-app/target/stubborn-contract-stub-runner-app-<version>-exec.jar
# (repackage runs with a classifier and attach=false, so it is neither installed nor
# deployed) and is attached to the GitHub Release here instead.
#
set -euo pipefail

VERSION="${1:?Usage: create-github-release.sh <version>}"

EXEC_JAR="stubborn-contract-stub-runner-app/target/stubborn-contract-stub-runner-app-${VERSION}-exec.jar"
ASSETS=()
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

All libraries are on [Maven Central](https://central.sonatype.com/namespace/sh.stubborn).
EOF

# Attach the standalone stub-runner executable jar (and its checksum) when it is
# present in the workspace. It is intentionally not on Maven Central.
if [[ -f "$EXEC_JAR" ]]; then
	CHECKSUM_FILE="${EXEC_JAR}.sha256"
	(cd "$(dirname "$EXEC_JAR")" && sha256sum "$(basename "$EXEC_JAR")" >"$(basename "$CHECKSUM_FILE")")
	ASSETS+=("$EXEC_JAR" "$CHECKSUM_FILE")
	cat >>"$NOTES_FILE" <<EOF

## Standalone stub runner

Prefer running the stub runner as a server? Download
\`stubborn-contract-stub-runner-app-${VERSION}-exec.jar\` below (SHA-256 alongside) and run:

\`\`\`bash
java -jar stubborn-contract-stub-runner-app-${VERSION}-exec.jar
\`\`\`

This executable jar is published here rather than to Maven Central.
EOF
else
	echo "Note: executable jar not found at ${EXEC_JAR} — creating the release without it." >&2
fi

# Idempotent: this script is also dispatchable on its own (publish-github-release.yml),
# so a re-run after a partial release must not fail on an existing release.
if gh release view "v${VERSION}" >/dev/null 2>&1; then
	echo "Release v${VERSION} already exists — updating assets."
else
	gh release create "v${VERSION}" \
		--title "v${VERSION}" \
		--notes-file "$NOTES_FILE" \
		--generate-notes
fi

if [[ ${#ASSETS[@]} -gt 0 ]]; then
	gh release upload "v${VERSION}" "${ASSETS[@]}" --clobber
fi
