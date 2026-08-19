---
title: Announcing Stubborn Contract 0.1.0
description: The first stable release of the official continuation of Spring Cloud Contract — modern JVM stack, Spring-free messaging, and drop-in SCC compatibility.
---

# Announcing Stubborn Contract 0.1.0

**Stubborn Contract 0.1.0** is the first stable release of the official continuation of
Spring Cloud Contract — consumer-driven contract testing for the JVM, rebuilt on a modern
stack and published to Maven Central.

If your team already writes Spring Cloud Contract contracts, this is the release you can
move to: your existing contracts, WireMock stubs, and stub-runner setups keep working, and
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

Spring Cloud Contract is a great idea that had fallen behind the platform it runs on. The
5.x line is tied to older Spring, Boot, and JDK baselines, and there was **no released,
Spring-Cloud-free Java/Maven verification stack on Maven Central** — Java teams were left
choosing between snapshots, staying on old SCC, or hand-rolling a verifier.

Stubborn Contract 0.1.0 closes that gap. It is the maintained continuation — led by Spring
Cloud Contract's original creator — on a current stack, and it is designed so migrating is
a find-and-replace, not a rewrite.

## What's in 0.1.0

### End-to-end contract testing for HTTP and messaging

The core workflow is unchanged and battle-tested: the **consumer** writes a contract, the
**producer** verifies it (Stubborn generates and runs the tests), and the **consumer** then
tests against a **stub generated from that same contract**. If both sides are green, they are
genuinely compatible — no drifting hand-written mocks, no slow end-to-end suite.

### A modern, Spring-Cloud-free foundation

- **Java 17+** (built and tested against **Java 21 and 25**).
- **Spring Boot 4.1 / Spring Framework 7 / Jackson 3**.
- **No `spring-cloud-build` dependency** — the parent POM is standalone, so releases aren't
  gated on the Spring Cloud release train.
- **Core modules are Spring-free and Groovy-free** (enforced by Maven Enforcer + ArchUnit),
  so the verifier, generator, stub-runner, and assertion libraries are usable well beyond
  Spring.

### Messaging beyond Spring: Spring-free building blocks

This is the biggest step past SCC. The messaging abstractions
(`MessageVerifierSender` / `MessageVerifierReceiver`) are Spring-free, and each broker has a
plain-client building block:

| Transport | Building block | Built on |
|-----------|----------------|----------|
| Kafka | `stubborn-contract-messaging-kafka` | `kafka-clients` |
| RabbitMQ | `stubborn-contract-messaging-rabbit` | `amqp-client` |
| JMS | `stubborn-contract-messaging-jms` | `jakarta.jms` |

They carry no Spring dependency, support **text and binary payloads**, and are held to a
transport-neutral **conformance TCK** that runs every block against a **real broker**
(Testcontainers / embedded Artemis). That means the same messaging contract can drive
verification from plain JUnit, **Quarkus, Micronaut, or Helidon** — not just Spring.

### Zero-config JSON conversion for typed listeners

A triggered messaging stub reaches a real broker as JSON. Stubborn now wires the JSON
converter **out of the box** for a typed listener, so a `@KafkaListener(Order order)`,
`@RabbitListener(Order order)`, or `@JmsListener(Order order)` binds with **no**
hand-configured `MessageConverter` / `JsonDeserializer` — the same zero-config experience
SCC users had with its in-memory binder. It backs off the moment you configure your own.

### Framework integrations and tooling

- **Spring Boot** auto-configuration (`@AutoConfigureStubRunner`, `@AutoConfigureMessageVerifier`).
- **Quarkus** — a CDI-free `QuarkusTestResource` for stub running, with a messaging variant
  that publishes triggered stubs to a real broker.
- **Micronaut / Helidon** consumer stub-runner adapters.
- **Polyglot** — Docker images and an npm verifier so a Java producer and a Node.js consumer
  (or vice versa) can share one set of contracts.
