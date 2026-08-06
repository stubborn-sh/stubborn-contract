# Maven Plugin Reference

## Adding the Maven Plugin

To add the Stubborn Contract BOM, include the following section in your `pom.xml` file:

```xml
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

The BOM manages compatible versions of `spring-cloud-stream`, `rest-assured`, and the Spring Boot 4.x split modules (`spring-boot-restclient`, `spring-boot-http-client`) so you do not need to declare those versions separately.

Next, add the Stubborn Contract Verifier Maven plugin:

```xml
<plugin>
    <groupId>sh.stubborn</groupId>
    <artifactId>stubborn-contract-maven-plugin</artifactId>
    <version>${stubborn-contract.version}</version>
    <extensions>true</extensions>
    <configuration>
        <packageWithBaseClasses>com.example.fraud</packageWithBaseClasses>
    </configuration>
</plugin>
```

Sometimes, regardless of the picked IDE, you can see that the `target/generated-test-source` folder is not visible on the IDE's classpath. To ensure that it is always there, you can add the following entry to your `pom.xml`:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>build-helper-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>add-source</id>
            <phase>generate-test-sources</phase>
            <goals>
                <goal>add-test-source</goal>
            </goals>
            <configuration>
                <sources>
                    <source>${project.build.directory}/generated-test-sources/contracts/</source>
                </sources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Maven and REST Assured

The `stubborn-contract-dependencies` BOM manages `io.rest-assured:rest-assured:6.0.0` so you do not need to specify the version in your project. Add the dependency without a `<version>`:

```xml
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>
```

## Using Snapshot Versions for Maven

To use snapshot versions, add the Maven Central snapshots repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>central-snapshots</id>
        <name>Maven Central Snapshots</name>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
        <releases>
            <enabled>false</enabled>
        </releases>
    </repository>
</repositories>
<pluginRepositories>
    <pluginRepository>
        <id>central-snapshots</id>
        <name>Maven Central Snapshots</name>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
        <releases>
            <enabled>false</enabled>
        </releases>
    </pluginRepository>
</pluginRepositories>
```

## Adding Stubs

By default, Stubborn Contract Verifier looks for stubs in the `src/test/resources/contracts` directory. The directory containing stub definitions is treated as a class name, and each stub definition is treated as a single test. We assume that it contains at least one directory to be used as the test class name. If there is more than one level of nested directories, all except the last one is used as the package name. Consider the following structure:

```groovy
src/test/resources/contracts/myservice/shouldCreateUser.groovy
src/test/resources/contracts/myservice/shouldReturnUser.groovy
```

Given that structure, Stubborn Contract Verifier creates a test class named `defaultBasePackage.MyService` with two methods:

- `shouldCreateUser()`
- `shouldReturnUser()`

## Run Plugin

The `generateTests` plugin goal is assigned to be invoked in the phase called `generate-test-sources`. If you want it to be part of your build process, you need not do anything. If you want only to generate tests, invoke the `generateTests` goal.

If you want to run stubs from Maven, call the `run` goal with the stubs to run as the `spring.cloud.contract.verifier.stubs` system property as follows:

```bash
mvn sh.stubborn:stubborn-contract-maven-plugin:run \
-Dspring.cloud.contract.verifier.stubs="com.acme:service-name"
```

## Configure Plugin

To change the default configuration, you can add a `configuration` section to the plugin definition or the `execution` definition:

```xml
<plugin>
    <groupId>sh.stubborn</groupId>
    <artifactId>stubborn-contract-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>convert</goal>
                <goal>generateStubs</goal>
                <goal>generateTests</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <basePackageForTests>org.springframework.cloud.verifier.twitter.place</basePackageForTests>
        <baseClassForTests>org.springframework.cloud.verifier.twitter.place.BaseMockMvcSpec</baseClassForTests>
    </configuration>
</plugin>
```

## Configuration Options

