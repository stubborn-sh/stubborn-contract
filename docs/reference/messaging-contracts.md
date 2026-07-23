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

## Input-only contract (consumer-side)

When testing a consumer that processes incoming messages, define what the consumer expects to receive:

```yaml
label: processPayment
input:
  messageFrom: payment-events
  messageBody:
    eventType: PAYMENT_COMPLETED
    amount: 99.99
  messageHeaders:
    contentType: application/json
```

No `outputMessage` needed — the consumer's behaviour (e.g., saving to a database) is verified by the consumer's own test.

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
  <groupId>sh.stubborn.contract</groupId>
  <artifactId>stubborn-starter-contract-verifier</artifactId>
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

With Spring Cloud Stream, the `sentTo` / `messageFrom` values map to **binding names** (not topic names):

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

## See also

- [HTTP Contracts](./http-contracts)
- [Stub Runner Reference](./stub-runner)
- [Spring Boot Auto-configuration](./stub-runner-spring-boot)
