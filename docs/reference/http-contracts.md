# HTTP Contracts

HTTP contracts define the request a consumer will send and the response the producer must return. Stubborn Contract generates:

- A WireMock stub JSON for consumers to run locally
- A test method on the producer side that verifies the actual implementation matches

## Contract formats

Contracts can be written in **YAML** (recommended for readability) or the **Java/Groovy DSL**.

## Minimal example

::: code-group

```yaml [YAML]
description: "Should return a fraud check result"
request:
  method: PUT
  url: /fraudcheck
  headers:
    Content-Type: application/json
  body:
    clientId: 1234567890
    loanAmount: 99999
  matchers:
    body:
      - path: $.clientId
        type: by_type
response:
  status: 200
  headers:
    Content-Type: application/json
  body:
    fraudCheckStatus: "FRAUD"
    rejection.reason: "Amount too high"
```

```groovy [Groovy DSL]
import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should return a fraud check result"
    request {
        method PUT()
        url '/fraudcheck'
        headers { contentType(applicationJson()) }
        body(clientId: 1234567890, loanAmount: 99999)
        bodyMatchers {
            jsonPath('$.clientId', byType())
        }
    }
    response {
        status OK()
        headers { contentType(applicationJson()) }
        body(fraudCheckStatus: "FRAUD", "rejection.reason": "Amount too high")
    }
}
```

:::

## Request matching

### URL matching

| Field | Description |
|---|---|
| `url` | Exact URL string |
| `urlPattern` | Regex pattern |
| `urlPath` | Exact path (ignores query params) |
| `urlPathPattern` | Regex path (ignores query params) |
| `queryParameters` | Map of expected query parameters |

```yaml
request:
  method: GET
  urlPath: /api/users
  queryParameters:
    status: active
    page: 1
```

### Header matching

```yaml
request:
  headers:
    Content-Type: application/json
    X-Custom-Header:
      matches: "Bearer .+"
```

Available matchers: `equalTo`, `matches` (regex), `contains`, `doesNotMatch`.

### Body matching

Use `body` for the exact value and `bodyMatchers` (YAML) / `bodyMatchers {}` (DSL) to override per-field:

```yaml
request:
  body:
    clientId: 1234567890
    name: "John Doe"
  matchers:
    body:
      - path: $.clientId
        type: by_regex
        value: "[0-9]{10}"
      - path: $.name
        type: by_type
```

| Matcher type | Meaning |
|---|---|
| `by_equality` | Exact value match (default) |
| `by_type` | Same JSON type (number, string, boolean, etc.) |
| `by_regex` | Value must match the regex `value` |
| `by_date` / `by_time` / `by_timestamp` | ISO-8601 date/time format |
| `by_null` | Field must be null/absent |

### Cookie matching

```yaml
request:
  cookies:
    SESSION_ID:
      matches: "[a-f0-9]{32}"
```

## Response definition

### Status and headers

```yaml
response:
  status: 201
  headers:
    Content-Type: application/json
    Location: /api/orders/123
```

### Body matchers (response side)

Response body matchers control what the **generated consumer test** asserts:

```yaml
response:
  body:
    id: 1
    name: "John"
  matchers:
    body:
      - path: $.id
        type: by_type
      - path: $.name
        type: by_regex
        value: "[A-Za-z ]+"
```

## Dynamic values

Use built-in dynamic value generators so stubs return realistic values on every call:

```yaml
response:
  body:
    id: 1
    uuid: "abc123"
  matchers:
    body:
      - path: $.id
        type: by_regex
        value: "[0-9]+"
      - path: $.uuid
        type: by_regex
        value: "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
```

In the Groovy DSL, use `$(consumer(regex(...)), producer(execute('...')))` or the shorthand `$(anyUuid())`, `$(anyPositiveInt())`, etc.

## Multipart requests

```yaml
request:
  method: POST
  url: /upload
  multipart:
    params:
      file:
        filename: "document.pdf"
        contentType: "application/pdf"
        fileContent: base64-encoded-content
      description: "My document"
```

## Ignoring contracts

Place a `Contract.make { ignored() }` (DSL) or add `ignored: true` at the top of a YAML contract to exclude it from test generation while keeping the stub for consumers.

## Priority

When multiple WireMock stubs could match a request, use `priority` to resolve ambiguity. Lower number = higher priority (default is `0`).

```yaml
priority: 1
request:
  url: /users/admin
```

## See also

- [YAML Contract Schema](./yaml-contracts)
- [Messaging Contracts](./messaging-contracts)
- [Groovy DSL (Spring Cloud Contract 5.x docs)](https://docs.spring.io/spring-cloud-contract/reference/)
