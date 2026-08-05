#!/usr/bin/env bats
# Tests for scripts/release/finalize-parent-version.sh
#
# Exercises the pure-text parent-ref rewrite offline (no Maven) against fixture poms.

setup() {
	REPO_ROOT="$(cd "$BATS_TEST_DIRNAME/../.." && pwd)"
	SCRIPT="$REPO_ROOT/scripts/release/finalize-parent-version.sh"
	TMP="$(mktemp -d)"
	write_pom() {
		# $1 = path, $2 = parent version, $3 = project version, $4 = dependency version
		cat > "$1" <<EOF
<project>
	<parent>
		<groupId>sh.stubborn</groupId>
		<artifactId>stubborn-contract-build</artifactId>
		<version>$2</version>
	</parent>
	<artifactId>demo</artifactId>
	<version>$3</version>
	<dependencies>
		<dependency>
			<groupId>com.example</groupId>
			<artifactId>lib</artifactId>
			<version>$4</version>
		</dependency>
	</dependencies>
</project>
EOF
	}
}

teardown() {
	rm -rf "$TMP"
}

parent_version() {
	# Prints the <version> inside the <parent> block of $1
	sed -n '/<parent>/,/<\/parent>/ s#.*<version>\(.*\)</version>.*#\1#p' "$1"
}

@test "finalize: rewrites the parent <version> to the release version" {
	write_pom "$TMP/pom.xml" "0.1.0-SNAPSHOT" "0.1.0-SNAPSHOT" "3.3.3-SNAPSHOT"
	run "$SCRIPT" "1.2.3" "$TMP/pom.xml"
	[ "$status" -eq 0 ]
	[ "$(parent_version "$TMP/pom.xml")" = "1.2.3" ]
}

@test "finalize: leaves the project <version> untouched" {
	write_pom "$TMP/pom.xml" "0.1.0-SNAPSHOT" "9.9.9" "3.3.3-SNAPSHOT"
	run "$SCRIPT" "1.2.3" "$TMP/pom.xml"
	[ "$status" -eq 0 ]
	# project version is outside the <parent> block and must stay 9.9.9
	grep -q "<version>9.9.9</version>" "$TMP/pom.xml"
}

@test "finalize: leaves dependency versions untouched" {
	write_pom "$TMP/pom.xml" "0.1.0-SNAPSHOT" "0.1.0-SNAPSHOT" "3.3.3-SNAPSHOT"
	run "$SCRIPT" "1.2.3" "$TMP/pom.xml"
	[ "$status" -eq 0 ]
	grep -q "<version>3.3.3-SNAPSHOT</version>" "$TMP/pom.xml"
}

@test "finalize: rewrites multiple poms in one call" {
	write_pom "$TMP/a.xml" "0.1.0-SNAPSHOT" "0.1.0-SNAPSHOT" "3.3.3-SNAPSHOT"
	write_pom "$TMP/b.xml" "0.1.0-SNAPSHOT" "0.1.0-SNAPSHOT" "3.3.3-SNAPSHOT"
	run "$SCRIPT" "2.0.0" "$TMP/a.xml" "$TMP/b.xml"
	[ "$status" -eq 0 ]
	[ "$(parent_version "$TMP/a.xml")" = "2.0.0" ]
	[ "$(parent_version "$TMP/b.xml")" = "2.0.0" ]
}

@test "finalize: fails on a missing pom" {
	run "$SCRIPT" "1.2.3" "$TMP/does-not-exist.xml"
	[ "$status" -ne 0 ]
}

@test "finalize: fails when no pom argument is given" {
	run "$SCRIPT" "1.2.3"
	[ "$status" -ne 0 ]
}
