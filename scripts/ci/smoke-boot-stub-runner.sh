#!/usr/bin/env bash
#
# smoke-boot-stub-runner.sh
#
# Boot the stubborn-contract-stub-runner Docker image and prove it actually
# starts. This is the check that was missing when the image shipped a thin,
# non-executable library jar and every consumer got:
#
#   Running Spring Cloud Contract Stub Runner
#   no main manifest attribute, in /home/scc/stub-runner-boot.jar
#
# (stubborn-sh/stubborn#91). The publish/acceptance workflows verified the image
# EXISTED in the registry but never ran it, so a non-booting image published
# green. This script closes that gap: it runs the container and fails unless the
# Spring Boot Stub Runner server reports it has started AND its HTTP port answers.
#
# Usage: smoke-boot-stub-runner.sh <image-ref> [host-port]
#
#   <image-ref>  full image reference to run, e.g.
#                mgrzejszczak/stubborn-contract-stub-runner:0.1.3-SNAPSHOT
#   [host-port]  host port mapped to the container's 8750 (default 8750)
#
# Environment:
#   DOCKER        docker binary to use (default: docker)
#   BOOT_TIMEOUT  seconds to wait for the startup log line (default: 120)
#
set -euo pipefail

IMAGE="${1:?image reference required}"
HOST_PORT="${2:-8750}"
DOCKER="${DOCKER:-docker}"
BOOT_TIMEOUT="${BOOT_TIMEOUT:-120}"

# The application binds server.port=8750 inside the container (Dockerfile ENV +
# application.yml). The stub runner server maps /stubs to the set of running
# stubs; with no stub ids configured it still boots the web server and serves an
# empty set, which is all this smoke test needs.
CONTAINER_PORT=8750
STARTED_MARKER="Started StubRunnerBoot"

cid=""
cleanup() {
	if [ -n "${cid}" ]; then
		"$DOCKER" logs "${cid}" > /tmp/stub-runner-boot.log 2>&1 || true
		"$DOCKER" rm -f "${cid}" >/dev/null 2>&1 || true
	fi
}
trap cleanup EXIT

echo "Booting ${IMAGE} (host port ${HOST_PORT} -> ${CONTAINER_PORT}) ..."
# Not --rm: keep the container around so its logs survive a crash for diagnosis.
cid="$("$DOCKER" run -d -p "${HOST_PORT}:${CONTAINER_PORT}" "${IMAGE}")"
echo "container: ${cid}"

# Wait for the Spring Boot startup line, bailing out early if the container dies
# (a non-executable jar exits within a second).
started=""
for _ in $(seq 1 "${BOOT_TIMEOUT}"); do
	if "$DOCKER" logs "${cid}" 2>&1 | grep -q "${STARTED_MARKER}"; then
		started="yes"
		break
	fi
	if [ -z "$("$DOCKER" ps -q --no-trunc --filter "id=${cid}")" ]; then
		echo "container exited before it started up; log:" >&2
		"$DOCKER" logs "${cid}" 2>&1 | tail -n 40 >&2 || true
		exit 1
	fi
	sleep 1
done

if [ -z "${started}" ]; then
	echo "did not see '${STARTED_MARKER}' within ${BOOT_TIMEOUT}s; log:" >&2
	"$DOCKER" logs "${cid}" 2>&1 | tail -n 40 >&2 || true
	exit 1
fi
echo "startup log line seen: ${STARTED_MARKER}"

# Confirm the HTTP server is actually listening. Any non-000 status proves the
# port answered; 200 means the empty stub set was served.
http_code="000"
for _ in $(seq 1 30); do
	http_code="$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:${HOST_PORT}/stubs" || true)"
	if [ -n "${http_code}" ] && [ "${http_code}" != "000" ]; then
		break
	fi
	sleep 1
done

if [ "${http_code}" = "000" ] || [ -z "${http_code}" ]; then
	echo "HTTP server never answered on port ${HOST_PORT}; log:" >&2
	"$DOCKER" logs "${cid}" 2>&1 | tail -n 40 >&2 || true
	exit 1
fi

echo "HTTP /stubs answered with ${http_code}"
echo "OK: ${IMAGE} boots and serves."
