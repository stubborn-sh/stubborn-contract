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

package sh.stubborn.messaging.jms;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import sh.stubborn.contract.verifier.messaging.MessagePayloads;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused round-trip tests for the Spring-free JMS sender and receiver, driven
 * by an embedded, in-VM ActiveMQ Artemis broker (no Docker). This exercises the same code
 * the TCK conformance suite covers, but as a self-contained unit test that does not
 * depend on the shared conformance base class, so it carries the module's coverage gate
 * on its own.
 *
 * @author Marcin Grzejszczak
 */
@SuppressWarnings("NullAway")
class StubbornJmsMessagingMutationTests {

	private static EmbeddedActiveMQ broker;

	private static ConnectionFactory connectionFactory;

	@BeforeAll
	static void startBroker() throws Exception {
		Configuration config = new ConfigurationImpl().setPersistenceEnabled(false)
			.setSecurityEnabled(false)
			.addAcceptorConfiguration("in-vm", "vm://0");
		broker = new EmbeddedActiveMQ().setConfiguration(config);
		broker.start();
		connectionFactory = new ActiveMQConnectionFactory("vm://0");
	}

	@AfterAll
	static void stopBroker() throws Exception {
		if (broker != null) {
			broker.stop();
		}
	}

	private static String uniqueQueue() {
		return "q-" + UUID.randomUUID();
	}

	@Test
	void textPayloadRoundTripsAsStringWithJsonContentType() {
		String queue = uniqueQueue();
		Map<String, Object> headers = new LinkedHashMap<>();
		headers.put("custom", "value");
		try (StubbornJmsMessageVerifierSender sender = new StubbornJmsMessageVerifierSender(connectionFactory);
				StubbornJmsMessageVerifierReceiver receiver = new StubbornJmsMessageVerifierReceiver(
						connectionFactory)) {
			sender.send(new JmsMessage("{\"a\":1}", headers), queue, null);
			JmsMessage received = receiver.receive(queue, 5, TimeUnit.SECONDS, null);
			assertThat(received).isNotNull();
			assertThat(received.getPayload()).isInstanceOf(String.class).isEqualTo("{\"a\":1}");
			assertThat(received.getHeaders()).containsEntry("custom", "value")
				.containsEntry(MessagePayloads.CONTENT_TYPE_HEADER, MessagePayloads.JSON_CONTENT_TYPE);
		}
	}

	@Test
	void binaryPayloadRoundTripsAsByteArrayWithBinaryContentType() {
		String queue = uniqueQueue();
		byte[] body = { 0, 1, 2, (byte) 0xFF, 65, 66 };
		try (StubbornJmsMessageVerifierSender sender = new StubbornJmsMessageVerifierSender(connectionFactory);
				StubbornJmsMessageVerifierReceiver receiver = new StubbornJmsMessageVerifierReceiver(
						connectionFactory)) {
			sender.send(new JmsMessage(body, new LinkedHashMap<>()), queue, null);
			JmsMessage received = receiver.receive(queue, 5, TimeUnit.SECONDS, null);
			assertThat(received).isNotNull();
			assertThat(received.getPayload()).isInstanceOf(byte[].class);
			assertThat((byte[]) received.getPayload()).containsExactly(0, 1, 2, 0xFF, 65, 66);
			assertThat(received.getHeaders()).containsEntry(MessagePayloads.CONTENT_TYPE_HEADER,
					MessagePayloads.BINARY_CONTENT_TYPE);
		}
	}

	@Test
	void payloadOverloadSendRoundTrips() {
		String queue = uniqueQueue();
		Map<String, Object> headers = new LinkedHashMap<>();
		headers.put("k", "v");
		try (StubbornJmsMessageVerifierSender sender = new StubbornJmsMessageVerifierSender(connectionFactory);
				StubbornJmsMessageVerifierReceiver receiver = new StubbornJmsMessageVerifierReceiver(
						connectionFactory)) {
			sender.send("plain-text", headers, queue, null);
			JmsMessage received = receiver.receive(queue, 5, TimeUnit.SECONDS, null);
			assertThat(received).isNotNull();
			assertThat(received.getPayload()).isEqualTo("plain-text");
			assertThat(received.getHeaders()).containsEntry("k", "v");
		}
	}

