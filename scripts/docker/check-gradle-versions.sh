#!/usr/bin/env bash
# Fail the build if the Docker image's gradle.properties has drifted from Maven.
#
# The Docker image's Gradle build reads project/gradle.properties across several invocations,
# so that file carries CONCRETE version values (not ${...} Maven placeholders — those do not
# reliably reach every reader and leave Gradle resolving literal "${project.version}"). Maven
# stays the single source of truth by way of this check: the docker module runs it in the
# validate phase, passing the Maven-resolved values, and the build fails if any concrete value
# in the file disagrees.
#
# Usage: check-gradle-versions.sh <gradle.properties> <expectedVerifier> <expectedSpringBoot> <expectedCamel>
set -euo pipefail

FILE="${1:?Usage: check-gradle-versions.sh <gradle.properties> <expectedVerifier> <expectedSpringBoot> <expectedCamel>}"
EXPECTED_VERIFIER="${2:?missing expected verifierVersion}"
EXPECTED_SPRING_BOOT="${3:?missing expected springBootVersion}"
EXPECTED_CAMEL="${4:?missing expected camelVersion}"

if [ ! -f "${FILE}" ]; then
	echo "error: gradle.properties not found: ${FILE}" >&2
	exit 1
fi

read_prop() {
	# Prints the value of the given key from FILE (first match, value after '=').
	sed -n -E "s#^$1=(.*)#\1#p" "${FILE}" | head -n1
}

ACTUAL_VERIFIER="$(read_prop verifierVersion)"
ACTUAL_SPRING_BOOT="$(read_prop springBootVersion)"
ACTUAL_CAMEL="$(read_prop camelVersion)"

status=0
check() {
	# $1 = key, $2 = expected, $3 = actual
	if [ "$2" != "$3" ]; then
		echo "error: ${FILE}: $1 is '$3' but Maven resolves '$2' — update ${FILE}" >&2
		status=1
	fi
}

check verifierVersion "${EXPECTED_VERIFIER}" "${ACTUAL_VERIFIER}"
check springBootVersion "${EXPECTED_SPRING_BOOT}" "${ACTUAL_SPRING_BOOT}"
check camelVersion "${EXPECTED_CAMEL}" "${ACTUAL_CAMEL}"

if [ "${status}" -ne 0 ]; then
	exit 1
fi

echo "OK: ${FILE} matches Maven (verifierVersion=${EXPECTED_VERIFIER}, springBootVersion=${EXPECTED_SPRING_BOOT}, camelVersion=${EXPECTED_CAMEL})"
