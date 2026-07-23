# Gradle Plugin Reference

## Prerequisites

To use Stubborn Contract Verifier with WireMock, you must use either a Gradle or a Maven plugin.

::: warning
If you want to use Spock in your projects, you must separately add the `spock-core` and `spock-spring` modules. See [Spock's documentation](https://spockframework.github.io/) for more information.
:::

## Add Gradle Plugin with Dependencies

To add a Gradle plugin with dependencies, you can use code similar to the following:

**Plugin DSL (GA versions):**

```groovy
// build.gradle
plugins {
  id "groovy"
  // this will work only for GA versions of Stubborn Contract
  id "sh.stubborn.contract" version "${GAVerifierVersion}"
}

dependencyManagement {
    imports {
        mavenBom "sh.stubborn:stubborn-contract-dependencies:${GAVerifierVersion}"
    }
}

dependencies {
    testImplementation "org.apache.groovy:groovy-all:${groovyVersion}"
    // example with adding Spock core and Spock Spring
    testImplementation "org.spockframework:spock-core:${spockVersion}"
    testImplementation "org.spockframework:spock-spring:${spockVersion}"
    testImplementation 'sh.stubborn:stubborn-starter-contract-verifier'
}
```

**Plugin DSL (non-GA versions):**

```groovy
// settings.gradle
pluginManagement {
    plugins {
        id "sh.stubborn.contract" version "${verifierVersion}"
    }
    repositories {
        // to pick from local .m2
        mavenLocal()
        // for snapshots
        maven { url "https://central.sonatype.com/repository/maven-snapshots/" }
        // for GA versions
        gradlePluginPortal()
    }
}

// build.gradle
plugins {
  id "groovy"
  id "sh.stubborn.contract"
}

dependencyManagement {
    imports {
        mavenBom "sh.stubborn:stubborn-contract-dependencies:${verifierVersion}"
    }
}

dependencies {
    testImplementation "org.apache.groovy:groovy-all:${groovyVersion}"
    testImplementation "org.spockframework:spock-core:${spockVersion}"
    testImplementation "org.spockframework:spock-spring:${spockVersion}"
    testImplementation 'sh.stubborn:stubborn-starter-contract-verifier'
}
```

**Legacy Plugin Application:**

```groovy
// build.gradle
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath "org.springframework.boot:spring-boot-gradle-plugin:${springboot_version}"
        classpath "sh.stubborn:stubborn-contract-gradle-plugin:${verifier_version}"
    }
}

apply plugin: 'groovy'
apply plugin: 'sh.stubborn.contract'

dependencyManagement {
    imports {
        mavenBom "sh.stubborn:stubborn-contract-dependencies:${verifier_version}"
    }
}

dependencies {
    testImplementation "org.apache.groovy:groovy-all:${groovyVersion}"
    testImplementation "org.spockframework:spock-core:${spockVersion}"
    testImplementation "org.spockframework:spock-spring:${spockVersion}"
    testImplementation 'sh.stubborn:stubborn-starter-contract-verifier'
}
```

## Gradle and Rest Assured 2.0

By default, Rest Assured 3.x is added to the classpath. However, to use Rest Assured 2.x, you can add it instead:

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath "org.springframework.boot:spring-boot-gradle-plugin:${springboot_version}"
        classpath "sh.stubborn:stubborn-contract-gradle-plugin:${verifier_version}"
    }
}

