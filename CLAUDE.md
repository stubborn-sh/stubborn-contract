# Stubborn Contract — Claude Code Project Guide

Stubborn Contract is the official continuation of Spring Cloud Contract, led by its original creator. It provides consumer-driven contract testing for JVM projects.

## Tech stack

- Java 17+ (source/target), tested against Java 21 and 25
- Spring Boot 4.1.x / Spring Framework 7.x
- Maven 3.9+ (wrapper: `./mvnw`)
- Spring Java Format (enforced via `spring-javaformat-maven-plugin`)

## Build commands

```bash
# Format before building (required by CI)
./mvnw spring-javaformat:apply

# Full build
./mvnw clean install

# Single module
./mvnw clean install -pl stubborn-contract-verifier -am

# Tests only
./mvnw test -pl <module>

# Skip tests
./mvnw clean install -DskipTests

# Run samples (in stubborn-samples repo)
cd sample-http/producer && ./mvnw clean install
cd ../consumer && ./mvnw test
```

## Module map

### Core (zero Spring — enforced by Maven Enforcer + ArchUnit)

| Module | Description |
|--------|-------------|
| `stubborn-contract-jsonassert` | JSON assertion helpers |
| `stubborn-contract-xmlassert` | XML assertion helpers |
| `specs/stubborn-contract-spec-java` | Java DSL for writing contracts |
| `specs/stubborn-contract-spec-groovy` | Groovy DSL for writing contracts — **also the home of the Groovy DSL parser** (`GroovyContractConverter`), the only place `groovy.lang.GroovyShell` runs |
| `stubborn-contract-verifier` | Contract verifier core — **Spring-free AND Groovy-free** production code; **Spring-free messaging abstractions** (`MessageVerifierSender<M>`/`MessageVerifierReceiver<M>`, `ContractMessage`, `MessagePayloads` for text/binary payloads), generic over the message type `M` |
| `stubborn-contract-generator` | Build-time test generator (Handlebars rendering, golden-master guarded) — split out of the verifier |
| `stubborn-contract-stub-runner` | Stub runner core |
| `stubborn-contract-wiremock` | WireMock core (no Spring) |
| `stubborn-contract-tools/stubborn-contract-converters` | Contract format converters (YAML/Java/Groovy) |

### Spring Framework tier (`-spring`)

| Module | Description |
|--------|-------------|
| `stubborn-contract-verifier-spring` | Spring messaging **backends** that implement the core abstractions (JMS, AMQP, Camel, Spring Integration, Spring Cloud Stream, Avro/Kafka) |
| `stubborn-contract-stub-runner-app` | Spring Framework stub runner integration |
| `stubborn-contract-wiremock-spring` | Spring MVC/RestTemplate WireMock helpers |

### Spring Boot tier (`-spring-boot`)

| Module | Description |
|--------|-------------|
| `stubborn-contract-verifier-spring-boot` | AutoConfigureMessageVerifier, all messaging backends (Integration, JMS, Camel, Avro) |
| `stubborn-contract-stub-runner-spring-boot` | AutoConfigureStubRunner, port injection |

### Spring Cloud tier (`-spring-cloud`)

| Module | Description |
|--------|-------------|
| `stubborn-contract-verifier-spring-cloud` | Spring Cloud Stream messaging verifier |
| `stubborn-contract-stub-runner-spring-cloud` | Eureka/Consul/Zookeeper service discovery for stub runner |

### Quarkus tier (framework-agnostic, no Spring)

| Module | Description |
|--------|-------------|
| `stubborn-contract-stub-runner-quarkus` | CDI-free Quarkus stub-runner integration (`StubRunnerResource`, a `QuarkusTestResourceLifecycleManager`) |
| `stubborn-contract-stub-runner-messaging-quarkus` | Quarkus stub-runner **with messaging** — publishes a triggered stub to a real broker (Kafka/RabbitMQ) via the Spring-free messaging building blocks |

### Infrastructure

