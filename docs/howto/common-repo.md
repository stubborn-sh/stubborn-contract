# How Can I Use a Common Repository with Contracts Instead of Storing with the Producer?

Instead of storing contracts with each producer, you can keep them in a common place. If you cannot clone and keep contracts in a single place, then you, as a producer, know how many consumers you have and which consumers you may break with your local changes.

Assume we have a producer with coordinates of `com.example:server` and three consumers: `client1`, `client2`, and `client3`. Then, in the repository with common contracts, you could have a setup that contains contracts for all three consumers.

## Repository Structure

See the [Stubborn Contract Samples repository](https://github.com/stubborn-sh/stubborn-samples/tree/main/standalone/dsl/contracts/) for an example of how to set up the repository.

The repository uses the assembly plugin to build the JAR with all the contracts.

See the [contracts.xml assembly descriptor example](https://github.com/stubborn-sh/stubborn-samples/tree/main/standalone/dsl/contracts/src/assembly/contracts.xml).

Stubborn Contract is set up on the consumer and on the producer side with the proper plugin setup in the common repository with contracts. CI jobs build an artifact of all contracts and upload it to Nexus or Artifactory.

## Maven Setup

### Producer Side (Maven)

The producer must set up the contracts path to point to the common repository. The following example shows how to configure the Maven plugin to fetch contracts from a common repository JAR:

```xml
<plugin>
    <groupId>sh.stubborn</groupId>
    <artifactId>stubborn-contract-maven-plugin</artifactId>
    <version>${stubborn-contract.version}</version>
    <extensions>true</extensions>
    <configuration>
        <contractDependency>
            <groupId>com.example.standalone</groupId>
            <artifactId>http-server-contracts</artifactId>
        </contractDependency>
        <contractsPath>/</contractsPath>
        <baseClassForTests>com.example.fraud.FraudBase</baseClassForTests>
    </configuration>
</plugin>
```

### Consumer Side (Maven)

The consumer adds the stubs as a dependency (from the producer's build, which the contracts repository triggers):

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>http-server</artifactId>
    <classifier>stubs</classifier>
    <version>${version}</version>
    <scope>test</scope>
</dependency>
```

## Defining Messaging Contracts Per Topic

When you work with messaging, you might have contracts related to a specific topic. You can use inclusion patterns for filtering common repository jar files by messaging topics.

### For Maven Projects

The `includedFiles` Maven Stubborn Contract plugin property lets you filter which contract files to include. The `contractsPath` also needs to be specified; the default path would be the common repository `groupid/artifactid`.

```xml
<plugin>
    <groupId>sh.stubborn</groupId>
    <artifactId>stubborn-contract-maven-plugin</artifactId>
    <extensions>true</extensions>
    <configuration>
        <contractDependency>
            <groupId>com.example.standalone</groupId>
            <artifactId>contracts</artifactId>
        </contractDependency>
        <contractsPath>/</contractsPath>
        <httpPort>8082</httpPort>
        <baseClassForTests>com.example.fraud.FraudBase</baseClassForTests>
        <includedFiles>
            <includedFile>**server**</includedFile>
        </includedFiles>
    </configuration>
</plugin>
```

### For Gradle Projects

In Gradle, you can set up the contracts dependency and filter the files using the configuration DSL:

```groovy
contracts {
    contractDependency {
        stringNotation = "${contractsGroupId}:${contractsArtifactId}:${contractsVersion}"
    }
    contractsPath = "/"
    packageWithBaseClasses = 'com.example.contractTest'
    baseClassMappings {
        baseClassMapping('.*messaging.*', 'com.example.MessagingBase')
        baseClassMapping('.*rest.*', 'com.example.RestBase')
    }
}
```

Alternatively, you can manually download and unzip the contracts JAR:

```groovy
// 1. Define the contracts configuration
configurations {
    contracts
}

dependencies {
    contracts "${contractsGroupId}:${contractsArtifactId}:${contractsVersion}"
}

// 2. Get the contracts
def downloadedContractsDir = new File(project.buildDir, "downloadedContracts")

task getContracts(type: Copy) {
    from configurations.contracts
    into downloadedContractsDir
}

// 3. Unzip the contracts
task unzipContracts(type: Copy, dependsOn: getContracts) {
    // Unzip each jar
    fileTree(downloadedContractsDir).each { jar ->
        from zipTree(jar)
    }
    into "${buildDir}/unpackedContracts"
}

// 4. Delete unwanted contracts (optional filtering)
task deleteUnwantedContracts(dependsOn: unzipContracts) {
    doLast {
        // delete contracts not belonging to this service
    }
}

// 5. Set task dependencies
unzipContracts.dependsOn("getContracts")
deleteUnwantedContracts.dependsOn("unzipContracts")
build.dependsOn("deleteUnwantedContracts")
```

Then configure the plugin by specifying the directory that contains the contracts:

```groovy
contracts {
    contractsDslDir = new File("${buildDir}/unpackedContracts")
}
```
