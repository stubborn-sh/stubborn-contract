---
title: Announcing Stubborn Contract 0.1.0
description: The first stable release of the official continuation of Spring Cloud Contract, with a modern JVM stack, Spring-free messaging, and drop-in SCC compatibility.
---

# Announcing Stubborn Contract 0.1.0

**Stubborn Contract 0.1.0** is the first stable release of the official continuation of
Spring Cloud Contract. It brings consumer-driven contract testing to the JVM on a current
stack, and it is published to Maven Central.

If your team already writes Spring Cloud Contract contracts, this is the release you can
move to. Your existing contracts, WireMock stubs, and stub-runner setups keep working, and
you get a maintained, up-to-date foundation on top.

::: tip Coordinates
```xml
<dependency>
  <groupId>sh.stubborn</groupId>
  <artifactId>stubborn-contract-dependencies</artifactId>
  <version>0.1.0</version>
  <type>pom</type>
  <scope>import</scope>
</dependency>
```
Pin the BOM once and every module's version is aligned. See
[Installation &amp; Coordinates](/getting-started/installation).
:::

## Why this release matters

Spring Cloud Contract proved that consumer-driven contract testing belongs in the everyday
JVM toolbox, and it remains a solid choice. Stubborn Contract carries that work forward under
its original creator: the same ideas, the same workflow, now with the messaging layer freed
from Spring so it can run anywhere on the JVM, and a build that is no longer tied to the
Spring Cloud release train. It is designed so moving over is a find-and-replace, not a
rewrite.

## What's in 0.1.0

### End-to-end contract testing for HTTP and messaging

The core workflow is unchanged and battle-tested. The **consumer** writes a contract, the
**producer** verifies it (Stubborn generates and runs the tests), and the **consumer** then
tests against a **stub generated from that same contract**. If both sides are green, they are
genuinely compatible, with no drifting hand-written mocks and no slow end-to-end suite.

### A modern foundation

- **Java 17+**, built and tested against **Java 21 and 25**.
- **Spring Boot 4.1 / Spring Framework 7 / Jackson 3**.
- **No `spring-cloud-build` dependency**. The parent POM is standalone, so releases are not
  gated on the Spring Cloud release train.
- **Core modules are Spring-free and Groovy-free** (enforced by Maven Enforcer and ArchUnit),
  so the verifier, generator, stub-runner, and assertion libraries are usable well beyond
  Spring.

### Messaging beyond Spring: Spring-free building blocks

This is the biggest new capability. The messaging abstractions
(`MessageVerifierSender` / `MessageVerifierReceiver`) are Spring-free, and each broker has a
plain-client building block:

| Transport | Building block | Built on |
|-----------|----------------|----------|
| Kafka | `stubborn-contract-messaging-kafka` | `kafka-clients` |
| RabbitMQ | `stubborn-contract-messaging-rabbit` | `amqp-client` |
| JMS | `stubborn-contract-messaging-jms` | `jakarta.jms` |

They carry no Spring dependency, support **text and binary payloads**, and are held to a
transport-neutral **conformance TCK** that runs every block against a **real broker**
(Testcontainers, or an embedded Artemis for JMS). Because they are Spring-free, the same
messaging contract can drive verification from any JVM runtime. Spring and Quarkus are
supported out of the box, and the samples also show Micronaut and Helidon consumers built
directly on the building blocks.

### Zero-config JSON conversion for typed listeners

A triggered messaging stub reaches a real broker as JSON. Stubborn wires the JSON converter
**out of the box** for a typed listener, so a `@KafkaListener(Order order)`,
`@RabbitListener(Order order)`, or `@JmsListener(Order order)` binds with **no**
hand-configured `MessageConverter` or `JsonDeserializer`. This is the same zero-config
experience SCC users had with its in-memory binder, and it backs off the moment you configure
your own.

### Framework integrations and tooling

- **Spring Boot** auto-configuration (`@AutoConfigureStubRunner`, `@AutoConfigureMessageVerifier`).
- **Quarkus**, out of the box: a CDI-free `QuarkusTestResource` for stub running, with a
  messaging variant that publishes triggered stubs to a real broker.
- **Micronaut and Helidon**, demonstrated in the samples as consumers driven by the
  Spring-free building blocks.
- **Polyglot**: Docker images and an npm verifier, so a Java producer and a Node.js consumer
  (or the reverse) can share one set of contracts.
