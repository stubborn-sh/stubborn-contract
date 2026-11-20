#!/usr/bin/env bats
# Tests for scripts/release/bump-samples.sh
#
# The point of these: before 0.2.2 this script swallowed a failed dispatch and exited 0,
# so the release step reported success while the samples stayed on the previous version.
# A green tick over a bump that did not happen is the bug being guarded here.

setup() {
	REPO_ROOT="$(cd "$BATS_TEST_DIRNAME/../.." && pwd)"
	SCRIPT="$REPO_ROOT/scripts/release/bump-samples.sh"
	TMP="$(mktemp -d)"

	# Stub gh: records how it was called and what payload it was given, and fails when asked.
	cat >"$TMP/gh" <<-'STUB'
		#!/usr/bin/env bash
		printf '%s\n' "$*" >"$GH_ARGS_FILE"
		cat >"$GH_BODY_FILE"
		# GH_MESSAGE stands in for what gh says on the way out — "gh: Bad credentials
		# (HTTP 401)" and the like, which is the part worth surfacing.
		[ -n "${GH_MESSAGE:-}" ] && printf '%s\n' "$GH_MESSAGE" >&2
		exit "${GH_EXIT:-0}"
	STUB
	chmod +x "$TMP/gh"

	export GH="$TMP/gh"
	export GH_ARGS_FILE="$TMP/args"
	export GH_BODY_FILE="$TMP/body"

	# Stands in for the file GitHub gives a step; the script appends markdown to it.
	export GITHUB_STEP_SUMMARY="$TMP/summary"
	: >"$GITHUB_STEP_SUMMARY"
}

teardown() {
	rm -rf "$TMP"
}

@test "bump-samples: dispatches stubborn-contract-released with the version" {
	GH_TOKEN=token run "$SCRIPT" 0.2.2
	[ "$status" -eq 0 ]

	grep -q "repos/stubborn-sh/stubborn-samples/dispatches" "$GH_ARGS_FILE"
	grep -q -- "--method POST" "$GH_ARGS_FILE"
	[ "$(jq -r '.event_type' "$GH_BODY_FILE")" = "stubborn-contract-released" ]
	[ "$(jq -r '.client_payload.version' "$GH_BODY_FILE")" = "0.2.2" ]
}

@test "bump-samples: strips a leading v from the version" {
	GH_TOKEN=token run "$SCRIPT" v0.2.2
	[ "$status" -eq 0 ]
	[ "$(jq -r '.client_payload.version' "$GH_BODY_FILE")" = "0.2.2" ]
}

@test "bump-samples: fails when the dispatch fails" {
	GH_TOKEN=token GH_EXIT=1 run "$SCRIPT" 0.2.2
	[ "$status" -ne 0 ]
	[[ "$output" == *"::error::"* ]]
	[[ "$output" == *"pin them by hand"* ]]
}

@test "bump-samples: fails when there is no token, rather than skipping quietly" {
	run env -u GH_TOKEN "$SCRIPT" 0.2.2
	[ "$status" -ne 0 ]
	[[ "$output" == *"::error::"* ]]
	[ ! -f "$GH_ARGS_FILE" ]
}

@test "bump-samples: honours SAMPLES_REPO" {
	GH_TOKEN=token SAMPLES_REPO=acme/samples run "$SCRIPT" 0.2.2
	[ "$status" -eq 0 ]
	grep -q "repos/acme/samples/dispatches" "$GH_ARGS_FILE"
}

# The job summary is the point of these: a continue-on-error step reports its conclusion
# as success through the API and the run's step list whatever the script's exit code, so
# the summary is the only place a failed bump shows without opening the raw log.

@test "bump-samples: a failed dispatch says so in the job summary" {
	GH_TOKEN=token GH_EXIT=1 GH_MESSAGE="gh: Bad credentials (HTTP 401)" run "$SCRIPT" 0.2.3
	[ "$status" -ne 0 ]

	grep -q "NOT bumped to 0.2.3" "$GITHUB_STEP_SUMMARY"
	grep -q "Pin them by hand" "$GITHUB_STEP_SUMMARY"
	grep -q "bump-stubborn-version.yml" "$GITHUB_STEP_SUMMARY"
}

@test "bump-samples: the summary carries the reason gh gave" {
	GH_TOKEN=token GH_EXIT=1 GH_MESSAGE="gh: Bad credentials (HTTP 401)" run "$SCRIPT" 0.2.3
	[ "$status" -ne 0 ]

	grep -q "Bad credentials (HTTP 401)" "$GITHUB_STEP_SUMMARY"
	# and it still reaches the log, as it did before
	[[ "$output" == *"Bad credentials (HTTP 401)"* ]]
}

@test "bump-samples: a missing token says so in the job summary too" {
	run env -u GH_TOKEN "$SCRIPT" 0.2.3
	[ "$status" -ne 0 ]

	grep -q "NOT bumped to 0.2.3" "$GITHUB_STEP_SUMMARY"
	grep -q "GH_TOKEN" "$GITHUB_STEP_SUMMARY"
}

@test "bump-samples: a successful dispatch is recorded in the job summary" {
	GH_TOKEN=token run "$SCRIPT" 0.2.3
	[ "$status" -eq 0 ]

	grep -q "asked to pin the samples to 0.2.3" "$GITHUB_STEP_SUMMARY"
	! grep -q "NOT bumped" "$GITHUB_STEP_SUMMARY"
}

@test "bump-samples: names the right repository in the summary when SAMPLES_REPO is set" {
	GH_TOKEN=token GH_EXIT=1 SAMPLES_REPO=acme/samples run "$SCRIPT" 0.2.3
	[ "$status" -ne 0 ]
	grep -q "acme/samples" "$GITHUB_STEP_SUMMARY"
}

@test "bump-samples: works with no GITHUB_STEP_SUMMARY at all" {
	GH_TOKEN=token run env -u GITHUB_STEP_SUMMARY "$SCRIPT" 0.2.3
	[ "$status" -eq 0 ]
	grep -q "repos/stubborn-sh/stubborn-samples/dispatches" "$GH_ARGS_FILE"
}
