#!/bin/bash

set -o errexit
set -o errtrace
set -o nounset
set -o pipefail

echo "Running Spring Cloud Contract Stub Runner"
ADDITIONAL_OPTS="${ADDITIONAL_OPTS:-}"
MESSAGING_TYPE="${MESSAGING_TYPE:-}"

if [[ "${MESSAGING_TYPE}" != "" ]]; then
  echo "Messaging type passed, will activate profile [${MESSAGING_TYPE}]"
  ADDITIONAL_OPTS="${ADDITIONAL_OPTS} --spring.profiles.active=${MESSAGING_TYPE}"
fi

exec \
 java \
  -Djava.security.egd=file:/dev/./urandom \
  -jar /home/scc/stub-runner-boot.jar \
  ${ADDITIONAL_OPTS} \
  "$@"
