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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessagePayloads;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;
import sh.stubborn.contract.verifier.messaging.kafka.KafkaMetadata;

/**
 * A Spring-free {@link MessageVerifierSender} for Apache Kafka, built directly on the
 * {@code kafka-clients} {@link KafkaProducer} (no {@code KafkaTemplate}), so it can back
 * contract verification from any JVM runtime — plain JUnit, Quarkus, Micronaut, Helidon —
 * as well as the Spring integration that builds on top of it.
 *
 * <p>
 * A Kafka record value is <strong>always {@code byte[]}</strong> at the protocol level,
 * so the value serializer is a {@link ByteArraySerializer} and never switched per
 * payload. A {@link org.apache.kafka.common.serialization.StringSerializer} is not a
 * different wire form — it is exactly {@code value.getBytes(UTF_8)} — so for a text
 * payload the bytes we publish are byte-for-byte identical to what a
 * {@code StringSerializer} would emit, and a downstream consumer using a
 * {@code StringDeserializer} decodes them unchanged. This one serde therefore carries
 * both <strong>text</strong> and <strong>binary</strong> contract payloads faithfully — a
 * JSON body as its UTF-8 bytes, an Avro/Protobuf {@code byte[]} verbatim — with no
 * double-encoding. The payload's form (text vs binary) is decided once by
 * {@link MessagePayloads} and the {@code contentType} header, not the serde, is what
 * carries it across the broker so {@link StubbornKafkaMessageVerifierReceiver}
 * reconstructs the same type. Keys are serialized as strings; headers are carried as
 * UTF-8 encoded record headers. Sends are acknowledged by all in-sync replicas
 * ({@code acks=all}) and awaited synchronously, so a real-broker round-trip is
 * deterministic rather than flaky.
 *
 * <p>
 * The record key and tombstones come from the contract's {@code kafka.outputMessage}
 * metadata, not from the message itself: {@code key} sets the record key, and
 * {@code tombstone: true} publishes a {@code null} value.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public final class StubbornKafkaMessageVerifierSender implements MessageVerifierSender<KafkaMessage>, Closeable {

	private static final Logger log = LoggerFactory.getLogger(StubbornKafkaMessageVerifierSender.class);

	private final Duration sendTimeout;

	private final Producer<String, byte[]> producer;

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
				new ByteArraySerializer());
	}

	@Override
	public void send(KafkaMessage message, String destination, @Nullable YamlContract contract) {
		sendRecord(message.getPayload(), message.getHeaders(), destination, contract);
	}

	@Override
	public <T> void send(T payload, @Nullable Map<String, Object> headers, String destination,
			@Nullable YamlContract contract) {
		sendRecord(payload, headers, destination, contract);
	}

	private void sendRecord(@Nullable Object payload, @Nullable Map<String, Object> headers, String destination,
			@Nullable YamlContract contract) {
		KafkaMetadata.MessageKafkaMetadata metadata = outputMessageMetadata(contract);
		String recordKey = metadata.getKey();
		// A tombstone carries no value, whatever the stub runner passed for the absent
		// body (it passes the JSON text "null").
		byte[] value = (payload != null && !metadata.isTombstone()) ? MessagePayloads.toByteArray(payload) : null;
		Map<String, Object> outHeaders = new LinkedHashMap<>();
		if (headers != null) {
			outHeaders.putAll(headers);
		}
		// Stamp the payload's form (text vs binary) so the receiver reconstructs it as
		// the
		// same type; a contract-provided contentType is never overridden.
		outHeaders.putIfAbsent(MessagePayloads.CONTENT_TYPE_HEADER, MessagePayloads.defaultContentType(payload));
		ProducerRecord<String, byte[]> record = new ProducerRecord<>(destination, recordKey, value);
		outHeaders.forEach((key, headerValue) -> {
			if (headerValue != null) {
				record.headers().add(key, headerValue.toString().getBytes(StandardCharsets.UTF_8));
			}
		});
		log.info("Sending message to Kafka topic '{}' with key '{}' ({})", destination, recordKey,
				(value != null) ? value.length + " byte value" : "null value");
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

	static KafkaMetadata.MessageKafkaMetadata outputMessageMetadata(@Nullable YamlContract contract) {
		if (contract == null || contract.metadata == null
				|| !contract.metadata.containsKey(KafkaMetadata.METADATA_KEY)) {
			return new KafkaMetadata.MessageKafkaMetadata();
		}
		return KafkaMetadata.fromMetadata(contract.metadata).getOutputMessage();
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
