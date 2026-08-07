#!/usr/bin/env bats
#
# Tests for scripts/release/validate-version.sh — the release version format
# gate used by release.yml.

SCRIPT="${BATS_TEST_DIRNAME}/../../scripts/release/validate-version.sh"

@test "accepts a plain X.Y.Z" {
	run bash "$SCRIPT" "0.1.0"
	[ "$status" -eq 0 ]
	[ "$output" = "0.1.0" ]
}

@test "accepts a pre-release qualifier" {
	run bash "$SCRIPT" "0.1.0-RC1"
	[ "$status" -eq 0 ]
	[ "$output" = "0.1.0-RC1" ]
}

@test "accepts a dotted qualifier" {
	run bash "$SCRIPT" "1.20.30-alpha.1"
	[ "$status" -eq 0 ]
}

@test "rejects a two-segment version" {
	run bash "$SCRIPT" "1.2"
	[ "$status" -ne 0 ]
	[[ "$output" == *"Invalid version format"* ]]
}

@test "rejects a four-segment version" {
	run bash "$SCRIPT" "1.2.3.4"
	[ "$status" -ne 0 ]
}

@test "rejects a leading v" {
	run bash "$SCRIPT" "v1.2.3"
	[ "$status" -ne 0 ]
}

@test "rejects a non-numeric version" {
	run bash "$SCRIPT" "foo"
	[ "$status" -ne 0 ]
}

@test "rejects an empty version" {
	run bash "$SCRIPT" ""
	[ "$status" -ne 0 ]
}
