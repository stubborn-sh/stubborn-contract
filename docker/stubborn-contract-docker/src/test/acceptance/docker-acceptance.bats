#!/usr/bin/env bats
#
# Acceptance test for the `stubborn-contract` Docker image.
#
# WHAT IT PROVES
#   Build the image -> run a container against a minimal fixture contract
#   project -> assert the container exits 0, generates the contract test
#   sources, and emits the `*-stubs.jar`. This is a test *of the image*, as
#   opposed to project/src/test/... which is the image's *in-image* harness.
#
# CI-VALIDATED ONLY
#   Requires a working Docker daemon. It was authored by inspection and has NOT
#   been executed in the dev sandbox (no Docker there). Expect it may need a CI
#   round or two to go green; assertions and logging are kept deliberately
#   simple so the first run is easy to debug.
#
# HOW IT IS DRIVEN
#   The container is run ONCE in setup_file; each @test inspects the captured
#   result. Inputs come from environment variables:
#     SC_DOCKER_IMAGE  (required) full image ref, e.g.
#                      stubborn-acceptance/stubborn-contract:0.1.0-SNAPSHOT
#     FIXTURE_PORT     (optional) host port for the fixture app (default 8888)
#     SC_DEBUG         (optional) "true" turns on the image's Gradle --debug
#
#   The image is built by the CI workflow (.github/workflows/docker-acceptance.yml)
#   before this runs. To build + run locally in one shot instead, set
#   SC_BUILD_IMAGE=true and this file will invoke ./build-image.sh first.

setup_file() {
  ACCEPTANCE_DIR="$( cd "$( dirname "$BATS_TEST_FILENAME" )" && pwd )"
  export ACCEPTANCE_DIR
  export FIXTURE_PORT="${FIXTURE_PORT:-8888}"

  # Optionally build the image locally (CI builds it in a dedicated step).
  if [ "${SC_BUILD_IMAGE:-false}" = "true" ]; then
    echo "# building image via build-image.sh ..." >&3
    SC_DOCKER_IMAGE="$( "${ACCEPTANCE_DIR}/build-image.sh" )"
    export SC_DOCKER_IMAGE
  fi

  if [ -z "${SC_DOCKER_IMAGE:-}" ]; then
    echo "SC_DOCKER_IMAGE is not set (and SC_BUILD_IMAGE!=true). Cannot run." >&3
    return 1
  fi
  echo "# using image: ${SC_DOCKER_IMAGE}" >&3

  WORK_DIR="$( mktemp -d )"
  export WORK_DIR
  export OUTPUT_DIR="${WORK_DIR}/output"
  mkdir -p "${OUTPUT_DIR}"
  # The image runs as uid 1000 (user scc); make the bind-mounted output dir
  # world-writable so the container can write results back to the host.
  chmod -R 777 "${WORK_DIR}"

  # Start the fixture app the EXPLICIT-mode generated test will hit.
  python3 "${ACCEPTANCE_DIR}/fixtures/app.py" "${FIXTURE_PORT}" \
    > "${WORK_DIR}/app.log" 2>&1 &
  echo $! > "${WORK_DIR}/app.pid"

  # Wait for the fixture app to become reachable.
  local ready=""
  for _ in $(seq 1 30); do
    if curl -fsS "http://localhost:${FIXTURE_PORT}/health" > /dev/null 2>&1; then
      ready="yes"; break
    fi
    sleep 1
  done
  if [ -z "${ready}" ]; then
    echo "# fixture app never became ready; log:" >&3
    cat "${WORK_DIR}/app.log" >&3 || true
  fi

  # Run the image once. Capture exit code + logs for the assertions below.
  #   PUBLISH_ARTIFACTS=false  -> do not try to publish to an artifact manager
  #                               (there is none in CI); keeps the run offline.
  #   FAIL_ON_NO_CONTRACTS=true-> fail loudly if /contracts was not picked up.
  #   --network host           -> let the container reach the host fixture app
  #                               at localhost:${FIXTURE_PORT}.
  set +e
  docker run --rm \
    --network host \
    -e APPLICATION_BASE_URL="http://localhost:${FIXTURE_PORT}" \
    -e PROJECT_NAME="acceptance" \
    -e PROJECT_GROUP="com.example" \
    -e PROJECT_VERSION="0.0.1" \
    -e PUBLISH_ARTIFACTS="false" \
    -e FAIL_ON_NO_CONTRACTS="true" \
    -e DEBUG="${SC_DEBUG:-false}" \
    -v "${ACCEPTANCE_DIR}/contracts:/contracts:ro" \
    -v "${OUTPUT_DIR}:/stubborn-contract-output" \
    "${SC_DOCKER_IMAGE}" \
    > "${WORK_DIR}/container.log" 2>&1
  echo $? > "${WORK_DIR}/container.exit"
  set -e

  echo "# ----- container log (last 60 lines) -----" >&3
  tail -n 60 "${WORK_DIR}/container.log" >&3 || true
  echo "# ----- output tree -----" >&3
  find "${OUTPUT_DIR}" -maxdepth 3 >&3 2>/dev/null || true
}

teardown_file() {
  if [ -n "${WORK_DIR:-}" ] && [ -f "${WORK_DIR}/app.pid" ]; then
    kill "$( cat "${WORK_DIR}/app.pid" )" 2>/dev/null || true
  fi
  if [ -n "${WORK_DIR:-}" ]; then
    rm -rf "${WORK_DIR}" || true
  fi
}

@test "fixture app answers GET /health with UP" {
  run curl -fsS "http://localhost:${FIXTURE_PORT}/health"
  [ "$status" -eq 0 ]
  [[ "$output" == *"UP"* ]]
}

@test "container exits 0" {
  local code
  code="$( cat "${WORK_DIR}/container.exit" )"
  if [ "$code" -ne 0 ]; then
    echo "# container exited ${code}; full log:" >&3
    cat "${WORK_DIR}/container.log" >&3 || true
  fi
  [ "$code" -eq 0 ]
}

@test "generated contract test sources exist" {
  # The gradle plugin writes generated *.java test sources under build/, which
  # copyOutput mirrors into /stubborn-contract-output. The only *.java that can
  # appear there are generated ones (base classes live under src/, not build/).
  run bash -c "find '${OUTPUT_DIR}' -name '*.java' | head -n1"
  [ "$status" -eq 0 ]
  [ -n "$output" ]
}

@test "stub jar is produced" {
  run bash -c "find '${OUTPUT_DIR}' -name '*-stubs.jar' | head -n1"
  [ "$status" -eq 0 ]
  [ -n "$output" ]
}
