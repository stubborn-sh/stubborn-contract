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
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;

/**
 * A Spring-free {@link MessageVerifierSender}/{@link MessageVerifierReceiver} for Apache
 * Kafka, built directly on the {@code kafka-clients}
 * {@link KafkaProducer}/{@link KafkaConsumer}. It carries no Spring dependency (no
 * {@code KafkaTemplate}), so it can back contract verification from any JVM runtime —
 * plain JUnit, Quarkus, Micronaut, Helidon — as well as the Spring integration that
 * builds on top of it.
 *
 * <p>
 * String (de)serialization is used for both keys and values: contract payloads are JSON
 * text and the generator compares them as text, so a
 * {@link StringSerializer}/{@link StringDeserializer} pair is the faithful,
 * transport-neutral choice. Headers are carried as UTF-8 encoded record headers.
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
 * @author Marcin Grzejszczak
 */
public class StubbornKafkaMessageVerifier
		implements MessageVerifierSender<KafkaMessage>, MessageVerifierReceiver<KafkaMessage>, Closeable {

	private static final Logger log = LoggerFactory.getLogger(StubbornKafkaMessageVerifier.class);

	private final String bootstrapServers;

	private final Duration defaultReceiveTimeout;

	private final Producer<String, String> producer;

	/**
	 * Creates a verifier against the given Kafka broker with a default receive timeout of
	 * five seconds.
	 * @param bootstrapServers the Kafka {@code bootstrap.servers} value (for example the
	 * address of a Testcontainers Kafka broker)
	 */
	public StubbornKafkaMessageVerifier(String bootstrapServers) {
		this(bootstrapServers, Duration.ofSeconds(5));
	}

	/**
	 * Creates a verifier against the given Kafka broker.
	 * @param bootstrapServers the Kafka {@code bootstrap.servers} value
	 * @param defaultReceiveTimeout the timeout applied by the no-timeout {@code receive}
	 * overloads
	 */
	public StubbornKafkaMessageVerifier(String bootstrapServers, Duration defaultReceiveTimeout) {
		this.bootstrapServers = bootstrapServers;
		this.defaultReceiveTimeout = defaultReceiveTimeout;
		this.producer = new KafkaProducer<>(producerProperties(), new StringSerializer(), new StringSerializer());
	}

	@Override
	public void send(KafkaMessage message, String destination, @Nullable YamlContract contract) {
		sendRecord(message.getPayload(), message.getHeaders(), destination);
	}

	@Override
	public <T> void send(T payload, @Nullable Map<String, Object> headers, String destination,
			@Nullable YamlContract contract) {
		sendRecord(payload, headers, destination);
	}

	private void sendRecord(@Nullable Object payload, @Nullable Map<String, Object> headers, String destination) {
		String value = (payload != null) ? payload.toString() : null;
		ProducerRecord<String, String> record = new ProducerRecord<>(destination, value);
		if (headers != null) {
			headers.forEach((key, headerValue) -> {
				if (headerValue != null) {
					record.headers().add(key, headerValue.toString().getBytes(StandardCharsets.UTF_8));
				}
			});
		}
		log.info("Sending message to Kafka topic '{}': {}", destination, value);
		try {
			this.producer.send(record).get(this.defaultReceiveTimeout.toMillis(), TimeUnit.MILLISECONDS);
			this.producer.flush();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while sending to Kafka topic '" + destination + "'", ex);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to send to Kafka topic '" + destination + "'", ex);
		}
	}

	@Override
	public @Nullable KafkaMessage receive(String destination, long timeout, TimeUnit timeUnit,
			@Nullable YamlContract contract) {
		long timeoutMs = timeUnit.toMillis(timeout);
		log.info("Receiving message from Kafka topic '{}' with timeout {}ms", destination, timeoutMs);
		try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties(),
				new StringDeserializer(), new StringDeserializer())) {
			consumer.subscribe(Collections.singletonList(destination));
			long deadline = System.currentTimeMillis() + timeoutMs;
			while (System.currentTimeMillis() < deadline) {
				long remaining = Math.max(deadline - System.currentTimeMillis(), 100L);
				ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(remaining));
				for (ConsumerRecord<String, String> record : records) {
					log.info("Received message from '{}': {}", destination, record.value());
					return toMessage(record);
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

	private KafkaMessage toMessage(ConsumerRecord<String, String> record) {
		Map<String, Object> headers = new LinkedHashMap<>();
		for (Header header : record.headers()) {
			headers.put(header.key(), new String(header.value(), StandardCharsets.UTF_8));
		}
		headers.putIfAbsent("contentType", "application/json");
		return new KafkaMessage(record.value(), headers);
	}

	private Properties producerProperties() {
		Properties props = new Properties();
		props.put("bootstrap.servers", this.bootstrapServers);
		props.put("acks", "all");
		return props;
	}

	private Properties consumerProperties() {
		Properties props = new Properties();
		props.put("bootstrap.servers", this.bootstrapServers);
		// Unique group per receive: never rejoin an existing group, so no committed
		// offsets or consumer-group state can hide a freshly produced record.
		props.put("group.id", "stubborn-contract-verifier-" + UUID.randomUUID());
		// The record is produced before receive() runs, so the consumer must be able to
		// read from the beginning of the partition rather than only new records.
		props.put("auto.offset.reset", "earliest");
		// No background offset commits to race with the poll loop; positioning is fully
		// determined by auto.offset.reset above.
		props.put("enable.auto.commit", "false");
		// Only surface fully written records, never records from an aborted transaction.
		props.put("isolation.level", "read_committed");
		return props;
	}

	@Override
	public void close() {
		this.producer.close();
	}

}
