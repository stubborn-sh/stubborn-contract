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

package sh.stubborn.contract.verifier.messaging;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link MessagePayloads}.
 */
class MessagePayloadsMutationTests {

	@Test
	void toByteArray_null_is_empty() {
		assertThat(MessagePayloads.toByteArray(null)).isEmpty();
	}

	@Test
	void toByteArray_byte_array_verbatim() {
		byte[] in = { 1, 2, 3 };
		assertThat(MessagePayloads.toByteArray(in)).isSameAs(in);
	}

	@Test
	void toByteArray_text_is_utf8() {
		assertThat(MessagePayloads.toByteArray("abc")).containsExactly(97, 98, 99);
	}

	@Test
	void defaultContentType_binary_for_bytes() {
		assertThat(MessagePayloads.defaultContentType(new byte[] { 1 })).isEqualTo(MessagePayloads.BINARY_CONTENT_TYPE);
	}

	@Test
	void defaultContentType_json_for_text() {
		assertThat(MessagePayloads.defaultContentType("x")).isEqualTo(MessagePayloads.JSON_CONTENT_TYPE);
	}

	@Test
	void defaultContentType_json_for_null() {
		assertThat(MessagePayloads.defaultContentType(null)).isEqualTo(MessagePayloads.JSON_CONTENT_TYPE);
	}

	@Test
	void isBinaryContentType_null_false() {
		assertThat(MessagePayloads.isBinaryContentType(null)).isFalse();
	}

	@Test
	void isBinaryContentType_exact_true() {
		assertThat(MessagePayloads.isBinaryContentType("application/octet-stream")).isTrue();
	}

	@Test
	void isBinaryContentType_case_and_whitespace_insensitive() {
		assertThat(MessagePayloads.isBinaryContentType("  APPLICATION/OCTET-STREAM  ")).isTrue();
	}

	@Test
	void isBinaryContentType_other_false() {
		assertThat(MessagePayloads.isBinaryContentType("application/json")).isFalse();
	}

	@Test
	void fromWire_binary_returns_bytes() {
		byte[] body = { 9, 8, 7 };
		Map<String, Object> headers = new HashMap<>();
		headers.put(MessagePayloads.CONTENT_TYPE_HEADER, MessagePayloads.BINARY_CONTENT_TYPE);
		Object result = MessagePayloads.fromWire(body, headers);
		assertThat(result).isInstanceOf(byte[].class);
		assertThat((byte[]) result).containsExactly(9, 8, 7);
	}

	@Test
	void fromWire_text_returns_string() {
		byte[] body = "hello".getBytes(StandardCharsets.UTF_8);
		Map<String, Object> headers = new HashMap<>();
		headers.put(MessagePayloads.CONTENT_TYPE_HEADER, MessagePayloads.JSON_CONTENT_TYPE);
		Object result = MessagePayloads.fromWire(body, headers);
		assertThat(result).isEqualTo("hello");
	}

	@Test
	void fromWire_missing_header_returns_string() {
		byte[] body = "world".getBytes(StandardCharsets.UTF_8);
		Object result = MessagePayloads.fromWire(body, new HashMap<>());
		assertThat(result).isEqualTo("world");
	}

	@Test
	void constants_have_expected_values() {
		assertThat(MessagePayloads.CONTENT_TYPE_HEADER).isEqualTo("contentType");
		assertThat(MessagePayloads.BINARY_CONTENT_TYPE).isEqualTo("application/octet-stream");
		assertThat(MessagePayloads.JSON_CONTENT_TYPE).isEqualTo("application/json");
	}

}
