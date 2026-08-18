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

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;
import sh.stubborn.messaging.kafka.StubbornKafkaMessageVerifierSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the transport-selection and argument-validation logic of
 * {@link MessagingStubRunnerResource#messageVerifierSender()}. These need no broker: they
 * assert the resource resolves the right {@link MessageVerifierSender} (or none) from its
 * init args. The full broker round-trip is exercised by the
 * {@code sample-quarkus-messaging} end-to-end sample.
 *
 * <p>
 * The tests live in the resource's own package so they can invoke the {@code protected}
 * {@link MessagingStubRunnerResource#messageVerifierSender()} hook directly.
 *
 * @author Marcin Grzejszczak
 */
class MessagingStubRunnerResourceTests {

	@Test
	void returnsNoSenderWhenTransportIsAbsent() {
		assertThat(senderFor(Map.of("ids", "my:group"))).isNull();
	}

	@Test
	void returnsNoSenderWhenTransportIsBlank() {
		assertThat(senderFor(Map.of(MessagingStubRunnerResource.TRANSPORT, "   "))).isNull();
	}

	@Test
	void failsWhenTransportIsSetButBrokerAddressIsMissing() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> senderFor(Map.of(MessagingStubRunnerResource.TRANSPORT, "kafka")))
			.withMessageContaining(MessagingStubRunnerResource.BROKER_ADDRESS);
	}

	@Test
	void failsForAnUnsupportedTransport() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> senderFor(Map.of(MessagingStubRunnerResource.TRANSPORT, "jms",
					MessagingStubRunnerResource.BROKER_ADDRESS, "vm://0")))
			.withMessageContaining("jms");
	}

	@Test
	void buildsAKafkaSenderForTheKafkaTransport() throws Exception {
		MessageVerifierSender<?> sender = senderFor(Map.of(MessagingStubRunnerResource.TRANSPORT, "kafka",
				MessagingStubRunnerResource.BROKER_ADDRESS, "localhost:9092"));

		assertThat(sender).isInstanceOf(StubbornKafkaMessageVerifierSender.class);
		// The kafka-clients producer connects lazily, so no running broker is needed
		// here; release the producer resources.
		if (sender instanceof StubbornKafkaMessageVerifierSender kafkaSender) {
			kafkaSender.close();
		}
	}

	@Test
	void isCaseInsensitiveForTheTransportName() throws Exception {
		MessageVerifierSender<?> sender = senderFor(Map.of(MessagingStubRunnerResource.TRANSPORT, "KAFKA",
				MessagingStubRunnerResource.BROKER_ADDRESS, "localhost:9092"));

		assertThat(sender).isInstanceOf(StubbornKafkaMessageVerifierSender.class);
		if (sender instanceof StubbornKafkaMessageVerifierSender kafkaSender) {
			kafkaSender.close();
		}
	}

	private static @Nullable MessageVerifierSender<?> senderFor(Map<String, String> initArgs) {
		MessagingStubRunnerResource resource = new MessagingStubRunnerResource();
		resource.init(initArgs);
		return resource.messageVerifierSender();
	}

}
