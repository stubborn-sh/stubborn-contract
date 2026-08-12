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

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.LongString;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;

/**
 * A Spring-free {@link MessageVerifierSender}/{@link MessageVerifierReceiver} for
 * RabbitMQ (AMQP), built directly on the {@code com.rabbitmq:amqp-client}
 * {@link Connection}/{@link Channel}. It carries no Spring dependency (no
 * {@code RabbitTemplate}), so it can back contract verification from any JVM runtime —
 * plain JUnit, Quarkus, Micronaut, Helidon — as well as the Spring integration that
 * builds on top of it.
 *
 * <p>
 * The {@code destination} is treated as a <strong>queue</strong>: messages are published
 * through the AMQP default exchange ({@code ""}) with the queue name as the routing key,
 * which routes straight to the queue of that name. The queue is (idempotently) declared
 * before publishing and before receiving, so a round-trip needs no external exchange or
 * binding setup. The body is the payload's text (UTF-8); headers travel as AMQP message
 * headers, stringified so behaviour matches the Kafka and JMS building blocks (the
 * transport-neutral payload-and-headers denominator on which cross-broker parity rests).
 *
 * <p>
 * Both sides are deliberately precise so a real-broker (Testcontainers) round-trip is
 * deterministic rather than flaky: the sender uses <em>publisher confirms</em>
 * ({@code confirmSelect} + {@code waitForConfirms}) so {@code send} only returns once the
 * broker has acknowledged the message; the receiver pulls with {@code basicGet} and
 * <em>manual acknowledgement</em> ({@code autoAck=false} then {@code basicAck}), so a
 * fetched-but-unprocessed message is never silently dropped and the first pulled message
 * is the earliest enqueued — a stable positional guarantee.
 *
 * @author Marcin Grzejszczak
 */
public class StubbornRabbitMessageVerifier
		implements MessageVerifierSender<RabbitMessage>, MessageVerifierReceiver<RabbitMessage>, Closeable {

	private static final Logger log = LoggerFactory.getLogger(StubbornRabbitMessageVerifier.class);

	private static final String DEFAULT_EXCHANGE = "";

	private static final long POLL_INTERVAL_MS = 100L;

	private final Duration defaultReceiveTimeout;

	private final Connection connection;

	/**
	 * Creates a verifier against the given broker with a default receive timeout of five
	 * seconds.
	 * @param amqpUri the AMQP URI (for example {@code amqp://host:port} from a
	 * Testcontainers RabbitMQ broker)
	 */
	public StubbornRabbitMessageVerifier(String amqpUri) {
		this(amqpUri, Duration.ofSeconds(5));
	}

	/**
	 * Creates a verifier against the given broker.
	 * @param amqpUri the AMQP URI (for example {@code amqp://host:port})
	 * @param defaultReceiveTimeout the timeout applied by the no-timeout {@code receive}
	 * overloads
	 */
	public StubbornRabbitMessageVerifier(String amqpUri, Duration defaultReceiveTimeout) {
		this(factoryFor(amqpUri), defaultReceiveTimeout);
	}

	/**
	 * Creates a verifier with a default receive timeout of five seconds, reusing an
	 * already-configured {@link ConnectionFactory}. This is the seam the Spring
	 * integration uses to share the application's Rabbit connection settings (host, port,
	 * credentials, virtual host).
	 * @param connectionFactory a fully configured AMQP connection factory
	 */
	public StubbornRabbitMessageVerifier(ConnectionFactory connectionFactory) {
		this(connectionFactory, Duration.ofSeconds(5));
	}

	/**
	 * Creates a verifier reusing an already-configured {@link ConnectionFactory}.
	 * @param connectionFactory a fully configured AMQP connection factory
	 * @param defaultReceiveTimeout the timeout applied by the no-timeout {@code receive}
	 * overloads
	 */
	public StubbornRabbitMessageVerifier(ConnectionFactory connectionFactory, Duration defaultReceiveTimeout) {
		this.defaultReceiveTimeout = defaultReceiveTimeout;
		this.connection = openConnection(connectionFactory);
	}

	private static ConnectionFactory factoryFor(String amqpUri) {
		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setUri(amqpUri);
			return factory;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Invalid RabbitMQ AMQP URI '" + amqpUri + "'", ex);
		}
	}

	private static Connection openConnection(ConnectionFactory factory) {
		try {
			return factory.newConnection();
		}
		catch (IOException | TimeoutException ex) {
			throw new IllegalStateException("Failed to connect to RabbitMQ broker", ex);
		}
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

	@Override
	public @Nullable RabbitMessage receive(String destination, long timeout, TimeUnit timeUnit,
			@Nullable YamlContract contract) {
		long timeoutMs = timeUnit.toMillis(timeout);
		log.info("Receiving message from RabbitMQ queue '{}' with timeout {}ms", destination, timeoutMs);
		try (Channel channel = this.connection.createChannel()) {
			channel.queueDeclare(destination, false, false, false, null);
			long deadline = System.currentTimeMillis() + timeoutMs;
			do {
				GetResponse response = channel.basicGet(destination, false);
				if (response != null) {
					channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
					RabbitMessage message = toMessage(response);
					log.info("Received message from '{}': {}", destination, message.getPayload());
					return message;
				}
				sleep(POLL_INTERVAL_MS);
			}
			while (System.currentTimeMillis() < deadline);
		}
		catch (IOException | TimeoutException ex) {
			throw new IllegalStateException("Failed to receive from RabbitMQ queue '" + destination + "'", ex);
		}
		log.warn("No message received from '{}' within {}ms", destination, timeoutMs);
		return null;
	}

	@Override
	public @Nullable RabbitMessage receive(String destination, @Nullable YamlContract contract) {
		return receive(destination, this.defaultReceiveTimeout.toSeconds(), TimeUnit.SECONDS, contract);
	}

	private static RabbitMessage toMessage(GetResponse response) {
		Map<String, Object> headers = new LinkedHashMap<>();
		Map<String, Object> amqpHeaders = response.getProps().getHeaders();
		if (amqpHeaders != null) {
			amqpHeaders.forEach((key, value) -> headers.put(key, decodeHeaderValue(value)));
		}
		headers.putIfAbsent("contentType", "application/json");
		return new RabbitMessage(new String(response.getBody(), StandardCharsets.UTF_8), headers);
	}

	private static @Nullable Object decodeHeaderValue(@Nullable Object value) {
		if (value instanceof LongString longString) {
			return longString.toString();
		}
		if (value instanceof byte[] bytes) {
			return new String(bytes, StandardCharsets.UTF_8);
		}
		return value;
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while polling RabbitMQ", ex);
		}
	}

	@Override
	public void close() {
		try {
			this.connection.close();
		}
		catch (IOException ex) {
			log.warn("Failed to close RabbitMQ connection", ex);
		}
	}

}
