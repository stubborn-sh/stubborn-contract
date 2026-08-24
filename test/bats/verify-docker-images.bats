#!/usr/bin/env bats
#
# Tests for scripts/ci/verify-docker-images.sh — the gate that stops a release
# reporting success when nothing reached the registry.

setup() {
	SCRIPT="${BATS_TEST_DIRNAME}/../../scripts/ci/verify-docker-images.sh"
	TMP="$(mktemp -d)"

	# Stub docker: `manifest inspect` succeeds unless the reference is listed
	# in $DOCKER_MISSING.
	cat >"$TMP/docker" <<-'STUB'
		#!/usr/bin/env bash
		printf '%s\n' "$*" >>"$DOCKER_LOG"
		if [[ "$1 $2" == "manifest inspect" ]]; then
			for missing in ${DOCKER_MISSING:-}; do
				[[ "$3" == "$missing" ]] && exit 1
			done
		fi
		exit 0
	STUB
	chmod +x "$TMP/docker"

	export DOCKER="$TMP/docker"
	export DOCKER_LOG="$TMP/docker.log"
	unset DOCKER_MISSING
}

teardown() {
	rm -rf "$TMP"
}

@test "verify: passes when both images are in the registry" {
	run "$SCRIPT" mgrzejszczak 0.1.2
	[ "$status" -eq 0 ]
	[[ "$output" == *"found in registry: mgrzejszczak/stubborn-contract:0.1.2"* ]]
	[[ "$output" == *"found in registry: mgrzejszczak/stubborn-contract-stub-runner:0.1.2"* ]]
}

@test "verify: asks the registry, not the local daemon" {
	run "$SCRIPT" mgrzejszczak 0.1.2
	[ "$status" -eq 0 ]
	grep -q "^manifest inspect " "$DOCKER_LOG"
	! grep -q "^images" "$DOCKER_LOG"
	! grep -q "^inspect " "$DOCKER_LOG"
}

@test "verify: fails when the stub-runner image never made it" {
	DOCKER_MISSING="mgrzejszczak/stubborn-contract-stub-runner:0.1.2" run "$SCRIPT" mgrzejszczak 0.1.2
	[ "$status" -ne 0 ]
	[[ "$output" == *"MISSING from registry: mgrzejszczak/stubborn-contract-stub-runner:0.1.2"* ]]
	[[ "$output" == *"1 image(s) are not published"* ]]
}

@test "verify: fails when neither image made it — the 139-green-runs case" {
	DOCKER_MISSING="mgrzejszczak/stubborn-contract:0.1.2 mgrzejszczak/stubborn-contract-stub-runner:0.1.2" \
		run "$SCRIPT" mgrzejszczak 0.1.2
	[ "$status" -ne 0 ]
	[[ "$output" == *"2 image(s) are not published"* ]]
}

@test "verify: reports every missing image, not just the first" {
	DOCKER_MISSING="mgrzejszczak/stubborn-contract:latest mgrzejszczak/stubborn-contract-stub-runner:latest" \
		run "$SCRIPT" mgrzejszczak 0.1.2 latest
	[ "$status" -ne 0 ]
	[[ "$output" == *"stubborn-contract:latest"* ]]
	[[ "$output" == *"stubborn-contract-stub-runner:latest"* ]]
}

@test "verify: checks every tag it is given" {
	run "$SCRIPT" mgrzejszczak 0.1.2 latest
	[ "$status" -eq 0 ]
	[ "$(grep -c "^manifest inspect" "$DOCKER_LOG")" -eq 4 ]
}

@test "verify: requires an organization" {
	run "$SCRIPT"
	[ "$status" -ne 0 ]
	[[ "$output" == *"organization required"* ]]
}

@test "verify: requires at least one tag — verifying nothing is not passing" {
	run "$SCRIPT" mgrzejszczak
	[ "$status" -ne 0 ]
	[[ "$output" == *"At least one tag"* ]]
}
