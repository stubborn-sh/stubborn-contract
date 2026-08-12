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

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessage;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trips a message through a real RabbitMQ broker (started with Testcontainers) to
 * prove the Spring-free {@link StubbornRabbitMessageVerifier} sends and receives payload
 * + headers, and that the base {@link ContractVerifierMessaging} surfaces both to the
 * generator's {@link ContractVerifierMessage} (because {@code RabbitMessage} is a
 * {@code ContractMessage}).
 *
 * <p>
 * This is the real-broker lane: no Spring, no mock transport — the actual
 * {@code com.rabbitmq:amqp-client} publish/get code path is exercised. It requires
 * Docker, so it runs in CI.
 *
 * <p>
 * Each test uses a unique queue so the shared broker cannot leak a message from one test
 * into another — the "very precise sender and receiver" property the real-broker lane
 * relies on to stay deterministic.
 *
 * @author Marcin Grzejszczak
 */
@Testcontainers
class StubbornRabbitMessageVerifierTests {

	@Container
	private static final RabbitMQContainer RABBIT = new RabbitMQContainer(
			DockerImageName.parse("rabbitmq:3.13-management-alpine"));

	private static StubbornRabbitMessageVerifier verifier() {
		return new StubbornRabbitMessageVerifier(RABBIT.getAmqpUrl());
	}

	@Test
	void sendsAndReceivesPayloadAndHeaders() {
		String queue = uniqueQueue("round-trip");
		try (StubbornRabbitMessageVerifier verifier = verifier()) {
			verifier.send(new RabbitMessage("{\"message\":\"hello\"}", Map.of("X-Custom-Header", "custom-value")),
					queue, null);

			RabbitMessage received = Objects.requireNonNull(verifier.receive(queue, 15, TimeUnit.SECONDS, null),
					"expected a message on '" + queue + "'");

			assertThat(received.getPayload()).isEqualTo("{\"message\":\"hello\"}");
			assertThat(received.getHeaders()).containsEntry("X-Custom-Header", "custom-value");
		}
	}

	@Test
	void contractVerifierMessagingPreservesPayloadAndHeadersViaContractMessage() {
		String queue = uniqueQueue("helper");
		try (StubbornRabbitMessageVerifier verifier = verifier()) {
			// No per-transport helper: the base ContractVerifierMessaging preserves
			// headers because RabbitMessage is a ContractMessage.
			ContractVerifierMessaging<RabbitMessage> messaging = new ContractVerifierMessaging<>(verifier, verifier);

			messaging.send(messaging.create("{\"id\":42}", Map.of("rabbit_routingKey", "k1")), queue, null);

			ContractVerifierMessage received = Objects.requireNonNull(messaging.receive(queue),
					"expected a message on '" + queue + "'");

			assertThat(received.getPayload()).isEqualTo("{\"id\":42}");
			assertThat(received.getHeader("rabbit_routingKey")).isEqualTo("k1");
		}
	}

	private static String uniqueQueue(String prefix) {
		return prefix + "-" + UUID.randomUUID();
	}

}