- `testMode`: Defines the mode for acceptance tests. By default, the mode is `MockMvc`, which is based on Spring's MockMvc. You can also change it to `WebTestClient`, `JaxRsClient`, or `Explicit` (for real HTTP calls).
- `basePackageForTests`: Specifies the base package for all generated tests. If not set, the value is picked from the package of `baseClassForTests` and from `packageWithBaseClasses`. If neither of these values are set, the value is set to `sh.stubborn.contract.verifier.tests`.
- `ruleClassForTests`: Specifies a rule that should be added to the generated test classes.
- `baseClassForTests`: Creates a base class for all generated tests. By default, if you use Spock classes, the class is `spock.lang.Specification`.
- `contractsDirectory`: Specifies a directory that contains contracts written with the Groovy DSL. The default directory is `/src/test/resources/contracts`.
- `generatedTestSourcesDir`: Specifies the test source directory where tests generated from the Groovy DSL should be placed. By default, its value is `$buildDir/generated-test-sources/contracts`.
- `generatedTestResourcesDir`: Specifies the test resource directory for resources used by the generated tests.
- `testFramework`: Specifies the target test framework to be used. Currently, Spock, JUnit 4 (`TestFramework.JUNIT`), and JUnit 5 are supported, with JUnit 4 being the default framework.
- `packageWithBaseClasses`: Defines a package where all the base classes reside. This setting takes precedence over `baseClassForTests`. The convention is such that, if you have a contract under (for example) `src/test/resources/contract/foo/bar/baz/` and set the value of the `packageWithBaseClasses` property to `com.example.base`, Stubborn Contract Verifier assumes that there is a `BarBazBase` class under the `com.example.base` package. In other words, the system takes the last two parts of the package, if they exist, and forms a class with `Base` as a suffix.
- `baseClassMappings`: Specifies a list of base class mappings that provide `contractPackageRegex` (which is checked against the package where the contract is located) and `baseClassFQN` (which maps to the fully qualified name of the base class for the matched contract). This setting takes precedence over `packageWithBaseClasses` and `baseClassForTests`.
- `contractsProperties`: A map that contains properties to be passed to Stubborn Contract components. Those properties might be used by (for example) built-in or custom Stub Downloaders.
- `failOnNoContracts`: When enabled, will throw an exception when no contracts were found. Defaults to `true`.
- `failOnInProgress`: If set to `true`, then, if any contracts that are in progress are found, they break the build. Defaults to `true`.
- `incrementalContractTests`: When enabled, tests are created only when contracts have changed since last build. Defaults to `true`.
- `incrementalContractStubs`: When enabled, stubs are created only when contracts have changed since last build. Defaults to `true`.
- `incrementalContractStubsJar`: When enabled, stubs jar is created only when stubs have changed since last build. Defaults to `true`.
- `httpPort`: HTTP port for the WireMock server that serves stubs.
- `skip`: Set this to `true` to bypass the verifier execution.
- `skipTestOnly`: Set this to `true` to bypass verifier test generation.
- `stubs`: List of stubs to be downloaded and ran in a colon separated Ivy notation.
- `minPort`: Specifies the minimal port at which the stub should start.
- `maxPort`: Specifies the maximal port at which the stub should start.
- `waitForKeyPressed`: Specifies if the plugin should wait for the user to press the key after starting the stubs.
- `stubsClassifier`: Specifies the classifier used by stubs artifacts.

If you want to download your contract definitions from a Maven repository, you can use the following options:

- `contractDependency`: The contract dependency that contains all the packaged contracts.
- `contractsPath`: The path to the concrete contracts in the JAR with packaged contracts. Defaults to `groupid/artifactid` where `groupid` is slash separated.
- `contractsMode`: Picks the mode in which stubs are found and registered.
- `deleteStubsAfterTest`: If set to `false`, do not remove any downloaded contracts from temporary directories.
- `contractsRepositoryUrl`: URL to a repository with the artifacts that have contracts.
- `contractsRepositoryUsername`: The user name to be used to connect to the repo with contracts.
- `contractsRepositoryPassword`: The password to be used to connect to the repo with contracts.
- `contractsRepositoryProxyHost`: The proxy host to be used to connect to the repo with contracts.
- `contractsRepositoryProxyPort`: The proxy port to be used to connect to the repo with contracts.

