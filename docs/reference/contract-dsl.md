# Contract DSL

Stubborn Contract supports DSLs written in the following languages:

- Groovy
- YAML
- Java
- Kotlin

::: tip
Stubborn Contract supports defining multiple contracts in a single file (in Groovy, return a list instead of a single contract).
:::

The following example shows a contract definition. See the source files for complete examples:

- [Groovy contract example](https://github.com/stubborn-sh/stubborn-contract/tree/main/stubborn-contract-verifier/src/test/groovy/org/springframework/cloud/contract/verifier/builder/SpringTestMethodBodyBuildersSpec.groovy)
- [YAML contract example](https://github.com/stubborn-sh/stubborn-contract/tree/main/stubborn-contract-verifier/src/test/resources/yml/contract_rest.yml)
- [Java contract example](https://github.com/stubborn-sh/stubborn-contract/tree/main/stubborn-contract-verifier/src/test/resources/contractsToCompile/contract_rest.java)
- [Kotlin contract example](https://github.com/stubborn-sh/stubborn-contract/tree/main/stubborn-contract-verifier/src/test/resources/kotlin/contract_rest.kts)

::: tip
You can compile contracts to stubs mapping by using the following standalone Maven command:

```bash
mvn sh.stubborn:stubborn-contract-maven-plugin:convert
```
:::

## Common Top-Level Elements

A contract can contain the following top-level elements:

- **description**: An optional description for the contract.
- **name**: An optional name. If provided, it is used to name the generated test method.
- **ignored**: Set to `true` to ignore the contract (no test generated, no stub generated).
- **inProgress**: Set to `true` to mark a contract as in-progress (stub generated but no test generated on the producer side).
- **request**: The expected request (HTTP or messaging input).
- **response**: The expected response (HTTP or messaging output).

### Description

You can add a description to your contract:

```groovy
org.springframework.cloud.contract.spec.Contract.make {
    description("""
        some interesting description
    """)
    // ...
}
```

### Name

If provided, the name is used to generate the test method name:

```groovy
org.springframework.cloud.contract.spec.Contract.make {
    name("some_special_name")
    // ...
}
```

### Ignoring Contracts

If you want to ignore a contract, you can set the value of `ignored` to `true` in the contract definition:

```groovy
org.springframework.cloud.contract.spec.Contract.make {
    ignored()
    // ...
}
```

### Contracts in Progress

If a contract is in progress, on the producer side, tests are not generated, but the stub is generated. See the [how-to guide on marking contracts in progress](../howto/mark-in-progress).

```groovy
org.springframework.cloud.contract.spec.Contract.make {
    inProgress()
    // ...
}
```

### Passing Values from Files

You can read values from files:

```groovy
value(file('request.json'))
```

## HTTP Contracts

### HTTP Top-Level Elements

For HTTP contracts, the `request` section may contain:

- `method`: HTTP method (GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE).
- `url` / `urlPath`: The URL or URL path.
- `headers`: HTTP headers.
- `body`: The request body.
- `bodyMatchers`: Matchers for the request body.
- `multipart`: Multipart request configuration.
- `cookies`: HTTP cookies.

The `response` section may contain:

- `status`: HTTP status code.
- `headers`: HTTP response headers.
- `body`: The response body.
- `bodyMatchers`: Matchers for the response body.
- `cookies`: HTTP cookies.
- `async`: Set to `true` for async processing.
- `fixedDelayMilliseconds`: Delay in milliseconds.

### HTTP Method

```groovy
Contract.make {
    request {
        method 'GET'
        // or using helper methods:
        method GET()
    }
}
```

### URL

```groovy
Contract.make {
    request {
        url '/foo/bar'
        // with query parameters:
        url(value(consumer(regex('/foo/[0-9]+')), producer('/foo/123'))) {
            queryParameters {
                parameter 'limit': $(consumer(anyPositiveInt()), producer(10))
                parameter 'offset': $(consumer(anyPositiveInt()), producer(20))
            }
        }
    }
}
```

### Headers

```groovy
Contract.make {
    request {
        headers {
            header 'Content-Type': 'application/json'
            header('Accept', value(consumer(regex('application/.*')), producer('application/json')))
        }
    }
    response {
        headers {
            contentType(applicationJson())
        }
    }
}
```

### Request/Response Body

```groovy
Contract.make {
    request {
        body([
            id: $(consumer(regex('[0-9]+')), producer('123')),
            name: 'John'
        ])
    }
    response {
        body([
            result: 'success'
        ])
    }
}
```

### Dynamic Properties

Stubborn Contract lets you define dynamic values that differ between the consumer and producer sides. See the [how-to guide on dynamic values](../howto/dynamic-values).

```groovy
Contract.make {
    request {
        body([
            time: $(consumer(regex('[0-9]{4}-[0-9]{2}-[0-9]{2}.*')), producer('2016-10-10 20:10:15')),
            id: $(consumer(anyUuid()), producer('9febab1c-6f36-4a0b-88d6-3b6a6d81cd4a')),
            body: 'foo'
        ])
    }
}
```

The following built-in dynamic values are available:

- `anyAlphaUnicode()`: Any Unicode alphabetic character
- `anyAlphaNumeric()`: Any alphanumeric character
- `anyNumber()`: Any number
- `anyInteger()`: Any integer
- `anyPositiveInt()`: Any positive integer
- `anyDouble()`: Any double
- `anyHex()`: Any hex string
- `aBoolean()`: `true` or `false`
- `anyIpAddress()`: Any IP address
- `anyHostname()`: Any hostname
- `anyEmail()`: Any email address
- `anyUrl()`: Any URL
- `anyHttpsUrl()`: Any HTTPS URL
- `anyUuid()`: Any UUID
- `anyDate()`: Any date in `yyyy-MM-dd` format
- `anyDateTime()`: Any date-time in `yyyy-MM-ddTHH:mm:ss` format
- `anyTime()`: Any time in `HH:mm:ss` format
- `anyIso8601WithOffset()`: Any ISO 8601 datetime with offset
- `anyNonBlankString()`: Any non-blank string
- `anyNonEmptyString()`: Any non-empty string
- `anyOf('value1', 'value2', ...)`: Any of the specified values

### XML Support

Stubborn Contract also supports XML request/response bodies. You can use XPath matchers with the `bodyMatchers` section when using YAML, or with Groovy DSL using `$(consumer(matching(...)))`.

### Stateful Contracts

Stubborn Contract supports scenario contracts. When you need to create a stateful stub, you can define your contracts under the same directory with a common prefix and use the `priority` field to define the order.

### Async Contracts

You can mark a response as asynchronous:

```groovy
Contract.make {
    request {
        method GET()
        url '/foo'
    }
    response {
        status 200
        body 'ok'
        async()
    }
}
```

## Messaging Contracts

For messaging, a contract defines:

- **label**: Trigger name for the stub runner.
- **input**: The triggering message or method call.
- **outputMessage**: The expected output message.

```groovy
Contract.make {
    label 'some_label'
    input {
        messageFrom 'input.topic'
        messageBody([
            bookName: 'foo'
        ])
        messageHeaders {
            header 'BOOK-NAME': 'foo'
        }
    }
    outputMessage {
        sentTo 'output.topic'
        body([
            correlation: $(consumer(anyUuid()), producer('5bc4dd20-...')),
            bookName: 'foo'
        ])
        headers {
            header 'BOOK-NAME': 'foo'
        }
    }
}
```

## YAML Contract Format

Below is a full YAML contract example:

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
    headers:
      - key: Content-Type
        regex: "application/json.*"
response:
  status: 200
  body:
    fraudCheckStatus: "FRAUD"
    "rejection.reason": "Amount too high"
  headers:
    Content-Type: application/json;charset=UTF-8
  matchers:
    body:
      - path: $.['fraudCheckStatus']
        type: by_regex
        value: "FRAUD|OK"
```

## Java Contract Format

You can also define contracts in Java:

```java
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.dsl.RequestResponseContract;

class ContractConfig {
    Contract contract = Contract.make(c -> {
        c.request(r -> {
            r.method("GET");
            r.url("/resource");
        });
        c.response(res -> {
            res.status(200);
            res.body("hello");
        });
    });
}
```

## Kotlin Contract Format

Contracts can also be defined in Kotlin:

```kotlin
import sh.stubborn.contract.spec.Contract

Contract.make {
    request {
        method = GET
        url = url("/resource")
    }
    response {
        status = OK
        body = body("hello")
    }
}
```

## Groovy DSL Limitations

The following features are not yet fully supported in the Groovy DSL but work in YAML or JSON:

- Some complex XML matching scenarios
- Very deeply nested JSON with dynamic values in all levels
