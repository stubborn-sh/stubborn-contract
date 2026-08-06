#!/usr/bin/env bash
#
# Build the stubborn-contract Docker image locally, WITHOUT pushing it.
#
# This mirrors what .github/workflows/docker-acceptance.yml does, so you can
# reproduce a CI acceptance run on a machine that has Docker. It assumes the
# reactor artifacts are already installed in your local ~/.m2 (run the reactor
# install steps below first if not).
#
# Prints the resulting image ref on stdout (and nothing else there), so it can
# be captured:  SC_DOCKER_IMAGE="$(./build-image.sh)"
#
# Everything diagnostic goes to stderr.
set -o errexit
set -o nounset
set -o pipefail

# Repo root is five levels up from this script:
#   docker/stubborn-contract-docker/src/test/acceptance/build-image.sh
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
REPO_ROOT="$( cd "${SCRIPT_DIR}/../../../../.." && pwd )"
cd "${REPO_ROOT}"

DOCKER_ORG="${DOCKER_ORG:-stubborn-acceptance}"

log() { echo "[build-image] $*" >&2; }

if [ "${SC_INSTALL_REACTOR:-false}" = "true" ]; then
  log "Installing reactor artifacts into ~/.m2 (this is slow) ..."
  ./mvnw install -f stubborn-contract-build/pom.xml -DskipTests --no-transfer-progress -q >&2
  find ~/.m2/repository/sh/stubborn -name "*.lastUpdated" -delete 2>/dev/null || true
  ./mvnw install -N -f stubborn-contract-dependencies/pom.xml -DskipTests --no-transfer-progress -q >&2
  ./mvnw install -DskipTests --no-transfer-progress -q >&2
  ./mvnw install -N -f docker/pom.xml -DskipTests --no-transfer-progress -q >&2
fi

VERSION="$( ./mvnw -q -N help:evaluate \
  -Dexpression=project.version -DforceStdout \
  -f docker/stubborn-contract-docker/pom.xml 2>/dev/null )"
log "Project version: ${VERSION}"

log "Building image (dockerfile-maven build, push skipped) ..."
./mvnw package -f docker/stubborn-contract-docker/pom.xml \
  -Ddocker.registry.organization="${DOCKER_ORG}" \
  -Ddockerfile.push.skip=true \
  -DskipTests --no-transfer-progress >&2

IMAGE="${DOCKER_ORG}/stubborn-contract:${VERSION}"
log "Built image: ${IMAGE}"
# Only the image ref goes to stdout.
echo "${IMAGE}"