| Module | Description |
|--------|-------------|
| `stubborn-contract-dependencies` | Consumer BOM (`stubborn-contract-dependencies`) — pin once, get all |
| `stubborn-contract-starters` | `stubborn-contract-starter-verifier`, `stubborn-contract-starter-stub-runner` |
| `stubborn-contract-build` | Parent POM (no spring-cloud-build) |
| `stubborn-contract-migration` | OpenRewrite recipes for SCC → Stubborn migration |
| `stubborn-contract-extras` | Kotlin DSL + Gradle plugin (designed for separate repo extraction) |
| `stubborn-contract-messaging-kafka` | Spring-free Kafka `MessageVerifier` building block (independent sender/receiver, text + binary payloads) |
| `stubborn-contract-messaging-rabbit` | Spring-free RabbitMQ (AMQP) `MessageVerifier` building block |
| `stubborn-contract-messaging-jms` | Spring-free JMS `MessageVerifier` building block |
| `stubborn-contract-messaging-tck` | Transport-neutral messaging conformance suite (text + binary parity) run by each building block against a real broker |

## Key conventions

- **Core modules MUST NOT import Spring.** Maven Enforcer (`ban-spring-in-core`) and ArchUnit (`NoSpringArchTests`, one per core module) both gate this.
- **`verifier`, `generator` and `stub-runner` production code MUST NOT depend on Groovy.** The `productionCodeHasNoGroovyDependencies` ArchUnit gate (one per module) bans `groovy..`, `org.codehaus.groovy..` and `org.apache.groovy..`. Groovy DSL parsing lives ONLY in `spec-groovy` (`GroovyContractConverter`), discovered at runtime through the `ContractConverter` `ServiceLoader` SPI — same mechanism as `KotlinContractConverter`. A module that must read `.groovy` contracts puts `spec-groovy` on its classpath: `converters` does (compile scope), which cascades transitively to `stub-runner`, its starter, and the Maven/Gradle plugins; `docker` declares it directly. `.java` contracts are still parsed by `ContractVerifierDslConverter` in the verifier.
- **Use SLF4J (`org.slf4j.Logger/LoggerFactory`) in core modules,** not `org.apache.commons.logging`.
- **Spring Java Format must pass** before committing. Run `./mvnw spring-javaformat:apply` after every Java edit.
- **Stub-runner Boot properties were renamed to `stubborn.contract.stubrunner.*`.** The legacy `spring.cloud.contract.stubrunner.*` prefix still binds via `StubRunnerPropertiesMigrator` (deprecated, logs a warning) — keep that shim working.
- **Existing SCC 5.x WireMock stubs work without modification** — `spring-cloud-contract` matcher alias is registered.

## Module boundary rules

| Module tier | Allowed deps |
|-------------|-------------|
| Core | JDK, SLF4J, Groovy (spec only), commons-text, WireMock, XmlUnit |
| `-spring` | core + spring-web / spring-messaging |
| `-spring-boot` | `-spring` + spring-boot-autoconfigure |
| `-spring-cloud` | `-spring-boot` + spring-cloud-* |

## External services (mock in tests)

- WireMock — used as a fake HTTP server in stub runner tests
- Testcontainers (Kafka, RabbitMQ, Artemis) — used in messaging integration tests; JMS conformance uses an embedded in-VM Artemis broker
- Eureka / Consul / Zookeeper — mocked in spring-cloud tier tests

## Snapshot repo (for consumers)

```xml
<repository>
    <id>central-snapshots</id>
    <url>https://central.sonatype.com/repository/maven-snapshots/</url>
    <snapshots><enabled>true</enabled></snapshots>
</repository>
```

## API compatibility

Breaking changes to `public`/`protected` methods, constructors, or types in modules without a `-spring`, `-spring-boot`, or `-spring-cloud` suffix require an explicit `revapi` justification in the module's `pom.xml`. The `revapi-maven-plugin` runs on every PR for: `stubborn-contract-verifier`, `stubborn-contract-stub-runner`, `stubborn-contract-wiremock`, `stubborn-contract-generator`, `stubborn-contract-jsonassert`, `stubborn-contract-xmlassert`.
