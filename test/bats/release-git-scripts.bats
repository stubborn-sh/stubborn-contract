#!/usr/bin/env bats
#
# Tests for the release git/gh orchestration scripts. These drive git / gh / the
# Maven wrapper, so the deterministically testable surface is argument
# validation: each must refuse to run (touching no git history, no tag, no
# release, no version bump) when its required argument is missing.

TAG_AND_PUSH="${BATS_TEST_DIRNAME}/../../scripts/release/tag-and-push.sh"
CREATE_RELEASE="${BATS_TEST_DIRNAME}/../../scripts/release/create-github-release.sh"
BACK_TO_SNAPSHOT="${BATS_TEST_DIRNAME}/../../scripts/release/back-to-snapshot.sh"

@test "tag-and-push refuses to run without a version" {
	run bash "$TAG_AND_PUSH"
	[ "$status" -ne 0 ]
	[[ "$output" == *"Usage: tag-and-push.sh"* ]]
}

@test "create-github-release refuses to run without a version" {
	run bash "$CREATE_RELEASE"
	[ "$status" -ne 0 ]
	[[ "$output" == *"Usage: create-github-release.sh"* ]]
}

@test "back-to-snapshot refuses to run without a next-snapshot version" {
	run bash "$BACK_TO_SNAPSHOT"
	[ "$status" -ne 0 ]
	[[ "$output" == *"Usage: back-to-snapshot.sh"* ]]
}
