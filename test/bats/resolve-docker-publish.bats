#!/usr/bin/env bats
#
# Tests for scripts/ci/resolve-docker-publish.sh — what publish-docker.yml
# publishes, for each of the ways that workflow can start.

setup() {
	SCRIPT="${BATS_TEST_DIRNAME}/../../scripts/ci/resolve-docker-publish.sh"
	TMP="$(mktemp -d)"

	# Stub gh: `release view` answers with $GH_LATEST_TAG, `api` succeeds unless
	# the tag is listed in $GH_MISSING_TAGS.
	cat >"$TMP/gh" <<-'STUB'
		#!/usr/bin/env bash
		case "$1" in
		release)
			[[ -n "${GH_LATEST_TAG:-}" ]] || exit 1
			echo "$GH_LATEST_TAG"
			;;
		api)
			for missing in ${GH_MISSING_TAGS:-}; do
				[[ "$2" == *"$missing" ]] && exit 1
			done
			echo "{}"
			;;
		esac
		exit 0
	STUB
	chmod +x "$TMP/gh"

	export GH="$TMP/gh"
	export GITHUB_REPOSITORY=stubborn-sh/stubborn-contract
	unset GITHUB_OUTPUT GITHUB_REF GITHUB_SHA GITHUB_EVENT_NAME INPUT_VERSION
	unset GH_LATEST_TAG GH_MISSING_TAGS
}

teardown() {
	rm -rf "$TMP"
}

field() {
	echo "$output" | grep "^$1=" | cut -d= -f2-
}

@test "resolve: a push to main publishes a snapshot and leaves latest alone" {
	GITHUB_EVENT_NAME=push GITHUB_REF=refs/heads/main GITHUB_SHA=abc123 run "$SCRIPT"
	[ "$status" -eq 0 ]
	[ -z "$(field version)" ]
	[ "$(field ref)" = "abc123" ]
	[ "$(field push_latest)" = "false" ]
}

@test "resolve: a tag push publishes that release and takes latest" {
	GITHUB_EVENT_NAME=push GITHUB_REF=refs/tags/v0.1.2 run "$SCRIPT"
	[ "$status" -eq 0 ]
	[ "$(field version)" = "0.1.2" ]
	[ "$(field ref)" = "v0.1.2" ]
	[ "$(field push_latest)" = "true" ]
}

@test "resolve: a dispatch with a version publishes that version" {
	GITHUB_EVENT_NAME=workflow_dispatch INPUT_VERSION=0.1.1 run "$SCRIPT"
	[ "$status" -eq 0 ]
	[ "$(field version)" = "0.1.1" ]
	[ "$(field ref)" = "v0.1.1" ]
	[ "$(field push_latest)" = "true" ]
}

@test "resolve: a dispatch tolerates a version given with its v prefix" {
	GITHUB_EVENT_NAME=workflow_dispatch INPUT_VERSION=v0.1.1 run "$SCRIPT"
	[ "$status" -eq 0 ]
	[ "$(field version)" = "0.1.1" ]
	[ "$(field ref)" = "v0.1.1" ]
}

@test "resolve: an empty dispatch input means the latest GitHub release" {
	GITHUB_EVENT_NAME=workflow_dispatch INPUT_VERSION= GH_LATEST_TAG=v0.1.2 run "$SCRIPT"
	[ "$status" -eq 0 ]
	[ "$(field version)" = "0.1.2" ]
	[ "$(field ref)" = "v0.1.2" ]
	[ "$(field push_latest)" = "true" ]
}

@test "resolve: an empty dispatch input fails when there is no release at all" {
	GITHUB_EVENT_NAME=workflow_dispatch INPUT_VERSION= run "$SCRIPT"
	[ "$status" -ne 0 ]
	[[ "$output" == *"No published release found"* ]]
}

@test "resolve: workflow_call takes the version the release workflow passes" {
	GITHUB_EVENT_NAME=workflow_call INPUT_VERSION=0.2.0 run "$SCRIPT"
	[ "$status" -eq 0 ]
	[ "$(field version)" = "0.2.0" ]
	[ "$(field push_latest)" = "true" ]
}

@test "resolve: a version with no matching tag fails early and says so" {
	GITHUB_EVENT_NAME=workflow_dispatch INPUT_VERSION=9.9.9 GH_MISSING_TAGS=v9.9.9 run "$SCRIPT"
	[ "$status" -ne 0 ]
	[[ "$output" == *"Tag v9.9.9 does not exist"* ]]
}

@test "resolve: a SNAPSHOT is never published as a release" {
	GITHUB_EVENT_NAME=workflow_dispatch INPUT_VERSION=0.1.3-SNAPSHOT run "$SCRIPT"
	[ "$status" -ne 0 ]
	[[ "$output" == *"Refusing to publish a SNAPSHOT"* ]]
}

@test "resolve: a version carrying shell metacharacters is refused" {
	GITHUB_EVENT_NAME=workflow_dispatch INPUT_VERSION='0.1.2; rm -rf /' run "$SCRIPT"
	[ "$status" -ne 0 ]
	[[ "$output" == *"Refusing suspicious version"* ]]
}

@test "resolve: a qualified release version is allowed" {
	GITHUB_EVENT_NAME=workflow_dispatch INPUT_VERSION=1.0.0-RC1 run "$SCRIPT"
	[ "$status" -eq 0 ]
	[ "$(field version)" = "1.0.0-RC1" ]
}

@test "resolve: a tag push does not spend an API call checking the tag exists" {
	# The tag is self-evidently there; a stubbed-missing tag must not fail it.
	GITHUB_EVENT_NAME=push GITHUB_REF=refs/tags/v0.1.2 GH_MISSING_TAGS=v0.1.2 run "$SCRIPT"
	[ "$status" -eq 0 ]
	[ "$(field version)" = "0.1.2" ]
}

@test "resolve: writes to GITHUB_OUTPUT when Actions provides it" {
	GITHUB_EVENT_NAME=workflow_dispatch INPUT_VERSION=0.1.2 GITHUB_OUTPUT="$TMP/out" run "$SCRIPT"
	[ "$status" -eq 0 ]
	grep -q "^version=0.1.2$" "$TMP/out"
	grep -q "^ref=v0.1.2$" "$TMP/out"
	grep -q "^push_latest=true$" "$TMP/out"
}
