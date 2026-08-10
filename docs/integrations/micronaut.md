# Micronaut integration

`stubborn-contract-stub-runner-micronaut` lets a [Micronaut](https://micronaut.io) consumer
application run Stubborn Contract stubs as part of its tests, so that `@Client` interfaces
and `@Value`-injected URLs talk to a locally started stub instead of a real collaborator.

## Motivation

Stubborn Contract already ships a first-class Spring Boot integration
(`@AutoConfigureStubRunner`). Micronaut users had no equivalent: they could call the
Spring-free stub-runner core by hand, but there was no idiomatic seam that started the stubs
*before* the Micronaut `ApplicationContext` came up and exposed the running ports as
configuration. This module fills that gap using the blessed Micronaut Test extension point,
`io.micronaut.test.support.TestPropertyProvider`, and adds **zero** Spring to the classpath —
it depends only on the core `stubborn-contract-stub-runner` and (optionally) Micronaut Test.

## What's integrated

| Capability | Status | How |
|------------|--------|-----|
| Consumer side — run HTTP stubs, inject their URLs/ports | ✅ Supported | `StubRunnerTest` (a `TestPropertyProvider`) or the framework-agnostic `StubRunnerSupport` helper |
| Consumer side — resolve a stub URL programmatically | ✅ Supported | `stubFinder().findStubUrl(group, artifact)` |
| Producer side — verify the Micronaut app against contracts | ⚠️ Via generated tests in `EXPLICIT` mode + RestAssured | see [Producer side](#producer-side) |
| Messaging (Kafka/AMQP/…) | ❌ Not yet | follow-up; the core messaging abstractions are Spring-free, a Micronaut binder can be added later |

### Consumer side

Two entry points are provided:

- **`StubRunnerTest`** — an abstract base class implementing `TestPropertyProvider`. Because
  `TestPropertyProvider#getProperties()` runs *before* the application context starts, the
  stub ports are known in time to be injected into `@Client` / `@Value`. Micronaut Test
  requires a per-class test instance, so the base class is annotated
  `@TestInstance(PER_CLASS)` and subclasses inherit it.
- **`StubRunnerSupport`** — a reusable, framework-agnostic helper (no Micronaut dependency)
  that starts a `BatchStubRunner`, exposes the `StubFinder`/`RunningStubs`, and renders the
  running stubs as a property map. `StubRunnerTest` is a thin wrapper over it, and it can
  back other integrations (e.g. a JUnit extension) unchanged.

The subclass supplies configuration by overriding protected methods:

| Method | Default | Purpose |
|--------|---------|---------|
| `stubIds()` | *(required)* | stub coordinates, `groupId:artifactId[:version[:classifier[:port]]]` |
| `stubsMode()` | `CLASSPATH` | `CLASSPATH`, `LOCAL`, or `REMOTE` |
| `repositoryRoot()` | `null` | stub repository root for `LOCAL`/`REMOTE` |
| `minPort()` / `maxPort()` | `10000` / `15000` | port range for bound stubs |

### Producer side

Producer-side contract *verification* does not need a Micronaut-specific module. Generate the
verification tests in `EXPLICIT` test mode and point [RestAssured](https://rest-assured.io)
at the running `@MicronautTest` server:

```java
@MicronautTest
class ProducerContractBase {

    @Inject
    EmbeddedServer server;

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = server.getPort();
    }
}
```

Configure the verifier plugin with `<testMode>EXPLICIT</testMode>` and
`<baseClassForTests>` pointing at the class above. The generated tests then issue real HTTP
calls against the embedded Micronaut server.

## Configuration keys

For every running stub the following properties are published into the Micronaut environment
(namespace matches the Spring Boot integration, `stubborn.contract.stubrunner.runningstubs`):

| Key | Example value |
|-----|---------------|
| `stubborn.contract.stubrunner.runningstubs.<artifactId>.port` | `10001` |
| `stubborn.contract.stubrunner.runningstubs.<artifactId>.url` | `http://localhost:10001` |
| `stubborn.contract.stubrunner.runningstubs.<groupId>.<artifactId>.port` | `10001` |
| `stubborn.contract.stubrunner.runningstubs.<groupId>.<artifactId>.url` | `http://localhost:10001` |

The group-qualified variants disambiguate stubs that share an `artifactId`. The `.url` keys
are directly consumable by `@Client("${...url}")` or `@Value`.

## Usage example

```java
@MicronautTest
class BeerConsumerTest extends StubRunnerTest {

    @Inject
    BeerClient beerClient; // a @Client whose base URL resolves from the property below

    @Override
    protected String[] stubIds() {
        return new String[] { "com.example:beer-api-producer" };
    }

    @Test
    void shouldTalkToTheStub() {
        assertThat(beerClient.hello()).isEqualTo("stubbed-response");
    }
}
```

```java
@Client("${stubborn.contract.stubrunner.runningstubs.beer-api-producer.url}")
interface BeerClient {

    @Get("/name")
    String hello();

}
```

Alternatively, resolve the URL programmatically without any application context:

```java
StubRunnerSupport support = new StubRunnerSupport(
        new StubRunnerOptionsBuilder()
            .withStubsMode(StubsMode.CLASSPATH)
            .withStubs("com.example:beer-api-producer")
            .build())
    .start();

URL url = support.stubFinder().findStubUrl("com.example", "beer-api-producer");
// ... make HTTP calls, then:
support.close();
```

## Limitations

- **HTTP stubs only.** Messaging contracts are not wired to a Micronaut binder yet.
- **Per-class lifecycle.** `TestPropertyProvider` mandates `@TestInstance(PER_CLASS)`; the
  base class enforces it and closes the runner in `@AfterAll`.
- **Micronaut Test is optional-scoped.** Consumers that only use `StubRunnerSupport` do not
  pull Micronaut Test transitively; a `@MicronautTest` subclass must declare the Micronaut
  Test dependency itself.
- **No auto-generated `@MicronautTest` integration test in this module.** The deterministic
  test exercises the pure adapter (start classpath stub → HTTP GET → assert body) without
  booting a context, keeping CI fast and non-flaky. A full `@MicronautTest` sample lives in
  the samples repository (follow-up).
