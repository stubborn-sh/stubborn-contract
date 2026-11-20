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
# Exiting non-zero is necessary but not sufficient: GitHub reports a continue-on-error
# step's conclusion as *success* through the REST API and the run's step list, so the
# `::error::` above is visible only to someone who opens the raw log. That is how 0.2.3
# shipped with its bump step reporting success over a 401 — `gh: Bad credentials` — and
# the samples left on 0.2.1 for a second release running. So every outcome, including
# whatever gh said about why, is also written to the run's job summary, which is rendered
# on the run page where nobody has to go looking for it.
#
# Environment:
#   GH_TOKEN      token with permission to dispatch to the samples repo
#                 (secrets.RELEASE_PAT); a missing token is an error, not a skip
#   SAMPLES_REPO  override the target repository (default stubborn-sh/stubborn-samples)
#   GH            override the gh binary (tests)
#   GITHUB_STEP_SUMMARY
#                 file GitHub renders on the run page; written when set, skipped when not
#
set -uo pipefail

VERSION="${1:?Usage: bump-samples.sh <version>}"
SAMPLES_REPO="${SAMPLES_REPO:-stubborn-sh/stubborn-samples}"
GH="${GH:-gh}"
VERSION="${VERSION#v}"

# Append to the run's job summary when running under Actions; a no-op anywhere else.
summary() {
	[[ -n "${GITHUB_STEP_SUMMARY:-}" ]] || return 0
	printf '%s\n' "$@" >>"${GITHUB_STEP_SUMMARY}"
}

# What a human has to do when the dispatch did not happen. One function for both failure
# paths: from the samples repo's point of view they are the same situation.
by_hand() {
	summary "### ⚠️ stubborn-samples was NOT bumped to ${VERSION}" \
		"" \
		"$1" \
		"" \
		"The release itself is unaffected — it is published. But the samples still point at the" \
		"previous version, so nothing has verified them against ${VERSION}." \
		"" \
		"**Pin them by hand:** run the \`Bump stubborn-contract version\` workflow in" \
		"[${SAMPLES_REPO}](https://github.com/${SAMPLES_REPO}/actions/workflows/bump-stubborn-version.yml)" \
		"with version \`${VERSION}\`."
}

if [[ -z "${GH_TOKEN:-}" ]]; then
	echo "::error::No GH_TOKEN — the stubborn-samples bump to ${VERSION} did not happen." >&2
	echo "Run the samples 'Bump stubborn-contract version' workflow by hand to pin them." >&2
	by_hand "No \`GH_TOKEN\` was available, so no dispatch was attempted — check that the secret the release workflow passes to this step still exists."
	exit 1
fi

payload="$(jq -n --arg version "$VERSION" \
	'{event_type: "stubborn-contract-released", client_payload: {version: $version}}')"

# gh's output is captured rather than streamed: it carries the reason (a 401 on an expired
# PAT, a 404 on a repository the token cannot see), and that reason is what the summary
# needs. It is still echoed on both paths, so the log reads as it did before.
if gh_output="$(printf '%s' "$payload" | "$GH" api "repos/${SAMPLES_REPO}/dispatches" --method POST --input - 2>&1)"; then
	[[ -n "$gh_output" ]] && printf '%s\n' "$gh_output"
	echo "Asked ${SAMPLES_REPO} to pin the samples to ${VERSION}."
	summary "### stubborn-samples asked to pin the samples to ${VERSION}" \
		"" \
		"Dispatched \`stubborn-contract-released\` to [${SAMPLES_REPO}](https://github.com/${SAMPLES_REPO}/actions)," \
		"which opens the pin PR there. That PR still has to go green and merge."
	exit 0
fi

[[ -n "$gh_output" ]] && printf '%s\n' "$gh_output" >&2
echo "::error::Could not dispatch the samples bump to ${SAMPLES_REPO} — pin them by hand" \
	"(workflow_dispatch on the samples 'Bump stubborn-contract version' workflow, version ${VERSION})." >&2
by_hand "The dispatch to \`${SAMPLES_REPO}\` failed.${gh_output:+ gh said:}"
[[ -n "$gh_output" ]] && summary "" '```' "${gh_output}" '```'
exit 1
