#!/usr/bin/env bash
#
# Tell stubborn-samples that a release was published, so it pins every sample to
# the released stubborn-contract version and verifies against it (see the samples
# repo's bump-stubborn-version.yml, which opens an auto-merging PR).
#
# Usage: bump-samples.sh <version>
#
# This never fails a release: the samples bump is a follow-up convenience, and it
# can also be run by hand (workflow_dispatch on the samples workflow). A missing
# token or a failed dispatch is reported and shrugged off.
#
# Environment:
#   GH_TOKEN      token with permission to dispatch to the samples repo
#                 (secrets.SAMPLES_DISPATCH_TOKEN); skipped when unset
#   SAMPLES_REPO  override the target repository (default stubborn-sh/stubborn-samples)
#   GH            override the gh binary (tests)
#
set -uo pipefail

VERSION="${1:?Usage: bump-samples.sh <version>}"
SAMPLES_REPO="${SAMPLES_REPO:-stubborn-sh/stubborn-samples}"
GH="${GH:-gh}"
VERSION="${VERSION#v}"

if [[ -z "${GH_TOKEN:-}" ]]; then
	echo "No GH_TOKEN — skipping the stubborn-samples bump to ${VERSION}."
	echo "Run the samples 'Bump stubborn-contract version' workflow by hand to pin them."
	exit 0
fi

payload="$(jq -n --arg version "$VERSION" \
	'{event_type: "stubborn-contract-released", client_payload: {version: $version}}')"

if printf '%s' "$payload" | "$GH" api "repos/${SAMPLES_REPO}/dispatches" --method POST --input -; then
	echo "Asked ${SAMPLES_REPO} to pin the samples to ${VERSION}."
else
	echo "Could not dispatch the samples bump to ${SAMPLES_REPO} — pin them by hand." >&2
fi

exit 0
