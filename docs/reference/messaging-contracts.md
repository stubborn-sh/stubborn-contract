# Messaging Contracts

Messaging contracts define the interaction between a message **producer** (the service sending events) and a **consumer** (the service processing them). Stubborn Contract supports:

- **Input-triggered** contracts — a consumer sends a message, the producer responds
- **Output-only** contracts — the producer publishes a message without input

Supported brokers: Kafka, Spring Cloud Stream, AMQP (ActiveMQ Artemis), JMS, Spring Integration.

## Input-triggered contract

The consumer sends a message on the input channel/topic and expects a response on the output channel:

```yaml
label: triggerOrder
input:
  triggeredBy: triggerOrder()
outputMessage:
  sentTo: orders-out
  body:
    orderId: 123
    status: CREATED
  headers:
    contentType: application/json
  matchers:
    body:
      - path: $.orderId
        type: by_type
```

The `triggeredBy` value is the name of a method the generated test will call on the producer's test base class to trigger the message flow.

## Output-only contract

The producer publishes spontaneously (scheduled jobs, domain events, etc.):

```yaml
outputMessage:
  sentTo: payment-events
  body:
    eventType: PAYMENT_COMPLETED
    amount: 99.99
    currency: USD
  headers:
    contentType: application/json
```

## The `input` block

