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

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * A minimal, transport-neutral view of a message: a payload and headers. It is the shared
 * contract implemented by the Spring-free {@code MessageVerifier} message types of every
 * transport building block (Kafka, RabbitMQ, JMS, …), so a single code path can carry any
 * of them.
 *
 * <p>
 * When a {@link MessageVerifierReceiver} returns a value that implements this interface,
 * {@code ContractVerifierMessaging} converts it to a {@code ContractVerifierMessage}
 * preserving both payload and headers — the per-transport conversion helper is no longer
 * needed.
 *
 * <p>
 * The surface is deliberately limited to payload and headers: that is the
 * transport-neutral denominator on which contract-level parity across brokers (and the
 * in-memory fast lane) rests. Transport-specific attributes (a Kafka key, a RabbitMQ
 * routing key, a JMS correlation id) are carried as headers so every implementation
 * honours the interface identically.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public interface ContractMessage {

	/**
	 * Returns the message payload.
	 * @return the payload, possibly {@code null}
	 */
	@Nullable Object getPayload();

	/**
	 * Returns the message headers.
	 * @return the headers, never {@code null}
	 */
	Map<String, Object> getHeaders();

}
