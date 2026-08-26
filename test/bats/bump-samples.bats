#!/usr/bin/env bats
# Tests for scripts/release/bump-samples.sh

setup() {
	REPO_ROOT="$(cd "$BATS_TEST_DIRNAME/../.." && pwd)"
	SCRIPT="$REPO_ROOT/scripts/release/bump-samples.sh"
	TMP="$(mktemp -d)"

	# Stub gh: records how it was called and what body it was given.
	cat >"$TMP/gh" <<-'STUB'
		#!/usr/bin/env bash
		printf '%s\n' "$*" >"$GH_ARGS_FILE"
		cat >"$GH_BODY_FILE"
		exit "${GH_EXIT:-0}"
	STUB
	chmod +x "$TMP/gh"

	export GH="$TMP/gh"
	export GH_ARGS_FILE="$TMP/args"
	export GH_BODY_FILE="$TMP/body"
}

teardown() {
	rm -rf "$TMP"
}

@test "bump-samples: dispatches stubborn-contract-released with the version" {
	GH_TOKEN=token run "$SCRIPT" 0.1.3
	[ "$status" -eq 0 ]
	grep -q "repos/stubborn-sh/stubborn-samples/dispatches" "$GH_ARGS_FILE"
	grep -q -- "--method POST" "$GH_ARGS_FILE"
	[ "$(jq -r '.event_type' "$GH_BODY_FILE")" = "stubborn-contract-released" ]
	[ "$(jq -r '.client_payload.version' "$GH_BODY_FILE")" = "0.1.3" ]
}

@test "bump-samples: strips a leading v from the version" {
	GH_TOKEN=token run "$SCRIPT" v0.1.3
	[ "$status" -eq 0 ]
	[ "$(jq -r '.client_payload.version' "$GH_BODY_FILE")" = "0.1.3" ]
}

@test "bump-samples: honours SAMPLES_REPO" {
	GH_TOKEN=token SAMPLES_REPO=acme/samples run "$SCRIPT" 0.1.3
	[ "$status" -eq 0 ]
	grep -q "repos/acme/samples/dispatches" "$GH_ARGS_FILE"
}

@test "bump-samples: skips quietly when no token is configured" {
	run env -u GH_TOKEN "$SCRIPT" 0.1.3
	[ "$status" -eq 0 ]
	[[ "$output" == *"skipping the stubborn-samples bump"* ]]
	[ ! -f "$GH_ARGS_FILE" ]
}

@test "bump-samples: a failed dispatch never fails the release" {
	GH_TOKEN=token GH_EXIT=1 run "$SCRIPT" 0.1.3
	[ "$status" -eq 0 ]
	[[ "$output" == *"pin them by hand"* ]]
}

@test "bump-samples: requires a version" {
	GH_TOKEN=token run "$SCRIPT"
	[ "$status" -ne 0 ]
}
