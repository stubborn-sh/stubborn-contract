#!/usr/bin/env bash
#
# Validate a release version string. Accepts SemVer-ish X.Y.Z with an optional
# -qualifier (e.g. 0.1.0, 0.1.0-RC1, 1.20.30-alpha.1). On mismatch it prints a
# GitHub Actions error annotation and exits non-zero; on success it echoes the
# version back.
#
# Usage: validate-version.sh <version>
#
set -euo pipefail

VERSION="${1:-}"
if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.]+)?$ ]]; then
	echo "::error::Invalid version format: '${VERSION}' (expected e.g. 0.1.0 or 0.1.0-RC1)"
	exit 1
fi
echo "$VERSION"
