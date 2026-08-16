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
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * The one authoritative place that decides how a contract payload crosses the wire as
 * bytes, so every Spring-free messaging building block (Kafka, RabbitMQ, JMS) treats text
 * and binary payloads identically — the same convention on which cross-broker parity
 * rests.
 *
 * <p>
 * A payload is either <strong>text</strong> (a JSON/XML/plain-text {@link String}, the
 * usual contract body the generator compares as text) or <strong>binary</strong> (a
 * {@code byte[]}, for example an Avro or Protobuf body compared byte-for-byte). The
 * {@code contentType} header carries the distinction across the broker: a binary payload
 * travels as {@value #BINARY_CONTENT_TYPE}, a text payload as {@value #JSON_CONTENT_TYPE}
 * (the default when a contract does not specify one). On receive, a building block reads
 * that header back to decide whether to surface a {@code byte[]} or a {@code String}, so
 * {@code send(bytes)} round-trips to {@code byte[]} and {@code send(text)} to
 * {@code String}.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public final class MessagePayloads {

	/** The header key carrying the payload's content type across every transport. */
	public static final String CONTENT_TYPE_HEADER = "contentType";

	/** The content type stamped on (and detected for) a binary {@code byte[]} payload. */
	public static final String BINARY_CONTENT_TYPE = "application/octet-stream";

	/** The default content type for a text payload when a contract specifies none. */
	public static final String JSON_CONTENT_TYPE = "application/json";

	private MessagePayloads() {
	}

	/**
	 * Serializes a payload to the bytes that travel on the wire: a {@code byte[]} is sent
	 * verbatim (binary), anything else is its {@link Object#toString()} in UTF-8 (text).
	 * A {@code null} payload becomes an empty array.
	 * @param payload the payload to serialize (may be {@code null})
	 * @return the wire bytes, never {@code null}
	 */
	public static byte[] toByteArray(@Nullable Object payload) {
		if (payload == null) {
			return new byte[0];
		}
		if (payload instanceof byte[] bytes) {
			return bytes;
		}
		return payload.toString().getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Returns the content type to stamp on a payload when a contract specifies none:
	 * {@value #BINARY_CONTENT_TYPE} for a {@code byte[]}, otherwise
	 * {@value #JSON_CONTENT_TYPE}.
	 * @param payload the payload (may be {@code null})
	 * @return the default content type
	 */
	public static String defaultContentType(@Nullable Object payload) {
		return (payload instanceof byte[]) ? BINARY_CONTENT_TYPE : JSON_CONTENT_TYPE;
	}

	/**
	 * Tells whether a {@code contentType} header value marks a binary payload (equals
	 * {@value #BINARY_CONTENT_TYPE}, ignoring case and surrounding whitespace).
	 * @param contentType the {@code contentType} header value (may be {@code null})
	 * @return {@code true} if the payload should be surfaced as {@code byte[]}
	 */
	public static boolean isBinaryContentType(@Nullable Object contentType) {
		return contentType != null && BINARY_CONTENT_TYPE.equalsIgnoreCase(contentType.toString().trim());
	}

	/**
	 * Reconstructs a received payload from its wire bytes, mirroring
	 * {@link #toByteArray}: the bytes are surfaced verbatim as a {@code byte[]} when
	 * {@code headers} mark the payload binary (see {@link #isBinaryContentType}),
	 * otherwise decoded as a UTF-8 {@link String}. This is what makes
	 * {@code send(byte[])} round-trip to {@code byte[]} and {@code send(String)} to
	 * {@code String}.
	 * @param body the received wire bytes
	 * @param headers the received headers (their {@code contentType} decides the form)
	 * @return the payload as {@code byte[]} (binary) or {@code String} (text)
	 */
	public static Object fromWire(byte[] body, Map<String, Object> headers) {
		if (isBinaryContentType(headers.get(CONTENT_TYPE_HEADER))) {
			return body;
		}
		return new String(body, StandardCharsets.UTF_8);
	}

}
