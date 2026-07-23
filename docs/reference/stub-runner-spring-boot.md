# Using the Stub Runner Boot Application

::: warning Standalone Fat JAR Not Available in Current Release
As of `4.1.6`, the executable (fat) JAR artifact for Stub Runner Boot is **not published to Maven Central** due to limitations in the current artifact repository release tooling. The sections below that describe the fat JAR, the standalone download, and the Spring Cloud CLI launcher are preserved for reference and will apply again if artifact publishing is restored.

**Current alternatives:**

- **Spring Boot tests (recommended):** annotate your test class with `@AutoConfigureStubRunner` — no separate process required. See [Stub Runner for JUnit 5](./stub-runner-junit5) for details.
- **Docker image (recommended for smoke/integration environments):** use the [Docker Stub Runner Boot image](./docker), which is the preferred runtime alternative.
- **Build from source:** clone [the repository](https://github.com/stubborn-sh/stubborn-contract/tree/main/stubborn-stub-runner-boot) and build the fat JAR yourself with `./mvnw package -pl stubborn-stub-runner-boot`.
:::

Stubborn Contract Stub Runner Boot is a Spring Boot application that exposes REST endpoints to trigger the messaging labels and to access WireMock servers.

## Stub Runner Boot Security

The Stub Runner Boot application is not secured by design — securing it would require adding security to all stubs even if they don't actually require it. Since this is a testing utility, the server is **not intended** to be used in production environments.

::: danger
It is expected that **only a trusted client** has access to the Stub Runner Boot server. You should not run this application as a Fat Jar or a [Docker Image](./docker) in untrusted locations.
:::

## Stub Runner Server

::: warning Fat JAR Not Currently Published
The dependency and `@EnableStubRunnerServer` approach below apply when building your own fat JAR from source. The pre-built artifact is not published in the current release. Use `@AutoConfigureStubRunner` in Spring Boot tests as the in-process alternative.
:::

To use the Stub Runner Server, add the following dependency:

```groovy
testImplementation "sh.stubborn:stubborn-starter-contract-stub-runner"
```

Then annotate a class with `@EnableStubRunnerServer`, build a fat jar, and it is ready to work.

## Stub Runner Server Fat Jar

::: warning Not Available in Current Release
The pre-built standalone JAR is not published to Maven Central as of `4.1.6`. The command below is preserved for when publishing resumes. In the meantime, build from source or use the Docker image.
:::

You can download a standalone JAR from Maven by running the following commands:

```bash
$ wget -O stub-runner.jar 'https://search.maven.org/remotecontent?filepath=sh/stubborn/stubborn-stub-runner-boot/2.0.1.RELEASE/stubborn-stub-runner-boot-2.0.1.RELEASE.jar'
$ java -jar stub-runner.jar --spring.cloud.contract.stubrunner.ids=... --spring.cloud.contract.stubrunner.repositoryRoot=...
```

## Spring Cloud CLI

::: warning Depends on the Fat JAR
The Spring Cloud CLI launcher requires the published fat JAR artifact. Until publishing resumes, the instructions below are for reference only.
:::

Starting from the `1.4.0.RELEASE` version of the [Spring Cloud CLI](https://cloud.spring.io/spring-cloud-cli) project, you can start Stub Runner Boot by running `spring cloud stubrunner`.

To pass the configuration, you can create a `spring.cloud.contract.stubrunner.yml` file in the current working directory, in a subdirectory called `config`, or in `~/.spring-cloud`. The file could resemble the following example for running stubs installed locally:

```yaml
spring.cloud.contract.stubrunner:
  stubsMode: LOCAL
  ids:
    - com.example:beer-api-producer:+:9876
```

Then you can call `spring cloud stubrunner` from your terminal window to start the Stub Runner server. It is available at port `8750`.

## Endpoints

Stub Runner Boot offers two sets of endpoints:

### HTTP

For HTTP, Stub Runner Boot makes the following endpoints available:

- `GET /stubs`: Returns a list of all running stubs in `ivy:integer` notation
- `GET /stubs/{ivy}`: Returns a port for the given `ivy` notation (when calling the endpoint `ivy` can also be `artifactId` only)

### Messaging

For Messaging, Stub Runner Boot makes the following endpoints available:

- `GET /triggers`: Returns a list of all running labels in `ivy : [label1, label2 ...]` notation
- `POST /triggers/{label}`: Runs a trigger with `label`
- `POST /triggers/{ivy}/{label}`: Runs a trigger with a `label` for the given `ivy` notation (when calling the endpoint, `ivy` can also be `artifactId` only)

## Stub Runner Boot with Service Discovery

One way to use Stub Runner Boot is to use it as a feed of stubs for "smoke tests". What does that mean? Assume that you do not want to deploy 50 microservices to a test environment in order to see whether your application works. You have already run a suite of tests during the build process, but you would also like to ensure that the packaging of your application works. You can deploy your application to an environment, start it, and run a couple of tests on it to see whether it works. We can call those tests "smoke tests", because their purpose is to check only a handful of testing scenarios.

The problem with this approach is that, if you use microservices, you most likely also use a service discovery tool. Stub Runner Boot lets you solve this issue by starting the required stubs and registering them in a service discovery tool.

Now assume that we want to start this application so that the stubs get automatically registered. We can do so by running the application with `java -jar ${SYSTEM_PROPS} stub-runner-boot-eureka-example.jar`.

That way, your deployed application can send requests to started WireMock servers through service discovery. Most likely, the basic properties could be set by default in `application.yml`, because they are not likely to change. That way, you can provide only the list of stubs to download whenever you start the Stub Runner Boot.