dependencies {
    // you can exclude rest-assured from stubborn-verifier
    testCompile "com.jayway.restassured:rest-assured:2.5.0"
    testCompile "com.jayway.restassured:spring-mock-mvc:2.5.0"
}
```

That way, the plugin automatically sees that Rest Assured 2.x is present on the classpath and modifies the imports accordingly.

## Snapshot Versions for Gradle

You can add the additional snapshot repository to your `settings.gradle` to use snapshot versions, which are automatically uploaded after every successful build.

## Add Stubs

By default, Stubborn Contract Verifier looks for stubs in the `src/contractTest/resources/contracts` directory. For transitional purposes, the plugin will also look for contracts in `src/test/resources/contracts`, however, this directory is deprecated as of Stubborn Contract 3.0.0.

It should also be noted that with this new Gradle source set, you should also migrate any base classes used within your contract tests to `src/contractTest/{language}` where `{language}` should be replaced with Java or Groovy as needed for your purposes.

The directory that contains stub definitions is treated as a class name, and each stub definition is treated as a single test. Stubborn Contract Verifier assumes that it contains at least one level of directories that are to be used as the test class name. If more than one level of nested directories is present, all except the last one is used as the package name. Consider the following structure:

```groovy
src/contractTest/resources/contracts/myservice/shouldCreateUser.groovy
src/contractTest/resources/contracts/myservice/shouldReturnUser.groovy
```

Given the preceding structure, Stubborn Contract Verifier creates a test class named `defaultBasePackage.MyService` with two methods:

- `shouldCreateUser()`
- `shouldReturnUser()`

## Running the Plugin

The plugin registers itself to be invoked before a `check` task. If you want it to be part of your build process, you need do nothing more. If you want only to generate tests, invoke the `generateContractTests` task.

## Default Setup

The default Gradle Plugin setup creates the following Gradle part of the build (in pseudocode):

```groovy
contracts {
    testFramework ='JUNIT'
    testMode = 'MockMvc'
    generatedTestJavaSourcesDir = project.file("${project.buildDir}/generated-test-sources/contractTest/java")
    generatedTestGroovySourcesDir = project.file("${project.buildDir}/generated-test-sources/contractTest/groovy")
    generatedTestResourcesDir = project.file("${project.buildDir}/generated-test-resources/contracts")
    contractsDslDir = project.file("${project.projectDir}/src/contractTest/resources/contracts")
    basePackageForTests = 'sh.stubborn.contract.verifier.tests'
    stubsOutputDir = project.file("${project.buildDir}/stubs")
    sourceSet = null
}
```

## Configuring the Plugin

To change the default configuration, you can add a `contracts` snippet to your Gradle configuration:

```groovy
contracts {
    testMode = 'MockMvc'
    baseClassForTests = 'org.mycompany.tests'
    generatedTestJavaSourcesDir = project.file('src/generatedContract')
}
```

To download contracts from a remote source, you can use the following snippets as needed:

```groovy
contracts {
    // If your contracts exist in a JAR archive published to a Maven repository
    contractDependency {
        stringNotation = ''
        // OR
        groupId = ''
        artifactId = ''
        version = ''
        classifier = ''
    }

    // If your contracts exist in a Git SCM repository
    contractRepository {
        repositoryUrl = ''
        // username = ''
        // password = ''
    }

    // controls the nested location to find the contracts in either the JAR or Git SCM source
    contractsPath = ''
}
```

You can also extend what is created by the `verifierStubsJar` using Gradle's native task customization:

::: info
For the sake of the example, we desire to add a `git.properties` file to the `verifierStubsJar`.
:::

```groovy
verifierStubsJar {
    from("${buildDir}/resources/main/") {
        include("git.properties")
    }
}
```

As of 3.0.0, the default publication has been disabled. As a result, you are able to create any named jar and publish it as you would normally have done via Gradle configuration options.

## Configuration Options

- `testMode`: Defines the mode for acceptance tests. By default, the mode is MockMvc, which is based on Spring's MockMvc. It can also be changed to WebTestClient, JaxRsClient, or Explicit (for real HTTP calls).
- `imports`: Creates an array with imports that should be included in the generated tests (for example, `['org.myorg.Matchers']`). By default, it creates an empty array.
- `staticImports`: Creates an array with static imports that should be included in generated tests (for example, `['org.myorg.Matchers.*']`). By default, it creates an empty array.
- `basePackageForTests`: Specifies the base package for all generated tests. If not set, the value is picked from the package of `baseClassForTests` and from `packageWithBaseClasses`. If neither of these values are set, the value is set to `sh.stubborn.contract.verifier.tests`.
- `baseClassForTests`: Creates a base class for all generated tests. By default, if you use Spock classes, the class is `spock.lang.Specification`.
- `packageWithBaseClasses`: Defines a package where all the base classes reside. This setting takes precedence over `baseClassForTests`.
- `baseClassMappings`: Explicitly maps a contract package to a FQN of a base class. This setting takes precedence over `packageWithBaseClasses` and `baseClassForTests`.
- `ignoredFiles`: Uses an `Antmatcher` to allow defining stub files for which processing should be skipped. By default, it is an empty array.
- `contractsDslDir`: Specifies the directory that contains contracts written by using the GroovyDSL. By default, its value is `$projectDir/src/contractTest/resources/contracts`.
- `generatedTestJavaSourcesDir`: Specifies the test source directory where Java/JUnit tests generated from the Groovy DSL should be placed. By default, it's value is `$buildDir/generated-test-sources/contractTest/java`.
- `generatedTestGroovySourcesDir`: Specifies the test source directory where Groovy/Spock tests generated from the Groovy DSL should be placed. By default, it's value is `$buildDir/generated-test-sources/contractTest/groovy`.
- `generatedTestResourcesDir`: Specifies the test resource directory where resources used by the tests generated from the Groovy DSL should be placed. By default, its value is `$buildDir/generated-test-resources/contractTest`.
- `stubsOutputDir`: Specifies the directory where the generated WireMock stubs from the Groovy DSL should be placed.
- `testFramework`: Specifies the target test framework to be used. Currently, Spock, JUnit 4 (`TestFramework.JUNIT`) and JUnit 5 are supported, with JUnit 4 being the default framework.
- `contractsProperties`: A map that contains properties to be passed to Stubborn Contract components.
- `sourceSet`: Source set where the contracts are stored. If not provided will assume `contractTest`.
- `contractDependency`: Specifies the Dependency that provides `groupid:artifactid:version:classifier` coordinates.
- `contractsPath`: Specifies the path to the jar. If contract dependencies are downloaded, the path defaults to `groupid/artifactid`, where `groupid` is slash separated.
- `contractsMode`: Specifies the mode for downloading contracts (whether the JAR is available offline, remotely, and so on).
- `deleteStubsAfterTest`: If set to `false`, does not remove any downloaded contracts from temporary directories.
- `failOnNoContracts`: When enabled, will throw an exception when no contracts were found. Defaults to `true`.
- `failOnInProgress`: If set to `true`, then, if any contracts that are in progress are found, they break the build. Defaults to `true`.

There is also the `contractRepository { ... }` closure that contains the following properties:

- `repositoryUrl`: The URL to the repository with contract definitions
- `username`: The repository username
- `password`: The repository password
- `proxyPort`: The port of the proxy
- `proxyHost`: The host of the proxy
- `cacheDownloadedContracts`: If set to `true`, caches the folder where non-snapshot contract artifacts got downloaded. Defaults to `true`.

You can also turn on the following experimental features in the plugin:

- `convertToYaml`: Converts all DSLs to the declarative YAML format. This can be extremely useful when you use external libraries in your Groovy DSLs. By turning this feature on (by setting it to `true`), you need not add the library dependency on the consumer side.
- `assertJsonSize`: You can check the size of JSON arrays in the generated tests. This feature is disabled by default.

## Single Base Class for All Tests

When using Stubborn Contract Verifier in MockMvc (the default), you need to create a base specification for all generated acceptance tests. In this class, you need to point to an endpoint, which should be verified.

See the [BaseMockMvcSpec.groovy example](https://github.com/stubborn-sh/stubborn-contract/tree/main/stubborn-contract-gradle-plugin/src/test/resources/functionalTest/bootSimple/src/test/groovy/org/springframework/cloud/contract/verifier/twitter/places/BaseMockMvcSpec.groovy).

If you use `Explicit` mode, you can use a base class to initialize the whole tested application, as you might see in regular integration tests. If you use the `JAXRSCLIENT` mode, this base class should also contain a `protected WebTarget webTarget` field. Right now, the only option to test the JAX-RS API is to start a web server.

## Different Base Classes for Contracts

If your base classes differ between contracts, you can tell the Stubborn Contract plugin which class should get extended by the autogenerated tests. You have two options:

- Follow a convention by providing the `packageWithBaseClasses`
- Provide explicit mapping by using `baseClassMappings`

### By Convention

The convention is such that, if you have a contract in (for example) `src/contractTest/resources/contract/foo/bar/baz/` and set the value of the `packageWithBaseClasses` property to `com.example.base`, then Stubborn Contract Verifier assumes that there is a `BarBazBase` class under the `com.example.base` package. In other words, the system takes the last two parts of the package, if they exist, and forms a class with a `Base` suffix. This rule takes precedence over `baseClassForTests`.

### By Mapping

You can manually map a regular expression of the contract's package to the fully qualified name of the base class for the matched contract. You have to provide a list called `baseClassMappings` that consists of `baseClassMapping` objects that take a `contractPackageRegex` to `baseClassFQN` mapping.

Assume that you have contracts in the following directories:

- `src/contractTest/resources/contract/com/`
- `src/contractTest/resources/contract/foo/`

By providing `baseClassForTests`, we have a fallback in case mapping did not succeed. (You could also provide the `packageWithBaseClasses` as a fallback.) That way, the tests generated from `src/contractTest/resources/contract/com/` contracts extend the `com.example.ComBase`, whereas the rest of the tests extend `com.example.FooBase`.

## Invoking Generated Tests

To ensure that the provider side is compliant with your defined contracts, you need to run the following command:

```bash
./gradlew contractTest
```

## Publishing Stubs to Artifact Repository

If you use a binary artifact repository to keep the stubs, you will need to configure the publishing section for Gradle to include the `verifierStubsJar`:

```groovy
apply plugin: 'maven-publish'

