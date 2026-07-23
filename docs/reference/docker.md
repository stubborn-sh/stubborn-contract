# Docker

In this section, we publish a `springcloud/spring-cloud-contract` Docker image that contains a project that generates tests and runs them in `EXPLICIT` mode against a running application.

::: tip
The `EXPLICIT` mode means that the tests generated from contracts send real requests and not mocked ones.
:::

We also publish a `spring-cloud/stubborn-stub-runner` Docker image that starts the standalone version of Stub Runner.

## A Short Introduction to Maven, JARs, and Binary Storage

Since non-JVM projects can use the Docker image, it is good to explain the basic terms behind Stubborn Contract packaging defaults.

- **Project**: Maven thinks in terms of projects. Projects are all you build. Those projects follow a well defined "Project Object Model". Projects can depend on other projects — in that case, the latter are called "dependencies". A project may consist of several subprojects. However, these subprojects are still treated equally as projects.
- **Artifact**: An artifact is something that is either produced or used by a project. Examples of artifacts produced by Maven for a project include JAR files and source and binary distributions. Each artifact is uniquely identified by a group ID and an artifact ID that is unique within a group.
- **JAR**: JAR stands for Java ARchive. Its format is based on the ZIP file format. Stubborn Contract packages the contracts and generated stubs in a JAR file.
- **GroupId**: A group ID is a universally unique identifier for a project. While this is often just the project name (for example, `commons-collections`), it is helpful to use a fully-qualified package name to distinguish it from other projects with a similar name (for example, `org.apache.maven`). Typically, when published to the Artifact Manager, the `GroupId` gets slash separated and forms part of the URL.
- **Classifier**: The Maven dependency notation looks as follows: `groupId:artifactId:version:classifier`. The classifier is an additional suffix passed to the dependency — for example, `stubs` or `sources`.
- **Artifact manager**: When you generate binaries, sources, or packages, you would like them to be available for others to download, reference, or reuse. Examples of such managers include [Artifactory](https://jfrog.com/artifactory/) and [Nexus](https://www.sonatype.org/nexus/).

## Generating Tests on the Producer Side

The image searches for contracts under the `/contracts` folder. The output from running the tests is available in the `/spring-cloud-contract/build` folder (useful for debugging purposes).

You can mount your contracts and pass the environment variables. The image then:

- Generates the contract tests
- Runs the tests against the provided URL
- Generates the [WireMock](https://github.com/tomakehurst/wiremock) stubs
- Publishes the stubs to an Artifact Manager (optional — turned on by default)

### Environment Variables

The Docker image requires some environment variables to point to your running application, to the Artifact manager instance, and so on.

**General environment variables:**

- `APPLICATION_BASE_URL` — URL at which the application is running
- `PUBLISH_ARTIFACTS` — if set to `true`, publishes the generated stubs to the artifact manager
- `PROJECT_NAME` — the artifact ID of your project
- `PROJECT_GROUP` — the group ID of your project
- `PROJECT_VERSION` — the version of your project
- `REPO_WITH_BINARIES_URL` — URL of the artifact manager (e.g., Artifactory)
- `REPO_WITH_BINARIES_USERNAME` — username for the artifact manager
- `REPO_WITH_BINARIES_PASSWORD` — password for the artifact manager
- `PUBLISH_STUBS_TO_SCM` — if set to `true`, publishes stubs to an SCM repository
- `MESSAGING_TYPE` — messaging type (`rabbit` or `kafka`)
- `FAIL_ON_NO_CONTRACTS` — if set to `true`, fails when no contracts are found. Defaults to `true`.
- `EXTERNAL_CONTRACTS_REPO_WITH_BINARIES_URL` — URL of an external repository with contracts
- `EXTERNAL_CONTRACTS_ARTIFACT_ID` — artifact ID for external contracts
- `EXTERNAL_CONTRACTS_GROUP_ID` — group ID for external contracts
- `EXTERNAL_CONTRACTS_VERSION` — version of external contracts

### Customizing the Gradle Build

You can provide a customized `gradle.build` to be run in the container by mounting your customized build file as a volume when running the container:

```bash
$ docker run -v <absolute-path-of-your-custom-file>:/spring-cloud-contract/build.gradle springcloud/spring-cloud-contract:<version>
```

### Example of Usage via HTTP

In this section, we explore a simple MVC application. To get started, clone the following git repository and cd to the resulting directory:

```bash
$ git clone https://github.com/stubborn-sh/stubborn-samples
$ cd bookstore
```

The contracts are available in the `/contracts` folder.

Since we want to run tests, we can run the following command:

```bash
$ npm test
```

However, for learning purposes, we split it into pieces, as follows:

```bash
# Stop docker infra (nodejs, artifactory)
$ ./stop_infra.sh
# Start docker infra (nodejs, artifactory)
$ ./setup_infra.sh

# Kill & Run app
$ pkill -f "node app"
$ nohup node app &

# Prepare environment variables
$ SC_CONTRACT_DOCKER_VERSION="..."
$ APP_IP="192.168.0.100"
$ APP_PORT="3000"
$ ARTIFACTORY_PORT="8081"
$ APPLICATION_BASE_URL="http://${APP_IP}:${APP_PORT}"
$ ARTIFACTORY_URL="http://${APP_IP}:${ARTIFACTORY_PORT}/artifactory/libs-release-local"
$ CURRENT_DIR="$( pwd )"
$ CURRENT_FOLDER_NAME=${PWD##*/}
$ PROJECT_VERSION="0.0.1.RELEASE"

# Run contract tests
$ docker run  --rm \
  -e "APPLICATION_BASE_URL=${APPLICATION_BASE_URL}" \
  -e "PUBLISH_ARTIFACTS=true" \
  -e "PROJECT_NAME=${CURRENT_FOLDER_NAME}" \
  -e "REPO_WITH_BINARIES_URL=${ARTIFACTORY_URL}" \
  -e "PROJECT_VERSION=${PROJECT_VERSION}" \
  -v "${CURRENT_DIR}/contracts/:/contracts:ro" \
  -v "${CURRENT_DIR}/node_modules/spring-cloud-contract/output:/spring-cloud-contract-output/" \
  springcloud/spring-cloud-contract:"${SC_CONTRACT_DOCKER_VERSION}"

# Kill app
$ pkill -f "node app"
```

Through bash scripts, the following happens:

- The infrastructure (MongoDb and Artifactory) is set up. In a real-life scenario, you would run the NodeJS application with a mocked database. In this example, we want to show how we can benefit from Stubborn Contract in very little time.
- Due to those constraints, the contracts also represent the stateful situation.
  - The first request is a `POST` that causes data to get inserted into the database.
  - The second request is a `GET` that returns a list of data with 1 previously inserted element.
- The NodeJS application is started (on port `3000`).
- The contract tests are generated through Docker, and tests are run against the running application.
  - The contracts are taken from `/contracts` folder.
  - The output of the test is available under `node_modules/spring-cloud-contract/output`.
- The stubs are uploaded to Artifactory. You can find them in `http://localhost:8081/artifactory/libs-release-local/com/example/bookstore/0.0.1.RELEASE/`.

### Example of Usage via Messaging

If you want to use Stubborn Contract with messaging via the Docker images (for example, in case of polyglot applications), then you'll have to have the following prerequisites met:

- Middleware (e.g. RabbitMQ or Kafka) must be running before generating tests
- Your contract needs to call a method `triggerMessage(...)` with a `String` parameter that is equal to the contract's `label`
- Your application needs to have an HTTP endpoint via which we can trigger a message
  - That endpoint should not be available on production (could be enabled via an environment variable)

#### Example of a Messaging Contract

The contract needs to call a `triggerMessage(...)` method. That method is already provided in the base class for all tests in the docker image and will send out a request to the HTTP endpoint on the producer side.

**Groovy:**

```groovy
import sh.stubborn.contract.spec.Contract

Contract.make {
    description 'Send a pong message in response to a ping message'
    label 'ping_pong'
    input {
        triggeredBy('triggerMessage("ping_pong")')
    }
    outputMessage {
        sentTo('output')
        body([
            message: 'pong'
        ])
    }
    metadata(
        [amqp:
         [
           outputMessage: [
               connectToBroker: [
                   declareQueueWithName: "queue"
               ],
                messageProperties: [
                    receivedRoutingKey: '#'
                ]
           ]
         ]
        ])
}
```

**YAML:**

```yaml
description: 'Send a pong message in response to a ping message'
label: 'ping_pong'
input:
    triggeredBy: 'triggerMessage("ping_pong")'
outputMessage:
    sentTo: 'output'
    body:
        message: 'pong'
metadata:
    amqp:
        outputMessage:
            connectToBroker:
                declareQueueWithName: "queue"
            messageProperties:
                receivedRoutingKey: '#'
```

#### HTTP Endpoint to Trigger a Message

Stubborn Contract would have to generate code in various languages to make it possible to trigger production code that sends a message to a broker. Instead, you provide an HTTP endpoint that the user prepares in the language of their choosing.

The endpoint must have the following configuration:

- URL: `/springcloudcontract/{label}` where `label` can be any text
- Method: `POST`
- Based on the `label`, it generates a message that will be sent to a given destination according to the contract definition

Below you have an example of such an endpoint in Python:

```python
#!/usr/bin/env python

from flask import Flask
from flask import jsonify
import pika
import os

app = Flask(__name__)

# Production code that sends a message to RabbitMQ
def send_message(cmd):
    connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
    channel = connection.channel()
    channel.basic_publish(
        exchange='output',
        routing_key='#',
        body=cmd,
        properties=pika.BasicProperties(
            delivery_mode=2,  # make message persistent
        ))
    connection.close()
    return " [x] Sent via Rabbit: %s" % cmd

# This should be ran in tests (shouldn't be publicly available)
if 'CONTRACT_TEST' in os.environ:
    @app.route('/springcloudcontract/<label>', methods=['POST'])
    def springcloudcontract(label):
        if label == "ping_pong":
            return send_message('{"message":"pong"}')
        else:
            raise ValueError('No such label expected.')
```

#### Running Message Tests on the Producer Side

```bash
#!/bin/bash
set -x

CURRENT_DIR="$( pwd )"

export SC_CONTRACT_DOCKER_VERSION="${SC_CONTRACT_DOCKER_VERSION:-4.0.1-SNAPSHOT}"
export APP_IP="$( ./whats_my_ip.sh )"
export APP_PORT="${APP_PORT:-8000}"
export APPLICATION_BASE_URL="http://${APP_IP}:${APP_PORT}"
export PROJECT_GROUP="${PROJECT_GROUP:-group}"
export PROJECT_NAME="${PROJECT_NAME:-application}"
export PROJECT_VERSION="${PROJECT_VERSION:-0.0.1-SNAPSHOT}"
# In our Python app we want to enable the HTTP endpoint
export CONTRACT_TEST="true"
# In the Verifier docker container we want to add support for RabbitMQ
export MESSAGING_TYPE="rabbit"

# Let's start the infrastructure (e.g. via Docker Compose)
yes | docker-compose kill || echo "Nothing running"
docker-compose up -d

# Let's run python app
gunicorn -w 4 --bind 0.0.0.0 main:app &
APP_PID=$!

# Generate and run tests
docker run  --rm \
    --name verifier \
    -e "SPRING_RABBITMQ_ADDRESSES=${APP_IP}:5672" \
    -e "MESSAGING_TYPE=${MESSAGING_TYPE}" \
    -e "PUBLISH_STUBS_TO_SCM=false" \
    -e "PUBLISH_ARTIFACTS=false" \
    -e "APPLICATION_BASE_URL=${APPLICATION_BASE_URL}" \
    -e "PROJECT_NAME=${PROJECT_NAME}" \
    -e "PROJECT_GROUP=${PROJECT_GROUP}" \
    -e "PROJECT_VERSION=${PROJECT_VERSION}" \
    -e "EXTERNAL_CONTRACTS_REPO_WITH_BINARIES_URL=git://https://github.com/marcingrzejszczak/cdct_python_contracts.git" \
    -e "EXTERNAL_CONTRACTS_ARTIFACT_ID=${PROJECT_NAME}" \
    -e "EXTERNAL_CONTRACTS_GROUP_ID=${PROJECT_GROUP}" \
    -e "EXTERNAL_CONTRACTS_VERSION=${PROJECT_VERSION}" \
    -v "${CURRENT_DIR}/build/spring-cloud-contract/output:/spring-cloud-contract-output/" \
    springcloud/spring-cloud-contract:"${SC_CONTRACT_DOCKER_VERSION}"

kill $APP_PID

yes | docker-compose kill
```

## Running Stubs on the Consumer Side

We publish a `spring-cloud/stubborn-stub-runner` Docker image that starts the standalone version of Stub Runner.

### Security

Since the Stubborn Contract Stub Runner Docker Image uses the standalone version of Stub Runner, the same security considerations need to be taken. You can read more about those in the [Stub Runner Boot Security section](./stub-runner-spring-boot#stub-runner-boot-security).

### Environment Variables

You can run the docker image and pass any of the common properties for JUnit and Spring as environment variables. The convention is that all the letters should be upper case. The dot (`.`) should be replaced with underscore (`_`) characters. For example, the `spring.cloud.contract.stubrunner.repositoryRoot` property should be represented as a `SPRING_CLOUD_CONTRACT_STUBRUNNER_REPOSITORY_ROOT` environment variable.

In addition to those variables you can set the following ones:

- `MESSAGING_TYPE` — what type of messaging system are you using (currently supported are `rabbit`, `kafka`)
- `ADDITIONAL_OPTS` — any additional properties that you would like to pass to the application

### Example of Usage

We want to use the stubs created in the Docker producer step. Assume that we want to run the stubs on port `9876`:

```bash
# Provide the Stubborn Contract Docker version
$ SC_CONTRACT_DOCKER_VERSION="..."
# The IP at which the app is running and Docker container can reach it
$ APP_IP="192.168.0.100"
# Stubborn Contract Stub Runner properties
$ SPRING_CLOUD_CONTRACT_STUBRUNNER_PORT="8083"
# Stub coordinates 'groupId:artifactId:version:classifier:port'
$ SPRING_CLOUD_CONTRACT_STUBRUNNER_IDS="com.example:bookstore:0.0.1.RELEASE:stubs:9876"
$ SPRING_CLOUD_CONTRACT_STUBRUNNER_REPOSITORY_ROOT="http://${APP_IP}:8081/artifactory/libs-release-local"
# Run the docker with Stub Runner Boot
$ docker run  --rm \
    -e "SPRING_CLOUD_CONTRACT_STUBRUNNER_IDS=${STUBRUNNER_IDS}" \
    -e "SPRING_CLOUD_CONTRACT_STUBRUNNER_REPOSITORY_ROOT=${STUBRUNNER_REPOSITORY_ROOT}" \
    -e "SPRING_CLOUD_CONTRACT_STUBRUNNER_STUBS_MODE=REMOTE" \
    -p "${STUBRUNNER_PORT}:${STUBRUNNER_PORT}" \
    -p "9876:9876" \
    springcloud/stubborn-stub-runner:"${SC_CONTRACT_DOCKER_VERSION}"
```

When the preceding commands run:

- A standalone Stub Runner application gets started.
- It downloads the stub with coordinates `com.example:bookstore:0.0.1.RELEASE:stubs` on port `9876`.
- It gets downloaded from Artifactory running at `http://192.168.0.100:8081/artifactory/libs-release-local`.
- After a while, Stub Runner is running on port `8083`.
- The stubs are running at port `9876`.

On the server side, we built a stateful stub. We can use curl to assert that the stubs are setup properly:

```bash
# let's run the first request (no response is returned)
$ curl -H "Content-Type:application/json" -X POST --data '{ "title" : "Title", "genre" : "Genre", "description" : "Description", "author" : "Author", "publisher" : "Publisher", "pages" : 100, "image_url" : "https://d213dhlpdb53mu.cloudfront.net/assets/...", "buy_url" : "https://pivotal.io" }' http://localhost:9876/api/books
# Now time for the second request
$ curl -X GET http://localhost:9876/api/books
# You will receive contents of the JSON
```

::: danger
If you want to use the stubs that you have built locally, on your host, you should set the `-e STUBRUNNER_STUBS_MODE=LOCAL` environment variable and mount the volume of your local m2 (`-v "${HOME}/.m2/:/home/scc/.m2:rw"`).
:::

### Example of Usage with Messaging

In order to make messaging work it's enough to pass the `MESSAGING_TYPE` environment variable with `kafka` or `rabbit` values. This will lead to setting up the Stub Runner Boot Docker image with dependencies required to connect to the broker.

In order to set the connection properties you can check out Spring Cloud Stream properties page to set proper environment variables.

- [Spring Boot Integration properties](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#integration-properties) — search for `spring.rabbitmq.xxx` or `spring.kafka.xxx` properties
- [Stream specific RabbitMQ properties](https://docs.spring.io/spring-cloud-stream-binder-rabbit/docs/3.1.0.M1/reference/html/index.html#_configuration_options)
- [Stream specific Kafka properties](https://docs.spring.io/spring-cloud-stream-binder-kafka/docs/3.1.0.M1/reference/html/index.html#_configuration_options)

The most common property you would set is the location of the running middleware. If a property to set it is called `spring.rabbitmq.addresses` or `spring.kafka.bootstrap-servers` then you should name the environment variable `SPRING_RABBITMQ_ADDRESSES` and `SPRING_KAFKA_BOOTSTRAP_SERVERS` respectively.

## Running Contract Tests against Existing Middleware

There is a legitimate reason to run your contract tests against existing middleware. Some testing frameworks might give you false positive results — the test within your build passes whereas on production the communication fails.

In Stubborn Contract docker images we give an option to connect to existing middleware. As presented in previous subsections we do support Kafka and RabbitMQ out of the box. However, via [Apache Camel Components](https://camel.apache.org/components/latest/index.html) we can support other middleware too.

### Stubborn Contract Docker and Running Middleware

In order to connect to arbitrary middleware, we leverage the `standalone` metadata entry in the contract section:

```yaml
description: 'Send a pong message in response to a ping message'
label: 'standalone_ping_pong' # (1)
input:
  triggeredBy: 'triggerMessage("ping_pong")' # (2)
outputMessage:
  sentTo: 'rabbitmq:output' # (3)
  body: # (4)
    message: 'pong'
metadata:
  standalone: # (5)
    setup: # (6)
      options: rabbitmq:output?queue=output&routingKey=# # (7)
    outputMessage: # (8)
      additionalOptions: routingKey=#&queue=output # (9)
```

// (1) Label by which we'll be able to trigger the message via Stub Runner
// (2) As in the previous messaging examples we'll need to trigger the HTTP endpoint in the running application
// (3) `protocol:destination` as requested by Apache Camel
// (4) Output message body
// (5) Standalone metadata entry
// (6) Setup part will contain information about how to prepare for running contract tests
// (7) Apache Camel URI to be called in the setup phase
// (8) Additional options to be appended to the `protocol:destination`

```bash
#!/bin/bash
set -x

# Setup
# Run the middleware
docker-compose up -d rabbitmq # (1)

# Run the python application
gunicorn -w 4 --bind 0.0.0.0 main:app & # (2)
APP_PID=$!

docker run  --rm \
    --name verifier \
    -e "STANDALONE_PROTOCOL=rabbitmq" \ # (3)
    -e "CAMEL_COMPONENT_RABBITMQ_ADDRESSES=172.18.0.1:5672" \ # (4)
    -e "PUBLISH_STUBS_TO_SCM=false" \
    -e "PUBLISH_ARTIFACTS=false" \
    -e "APPLICATION_BASE_URL=172.18.0.1" \
    -e "PROJECT_NAME=application" \
    -e "PROJECT_GROUP=group" \
    -e "EXTERNAL_CONTRACTS_ARTIFACT_ID=application" \
    -e "EXTERNAL_CONTRACTS_GROUP_ID=group" \
    -e "EXTERNAL_CONTRACTS_VERSION=0.0.1-SNAPSHOT" \
    -v "${CURRENT_DIR}/build/spring-cloud-contract/output:/spring-cloud-contract-output/" \
    springcloud/spring-cloud-contract:"${SC_CONTRACT_DOCKER_VERSION}"

# Teardown
kill $APP_PID
yes | docker-compose kill
```

// (1) We need to have the middleware running first
// (2) The application needs to be up and running
// (3) Via the `STANDALONE_PROTOCOL` environment variable we will fetch an Apache Camel Component
// (4) We're setting addresses via Camel's Spring Boot Starter mechanisms

### Stub Runner Docker and Running Middleware

In order to trigger a stub message against running middleware, we can run Stub Runner Docker image in the following manner:

```bash
$ docker run \
    -e "CAMEL_COMPONENT_RABBITMQ_ADDRESSES=172.18.0.1:5672" \ # (1)
    -e "SPRING_CLOUD_CONTRACT_STUBRUNNER_IDS=group:application:0.0.1-SNAPSHOT" \ # (2)
    -e "SPRING_CLOUD_CONTRACT_STUBRUNNER_REPOSITORY_ROOT=git://https://github.com/marcingrzejszczak/cdct_python_contracts.git" \ # (3)
    -e ADDITIONAL_OPTS="--thin.properties.dependencies.rabbitmq=org.apache.camel.springboot:camel-rabbitmq-starter:3.4.0" \ # (4)
    -e "SPRING_CLOUD_CONTRACT_STUBRUNNER_STUBS_MODE=REMOTE" \ # (5)
    -v "${HOME}/.m2/:/home/scc/.m2:rw" \ # (6)
    -p 8750:8750 \ # (7)
    springcloud/stubborn-stub-runner:3.0.4-SNAPSHOT # (8)
```

// (1) We're injecting the address of RabbitMQ via Apache Camel's Spring Boot Auto-Configuration
// (2) We're telling Stub Runner which stubs to download
// (3) We're providing an external location for our stubs (Git repository)
// (4) Via the `ADDITIONAL_OPTS=--thin.properties.dependencies.XXX=GROUP:ARTIFACT:VERSION` property we're telling Stub Runner which additional dependency to fetch at runtime
// (5) Since we're using Git, the remote option of fetching stubs needs to be set
// (6) So that we speed up launching of Stub Runner, we're attaching our local Maven repository `.m2` as a volume
// (7) We expose the port `8750` at which Stub Runner is running
// (8) Coordinates of the Stub Runner Docker image

After a while you'll notice the following text in your console, which means that Stub Runner is ready to accept requests:

```bash
o.a.c.impl.engine.AbstractCamelContext   : Apache Camel 3.4.3 (camel-1) started in 0.007 seconds
o.s.c.c.s.server.StubRunnerBoot          : Started StubRunnerBoot in 14.483 seconds (JVM running for 18.666)
```

To get the list of triggers you can send an HTTP GET request to `localhost:8750/triggers` endpoint. To trigger a stub message, you can send a HTTP POST request to `localhost:8750/triggers/standalone_ping_pong`.
