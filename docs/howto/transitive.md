# How Can I Work with Transitive Dependencies?

The Stubborn Contract plugins add the tasks that create the stubs jar for you. One problem that arises is that, when reusing the stubs, you can mistakenly import all of that stub's dependencies. When building a Maven artifact, even though you have a couple of different jars, all of them share one `pom.xml` file, as the following listing shows:

```
├── producer-0.0.1.BUILD-20160903.075506-1-stubs.jar
├── producer-0.0.1.BUILD-20160903.075506-1-stubs.jar.sha1
├── producer-0.0.1.BUILD-20160903.075655-2-stubs.jar
├── producer-0.0.1.BUILD-20160903.075655-2-stubs.jar.sha1
├── producer-0.0.1.BUILD-SNAPSHOT.jar
├── producer-0.0.1.BUILD-SNAPSHOT.pom
├── producer-0.0.1.BUILD-SNAPSHOT-stubs.jar
├── ...
└── ...
```

There are three possibilities of working with those dependencies so as not to have any issues with transitive dependencies:

1. [Mark all application dependencies as optional](#mark-all-application-dependencies-as-optional)
2. [Create a separate `artifactid` for the stubs](#create-a-separate-artifactid-for-the-stubs)
3. [Exclude dependencies on the consumer side](#exclude-dependencies-on-the-consumer-side)

## Mark All Application Dependencies as Optional

If, in the `producer` application, you mark all of your dependencies as optional, when you include the `producer` stubs in another application (or when that dependency gets downloaded by Stub Runner), then, since all of the dependencies are optional, they do not get downloaded.

In Maven, you can mark dependencies as optional:

```xml
<dependency>
    <groupId>some.group</groupId>
    <artifactId>some-artifact</artifactId>
    <optional>true</optional>
</dependency>
```

## Create a Separate `artifactid` for the Stubs

If you create a separate `artifactid`, you can set it up in whatever way you wish. For example, you might decide to have no dependencies at all.

You can create a dedicated Maven module (for example, `myservice-contracts`) that contains only the contract definitions and has minimal dependencies. The stubs generated from this module will have only the dependencies declared in that module's `pom.xml`.

## Exclude Dependencies on the Consumer Side

As a consumer, if you add the stub dependency to your classpath, you can explicitly exclude the unwanted dependencies.

In Maven, add exclusions to the stubs dependency:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>producer</artifactId>
    <classifier>stubs</classifier>
    <version>${version}</version>
    <scope>test</scope>
    <exclusions>
        <exclusion>
            <groupId>*</groupId>
            <artifactId>*</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

In Gradle, use the `transitive = false` flag.

Groovy DSL:

```groovy
testImplementation("com.example:producer:${version}:stubs") {
    transitive = false
}
```

Kotlin DSL:

```kotlin
testImplementation("com.example:producer:${version}:stubs") {
    isTransitive = false
}
```
