#!/usr/bin/env bats
# Tests for scripts/ci/publish-docker-image.sh and scripts/ci/install-bom.sh

setup() {
	CI_DIR="${BATS_TEST_DIRNAME}/../../scripts/ci"
	TMP="$(mktemp -d)"

	cat >"$TMP/mvn" <<-'STUB'
		#!/usr/bin/env bash
		printf '%s\n' "$*" >>"$MVN_LOG"
		exit "${MVN_EXIT:-0}"
	STUB
	chmod +x "$TMP/mvn"

	export MVN="$TMP/mvn"
	export MVN_LOG="$TMP/mvn.log"
	unset MVN_EXIT
}

teardown() {
	rm -rf "$TMP"
}

@test "publish-docker-image: runs deploy, because push binds to the deploy phase" {
	DOCKER_ORG=acme run "$CI_DIR/publish-docker-image.sh" docker/stubborn-contract-docker/pom.xml
	[ "$status" -eq 0 ]
	grep -q "^deploy " "$MVN_LOG"
	# The bug this replaces: `package` builds the image and pushes nothing.
	! grep -q "^package " "$MVN_LOG"
}

@test "publish-docker-image: skips artifact deployment, which is not what deploy is for here" {
	DOCKER_ORG=acme run "$CI_DIR/publish-docker-image.sh" some/pom.xml
	[ "$status" -eq 0 ]
	grep -q -- "-Dmaven.deploy.skip=true" "$MVN_LOG"
}

@test "publish-docker-image: passes the registry organization and the pom" {
	DOCKER_ORG=mgrzejszczak run "$CI_DIR/publish-docker-image.sh" docker/x/pom.xml
	[ "$status" -eq 0 ]
	grep -q -- "-Ddocker.registry.organization=mgrzejszczak" "$MVN_LOG"
	grep -q -- "-f docker/x/pom.xml" "$MVN_LOG"
}

@test "publish-docker-image: requires a pom path" {
	DOCKER_ORG=acme run "$CI_DIR/publish-docker-image.sh"
	[ "$status" -ne 0 ]
	[[ "$output" == *"pom path required"* ]]
}

@test "publish-docker-image: requires DOCKER_ORG rather than defaulting to springcloud" {
	run env -u DOCKER_ORG "$CI_DIR/publish-docker-image.sh" some/pom.xml
	[ "$status" -ne 0 ]
	[[ "$output" == *"DOCKER_ORG is required"* ]]
}

@test "publish-docker-image: a failing maven fails the step" {
	DOCKER_ORG=acme MVN_EXIT=1 run "$CI_DIR/publish-docker-image.sh" some/pom.xml
	[ "$status" -ne 0 ]
}

@test "install-bom: installs the BOM non-recursively" {
	M2_REPO="$TMP/m2" run "$CI_DIR/install-bom.sh"
	[ "$status" -eq 0 ]
	grep -q -- "-f stubborn-contract-dependencies/pom.xml" "$MVN_LOG"
	grep -q -- "-N" "$MVN_LOG"
}

@test "install-bom: clears stale resolution failures before building" {
	mkdir -p "$TMP/m2/sh/stubborn/stubborn-contract-verifier/0.1.3-SNAPSHOT"
	stale="$TMP/m2/sh/stubborn/stubborn-contract-verifier/0.1.3-SNAPSHOT/resolver-status.properties.lastUpdated"
	touch "$stale"
	keep="$TMP/m2/sh/stubborn/stubborn-contract-verifier/0.1.3-SNAPSHOT/real.jar"
	touch "$keep"

	M2_REPO="$TMP/m2" run "$CI_DIR/install-bom.sh"
	[ "$status" -eq 0 ]
	[ ! -f "$stale" ]
	[ -f "$keep" ]
}

@test "install-bom: survives a local repository that does not exist yet" {
	M2_REPO="$TMP/nope" run "$CI_DIR/install-bom.sh"
	[ "$status" -eq 0 ]
	grep -q -- "-f stubborn-contract-dependencies/pom.xml" "$MVN_LOG"
}
