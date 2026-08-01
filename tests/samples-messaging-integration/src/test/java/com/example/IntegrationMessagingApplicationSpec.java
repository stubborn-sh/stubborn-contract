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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessage;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;

import static org.assertj.core.api.BDDAssertions.then;

// Context configuration would end up in base class
@AutoConfigureMessageVerifier
@SpringBootTest(classes = IntegrationMessagingApplication.class)
class IntegrationMessagingApplicationSpec {

	// ALL CASES
	@Autowired
	ContractVerifierMessaging<Message<?>> contractVerifierMessaging;

	ContractVerifierObjectMapper contractVerifierObjectMapper = new ContractVerifierObjectMapper();

	@Test
	@DisplayName("should work for triggered based messaging")
	void shouldWorkForTriggeredBasedMessaging() {
		// generated test should look like this:
		bookReturnedTriggered();
		ContractVerifierMessage response = Objects.requireNonNull(this.contractVerifierMessaging.receive("output"));
		then(response.getHeaders().get("BOOK-NAME")).isEqualTo("foo");
		Object payload = Objects.requireNonNull(response.getPayload());
		DocumentContext parsedJson = JsonPath.parse(this.contractVerifierObjectMapper.writeValueAsString(payload));
		JsonAssertion.assertThat(parsedJson).field("bookName").isEqualTo("foo");
	}

	// BASE CLASS WOULD HAVE THIS:

	@Autowired
	BookService bookService;

	@Autowired
	BookListener bookListener;

	void bookReturnedTriggered() {
		this.bookService.returnBook(new BookReturned("foo"));
	}

}
