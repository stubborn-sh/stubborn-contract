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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
@SpringBootTest(classes = KafkaStubRunnerSpec.Config.class, properties = "debug=true")
@AutoConfigureStubRunner
@DisabledOnOs(OS.WINDOWS)
@EmbeddedKafka(topics = { "input", "input2", "output", "delete" })
@Disabled("Legacy embedded-broker spec superseded by real-broker coverage - the "
		+ "stubborn-contract-messaging-tck conformance suite and the broker-backed consumer ITs "
		+ "(stubborn samples). Kept until migrated to the Testcontainers/middleware approach.")
class KafkaStubRunnerSpec {

	private static final Logger log = LoggerFactory.getLogger(KafkaStubRunnerSpec.class);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Autowired
	StubFinder stubFinder;

	@Autowired
	KafkaTemplate<Object, Object> kafkaTemplate;

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

	private @Nullable Message<?> receiveNullableMessageFromOutput() {
		Message<?> m = this.myMessageListener.output();
		log.info("Received message [" + m + "]");
		return m;
	}

	// Skipping the test on Jenkins cause it's for some reason flakey only there
	@Test
	void shouldDownloadTheStubAndRegisterARouteForIt() {
		log.info("Sending the message");
		// tag::client_send[]
		Message<?> message = MessageBuilder.createMessage(new BookReturned("foo"),
				new MessageHeaders(Map.of("sample", "header")));
		this.kafkaTemplate.setDefaultTopic("input");
		var unused = this.kafkaTemplate.send(message);
		// end::client_send[]
		log.info("Message sent");
		Awaitility.await().pollInterval(200, TimeUnit.MILLISECONDS).untilAsserted(() -> {
			log.info("Receiving the message");
			// tag::client_receive[]
			Message<?> receivedMessage = receiveFromOutput();
			// end::client_receive[]
			log.info("Message received [" + receivedMessage + "]");
			// tag::client_receive_message[]
			then(receivedMessage).isNotNull();
			then(assertThatBodyContainsBookNameFoo(receivedMessage.getPayload())).isTrue();
			then(receivedMessage.getHeaders().get("BOOK-NAME")).isEqualTo("foo");
			// end::client_receive_message[]
		});
	}

	@Test
	void shouldPropagateTheKafkaRecordKeyViaMessageHeaders() {
		log.info("Sending the message");
		// tag::client_send[]
		Message<?> message = MessageBuilder.createMessage(new BookReturned("bar"),
				new MessageHeaders(Map.of("kafka_messageKey", "bar5150")));
		this.kafkaTemplate.setDefaultTopic("input2");
		var unused = this.kafkaTemplate.send(message);
		// end::client_send[]
		log.info("Message sent");
		Awaitility.await().pollInterval(200, TimeUnit.MILLISECONDS).untilAsserted(() -> {
			log.info("Receiving the message");
			// tag::client_receive[]
			Message<?> receivedMessage = receiveFromOutput();
			// end::client_receive[]
			log.info("Message received [" + receivedMessage + "]");
			// tag::client_receive_message[]
			then(receivedMessage).isNotNull();
			then(assertThatBodyContainsBookName(receivedMessage.getPayload(), "bar")).isTrue();
			then(receivedMessage.getHeaders().get("BOOK-NAME")).isEqualTo("bar");
			then(receivedMessage.getHeaders().get("kafka_receivedMessageKey")).isEqualTo("bar5150");
			// end::client_receive_message[]
		});
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
			then(assertThatBodyContainsBookNameFoo(receivedMessage.getPayload())).isTrue();
			then(receivedMessage.getHeaders().get("BOOK-NAME")).isEqualTo("foo");
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
			then(assertThatBodyContainsBookNameFoo(receivedMessage.getPayload())).isTrue();
			then(receivedMessage.getHeaders().get("BOOK-NAME")).isEqualTo("foo");
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
			then(assertThatBodyContainsBookNameFoo(receivedMessage.getPayload())).isTrue();
			then(receivedMessage.getHeaders().get("BOOK-NAME")).isEqualTo("foo");
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
			then(assertThatBodyContainsBookName(receivedMessage.getPayload())).isTrue();
			then(receivedMessage.getHeaders().get("BOOK-NAME")).isNotNull();
		});
	}

	@Test
	void shouldTriggerALabelWithNoOutputMessage() {
		// tag::trigger_no_output[]
		Message<?> message = MessageBuilder.createMessage(new BookReturned("foo"),
				new MessageHeaders(Map.of("sample", "header")));
		this.kafkaTemplate.setDefaultTopic("delete");
		var unused = this.kafkaTemplate.send(message);
		// end::trigger_no_output[]
	}

	@Test
	void shouldNotTriggerAMessageThatDoesNotMatchInput() {
		Message<?> message = MessageBuilder.createMessage(new BookReturned("notmatching"),
				new MessageHeaders(Map.of("wrong", "header")));
		this.kafkaTemplate.setDefaultTopic("input");
		var unused = this.kafkaTemplate.send(message);

		Message<?> receivedMessage = receiveNullableMessageFromOutput();

		then(receivedMessage).isNull();
	}

	private boolean assertThatBodyContainsBookNameFoo(Object payload) throws Exception {
		return assertThatBodyContainsBookName(payload, "foo");
	}

	private boolean assertThatBodyContainsBookName(Object payload, String expectedValue) throws Exception {
		log.info("Got payload [" + payload + "]");
		String objectAsString = (payload instanceof String s) ? s : OBJECT_MAPPER.writeValueAsString(payload);
		JsonNode json = OBJECT_MAPPER.readTree(objectAsString);
		return expectedValue.equals(json.get("bookName").asText());
	}

	private boolean assertThatBodyContainsBookName(Object payload) throws Exception {
		log.info("Got payload [" + payload + "]");
		String objectAsString = (payload instanceof String s) ? s : OBJECT_MAPPER.writeValueAsString(payload);
		JsonNode json = OBJECT_MAPPER.readTree(objectAsString);
		return json.get("bookName") != null;
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

		CountDownLatch latch = new CountDownLatch(1);

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
