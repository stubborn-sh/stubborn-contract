#!/usr/bin/env bash
#
# coverage-report.sh
#
# Aggregates JaCoCo CSV reports across the 12 "core, spring-less" quality-gate
# modules into a single LINE / BRANCH coverage figure.
#
# Usage:
#   scripts/quality/coverage-report.sh [--enforce]
#
#   --enforce   exit non-zero when aggregate LINE < 80% or BRANCH < 80%.
#               Default: report only, always exit 0.
#
# Prerequisite: the JaCoCo CSV reports must already exist. Produce them by running
# the gate modules under the -Psonar profile (which binds jacoco:prepare-agent and
# jacoco:report), e.g. from the repo root:
#
#   ./mvnw -Psonar test \
#     -pl stubborn-contract-jsonassert,stubborn-contract-xmlassert,\
# specs/stubborn-contract-spec-java,specs/stubborn-contract-spec-groovy,\
# stubborn-contract-verifier,stubborn-contract-generator,stubborn-contract-stub-runner,\
# stubborn-contract-wiremock,stubborn-contract-tools/stubborn-contract-converters,\
# stubborn-contract-messaging-kafka,stubborn-contract-messaging-rabbit,\
# stubborn-contract-messaging-jms -am
#
# Each module then has target/site/jacoco/jacoco.csv. This script sums the
# INSTRUCTION / BRANCH / LINE missed+covered counters across all of them and
# prints:  LINE=xx.x% BRANCH=xx.x%
#
# The module list below is the authoritative gate set and MUST stay in sync with
# docs/quality-gates.md and scripts/quality/changed-core-classes.sh.
set -euo pipefail

ENFORCE=0
for arg in "$@"; do
	case "${arg}" in
		--enforce) ENFORCE=1 ;;
		*) echo "unknown argument: ${arg}" >&2; exit 2 ;;
	esac
done

# Resolve repo root so the script works from any working directory.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

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

# Collect every jacoco.csv found under the gate modules' target dirs.
csvs=()
for module in "${GATE_MODULES[@]}"; do
	while IFS= read -r f; do
		[ -n "${f}" ] && csvs+=("${f}")
	done < <(find "${REPO_ROOT}/${module}" -path '*/target/site/jacoco/jacoco.csv' 2>/dev/null || true)
done

if [ "${#csvs[@]}" -eq 0 ]; then
	echo "No jacoco.csv reports found under the gate modules." >&2
	echo "Run the gate modules with -Psonar first (see the header of this script)." >&2
	# Report-only mode should not fail the caller; enforce mode should.
	[ "${ENFORCE}" -eq 1 ] && exit 1
	exit 0
fi

# JaCoCo CSV columns (header row present in each file):
#   GROUP,PACKAGE,CLASS,
#   INSTRUCTION_MISSED,INSTRUCTION_COVERED,
#   BRANCH_MISSED,BRANCH_COVERED,
#   LINE_MISSED,LINE_COVERED,
#   COMPLEXITY_MISSED,COMPLEXITY_COVERED,
#   METHOD_MISSED,METHOD_COVERED
ENFORCE="${ENFORCE}" python3 - "${csvs[@]}" <<'PY'
import csv, os, sys

files = sys.argv[1:]
enforce = os.environ.get("ENFORCE", "0") == "1"

inst_m = inst_c = br_m = br_c = ln_m = ln_c = 0
for path in files:
	with open(path, newline="") as fh:
		for row in csv.DictReader(fh):
			inst_m += int(row["INSTRUCTION_MISSED"])
			inst_c += int(row["INSTRUCTION_COVERED"])
			br_m += int(row["BRANCH_MISSED"])
			br_c += int(row["BRANCH_COVERED"])
			ln_m += int(row["LINE_MISSED"])
			ln_c += int(row["LINE_COVERED"])

def pct(covered, missed):
	total = covered + missed
	return 100.0 * covered / total if total else 100.0

line_pct = pct(ln_c, ln_m)
branch_pct = pct(br_c, br_m)
inst_pct = pct(inst_c, inst_m)

print(f"LINE={line_pct:.1f}% BRANCH={branch_pct:.1f}% INSTRUCTION={inst_pct:.1f}% "
      f"(files={len(files)})")

if enforce and (line_pct < 80.0 or branch_pct < 80.0):
	print(f"FAIL: coverage below floor (LINE>=80% BRANCH>=80% required)", file=sys.stderr)
	sys.exit(1)
PY
