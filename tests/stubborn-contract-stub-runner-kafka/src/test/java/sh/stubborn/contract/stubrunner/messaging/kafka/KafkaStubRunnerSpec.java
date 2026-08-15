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

package sh.stubborn.contract.stubrunner.messaging.kafka;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Stub-runner Kafka integration test backed by a real Kafka broker (Testcontainers).
 * Verifies that triggering a messaging stub by label publishes the contract's output
 * message to the broker where the application consumes it. Requires Docker, so it runs in
 * CI.
 *
 * <p>
 * Only the trigger-by-label paths are exercised because the test fixture ships a single,
 * trigger-only contract ({@code bookReturned1.groovy}); there is no input contract, so
 * input&#8594;output stub matching is intentionally not covered here (that behaviour is
 * exercised by the Spring Cloud Stream stub-runner spec, which ships input contracts).
 *
 * @author Marcin Grzejszczak
 */
@SpringBootTest(classes = KafkaStubRunnerSpec.Config.class, properties = "debug=true")
@AutoConfigureStubRunner
@AutoConfigureMessageVerifier
@DisabledOnOs(OS.WINDOWS)
@Testcontainers
class KafkaStubRunnerSpec {

	private static final Logger log = LoggerFactory.getLogger(KafkaStubRunnerSpec.class);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Container
	private static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.1"));

	@DynamicPropertySource
	static void kafkaProperties(DynamicPropertyRegistry registry) {
		// Points BOTH the application's Spring Kafka client and the stub-runner Kafka
		// backend (which derives its bootstrap servers from the application's
		// KafkaTemplate) at the Testcontainers broker.
		registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
	}

	@Autowired
	StubFinder stubFinder;

	@Autowired
	MyMessageListener myMessageListener;

	@BeforeEach
	@AfterEach
	void setup() {
		this.myMessageListener.clear();
	}

	private Message<?> receiveFromOutput() {
		AtomicReference<Message<?>> ref = new AtomicReference<>();
		Awaitility.await().untilAsserted(() -> {
			Message<?> m = this.myMessageListener.output();
			log.info("Received from message [" + m + "]");
			then(m).isNotNull();
			ref.set(m);
		});
		Message<?> result = ref.get();
		Objects.requireNonNull(result);
		return result;
	}

	@Test
	void shouldTriggerAMessageByLabel() {
		// tag::client_trigger[]
		this.stubFinder.trigger("return_book_1");
		// end::client_trigger[]
		Awaitility.await().pollInterval(200, TimeUnit.MILLISECONDS).untilAsserted(() -> {
			// tag::client_trigger_receive[]
			Message<?> receivedMessage = receiveFromOutput();
			// end::client_trigger_receive[]
			// tag::client_trigger_message[]
			then(receivedMessage).isNotNull();
			then(bodyContainsBookName(receivedMessage.getPayload(), "foo")).isTrue();
			then(headerAsString(receivedMessage.getHeaders().get("BOOK-NAME"))).isEqualTo("foo");
			// end::client_trigger_message[]
		});
	}

	@Test
	void shouldTriggerALabelForTheExistingGroupIdAndArtifactId() {
		// tag::trigger_group_artifact[]
		this.stubFinder.trigger("my:stubs", "return_book_1");
		// end::trigger_group_artifact[]
		Awaitility.await().pollInterval(200, TimeUnit.MILLISECONDS).untilAsserted(() -> {
			Message<?> receivedMessage = receiveFromOutput();
			then(receivedMessage).isNotNull();
			then(bodyContainsBookName(receivedMessage.getPayload(), "foo")).isTrue();
			then(headerAsString(receivedMessage.getHeaders().get("BOOK-NAME"))).isEqualTo("foo");
		});
	}

	@Test
	void shouldTriggerALabelForTheExistingArtifactId() {
		// tag::trigger_artifact[]
		this.stubFinder.trigger("stubs", "return_book_1");
		// end::trigger_artifact[]
		Awaitility.await().pollInterval(200, TimeUnit.MILLISECONDS).untilAsserted(() -> {
			Message<?> receivedMessage = receiveFromOutput();
			then(receivedMessage).isNotNull();
			then(bodyContainsBookName(receivedMessage.getPayload(), "foo")).isTrue();
			then(headerAsString(receivedMessage.getHeaders().get("BOOK-NAME"))).isEqualTo("foo");
		});
	}

	@Test
	void shouldThrowAnExceptionWhenMissingLabelIsPassed() {
		thenThrownBy(() -> this.stubFinder.trigger("missing label")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldThrowAnExceptionWhenMissingLabelAndArtifactidIsPassed() {
		thenThrownBy(() -> this.stubFinder.trigger("some:service", "return_book_1"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldTriggerMessagesByRunningAllTriggers() {
		// tag::trigger_all[]
		this.stubFinder.trigger();
		// end::trigger_all[]
		Awaitility.await().pollInterval(200, TimeUnit.MILLISECONDS).untilAsserted(() -> {
			Message<?> receivedMessage = receiveFromOutput();
			then(receivedMessage).isNotNull();
			then(bodyContainsBookName(receivedMessage.getPayload(), "foo")).isTrue();
			then(headerAsString(receivedMessage.getHeaders().get("BOOK-NAME"))).isNotNull();
		});
	}

	private static boolean bodyContainsBookName(Object payload, String expectedValue) throws Exception {
		log.info("Got payload [" + payload + "]");
		String objectAsString = payloadAsString(payload);
		JsonNode json = OBJECT_MAPPER.readTree(objectAsString);
		return expectedValue.equals(json.get("bookName").asText());
	}

	private static String payloadAsString(Object payload) throws Exception {
		if (payload instanceof String string) {
			return string;
		}
		if (payload instanceof byte[] bytes) {
			return new String(bytes, StandardCharsets.UTF_8);
		}
		return OBJECT_MAPPER.writeValueAsString(payload);
	}

	private static @Nullable String headerAsString(@Nullable Object header) {
		if (header instanceof byte[] bytes) {
			return new String(bytes, StandardCharsets.UTF_8);
		}
		return (header != null) ? header.toString() : null;
	}

	@Configuration
	@ComponentScan
	@EnableAutoConfiguration
	@EnableKafka
	static class Config {

	}

	@Component
	static class MyMessageListener {

		private static final Logger log = LoggerFactory.getLogger(MyMessageListener.class);

		@Nullable Message<?> output;

		@KafkaListener(topics = { "output" })
		void output(Message<?> message) {
			log.info("I got the message [" + message + "]");
			this.output = message;
		}

		void clear() {
			this.output = null;
		}

		@Nullable Message<?> output() {
			return this.output;
		}

	}

}
