#!/usr/bin/env bash
# Tests for check-docs-links.sh
# Spins up real npx serve instances against controlled dist dirs.
# Usage: bash scripts/check-docs-links.test.sh

set -euo pipefail

PASS=0
FAIL=0
PIDS=()

cleanup() {
  for pid in "${PIDS[@]:-}"; do
    kill "$pid" 2>/dev/null || true
  done
}
trap cleanup EXIT

assert_contains() {
  local label="$1" expected="$2" actual="$3"
  if echo "$actual" | grep -qF "$expected"; then
    echo "PASS: $label"
    PASS=$((PASS + 1))
  else
    echo "FAIL: $label"
    echo "  Expected: $expected"
    echo "  Output:"; echo "$actual" | sed 's/^/    /'
    FAIL=$((FAIL + 1))
  fi
}

assert_exit() {
  local label="$1" expected="$2" actual="$3"
  if [ "$actual" -eq "$expected" ]; then
    echo "PASS: $label (exit $actual)"
    PASS=$((PASS + 1))
  else
    echo "FAIL: $label (expected exit $expected, got $actual)"
    FAIL=$((FAIL + 1))
  fi
}

wait_for_port() {
  local port="$1"
  for _ in $(seq 1 20); do
    if curl -s -o /dev/null "http://localhost:$port" 2>/dev/null; then return 0; fi
    sleep 0.3
  done
  echo "Server on port $port never became ready" >&2
  return 1
}

# ===== Test 1: all pages return 200 — script exits 0 =====
DIST1=$(mktemp -d)
trap 'rm -rf "$DIST1"' EXIT
mkdir -p "$DIST1/getting-started"
echo "<html><body>Home</body></html>"            > "$DIST1/index.html"
echo "<html><body>Getting Started</body></html>" > "$DIST1/getting-started/index.html"

OUTPUT1=$(DIST="$DIST1" PORT=19181 bash scripts/check-docs-links.sh 2>/dev/null) || true
EXIT1=$?

assert_contains "all-ok: reports index.html as OK"           "OK   [200]" "$OUTPUT1"
assert_contains "all-ok: reports getting-started as OK"      "OK   [200]" "$OUTPUT1"
assert_contains "all-ok: counts 2 pages"                     "Checked 2 pages" "$OUTPUT1"
assert_contains "all-ok: reports 0 failures"                 "0 failures" "$OUTPUT1"
assert_exit     "all-ok: exits 0"                            0 "$EXIT1"

# ===== Test 2: real built docs have zero 404s =====
if [ -d "docs/.vitepress/dist" ]; then
  OUTPUT2=$(bash scripts/check-docs-links.sh 2>/dev/null) || true
  EXIT2=$?
  assert_contains "built-docs: reports 0 failures"  "0 failures" "$OUTPUT2"
  assert_exit     "built-docs: exits 0"             0 "$EXIT2"
else
  echo "SKIP: docs/.vitepress/dist not found — run npm run docs:build first"
fi

# ===== Summary =====
echo ""
echo "Results: $PASS passed, $FAIL failed."
[ "$FAIL" -eq 0 ]
