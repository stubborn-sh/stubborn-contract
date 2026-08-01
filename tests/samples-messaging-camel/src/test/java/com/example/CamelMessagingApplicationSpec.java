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
import com.toomuchcoding.jsonassert.JsonAssertion;
import org.apache.camel.model.ModelCamelContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessage;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SPIKE ON TESTS FROM NOTES IN MessagingSpec
 */
// Context configuration would end up in base class
@AutoConfigureMessageVerifier
@SpringBootTest(classes = CamelMessagingApplication.class,
		properties = "camel.component.spring-rabbitmq.auto-declare-producer=true")
@Testcontainers
class CamelMessagingApplicationSpec {

	// ALL CASES
	@Autowired
	ModelCamelContext camelContext;

	@Autowired
	ContractVerifierMessaging<?> contractVerifierMessaging;

	ContractVerifierObjectMapper contractVerifierObjectMapper = new ContractVerifierObjectMapper();

	@Container
	static RabbitMQContainer broker = new RabbitMQContainer("rabbitmq:3.7.25-management-alpine");

	@DynamicPropertySource
	static void setup(DynamicPropertyRegistry registry) {
		registry.add("spring.rabbitmq.port", () -> broker.getAmqpPort());
	}

	@Test
	@DisplayName("should work for triggered based messaging")
	void shouldWorkForTriggeredBasedMessaging() {
		// tag::sample_dsl[]
		Contract.make((c) -> {
			c.label("return_book_1");
			c.input((i) -> i.triggeredBy("bookReturnedTriggered()"));
			c.outputMessage((o) -> {
				o.sentTo("rabbitmq:output?queue=output");
				o.body("{ \"bookName\" : \"foo\" }");
				o.headers((h) -> h.header("BOOK-NAME", "foo"));
			});
		});
		// end::sample_dsl[]
		// generated test should look like this:
		bookReturnedTriggered();
		ContractVerifierMessage response = Objects
			.requireNonNull(this.contractVerifierMessaging.receive("spring-rabbitmq:output?queues=output"));
		assertThat(response.getHeaders().get("BOOK-NAME")).isEqualTo("foo");
		DocumentContext parsedJson = JsonPath
			.parse(this.contractVerifierObjectMapper.writeValueAsString(Objects.requireNonNull(response.getPayload())));
		JsonAssertion.assertThat(parsedJson).field("bookName").isEqualTo("foo");
	}

	void bookReturnedTriggered() {
		this.camelContext.createProducerTemplate().sendBody("direct:start", "{\"bookName\" : \"foo\" }");
	}

}
