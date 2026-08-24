#!/usr/bin/env bats
# Tests for scripts/ci/push-latest-tags.sh

setup() {
	SCRIPT="${BATS_TEST_DIRNAME}/../../scripts/ci/push-latest-tags.sh"
	TMP="$(mktemp -d)"

	cat >"$TMP/docker" <<-'STUB'
		#!/usr/bin/env bash
		printf '%s\n' "$*" >>"$DOCKER_LOG"
		exit "${DOCKER_EXIT:-0}"
	STUB
	chmod +x "$TMP/docker"

	export DOCKER="$TMP/docker"
	export DOCKER_LOG="$TMP/docker.log"
}

teardown() {
	rm -rf "$TMP"
}

@test "push-latest-tags: tags and pushes both images as latest" {
	run "$SCRIPT" mgrzejszczak 0.1.2
	[ "$status" -eq 0 ]

	grep -qx "tag mgrzejszczak/stubborn-contract:0.1.2 mgrzejszczak/stubborn-contract:latest" "$DOCKER_LOG"
	grep -qx "push mgrzejszczak/stubborn-contract:latest" "$DOCKER_LOG"
	grep -qx "tag mgrzejszczak/stubborn-contract-stub-runner:0.1.2 mgrzejszczak/stubborn-contract-stub-runner:latest" "$DOCKER_LOG"
	grep -qx "push mgrzejszczak/stubborn-contract-stub-runner:latest" "$DOCKER_LOG"
	[ "$(wc -l <"$DOCKER_LOG")" -eq 4 ]
}

@test "push-latest-tags: an empty version is a no-op, not a failure" {
	run "$SCRIPT" mgrzejszczak ""
	[ "$status" -eq 0 ]
	[[ "$output" == *"nothing to tag as latest"* ]]
	[ ! -f "$DOCKER_LOG" ]
}

@test "push-latest-tags: a missing version argument is a no-op too" {
	run "$SCRIPT" mgrzejszczak
	[ "$status" -eq 0 ]
	[ ! -f "$DOCKER_LOG" ]
}

@test "push-latest-tags: requires the registry organization" {
	run "$SCRIPT"
	[ "$status" -ne 0 ]
	[[ "$output" == *"organization required"* ]]
}

@test "push-latest-tags: honours the organization it is given" {
	run "$SCRIPT" acme 1.0.0
	[ "$status" -eq 0 ]
	grep -q "acme/stubborn-contract:1.0.0" "$DOCKER_LOG"
	! grep -q "mgrzejszczak" "$DOCKER_LOG"
}

@test "push-latest-tags: a failing docker push fails the step" {
	DOCKER_EXIT=1 run "$SCRIPT" mgrzejszczak 0.1.2
	[ "$status" -ne 0 ]
}