We cache only non-snapshot, explicitly provided versions (for example `+` or `1.0.0.BUILD-SNAPSHOT` do not get cached). By default, this feature is turned on.

The following list describes experimental features that you can turn on in the plugin:

- `convertToYaml`: Converts all DSLs to the declarative YAML format. This can be extremely useful when you use external libraries in your Groovy DSLs. By turning this feature on (by setting it to `true`), you need not add the library dependency on the consumer side.
- `assertJsonSize`: You can check the size of JSON arrays in the generated tests. This feature is disabled by default.

## Single Base Class for All Tests

When using Stubborn Contract Verifier in the default (MockMvc), you need to create a base specification for all generated acceptance tests. In this class, you need to point to an endpoint, which should be verified. The following example shows how to do so:

```groovy
package org.mycompany.tests

import org.mycompany.ExampleSpringController
import com.jayway.restassured.module.mockmvc.RestAssuredMockMvc
import spock.lang.Specification

class MvcSpec extends Specification {
  def setup() {
   RestAssuredMockMvc.standaloneSetup(new ExampleSpringController())
  }
}
```

If necessary, you can also set up the whole context:

```java
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = SomeConfig.class, properties="some=property")
public abstract class BaseTestClass {

    @Autowired
    WebApplicationContext context;

    @BeforeEach
    public void setup() {
        RestAssuredMockMvc.webAppContextSetup(this.context);
    }
}
```

If you use `EXPLICIT` mode, you can use a base class to initialize the whole tested app:

```java
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = SomeConfig.class, properties="some=property")
public abstract class BaseTestClass {

    @LocalServerPort
    int port;

    @BeforeEach
    public void setup() {
        RestAssured.baseURI = "http://localhost:" + this.port;
    }
}
```

If you use the `JAXRSCLIENT` mode, this base class should also contain a `protected WebTarget webTarget` field. Right now, the only way to test the JAX-RS API is to start a web server.

## Using Different Base Classes for Contracts

If your base classes differ between contracts, you can tell the Stubborn Contract plugin which class should get extended by the autogenerated tests. You have two options:

- Follow a convention by providing a value for `packageWithBaseClasses`
- Provide explicit mapping with `baseClassMappings`

### By Convention

The convention is such that if you have a contract under (for example) `src/test/resources/contract/foo/bar/baz/` and set the value of the `packageWithBaseClasses` property to `com.example.base`, then Stubborn Contract Verifier assumes that there is a `BarBazBase` class under the `com.example.base` package. In other words, the system takes the last two parts of the package, if they exist, and forms a class with a `Base` suffix. This rule takes precedence over `baseClassForTests`.

### By Mapping

You can manually map a regular expression of the contract's package to the fully qualified name of the base class for the matched contract. You have to provide a list called `baseClassMappings` that consists of `baseClassMapping` objects that each take a `contractPackageRegex` to `baseClassFQN` mapping.

Assume that you have contracts under these two locations:

- `src/test/resources/contract/com/`
- `src/test/resources/contract/foo/`

By providing the `baseClassForTests`, we have a fallback in case mapping did not succeed. (You can also provide the `packageWithBaseClasses` as a fallback.) That way, the tests generated from `src/test/resources/contract/com/` contracts extend the `com.example.ComBase`, whereas the rest of the tests extend `com.example.FooBase`.

## Invoking Generated Tests

The Stubborn Contract Maven Plugin generates verification code in a directory called `/generated-test-sources/contractVerifier` and attaches this directory to `testCompile` goal.

For Groovy Spock code, you can use the following:

