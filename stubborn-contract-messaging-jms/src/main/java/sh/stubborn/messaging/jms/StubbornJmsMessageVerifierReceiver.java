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
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;

/**
 * A Spring-free {@link MessageVerifierReceiver} for JMS, built directly on the
 * {@code jakarta.jms} API (no {@code JmsTemplate}), so it can back contract verification
 * from any JVM runtime — plain JUnit, Quarkus, Micronaut, Helidon — as well as the Spring
 * integration that builds on top of it. It is given a provider {@link ConnectionFactory}
 * rather than a URI.
 *
 * <p>
 * The {@code destination} is treated as a <strong>queue</strong>. Received bodies are
 * read as text; JMS string properties are surfaced as message headers, with a
 * {@code contentType} of {@code application/json} added when absent, so behaviour matches
 * the Kafka and RabbitMQ building blocks.
 *
 * <p>
 * The receiver is deliberately precise so a real-broker round-trip is deterministic
 * rather than flaky: it uses a blocking {@code consumer.receive(timeout)} on a
 * {@code CLIENT_ACKNOWLEDGE} session and acknowledges only after a successful read, so a
 * fetched-but-unprocessed message is never silently dropped and the first delivered
 * message is the earliest enqueued.
 *
 * @author Marcin Grzejszczak
 */
public final class StubbornJmsMessageVerifierReceiver extends AbstractStubbornJmsVerifier
		implements MessageVerifierReceiver<JmsMessage> {

	private static final Logger log = LoggerFactory.getLogger(StubbornJmsMessageVerifierReceiver.class);

	/**
	 * Creates a receiver with a default receive timeout of five seconds.
	 * @param connectionFactory the JMS provider connection factory
	 */
	public StubbornJmsMessageVerifierReceiver(ConnectionFactory connectionFactory) {
		this(connectionFactory, Duration.ofSeconds(5));
	}

	/**
	 * Creates a receiver.
	 * @param connectionFactory the JMS provider connection factory
	 * @param defaultReceiveTimeout the timeout applied by the no-timeout {@code receive}
	 * overloads
	 */
	public StubbornJmsMessageVerifierReceiver(ConnectionFactory connectionFactory, Duration defaultReceiveTimeout) {
		super(connectionFactory, defaultReceiveTimeout);
	}

	@Override
	public @Nullable JmsMessage receive(String destination, long timeout, TimeUnit timeUnit,
			@Nullable YamlContract contract) {
		long timeoutMs = timeUnit.toMillis(timeout);
		log.info("Receiving message from JMS queue '{}' with timeout {}ms", destination, timeoutMs);
		try (Session session = this.connection.createSession(false, Session.CLIENT_ACKNOWLEDGE)) {
			Queue queue = session.createQueue(destination);
			try (MessageConsumer consumer = session.createConsumer(queue)) {
				Message message = consumer.receive(timeoutMs);
				if (message == null) {
					log.warn("No message received from '{}' within {}ms", destination, timeoutMs);
					return null;
				}
				message.acknowledge();
				JmsMessage received = toMessage(message);
				log.info("Received message from '{}': {}", destination, received.getPayload());
				return received;
			}
		}
		catch (JMSException ex) {
			throw new IllegalStateException("Failed to receive from JMS queue '" + destination + "'", ex);
		}
	}

	@Override
	public @Nullable JmsMessage receive(String destination, @Nullable YamlContract contract) {
		return receive(destination, this.defaultReceiveTimeout.toSeconds(), TimeUnit.SECONDS, contract);
	}

	private static JmsMessage toMessage(Message message) throws JMSException {
		Map<String, Object> headers = new LinkedHashMap<>();
		Enumeration<?> names = message.getPropertyNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			headers.put(name, message.getStringProperty(name));
		}
		headers.putIfAbsent("contentType", "application/json");
		return new JmsMessage(message.getBody(String.class), headers);
	}

}
