# Getting Started with Stubborn Contract

Stubborn Contract brings consumer-driven contract (CDC) testing to your team. In CDC, consumers define what they expect from producer APIs — and producers prove they meet those expectations with generated tests.

## Core Concepts

| Term | Meaning |
|------|---------|
| **Contract** | A YAML or Groovy file describing one request/response interaction |
| **Producer** | The service implementing an API — runs generated contract tests |
| **Consumer** | The service calling an API — tests against a generated stub |
| **Stub** | A WireMock-based fake server generated from contracts |
| **Stub Runner** | Downloads and starts stubs in consumer tests |
| **Verifier** | The Maven/Gradle plugin that generates producer-side tests |
| **CDC** | Consumer-Driven Contracts — a testing methodology |

## Prerequisites

Before you start:
- **Java 17+** (tested against Java 21 and 25)
- **Maven 3.9+** or Gradle 8+
- **Spring Boot 4.1.x** (for Spring Boot integration)

## Learning Path

Choose your starting point:

### → [Quick Start (3 min)](./quick-start)
Zero-to-working in 3 minutes. Producer + consumer, minimal setup.

### → [First Application](./first-application)
Step-by-step walkthrough building a loan issuance service with CDC from scratch.

### → [Consumer-Driven CDC Tutorial](./cdc)
Full realistic scenario: a fraud-detection service across two teams, with broker integration.

## Migrating from Spring Cloud Contract?

Stubborn Contract is the direct successor to Spring Cloud Contract 5.x. Your existing contracts and stubs work without modification. See the [migration guide](../migration/from-spring-cloud-contract).
