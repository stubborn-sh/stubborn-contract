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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;

import sh.stubborn.contract.verifier.messaging.MessagePayloads;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for the Spring-free Kafka sender and receiver that do not
 * require a running broker. The send path is driven through a {@link MockProducer}
 * swapped into the sender; the receive-to-message mapping is exercised through the
 * package-private {@code toMessage} conversion; and the empty-poll timeout path runs
 * against an unreachable broker. The full real-broker round-trip lives in the
 * Testcontainers conformance suite.
 *
 * @author Marcin Grzejszczak
 */
@SuppressWarnings("NullAway")
class StubbornKafkaMessagingMutationTests {

	private static ConsumerRecord<String, byte[]> record(byte[] value, Map<String, String> headers) {
		ConsumerRecord<String, byte[]> record = new ConsumerRecord<>("topic", 0, 0L, "key", value);
		headers.forEach((k, v) -> record.headers().add(k, v.getBytes(StandardCharsets.UTF_8)));
		return record;
	}

	@SuppressWarnings("unchecked")
	private static KafkaMessage toMessage(ConsumerRecord<String, byte[]> record) throws Exception {
		Method m = StubbornKafkaMessageVerifierReceiver.class.getDeclaredMethod("toMessage", ConsumerRecord.class);
		m.setAccessible(true);
		return (KafkaMessage) m.invoke(null, record);
	}

	private static final Partitioner ZERO_PARTITIONER = new Partitioner() {
		@Override
		public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes,
				Cluster cluster) {
			return 0;
		}

		@Override
		public void close() {
		}

