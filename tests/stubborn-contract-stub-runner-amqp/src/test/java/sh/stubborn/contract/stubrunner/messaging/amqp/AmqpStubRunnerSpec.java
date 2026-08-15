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

package sh.stubborn.contract.stubrunner.messaging.amqp;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import sh.stubborn.contract.stubrunner.StubTrigger;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@AutoConfigureStubRunner
@AutoConfigureMessageVerifier
@SpringBootTest(classes = AmqpMessagingApplication.class)
@Testcontainers
class AmqpStubRunnerSpec {

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

	@MockitoSpyBean
	MessageSubscriber messageSubscriber;

	ArgumentCaptor<Person> personArgumentCaptor = ArgumentCaptor.forClass(Person.class);

	@Test
	void shouldTriggerStubAmqpMessage() {
		// tag::client_trigger[]
		this.stubTrigger.trigger("contract-test.person.created.event");
		// end::client_trigger[]

		// The stub-runner Rabbit backend publishes to a real broker, so the listener
		// receives the Person asynchronously.
		Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
			then(this.messageSubscriber).should().handleMessage(this.personArgumentCaptor.capture());
			assertThat(this.personArgumentCaptor.getValue().getName()).isNotNull();
		});
	}

}
