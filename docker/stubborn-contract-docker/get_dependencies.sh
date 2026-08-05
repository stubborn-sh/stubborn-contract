#!/bin/bash

set -e

WRAPPER_VERSION="8.14.3"
GRADLE_BIN_DIR="gradle-${WRAPPER_VERSION}-bin"
GRADLE_WRAPPER_DIR="${HOME}/.gradle/wrapper/dists/${GRADLE_BIN_DIR}"
CURRENT_DIR="$( pwd )"
GRADLE_OUTPUT_DIR="${CURRENT_DIR}/target/gradle_dependencies/"
# gradle.properties in source carries Maven placeholders (${project.version}, ...). Use the
# Maven-filtered copy (staged by maven-resources-plugin) for the host-side pre-download so gradle
# resolves real versions, then restore the source file so the checkout stays clean.
FILTERED_PROPS="${CURRENT_DIR}/target/docker-context/gradle.properties"
pushd project
  rm -rf .gradle
  if [ -f "${FILTERED_PROPS}" ]; then
    cp gradle.properties gradle.properties.orig
    cp "${FILTERED_PROPS}" gradle.properties
  fi
  ./gradlew wrapper --gradle-version "${WRAPPER_VERSION}"
  ./gradlew clean resolveDependencies build -g "${GRADLE_OUTPUT_DIR}" -x copyOutput || echo "Expected to fail the build"
  if [ -f gradle.properties.orig ]; then
    mv gradle.properties.orig gradle.properties
  fi
  if [ -d "${GRADLE_WRAPPER_DIR}" ]; then
      echo "Copying Gradle Wrapper version [${WRAPPER_VERSION}]"
      mkdir -p "${GRADLE_OUTPUT_DIR}/wrapper/dists/"
      cp -r "${GRADLE_WRAPPER_DIR}" "${GRADLE_OUTPUT_DIR}/wrapper/dists/"
  else
      echo "Gradle Wrapper [${GRADLE_WRAPPER_DIR}] not found. Will not copy it"
  fi
popd

./build_adocs.sh

pushd project
  rm -rf build
popd