		@Override
		public void configure(Map<String, ?> configs) {
		}
	};

	private static MockProducer<String, byte[]> swapInMockProducer(StubbornKafkaMessageVerifierSender sender)
			throws Exception {
		Field field = StubbornKafkaMessageVerifierSender.class.getDeclaredField("producer");
		field.setAccessible(true);
		Producer<String, byte[]> real = (Producer<String, byte[]>) field.get(sender);
		real.close(Duration.ZERO);
		MockProducer<String, byte[]> mock = new MockProducer<>(true, ZERO_PARTITIONER, new StringSerializer(),
				new ByteArraySerializer());
		field.set(sender, mock);
		return mock;
	}

	@Test
	void toMessageDecodesTextPayloadAndDefaultsJsonContentType() throws Exception {
		Map<String, String> headers = new LinkedHashMap<>();
		headers.put("trace", "abc");
		KafkaMessage message = toMessage(record("{\"a\":1}".getBytes(StandardCharsets.UTF_8), headers));
		assertThat(message.getPayload()).isInstanceOf(String.class).isEqualTo("{\"a\":1}");
		assertThat(message.getHeaders()).containsEntry("trace", "abc")
			.containsEntry(MessagePayloads.CONTENT_TYPE_HEADER, MessagePayloads.JSON_CONTENT_TYPE);
	}

	@Test
	void toMessageKeepsBinaryPayloadWhenContentTypeBinary() throws Exception {
		Map<String, String> headers = new LinkedHashMap<>();
		headers.put(MessagePayloads.CONTENT_TYPE_HEADER, MessagePayloads.BINARY_CONTENT_TYPE);
		byte[] value = { 9, 8, 7, (byte) 0xFE };
		KafkaMessage message = toMessage(record(value, headers));
		assertThat(message.getPayload()).isInstanceOf(byte[].class);
		assertThat((byte[]) message.getPayload()).containsExactly(9, 8, 7, 0xFE);
		assertThat(message.getHeaders()).containsEntry(MessagePayloads.CONTENT_TYPE_HEADER,
				MessagePayloads.BINARY_CONTENT_TYPE);
	}

	@Test
	void toMessageWithNullValueYieldsNullPayloadAndJsonContentType() throws Exception {
		KafkaMessage message = toMessage(record(null, new LinkedHashMap<>()));
		assertThat(message.getPayload()).isNull();
		assertThat(message.getHeaders()).containsEntry(MessagePayloads.CONTENT_TYPE_HEADER,
				MessagePayloads.JSON_CONTENT_TYPE);
	}

	@Test
	void sendMessageSerializesTextAndStampsJsonContentType() throws Exception {
		try (StubbornKafkaMessageVerifierSender sender = new StubbornKafkaMessageVerifierSender("localhost:9092")) {
			MockProducer<String, byte[]> mock = swapInMockProducer(sender);
			Map<String, Object> headers = new LinkedHashMap<>();
			headers.put("custom", "value");
			sender.send(new KafkaMessage("hello", headers), "dest", null);
			List<ProducerRecord<String, byte[]>> history = mock.history();
			assertThat(history).hasSize(1);
			ProducerRecord<String, byte[]> sent = history.get(0);
			assertThat(sent.topic()).isEqualTo("dest");
			assertThat(sent.value()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
			assertThat(headerValue(sent, "custom")).isEqualTo("value");
			assertThat(headerValue(sent, MessagePayloads.CONTENT_TYPE_HEADER))
				.isEqualTo(MessagePayloads.JSON_CONTENT_TYPE);
		}
	}

	@Test
	void sendPayloadOverloadSerializesBinaryAndStampsBinaryContentType() throws Exception {
		try (StubbornKafkaMessageVerifierSender sender = new StubbornKafkaMessageVerifierSender("localhost:9092")) {
			MockProducer<String, byte[]> mock = swapInMockProducer(sender);
			byte[] payload = { 1, 2, 3 };
			sender.send(payload, new LinkedHashMap<>(), "dest", null);
			ProducerRecord<String, byte[]> sent = mock.history().get(0);
			assertThat(sent.value()).containsExactly(1, 2, 3);
			assertThat(headerValue(sent, MessagePayloads.CONTENT_TYPE_HEADER))
				.isEqualTo(MessagePayloads.BINARY_CONTENT_TYPE);
		}
	}

	@Test
	void sendNullPayloadProducesNullValueRecord() throws Exception {
		try (StubbornKafkaMessageVerifierSender sender = new StubbornKafkaMessageVerifierSender("localhost:9092")) {
			MockProducer<String, byte[]> mock = swapInMockProducer(sender);
			sender.send(new KafkaMessage(null, new LinkedHashMap<>()), "dest", null);
			assertThat(mock.history().get(0).value()).isNull();
		}
	}

	@Test
	void sendSkipsNullHeaderValues() throws Exception {
		try (StubbornKafkaMessageVerifierSender sender = new StubbornKafkaMessageVerifierSender("localhost:9092")) {
			MockProducer<String, byte[]> mock = swapInMockProducer(sender);
			Map<String, Object> headers = new LinkedHashMap<>();
			headers.put("present", "yes");
			headers.put("absent", null);
			sender.send(new KafkaMessage("x", headers), "dest", null);
			ProducerRecord<String, byte[]> sent = mock.history().get(0);
			assertThat(headerValue(sent, "present")).isEqualTo("yes");
			assertThat(sent.headers().lastHeader("absent")).isNull();
		}
	}

	@Test
	void receiveReturnsNullWhenBrokerUnreachableBeforeTimeout() {
		try (StubbornKafkaMessageVerifierReceiver receiver = new StubbornKafkaMessageVerifierReceiver("localhost:1",
				Duration.ofMillis(300))) {
			KafkaMessage message = receiver.receive("dest", 300, TimeUnit.MILLISECONDS, null);
			assertThat(message).isNull();
		}
	}

	@Test
	void defaultTimeoutReceiveOverloadReturnsNullWhenBrokerUnreachable() {
		try (StubbornKafkaMessageVerifierReceiver receiver = new StubbornKafkaMessageVerifierReceiver("localhost:1",
				Duration.ofMillis(300))) {
			KafkaMessage message = receiver.receive("dest", null);
			assertThat(message).isNull();
		}
	}

	@Test
	void consumerPropertiesCarryDeterministicReceiveSettings() throws Exception {
		StubbornKafkaMessageVerifierReceiver receiver = new StubbornKafkaMessageVerifierReceiver("broker:9092");
		Method m = StubbornKafkaMessageVerifierReceiver.class.getDeclaredMethod("consumerProperties");
		m.setAccessible(true);
		Properties props = (Properties) m.invoke(receiver);
		assertThat(props).isNotNull();
		assertThat(props.getProperty("bootstrap.servers")).isEqualTo("broker:9092");
		assertThat(props.getProperty("auto.offset.reset")).isEqualTo("earliest");
		assertThat(props.getProperty("enable.auto.commit")).isEqualTo("false");
		assertThat(props.getProperty("isolation.level")).isEqualTo("read_committed");
		assertThat(props.getProperty("group.id")).startsWith("stubborn-contract-verifier-");
		receiver.close();
	}

	@Test
	void producerPropertiesRequireAllAcks() throws Exception {
		Method m = StubbornKafkaMessageVerifierSender.class.getDeclaredMethod("producerProperties", String.class);
		m.setAccessible(true);
		Properties props = (Properties) m.invoke(null, "broker:9092");
		assertThat(props).isNotNull();
		assertThat(props.getProperty("bootstrap.servers")).isEqualTo("broker:9092");
		assertThat(props.getProperty("acks")).isEqualTo("all");
	}

	private static String headerValue(ProducerRecord<String, byte[]> record, String key) {
		Header header = record.headers().lastHeader(key);
		return (header != null) ? new String(header.value(), StandardCharsets.UTF_8) : null;
	}

}
