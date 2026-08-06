# YAML Contract Schema

YAML contracts are the recommended format for writing Stubborn Contract tests. Each `.yml` file in your `src/test/resources/contracts/` directory is one contract.

## Top-level fields

| Field | Type | Required | Description |
|---|---|---|---|
| `description` | string | no | Human-readable description of the scenario |
| `name` | string | no | Name used for the generated test method (auto-generated from file name if absent) |
| `priority` | integer | no | WireMock stub priority (lower = higher priority, default 0) |
| `ignored` | boolean | no | Exclude from test generation; stub is still generated |
| `inProgress` | boolean | no | Mark as in-progress; generates a TODO test, stub is still usable |
| `label` | string | no | Identifier for triggering messaging contracts from `StubTrigger` |
| `request` | object | HTTP only | Incoming request definition |
| `response` | object | HTTP only | Response to return |
| `input` | object | Messaging only | Input message definition |
| `outputMessage` | object | Messaging only | Output message definition |

## `request` object

| Field | Type | Description |
|---|---|---|
| `method` | string | HTTP method: `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `HEAD`, `OPTIONS` |
| `url` | string | Exact URL (including query string) |
| `urlPath` | string | Exact path without query string |
| `urlPattern` | string | Regex pattern for full URL |
| `urlPathPattern` | string | Regex pattern for path only |
| `queryParameters` | map | Query parameter matchers |
| `headers` | map | Request header matchers |
| `cookies` | map | Cookie matchers |
| `body` | any | Expected request body (JSON, XML, or plain string) |
| `matchers.body` | list | Per-field body matchers (overrides exact matching for listed paths) |
| `matchers.headers` | list | Per-header matchers |
| `matchers.queryParameters` | list | Per-parameter matchers |

## `response` object

| Field | Type | Description |
|---|---|---|
| `status` | integer | HTTP status code |
| `headers` | map | Response headers |
| `body` | any | Response body |
| `bodyFromFile` | string | Read body from this classpath file |
| `bodyFromFileAsBytes` | string | Read body as raw bytes from this classpath file |
| `matchers.body` | list | Asserted paths in generated consumer tests |
| `matchers.headers` | list | Asserted headers in generated consumer tests |
| `fixedDelayMilliseconds` | integer | Artificial delay for the WireMock stub |
| `async` | boolean | Respond asynchronously |

## Body matchers list entry

Each entry in `matchers.body` applies to one JSON path:

| Field | Type | Required | Description |
|---|---|---|---|
| `path` | string | yes | JSONPath expression (e.g., `$.items[0].id`) |
| `type` | string | yes | Matcher type (see table below) |
| `value` | string | conditional | Required for `by_regex`, `by_equality`, `by_command` |
| `minOccurrence` | integer | no | For array paths: minimum number of elements |
| `maxOccurrence` | integer | no | For array paths: maximum number of elements |
| `predefinedRegex` | string | no | One of the pre-built regex shortcuts |

### Matcher types

| `type` | Description |
|---|---|
| `by_equality` | Exact value match |
| `by_type` | Same JSON type (string, number, boolean, object, array) |
| `by_regex` | Value matches the `value` regex |
| `by_date` | ISO-8601 date (`yyyy-MM-dd`) |
| `by_time` | ISO-8601 time (`HH:mm:ss`) |
| `by_timestamp` | ISO-8601 datetime |
| `by_null` | Value is null |
| `by_command` | Call `value` as a Java method to assert |
| `by_empty` | Empty string |

### Predefined regex shortcuts

| `predefinedRegex` | Pattern |
|---|---|
| `only_alpha_unicode` | `[\\p{L}]*` |
| `number` | `-?\\d+(\\.\\d+)?` |
| `any_double` | `-?\\d+\\.\\d+` |
| `any_boolean` | `(true\|false)` |
| `ip_address` | `(\\d{1,3}\\.){3}\\d{1,3}` |
| `hostname` | `(\\w[\\w\\-]*\\.)*[\\w][\\w\\-]*` |
| `email` | `[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}` |
| `url` | `((www\\.)([-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.)[a-z]{2,6}\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*))` |
| `uuid` | `[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}` |
| `iso_date` | `(\\d\\d\\d\\d)-(0[1-9]\|1[012])-(0[1-9]\|[12][0-9]\|3[01])` |
| `iso_date_time_with_millis` | datetime with milliseconds |
| `non_empty` | `.+` |
| `non_blank` | `\\S+.*` |

## Messaging fields

### `input` object

| Field | Type | Description |
|---|---|---|
| `triggeredBy` | string | Java method name to call on the test base class |
| `messageFrom` | string | Channel/topic name the consumer reads from |
| `messageBody` | any | Message payload |
| `messageHeaders` | map | Message headers |
| `assertThat` | string | Method name called after the message is processed (for assertions) |
| `matchers.body` | list | Per-field body matchers |

### `outputMessage` object

| Field | Type | Description |
|---|---|---|
| `sentTo` | string | Channel/topic name the producer writes to |
| `body` | any | Expected message payload |
| `headers` | map | Expected message headers |
| `matchers.body` | list | Per-field body matchers |

## Full HTTP example

```yaml
description: "Return fraud check result for high loan amounts"
priority: 1
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
        type: by_regex
        predefinedRegex: number
      - path: $.loanAmount
        type: by_type
response:
  status: 200
  headers:
    Content-Type: application/json
  body:
    fraudCheckStatus: "FRAUD"
    rejection.reason: "Amount too high"
  matchers:
    body:
      - path: $.fraudCheckStatus
        type: by_regex
        value: "FRAUD|OK"
```

## Full messaging example

```yaml
label: triggerNewOrder
input:
  triggeredBy: triggerNewOrder()
outputMessage:
  sentTo: orders-out
  body:
    orderId: 123
    status: CREATED
    amount: 49.99
  headers:
    contentType: application/json
  matchers:
    body:
      - path: $.orderId
        type: by_type
      - path: $.amount
        type: by_regex
        predefinedRegex: number
```

## See also

- [HTTP Contracts](./http-contracts)
- [Messaging Contracts](./messaging-contracts)
- [Contract DSL](./contract-dsl)
