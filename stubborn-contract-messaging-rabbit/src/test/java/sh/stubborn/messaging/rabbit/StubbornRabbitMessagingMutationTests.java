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

package sh.stubborn.messaging.rabbit;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.LongString;
import com.rabbitmq.client.impl.LongStringHelper;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.verifier.messaging.MessagePayloads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Mutation-focused tests for the Spring-free RabbitMQ building block that do not require
 * a running broker: the connection-factory URI parsing, the AMQP-to-{@link RabbitMessage}
 * conversion, the header-value decoding, and the outgoing {@code BasicProperties}
 * mapping. The full real-broker round-trip lives in the Testcontainers conformance suite.
 *
 * @author Marcin Grzejszczak
 */
@SuppressWarnings("NullAway")
class StubbornRabbitMessagingMutationTests {

	private static GetResponse getResponse(byte[] body, Map<String, Object> headers) {
		Envelope envelope = new Envelope(1L, false, "exchange", "rk");
		AMQP.BasicProperties props = new AMQP.BasicProperties.Builder().headers(headers).build();
		return new GetResponse(envelope, props, body, 0);
	}

	private static RabbitMessage toMessage(GetResponse response) throws Exception {
		Method m = StubbornRabbitMessageVerifierReceiver.class.getDeclaredMethod("toMessage", GetResponse.class);
		m.setAccessible(true);
		return (RabbitMessage) m.invoke(null, response);
	}

	private static Object decodeHeaderValue(Object value) throws Exception {
		Method m = StubbornRabbitMessageVerifierReceiver.class.getDeclaredMethod("decodeHeaderValue", Object.class);
		m.setAccessible(true);
		return m.invoke(null, value);
	}

	private static AMQP.BasicProperties propertiesFor(Map<String, Object> headers) throws Exception {
		Method m = StubbornRabbitMessageVerifierSender.class.getDeclaredMethod("propertiesFor", Map.class);
		m.setAccessible(true);
		return (AMQP.BasicProperties) m.invoke(null, headers);
	}

	@Test
	void factoryForParsesValidUri() {
		ConnectionFactory factory = AbstractStubbornRabbitVerifier.factoryFor("amqp://guest:guest@example.org:5673");
		assertThat(factory.getHost()).isEqualTo("example.org");
		assertThat(factory.getPort()).isEqualTo(5673);
	}

	@Test
	void factoryForRejectsInvalidUri() {
		assertThatThrownBy(() -> AbstractStubbornRabbitVerifier.factoryFor("not a uri"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Invalid RabbitMQ AMQP URI");
	}

	@Test
	void toMessageDecodesTextPayloadAndDefaultsJsonContentType() throws Exception {
		Map<String, Object> headers = new LinkedHashMap<>();
		headers.put("trace", LongStringHelper.asLongString("abc"));
		RabbitMessage message = toMessage(getResponse("{\"a\":1}".getBytes(StandardCharsets.UTF_8), headers));
		assertThat(message.getPayload()).isInstanceOf(String.class).isEqualTo("{\"a\":1}");
		assertThat(message.getHeaders()).containsEntry("trace", "abc")
			.containsEntry(MessagePayloads.CONTENT_TYPE_HEADER, MessagePayloads.JSON_CONTENT_TYPE);
	}

	@Test
	void toMessageKeepsBinaryPayloadWhenContentTypeBinary() throws Exception {
		Map<String, Object> headers = new LinkedHashMap<>();
		headers.put(MessagePayloads.CONTENT_TYPE_HEADER, MessagePayloads.BINARY_CONTENT_TYPE);
		byte[] body = { 5, 6, 7, (byte) 0x80 };
		RabbitMessage message = toMessage(getResponse(body, headers));
		assertThat(message.getPayload()).isInstanceOf(byte[].class);
		assertThat((byte[]) message.getPayload()).containsExactly(5, 6, 7, 0x80);
		assertThat(message.getHeaders()).containsEntry(MessagePayloads.CONTENT_TYPE_HEADER,
				MessagePayloads.BINARY_CONTENT_TYPE);
	}

	@Test
	void toMessageWithoutAmqpHeadersStillDefaultsContentType() throws Exception {
		Envelope envelope = new Envelope(1L, false, "exchange", "rk");
		AMQP.BasicProperties props = new AMQP.BasicProperties.Builder().build();
		GetResponse response = new GetResponse(envelope, props, "text".getBytes(StandardCharsets.UTF_8), 0);
		RabbitMessage message = toMessage(response);
		assertThat(message.getPayload()).isEqualTo("text");
		assertThat(message.getHeaders())
			.containsExactly(Map.entry(MessagePayloads.CONTENT_TYPE_HEADER, MessagePayloads.JSON_CONTENT_TYPE));
	}

	@Test
	void decodeHeaderValueConvertsLongString() throws Exception {
		LongString longString = LongStringHelper.asLongString("hello");
		assertThat(decodeHeaderValue(longString)).isEqualTo("hello");
	}

	@Test
	void decodeHeaderValueConvertsByteArrayAsUtf8() throws Exception {
		Object decoded = decodeHeaderValue("world".getBytes(StandardCharsets.UTF_8));
		assertThat(decoded).isEqualTo("world");
	}

	@Test
	void decodeHeaderValuePassesThroughOtherTypes() throws Exception {
		assertThat(decodeHeaderValue(42)).isEqualTo(42);
	}

	@Test
	void propertiesForCarriesHeadersAndContentTypeAndSkipsNulls() throws Exception {
		Map<String, Object> headers = new LinkedHashMap<>();
		headers.put("present", "yes");
		headers.put("absent", null);
		headers.put(MessagePayloads.CONTENT_TYPE_HEADER, MessagePayloads.JSON_CONTENT_TYPE);
		AMQP.BasicProperties props = propertiesFor(headers);
		assertThat(props.getContentType()).isEqualTo(MessagePayloads.JSON_CONTENT_TYPE);
		assertThat(props.getHeaders()).containsEntry("present", "yes")
			.containsEntry(MessagePayloads.CONTENT_TYPE_HEADER, MessagePayloads.JSON_CONTENT_TYPE)
			.doesNotContainKey("absent");
	}

	@Test
	void propertiesForLeavesContentTypeUnsetWhenAbsent() throws Exception {
		Map<String, Object> headers = new LinkedHashMap<>();
		headers.put("only", "value");
		AMQP.BasicProperties props = propertiesFor(headers);
		assertThat(props.getContentType()).isNull();
		assertThat(props.getHeaders()).containsExactly(Map.entry("only", "value"));
	}

}
