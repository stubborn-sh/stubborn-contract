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

package sh.stubborn.contract.stubrunner.messaging.stream;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
@ContextConfiguration(classes = StreamStubRunnerSpec.Config.class, loader = SpringBootContextLoader.class)
@SpringBootTest(properties = "debug=true")
@AutoConfigureStubRunner
@AutoConfigureMessageVerifier
class StreamStubRunnerSpec {

	@Autowired
	StubFinder stubFinder;

	@Autowired
	MessageVerifierReceiver<Message<?>> messaging;

	@Test
	@DisplayName("should trigger a message by label")
	void shouldTriggerAMessageByLabel() {
		// tag::client_trigger[]
		this.stubFinder.trigger("return_book_1");
		// end::client_trigger[]
		Awaitility.await().untilAsserted(() -> {
			// tag::client_trigger_receive[]
			Message<?> receivedMessage = this.messaging.receive("outputToAssertBook");
			// end::client_trigger_receive[]
			// tag::client_trigger_message[]
			then(receivedMessage).isNotNull();
			Message<?> message = Objects.requireNonNull(receivedMessage);
			then(assertJsons(message.getPayload())).isTrue();
			then(message.getHeaders().get("BOOK-NAME")).isEqualTo("foo");
			// end::client_trigger_message[]
		});
	}

	@Test
	@DisplayName("should trigger a label for the existing groupId and artifactId")
	void shouldTriggerALabelForTheExistingGroupIdAndArtifactId() throws Exception {
		// tag::trigger_group_artifact[]
		this.stubFinder.trigger("sh.stubborn.contract.verifier.stubs:streamService", "return_book_1");
		// end::trigger_group_artifact[]
		Message<?> receivedMessage = this.messaging.receive("outputToAssertBook");
		then(receivedMessage).isNotNull();
		Message<?> message = Objects.requireNonNull(receivedMessage);
		then(assertJsons(message.getPayload())).isTrue();
		then(message.getHeaders().get("BOOK-NAME")).isEqualTo("foo");
	}

	@Test
	@DisplayName("should trigger a label for the existing artifactId")
	void shouldTriggerALabelForTheExistingArtifactId() throws Exception {
		// tag::trigger_artifact[]
		this.stubFinder.trigger("streamService", "return_book_1");
		// end::trigger_artifact[]
		Message<?> receivedMessage = this.messaging.receive("outputToAssertBook");
		then(receivedMessage).isNotNull();
		Message<?> message = Objects.requireNonNull(receivedMessage);
		then(assertJsons(message.getPayload())).isTrue();
		then(message.getHeaders().get("BOOK-NAME")).isEqualTo("foo");
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
	void shouldTriggerMessagesByRunningAllTriggers() throws Exception {
		// tag::trigger_all[]
		this.stubFinder.trigger();
		// end::trigger_all[]
		Message<?> receivedMessage = this.messaging.receive("outputToAssertBook");
		then(receivedMessage).isNotNull();
		Message<?> message = Objects.requireNonNull(receivedMessage);
		then(assertJsons(message.getPayload())).isTrue();
		then(message.getHeaders().get("BOOK-NAME")).isEqualTo("foo");
	}

	private boolean assertJsons(Object payload) throws Exception {
		String objectAsString;
		if (payload instanceof String string) {
			objectAsString = string;
		}
		else if (payload instanceof byte[] bytes) {
			objectAsString = new String(bytes, StandardCharsets.UTF_8);
		}
		else {
			objectAsString = new ObjectMapper().writeValueAsString(payload);
		}
		JsonNode json = new ObjectMapper().readTree(objectAsString);
		return "foo".equals(json.get("bookName").asText());
	}

	// Contract from the other service that is a producer (I'm a consumer)
	Contract dsl =
			// tag::sample_dsl[]
			Contract.make((c) -> {
				c.label("return_book_1");
				c.input((i) -> i.triggeredBy("bookReturnedTriggered()"));
				c.outputMessage((o) -> {
					o.sentTo("returnBook");
					o.body("{ \"bookName\" : \"foo\" }");
					o.headers((h) -> h.header("BOOK-NAME", "foo"));
				});
			});

	// end::sample_dsl[]

	// Contract from my service that is processing the input message and sending out
	// another message (I'm a producer)
	Contract myDsl =
			// tag::sample_producer_dsl[]
			Contract.make((c) -> {
				c.label("return_book_2");
				c.input((i) -> i.triggeredBy("gotAMessageFromFunction()"));
				c.outputMessage((o) -> {
					o.sentTo("outputToAssertBook");
					o.body("{ \"bookName\" : \"foo\" }");
					o.headers((h) -> h.header("BOOK-NAME", "foo"));
				});
			});

	// end::sample_producer_dsl[]

	// tag::setup[]
	@ImportAutoConfiguration(TestChannelBinderConfiguration.class)
	@Configuration(proxyBeanMethods = true)
	@EnableAutoConfiguration
	protected static class Config {

		@Bean
		Function<String, String> test1() {
			return (input) -> {
				System.out.println("Test 1 [" + input + "]");
				return input;
			};
		}

	}
	// end::setup[]

}