publishing {
    publications {
        maven(MavenPublication) {
            // other configuration

            artifact verifierStubsJar
        }
    }
}
```

Since 3.0.0, the internal stubs publication has been deprecated and disabled by default. It is recommended to include the `verifierStubsJar` with one of your own publications.

## Pushing Stubs to SCM

If you use the SCM repository to keep the contracts and stubs, you might want to automate the step of pushing stubs to the repository. To do that, you can call the `pushStubsToScm` task:

```bash
$ ./gradlew pushStubsToScm
```

You can find all possible configuration options in the [Git storage how-to guide](../howto/git-storage). You can pass them either through the `contractsProperties` field (for example, `contracts { contractsProperties = [foo:"bar"] }`), through the `contractsProperties` method (for example, `contracts { contractsProperties([foo:"bar"]) }`), or through a system property or an environment variable.

## Stubborn Contract Verifier on the Consumer Side

In a consuming service, you need to configure the Stubborn Contract Verifier plugin in exactly the same way as in the case of a provider. If you do not want to use Stub Runner, you need to copy the contracts stored in `src/contractTest/resources/contracts` and generate WireMock JSON stubs by using the following command:

```bash
./gradlew generateClientStubs
```

::: info
The `stubsOutputDir` option has to be set for stub generation to work.
:::

When present, you can use JSON stubs in automated tests to consume a service:

```groovy
@ContextConfiguration(loader == SpringApplicationContextLoader, classes == Application)
class LoanApplicationServiceSpec extends Specification {

 @ClassRule
 @Shared
 WireMockClassRule wireMockRule == new WireMockClassRule()

 @Autowired
 LoanApplicationService sut

 def 'should successfully apply for loan'() {
   given:
    LoanApplication application =
            new LoanApplication(client: new Client(clientPesel: '12345678901'), amount: 123.123)
   when:
    LoanApplicationResult loanApplication == sut.loanApplication(application)
   then:
    loanApplication.loanApplicationStatus == LoanApplicationStatus.LOAN_APPLIED
    loanApplication.rejectionReason == null
 }
}
```

In the preceding example, `LoanApplication` makes a call to the `FraudDetection` service. This request is handled by a WireMock server configured with stubs that were generated by Stubborn Contract Verifier.
