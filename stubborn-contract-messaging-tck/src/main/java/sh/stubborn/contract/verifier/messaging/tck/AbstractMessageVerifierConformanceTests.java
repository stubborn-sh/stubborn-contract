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

package sh.stubborn.contract.verifier.messaging.tck;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.verifier.messaging.ContractMessage;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessage;
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A transport-neutral conformance suite (TCK) for the Spring-free messaging building
 * blocks. It asserts that a {@code MessageVerifierSender} /
 * {@code MessageVerifierReceiver} pair round-trips a message through a <strong>real
 * broker</strong> preserving payload and headers identically — both a
 * <strong>text</strong> (JSON) payload, surfaced as a {@code String}, and a
 * <strong>binary</strong> ({@code byte[]}) payload, surfaced verbatim as a {@code byte[]}
 * — so Kafka, RabbitMQ and JMS all behave the same way at the contract-verification
 * level.
 *
 * <p>
 * Each transport provides a thin subclass supplying a broker-bound sender and receiver
 * and a way to build its message type {@code M}; the assertions live here, once. Header
 * keys use only letters, digits and {@code _} — the intersection of what all three
 * transports accept (JMS message-property names must be valid identifiers), so the same
 * data is legal everywhere.
 *
 * @param <M> the transport's message type
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public abstract class AbstractMessageVerifierConformanceTests<M extends ContractMessage> {

	/** A JSON payload compared as text by the generator. */
	protected static final String JSON_PAYLOAD = "{\"id\":\"42\",\"message\":\"hello\"}";

	/**
	 * A binary payload (an Avro/Protobuf body stands in for it) deliberately containing
	 * bytes that are <em>not</em> valid UTF-8 ({@code 0xFF}, {@code 0xFE}, {@code 0x80}),
	 * so a text-only round-trip would corrupt them — the test fails unless bytes are
	 * preserved verbatim.
	 */
	protected static final byte[] BINARY_PAYLOAD = { 0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE, (byte) 0x80, 'h',
			'i' };

	/** A header key valid across Kafka, RabbitMQ and JMS (a valid JMS identifier). */
	protected static final String HEADER_KEY = "X_Custom_Header";

	/** The header value carried alongside {@link #HEADER_KEY}. */
	protected static final String HEADER_VALUE = "custom-value";

	/**
	 * Returns a fresh sender bound to a live broker. It is closed after each test if it
	 * is {@link AutoCloseable}.
	 * @return the sender under test
	 */
	protected abstract MessageVerifierSender<M> sender();

	/**
	 * Returns a fresh receiver bound to a live broker. It is closed after each test if it
	 * is {@link AutoCloseable}.
	 * @return the receiver under test
	 */
	protected abstract MessageVerifierReceiver<M> receiver();

	/**
	 * Builds the transport's message type from a payload and headers.
	 * @param payload the payload (may be {@code null})
	 * @param headers the headers
	 * @return the transport message
	 */
	protected abstract M message(@Nullable Object payload, Map<String, Object> headers);

	@Test
	void sends_and_receives_payload_and_headers() {
		String destination = uniqueDestination("conformance");
		MessageVerifierSender<M> sender = sender();
		MessageVerifierReceiver<M> receiver = receiver();
		try {
			sender.send(message(JSON_PAYLOAD, Map.of(HEADER_KEY, HEADER_VALUE)), destination, null);

			M received = Objects.requireNonNull(receiver.receive(destination, 15, TimeUnit.SECONDS, null),
					"expected a message on '" + destination + "'");

			assertThat(received.getPayload()).isEqualTo(JSON_PAYLOAD);
			assertThat(received.getHeaders()).containsEntry(HEADER_KEY, HEADER_VALUE);
		}
		finally {
			closeQuietly(sender);
			closeQuietly(receiver);
		}
	}

	@Test
	void sends_and_receives_binary_payload_and_headers() {
		String destination = uniqueDestination("conformance_bin");
		MessageVerifierSender<M> sender = sender();
		MessageVerifierReceiver<M> receiver = receiver();
		try {
			sender.send(message(BINARY_PAYLOAD, Map.of(HEADER_KEY, HEADER_VALUE)), destination, null);

			M received = Objects.requireNonNull(receiver.receive(destination, 15, TimeUnit.SECONDS, null),
					"expected a message on '" + destination + "'");

			// A binary payload must round-trip as a byte[] preserving every byte — never
			// decoded to a String (which would corrupt the non-UTF-8 bytes above).
			assertThat(received.getPayload()).isInstanceOf(byte[].class);
			assertThat((byte[]) received.getPayload()).isEqualTo(BINARY_PAYLOAD);
			assertThat(received.getHeaders()).containsEntry(HEADER_KEY, HEADER_VALUE);
			// A binary payload defaults to the octet-stream content type across all
			// transports.
			assertThat(received.getHeaders()).containsEntry("contentType", "application/octet-stream");
		}
		finally {
			closeQuietly(sender);
			closeQuietly(receiver);
		}
	}

	@Test
	void contract_verifier_messaging_preserves_payload_and_headers() {
		String destination = uniqueDestination("conformance_cvm");
		MessageVerifierSender<M> sender = sender();
		MessageVerifierReceiver<M> receiver = receiver();
		try {
			// The base ContractVerifierMessaging preserves headers because M is a
			// ContractMessage — no per-transport helper.
			ContractVerifierMessaging<M> messaging = new ContractVerifierMessaging<>(sender, receiver);

			messaging.send(messaging.create(JSON_PAYLOAD, Map.of(HEADER_KEY, HEADER_VALUE)), destination, null);

			ContractVerifierMessage received = Objects.requireNonNull(messaging.receive(destination),
					"expected a message on '" + destination + "'");

			assertThat(received.getPayload()).isEqualTo(JSON_PAYLOAD);
			assertThat(received.getHeader(HEADER_KEY)).isEqualTo(HEADER_VALUE);
		}
		finally {
			closeQuietly(sender);
			closeQuietly(receiver);
		}
	}

	@Test
	void adds_default_content_type_when_absent() {
		String destination = uniqueDestination("conformance_ct");
		MessageVerifierSender<M> sender = sender();
		MessageVerifierReceiver<M> receiver = receiver();
		try {
			sender.send(message(JSON_PAYLOAD, Map.of()), destination, null);

			M received = Objects.requireNonNull(receiver.receive(destination, 15, TimeUnit.SECONDS, null),
					"expected a message on '" + destination + "'");

			assertThat(received.getHeaders()).containsEntry("contentType", "application/json");
		}
		finally {
			closeQuietly(sender);
			closeQuietly(receiver);
		}
	}

	/**
	 * Returns a unique destination name using only characters legal for a Kafka topic, a
	 * RabbitMQ queue and a JMS queue alike (letters, digits, {@code _}).
	 * @param prefix a human-readable prefix
	 * @return a unique destination name
	 */
	protected String uniqueDestination(String prefix) {
		return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
	}

	private static void closeQuietly(Object candidate) {
		if (candidate instanceof AutoCloseable closeable) {
			try {
				closeable.close();
			}
			catch (Exception ex) {
				// best effort — the broker container/embedded server is torn down anyway
			}
		}
	}

}