	@Test
	void nullPayloadIsSentAsEmptyText() {
		String queue = uniqueQueue();
		try (StubbornJmsMessageVerifierSender sender = new StubbornJmsMessageVerifierSender(connectionFactory);
				StubbornJmsMessageVerifierReceiver receiver = new StubbornJmsMessageVerifierReceiver(
						connectionFactory)) {
			sender.send(new JmsMessage(null, new LinkedHashMap<>()), queue, null);
			JmsMessage received = receiver.receive(queue, 5, TimeUnit.SECONDS, null);
			assertThat(received).isNotNull();
			assertThat(received.getPayload()).isEqualTo("");
			assertThat(received.getHeaders()).containsEntry(MessagePayloads.CONTENT_TYPE_HEADER,
					MessagePayloads.JSON_CONTENT_TYPE);
		}
	}

	@Test
	void explicitContentTypeHeaderIsNotOverridden() {
		String queue = uniqueQueue();
		Map<String, Object> headers = new LinkedHashMap<>();
		headers.put(MessagePayloads.CONTENT_TYPE_HEADER, "text/plain");
		try (StubbornJmsMessageVerifierSender sender = new StubbornJmsMessageVerifierSender(connectionFactory);
				StubbornJmsMessageVerifierReceiver receiver = new StubbornJmsMessageVerifierReceiver(
						connectionFactory)) {
			sender.send(new JmsMessage("hi", headers), queue, null);
			JmsMessage received = receiver.receive(queue, 5, TimeUnit.SECONDS, null);
			assertThat(received).isNotNull();
			assertThat(received.getHeaders()).containsEntry(MessagePayloads.CONTENT_TYPE_HEADER, "text/plain");
		}
	}

	@Test
	void nullHeaderValueIsSkippedNotSet() {
		String queue = uniqueQueue();
		Map<String, Object> headers = new LinkedHashMap<>();
		headers.put("present", "yes");
		headers.put("absent", null);
		try (StubbornJmsMessageVerifierSender sender = new StubbornJmsMessageVerifierSender(connectionFactory);
				StubbornJmsMessageVerifierReceiver receiver = new StubbornJmsMessageVerifierReceiver(
						connectionFactory)) {
			sender.send(new JmsMessage("hi", headers), queue, null);
			JmsMessage received = receiver.receive(queue, 5, TimeUnit.SECONDS, null);
			assertThat(received).isNotNull();
			assertThat(received.getHeaders()).containsEntry("present", "yes").doesNotContainKey("absent");
		}
	}

	@Test
	void receiveReturnsNullWhenNoMessageBeforeTimeout() {
		String queue = uniqueQueue();
		try (StubbornJmsMessageVerifierReceiver receiver = new StubbornJmsMessageVerifierReceiver(connectionFactory,
				Duration.ofMillis(300))) {
			JmsMessage received = receiver.receive(queue, 300, TimeUnit.MILLISECONDS, null);
			assertThat(received).isNull();
		}
	}

	@Test
	void defaultTimeoutReceiveOverloadRoundTrips() {
		String queue = uniqueQueue();
		try (StubbornJmsMessageVerifierSender sender = new StubbornJmsMessageVerifierSender(connectionFactory);
				StubbornJmsMessageVerifierReceiver receiver = new StubbornJmsMessageVerifierReceiver(connectionFactory,
						Duration.ofSeconds(5))) {
			sender.send(new JmsMessage("via-default", new LinkedHashMap<>()), queue, null);
			JmsMessage received = receiver.receive(queue, null);
			assertThat(received).isNotNull();
			assertThat(received.getPayload()).isEqualTo("via-default");
		}
	}

}
