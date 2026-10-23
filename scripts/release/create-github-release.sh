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
REPO="${GITHUB_REPOSITORY:-stubborn-sh/stubborn-contract}"
NOTES_DIR="${NOTES_DIR:-changelog}"
DEFAULT_BRANCH="${DEFAULT_BRANCH:-main}"

# The human-written part of the release notes: highlights, upgrade notes, anything a
# reader needs that a list of merged PRs does not convey. Written as changelog/<version>.md
# in the repository, so it is reviewed in the PR that earns it rather than invented at
# release time — and so the stubborn.sh blog post, which is generated from this release
# body, says something useful.
#
# Normally the file is already committed when the tag is cut, so it is simply in the
# working tree. When this script is re-dispatched for an older tag (publish-github-release.yml
# checks out v<version>), the file may have been added to the default branch afterwards —
# fall back to reading it from there.
narrative=""
if [[ -f "${NOTES_DIR}/${VERSION}.md" ]]; then
	narrative="$(cat "${NOTES_DIR}/${VERSION}.md")"
	echo "Using release notes from ${NOTES_DIR}/${VERSION}.md"
elif git rev-parse --git-dir >/dev/null 2>&1; then
	git fetch --depth=1 origin "$DEFAULT_BRANCH" >/dev/null 2>&1 || true
	narrative="$(git show "FETCH_HEAD:${NOTES_DIR}/${VERSION}.md" 2>/dev/null || true)"
	[[ -n "$narrative" ]] && echo "Using release notes from ${DEFAULT_BRANCH}:${NOTES_DIR}/${VERSION}.md"
fi
if [[ -z "$narrative" ]]; then
	echo "No ${NOTES_DIR}/${VERSION}.md — the release notes will be the boilerplate plus the generated changelog." >&2
else
	printf '%s\n\n' "$narrative" >"$NOTES_FILE"
fi

cat >>"$NOTES_FILE" <<EOF
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
	echo "Release v${VERSION} already exists — updating its notes and assets."
	# Keep the generated "What's Changed" list that --generate-notes produced when the
	# release was first created; only the part above it is ours to rewrite. If the existing
	# body has no such section (a hand-made release), ask GitHub to generate one.
	existing="$(gh release view "v${VERSION}" --json body --jq .body 2>/dev/null || true)"
	generated="$(printf '%s' "$existing" | sed -n "/^## What's Changed/,\$p")"
	if [[ -z "$generated" ]]; then
		generated="$(gh api "repos/${REPO}/releases/generate-notes" -f tag_name="v${VERSION}" --jq .body 2>/dev/null || true)"
	fi
	[[ -n "$generated" ]] && printf '\n%s\n' "$generated" >>"$NOTES_FILE"
	gh release edit "v${VERSION}" --notes-file "$NOTES_FILE"
else
	gh release create "v${VERSION}" \
		--title "v${VERSION}" \
		--notes-file "$NOTES_FILE" \
		--generate-notes
fi

if [[ ${#ASSETS[@]} -gt 0 ]]; then
	gh release upload "v${VERSION}" "${ASSETS[@]}" --clobber
fi
