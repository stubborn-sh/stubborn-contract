#!/usr/bin/env bash
# Serves the built VitePress docs locally and checks every internal link for 404s.
# Requires: node (for serve), curl
# Usage: ./scripts/check-docs-links.sh

set -euo pipefail

DIST="docs/.vitepress/dist"
BASE="/stubborn-contract"
PORT=4173

if [ ! -d "$DIST" ]; then
  echo "Building docs first..."
  npm run docs:build
fi

# Start a static file server in the background
npx --yes serve "$DIST" --listen "$PORT" --single &
SERVER_PID=$!
trap 'kill $SERVER_PID 2>/dev/null' EXIT

# Wait for it to be ready
sleep 2

BASE_URL="http://localhost:$PORT$BASE"
FAILED=0
CHECKED=0

echo "Checking links under $BASE_URL ..."

# Collect all .html files in the dist and derive their URL paths
while IFS= read -r -d '' html_file; do
  rel="${html_file#$DIST}"          # e.g. /getting-started/index.html
  url="http://localhost:$PORT${rel}" # no base prefix needed — serve serves from dist root
  status=$(curl -s -o /dev/null -w "%{http_code}" "$url")
  CHECKED=$((CHECKED + 1))
  if [ "$status" != "200" ]; then
    echo "FAIL [$status] $url"
    FAILED=$((FAILED + 1))
  else
    echo "OK   [$status] $url"
  fi
done < <(find "$DIST" -name "*.html" -print0)

echo ""
echo "Checked $CHECKED pages — $FAILED failures."
[ "$FAILED" -eq 0 ]
