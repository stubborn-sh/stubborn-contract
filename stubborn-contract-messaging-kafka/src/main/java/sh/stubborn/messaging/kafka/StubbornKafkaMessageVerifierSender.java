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
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;

/**
 * A Spring-free {@link MessageVerifierSender} for Apache Kafka, built directly on the
 * {@code kafka-clients} {@link KafkaProducer} (no {@code KafkaTemplate}), so it can back
 * contract verification from any JVM runtime — plain JUnit, Quarkus, Micronaut, Helidon —
 * as well as the Spring integration that builds on top of it.
 *
 * <p>
 * String serialization is used for both keys and values: contract payloads are JSON text
 * and the generator compares them as text, so a {@link StringSerializer} is the faithful,
 * transport-neutral choice. Headers are carried as UTF-8 encoded record headers. Sends
 * are acknowledged by all in-sync replicas ({@code acks=all}) and awaited synchronously,
 * so a real-broker round-trip is deterministic rather than flaky.
 *
 * @author Marcin Grzejszczak
 */
public final class StubbornKafkaMessageVerifierSender implements MessageVerifierSender<KafkaMessage>, Closeable {

	private static final Logger log = LoggerFactory.getLogger(StubbornKafkaMessageVerifierSender.class);

	private final Duration sendTimeout;

	private final Producer<String, String> producer;

	/**
	 * Creates a sender against the given Kafka broker with a default send timeout of five
	 * seconds.
	 * @param bootstrapServers the Kafka {@code bootstrap.servers} value (for example the
	 * address of a Testcontainers Kafka broker)
	 */
	public StubbornKafkaMessageVerifierSender(String bootstrapServers) {
		this(bootstrapServers, Duration.ofSeconds(5));
	}

	/**
	 * Creates a sender against the given Kafka broker.
	 * @param bootstrapServers the Kafka {@code bootstrap.servers} value
	 * @param sendTimeout the timeout awaited for the send acknowledgement
	 */
	public StubbornKafkaMessageVerifierSender(String bootstrapServers, Duration sendTimeout) {
		this.sendTimeout = sendTimeout;
		this.producer = new KafkaProducer<>(producerProperties(bootstrapServers), new StringSerializer(),
				new StringSerializer());
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
			this.producer.send(record).get(this.sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
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

	private static Properties producerProperties(String bootstrapServers) {
		Properties props = new Properties();
		props.put("bootstrap.servers", bootstrapServers);
		props.put("acks", "all");
		return props;
	}

	@Override
	public void close() {
		this.producer.close();
	}

}
