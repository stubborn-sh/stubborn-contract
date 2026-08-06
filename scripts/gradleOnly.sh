#!/usr/bin/env bash

source common.sh || source scripts/common.sh || echo "No common.sh script found..."

FOLDER=`pwd`

set -e

cd "${FOLDER}/stubborn-contract-extras/stubborn-contract-gradle-plugin"
  ./gradlew clean build publishToMavenLocal
cd "${FOLDER}"
