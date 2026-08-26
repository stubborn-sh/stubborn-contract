#!/usr/bin/env bats
#
# Acceptance test for the `stubborn-contract-stub-runner` Docker image.
#
# WHAT IT PROVES
#   `docker run` the image and confirm it actually BOOTS: the Spring Boot Stub
#   Runner server starts from the baked stub-runner-boot.jar and its HTTP port
#   answers. This is a test *of the image*, and it is the check that was missing
#   when the image shipped a thin, non-executable library jar and every consumer
#   got "no main manifest attribute, in /home/scc/stub-runner-boot.jar"
#   (stubborn-sh/stubborn#91).
#
# CI-VALIDATED ONLY
#   Requires a working Docker daemon; not executed in the dev sandbox.
#
# HOW IT IS DRIVEN
#   The boot + assertions live in scripts/ci/smoke-boot-stub-runner.sh so the
#   exact same check runs here (against the locally built image) and in
#   publish-docker.yml (against the pushed image). Inputs come from environment:
#     SR_DOCKER_IMAGE  (required) full image ref, e.g.
#                      stubborn-acceptance/stubborn-contract-stub-runner:0.1.3-SNAPSHOT
#     SR_PORT          (optional) host port mapped to the container's 8750
#                      (default 8750)

setup_file() {
  ACCEPTANCE_DIR="$( cd "$( dirname "$BATS_TEST_FILENAME" )" && pwd )"
  # repo root is five levels up:
  # docker/stubborn-contract-stub-runner-docker/src/test/acceptance
  REPO_ROOT="$( cd "${ACCEPTANCE_DIR}/../../../../.." && pwd )"
  export SMOKE_SCRIPT="${REPO_ROOT}/scripts/ci/smoke-boot-stub-runner.sh"
  export SR_PORT="${SR_PORT:-8750}"

  if [ -z "${SR_DOCKER_IMAGE:-}" ]; then
    echo "SR_DOCKER_IMAGE is not set. Cannot run." >&3
    return 1
  fi
  echo "# using image: ${SR_DOCKER_IMAGE}" >&3
}

@test "stub-runner image boots and serves HTTP" {
  run bash "${SMOKE_SCRIPT}" "${SR_DOCKER_IMAGE}" "${SR_PORT}"
  echo "# ----- smoke-boot output -----" >&3
  echo "${output}" >&3
  [ "$status" -eq 0 ]
}