```xml
<plugin>
    <groupId>org.codehaus.gmavenplus</groupId>
    <artifactId>gmavenplus-plugin</artifactId>
    <version>1.5</version>
    <executions>
        <execution>
            <goals>
                <goal>testCompile</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <testSources>
            <testSource>
                <directory>${project.basedir}/src/test/groovy</directory>
                <includes>
                    <include>**/*.groovy</include>
                </includes>
            </testSource>
            <testSource>
                <directory>${project.build.directory}/generated-test-sources/contractVerifier</directory>
                <includes>
                    <include>**/*.groovy</include>
                </includes>
            </testSource>
        </testSources>
    </configuration>
</plugin>
```

To ensure that the provider side is compliant with defined contracts, you need to invoke `mvn generateTests test`.

## Pushing Stubs to SCM

If you use the SCM (Source Control Management) repository to keep the contracts and stubs, you might want to automate the step of pushing stubs to the repository. To do that, you can add the `pushStubsToScm` goal:

```xml
<plugin>
    <groupId>sh.stubborn</groupId>
    <artifactId>stubborn-contract-maven-plugin</artifactId>
    <version>${stubborn-contract.version}</version>
    <extensions>true</extensions>
    <configuration>
        <!-- We want to pick contracts from a Git repository -->
        <contractsRepositoryUrl>git://https://github.com/spring-cloud-samples/spring-cloud-contract-nodejs-contracts-git.git</contractsRepositoryUrl>

        <!-- We reuse the contract dependency section to set up the path
        to the folder that contains the contract definitions. In our case the
        path will be /groupId/artifactId/version/contracts -->
        <contractDependency>
            <groupId>${project.groupId}</groupId>
            <artifactId>${project.artifactId}</artifactId>
            <version>${project.version}</version>
        </contractDependency>

        <!-- The contracts mode can't be classpath -->
        <contractsMode>REMOTE</contractsMode>
    </configuration>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <!-- By default we will not push the stubs back to SCM,
                you have to explicitly add it as a goal -->
                <goal>pushStubsToScm</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

You can find all possible configuration options that you can pass through the `<configuration><contractsProperties>` map, a system property, or an environment variable in the [advanced section on the SCM Stub Downloader](../howto/git-storage). For instance, you could specify a concrete branch to checkout instead of the default one by using `<git.branch>another_branch</git.branch>` inside `<contractsProperties>`.

## Maven Plugin and STS

When you use STS, you might encounter plugin execution errors. To fix this issue, provide the following section in your `pom.xml`:

```xml
<build>
    <pluginManagement>
        <plugins>
            <!--This plugin's configuration is used to store Eclipse m2e settings
                only. It has no influence on the Maven build itself. -->
            <plugin>
                <groupId>org.eclipse.m2e</groupId>
                <artifactId>lifecycle-mapping</artifactId>
                <version>1.0.0</version>
                <configuration>
                    <lifecycleMappingMetadata>
                        <pluginExecutions>
                             <pluginExecution>
                                <pluginExecutionFilter>
                                    <groupId>sh.stubborn</groupId>
                                    <artifactId>stubborn-contract-maven-plugin</artifactId>
                                    <versionRange>[1.0,)</versionRange>
                                    <goals>
                                        <goal>convert</goal>
                                    </goals>
                                </pluginExecutionFilter>
                                <action>
                                    <execute />
                                </action>
                             </pluginExecution>
                        </pluginExecutions>
                    </lifecycleMappingMetadata>
                </configuration>
            </plugin>
        </plugins>
    </pluginManagement>
</build>
```

## Maven Plugin with Spock Tests

You can select the [Spock Framework](http://spockframework.org/) for creating and running the auto-generated contract verification tests with both Maven and Gradle. However, while using Gradle is straightforward, in Maven, you need some additional setup in order to make the tests compile and execute properly.

First of all, you must use a plugin, such as the [GMavenPlus](https://github.com/groovy/GMavenPlus) plugin, to add Groovy to your project. In GMavenPlus plugin, you need to explicitly set test sources, including both the path where your base test classes are defined and the path where the generated contract tests are added.

If you uphold the Spock convention of ending the test class names with `Spec`, you also need to adjust your Maven Surefire plugin setup.
