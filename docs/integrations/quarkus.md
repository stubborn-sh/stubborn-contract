# Quarkus integration

`stubborn-contract-stub-runner-quarkus` lets a **Quarkus** application run Stubborn
Contract consumer stubs inside a `@QuarkusTest`, without pulling in Spring. It is a thin
adapter over the framework-agnostic stub runner core (`BatchStubRunner`) wired through
Quarkus' own `QuarkusTestResourceLifecycleManager` lifecycle.

## Motivation

The Spring Boot integration (`@AutoConfigureStubRunner`) is the canonical way to consume
stubs, but it assumes a Spring `ApplicationContext`. Quarkus users had no first-class way
to boot the same WireMock-backed stubs during their tests. Because the stub runner core is
deliberately Spring-free, the only thing missing was a lifecycle shim — this module
provides it, reusing 100% of the download/serve/close machinery.

## What's integrated

| Capability | Status | Note |
|------------|--------|------|
| Consumer stub runner (HTTP) | **YES** | Full reuse of `BatchStubRunner`; CLASSPATH / LOCAL / REMOTE modes, port publishing, `StubFinder` injection. |
| Producer HTTP verification | **partial** | Not provided by this module. Producers verify contracts with the generated tests run in `TestMode.EXPLICIT` against a running Quarkus app + RestAssured (see below). |
| Messaging (Kafka/JMS/AMQP…) | **not-yet** | The core messaging abstractions are Spring-free, but no Quarkus messaging backend is wired. Consumer HTTP stubbing only for now. |

### Producer-side HTTP verification (partial)

This module is consumer-side only. On the producer, generate contract tests as usual and
run them in **EXPLICIT** test mode against a booted Quarkus endpoint using RestAssured:

```java
@QuarkusTest
class ProducerContractVerificationTest {

    @Test
    void shouldReturnHello() {
        // Quarkus has already started the app on quarkus.http.test-port
        io.restassured.RestAssured.given()
            .when().get("/hello")
            .then().statusCode(200)
            .body(org.hamcrest.Matchers.equalTo("Hello from Stubborn Contract stub"));
    }
}
```

Point the verifier at `EXPLICIT` mode so the generated tests issue real HTTP calls to the
running Quarkus server rather than using Spring MockMvc.

## Configuration keys

Register `StubRunnerResource` with `@QuarkusTestResource` and pass init args via
`@ResourceArg`:

| Init arg | Required | Default | Meaning |
|----------|----------|---------|---------|
| `ids` | yes | — | Stub coordinates in Ivy notation `groupId:artifactId:version:classifier:port` (comma-separated for multiple; port optional). |
| `stubsMode` | no | `CLASSPATH` | `CLASSPATH`, `LOCAL` (from `~/.m2`) or `REMOTE`. |
| `repositoryRoot` | no | — | Stub repository root for `LOCAL`/`REMOTE` (Maven repo URL or file/classpath location). |
| `stubsClassifier` | no | `stubs` | Stubs artifact classifier. |
| `minPort` / `maxPort` | no | `10000` / `15000` | Port range for the stub servers. |

On start, for every running stub the resource publishes these Quarkus config properties
(both an `artifactId`-only and a `groupId.artifactId` variant):

| Property | Example value |
|----------|---------------|
| `stubborn.contract.stubrunner.runningstubs.<artifactId>.port` | `10001` |
| `stubborn.contract.stubrunner.runningstubs.<artifactId>.url` | `http://localhost:10001` |
| `stubborn.contract.stubrunner.runningstubs.<groupId>.<artifactId>.port` | `10001` |
| `stubborn.contract.stubrunner.runningstubs.<groupId>.<artifactId>.url` | `http://localhost:10001` |

## Full `@QuarkusTest` usage

Add the dependency (test scope) plus the Quarkus JUnit 5 harness:

```xml
<dependency>
    <groupId>sh.stubborn</groupId>
    <artifactId>stubborn-contract-stub-runner-quarkus</artifactId>
    <version>${stubborn-contract.version}</version>
    <scope>test</scope>
</dependency>
```

Make the producer's stubs available on the test classpath (for `CLASSPATH` mode) — e.g. a
dependency on `com.example:producer:...:stubs` — or use `LOCAL`/`REMOTE` mode to resolve
them from a Maven repository.

```java
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.quarkus.StubRunnerResource;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(value = StubRunnerResource.class, initArgs = {
    @ResourceArg(name = "ids", value = "com.example:producer:+:stubs"),
    @ResourceArg(name = "stubsMode", value = "CLASSPATH")
})
class ConsumerStubRunnerTest {

    // Injected by StubRunnerResource#inject — look up stub URLs programmatically.
    StubFinder stubFinder;

    // Or read the published base URL straight from Quarkus config.
    @ConfigProperty(name = "stubborn.contract.stubrunner.runningstubs.producer.url")
    String producerUrl;

    @Test
    void consumesTheStubbedEndpoint() throws Exception {
        java.net.URI uri = java.net.URI.create(producerUrl + "/hello");
        var client = java.net.http.HttpClient.newHttpClient();
        var response = client.send(
            java.net.http.HttpRequest.newBuilder(uri).GET().build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("Hello from Stubborn Contract stub");

        // The injected StubFinder resolves the same server:
        assertThat(stubFinder.findStubUrl("com.example", "producer")).isNotNull();
    }
}
```

## Limitations

- **Consumer HTTP only.** No messaging backend is wired yet; contract messaging (Kafka,
  JMS, AMQP) is a follow-up.
- **No Quarkus Dev Services / extension.** This is a plain test-resource, not a Quarkus
  extension — it runs in JVM test mode. It has not been validated under native-image
  test runs.
- **The `quarkus-bom` is intentionally not imported** by the module. It re-manages
  transitive versions (notably downgrading `jackson-annotations` below what the stub
  runner's embedded WireMock needs). The module pins only the single Quarkus interface it
  compiles against.
- **No starter / sample yet.** A `stubborn-contract-starter-stub-runner-quarkus` and a
  runnable sample under `stubborn-samples` are natural follow-ups.
