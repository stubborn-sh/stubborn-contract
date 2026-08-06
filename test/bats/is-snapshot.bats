#!/usr/bin/env bats
# Tests for scripts/release/is-snapshot.sh

setup() {
	REPO_ROOT="$(cd "$BATS_TEST_DIRNAME/../.." && pwd)"
	SCRIPT="$REPO_ROOT/scripts/release/is-snapshot.sh"
	TMP="$(mktemp -d)"
}

teardown() {
	rm -rf "$TMP"
}

@test "is-snapshot: true for 1.2.3-SNAPSHOT" {
	printf '<project>\n  <version>1.2.3-SNAPSHOT</version>\n</project>\n' > "$TMP/pom.xml"
	run "$SCRIPT" "$TMP/pom.xml"
	[ "$status" -eq 0 ]
	[ "$output" = "true" ]
}

@test "is-snapshot: false for 1.2.3" {
	printf '<project>\n  <version>1.2.3</version>\n</project>\n' > "$TMP/pom.xml"
	run "$SCRIPT" "$TMP/pom.xml"
	[ "$status" -eq 0 ]
	[ "$output" = "false" ]
}

@test "is-snapshot: reads the FIRST <version> (parent block first)" {
	# Mirrors a real pom where <parent> precedes the project <version>; during a release
	# both are non-SNAPSHOT, during development both are -SNAPSHOT.
	printf '<project>\n  <parent>\n    <version>1.2.3-SNAPSHOT</version>\n  </parent>\n  <version>1.2.3-SNAPSHOT</version>\n</project>\n' > "$TMP/pom.xml"
	run "$SCRIPT" "$TMP/pom.xml"
	[ "$status" -eq 0 ]
	[ "$output" = "true" ]
}
