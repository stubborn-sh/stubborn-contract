/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sh.stubborn.messaging.kafka;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessagePayloads;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.kafka.KafkaMetadata;

/**
 * A Spring-free {@link MessageVerifierReceiver} for Apache Kafka, built directly on the
 * {@code kafka-clients} {@link KafkaConsumer} (no {@code KafkaTemplate}), so it can back
 * contract verification from any JVM runtime — plain JUnit, Quarkus, Micronaut, Helidon —
 * as well as the Spring integration that builds on top of it.
 *
 * <p>
 * The consumer is deliberately precise so a real-broker (Testcontainers) round-trip is
 * deterministic rather than flaky: a fresh, uniquely named consumer group is created per
 * {@code receive} with {@code auto.offset.reset=earliest} (the message is produced before
 * {@code receive} is called, so the consumer must be able to read from the start of the
 * partition), auto-commit is disabled (no background offset commits to race with the
 * poll), and {@code isolation.level=read_committed} so only fully written records are
 * surfaced. Testcontainers topics are single-partition, so the first polled record is the
 * earliest produced — a stable positional guarantee.
 *
 * <p>
 * A contract can select the record it checks through its {@code kafka.outputMessage}
 * metadata: with a {@code key}, records with another key are skipped, and with
 * {@code tombstone: true} only a record with a {@code null} value is accepted (see
 * {@link #matches}). This lets several contracts share a {@code triggeredBy} that
 * publishes several records, such as a tombstone for an old key and a record for a new
 * one. The record key is exposed as the {@value KafkaMessage#RECEIVED_KEY_HEADER} header.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public final class StubbornKafkaMessageVerifierReceiver implements MessageVerifierReceiver<KafkaMessage>, Closeable {

	private static final Logger log = LoggerFactory.getLogger(StubbornKafkaMessageVerifierReceiver.class);

	private final String bootstrapServers;

	private final Duration defaultReceiveTimeout;

	/**
	 * Creates a receiver against the given Kafka broker with a default receive timeout of
	 * five seconds.
	 * @param bootstrapServers the Kafka {@code bootstrap.servers} value (for example the
	 * address of a Testcontainers Kafka broker)
	 */
	public StubbornKafkaMessageVerifierReceiver(String bootstrapServers) {
		this(bootstrapServers, Duration.ofSeconds(5));
	}

	/**
	 * Creates a receiver against the given Kafka broker.
	 * @param bootstrapServers the Kafka {@code bootstrap.servers} value
	 * @param defaultReceiveTimeout the timeout applied by the no-timeout {@code receive}
	 * overloads
	 */
	public StubbornKafkaMessageVerifierReceiver(String bootstrapServers, Duration defaultReceiveTimeout) {
		this.bootstrapServers = bootstrapServers;
		this.defaultReceiveTimeout = defaultReceiveTimeout;
	}

	@Override
	public @Nullable KafkaMessage receive(String destination, long timeout, TimeUnit timeUnit,
			@Nullable YamlContract contract) {
		long timeoutMs = timeUnit.toMillis(timeout);
		KafkaMetadata.MessageKafkaMetadata metadata = StubbornKafkaMessageVerifierSender
			.outputMessageMetadata(contract);
		log.info("Receiving message from Kafka topic '{}' with timeout {}ms", destination, timeoutMs);
		try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(consumerProperties(),
				new StringDeserializer(), new ByteArrayDeserializer())) {
			consumer.subscribe(Collections.singletonList(destination));
			long deadline = System.currentTimeMillis() + timeoutMs;
			while (System.currentTimeMillis() < deadline) {
				long remaining = Math.max(deadline - System.currentTimeMillis(), 100L);
				ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(remaining));
				for (ConsumerRecord<String, byte[]> record : records) {
					if (!matches(record, metadata)) {
						log.debug("Skipping record with key '{}' from '{}': it is not the record the contract selects",
								record.key(), destination);
						continue;
					}
					KafkaMessage message = toMessage(record);
					log.info("Received message from '{}': {}", destination, message.getPayload());
					return message;
				}
			}
		}
		log.warn("No message received from '{}' within {}ms", destination, timeoutMs);
		return null;
	}

	@Override
	public @Nullable KafkaMessage receive(String destination, @Nullable YamlContract contract) {
		return receive(destination, this.defaultReceiveTimeout.toSeconds(), TimeUnit.SECONDS, contract);
	}

	/**
	 * Whether the record is the one the contract selects. Without a key or a tombstone in
	 * the contract's {@code kafka.outputMessage} metadata, the first record is accepted.
	 * With a key, only a record with that key is accepted; a tombstone only accepts a
	 * {@code null} value, and a contract with a key that is not a tombstone only accepts
	 * a non-{@code null} value.
	 * @param record the consumed record
	 * @param metadata the contract's output message Kafka metadata
	 * @return {@code true} if the contract checks this record
	 */
	static boolean matches(ConsumerRecord<String, byte[]> record, KafkaMetadata.MessageKafkaMetadata metadata) {
		String key = metadata.getKey();
		if (key == null && !metadata.isTombstone()) {
			return true;
		}
		if (key != null && !key.equals(record.key())) {
			return false;
		}
		return metadata.isTombstone() == (record.value() == null);
	}

	private static KafkaMessage toMessage(ConsumerRecord<String, byte[]> record) {
		Map<String, Object> headers = new LinkedHashMap<>();
		for (Header header : record.headers()) {
			headers.put(header.key(), new String(header.value(), StandardCharsets.UTF_8));
		}
		if (record.key() != null) {
			// Same header name as Spring Kafka, so a contract can match on the key.
			headers.put(KafkaMessage.RECEIVED_KEY_HEADER, record.key());
		}
		byte[] value = record.value();
		// Reconstruct the payload in the same form it was sent (text vs binary), decided
		// by
		// the contentType header, then default it if the producer supplied none.
		Object payload = (value != null) ? MessagePayloads.fromWire(value, headers) : null;
		headers.putIfAbsent(MessagePayloads.CONTENT_TYPE_HEADER, MessagePayloads.defaultContentType(payload));
		return new KafkaMessage(payload, headers);
	}

	private Properties consumerProperties() {
		Properties props = new Properties();
		props.put("bootstrap.servers", this.bootstrapServers);
		// Unique group per receive: never rejoin an existing group, so no committed
		// offsets
		// or consumer-group state can hide a freshly produced record.
		props.put("group.id", "stubborn-contract-verifier-" + UUID.randomUUID());
		// The record is produced before receive() runs, so the consumer must be able to
		// read
		// from the beginning of the partition rather than only new records.
		props.put("auto.offset.reset", "earliest");
		// No background offset commits to race with the poll loop.
		props.put("enable.auto.commit", "false");
		// Only surface fully written records, never records from an aborted transaction.
		props.put("isolation.level", "read_committed");
		return props;
	}

	/**
	 * A no-op: a fresh {@link KafkaConsumer} is created and closed within each
	 * {@code receive}, so this receiver holds no long-lived resource. The method is
	 * present so the receiver can be managed uniformly with the sender (for example as a
	 * {@code @Bean(destroyMethod = "close")}).
	 */
	@Override
	public void close() {
	}

}
