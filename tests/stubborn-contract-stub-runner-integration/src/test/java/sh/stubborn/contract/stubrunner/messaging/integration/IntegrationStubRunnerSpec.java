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

package sh.stubborn.contract.stubrunner.messaging.integration;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.messaging.Message;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
@ImportResource("classpath*:integration-context.xml")
@AutoConfigureStubRunner
@SpringBootTest(classes = IntegrationStubRunnerSpec.Config.class)
@DisabledOnOs(OS.WINDOWS)
class IntegrationStubRunnerSpec {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Autowired
	StubFinder stubFinder;

	@Autowired
	MessageVerifierReceiver<Message<?>> messaging;

	@BeforeEach
	void setup() {
		// ensure that message were taken from the queue
		this.messaging.receive("outputTest", 100, TimeUnit.MILLISECONDS);
	}

	@Test
	@DisplayName("should trigger a message by label")
	void shouldTriggerAMessageByLabel() {
		// tag::client_trigger[]
		this.stubFinder.trigger("return_book_1");
		// end::client_trigger[]

		// tag::client_trigger_receive[]
		Message<?> receivedMessage = this.messaging.receive("outputTest");
		// end::client_trigger_receive[]

		// tag::client_trigger_message[]
		then(receivedMessage).isNotNull();
		then(receivedMessage).satisfies((message) -> {
			then(assertJsons(message.getPayload())).isTrue();
			then(message.getHeaders().get("BOOK-NAME")).isEqualTo("foo");
		});
		// end::client_trigger_message[]
	}

	@Test
	@DisplayName("should trigger a label for the existing groupId and artifactId")
	void shouldTriggerALabelForTheExistingGroupIdAndArtifactId() {
		// tag::trigger_group_artifact[]
		this.stubFinder.trigger("sh.stubborn.contract.verifier.stubs:integrationService", "return_book_1");
		// end::trigger_group_artifact[]

		Message<?> receivedMessage = this.messaging.receive("outputTest");

		then(receivedMessage).isNotNull();
		then(receivedMessage).satisfies((message) -> {
			then(assertJsons(message.getPayload())).isTrue();
			then(message.getHeaders().get("BOOK-NAME")).isEqualTo("foo");
		});
	}

	@Test
	@DisplayName("should trigger a label for the existing artifactId")
	void shouldTriggerALabelForTheExistingArtifactId() {
		// tag::trigger_artifact[]
		this.stubFinder.trigger("integrationService", "return_book_1");
		// end::trigger_artifact[]

		Message<?> receivedMessage = this.messaging.receive("outputTest");

		then(receivedMessage).isNotNull();
		then(receivedMessage).satisfies((message) -> {
			then(assertJsons(message.getPayload())).isTrue();
			then(message.getHeaders().get("BOOK-NAME")).isEqualTo("foo");
		});
	}

	@Test
	@DisplayName("should throw exception when missing label is passed")
	void shouldThrowExceptionWhenMissingLabelIsPassed() {
		thenThrownBy(() -> this.stubFinder.trigger("missing label")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("should throw exception when missing label and artifactid is passed")
	void shouldThrowExceptionWhenMissingLabelAndArtifactidIsPassed() {
		thenThrownBy(() -> this.stubFinder.trigger("some:service", "return_book_1"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("should trigger messages by running all triggers")
	void shouldTriggerMessagesByRunningAllTriggers() {
		// tag::trigger_all[]
		this.stubFinder.trigger();
		// end::trigger_all[]

		Message<?> receivedMessage = this.messaging.receive("outputTest");

		then(receivedMessage).isNotNull();
		then(receivedMessage).satisfies((message) -> {
			then(assertJsons(message.getPayload())).isTrue();
			then(message.getHeaders().get("BOOK-NAME")).isEqualTo("foo");
		});
	}

	private boolean assertJsons(Object payload) {
		try {
			String objectAsString = (payload instanceof String string) ? string
					: OBJECT_MAPPER.writeValueAsString(payload);
			JsonNode json = OBJECT_MAPPER.readTree(objectAsString);
			return "foo".equals(json.path("bookName").asText());
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Configuration
	@ComponentScan
	@EnableAutoConfiguration
	static class Config {

	}

}
