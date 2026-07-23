# How Can I Mark that a Contract Is in Progress?

If a contract is in progress, it means that, on the producer side, tests are not generated, but the stub is generated. You can read more about this in the [Contract DSL section](../reference/contract-dsl#ignoring-contracts).

## Marking a Contract as In Progress

In the Groovy DSL, you can mark a contract as in progress as follows:

```groovy
Contract.make {
    inProgress()
    request {
        method PUT()
        url '/api/test'
        // ...
    }
    response {
        status 200
        // ...
    }
}
```

In YAML:

```yaml
inProgress: true
request:
  method: PUT
  url: /api/test
response:
  status: 200
```

## Behavior

When a contract is marked as `inProgress`:

- **On the producer side**: No test is generated for this contract. This means you don't have to implement the feature yet.
- **On the consumer side**: A stub IS generated. This means the consumer can start developing against the stub before the producer has implemented the feature.

## CI Build Considerations

In a CI build, before going to production, you would like to ensure that no in-progress contracts are on the classpath, because they may lead to false positives. For this reason, by default, in the Stubborn Contract plugin, we set the value of `failOnInProgress` to `true`.

If you want to allow such contracts when tests are to be generated, set the flag to `false` in your Maven plugin configuration:

```xml
<plugin>
    <groupId>sh.stubborn</groupId>
    <artifactId>stubborn-contract-maven-plugin</artifactId>
    <configuration>
        <failOnInProgress>false</failOnInProgress>
    </configuration>
</plugin>
```

Or in your Gradle build:

```groovy
contracts {
    failOnInProgress = false
}
```
