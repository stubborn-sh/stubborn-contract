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
| `stubborn-jsonassert` | JSON assertion helpers |
| `stubborn-xmlassert` | XML assertion helpers |
| `specs/stubborn-contract-spec-java` | Java DSL for writing contracts |
| `specs/stubborn-contract-spec-groovy` | Groovy DSL for writing contracts |
| `stubborn-contract-verifier` | Contract verifier core — test generator |
| `stubborn-contract-stub-runner` | Stub runner core |
| `stubborn-contract-wiremock` | WireMock core (no Spring) |
| `stubborn-contract-converters` | Contract format converters (YAML/Java/Groovy) |

### Spring Framework tier (`-spring`)

| Module | Description |
|--------|-------------|
| `stubborn-contract-verifier-spring` | Messaging abstractions (ContractVerifierMessage, MessageVerifierSender/Receiver) |
| `stubborn-contract-stub-runner-boot` | Spring Framework stub runner integration |
| `stubborn-wiremock-spring` | Spring MVC/RestTemplate WireMock helpers |

### Spring Boot tier (`-spring-boot`)

| Module | Description |
|--------|-------------|
| `stubborn-contract-verifier-spring-boot` | AutoConfigureMessageVerifier, all messaging backends (Integration, JMS, Camel, Avro) |
| `stubborn-contract-stub-runner-spring-boot` | AutoConfigureStubRunner, port injection |
| `stubborn-wiremock-spring-boot` | Spring Boot WireMock autoconfigure |

### Spring Cloud tier (`-spring-cloud`)

| Module | Description |
|--------|-------------|
| `stubborn-contract-verifier-spring-cloud` | Spring Cloud Stream messaging verifier |
| `stubborn-contract-stub-runner-spring-cloud` | Eureka/Consul/Zookeeper service discovery for stub runner |

### Infrastructure

| Module | Description |
|--------|-------------|
| `stubborn-contract-dependencies` | Consumer BOM (`stubborn-contract-dependencies`) — pin once, get all |
| `stubborn-contract-starters` | `stubborn-starter-contract-verifier`, `stubborn-starter-contract-stub-runner` |
| `stubborn-build` | Parent POM (no spring-cloud-build) |
| `stubborn-migration` | OpenRewrite recipes for SCC → Stubborn migration |
| `stubborn-extras` | Kotlin DSL + Gradle plugin (designed for separate repo extraction) |
| `stubborn-messaging-kafka` | Kafka messaging support |

## Key conventions

- **Core modules MUST NOT import Spring.** Maven Enforcer (`ban-spring-in-core`) and ArchUnit (`CoreModuleArchTest`) both gate this.
- **Use SLF4J (`org.slf4j.Logger/LoggerFactory`) in core modules,** not `org.apache.commons.logging`.
- **Spring Java Format must pass** before committing. Run `./mvnw spring-javaformat:apply` after every Java edit.
- **`spring.cloud.contract.*` Boot properties are NOT renamed** — preserved for backward compatibility.
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
- Testcontainers (Kafka, Artemis) — used in messaging integration tests
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

Breaking changes to `public`/`protected` methods, constructors, or types in modules without a `-spring`, `-spring-boot`, or `-spring-cloud` suffix require an explicit `revapi` justification in the module's `pom.xml`. The `revapi-maven-plugin` runs on every PR for: `stubborn-contract-verifier`, `stubborn-contract-stub-runner`, `stubborn-contract-wiremock`, `stubborn-contract-converters`, `stubborn-jsonassert`, `stubborn-xmlassert`.
