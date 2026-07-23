# Configuration Reference

## Common Application Properties

Various properties can be specified inside your `application.properties` file, inside your `application.yml` file, or as command line switches.

::: info
Property contributions can come from additional jar files on your classpath, so you should not consider this an exhaustive list. Also, you can define your own properties.
:::

## Stub Runner Properties

These are the key configuration properties for Stubborn Contract Stub Runner. Note: the `spring.cloud.contract.stubrunner.*` property names are preserved for backward compatibility.

| Property | Default | Description |
|----------|---------|-------------|
| `spring.cloud.contract.stubrunner.ids` | — | Comma-separated list of stubs to download in `groupId:artifactId:version:classifier:port` format |
| `spring.cloud.contract.stubrunner.repositoryRoot` | — | URL of the repository with stubs |
| `spring.cloud.contract.stubrunner.stubsMode` | `CLASSPATH` | Mode for downloading stubs (`CLASSPATH`, `LOCAL`, `REMOTE`) |
| `spring.cloud.contract.stubrunner.deleteStubsAfterTest` | `true` | If `true`, deletes stubs after the test run |
| `spring.cloud.contract.stubrunner.generateStubs` | `false` | If `true`, generates stubs from contracts |
| `spring.cloud.contract.stubrunner.failOnNoStubs` | `true` | If `true`, fails when no stubs are found |
| `spring.cloud.contract.stubrunner.properties` | — | Map with properties that can be passed to custom Stub Downloaders |
| `spring.cloud.contract.stubrunner.httpServerStubConfigurer` | — | Fully qualified class name of `HttpServerStubConfigurer` implementation |
| `spring.cloud.contract.stubrunner.classifier` | `stubs` | Classifier for the stubs artifact |
| `spring.cloud.contract.stubrunner.stubsPerConsumer` | `false` | If `true`, only registers stubs from the stub directory that is named the same as the consumer application |
| `spring.cloud.contract.stubrunner.consumerName` | — | The consumer name to be used when the `stubsPerConsumer` feature is used |
| `spring.cloud.contract.stubrunner.mappingsOutputFolder` | — | Path to a folder where stub mappings are dumped |
| `spring.cloud.contract.stubrunner.minPort` | `10000` | Minimum port at which the stub should start |
| `spring.cloud.contract.stubrunner.maxPort` | `15000` | Maximum port at which the stub should start |

## Verifier Properties

These are the key configuration properties for Stubborn Contract Verifier (Maven/Gradle plugin):

| Property | Default | Description |
|----------|---------|-------------|
| `testMode` | `MockMvc` | Test mode: `MockMvc`, `WebTestClient`, `JaxRsClient`, or `Explicit` |
| `testFramework` | `JUNIT` | Test framework: `JUNIT` (JUnit 4), `JUNIT5`, or `SPOCK` |
| `basePackageForTests` | `sh.stubborn.contract.verifier.tests` | Base package for all generated tests |
| `baseClassForTests` | — | Base class for all generated tests |
| `packageWithBaseClasses` | — | Package where all base classes reside |
| `baseClassMappings` | — | List of base class mappings (contract package regex → base class FQN) |
| `contractsDirectory` | `src/test/resources/contracts` | Directory that contains contracts |
| `generatedTestSourcesDir` | `$buildDir/generated-test-sources/contracts` | Directory for generated test sources |
| `generatedTestResourcesDir` | — | Directory for generated test resources |
| `stubsOutputDir` | `$buildDir/stubs` | Directory for generated WireMock stubs |
| `failOnNoContracts` | `true` | Throws an exception when no contracts are found |
| `failOnInProgress` | `true` | Fails the build if any in-progress contracts are found |
| `incrementalContractTests` | `true` | Only generate tests when contracts change |
| `incrementalContractStubs` | `true` | Only generate stubs when contracts change |
| `convertToYaml` | `false` | Converts all DSLs to YAML format |
| `assertJsonSize` | `false` | Checks the size of JSON arrays in generated tests |
| `contractsMode` | — | Mode for downloading contracts (`CLASSPATH`, `LOCAL`, `REMOTE`) |
| `contractDependency` | — | The contract dependency that contains all packaged contracts |
| `contractsPath` | — | Path to the concrete contracts in the JAR with packaged contracts |
| `contractsRepositoryUrl` | — | URL to a repository with the artifacts that have contracts |
| `contractsProperties` | — | Map of properties passed to Stubborn Contract components |

## SCM / Git Properties

These properties configure the Git-based stub downloader (used when `contractsRepositoryUrl` starts with `git://`):

| Property | Description |
|----------|-------------|
| `git.branch` | Branch to use when checking out contracts |
| `git.username` | Username for the Git repository |
| `git.password` | Password for the Git repository |
| `git.no-of-attempts` | Number of retries for Git operations |
| `git.wait-between-attempts` | Wait time (ms) between retries |

These can be set via `contractsProperties` in Maven/Gradle, or as system properties/environment variables.

## Full Configuration Properties List

For the complete list of auto-configured properties, see the auto-generated configuration properties documentation or use the Spring Boot Actuator's `/actuator/configprops` endpoint (in development only).

Common property prefixes:

- `spring.cloud.contract.stubrunner.*` — Stub Runner configuration
- `spring.cloud.contract.verifier.*` — Verifier HTTP port configuration
