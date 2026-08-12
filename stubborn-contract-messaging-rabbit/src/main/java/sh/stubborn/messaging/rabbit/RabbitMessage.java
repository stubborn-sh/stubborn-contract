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

import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.verifier.messaging.ContractMessage;

/**
 * A minimal, Spring-free carrier for a RabbitMQ (AMQP) message's payload and headers.
 *
 * <p>
 * It is the message type {@code M} of the Spring-free
 * {@link StubbornRabbitMessageVerifier} (both
 * {@code MessageVerifierSender<RabbitMessage>} and
 * {@code MessageVerifierReceiver<RabbitMessage>}), deliberately independent of
 * {@code org.springframework.amqp.core.Message} so the core messaging building block
 * carries no Spring dependency. As a {@link ContractMessage}, its payload and headers are
 * preserved automatically when {@code ContractVerifierMessaging} converts a received
 * message to the generator's {@code ContractVerifierMessage} — no per-transport helper
 * required.
 *
 * <p>
 * AMQP-specific attributes (a routing key, a correlation id) are carried as headers so
 * this type honours {@link ContractMessage} identically to the Kafka and JMS building
 * blocks.
 *
 * @author Marcin Grzejszczak
 */
public final class RabbitMessage implements ContractMessage {

	private final @Nullable Object payload;

	private final Map<String, Object> headers;

	/**
	 * Creates a message with the given payload and headers.
	 * @param payload the message body (may be {@code null})
	 * @param headers the message headers (a {@code null} map is treated as empty);
	 * defensively copied
	 */
	public RabbitMessage(@Nullable Object payload, @Nullable Map<String, Object> headers) {
		this.payload = payload;
		this.headers = (headers != null) ? new LinkedHashMap<>(headers) : new LinkedHashMap<>();
	}

	/**
	 * Returns the message payload.
	 * @return the payload, possibly {@code null}
	 */
	@Override
	public @Nullable Object getPayload() {
		return this.payload;
	}

	/**
	 * Returns the (defensively copied) message headers.
	 * @return the headers, never {@code null}
	 */
	@Override
	public Map<String, Object> getHeaders() {
		return this.headers;
	}

	@Override
	public String toString() {
		return "RabbitMessage{payload=" + this.payload + ", headers=" + this.headers + '}';
	}

}
