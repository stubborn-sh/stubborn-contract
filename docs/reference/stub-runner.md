# Stub Runner Core

The stub runner core runs stubs for service collaborators. Treating stubs as contracts of services lets you use stub-runner as an implementation of [Consumer-driven Contracts](https://martinfowler.com/articles/consumerDrivenContracts.html).

Stub Runner lets you automatically download the stubs of the provided dependencies (or pick those from the classpath), start WireMock servers for them, and feed them with proper stub definitions. For messaging, special stub routes are defined.

## Retrieving Stubs

You can pick from the following options of acquiring stubs:

- Aether-based solution that downloads JARs with stubs from Artifactory or Nexus
- Classpath-scanning solution that searches the classpath with a pattern to retrieve stubs
- Writing your own implementation of the `sh.stubborn.contract.stubrunner.StubDownloaderBuilder` for full customization

### Downloading Stubs

You can control the downloading of stubs with the `stubsMode` switch. It picks value from the `StubRunnerProperties.StubsMode` enumeration. You can use the following options:

- `StubRunnerProperties.StubsMode.CLASSPATH` (default value): Picks stubs from the classpath
- `StubRunnerProperties.StubsMode.LOCAL`: Picks stubs from a local storage (for example, `.m2`)
- `StubRunnerProperties.StubsMode.REMOTE`: Picks stubs from a remote location

The following example picks stubs from a local location:

```java
@AutoConfigureStubRunner(
    repositoryRoot="https://foo.bar",
    ids = "com.example:beer-api-producer:+:stubs:8095",
    stubsMode = StubRunnerProperties.StubsMode.LOCAL)
```

### Classpath Scanning

If you set the `stubsMode` property to `StubRunnerProperties.StubsMode.CLASSPATH` (or set nothing since `CLASSPATH` is the default value), the classpath is scanned. Consider the following example:

```java
@AutoConfigureStubRunner(ids = {
    "com.example:beer-api-producer:+:stubs:8095",
    "com.example.foo:bar:1.0.0:superstubs:8096"
})
```

You can add the dependencies to your classpath as follows:

**Maven:**

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>beer-api-producer-restdocs</artifactId>
    <classifier>stubs</classifier>
    <version>0.0.1-SNAPSHOT</version>
    <scope>test</scope>
    <exclusions>
        <exclusion>
            <groupId>*</groupId>
            <artifactId>*</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>com.example.thing1</groupId>
    <artifactId>thing2</artifactId>
    <classifier>superstubs</classifier>
    <version>1.0.0</version>
    <scope>test</scope>
    <exclusions>
        <exclusion>
            <groupId>*</groupId>
            <artifactId>*</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

**Gradle:**

```groovy
testCompile("com.example:beer-api-producer-restdocs:0.0.1-SNAPSHOT:stubs") {
    transitive = false
}
testCompile("com.example.thing1:thing2:1.0.0:superstubs") {
    transitive = false
}
```

Then the specified locations on your classpath get scanned. For `com.example:beer-api-producer-restdocs`, the following locations are scanned:

- `/META-INF/com.example/beer-api-producer-restdocs/**/*.*`
- `/contracts/com.example/beer-api-producer-restdocs/**/*.*`
- `/mappings/com.example/beer-api-producer-restdocs/**/*.*`

::: tip
You have to explicitly provide the group and artifact IDs when you package the producer stubs.
:::

To achieve proper stub packaging, the producer would set up the contracts as follows:

```
└── src
    └── test
        └── resources
            └── contracts
                └── com.example
                    └── beer-api-producer-restdocs
                        └── nested
                            └── contract3.groovy
```

By using the Maven `assembly` plugin or the Gradle Jar task, you have to create the following structure in your stubs jar:

```
└── META-INF
    └── com.example
        └── beer-api-producer-restdocs
            └── 2.0.0
                ├── contracts
                │   └── nested
                │       └── contract2.groovy
                └── mappings
                    └── mapping.json
```

By maintaining this structure, the classpath gets scanned and you can profit from the messaging or HTTP stubs without the need to download artifacts.

### Configuring HTTP Server Stubs

Stub Runner has a notion of a `HttpServerStub` that abstracts the underlying concrete implementation of the HTTP server (for example, WireMock is one of the implementations). Sometimes, you need to perform some additional tuning of the stub servers. To do that, Stub Runner gives you the `httpServerStubConfigurer` property that is available in the annotation and the JUnit rule and is accessible through system properties, where you can provide your implementation of the `sh.stubborn.contract.stubrunner.HttpServerStubConfigurer` interface.

Stubborn Contract Stub Runner comes with an implementation that you can extend for WireMock: `sh.stubborn.contract.stubrunner.provider.wiremock.WireMockHttpServerStubConfigurer`. In the `configure` method, you can provide your own custom configuration for the given stub. The use case might be starting WireMock for the given artifact ID, on an HTTPS port.

You can then reuse it with the `@AutoConfigureStubRunner` annotation:

```groovy
@AutoConfigureStubRunner(
    ids = "com.example:beer-api-producer:+:stubs:8095",
    httpServerStubConfigurer = MyWireMockHttpServerStubConfigurer.class
)
```

Whenever an HTTPS port is found, it takes precedence over the HTTP port.

## Running Stubs

### HTTP Stubs

Stubs are defined in JSON documents, whose syntax is defined in the [WireMock documentation](http://wiremock.org/stubbing.html).

The following example defines a stub in JSON:

```json
{
    "request": {
        "method": "GET",
        "url": "/ping"
    },
    "response": {
        "status": 200,
        "body": "pong",
        "headers": {
            "Content-Type": "text/plain"
        }
    }
}
```

### Viewing Registered Mappings

Every stubbed collaborator exposes a list of defined mappings under the `__/admin/` endpoint.

You can also use the `mappingsOutputFolder` property to dump the mappings to files. For the annotation-based approach:

```java
@AutoConfigureStubRunner(
    ids="a.b.c:loanIssuance,a.b.c:fraudDetectionServer",
    mappingsOutputFolder = "target/outputmappings/")
```

For the JUnit approach:

```java
@ClassRule
@Shared
StubRunnerRule rule = new StubRunnerRule()
    .repoRoot("https://some_url")
    .downloadStub("a.b.c", "loanIssuance")
    .downloadStub("a.b.c:fraudDetectionServer")
    .withMappingsOutputFolder("target/outputmappings")
```

Then, if you check out the `target/outputmappings` folder, you would see the following structure:

```
.
├── fraudDetectionServer_13705
└── loanIssuance_12255
```

That means that there were two stubs registered. `fraudDetectionServer` was registered at port `13705` and `loanIssuance` at port `12255`. If we take a look at one of the files, we would see (for WireMock) the mappings available for the given server:

```json
[{
  "id" : "f9152eb9-bf77-4c38-8289-90be7d10d0d7",
  "request" : {
    "url" : "/name",
    "method" : "GET"
  },
  "response" : {
    "status" : 200,
    "body" : "fraudDetectionServer"
  },
  "uuid" : "f9152eb9-bf77-4c38-8289-90be7d10d0d7"
}]
```

### Messaging Stubs

Depending on the provided Stub Runner dependency and the DSL, the messaging routes are automatically set up.
