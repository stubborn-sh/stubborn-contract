# Helidon Integration

Stubborn Contract ships a consumer-side stub runner integration for
[Helidon](https://helidon.io) MicroProfile applications. It lets a Helidon MP test boot
the producer's WireMock stubs in-process and reach them from the application under test
through **MicroProfile Config**, exactly the way the Spring Boot integration wires stub
URLs into the Spring `Environment`.

The module is `sh.stubborn:stubborn-contract-stub-runner-helidon`. It has **zero Spring
dependencies** — it builds only on the Spring-free `stubborn-contract-stub-runner` core
plus the JUnit 5 extension SPI.

## Motivation

Helidon MP users writing consumer-driven contract tests want the same experience Spring
Boot users get from `@AutoConfigureStubRunner`: annotate the test, have the producer's
stubs start on a free port, and let the HTTP client in the application under test discover
that port. Helidon has no Spring `Environment`, but it does have MicroProfile Config, and
Helidon MP Config reads **System properties** by default. That is the seam this module
plugs into.

## What's integrated

| Capability | Status | How |
|------------|--------|-----|
| Consumer stub runner (Helidon MP) | ✅ Supported | `@StubRunner` + `StubRunnerExtension` publish running stub ports as MP Config |
| Stub URL discovery in test code | ✅ Supported | `StubFinder` / `StubRunning` injected as a JUnit 5 test parameter |
| Producer-side HTTP verification (Helidon MP) | ✅ Supported (pattern) | Generate tests in `EXPLICIT` mode and drive `@HelidonTest` with RestAssured (see below) |
| Producer-side HTTP verification (Helidon SE) | ⚠️ HTTP-only | RestAssured against a manually started `WebServer`; no DI scaffolding is provided |
| Messaging (Helidon Messaging / Reactive Messaging) | ❌ Not yet | Follow-up; the core `MessageVerifierSender`/`Receiver` abstractions are Spring-free and reusable |

Notes:

- **Helidon MP vs SE.** This module targets **Helidon MP** (CDI + JAX-RS, the flavor with
  `@HelidonTest`). For consumer tests the runtime flavor barely matters — stub ports are
  published as System properties and any MP Config lookup or plain
  `System.getProperty(...)` sees them, so **Helidon SE** consumers can use the same
  extension and read the port directly. What SE does *not* get is DI-based injection of
  configuration; SE is therefore "HTTP-only": start your `WebServer`, resolve the stub URL
  from the injected `StubFinder`, and call it with any HTTP client.
- **Producer side is a pattern, not code.** There is no producer autoconfiguration to
  write for Helidon — you generate the verification tests in `EXPLICIT` test mode and point
  RestAssured at the running `@HelidonTest` server. Example below.

## Configuration keys

For every running stub the extension publishes two System properties (identical to the
Spring Boot integration, so property lookups are portable):

```
stubborn.contract.stubrunner.runningstubs.<artifactId>.port
stubborn.contract.stubrunner.runningstubs.<groupId>.<artifactId>.port
```

`@StubRunner` attributes:

| Attribute | Default | Meaning |
|-----------|---------|---------|
| `ids` | `{}` | Ivy notations `group:artifact:version:classifier:port` (port optional) |
| `stubsMode` | `CLASSPATH` | `CLASSPATH`, `LOCAL`, or `REMOTE` |
| `repositoryRoot` | `""` | Stub repository root (for `LOCAL`/`REMOTE`) |
| `minPort` / `maxPort` | `10000` / `15000` | Port range when a stub declares no explicit port |

## Usage

### Consumer test (Helidon MP)

```java
@StubRunner(ids = "com.example:fraud-service:+:stubs", stubsMode = StubsMode.CLASSPATH)
@HelidonTest
class OrderResourceTest {

    // Helidon MP Config resolves this from the System property the extension set
    @Inject
    @ConfigProperty(name = "stubborn.contract.stubrunner.runningstubs.fraud-service.port")
    int fraudPort;

    @Test
    void callsFraudStub() {
        // your JAX-RS client / WebTarget pointed at http://localhost:{fraudPort}
    }
}
```

You can also inject a `StubFinder` straight into a test method and resolve URLs
programmatically — no CDI needed, which is what the module's own test does:

```java
@StubRunner(ids = "com.example:fraud-service:+:stubs")
class OrderConsumerTest {
    @Test
    void resolvesUrl(StubFinder stubs) {
        URL url = stubs.findStubUrl("com.example", "fraud-service");
        // GET url + "/frauds" ...
    }
}
```

### Producer HTTP verification (Helidon MP, EXPLICIT mode)

Generate the verifier tests in `EXPLICIT` mode (real HTTP, not MockMvc) and drive the app
with `@HelidonTest` + RestAssured:

```java
@HelidonTest
class ContractVerificationBase {
    @BeforeEach
    void setup(WebTarget target) {
        RestAssured.baseURI = target.getUri().toString();
    }
}
```

## Important caveat: extension ordering vs `@HelidonTest`

The extension sets the stub-port System properties from its **`beforeAll`** callback.
Helidon builds its `Config` snapshot and CDI container when **its own** extension starts.
JUnit 5 runs `BeforeAllCallback`s in extension-registration order, so **`@StubRunner` must
be declared before `@HelidonTest`** on the test class:

```java
@StubRunner(ids = "...")   // first — sets System properties
@HelidonTest               // second — reads Config after properties are set
class MyTest { }
```

If the order is reversed, Helidon may snapshot its Config before the ports exist and the
`@ConfigProperty` lookups will fail to resolve.

### Why the extension approach (and the ConfigSource alternative)

Two designs were considered:

1. **JUnit 5 extension (chosen).** A `@StubRunner` annotation meta-annotated with
   `@ExtendWith(StubRunnerExtension.class)`. Pros: idiomatic for tests, injects
   `StubFinder` as a parameter, deterministic lifecycle (`beforeAll`/`afterAll`), no
   ServiceLoader/global state. Con: the ordering-vs-`@HelidonTest` constraint above.
2. **MicroProfile `ConfigSource` (ServiceLoader).** A `ConfigSource` registered via
   `META-INF/services` that lazily starts the runner the first time Helidon reads config.
   Pro: no ordering constraint — Config pulls the ports on demand. Cons: a global,
   ServiceLoader-activated side effect that starts network servers as a side effect of
   *reading config*; awkward lifecycle/teardown (ConfigSources have no close hook tied to
   the test); harder to scope per-test; not idiomatic for a test-only concern.

The extension is the cleaner fit for a **test-scoped** integration, so it is the shipped
approach. The ordering constraint is a one-line documentation rule rather than a hidden
global. If a future need arises to remove the ordering constraint (e.g. non-test usage),
the `ConfigSource` variant can be added alongside it.

## Limitations

- MP-first: SE consumers work but get HTTP-only usage (no DI injection helper).
- Producer messaging verification for Helidon is not yet provided.
- The `@StubRunner`/`@HelidonTest` ordering rule is a documented convention, not enforced
  by code.
- No dedicated Helidon SE `WebServer` bootstrap helper is shipped yet (follow-up).
