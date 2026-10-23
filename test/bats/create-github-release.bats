#!/usr/bin/env bats
# Tests for scripts/release/create-github-release.sh

setup() {
	REPO_ROOT="$(cd "$BATS_TEST_DIRNAME/../.." && pwd)"
	SCRIPT="$REPO_ROOT/scripts/release/create-github-release.sh"
	TMP="$(mktemp -d)"

	# Stub gh: records every invocation, and answers the two queries the script makes.
	# GH_RELEASE_EXISTS decides whether "release view" succeeds; GH_EXISTING_BODY is what
	# "release view --json body" returns.
	cat >"$TMP/gh" <<-'STUB'
		#!/usr/bin/env bash
		printf '%s\n' "$*" >>"$GH_ARGS_FILE"
		case "$*" in
		*"release view"*"--json body"*)
			printf '%s' "${GH_EXISTING_BODY:-}"
			;;
		*"release view"*)
			[ "${GH_RELEASE_EXISTS:-0}" = "1" ] || exit 1
			;;
		*"release edit"*|*"release create"*)
			# capture the notes file the script handed us
			for a in "$@"; do
				if [ "$prev" = "--notes-file" ]; then cp "$a" "$GH_NOTES_FILE"; fi
				prev="$a"
			done
			;;
		*generate-notes*)
			printf '%s' "${GH_GENERATED:-}"
			;;
		esac
		exit 0
	STUB
	chmod +x "$TMP/gh"

	mkdir -p "$TMP/work"
	cd "$TMP/work"
	export PATH="$TMP:$PATH"
	export GH_ARGS_FILE="$TMP/args"
	export GH_NOTES_FILE="$TMP/notes"
	: >"$GH_ARGS_FILE"
}

teardown() {
	cd /
	rm -rf "$TMP"
}

@test "create-github-release: renders changelog/<version>.md above the boilerplate" {
	mkdir -p changelog
	printf '## Highlights\n\nThe BOM stopped overriding Boot.\n' >changelog/0.2.0.md

	GH_RELEASE_EXISTS=0 run "$SCRIPT" 0.2.0
	[ "$status" -eq 0 ]

	# the narrative comes first, the "Use it" boilerplate after it
	grep -q "The BOM stopped overriding Boot." "$GH_NOTES_FILE"
	narrative_line="$(grep -n '## Highlights' "$GH_NOTES_FILE" | cut -d: -f1)"
	useit_line="$(grep -n '## Use it' "$GH_NOTES_FILE" | cut -d: -f1)"
	[ "$narrative_line" -lt "$useit_line" ]
}

@test "create-github-release: works without a changelog file" {
	GH_RELEASE_EXISTS=0 run "$SCRIPT" 0.2.0
	[ "$status" -eq 0 ]
	grep -q "## Use it" "$GH_NOTES_FILE"
	grep -q -- "--generate-notes" "$GH_ARGS_FILE"
}

@test "create-github-release: updates the notes of an existing release" {
	mkdir -p changelog
	printf '## Highlights\n\nSomething worth saying.\n' >changelog/0.2.0.md

	GH_RELEASE_EXISTS=1 \
		GH_EXISTING_BODY=$'## Use it\n\nold boilerplate\n\n## What'"'"'s Changed\n* a PR by @someone\n' \
		run "$SCRIPT" 0.2.0
	[ "$status" -eq 0 ]

	grep -q -- "release edit" "$GH_ARGS_FILE"
	grep -q "Something worth saying." "$GH_NOTES_FILE"
	# the generated changelog from the existing body is preserved
	grep -q "a PR by @someone" "$GH_NOTES_FILE"
	# ...and the stale boilerplate above it is not carried over twice
	[ "$(grep -c '## Use it' "$GH_NOTES_FILE")" -eq 1 ]
}

@test "create-github-release: generates a changelog when the existing body has none" {
	mkdir -p changelog
	printf '## Highlights\n\nhi\n' >changelog/0.2.0.md

	GH_RELEASE_EXISTS=1 GH_EXISTING_BODY='## Use it' \
		GH_GENERATED=$'## What'"'"'s Changed\n* freshly generated\n' \
		run "$SCRIPT" 0.2.0
	[ "$status" -eq 0 ]
	grep -q "freshly generated" "$GH_NOTES_FILE"
}
