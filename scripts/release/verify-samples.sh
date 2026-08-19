#!/usr/bin/env bash
#
# Pre-publish release gate: build the stubborn-samples suite against a specific
# stubborn-contract release version.
#
# The Release workflow runs this AFTER `mvn clean install` has populated the local
# ~/.m2 with the fully-versioned release artifacts and BEFORE the deploy/publish steps.
# The samples resolve stubborn-contract from that local repository (the exact bytes that
# will be deployed), so a functional regression in a release — e.g. a messaging sample
# that no longer verifies — fails this script and aborts the release before anything is
# published to Maven Central.
#
# It mirrors the Maven jobs of stubborn-samples' own CI (.github/workflows/ci.yml):
# per sample, build the producer (install), then any standalone module, then the
# consumer(s); plus the SCC <-> Stubborn compatibility flow. The stubborn-contract
# version each sample pins is overridden on the command line to ${VERSION}.
#
# Gradle and the Quarkus framework samples are intentionally out of scope here (Gradle
# takes the version differently and the Quarkus samples need JDK 21); the Maven jobs
# cover the functional consumer-driven verification this gate exists for.
#
# Usage: verify-samples.sh <release-version>
#
# Env overrides:
#   SAMPLES_REPO_URL  git URL of the samples repo (default the public stubborn-samples)
#   SAMPLES_REF       branch/tag to check out (default main)
#
set -euo pipefail

VERSION="${1:?Usage: verify-samples.sh <release-version>}"
SAMPLES_REPO_URL="${SAMPLES_REPO_URL:-https://github.com/stubborn-sh/stubborn-samples.git}"
SAMPLES_REF="${SAMPLES_REF:-main}"

WORKDIR="$(mktemp -d)"
trap 'rm -rf "${WORKDIR}"' EXIT

echo "Verifying stubborn-samples (${SAMPLES_REF}) against stubborn-contract ${VERSION}"
git clone --depth 1 --branch "${SAMPLES_REF}" "${SAMPLES_REPO_URL}" "${WORKDIR}/samples"
cd "${WORKDIR}/samples"

# Override the pinned stubborn-contract.version so every sample resolves the release
# artifacts from the local ~/.m2 rather than its default snapshot.
MVN_ARGS=(-B --no-transfer-progress "-Dstubborn-contract.version=${VERSION}")

FAILED=()

# Build one Maven module directory (clean install), recording a failure without aborting
# the whole loop so the summary lists every broken sample.
build_module() {
	local dir="$1"
	[ -f "${dir}/pom.xml" ] || return 0
	echo "::group::verify ${dir}"
	if ! ( cd "${dir}" && ./mvnw "${MVN_ARGS[@]}" clean install ); then
		echo "::error::sample verification failed: ${dir}"
		FAILED+=("${dir}")
	fi
	echo "::endgroup::"
}

# Maven samples — mirrors stubborn-samples ci.yml `build-maven` (producer -> standalone
# -> consumer(s)). A sample is either a producer/consumer pair or a single standalone
# module.
MAVEN_SAMPLES=(
	sample-http
	sample-messaging
	sample-kafka
	sample-rest-assured
	sample-webflux
	sample-stream
	sample-jms
	sample-restdocs
	sample-security
	sample-wiremock
	sample-stubs-per-consumer
	sample-graphql
	sample-grpc
)
for s in "${MAVEN_SAMPLES[@]}"; do
	if [ -f "${s}/producer/pom.xml" ]; then
		build_module "${s}/producer"
	elif [ -f "${s}/pom.xml" ]; then
		build_module "${s}"
	fi
	build_module "${s}/consumer"
	build_module "${s}/consumer-a"
	build_module "${s}/consumer-b"
done

# Compatibility flow — mirrors ci.yml `build-compatibility`: an SCC producer's stubs are
# consumed by a Stubborn consumer and vice versa.
compat() {
	local dir="$1" goal="$2"
	[ -f "${dir}/pom.xml" ] || return 0
	echo "::group::verify ${dir} (${goal})"
	if ! ( cd "${dir}" && ./mvnw "${MVN_ARGS[@]}" "${goal}" ); then
		echo "::error::sample verification failed: ${dir}"
		FAILED+=("${dir}")
	fi
	echo "::endgroup::"
}
compat sample-compatibility/scc-to-stubborn/producer install
compat sample-compatibility/scc-to-stubborn/consumer test
compat sample-compatibility/stubborn-to-scc/producer install
compat sample-compatibility/stubborn-to-scc/consumer test

if [ "${#FAILED[@]}" -ne 0 ]; then
	echo "::error::${#FAILED[@]} sample(s) failed against stubborn-contract ${VERSION}:" >&2
	printf '  - %s\n' "${FAILED[@]}" >&2
	exit 1
fi

echo "All verified samples pass against stubborn-contract ${VERSION}."
