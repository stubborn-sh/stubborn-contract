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

package sh.stubborn.contract.stubrunner.messaging.jms;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.condition.OS;
import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
@SpringBootTest(classes = JmsStubRunnerSpec.Config.class, properties = "debug=true")
@AutoConfigureStubRunner
@DisabledOnOs(OS.WINDOWS)
@EnabledForJreRange(max = JRE.JAVA_22)
class JmsStubRunnerSpec {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Autowired
	StubFinder stubFinder;

	@Autowired
	JmsTemplate jmsTemplate;

	@AfterEach
	void cleanup() {
		// ensure that message were taken from the queue
		this.jmsTemplate.receive("output");
		this.jmsTemplate.receive("input");
	}

	@Test
	void shouldTriggerAMessageByLabel() throws Exception {
		// tag::client_trigger[]
		this.stubFinder.trigger("return_book_1");
		// end::client_trigger[]

		// tag::client_trigger_receive[]
		TextMessage receivedMessage = (TextMessage) this.jmsTemplate.receive("output");
		// end::client_trigger_receive[]

		// tag::client_trigger_message[]
		then(receivedMessage).isNotNull();
		Objects.requireNonNull(receivedMessage);
		then(assertThatBodyContainsBookNameFoo(receivedMessage.getText())).isTrue();
		then(receivedMessage.getStringProperty("BOOKNAME")).isEqualTo("foo");
		// end::client_trigger_message[]
	}

	@Test
	void shouldTriggerALabelForTheExistingGroupIdAndArtifactId() throws Exception {
		// tag::trigger_group_artifact[]
		this.stubFinder.trigger("my:stubs", "return_book_1");
		// end::trigger_group_artifact[]

		TextMessage receivedMessage = (TextMessage) this.jmsTemplate.receive("output");

		then(receivedMessage).isNotNull();
		Objects.requireNonNull(receivedMessage);
		then(assertThatBodyContainsBookNameFoo(receivedMessage.getText())).isTrue();
		then(receivedMessage.getStringProperty("BOOKNAME")).isEqualTo("foo");
	}

	@Test
	void shouldTriggerALabelForTheExistingArtifactId() throws Exception {
		// tag::trigger_artifact[]
		this.stubFinder.trigger("stubs", "return_book_1");
		// end::trigger_artifact[]

		TextMessage receivedMessage = (TextMessage) this.jmsTemplate.receive("output");

		then(receivedMessage).isNotNull();
		Objects.requireNonNull(receivedMessage);
		then(assertThatBodyContainsBookNameFoo(receivedMessage.getText())).isTrue();
		then(receivedMessage.getStringProperty("BOOKNAME")).isEqualTo("foo");
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
	void shouldTriggerMessagesByRunningAllTriggers() throws Exception {
		// tag::trigger_all[]
		this.stubFinder.trigger();
		// end::trigger_all[]

		TextMessage receivedMessage = (TextMessage) this.jmsTemplate.receive("output");

		then(receivedMessage).isNotNull();
		Objects.requireNonNull(receivedMessage);
		then(assertThatBodyContainsBookNameFoo(receivedMessage.getText())).isTrue();
		then(receivedMessage.getStringProperty("BOOKNAME")).isEqualTo("foo");
	}

	@Test
	void shouldTriggerALabelWithNoOutputMessage() {
		// tag::trigger_no_output[]
		this.jmsTemplate.convertAndSend("delete", new BookReturned("foo"), new MessagePostProcessor() {
			@Override
			public Message postProcessMessage(Message message) throws JMSException {
				message.setStringProperty("sample", "header");
				return message;
			}
		});
		// end::trigger_no_output[]
	}

	@Test
	void shouldNotTriggerAMessageThatDoesNotMatchInput() {
		this.jmsTemplate.convertAndSend("input", new BookReturned("notmatching"), new MessagePostProcessor() {
			@Override
			public Message postProcessMessage(Message message) throws JMSException {
				message.setStringProperty("wrong", "header");
				return message;
			}
		});

		TextMessage receivedMessage = (TextMessage) this.jmsTemplate.receive("output");

		then(receivedMessage).isNull();
	}

	private boolean assertThatBodyContainsBookNameFoo(Object payload) throws Exception {
		String objectAsString = (payload instanceof String s) ? s : OBJECT_MAPPER.writeValueAsString(payload);
		JsonNode json = OBJECT_MAPPER.readTree(objectAsString);
		return "foo".equals(json.get("bookName").asText());
	}

	@Configuration
	@ComponentScan
	@EnableAutoConfiguration
	@EnableJms
	static class Config {

	}

}
