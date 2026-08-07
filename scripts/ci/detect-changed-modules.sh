#!/usr/bin/env bash
#
# Decide whether CI should run a FULL reactor build or a narrowed
# changed-modules build, and — for the narrowed case — which top-level module
# directories were touched.
#
# Emits two GitHub Actions step outputs, `full_build` and `modules`, by
# appending to $GITHUB_OUTPUT when it is set (i.e. running under Actions), or
# printing `key=value` to stdout otherwise (so the BATS suite can assert on the
# result without a live Actions runner).
#
# Inputs (environment):
#   EVENT_NAME     github.event_name. Anything other than "pull_request"
#                  (push to main, schedule, workflow_dispatch) => full build.
#   BASE_SHA       github.event.pull_request.base.sha (the PR base commit).
#   HEAD_SHA       github.sha (the PR head commit).
#   CHANGED_FILES  OPTIONAL. A newline-separated list of changed paths. When
#                  set it is used verbatim instead of running `git diff`; this
#                  is the test seam the BATS suite drives.
#
# Classification rules (a change to any of these forces a FULL build):
#   * a top-level src/ or .mvn/ path — shared build/wrapper config;
#   * ANY pom.xml (root, build parent, or a module) — a dependency/version
#     change can reshape the reactor's resolution graph, and a partial reactor
#     built with --also-make-dependents can then drag in a dependent whose own
#     upstream sibling is not in the reactor and is not published as a snapshot
#     (e.g. a change under stubborn-contract-verifier* pulls in
#     stubborn-contract-wiremock-spring via -amd, which needs
#     stubborn-contract-wiremock — an artifact --also-make never adds);
#   * an aggregator module (packaging=pom carrying <modules>) — selecting it
#     with --also-make-dependents pulls its sibling submodules WITHOUT their
#     own upstreams, which then cannot be resolved.
# Paths under .github/, docs/, config/ and scripts/ are ignored for module
# mapping. If nothing maps to a buildable module, fall back to a full build.
#
set -euo pipefail

emit() {
	# emit KEY VALUE -> $GITHUB_OUTPUT when set, else stdout.
	local key="$1" value="$2"
	if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
		echo "${key}=${value}" >>"$GITHUB_OUTPUT"
	else
		echo "${key}=${value}"
	fi
}

emit_full_build() {
	emit full_build true
	emit modules ""
}

detect() {
	if [[ "${EVENT_NAME:-}" != "pull_request" ]]; then
		emit_full_build
		return 0
	fi

	local changed
	if [[ -n "${CHANGED_FILES:-}" ]]; then
		changed="$CHANGED_FILES"
	else
		changed="$(git diff --name-only "${BASE_SHA:-}" "${HEAD_SHA:-}" 2>/dev/null ||
			git diff --name-only HEAD~1 HEAD)"
	fi

	local modules="" file dir
	declare -A seen
	while IFS= read -r file; do
		# Skip blank lines: an empty $file yields an empty $dir, which is an
		# invalid Bash array subscript and would abort the step. git diff can
		# emit a trailing newline, and an empty list still enters the loop once.
		[[ -z "$file" ]] && continue
		dir="${file%%/*}"

		# Non-module top-level paths never map to a Maven module.
		case "$dir" in
		.github | docs | config | scripts)
			continue
			;;
		src | .mvn)
			# Shared build config (checkstyle, etc.) or Maven wrapper config
			# affects every module.
			emit_full_build
			return 0
			;;
		esac

		# Any pom.xml change -> full build (see header for the reasoning).
		if [[ "$file" == "pom.xml" || "$file" == */pom.xml ]]; then
			emit_full_build
			return 0
		fi

		# Aggregator module (packaging=pom with <modules>) -> full build.
		if [[ -f "$dir/pom.xml" ]] && grep -q "<modules>" "$dir/pom.xml"; then
			emit_full_build
			return 0
		fi

		[[ -n "${seen[$dir]:-}" ]] && continue
		seen[$dir]=1
		[[ -d "$dir" && -f "$dir/pom.xml" ]] && modules="${modules:+$modules,}$dir"
	done <<<"$changed"

	if [[ -z "$modules" ]]; then
		emit_full_build
	else
		emit full_build false
		emit modules "$modules"
	fi
}

detect "$@"