- **Maven and Gradle plugins**, a **JUnit 5** stub runner, a **Kotlin DSL**, the **Stubborn
  Broker** for cross-team contract exchange, and **OpenRewrite** migration recipes.

## Improvements over Spring Cloud Contract

| Area | Spring Cloud Contract 5.x | Stubborn Contract 0.1.0 |
|------|---------------------------|-------------------------|
| JDK baseline | Older LTS | Java 17+, tested on 21 & 25 |
| Spring | Spring Boot 3.x / Framework 6 | Spring Boot 4.1 / Framework 7 / Jackson 3 |
| Release train | Coupled to `spring-cloud-build` | Standalone parent POM |
| Messaging | Spring-centric | Spring-free building blocks (Kafka/Rabbit/JMS), real-broker TCK |
| Non-Spring runtimes | — | Quarkus, Micronaut, Helidon |
| Typed listener binding | Manual converter setup | Zero-config JSON conversion out of the box |
| Test framework | Spock | JUnit 5 (+ AssertJ / Mockito) |
| Maintenance | Community best-effort | Actively maintained continuation |
| Existing SCC contracts | — | Run unchanged |

## Compatibility: SCC contracts, Stubborn contracts, and WireMock stubs all work

Migration is safe because 0.1.0 is deliberately backward-compatible:

- **Existing Spring Cloud Contract contracts run unchanged** — Groovy DSL, YAML, and Java
  DSL formats are all supported (plus a Kotlin DSL).
- **SCC-generated WireMock stubs work as-is** — the `spring-cloud-contract` custom matcher is
  registered as an alias, so stubs produced by older SCC still match at runtime.
- **Legacy `spring.cloud.contract.stubrunner.*` properties still bind**, mapped to the new
  `stubborn.contract.stubrunner.*` prefix (deprecated, logs a warning) so old configuration
  keeps working while you migrate.
- **Both content types are recognised** — the native `application/x-stubborn+yaml` and the
  SCC YAML format.

You can adopt Stubborn Contract on an existing SCC codebase incrementally, without
rewriting contracts or regenerating stubs.

## Migrating from Spring Cloud Contract

The migration is mostly a groupId change (`org.springframework.cloud` → `sh.stubborn`) and a
package rename (`org.springframework.cloud.contract` → `sh.stubborn.contract`). The fastest
path is the OpenRewrite recipe that automates the mechanical steps:

```bash
./mvnw -U org.openrewrite.maven:rewrite-maven-plugin:run \
  -Drewrite.recipeArtifactCoordinates=sh.stubborn:stubborn-contract-migration:LATEST \
  -Drewrite.activeRecipes=sh.stubborn.contract.migration.MigrateFromSpringCloudContract
```

It rewrites your dependencies and BOM, renames the Java packages (including the
`jsonassert` / `xmlassert` helpers), and converts JUnit 4 `StubRunnerRule` to the JUnit 5
`StubRunnerExtension`. The full, step-by-step guide is here:
**[Migrating from Spring Cloud Contract](/migration/from-spring-cloud-contract)**.

## Get started

- **[Quick Start (3 min)](/getting-started/quick-start)** — your first contract and stub.
- **[Installation &amp; Coordinates](/getting-started/installation)** — the BOM, starters, and plugins.
- **[Messaging Contracts](/reference/messaging-contracts)** — Kafka, RabbitMQ, JMS, and the zero-config listener story.
- **[Migration guide](/migration/from-spring-cloud-contract)** — move an SCC project across.
- **[GitHub](https://github.com/stubborn-sh/stubborn-contract)** — source, issues, and the roadmap.

Thank you to everyone who filed issues, tried the snapshots, and pushed for a stable Java
release. Stubborn Contract is just getting started — the roadmap includes AsyncAPI contract
validation, schema-driven generative testing, and semantic contract diffing. If there's a
capability you want, [open an issue](https://github.com/stubborn-sh/stubborn-contract/issues).
