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

import org.jspecify.annotations.Nullable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;
import sh.stubborn.contract.verifier.messaging.tck.AbstractMessageVerifierConformanceTests;

/**
 * Runs the transport-neutral messaging conformance suite against the Spring-free Kafka
 * sender and receiver, backed by a real Kafka broker (Testcontainers). Requires Docker,
 * so it runs in CI.
 *
 * @author Marcin Grzejszczak
 */
@Testcontainers
class KafkaMessageVerifierConformanceTests extends AbstractMessageVerifierConformanceTests<KafkaMessage> {

	@Container
	private static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.1"));

	@Override
	protected MessageVerifierSender<KafkaMessage> sender() {
		return new StubbornKafkaMessageVerifierSender(KAFKA.getBootstrapServers());
	}

	@Override
	protected MessageVerifierReceiver<KafkaMessage> receiver() {
		return new StubbornKafkaMessageVerifierReceiver(KAFKA.getBootstrapServers());
	}

	@Override
	protected KafkaMessage message(@Nullable Object payload, Map<String, Object> headers) {
		return new KafkaMessage(payload, headers);
	}

}
