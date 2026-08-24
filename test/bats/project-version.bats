#!/usr/bin/env bats
# Tests for scripts/ci/project-version.sh

setup() {
	SCRIPT="${BATS_TEST_DIRNAME}/../../scripts/ci/project-version.sh"
	TMP="$(mktemp -d)"

	cat >"$TMP/mvn" <<-'STUB'
		#!/usr/bin/env bash
		printf '%s\n' "$*" >>"$MVN_LOG"
		printf '%s' "${MVN_VERSION-0.1.3-SNAPSHOT}"
	STUB
	chmod +x "$TMP/mvn"

	export MVN="$TMP/mvn"
	export MVN_LOG="$TMP/mvn.log"
	unset GITHUB_OUTPUT
}

teardown() {
	rm -rf "$TMP"
}

@test "project-version: prints the resolved version" {
	run "$SCRIPT" docker/stubborn-contract-docker/pom.xml
	[ "$status" -eq 0 ]
	[ "$output" = "0.1.3-SNAPSHOT" ]
}

@test "project-version: evaluates project.version against the given pom" {
	run "$SCRIPT" docker/stubborn-contract-docker/pom.xml
	[ "$status" -eq 0 ]
	grep -q -- "-Dexpression=project.version" "$MVN_LOG"
	grep -q -- "-f docker/stubborn-contract-docker/pom.xml" "$MVN_LOG"
}

@test "project-version: strips whitespace maven pads the value with" {
	MVN_VERSION="  0.1.2
	" run "$SCRIPT" pom.xml
	[ "$status" -eq 0 ]
	[ "$output" = "0.1.2" ]
}

@test "project-version: fails when maven resolves nothing" {
	MVN_VERSION="" run "$SCRIPT" pom.xml
	[ "$status" -ne 0 ]
	[[ "$output" == *"Could not resolve project.version"* ]]
}

@test "project-version: requires a pom path" {
	run "$SCRIPT"
	[ "$status" -ne 0 ]
	[[ "$output" == *"pom path required"* ]]
}

@test "project-version: exports to GITHUB_OUTPUT when Actions provides it" {
	GITHUB_OUTPUT="$TMP/out" run "$SCRIPT" pom.xml
	[ "$status" -eq 0 ]
	[ "$(cat "$TMP/out")" = "version=0.1.3-SNAPSHOT" ]
}
