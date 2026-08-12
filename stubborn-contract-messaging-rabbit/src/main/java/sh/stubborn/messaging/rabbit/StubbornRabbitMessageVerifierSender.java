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

package sh.stubborn.messaging.rabbit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;

/**
 * A Spring-free {@link MessageVerifierSender} for RabbitMQ (AMQP), built directly on the
 * {@code com.rabbitmq:amqp-client} {@code Connection}/{@link Channel}. It carries no
 * Spring dependency (no {@code RabbitTemplate}), so it can back contract verification
 * from any JVM runtime — plain JUnit, Quarkus, Micronaut, Helidon — as well as the Spring
 * integration that builds on top of it.
 *
 * <p>
 * The {@code destination} is treated as a <strong>queue</strong>: messages are published
 * through the AMQP default exchange ({@code ""}) with the queue name as the routing key,
 * which routes straight to the queue of that name. The queue is (idempotently) declared
 * before publishing. The body is the payload's text (UTF-8); headers travel as AMQP
 * message headers, stringified so behaviour matches the Kafka and JMS building blocks
 * (the transport-neutral payload-and-headers denominator on which cross-broker parity
 * rests).
 *
 * <p>
 * The sender is deliberately precise so a real-broker (Testcontainers) round-trip is
 * deterministic rather than flaky: it uses <em>publisher confirms</em>
 * ({@code confirmSelect} + {@code waitForConfirms}) so {@code send} only returns once the
 * broker has acknowledged the message.
 *
 * @author Marcin Grzejszczak
 */
public final class StubbornRabbitMessageVerifierSender extends AbstractStubbornRabbitVerifier
		implements MessageVerifierSender<RabbitMessage> {

	private static final Logger log = LoggerFactory.getLogger(StubbornRabbitMessageVerifierSender.class);

	/**
	 * Creates a sender against the given broker with a default confirm timeout of five
	 * seconds.
	 * @param amqpUri the AMQP URI (for example {@code amqp://host:port} from a
	 * Testcontainers RabbitMQ broker)
	 */
	public StubbornRabbitMessageVerifierSender(String amqpUri) {
		this(amqpUri, Duration.ofSeconds(5));
	}

	/**
	 * Creates a sender against the given broker.
	 * @param amqpUri the AMQP URI (for example {@code amqp://host:port})
	 * @param confirmTimeout the timeout awaited for a publisher confirm
	 */
	public StubbornRabbitMessageVerifierSender(String amqpUri, Duration confirmTimeout) {
		super(factoryFor(amqpUri), confirmTimeout);
	}

	/**
	 * Creates a sender with a default confirm timeout of five seconds, reusing an
	 * already-configured {@link ConnectionFactory}. This is the seam the Spring
	 * integration uses to share the application's Rabbit connection settings (host, port,
	 * credentials, virtual host).
	 * @param connectionFactory a fully configured AMQP connection factory
	 */
	public StubbornRabbitMessageVerifierSender(ConnectionFactory connectionFactory) {
		this(connectionFactory, Duration.ofSeconds(5));
	}

	/**
	 * Creates a sender reusing an already-configured {@link ConnectionFactory}.
	 * @param connectionFactory a fully configured AMQP connection factory
	 * @param confirmTimeout the timeout awaited for a publisher confirm
	 */
	public StubbornRabbitMessageVerifierSender(ConnectionFactory connectionFactory, Duration confirmTimeout) {
		super(connectionFactory, confirmTimeout);
	}

	@Override
	public void send(RabbitMessage message, String destination, @Nullable YamlContract contract) {
		sendMessage(message.getPayload(), message.getHeaders(), destination);
	}

	@Override
	public <T> void send(T payload, @Nullable Map<String, Object> headers, String destination,
			@Nullable YamlContract contract) {
		sendMessage(payload, headers, destination);
	}

	private void sendMessage(@Nullable Object payload, @Nullable Map<String, Object> headers, String destination) {
		String body = (payload != null) ? payload.toString() : "";
		log.info("Sending message to RabbitMQ queue '{}': {}", destination, body);
		try (Channel channel = this.connection.createChannel()) {
			channel.queueDeclare(destination, false, false, false, null);
			channel.confirmSelect();
			channel.basicPublish(DEFAULT_EXCHANGE, destination, propertiesFor(headers),
					body.getBytes(StandardCharsets.UTF_8));
			if (!channel.waitForConfirms(this.defaultReceiveTimeout.toMillis())) {
				throw new IllegalStateException("RabbitMQ broker nacked the message to queue '" + destination + "'");
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while sending to RabbitMQ queue '" + destination + "'", ex);
		}
		catch (IOException | TimeoutException ex) {
			throw new IllegalStateException("Failed to send to RabbitMQ queue '" + destination + "'", ex);
		}
	}

	private static AMQP.BasicProperties propertiesFor(@Nullable Map<String, Object> headers) {
		Map<String, Object> amqpHeaders = new LinkedHashMap<>();
		if (headers != null) {
			headers.forEach((key, value) -> {
				if (value != null) {
					amqpHeaders.put(key, value.toString());
				}
			});
		}
		return new AMQP.BasicProperties.Builder().contentType("application/json").headers(amqpHeaders).build();
	}

}
