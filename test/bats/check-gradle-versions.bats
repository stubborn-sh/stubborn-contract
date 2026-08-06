#!/usr/bin/env bats
# Tests for scripts/docker/check-gradle-versions.sh
#
# Exercises the drift check offline (no Maven) against a fixture gradle.properties.

setup() {
	REPO_ROOT="$(cd "$BATS_TEST_DIRNAME/../.." && pwd)"
	SCRIPT="$REPO_ROOT/scripts/docker/check-gradle-versions.sh"
	TMP="$(mktemp -d)"
	PROPS="$TMP/gradle.properties"
	write_props() {
		# $1 = verifierVersion, $2 = springBootVersion, $3 = camelVersion
		cat > "$PROPS" <<EOF
org.gradle.daemon=false
org.gradle.parallel=false
verifierVersion=$1
springBootVersion=$2
camelVersion=$3
EOF
	}
}

teardown() {
	rm -rf "$TMP"
}

@test "check: passes when all three versions match" {
	write_props "0.1.0-SNAPSHOT" "4.1.0" "4.21.0"
	run "$SCRIPT" "$PROPS" "0.1.0-SNAPSHOT" "4.1.0" "4.21.0"
	[ "$status" -eq 0 ]
	[[ "$output" == OK:* ]]
}

@test "check: fails when verifierVersion drifts" {
	write_props "9.9.9" "4.1.0" "4.21.0"
	run "$SCRIPT" "$PROPS" "0.1.0-SNAPSHOT" "4.1.0" "4.21.0"
	[ "$status" -eq 1 ]
	[[ "$output" == *verifierVersion* ]]
}

@test "check: fails when springBootVersion drifts" {
	write_props "0.1.0-SNAPSHOT" "3.0.0" "4.21.0"
	run "$SCRIPT" "$PROPS" "0.1.0-SNAPSHOT" "4.1.0" "4.21.0"
	[ "$status" -eq 1 ]
	[[ "$output" == *springBootVersion* ]]
}

@test "check: fails when camelVersion drifts" {
	write_props "0.1.0-SNAPSHOT" "4.1.0" "1.2.3"
	run "$SCRIPT" "$PROPS" "0.1.0-SNAPSHOT" "4.1.0" "4.21.0"
	[ "$status" -eq 1 ]
	[[ "$output" == *camelVersion* ]]
}

@test "check: reports every mismatch at once" {
	write_props "9.9.9" "3.0.0" "1.2.3"
	run "$SCRIPT" "$PROPS" "0.1.0-SNAPSHOT" "4.1.0" "4.21.0"
	[ "$status" -eq 1 ]
	[[ "$output" == *verifierVersion* ]]
	[[ "$output" == *springBootVersion* ]]
	[[ "$output" == *camelVersion* ]]
}

@test "check: fails on a missing gradle.properties" {
	run "$SCRIPT" "$TMP/does-not-exist.properties" "0.1.0-SNAPSHOT" "4.1.0" "4.21.0"
	[ "$status" -ne 0 ]
}

@test "check: fails when arguments are missing" {
	write_props "0.1.0-SNAPSHOT" "4.1.0" "4.21.0"
	run "$SCRIPT" "$PROPS" "0.1.0-SNAPSHOT"
	[ "$status" -ne 0 ]
}