A messaging contract is **trigger → output**: the `input` block names the trigger, and
`outputMessage` describes what the producer publishes as a result. `input` supports two fields
(see the [schema](./yaml-contracts#input-object)):

```yaml
label: processPayment
input:
  triggeredBy: sendPayment()   # a method on the test base class that makes the app publish
outputMessage:
  sentTo: payments-out
  body:
    eventType: PAYMENT_COMPLETED
    amount: 99.99
  headers:
    contentType: application/json
```

`triggeredBy` calls a method on the base class to provoke the producer; `assertThat` optionally
calls a method afterwards. A consumer that merely *reads* a message is verified by running its own
test against the stub-triggered message (see the stub-runner messaging guides), not by a separate
input-message contract.

## Body and header matchers

The same matcher types available for HTTP contracts work for messaging:

```yaml
outputMessage:
  body:
    orderId: 123
    createdAt: "2024-01-15T10:00:00Z"
  matchers:
    body:
      - path: $.orderId
        type: by_type
      - path: $.createdAt
        type: by_timestamp
  headers:
    contentType: application/json
```

## Kafka-specific setup

For Kafka contracts, add the Kafka messaging verifier to your producer test:

```xml
<dependency>
  <groupId>sh.stubborn</groupId>
  <artifactId>stubborn-contract-starter-verifier</artifactId>
  <scope>test</scope>
</dependency>
```

In `application.yml` (test):

```yaml
spring:
  kafka:
    bootstrap-servers: ${spring.embedded.kafka.brokers}
```

The base class for generated tests must implement `ContractVerifierMessage` / extend `MessagingBase`:

```java
@SpringBootTest
@AutoConfigureMessageVerifier
public abstract class MessagingBase {
    @Autowired
    private OrderService orderService;

    public void triggerOrder() {
        orderService.createOrder(new CreateOrderRequest(123L, "STANDARD"));
    }
}
```

## Spring Cloud Stream

With Spring Cloud Stream, the `sentTo` value maps to a **binding name** (not a topic name):

```yaml
outputMessage:
  sentTo: orders-out-0    # binding name, not Kafka topic
```

## AMQP / ActiveMQ Artemis

Add `stubborn-contract-verifier-spring-boot` and `spring-boot-starter-artemis`. The `sentTo` value is the queue/topic name.

## Stub Runner for messaging consumers

When a consumer test needs to receive messages from a stub:

```java
@AutoConfigureStubRunner(ids = "sh.stubborn:order-service:+:stubs:8080")
@SpringBootTest
class OrderConsumerTest {
    @Autowired
    private StubTrigger stubTrigger;

    @Test
    void should_process_created_order() {
        stubTrigger.trigger("triggerOrder");
        // assert consumer side effects
    }
}
```

### Zero-config JSON conversion for typed listeners

A triggered messaging stub is published to a **real broker** as raw JSON bytes with a
`contentType=application/json` header and **no** `__TypeId__` header. When the consumer's
listener binds a **typed** parameter — for example:

```java
@KafkaListener(topics = "orders")
void onOrder(Order order) { ... }        // Kafka

@RabbitListener(queues = "orders")
void onOrder(Order order) { ... }        // RabbitMQ
```

Stubborn registers a JSON message converter out of the box (through
`@AutoConfigureMessageVerifier`, which `@AutoConfigureStubRunner` includes) so the JSON body is
deserialized into the type inferred from the listener method parameter. You do **not** have to
hand-configure a `JsonDeserializer`, a `RecordMessageConverter`, or a `MessageConverter` — this
restores the zero-config experience Spring Cloud Contract users had with its in-memory binder.

The converter is added only when the consumer has not already defined one **and** has not
configured its own `spring.kafka.consumer.value-deserializer` (a consumer that sets, say, a
`JsonDeserializer` already yields a typed value, so Stubborn backs off rather than
double-converting). Either way, a project that configures its own conversion keeps full
control. To opt out entirely, set:

```properties
stubborn.contract.messaging.consumer-converters.enabled=false
```

## Messaging backends

The Spring-free messaging abstractions (`MessageVerifierSender` / `MessageVerifierReceiver`,
`ContractMessage`) live in the core `stubborn-contract-verifier` module. The concrete Spring
backends that implement them are auto-configured by `@AutoConfigureMessageVerifier` in
`stubborn-contract-verifier-spring-boot`:

| Backend | Where |
|---------|-------|
| Spring Integration | `stubborn-contract-verifier-spring-boot` (`integration`) |
| JMS | `stubborn-contract-verifier-spring-boot` (`jms`) |
| Apache Camel | `stubborn-contract-verifier-spring-boot` (`camel`) |
| Apache Avro payloads | `stubborn-contract-verifier-spring-boot` (`avro`) |
| Spring Cloud Stream | `stubborn-contract-verifier-spring-cloud` |
| Kafka | `stubborn-contract-messaging-kafka` |

`@AutoConfigureMessageVerifier` picks the backend present on the classpath, so in most projects
you add the relevant Spring Boot messaging starter and Stubborn wires the rest. See
[Modules & Architecture](./modules) for how these sit in the tier model.

## Spring-free building blocks (Kafka, RabbitMQ, JMS)

Underneath the Spring auto-configuration, each broker has a **Spring-free building block** — a
`MessageVerifierSender` and a `MessageVerifierReceiver` built directly on the broker client
(no `KafkaTemplate` / `RabbitTemplate` / `JmsTemplate`). They carry no Spring dependency, so
the same verifier drives contract verification from **any** JVM runtime — plain JUnit, Quarkus,
Micronaut, Helidon — and the Spring integration simply builds on top of them.

| Transport | Module | Sender / Receiver |
|-----------|--------|-------------------|
| Kafka | `stubborn-contract-messaging-kafka` | `StubbornKafkaMessageVerifierSender` / `…Receiver` |
| RabbitMQ | `stubborn-contract-messaging-rabbit` | `StubbornRabbitMessageVerifierSender` / `…Receiver` |
| JMS | `stubborn-contract-messaging-jms` | `StubbornJmsMessageVerifierSender` / `…Receiver` |

The sender and receiver are **independent types** (not one class implementing both), so a project
can override just the send side or just the receive side.

```java
var sender = new StubbornKafkaMessageVerifierSender(bootstrapServers);
var receiver = new StubbornKafkaMessageVerifierReceiver(bootstrapServers);

sender.send("{\"id\":42}", Map.of("contentType", "application/json"), "orders", null);
KafkaMessage message = receiver.receive("orders", 15, TimeUnit.SECONDS, null);
```

### Text and binary payloads

A contract payload is either **text** or **binary**, and the building blocks round-trip both
faithfully:

- A **`String`** payload (JSON, XML, plain text — the common contract body compared as text) is
  sent as text and received back as a `String`. When a contract specifies no `contentType`, the
  building blocks default it to `application/json`.
- A **`byte[]`** payload (an Avro or Protobuf body compared byte-for-byte) is sent verbatim and
  received back as a `byte[]`, with **no UTF-8 round-trip** to corrupt it. When a contract
  specifies no `contentType`, the building blocks default it to `application/octet-stream`.

The `contentType` header carries the text/binary distinction across the broker, so
`send(bytes)` reconstructs to `byte[]` and `send(text)` to `String`. On the wire each transport
uses its native binary carrier — Kafka a byte-array value serializer, RabbitMQ the raw AMQP body,
JMS a `BytesMessage` (versus a `TextMessage` for text). The single source of truth for this
convention is `MessagePayloads` in `stubborn-contract-verifier`.

```java
byte[] avro = encodeAvro(order);
sender.send(avro, Map.of(), "orders-avro", null);        // sent as application/octet-stream
KafkaMessage message = receiver.receive("orders-avro", 15, TimeUnit.SECONDS, null);
assert message.getPayload() instanceof byte[];           // preserved verbatim
```

### Cross-transport parity (conformance TCK)

`stubborn-contract-messaging-tck` holds a single transport-neutral conformance suite,
`AbstractMessageVerifierConformanceTests`, that Kafka, RabbitMQ and JMS each run against a **real
broker** (Testcontainers, or an embedded in-VM broker for JMS). It locks the same behaviour for
every transport — payload and header preservation for **both** a text (JSON) payload and a
binary (`byte[]`) payload — so the three building blocks stay interchangeable at the
contract-verification level.

## See also

- [HTTP Contracts](./http-contracts)
- [Stub Runner Reference](./stub-runner)
- [Spring Boot Auto-configuration](./stub-runner-spring-boot)
- [Modules & Architecture](./modules)