- **Maven and Gradle plugins**, a **JUnit 5** stub runner, a **Kotlin DSL**, the **Stubborn
  Broker** for cross-team contract exchange, and **OpenRewrite** migration recipes.

## What Stubborn Contract adds on top of Spring Cloud Contract

Everything you rely on in SCC is still here. These are the additions in 0.1.0:

- **Spring-free messaging building blocks** for Kafka, RabbitMQ, and JMS, with a real-broker
  conformance TCK, so contract-driven messaging is no longer tied to Spring.
- **Zero-config JSON conversion** for typed `@KafkaListener` / `@RabbitListener` /
  `@JmsListener` parameters, out of the box.
- **Groovy is out of the core and the tests.** The production code and the test suite no
  longer depend on Groovy (tests are JUnit 5 with AssertJ and Mockito), which makes the
  codebase easier to read, maintain, and contribute to. Groovy DSL contracts are still
  fully supported through a dedicated module.
- **A standalone parent POM** (no `spring-cloud-build`), so releases are not coupled to the
  Spring Cloud train.
- **Built and tested on current JDKs** (17, 21, and 25).
- **Your existing SCC contracts, WireMock stubs, and stub-runner configuration keep working**
  unchanged.

## Compatibility: SCC contracts, Stubborn contracts, and WireMock stubs all work

Migration is safe because 0.1.0 is deliberately backward-compatible:

- **Existing Spring Cloud Contract contracts run unchanged.** Groovy DSL, YAML, and Java DSL
  formats are all supported, plus a Kotlin DSL.
- **SCC-generated WireMock stubs work as-is.** The `spring-cloud-contract` custom matcher is
  registered as an alias, so stubs produced by older SCC still match at runtime.
- **Legacy `spring.cloud.contract.stubrunner.*` properties still bind**, mapped to the new
  `stubborn.contract.stubrunner.*` prefix (deprecated, logs a warning), so old configuration
  keeps working while you migrate.
- **Both content types are recognised**: the native `application/x-stubborn+yaml` and the
  SCC YAML format.

You can adopt Stubborn Contract on an existing SCC codebase incrementally, without rewriting
contracts or regenerating stubs.

## Migrating from Spring Cloud Contract

The migration is mostly a groupId change (`org.springframework.cloud` becomes `sh.stubborn`)
and a package rename (`org.springframework.cloud.contract` becomes `sh.stubborn.contract`).
The fastest path is the OpenRewrite recipe that automates the mechanical steps:

```bash
./mvnw -U org.openrewrite.maven:rewrite-maven-plugin:run \
  -Drewrite.recipeArtifactCoordinates=sh.stubborn:stubborn-contract-migration:LATEST \
  -Drewrite.activeRecipes=sh.stubborn.contract.migration.MigrateFromSpringCloudContract
```

It rewrites your dependencies and BOM, renames the Java packages (including the `jsonassert`
and `xmlassert` helpers), and converts JUnit 4 `StubRunnerRule` to the JUnit 5
`StubRunnerExtension`. The full, step-by-step guide is here:
**[Migrating from Spring Cloud Contract](/migration/from-spring-cloud-contract)**.

## Enterprise support

Stubborn Contract is open source and free to use. For teams that want more, commercial
support is available, and enterprises that depend on it can help shape the roadmap and
prioritise the capabilities they need. If that is you, reach out through the project website
or open an issue on [GitHub](https://github.com/stubborn-sh/stubborn-contract).

## Get started

- **[Quick Start (3 min)](/getting-started/quick-start)**: your first contract and stub.
- **[Installation &amp; Coordinates](/getting-started/installation)**: the BOM, starters, and plugins.
- **[Messaging Contracts](/reference/messaging-contracts)**: Kafka, RabbitMQ, JMS, and the zero-config listener story.
- **[Migration guide](/migration/from-spring-cloud-contract)**: move an SCC project across.
- **[GitHub](https://github.com/stubborn-sh/stubborn-contract)**: source, issues, and the roadmap.

Thank you to everyone who filed issues, tried the snapshots, and pushed for a stable Java
release. Stubborn Contract is just getting started. The roadmap includes AsyncAPI contract
validation, schema-driven generative testing, and semantic contract diffing. If there is a
capability you want, [open an issue](https://github.com/stubborn-sh/stubborn-contract/issues).
