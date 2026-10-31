#!/usr/bin/env bash
#
# Tell stubborn-samples that a release was published, so it pins every sample to
# the released stubborn-contract version and verifies against it (see the samples
# repo's bump-stubborn-version.yml, which opens an auto-merging PR).
#
# Usage: bump-samples.sh <version>
#
# This exits non-zero when the dispatch does not happen, and the release workflow marks
# the step continue-on-error. So a release that has already published is never failed
# retroactively, but the step goes red rather than reporting success over a bump that did
# nothing — which is what happened for 0.2.2: the dispatch failed, the `|| echo` swallowed
# it, the step was green, and the samples stayed on 0.2.1.
#
# Environment:
#   GH_TOKEN      token with permission to dispatch to the samples repo
#                 (secrets.RELEASE_PAT); a missing token is an error, not a skip
#   SAMPLES_REPO  override the target repository (default stubborn-sh/stubborn-samples)
#   GH            override the gh binary (tests)
#
set -uo pipefail

VERSION="${1:?Usage: bump-samples.sh <version>}"
SAMPLES_REPO="${SAMPLES_REPO:-stubborn-sh/stubborn-samples}"
GH="${GH:-gh}"
VERSION="${VERSION#v}"

if [[ -z "${GH_TOKEN:-}" ]]; then
	echo "::error::No GH_TOKEN — the stubborn-samples bump to ${VERSION} did not happen." >&2
	echo "Run the samples 'Bump stubborn-contract version' workflow by hand to pin them." >&2
	exit 1
fi

payload="$(jq -n --arg version "$VERSION" \
	'{event_type: "stubborn-contract-released", client_payload: {version: $version}}')"

if printf '%s' "$payload" | "$GH" api "repos/${SAMPLES_REPO}/dispatches" --method POST --input -; then
	echo "Asked ${SAMPLES_REPO} to pin the samples to ${VERSION}."
	exit 0
fi

echo "::error::Could not dispatch the samples bump to ${SAMPLES_REPO} — pin them by hand" \
	"(workflow_dispatch on the samples 'Bump stubborn-contract version' workflow, version ${VERSION})." >&2
exit 1
