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

package sh.stubborn.contract.stubrunner.consumer.converters;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import sh.stubborn.contract.stubrunner.StubTrigger;
import sh.stubborn.contract.stubrunner.StubsMode;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Zero-config RabbitMQ consumer contract test against a real broker (Testcontainers). The
 * consumer application configures <strong>no</strong> {@code MessageConverter}: it relies
 * entirely on {@code ContractVerifierRabbitConsumerConverterConfiguration} (activated
 * through {@code @AutoConfigureStubRunner} / {@code @AutoConfigureMessageVerifier}) to
 * populate a typed {@code @RabbitListener} parameter from the contract's JSON
 * {@code outputMessage}.
 *
 * <p>
 * Without the auto-config Spring AMQP's default {@code SimpleMessageConverter} yields a
 * {@code String}, which cannot bind to {@link Person}, so the listener would fail and
 * this test would time out — which is exactly what makes it a genuine end-to-end proof of
 * the feature.
 *
 * @author Marcin Grzejszczak
 */
@SpringBootTest(classes = RabbitConsumerConverterSpec.Config.class,
		properties = { "stubborn.contract.stubrunner.rabbit.enabled=true",
				"stubborn.contract.stubrunner.kafka.enabled=false",
				"spring.autoconfigure.exclude=org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration" })
@AutoConfigureStubRunner(ids = "my:rabbit", repositoryRoot = "stubs:classpath:/stubs-rabbit/",
		stubsMode = StubsMode.REMOTE)
@AutoConfigureMessageVerifier
@DisabledOnOs(OS.WINDOWS)
@Testcontainers
class RabbitConsumerConverterSpec {

	static final String QUEUE = "consumer-converters.person.queue";

	@Container
	private static final RabbitMQContainer RABBIT = new RabbitMQContainer(
			DockerImageName.parse("rabbitmq:3.13-management-alpine"));

	@DynamicPropertySource
	static void rabbitProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.rabbitmq.host", RABBIT::getHost);
		registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
		registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
		registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
	}

	@Autowired
	StubTrigger stubTrigger;

	@Autowired
	TypedPersonListener listener;

	@Test
	void shouldBindTypedListenerParameterWithoutAnyManualJsonConfiguration() {
		this.stubTrigger.trigger("person_created_typed");

		Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
			Person received = this.listener.received();
			then(received).isNotNull();
			then(received.getName()).isEqualTo("me");
		});
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableRabbit
	static class Config {

		// Declared with the same parameters the stub-runner Rabbit sender uses
		// (non-durable, non-exclusive, non-auto-delete) so the idempotent redeclare never
		// conflicts.
		@Bean
		Queue personQueue() {
			return new Queue(QUEUE, false, false, false);
		}

		@Bean
		TypedPersonListener typedPersonListener() {
			return new TypedPersonListener();
		}

	}

	static class TypedPersonListener {

		private final AtomicReference<Person> received = new AtomicReference<>();

		// The stub message carries a JSON body with contentType=application/json and no
		// __TypeId__ header; the target type is inferred from this method parameter.
		@RabbitListener(queues = QUEUE)
		void listen(Person person) {
			this.received.set(person);
		}

		@Nullable Person received() {
			return this.received.get();
		}

	}

}
