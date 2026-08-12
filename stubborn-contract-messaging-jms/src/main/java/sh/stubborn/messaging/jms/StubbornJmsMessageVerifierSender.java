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

import jakarta.jms.BytesMessage;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessagePayloads;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;

/**
 * A Spring-free {@link MessageVerifierSender} for JMS, built directly on the
 * {@code jakarta.jms} API (no {@code JmsTemplate}), so it can back contract verification
 * from any JVM runtime — plain JUnit, Quarkus, Micronaut, Helidon — as well as the Spring
 * integration that builds on top of it. It is given a provider {@link ConnectionFactory}
 * (for example an ActiveMQ Artemis one) rather than a URI, because JMS has no standard
 * URI form for connection factories.
 *
 * <p>
 * The {@code destination} is treated as a <strong>queue</strong>. A text payload is sent
 * as a {@code TextMessage} and a binary {@code byte[]} as a {@code BytesMessage}, so the
 * JMS message type itself carries the text/binary distinction the receiver reads back.
 * Headers are set as JMS string properties (so their names must be valid JMS
 * identifiers). The message is sent with a synchronous, acknowledged
 * {@code producer.send}.
 *
 * @author Marcin Grzejszczak
 */
public final class StubbornJmsMessageVerifierSender extends AbstractStubbornJmsVerifier
		implements MessageVerifierSender<JmsMessage> {

	private static final Logger log = LoggerFactory.getLogger(StubbornJmsMessageVerifierSender.class);

	/**
	 * Creates a sender with a default timeout of five seconds.
	 * @param connectionFactory the JMS provider connection factory
	 */
	public StubbornJmsMessageVerifierSender(ConnectionFactory connectionFactory) {
		this(connectionFactory, Duration.ofSeconds(5));
	}

	/**
	 * Creates a sender.
	 * @param connectionFactory the JMS provider connection factory
	 * @param defaultTimeout the default timeout (kept for symmetry with the receiver)
	 */
	public StubbornJmsMessageVerifierSender(ConnectionFactory connectionFactory, Duration defaultTimeout) {
		super(connectionFactory, defaultTimeout);
	}

	@Override
	public void send(JmsMessage message, String destination, @Nullable YamlContract contract) {
		sendMessage(message.getPayload(), message.getHeaders(), destination);
	}

	@Override
	public <T> void send(T payload, @Nullable Map<String, Object> headers, String destination,
			@Nullable YamlContract contract) {
		sendMessage(payload, headers, destination);
	}

	private void sendMessage(@Nullable Object payload, @Nullable Map<String, Object> headers, String destination) {
		log.info("Sending message to JMS queue '{}'", destination);
		try (Session session = this.connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
			Queue queue = session.createQueue(destination);
			Message message = createMessage(session, payload);
			Map<String, Object> outHeaders = new LinkedHashMap<>();
			if (headers != null) {
				outHeaders.putAll(headers);
			}
			// Stamp the payload's form so a consumer reading only headers still sees it;
			// the
			// JMS message type (Text vs Bytes) is the primary signal the receiver uses.
			outHeaders.putIfAbsent(MessagePayloads.CONTENT_TYPE_HEADER, MessagePayloads.defaultContentType(payload));
			for (Map.Entry<String, Object> entry : outHeaders.entrySet()) {
				if (entry.getValue() != null) {
					message.setStringProperty(entry.getKey(), entry.getValue().toString());
				}
			}
			try (MessageProducer producer = session.createProducer(queue)) {
				producer.send(message);
			}
		}
		catch (JMSException ex) {
			throw new IllegalStateException("Failed to send to JMS queue '" + destination + "'", ex);
		}
	}

	private static Message createMessage(Session session, @Nullable Object payload) throws JMSException {
		if (payload instanceof byte[] bytes) {
			BytesMessage message = session.createBytesMessage();
			message.writeBytes(bytes);
			return message;
		}
		return session.createTextMessage((payload != null) ? payload.toString() : "");
	}

}
