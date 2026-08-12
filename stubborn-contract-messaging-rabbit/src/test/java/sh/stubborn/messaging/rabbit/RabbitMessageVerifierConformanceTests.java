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

import org.jspecify.annotations.Nullable;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;
import sh.stubborn.contract.verifier.messaging.tck.AbstractMessageVerifierConformanceTests;

/**
 * Runs the transport-neutral messaging conformance suite against the Spring-free RabbitMQ
 * sender and receiver, backed by a real RabbitMQ broker (Testcontainers). Requires
 * Docker, so it runs in CI.
 *
 * @author Marcin Grzejszczak
 */
@Testcontainers
class RabbitMessageVerifierConformanceTests extends AbstractMessageVerifierConformanceTests<RabbitMessage> {

	@Container
	private static final RabbitMQContainer RABBIT = new RabbitMQContainer(
			DockerImageName.parse("rabbitmq:3.13-management-alpine"));

	@Override
	protected MessageVerifierSender<RabbitMessage> sender() {
		return new StubbornRabbitMessageVerifierSender(RABBIT.getAmqpUrl());
	}

	@Override
	protected MessageVerifierReceiver<RabbitMessage> receiver() {
		return new StubbornRabbitMessageVerifierReceiver(RABBIT.getAmqpUrl());
	}

	@Override
	protected RabbitMessage message(@Nullable Object payload, Map<String, Object> headers) {
		return new RabbitMessage(payload, headers);
	}

}
