# Developing Your First Stubborn Contract-based Application

This brief tour walks through using Stubborn Contract. It covers:

- [On the Producer Side](#on-the-producer-side)
- [On the Consumer Side](#on-the-consumer-side)

You can find an even more brief tour in [A Three-Second Tour](./quick-start).

For the sake of this example, the Stub Storage is Nexus/Artifactory.

## On the Producer Side

To start working with Stubborn Contract, add the Stubborn Contract Verifier dependency and plugin to your build file. See the [full pom.xml example](https://github.com/stubborn-sh/stubborn-samples/tree/main/standalone/dsl/http-server/pom.xml).

The following listing shows how to add the plugin to the build/plugins section:

```xml
<plugin>
    <groupId>sh.stubborn</groupId>
    <artifactId>stubborn-contract-maven-plugin</artifactId>
    <version>${stubborn-contract.version}</version>
    <extensions>true</extensions>
</plugin>
```

::: tip
The easiest way to get started is to go to [the Spring Initializr](https://start.spring.io) and add "Web" and "Contract Verifier" as dependencies. Doing so pulls in the previously mentioned dependencies and everything else you need in the `pom.xml` file (except for setting the base test class, which we cover later in this section).
:::

Now you can add files with REST or messaging contracts expressed in either Groovy DSL or YAML to the contracts directory, which is set by the `contractsDslDir` property. By default, it is `$rootDir/src/test/resources/contracts`. Note that the file name does not matter. You can organize your contracts within this directory with whatever naming scheme you like.

For HTTP stubs, a contract defines what kind of response should be returned for a given request (taking into account the HTTP methods, URLs, headers, status codes, and so on). The following example shows an HTTP stub contract in both Groovy and YAML:

**Groovy:**

```groovy
package contracts

sh.stubborn.contract.spec.Contract.make {
    request {
        method 'PUT'
        url '/fraudcheck'
        body([
               "client.id": $(regex('[0-9]{10}')),
               loanAmount: 99999
        ])
        headers {
            contentType('application/json')
        }
    }
    response {
        status OK()
        body([
               fraudCheckStatus: "FRAUD",
               "rejection.reason": "Amount too high"
        ])
        headers {
            contentType('application/json')
        }
    }
}
```

**YAML:**

```yaml
request:
  method: PUT
  url: /fraudcheck
  body:
    "client.id": 1234567890
    loanAmount: 99999
  headers:
    Content-Type: application/json
  matchers:
    body:
      - path: $.['client.id']
        type: by_regex
        value: "[0-9]{10}"
response:
  status: 200
  body:
    fraudCheckStatus: "FRAUD"
    "rejection.reason": "Amount too high"
  headers:
    Content-Type: application/json;charset=UTF-8
```

For messaging contracts, you can define:

- The input and output messages (from where it was sent, the message body, and the header).
- The methods that should be called after the message is received.
- The methods that, when called, should trigger a message.

Running `./mvnw clean install` automatically generates tests that verify the application's compliance with the added contracts. By default, the generated tests are under `sh.stubborn.contract.verifier.tests`.

The generated tests may differ depending on which framework and test type you set up in your plugin. Stubborn Contract supports:

- **MockMvc** (default): the default test mode for HTTP contracts
- **JaxRsClient**: a JAX-RS client with the `JAXRS` test mode
- **WebTestClient**: recommended for reactive, WebFlux-based applications, set with the `WEBTESTCLIENT` test mode

::: info
You need only one of these test frameworks. MockMvc is the default. To use one of the other frameworks, add its library to your classpath.
:::

**MockMvc example (generated test):**

```java
@Test
public void validate_shouldMarkClientAsFraud() throws Exception {
    // given:
        MockMvcRequestSpecification request = given()
                .header("Content-Type", "application/vnd.fraud.v1+json")
                .body("{\"client.id\":\"1234567890\",\"loanAmount\":99999}");

    // when:
        ResponseOptions response = given().spec(request)
                .put("/fraudcheck");

    // then:
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.header("Content-Type")).matches("application/vnd.fraud.v1\\+json.*");
    // and:
        DocumentContext parsedJson = JsonPath.parse(response.getBody().asString());
        assertThatJson(parsedJson).field("['fraudCheckStatus']").matches("[A-Z]{5}");
        assertThatJson(parsedJson).field("['rejection.reason']").isEqualTo("Amount too high");
}
```

As the implementation of the functionalities described by the contracts is not yet present, the tests fail.

To make them pass, you must add the correct implementation of handling either HTTP requests or messages. You must also add a base test class for auto-generated tests to the project. This class is extended by all the auto-generated tests and should contain all the necessary setup (for example, `RestAssuredMockMvc` controller setup or messaging test setup).

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

The following example shows a minimal (but functional) base test class:

```java
package com.example.contractTest;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;

public class BaseTestClass {

    @BeforeEach
    public void setup() {
        RestAssuredMockMvc.standaloneSetup(new FraudController());
    }
}
```

Now we can move on to the implementation. First, we need a data class:

```java
package com.example.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoanRequest {

    @JsonProperty("client.id")
    private String clientId;

    private Long loanAmount;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Long getLoanAmount() {
        return loanAmount;
    }

    public void setLoanRequestAmount(Long loanAmount) {
        this.loanAmount = loanAmount;
    }
}
```

Because the client ID in the contract is called `client.id`, we need to use the `@JsonProperty("client.id")` parameter to map it to the `clientId` field.

Now we can move along to the controller:

```java
package com.example.docTest;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FraudController {

    @PutMapping(value = "/fraudcheck", consumes="application/json", produces="application/json")
    public String check(@RequestBody LoanRequest loanRequest) { // (1)

        if (loanRequest.getLoanAmount() > 10000) { // (2)
            return "{fraudCheckStatus: FRAUD, rejection.reason: Amount too high}"; // (3)
        } else {
            return "{fraudCheckStatus: OK, acceptance.reason: Amount OK}"; // (4)
        }
    }
}
```

// (1) We map the incoming parameters to a `LoanRequest` object.
// (2) We check the requested loan amount to see if it is too much.
// (3) If it is too much, we return the JSON that the test expects.
// (4) If we had a test to catch when the amount is allowable, we could match it to this output.

Once the implementation and the test base class are in place, the tests pass, and both the application and the stub artifacts are built and installed in the local Maven repository. Information about installing the stubs jar to the local repository appears in the logs:

```bash
[INFO] --- stubborn-contract-maven-plugin:1.0.0.BUILD-SNAPSHOT:generateStubs (default-generateStubs) @ http-server ---
[INFO] Building jar: /some/path/http-server/target/http-server-0.0.1-SNAPSHOT-stubs.jar
[INFO]
[INFO] --- maven-jar-plugin:2.6:jar (default-jar) @ http-server ---
[INFO] Building jar: /some/path/http-server/target/http-server-0.0.1-SNAPSHOT.jar
...
[INFO] Installing /some/path/http-server/target/http-server-0.0.1-SNAPSHOT-stubs.jar to /path/to/your/.m2/repository/com/example/http-server/0.0.1-SNAPSHOT/http-server-0.0.1-SNAPSHOT-stubs.jar
```

You can now merge the changes and publish both the application and the stub artifacts in an online repository.

## On the Consumer Side

You can use Stubborn Contract Stub Runner in the integration tests to get a running WireMock instance or messaging route that simulates the actual service.

To get started, add the dependency to Stubborn Contract Stub Runner. See the [full pom.xml example](https://github.com/stubborn-sh/stubborn-samples/tree/main/standalone/dsl/http-client/pom.xml).

You can get the producer-side stubs installed in your Maven repository in either of two ways:

- By checking out the producer side repository and adding contracts and generating the stubs by running:

```bash
$ cd local-http-server-repo
$ ./mvnw clean install -DskipTests
```

::: info
The tests are skipped because the producer-side contract implementation is not yet in place, so the automatically generated contract tests fail.
:::

- By getting existing producer service stubs from a remote repository. To do so, pass the stub artifact IDs and artifact repository URL as Stubborn Contract Stub Runner properties. See the [application-test-repo.yaml example](https://github.com/stubborn-sh/stubborn-samples/tree/main/standalone/dsl/http-client/src/test/resources/application-test-repo.yaml).

Now you can annotate your test class with `@AutoConfigureStubRunner`:

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

In your integration test, you can receive stubbed versions of HTTP responses or messages that are expected to be emitted by the collaborator service. You can see entries similar to the following in the build logs:

```bash
2016-07-19 14:22:25.403  INFO 41050 --- [main] o.s.c.c.stubrunner.AetherStubDownloader  : Desired version is + - will try to resolve the latest version
2016-07-19 14:22:25.438  INFO 41050 --- [main] o.s.c.c.stubrunner.AetherStubDownloader  : Resolved version is 0.0.1-SNAPSHOT
2016-07-19 14:22:25.439  INFO 41050 --- [main] o.s.c.c.stubrunner.AetherStubDownloader  : Resolving artifact com.example:http-server:jar:stubs:0.0.1-SNAPSHOT using remote repositories []
2016-07-19 14:22:25.451  INFO 41050 --- [main] o.s.c.c.stubrunner.AetherStubDownloader  : Resolved artifact com.example:http-server:jar:stubs:0.0.1-SNAPSHOT to /path/to/.m2/repository/...
2016-07-19 14:22:27.737  INFO 41050 --- [main] o.s.c.c.stubrunner.StubRunnerExecutor    : All stubs are now running RunningStubs [namesAndPorts={com.example:http-server:0.0.1-SNAPSHOT:stubs=8080}]
```
