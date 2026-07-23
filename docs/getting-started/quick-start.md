# A Three-Second Tour

This very brief tour walks through using Stubborn Contract. It covers:

- [On the Producer Side](#on-the-producer-side)
- [On the Consumer Side](#on-the-consumer-side)

You can find a more detailed tour in [Developing Your First Application](./first-application).

The following diagram shows the relationship of the parts within Stubborn Contract:

```
API Producer  --> add Stubborn Contract (SC) plugin
API Producer  --> add SC Verifier dependency
API Producer  --> define contracts
API Producer  --> Build: run build
Build         --> SC Plugin: generate tests, stubs, and stubs artifact (e.g. stubs-jar)
Build         --> Stub Storage: upload contracts, stubs, and project artifact
Build         --> API Producer: Build successful
API Consumer  --> add SC Stub Runner dependency
API Consumer  --> write a SC Stub Runner based contract test
SC Stub Runner --> Stub Storage: test asks for [API Producer] stubs
Stub Storage  --> SC Stub Runner: fetch [API Producer] stubs
SC Stub Runner --> SC Stub Runner: run in-memory HTTP server stubs
API Consumer  --> SC Stub Runner: send a request to the HTTP server stub
SC Stub Runner --> API Consumer: communication is correct
```

## On the Producer Side

To start working with Stubborn Contract, you can add files with REST or messaging contracts expressed in either Groovy DSL or YAML to the contracts directory, which is set by the `contractsDslDir` property. By default, it is `$rootDir/src/test/resources/contracts`.

Then add the Stubborn Contract Verifier dependency and plugin to your build file. See the [full pom.xml example](https://github.com/stubborn-sh/stubborn-samples/tree/main/standalone/dsl/http-server/pom.xml).

The following listing shows how to add the plugin to the build/plugins section:

```xml
<plugin>
    <groupId>sh.stubborn</groupId>
    <artifactId>stubborn-contract-maven-plugin</artifactId>
    <version>${stubborn-contract.version}</version>
    <extensions>true</extensions>
</plugin>
```

Running `./mvnw clean install` automatically generates tests that verify the application's compliance with the added contracts. By default, the tests get generated under `sh.stubborn.contract.verifier.tests`.

As the implementation of the functionalities described by the contracts is not yet present, the tests fail.

To make them pass, you must add the correct implementation of either handling HTTP requests or messages. Also, you must add a base test class for auto-generated tests to the project. This class is extended by all the auto-generated tests and should contain all the setup information necessary to run them (for example, `RestAssuredMockMvc` controller setup or messaging test setup).

The following example from `pom.xml` shows how to specify the base test class:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>sh.stubborn</groupId>
            <artifactId>stubborn-contract-maven-plugin</artifactId>
            <version>${stubborn-contract.version}</version>
            <extensions>true</extensions>
            <configuration>
                <baseClassForTests>com.example.contractTest.BaseTestClass</baseClassForTests> <!-- (1) -->
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

// (1) The `baseClassForTests` element lets you specify your base test class. It must be a child of a `configuration` element within `stubborn-contract-maven-plugin`.

Once the implementation and the test base class are in place, the tests pass, and both the application and the stub artifacts are built and installed in the local Maven repository. You can now merge the changes and publish both the application and the stub artifacts in an online repository.

## On the Consumer Side

You can use Stubborn Contract Stub Runner in the integration tests to get a running WireMock instance or messaging route that simulates the actual service.

To do so, add the dependency to Stubborn Contract Stub Runner. See the [full pom.xml example](https://github.com/stubborn-sh/stubborn-samples/tree/main/standalone/dsl/http-client/pom.xml).

You can get the producer-side stubs installed in your Maven repository in either of two ways:

- By checking out the producer side repository and adding contracts and generating the stubs by running:

```bash
$ cd local-http-server-repo
$ ./mvnw clean install -DskipTests
```

::: tip
The tests are being skipped because the producer-side contract implementation is not in place yet, so the automatically generated contract tests fail.
:::

- By getting already-existing producer service stubs from a remote repository. To do so, pass the stub artifact IDs and artifact repository URL as Stubborn Contract Stub Runner properties. See the [application-test-repo.yaml example](https://github.com/stubborn-sh/stubborn-samples/tree/main/standalone/dsl/http-client/src/test/resources/application-test-repo.yaml).

Now you can annotate your test class with `@AutoConfigureStubRunner`. In the annotation, provide the `group-id` and `artifact-id` values for Stubborn Contract Stub Runner to run the collaborators' stubs for you:

```java
@SpringBootTest(webEnvironment=WebEnvironment.NONE)
@AutoConfigureStubRunner(ids = {"com.example:http-server-dsl:+:stubs:6565"},
        stubsMode = StubsMode.LOCAL)
class LoanApplicationServiceTests {
    // ...
}
```

::: tip
Use the `REMOTE` `stubsMode` when downloading stubs from an online repository and `LOCAL` for offline work.
:::

Now, in your integration test, you can receive stubbed versions of HTTP responses or messages that are expected to be emitted by the collaborator service.
