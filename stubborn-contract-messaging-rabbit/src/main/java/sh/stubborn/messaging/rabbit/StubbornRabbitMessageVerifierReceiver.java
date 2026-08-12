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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.LongString;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.MessagePayloads;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;

/**
 * A Spring-free {@link MessageVerifierReceiver} for RabbitMQ (AMQP), built directly on
 * the {@code com.rabbitmq:amqp-client} {@code Connection}/{@link Channel}. It carries no
 * Spring dependency (no {@code RabbitTemplate}), so it can back contract verification
 * from any JVM runtime — plain JUnit, Quarkus, Micronaut, Helidon — as well as the Spring
 * integration that builds on top of it.
 *
 * <p>
 * The {@code destination} is treated as a <strong>queue</strong>, declared idempotently
 * before receiving. A received body is surfaced as a {@code String} (UTF-8) or a
 * {@code byte[]} depending on the {@code contentType} header, mirroring what was sent;
 * AMQP message headers are surfaced as message headers (decoding {@code LongString}
 * values), with a {@code contentType} added when absent, so behaviour matches the Kafka
 * and JMS building blocks.
 *
 * <p>
 * The receiver is deliberately precise so a real-broker (Testcontainers) round-trip is
 * deterministic rather than flaky: it pulls with {@code basicGet} and <em>manual
 * acknowledgement</em> ({@code autoAck=false} then {@code basicAck}), so a
 * fetched-but-unprocessed message is never silently dropped and the first pulled message
 * is the earliest enqueued — a stable positional guarantee.
 *
 * @author Marcin Grzejszczak
 */
public final class StubbornRabbitMessageVerifierReceiver extends AbstractStubbornRabbitVerifier
		implements MessageVerifierReceiver<RabbitMessage> {

	private static final Logger log = LoggerFactory.getLogger(StubbornRabbitMessageVerifierReceiver.class);

	private static final long POLL_INTERVAL_MS = 100L;

	/**
	 * Creates a receiver against the given broker with a default receive timeout of five
	 * seconds.
	 * @param amqpUri the AMQP URI (for example {@code amqp://host:port} from a
	 * Testcontainers RabbitMQ broker)
	 */
	public StubbornRabbitMessageVerifierReceiver(String amqpUri) {
		this(amqpUri, Duration.ofSeconds(5));
	}

	/**
	 * Creates a receiver against the given broker.
	 * @param amqpUri the AMQP URI (for example {@code amqp://host:port})
	 * @param defaultReceiveTimeout the timeout applied by the no-timeout {@code receive}
	 * overloads
	 */
	public StubbornRabbitMessageVerifierReceiver(String amqpUri, Duration defaultReceiveTimeout) {
		super(factoryFor(amqpUri), defaultReceiveTimeout);
	}

	/**
	 * Creates a receiver with a default receive timeout of five seconds, reusing an
	 * already-configured {@link ConnectionFactory}. This is the seam the Spring
	 * integration uses to share the application's Rabbit connection settings (host, port,
	 * credentials, virtual host).
	 * @param connectionFactory a fully configured AMQP connection factory
	 */
	public StubbornRabbitMessageVerifierReceiver(ConnectionFactory connectionFactory) {
		this(connectionFactory, Duration.ofSeconds(5));
	}

	/**
	 * Creates a receiver reusing an already-configured {@link ConnectionFactory}.
	 * @param connectionFactory a fully configured AMQP connection factory
	 * @param defaultReceiveTimeout the timeout applied by the no-timeout {@code receive}
	 * overloads
	 */
	public StubbornRabbitMessageVerifierReceiver(ConnectionFactory connectionFactory, Duration defaultReceiveTimeout) {
		super(connectionFactory, defaultReceiveTimeout);
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
		// Reconstruct the payload in the same form it was sent (text vs binary), decided
		// by
		// the contentType header, then default it if the publisher supplied none.
		Object payload = MessagePayloads.fromWire(response.getBody(), headers);
		headers.putIfAbsent(MessagePayloads.CONTENT_TYPE_HEADER, MessagePayloads.defaultContentType(payload));
		return new RabbitMessage(payload, headers);
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

}
