#!/usr/bin/env bats
#
# Tests for scripts/ci/version-from-ref.sh and scripts/ci/compute-image-tags.sh
# — the Docker image tag computation used by publish-docker.yml.

VERSION_FROM_REF="${BATS_TEST_DIRNAME}/../../scripts/ci/version-from-ref.sh"
COMPUTE_TAGS="${BATS_TEST_DIRNAME}/../../scripts/ci/compute-image-tags.sh"

setup() { unset GITHUB_OUTPUT; }

@test "version-from-ref extracts the version from a release tag" {
	run bash "$VERSION_FROM_REF" "refs/tags/v0.1.0"
	[ "$status" -eq 0 ]
	[ "$output" = "0.1.0" ]
}

@test "version-from-ref extracts a qualified version" {
	run bash "$VERSION_FROM_REF" "refs/tags/v1.2.3-RC1"
	[ "$output" = "1.2.3-RC1" ]
}

@test "version-from-ref is empty for a branch ref" {
	run bash "$VERSION_FROM_REF" "refs/heads/main"
	[ "$status" -eq 0 ]
	[ -z "$output" ]
}

@test "version-from-ref is empty for a tag without the v prefix" {
	run bash "$VERSION_FROM_REF" "refs/tags/0.1.0"
	[ -z "$output" ]
}

@test "version-from-ref reads GITHUB_REF when no arg is given" {
	GITHUB_REF="refs/tags/v9.9.9" run bash "$VERSION_FROM_REF"
	[ "$output" = "9.9.9" ]
}

@test "compute-image-tags on a release tag is '<version> latest'" {
	run bash "$COMPUTE_TAGS" "refs/tags/v0.1.0"
	[ "$status" -eq 0 ]
	[ "$output" = "tags=0.1.0 latest" ]
}

@test "compute-image-tags on a branch is just 'latest'" {
	run bash "$COMPUTE_TAGS" "refs/heads/main"
	[ "$status" -eq 0 ]
	[ "$output" = "tags=latest" ]
}
