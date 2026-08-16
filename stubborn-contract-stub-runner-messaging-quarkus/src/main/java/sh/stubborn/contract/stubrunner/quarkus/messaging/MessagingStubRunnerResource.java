/*
 * Copyright 2026-present the original author or authors.
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

package sh.stubborn.contract.stubrunner.quarkus.messaging;

import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.stubrunner.quarkus.StubRunnerResource;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;
import sh.stubborn.messaging.kafka.StubbornKafkaMessageVerifierSender;
import sh.stubborn.messaging.rabbit.StubbornRabbitMessageVerifierSender;

/**
 * A Quarkus {@code QuarkusTestResourceLifecycleManager} that boots the Stubborn Contract
 * stub runner <em>with messaging</em>: on top of the consumer-side HTTP stubbing provided
 * by {@link StubRunnerResource}, it wires a Spring-free {@link MessageVerifierSender} so
 * a triggered messaging stub is published to a <strong>real broker</strong> (Kafka or
 * RabbitMQ). A {@code @QuarkusTest} injects the running {@code BatchStubRunner} and calls
 * {@code trigger("<label>")} to send the contract's output message; the application's
 * listener then consumes it.
 *
 * <p>
 * It stays deliberately Spring-free and CDI-free, matching the HTTP module: the sender is
 * one of the plain {@code kafka-clients} / {@code amqp-client} building blocks, built
 * here from init args rather than injected. Register it with {@code @QuarkusTestResource}
 * pointing at this class and, in addition to the {@link StubRunnerResource} init args,
 * supply:
 * <ul>
 * <li>{@code transport} — {@code kafka} or {@code rabbit}.</li>
 * <li>{@code brokerAddress} — the broker address: {@code bootstrap.servers} for Kafka
 * (for example {@code localhost:9092}) or an AMQP URI for RabbitMQ (for example
 * {@code amqp://localhost:5672}).</li>
 * </ul>
 * When {@code transport} is absent the resource behaves exactly like the HTTP-only
 * {@link StubRunnerResource} (a no-op messaging backend).
 *
 * @author Stubborn Contract
 * @since 1.0.0
 * @see StubRunnerResource
 */
public class MessagingStubRunnerResource extends StubRunnerResource {

	/**
	 * Init arg selecting the transport: {@code kafka} or {@code rabbit}.
	 */
	public static final String TRANSPORT = "transport";

	/**
	 * Init arg holding the broker address ({@code bootstrap.servers} for Kafka, an AMQP
	 * URI for RabbitMQ).
	 */
	public static final String BROKER_ADDRESS = "brokerAddress";

	private static final Logger log = LoggerFactory.getLogger(MessagingStubRunnerResource.class);

	private @Nullable AutoCloseable senderToClose;

	@Override
	protected @Nullable MessageVerifierSender<?> messageVerifierSender() {
		String transport = initArg(TRANSPORT);
		if (transport == null || transport.isBlank()) {
			return null;
		}
		String brokerAddress = initArg(BROKER_ADDRESS);
		if (brokerAddress == null || brokerAddress.isBlank()) {
			throw new IllegalArgumentException(
					"The '" + BROKER_ADDRESS + "' init arg is required when '" + TRANSPORT + "' is set.");
		}
		MessageVerifierSender<?> sender = createSender(transport.trim().toLowerCase(Locale.ROOT), brokerAddress.trim());
		if (sender instanceof AutoCloseable closeable) {
			this.senderToClose = closeable;
		}
		log.info("Stub runner messaging enabled: transport='{}', brokerAddress='{}'", transport, brokerAddress);
		return sender;
	}

	private static MessageVerifierSender<?> createSender(String transport, String brokerAddress) {
		return switch (transport) {
			case "kafka" -> new StubbornKafkaMessageVerifierSender(brokerAddress);
			case "rabbit" -> new StubbornRabbitMessageVerifierSender(brokerAddress);
			default -> throw new IllegalArgumentException(
					"Unsupported '" + TRANSPORT + "' value '" + transport + "'; expected 'kafka' or 'rabbit'.");
		};
	}

	@Override
	public void stop() {
		super.stop();
		AutoCloseable closeable = this.senderToClose;
		if (closeable != null) {
			try {
				closeable.close();
			}
			catch (Exception ex) {
				log.warn("Failed to close the messaging sender", ex);
			}
			finally {
				this.senderToClose = null;
			}
		}
	}

}
