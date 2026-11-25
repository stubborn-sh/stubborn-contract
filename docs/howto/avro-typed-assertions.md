# How Can I Verify an Avro Message the Way Its Consumer Reads It?

A generated messaging test compares the received body as JSON. JSON has fewer types than Avro, so a JSON comparison cannot see a type change. If a producer changes `createdAt` from a `timestamp-millis` `long` to a `string`, a message with the same text still passes.

Turn on `assertAvroTypes` to read the received record with the contract's Avro schema instead, under the same schema resolution rules that Schema Registry applies, and then compare the values the consumer reads.

## Turning It On

The feature is incubating and off by default. Contracts generate the same tests as before until you turn it on.

In Maven, set the plugin parameter. The plugin brings Avro itself:

```xml
<plugin>
    <groupId>sh.stubborn</groupId>
    <artifactId>stubborn-contract-maven-plugin</artifactId>
    <version>${stubborn-contract.version}</version>
    <extensions>true</extensions>
    <configuration>
        <assertAvroTypes>true</assertAvroTypes>
    </configuration>
</plugin>
```

In Gradle, set the extension property. The test generator sees your `contractTest` runtime classpath, so Avro on that classpath is enough:

```groovy
contracts {
    assertAvroTypes = true
}
```

## Which Contracts It Applies To

Only messaging contracts that declare an Avro schema under `metadata.kafka.avro.schema`. That schema is the consumer's **reader** schema: the fields the consumer reads, under the record name the producer writes. Fields the consumer does not read stay out of it, so the producer remains free to change them.

```yaml
label: order_created
input:
  triggeredBy: createOrder()
outputMessage:
  sentTo: orders
  body:
    orderId: o-1
    createdAt: "2023-11-14T22:13:20Z"
    status: PAID
    lines:
      - sku: A-1
  matchers:
    body:
      - path: $.createdAt
        type: by_type
metadata:
  kafka:
    avro:
      schema: src/main/avro/order-created-reader.avsc
```

The schema can be:

- inline JSON, starting with `{`
- a file path, resolved against the working directory and then against the contract's directory
- `classpath:` followed by a resource path, resolved on the test generator's classpath. With Maven, add the jar that holds the schema as a plugin dependency.

The schema is read once, when tests are generated, and written into the generated test. The test does not look it up again at run time.

Generation fails, naming the contract file, when a body value does not fit the schema (for example text for an `int` field, or a field the record does not have), or when a body matcher matches no field of the body.

## What Gets Generated

```java
GenericRecordAssert avroRecord = assertThatAvro(response.getPayload()).readWith("{\"type\":\"record\",...}");
avroRecord.field("orderId").isEqualTo("o-1");
avroRecord.field("createdAt").isNotNull();
avroRecord.field("status").isEqualTo("PAID");
avroRecord.field("lines[0].sku").isEqualTo("A-1");
```

`readWith` checks that the record's own (writer) schema can be read with the contract's (reader) schema, using Avro's `SchemaCompatibility`, the check Schema Registry runs. Then it reads the record as the consumer would. So the result matches what Schema Registry would decide for that consumer:

| Producer change | Result |
|---|---|
| adds a field | passes, the field is dropped |
| removes or renames a field the contract's schema does not name | passes |
| `int` to `long`, `bytes` to `string`, or adds a logical type such as `uuid` | passes, the value is promoted |
| removes or renames a field the contract's schema names, without a default | fails |
| `long` to `string`, a removed enum symbol, ... | fails, naming the field |

Before reading, the record is written with its own schema and read back as plain Avro data. A `SpecificRecord`, Java enums and logical-type conversions (`Instant`, `UUID`, `LocalDate`, `BigDecimal`) are therefore compared the same way as a `GenericRecord`:

| Avro type | Path | Compared as |
|-----------|------|-------------|
| record | `customer.name` | each named field |
| array | `lines[0].sku` | each listed element |
| map | `attributes['channel']` | each listed entry |
| union | the field itself | the branch the value resolves to |
| enum | the field itself | the symbol as text |
| `decimal` | the field itself | `BigDecimal`, by numeric value |
| `timestamp-millis`, `timestamp-micros`, `local-timestamp-*`, `date` | the field itself | the underlying number; the contract may give an ISO-8601 text instead |
| `bytes`, `fixed` without `decimal` | the field itself | presence only; `readWith` checks the type |

Body matchers work as they do for JSON. `byRegex` (and `byDate`, `byTime`, `byTimestamp`) emits `matches(...)`; a timestamp, date or time field is matched as ISO-8601 text at second precision in UTC (`2023-11-14T22:13:20`), the form those three expect. `byType()` emits `isNotNull()` plus the size checks of `minOccurrence` and `maxOccurrence` on an array; below a `byType()` record, map or array no values are compared, but matchers on its children still apply. `byNull()` emits `isNull()`, and `byCommand(...)` receives `field(...).value()`. Matcher paths use dots and `[*]` for every array element:

```groovy
bodyMatchers {
    jsonPath('$.orderId', byRegex('o-[0-9]+'))
    jsonPath('$.createdAt', byType())
    jsonPath('$.lines', byType { minOccurrence(1) })
    jsonPath('$.lines[*].sku', byRegex('[A-Z]-[0-9]+'))
}
```

## Limits

- Values are compared only for the fields the body names. The schema check covers every field of the contract's schema.
- The received payload must be an Avro `GenericRecord` (a `SpecificRecord` is one too). The stock receivers return the raw bytes for an Avro message, so configure a receiver that deserializes Avro, for example a Spring Kafka consumer with `KafkaAvroDeserializer` as its value deserializer. A test fails with a message that says so when the payload is something else.
- Bracket (`$['a']`) and recursive (`$..a`) matcher paths are not supported. Generation fails on them.
