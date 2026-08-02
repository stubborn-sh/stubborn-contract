# Installation & Coordinates

Everything Stubborn Contract publishes lives under the **`sh.stubborn`** group id, and every
artifact shares a single version per release. You never pin those versions by hand — you
import the BOM once and let it manage them. The examples below use
`${stubborn-contract.version}` (Maven) / `${verifierVersion}` (Gradle) as a stand-in for the
version you are targeting; set it to the current release or snapshot.

## 1. Import the BOM

Add the `stubborn-contract-dependencies` BOM to your `dependencyManagement`. From then on you
declare `sh.stubborn:*` dependencies **without a `<version>`**.

::: code-group

```xml [pom.xml]
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>sh.stubborn</groupId>
            <artifactId>stubborn-contract-dependencies</artifactId>
            <version>${stubborn-contract.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

```groovy [build.gradle]
dependencies {
    implementation platform("sh.stubborn:stubborn-contract-dependencies:${verifierVersion}")
}
```

:::

The BOM also manages compatible versions of the third-party libraries the tooling relies on
(for example `spring-cloud-stream`, `io.rest-assured:rest-assured`, and the Spring Boot 4.x
split modules `spring-boot-restclient` / `spring-boot-http-client`), so you do not declare
those versions either.

## 2. Add the snapshot repository (for `-SNAPSHOT` builds)

Snapshots are published to Maven Central snapshots. Add the repository to both `repositories`
and `pluginRepositories` (the Maven plugin is resolved from there too):

```xml
<repositories>
    <repository>
        <id>central-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <snapshots><enabled>true</enabled></snapshots>
        <releases><enabled>false</enabled></releases>
    </repository>
</repositories>
<pluginRepositories>
    <pluginRepository>
        <id>central-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <snapshots><enabled>true</enabled></snapshots>
        <releases><enabled>false</enabled></releases>
    </pluginRepository>
</pluginRepositories>
```

## 3. Pick a starter

Two starters cover the two sides of contract testing. They inherit their version from the BOM.

| Starter | Use it on the… | Pulls in |
|---------|----------------|----------|
| `sh.stubborn:stubborn-starter-contract-verifier` | **Producer** side | verifier + REST Assured + generated-test support |
| `sh.stubborn:stubborn-starter-contract-stub-runner` | **Consumer** side | stub runner + `@AutoConfigureStubRunner` |

```xml
<!-- Producer -->
<dependency>
    <groupId>sh.stubborn</groupId>
    <artifactId>stubborn-starter-contract-verifier</artifactId>
    <scope>test</scope>
</dependency>

<!-- Consumer -->
<dependency>
    <groupId>sh.stubborn</groupId>
    <artifactId>stubborn-starter-contract-stub-runner</artifactId>
    <scope>test</scope>
</dependency>
```

## 4. Add a build plugin (producer only)

The producer needs the build plugin to generate and run contract tests and to package the
stub JAR.

::: code-group

```xml [Maven]
<plugin>
    <groupId>sh.stubborn</groupId>
    <artifactId>stubborn-contract-maven-plugin</artifactId>
    <version>${stubborn-contract.version}</version>
    <extensions>true</extensions>
    <configuration>
        <baseClassForTests>com.example.BaseTestClass</baseClassForTests>
    </configuration>
</plugin>
```

```groovy [Gradle]
plugins {
    id 'sh.stubborn.contract' version "${verifierVersion}"
}
```

:::

The Maven plugin exposes the goals `generateTests`, `generateStubs`, `convert`, `run` and
`pushStubsToScm`. See the [Maven Plugin reference](../reference/maven-plugin) and the
[Gradle Plugin reference](../reference/gradle-plugin) for full configuration.

## A minimal producer + consumer example

### Producer — write a contract

`src/test/resources/contracts/beer/shouldReturnOk.yaml`:

```yaml
request:
  method: GET
  url: /beer
response:
  status: 200
  headers:
    Content-Type: application/json
  body:
    status: OK
```

Provide a base test class the generated tests extend (setup for `RestAssuredMockMvc`), then
run `./mvnw clean install`. The plugin generates a test that hits `/beer`, verifies the
producer honours the contract, and packages a `…-stubs.jar` containing the WireMock mapping.

### Consumer — run against the stub

```java
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@AutoConfigureStubRunner(
        ids = "com.example:beer-producer:+:stubs:6565",
        stubsMode = StubsMode.LOCAL)
class BeerConsumerTests {

    @Test
    void should_get_ok_from_stub() {
        // the stub runner has started a WireMock server on port 6565
        // serving the mapping generated from the producer's contract
    }
}
```

The `ids` use Ivy notation `groupId:artifactId:version:classifier:port` (`+` = latest,
`stubs` = classifier, `6565` = fixed port). Use `StubsMode.LOCAL` for offline work and
`StubsMode.REMOTE` when downloading stubs from a repository.

## Where to go next

- [Quick Start (3 min)](./quick-start) — the fastest end-to-end tour
- [First Application](./first-application) — a full step-by-step walkthrough
- [Modules & Architecture](../reference/modules) — what each artifact is and how the tiers fit together
- [Migration from Spring Cloud Contract](../migration/from-spring-cloud-contract)
