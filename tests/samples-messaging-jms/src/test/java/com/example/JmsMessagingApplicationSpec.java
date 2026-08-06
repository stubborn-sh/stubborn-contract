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
import jakarta.jms.Message;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import sh.stubborn.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessage;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierObjectMapper;
import sh.stubborn.jsonassert.JsonAssertion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * SPIKE ON TESTS FROM NOTES IN MessagingSpec
 */
// Context configuration would end up in base class
@AutoConfigureMessageVerifier
@SpringBootTest(classes = JmsMessagingApplication.class)
@EnabledForJreRange(max = JRE.JAVA_22)
class JmsMessagingApplicationSpec {

	// ALL CASES
	@Autowired
	JmsTemplate jmsTemplate;

	@Autowired
	ContractVerifierMessaging contractVerifierMessaging;

	@Autowired
	ContractVerifierObjectMapper contractVerifierObjectMapper;

	@BeforeAll
	static void setupSpec() {
		System.setProperty("debug", "true");
	}

	@Test
	void shouldWorkForTriggeredBasedMessaging() {
		// generated test should look like this:
		bookReturnedTriggered();
		ContractVerifierMessage response = Objects.requireNonNull(this.contractVerifierMessaging.receive("output"));
		then(response.getHeader("BOOKNAME")).isEqualTo("foo");
		Object payload = Objects.requireNonNull(response.getPayload());
		DocumentContext parsedJson = JsonPath.parse(this.contractVerifierObjectMapper.writeValueAsString(payload));
		JsonAssertion.assertThat(parsedJson).field("bookName").isEqualTo("foo");
	}

	void bookReturnedTriggered() {
		this.jmsTemplate.convertAndSend("output", "{\"bookName\" : \"foo\" }", (Message message) -> {
			message.setStringProperty("BOOKNAME", "foo");
			return message;
		});
	}

}
