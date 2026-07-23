---
layout: home

hero:
  name: Stubborn Contract
  text: Consumer-Driven Contract Testing
  tagline: Brings TDD to the architectural level. The official continuation of Spring Cloud Contract.
  actions:
    - theme: brand
      text: Get Started
      link: /getting-started/
    - theme: alt
      text: Migrate from Spring Cloud Contract
      link: /migration/from-spring-cloud-contract

features:
  - title: Consumer-Driven
    details: Contracts are defined by the consumer, ensuring the producer always satisfies real-world usage.
    link: /getting-started/cdc
    linkText: How CDC works
  - title: Polyglot
    details: JVM-native (REST Assured, MockMvc, WebTestClient, Spock) and Node.js-native (@stubborn-sh/jest, stub-server). Any HTTP service can be a producer.
    link: /howto/non-jvm
    linkText: Node.js guide
  - title: Stub Runner
    details: Automatically downloads and runs stubs from Maven repositories or Git — no manual setup.
    link: /reference/stub-runner
    linkText: Stub Runner docs
---

## What is Stubborn Contract?

Stubborn Contract is a consumer-driven contract (CDC) testing library for JVM and polyglot projects. A **contract** is a YAML or Groovy file that describes one interaction between two services — what the consumer expects to send and receive.

**The producer** uses the contract to auto-generate and run tests that prove their API honours each contract. **The consumer** gets a generated stub — a WireMock-based fake HTTP server — that it can test against without running the real service.

Stubborn Contract is the official continuation of [Spring Cloud Contract](https://spring.io/projects/spring-cloud-contract) 5.x, led by its original creator. Existing SCC contracts, stubs, and Groovy DSL files work without modification.

## How It Works

1. **Write a contract** describing the expected request and response (YAML or Groovy DSL)
2. **Run the verifier** — the Maven/Gradle plugin generates and runs tests against the real producer
3. **Publish a stub JAR** containing WireMock mappings generated from the contracts
4. **Consumer downloads the stub** via the Stub Runner and tests against it without a live service
