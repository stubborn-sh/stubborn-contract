#!/usr/bin/env bash
#
# Decide what publish-docker.yml should publish, for all the ways it can start.
#
#   push to main          the snapshot built from the current commit; the image
#                         tag comes from the POM version, and :latest is left
#                         alone so a snapshot never becomes the default pull
#   push of a vX.Y.Z tag  that release
#   workflow_dispatch     the version given, or the latest GitHub release when
#                         the input is left empty
#   workflow_call         the version the release workflow passes in
#
# Usage: resolve-docker-publish.sh
#
# Emits version=, ref= and push_latest= to $GITHUB_OUTPUT, or to stdout when
# that is unset (the test seam). `version` is empty for a snapshot build.
#
# Environment:
#   GITHUB_EVENT_NAME, GITHUB_REF, GITHUB_SHA, GITHUB_REPOSITORY   from Actions
#   INPUT_VERSION   the dispatch/call input; empty means "the latest release"
#   GH              gh binary to use (tests)
#
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GH="${GH:-gh}"
REPO="${GITHUB_REPOSITORY:-stubborn-sh/stubborn-contract}"
EVENT="${GITHUB_EVENT_NAME:-}"
INPUT_VERSION="${INPUT_VERSION:-}"

fail() {
	echo "$1" >&2
	exit 1
}

version=""
case "$EVENT" in
workflow_dispatch | workflow_call)
	if [[ -n "$INPUT_VERSION" ]]; then
		version="${INPUT_VERSION#v}"
	else
		latest="$("$GH" release view --repo "$REPO" --json tagName --jq .tagName 2>/dev/null || true)"
		[[ -n "$latest" ]] || fail "No published release found in ${REPO}; pass a version explicitly."
		version="${latest#v}"
		echo "Latest release in ${REPO} is ${latest}." >&2
	fi
	;;
*)
	# A tag push carries the version in the ref; anything else is a snapshot.
	version="$("$here/version-from-ref.sh")"
	;;
esac

if [[ -n "$version" ]]; then
	# The value reaches a git ref and a Docker tag, so keep it to characters
	# that are unambiguous in both.
	[[ "$version" =~ ^[0-9A-Za-z][0-9A-Za-z._-]*$ ]] ||
		fail "Refusing suspicious version: ${version}"
	# A snapshot has no tag to check out, and must never take over :latest.
	[[ "$version" != *SNAPSHOT* ]] ||
		fail "Refusing to publish a SNAPSHOT as a release: ${version}"
fi

# On a tag push the tag is self-evidently there; otherwise make a bad version
# fail here with a clear message rather than as a confusing checkout error.
if [[ -n "$version" && "$EVENT" != "push" ]]; then
	"$GH" api "repos/${REPO}/git/ref/tags/v${version}" >/dev/null 2>&1 ||
		fail "Tag v${version} does not exist in ${REPO}."
fi

if [[ -n "$version" ]]; then
	ref="v${version}"
	push_latest="true"
else
	ref="${GITHUB_SHA:-}"
	push_latest="false"
fi

{
	echo "version=${version}"
	echo "ref=${ref}"
	echo "push_latest=${push_latest}"
} >>"${GITHUB_OUTPUT:-/dev/stdout}"
