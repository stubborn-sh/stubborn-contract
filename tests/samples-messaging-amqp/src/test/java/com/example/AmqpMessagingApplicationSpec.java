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

package com.example;

import java.util.Objects;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import sh.stubborn.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessage;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierObjectMapper;
import sh.stubborn.jsonassert.JsonAssertion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

// Context configuration would end up in base class
@AutoConfigureMessageVerifier
@SpringBootTest(classes = AmqpMessagingApplication.class,
		properties = "stubborn.contract.stubrunner.rabbit.enabled=true")
@Testcontainers
class AmqpMessagingApplicationSpec {

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

	// ALL CASES
	@Autowired
	ContractVerifierMessaging<?> contractVerifierMessaging;

	@Autowired
	ContractVerifierObjectMapper contractVerifierObjectMapper;

	@Test
	void should_work_for_triggered_based_messaging() {
		// generated test should look like this:
		publishBook();
		ContractVerifierMessage response = Objects
			.requireNonNull(this.contractVerifierMessaging.receive("test-exchange"));
		assertThat(response.getHeaders().get("contentType")).isEqualTo("application/json");
		DocumentContext parsedJson = JsonPath
			.parse(this.contractVerifierObjectMapper.writeValueAsString(Objects.requireNonNull(response.getPayload())));
		JsonAssertion.assertThat(parsedJson).field("name").isEqualTo("some");
	}

	// BASE CLASS WOULD HAVE THIS:

	@Autowired
	MessagePublisher messagePublisher;

	void publishBook() {
		this.messagePublisher.sendMessage(new Book("some"));
	}

}
