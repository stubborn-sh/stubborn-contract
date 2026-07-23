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
