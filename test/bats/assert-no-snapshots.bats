#!/usr/bin/env bats
# Tests for scripts/release/assert-no-snapshots.sh
#
# The guard relies on `git ls-files` pathspec exclusions, so each test builds a throwaway
# git repo fixture and runs the script from its root.

setup() {
	REPO_ROOT="$(cd "$BATS_TEST_DIRNAME/../.." && pwd)"
	SCRIPT="$REPO_ROOT/scripts/release/assert-no-snapshots.sh"
	FIX="$(mktemp -d)"
	cd "$FIX"
	git init -q
	git config user.email "t@t.t"
	git config user.name "t"
	mkdir -p sub docker/mod modx/src/test
	# An all-released baseline tree.
	printf '<project><version>1.2.3</version></project>\n' > pom.xml
	printf '<project><version>1.2.3</version></project>\n' > sub/pom.xml
	git add -A
	git commit -qm init
}

teardown() {
	cd /
	rm -rf "$FIX"
}

@test "assert-no-snapshots: passes on an all-released tree" {
	run "$SCRIPT"
	[ "$status" -eq 0 ]
	[[ "$output" == *"No -SNAPSHOT versions remain"* ]]
}

@test "assert-no-snapshots: fails when a -SNAPSHOT is planted" {
	printf '<project><version>1.2.3-SNAPSHOT</version></project>\n' > sub/pom.xml
	git add -A
	run "$SCRIPT"
	[ "$status" -ne 0 ]
	[[ "$output" == *"sub/pom.xml"* ]]
}

@test "assert-no-snapshots: ignores -SNAPSHOT under docker/" {
	printf '<project><version>1.2.3-SNAPSHOT</version></project>\n' > docker/mod/pom.xml
	git add -A
	run "$SCRIPT"
	[ "$status" -eq 0 ]
}

@test "assert-no-snapshots: ignores -SNAPSHOT under */src/test/*" {
	printf '<project><version>0.1.BUILD-SNAPSHOT</version></project>\n' > modx/src/test/pom.xml
	git add -A
	run "$SCRIPT"
	[ "$status" -eq 0 ]
}

@test "assert-no-snapshots: ignores *.flattened-pom.xml" {
	printf '<project><version>1.2.3-SNAPSHOT</version></project>\n' > sub/app.flattened-pom.xml
	git add -A
	run "$SCRIPT"
	[ "$status" -eq 0 ]
}
