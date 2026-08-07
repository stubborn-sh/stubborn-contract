#!/usr/bin/env bats
#
# Tests for scripts/ci/detect-changed-modules.sh — the CI changed-modules
# classifier. Each case drives the script through its CHANGED_FILES test seam
# inside a fixture directory laid out with leaf and aggregator module poms, and
# asserts the emitted `full_build` / `modules` step outputs (printed to stdout
# when GITHUB_OUTPUT is unset).

SCRIPT="${BATS_TEST_DIRNAME}/../../scripts/ci/detect-changed-modules.sh"

setup() {
	FIX="$(mktemp -d)"
	# Two leaf modules (no <modules>) and one aggregator (packaging=pom).
	mkdir -p "$FIX/moduleA" "$FIX/moduleB" "$FIX/aggr" "$FIX/src" "$FIX/.mvn"
	printf '<project><artifactId>moduleA</artifactId></project>\n' >"$FIX/moduleA/pom.xml"
	printf '<project><artifactId>moduleB</artifactId></project>\n' >"$FIX/moduleB/pom.xml"
	printf '<project><modules><module>moduleA</module></modules></project>\n' >"$FIX/aggr/pom.xml"
	printf '<project><artifactId>root</artifactId></project>\n' >"$FIX/pom.xml"
	cd "$FIX"
	unset GITHUB_OUTPUT
}

teardown() {
	rm -rf "$FIX"
}

run_detect() { # run_detect <event> <changed-files-newline-list>
	EVENT_NAME="$1" CHANGED_FILES="$2" run bash "$SCRIPT"
}

@test "push event forces a full build" {
	run_detect "push" ""
	[ "$status" -eq 0 ]
	[[ "$output" == *"full_build=true"* ]]
	[[ "$output" == *"modules="* ]]
}

@test "schedule/dispatch (non-PR) forces a full build" {
	run_detect "schedule" "moduleA/src/main/java/X.java"
	[ "$status" -eq 0 ]
	[[ "$output" == *"full_build=true"* ]]
}

@test "blank lines do not crash and fall back to a full build" {
	run_detect "pull_request" $'\n\n'
	[ "$status" -eq 0 ]
	[[ "$output" != *"bad array subscript"* ]]
	[[ "$output" == *"full_build=true"* ]]
}

@test "root pom.xml change forces a full build" {
	run_detect "pull_request" "pom.xml"
	[ "$status" -eq 0 ]
	[[ "$output" == *"full_build=true"* ]]
}

@test "a module pom.xml change forces a full build" {
	run_detect "pull_request" "moduleA/pom.xml"
	[ "$status" -eq 0 ]
	[[ "$output" == *"full_build=true"* ]]
}

@test "src/ shared config change forces a full build" {
	run_detect "pull_request" "src/checkstyle/checkstyle.xml"
	[ "$status" -eq 0 ]
	[[ "$output" == *"full_build=true"* ]]
}

@test ".mvn wrapper config change forces a full build" {
	run_detect "pull_request" ".mvn/wrapper/maven-wrapper.properties"
	[ "$status" -eq 0 ]
	[[ "$output" == *"full_build=true"* ]]
}

@test "aggregator submodule change forces a full build" {
	run_detect "pull_request" "aggr/README.md"
	[ "$status" -eq 0 ]
	[[ "$output" == *"full_build=true"* ]]
}

@test "a single leaf module source change narrows to that module" {
	run_detect "pull_request" "moduleA/src/main/java/X.java"
	[ "$status" -eq 0 ]
	[[ "$output" == *"full_build=false"* ]]
	[[ "$output" == *"modules=moduleA"* ]]
}

@test "two leaf modules are both listed and de-duplicated" {
	run_detect "pull_request" $'moduleA/src/main/java/X.java\nmoduleA/src/test/java/XT.java\nmoduleB/src/main/java/Y.java'
	[ "$status" -eq 0 ]
	[[ "$output" == *"full_build=false"* ]]
	[[ "$output" == *"modules=moduleA,moduleB"* ]]
}

@test "docs-only change maps to no module and falls back to a full build" {
	run_detect "pull_request" "docs/index.md"
	[ "$status" -eq 0 ]
	[[ "$output" == *"full_build=true"* ]]
}

@test "a leaf change mixed with a blank line still narrows correctly" {
	run_detect "pull_request" $'\nmoduleB/src/main/java/Y.java\n'
	[ "$status" -eq 0 ]
	[[ "$output" == *"full_build=false"* ]]
	[[ "$output" == *"modules=moduleB"* ]]
}
