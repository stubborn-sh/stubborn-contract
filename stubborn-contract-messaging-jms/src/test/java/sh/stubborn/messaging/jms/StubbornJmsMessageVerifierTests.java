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

import java.util.Map;
import java.util.Objects;
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
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessage;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trips a message through a real (embedded, in-VM) ActiveMQ Artemis broker to prove
 * the Spring-free {@link StubbornJmsMessageVerifierSender} and
 * {@link StubbornJmsMessageVerifierReceiver} send and receive payload + headers, and that
 * the base {@link ContractVerifierMessaging} surfaces both to the generator's
 * {@link ContractVerifierMessage} (because {@code JmsMessage} is a
 * {@code ContractMessage}).
 *
 * <p>
 * The broker runs in-process over the {@code vm://} transport, so unlike the Kafka and
 * RabbitMQ building blocks this real-broker round-trip needs no Docker.
 *
 * <p>
 * Sender and receiver are independent objects (as they are independent beans), and each
 * test uses a unique queue so the broker cannot leak a message from one test into
 * another. JMS property names must be valid identifiers, so header keys use {@code _}
 * rather than {@code -}.
 *
 * @author Marcin Grzejszczak
 */
class StubbornJmsMessageVerifierTests {

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

	private static StubbornJmsMessageVerifierSender sender() {
		return new StubbornJmsMessageVerifierSender(connectionFactory);
	}

	private static StubbornJmsMessageVerifierReceiver receiver() {
		return new StubbornJmsMessageVerifierReceiver(connectionFactory);
	}

	@Test
	void sendsAndReceivesPayloadAndHeaders() {
		String queue = uniqueQueue("round_trip");
		try (StubbornJmsMessageVerifierSender sender = sender();
				StubbornJmsMessageVerifierReceiver receiver = receiver()) {
			sender.send(new JmsMessage("{\"message\":\"hello\"}", Map.of("X_Custom_Header", "custom-value")), queue,
					null);

			JmsMessage received = Objects.requireNonNull(receiver.receive(queue, 15, TimeUnit.SECONDS, null),
					"expected a message on '" + queue + "'");

			assertThat(received.getPayload()).isEqualTo("{\"message\":\"hello\"}");
			assertThat(received.getHeaders()).containsEntry("X_Custom_Header", "custom-value");
		}
	}

	@Test
	void contractVerifierMessagingPreservesPayloadAndHeadersViaContractMessage() {
		String queue = uniqueQueue("helper");
		try (StubbornJmsMessageVerifierSender sender = sender();
				StubbornJmsMessageVerifierReceiver receiver = receiver()) {
			// No per-transport helper: the base ContractVerifierMessaging preserves
			// headers
			// because JmsMessage is a ContractMessage.
			ContractVerifierMessaging<JmsMessage> messaging = new ContractVerifierMessaging<>(sender, receiver);

			messaging.send(messaging.create("{\"id\":42}", Map.of("jms_correlationId", "k1")), queue, null);

			ContractVerifierMessage received = Objects.requireNonNull(messaging.receive(queue),
					"expected a message on '" + queue + "'");

			assertThat(received.getPayload()).isEqualTo("{\"id\":42}");
			assertThat(received.getHeader("jms_correlationId")).isEqualTo("k1");
		}
	}

	private static String uniqueQueue(String prefix) {
		return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
	}

}
