#!/usr/bin/env bats
# Tests for scripts/release/finalize-project-version.sh
#
# Exercises the pure-text project-<version> rewrite offline (no Maven) against fixture poms
# shaped like the out-of-reactor Docker poms (a <parent> block, the project's own <version>,
# and unrelated dependency/plugin <version>s that must be left untouched).

setup() {
	REPO_ROOT="$(cd "$BATS_TEST_DIRNAME/../.." && pwd)"
	SCRIPT="$REPO_ROOT/scripts/release/finalize-project-version.sh"
	TMP="$(mktemp -d)"
	write_pom() {
		# $1 = path, $2 = parent version, $3 = project version, $4 = plugin version
		cat > "$1" <<EOF
<project>
	<parent>
		<groupId>sh.stubborn</groupId>
		<artifactId>stubborn-contract-docker-parent</artifactId>
		<version>$2</version>
	</parent>
	<artifactId>demo-docker</artifactId>
	<packaging>pom</packaging>
	<version>$3</version>
	<build>
		<plugins>
			<plugin>
				<artifactId>maven-deploy-plugin</artifactId>
				<version>$4</version>
			</plugin>
		</plugins>
	</build>
</project>
EOF
	}
	project_version() {
		# Prints the project's own <version> (first <version> after </parent>).
		awk '/<\/parent>/{p=1;next} p&&/<version>/{gsub(/.*<version>|<\/version>.*/,"");print;exit}' "$1"
	}
	parent_version() {
		sed -n '/<parent>/,/<\/parent>/ s#.*<version>\(.*\)</version>.*#\1#p' "$1"
	}
}

teardown() {
	rm -rf "$TMP"
}

@test "finalize-project: rewrites the project <version> to the release version" {
	write_pom "$TMP/pom.xml" "0.1.2-SNAPSHOT" "0.1.2-SNAPSHOT" "2.8.2"
	run "$SCRIPT" "0.1.2" "$TMP/pom.xml"
	[ "$status" -eq 0 ]
	[ "$(project_version "$TMP/pom.xml")" = "0.1.2" ]
}

@test "finalize-project: leaves the parent <version> untouched" {
	write_pom "$TMP/pom.xml" "7.7.7-SNAPSHOT" "0.1.2-SNAPSHOT" "2.8.2"
	run "$SCRIPT" "0.1.2" "$TMP/pom.xml"
	[ "$status" -eq 0 ]
	# the parent ref is finalized separately by finalize-parent-version.sh
	[ "$(parent_version "$TMP/pom.xml")" = "7.7.7-SNAPSHOT" ]
}

@test "finalize-project: leaves plugin/dependency versions untouched" {
	write_pom "$TMP/pom.xml" "0.1.2-SNAPSHOT" "0.1.2-SNAPSHOT" "2.8.2"
	run "$SCRIPT" "0.1.2" "$TMP/pom.xml"
	[ "$status" -eq 0 ]
	grep -q "<version>2.8.2</version>" "$TMP/pom.xml"
}

@test "finalize-project: rewrites multiple poms in one call" {
	write_pom "$TMP/a.xml" "0.1.2-SNAPSHOT" "0.1.2-SNAPSHOT" "2.8.2"
	write_pom "$TMP/b.xml" "0.1.2-SNAPSHOT" "0.1.2-SNAPSHOT" "1.4.13"
	run "$SCRIPT" "2.0.0" "$TMP/a.xml" "$TMP/b.xml"
	[ "$status" -eq 0 ]
	[ "$(project_version "$TMP/a.xml")" = "2.0.0" ]
	[ "$(project_version "$TMP/b.xml")" = "2.0.0" ]
}

@test "finalize-project: only the first post-parent <version> is rewritten" {
	# A second <version> that equals the old project version (e.g. a pinned reactor artifact)
	# must NOT be swept up — only the project's own <version> changes.
	cat > "$TMP/pom.xml" <<EOF
<project>
	<parent>
		<groupId>sh.stubborn</groupId>
		<artifactId>stubborn-contract-docker-parent</artifactId>
		<version>0.1.2-SNAPSHOT</version>
	</parent>
	<artifactId>demo-docker</artifactId>
	<packaging>pom</packaging>
	<version>0.1.2-SNAPSHOT</version>
	<dependencies>
		<dependency>
			<groupId>sh.stubborn</groupId>
			<artifactId>some-pinned-artifact</artifactId>
			<version>0.1.2-SNAPSHOT</version>
		</dependency>
	</dependencies>
</project>
EOF
	run "$SCRIPT" "0.1.2" "$TMP/pom.xml"
	[ "$status" -eq 0 ]
	[ "$(project_version "$TMP/pom.xml")" = "0.1.2" ]
	# the dependency's literal version is left for the caller to manage explicitly
	grep -q "<artifactId>some-pinned-artifact</artifactId>" "$TMP/pom.xml"
	run bash -c "grep -c '<version>0.1.2-SNAPSHOT</version>' '$TMP/pom.xml'"
	[ "$output" = "2" ]
}

@test "finalize-project: fails on a missing pom" {
	run "$SCRIPT" "1.2.3" "$TMP/does-not-exist.xml"
	[ "$status" -ne 0 ]
}

@test "finalize-project: fails when no pom argument is given" {
	run "$SCRIPT" "1.2.3"
	[ "$status" -ne 0 ]
}

@test "finalize-project: fails on a pom with no <parent> block" {
	cat > "$TMP/noparent.xml" <<EOF
<project>
	<artifactId>demo</artifactId>
	<version>0.1.2-SNAPSHOT</version>
</project>
EOF
	run "$SCRIPT" "0.1.2" "$TMP/noparent.xml"
	[ "$status" -ne 0 ]
}
