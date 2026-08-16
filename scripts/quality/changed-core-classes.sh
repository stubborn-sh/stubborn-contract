#!/usr/bin/env bash
#
# changed-core-classes.sh
#
# Lists the production Java classes that changed on this branch, restricted to the
# 12 "core, spring-less" quality-gate modules, grouped per module.
#
# Usage:
#   scripts/quality/changed-core-classes.sh [BASE_REF]
#
#   BASE_REF   git ref to diff against (default: origin/main)
#
# Output (one line per module that has changed production classes):
#
#   <MODULE_PATH><TAB><comma,separated,fully.qualified.ClassNames>
#
# Modules with no changed src/main/java classes are skipped. When nothing changed
# the script prints nothing and exits 0. Consumed by the mutation-diff CI job to
# build PIT -DtargetClasses globs.
#
# The module list below is the authoritative gate set and MUST stay in sync with
# docs/quality-gates.md, scripts/quality/coverage-report.sh, and the pitest
# plugin declarations in each module pom.
set -euo pipefail

BASE_REF="${1:-origin/main}"

# True (exit 0) when the file's diff against BASE_REF contains at least one
# added/removed line that is real code — i.e. not a Javadoc/block/line comment or
# a blank line. Comment-only changes (e.g. adding @since tags) cannot alter
# bytecode, so they must not drag a class into the mutation gate.
is_code_change() {
	local file="$1" body
	body="$(git diff "${BASE_REF}...HEAD" -- "${file}" \
		| grep -E '^[+-]' \
		| grep -Ev '^(\+\+\+|---)' \
		| sed -E 's/^[+-][[:space:]]*//' \
		| grep -Ev '^([[:space:]]*$|\*|/\*\*|\*/|//)' \
		|| true)"
	[ -n "${body}" ]
}

# The 12 core, spring-less gate modules (paths relative to the repo root).
GATE_MODULES=(
	"stubborn-contract-jsonassert"
	"stubborn-contract-xmlassert"
	"specs/stubborn-contract-spec-java"
	"specs/stubborn-contract-spec-groovy"
	"stubborn-contract-verifier"
	"stubborn-contract-generator"
	"stubborn-contract-stub-runner"
	"stubborn-contract-wiremock"
	"stubborn-contract-tools/stubborn-contract-converters"
	"stubborn-contract-messaging-kafka"
	"stubborn-contract-messaging-rabbit"
	"stubborn-contract-messaging-jms"
)

# All changed production Java sources across the whole diff. `... ` (three dots)
# diffs against the merge base, matching how PR diffs are computed. Tolerate the
# case where there are no changes at all.
changed_files="$(git diff --name-only "${BASE_REF}...HEAD" -- '*/src/main/java/*.java' || true)"

[ -z "${changed_files}" ] && exit 0

for module in "${GATE_MODULES[@]}"; do
	fqcns=""
	while IFS= read -r file; do
		[ -z "${file}" ] && continue
		# Only files under this module's src/main/java.
		case "${file}" in
			"${module}/src/main/java/"*) ;;
			*) continue ;;
		esac
		# Skip files whose change is Javadoc/comment-only — no bytecode impact.
		is_code_change "${file}" || continue
		# Strip everything up to and including src/main/java/, drop the .java
		# suffix, and turn path separators into dots -> fully qualified class name.
		fqcn="${file#*/src/main/java/}"
		fqcn="${fqcn%.java}"
		fqcn="${fqcn//\//.}"
		if [ -z "${fqcns}" ]; then
			fqcns="${fqcn}"
		else
			fqcns="${fqcns},${fqcn}"
		fi
	done <<< "${changed_files}"

	if [ -n "${fqcns}" ]; then
		printf '%s\t%s\n' "${module}" "${fqcns}"
	fi
done

# Never let the exit status of the last loop iteration (an empty-module `[ -n ]`
# test returning 1) leak out: the CI step captures our stdout under `set -e`, so
# a non-zero exit here aborts the whole mutation gate even on success.
exit 0
