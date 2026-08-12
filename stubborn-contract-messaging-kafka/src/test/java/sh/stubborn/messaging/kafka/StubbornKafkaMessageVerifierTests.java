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

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessage;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trips a message through a real Kafka broker (started with Testcontainers) to
 * prove the Spring-free {@link StubbornKafkaMessageVerifier} sends and receives payload +
 * headers, and that the base {@link ContractVerifierMessaging} surfaces both to the
 * generator's {@link ContractVerifierMessage} (because {@code KafkaMessage} is a
 * {@code ContractMessage}).
 *
 * <p>
 * This is the real-broker lane: no Spring, no mock transport — the actual
 * {@code kafka-clients} producer/consumer code path is exercised. It requires Docker, so
 * it runs in CI.
 *
 * <p>
 * Each test uses a unique topic so the shared broker cannot leak a record from one test
 * into another — the "very precise sender and receiver" property the real-broker lane
 * relies on to stay deterministic.
 *
 * @author Marcin Grzejszczak
 */
@Testcontainers
class StubbornKafkaMessageVerifierTests {

	@Container
	private static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.1"));

	private static StubbornKafkaMessageVerifier verifier() {
		return new StubbornKafkaMessageVerifier(KAFKA.getBootstrapServers());
	}

	@Test
	void sendsAndReceivesPayloadAndHeaders() {
		String topic = uniqueTopic("round-trip");
		try (StubbornKafkaMessageVerifier verifier = verifier()) {
			verifier.send(new KafkaMessage("{\"message\":\"hello\"}", Map.of("X-Custom-Header", "custom-value")), topic,
					null);

			KafkaMessage received = Objects.requireNonNull(verifier.receive(topic, 15, TimeUnit.SECONDS, null),
					"expected a message on '" + topic + "'");

			assertThat(received.getPayload()).isEqualTo("{\"message\":\"hello\"}");
			assertThat(received.getHeaders()).containsEntry("X-Custom-Header", "custom-value");
		}
	}

	@Test
	void contractVerifierMessagingPreservesPayloadAndHeadersViaContractMessage() {
		String topic = uniqueTopic("helper");
		try (StubbornKafkaMessageVerifier verifier = verifier()) {
			// No per-transport helper: the base ContractVerifierMessaging preserves
			// headers because KafkaMessage is a ContractMessage.
			ContractVerifierMessaging<KafkaMessage> messaging = new ContractVerifierMessaging<>(verifier, verifier);

			messaging.send(messaging.create("{\"id\":42}", Map.of("kafka_messageKey", "k1")), topic, null);

			ContractVerifierMessage received = Objects.requireNonNull(messaging.receive(topic),
					"expected a message on '" + topic + "'");

			assertThat(received.getPayload()).isEqualTo("{\"id\":42}");
			assertThat(received.getHeader("kafka_messageKey")).isEqualTo("k1");
		}
	}

	private static String uniqueTopic(String prefix) {
		return prefix + "-" + UUID.randomUUID();
	}

}
