# Changelog

All notable changes to Stubborn Contract are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

From `0.2.0` onward, release sections are generated automatically from the commit
history with [git-cliff](https://git-cliff.org) (see `cliff.toml` and
`.github/workflows/changelog.yml`); the `0.1.0` entry below is the curated
initial-release summary.

## [Unreleased]

## [0.1.0] - 2026-08-14

Initial release of **Stubborn Contract**, the official continuation of Spring
Cloud Contract led by its original creator. Consumer-driven contract testing for
JVM projects, re-architected around a framework-agnostic core.

### Added

#### Core & architecture
- Framework-agnostic core with **zero Spring on the compile classpath**, enforced
  by both a Maven Enforcer `ban-spring-in-core` rule and ArchUnit `NoSpringArchTests`.
- Four-tier layering: core → `-spring` → `-spring-boot` → `-spring-cloud`, plus a
  standalone Quarkus tier.
- Consumer BOM (`stubborn-contract-dependencies`) and starters
  (`stubborn-contract-starter-verifier`, `stubborn-contract-starter-stub-runner`).

#### HTTP
- WireMock-based HTTP stubbing (`stubborn-contract-wiremock` core, plus `-spring`
  and `-spring-boot` helpers).
- Stub runner in core / Spring Boot / Spring Cloud tiers, with JUnit 5 support and
  service discovery (Eureka/Consul/Zookeeper) in the Spring Cloud tier.

#### Messaging
- Spring-free messaging building blocks: `stubborn-contract-messaging-kafka`,
  `-rabbit`, `-jms`, each with independent sender/receiver types and text + binary
  payload support.
- Spring-free core abstractions (`MessageVerifierSender`/`MessageVerifierReceiver`,
  `ContractMessage`) in the verifier core.
- Transport-neutral messaging conformance TCK
  (`stubborn-contract-messaging-tck`) run by each building block against a real
  broker, plus Spring messaging backends (JMS, AMQP, Camel, Spring Integration,
  Spring Cloud Stream, Avro/Kafka) in the `-spring` tier.

#### Quarkus
- CDI-free Quarkus stub-runner integration (`stubborn-contract-stub-runner-quarkus`)
  and Quarkus messaging integration against a real broker
  (`stubborn-contract-stub-runner-messaging-quarkus`).

#### Contract authoring & generation
- Contract DSLs: Java (`stubborn-contract-spec-java`), Groovy
  (`stubborn-contract-spec-groovy`), and YAML.
- Format converters (`stubborn-contract-converters`) and a test generator
  (`stubborn-contract-generator`) rendering via Handlebars, guarded by a
  golden-master snapshot suite.
- Native `application/x-stubborn+yaml` content type.

#### Tooling
- Maven and Gradle plugins.
- Docker images published to the container registry.

#### Spring Cloud Contract compatibility
- Existing SCC 5.x WireMock stubs work unmodified — the `spring-cloud-contract`
  WireMock matcher alias is registered.
- Legacy `spring.cloud.contract.stubrunner.*` properties bind via a deprecated,
  warning-logging shim (`StubRunnerPropertiesMigrator`).
- Classpath stub resolution matches SCC's published layouts
  (`contracts|mappings|META-INF/<group>/<artifact>/…`), including stubs placed
  directly under the artifact folder.
- Compatibility test suite: classpath resolution + HTTP serving + stub-per-consumer
  + legacy-property + SCC YAML parsing + SCC messaging-trigger, and two-way runtime
  interop samples (SCC→Stubborn and Stubborn→SCC, in both LOCAL and CLASSPATH modes).

#### Migration
- OpenRewrite recipe `MigrateFromSpringCloudContract` with six sub-recipes
  (Maven + Gradle coordinates, Java packages, stub-runner properties, verifier
  properties, JUnit 4 → 5), each individually tested plus an end-to-end composite
  test, and a migration guide.

### Security
- Credentials are never emitted in `toString()` (obfuscated) or logs.
- SpotBugs + FindSecBugs, Error Prone + NullAway, CodeQL, and OWASP
  dependency-check wired into CI.

[Unreleased]: https://github.com/stubborn-sh/stubborn-contract/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/stubborn-sh/stubborn-contract/releases/tag/v0.1.0
