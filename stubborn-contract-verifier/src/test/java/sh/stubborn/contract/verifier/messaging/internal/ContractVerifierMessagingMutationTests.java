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

package sh.stubborn.contract.verifier.messaging.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.verifier.converter.YamlContract;
import sh.stubborn.contract.verifier.messaging.ContractMessage;
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver;
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link ContractVerifierMessaging}.
 */
class ContractVerifierMessagingMutationTests {

	@Test
	void send_forwards_payload_headers_and_destination() {
		CapturingSender sender = new CapturingSender();
		ContractVerifierMessaging<Object> messaging = new ContractVerifierMessaging<>(sender, new StubReceiver(null));
		Map<String, Object> headers = new HashMap<>();
		headers.put("h", "1");
		ContractVerifierMessage message = new ContractVerifierMessage("body", headers);
		messaging.send(message, "queue");
		assertThat(sender.payload).isEqualTo("body");
		assertThat(sender.headers).containsEntry("h", "1");
		assertThat(sender.destination).isEqualTo("queue");
		assertThat(sender.contract).isNull();
	}

	@Test
	void send_with_contract_sets_input_message_type() {
		CapturingSender sender = new CapturingSender();
		ContractVerifierMessaging<Object> messaging = new ContractVerifierMessaging<>(sender, new StubReceiver(null));
		YamlContract contract = new YamlContract();
		ContractVerifierMessage message = new ContractVerifierMessage("body", null);
		messaging.send(message, "queue", contract);
		assertThat(sender.contract).isSameAs(contract);
		Object metadata = contract.metadata.get(ContractVerifierMessageMetadata.METADATA_KEY);
		assertThat(metadata).isInstanceOf(ContractVerifierMessageMetadata.class);
		assertThat(((ContractVerifierMessageMetadata) metadata).getMessageType())
			.isEqualTo(ContractVerifierMessageMetadata.MessageType.INPUT);
	}

	@Test
	void receive_wraps_contract_message_payload_and_headers() {
		Map<String, Object> headers = new HashMap<>();
		headers.put("h", "1");
		SimpleContractMessage received = new SimpleContractMessage("body", headers);
		ContractVerifierMessaging<Object> messaging = new ContractVerifierMessaging<>(new CapturingSender(),
				new StubReceiver(received));
		ContractVerifierMessage result = messaging.receive("queue");
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("body");
		assertThat(result.getHeaders()).containsEntry("h", "1");
	}

	@Test
	void receive_wraps_plain_payload_with_no_headers() {
		ContractVerifierMessaging<Object> messaging = new ContractVerifierMessaging<>(new CapturingSender(),
				new StubReceiver("plain"));
		ContractVerifierMessage result = messaging.receive("queue");
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("plain");
		assertThat(result.getHeaders()).isEmpty();
	}

	@Test
	void receive_with_contract_sets_output_message_type() {
		StubReceiver receiver = new StubReceiver("plain");
		ContractVerifierMessaging<Object> messaging = new ContractVerifierMessaging<>(new CapturingSender(), receiver);
		YamlContract contract = new YamlContract();
		messaging.receive("queue", contract);
		assertThat(receiver.contract).isSameAs(contract);
		Object metadata = contract.metadata.get(ContractVerifierMessageMetadata.METADATA_KEY);
		assertThat(((ContractVerifierMessageMetadata) metadata).getMessageType())
			.isEqualTo(ContractVerifierMessageMetadata.MessageType.OUTPUT);
	}

	@Test
	void create_builds_message() {
		ContractVerifierMessaging<Object> messaging = new ContractVerifierMessaging<>(new CapturingSender(),
				new StubReceiver(null));
		Map<String, Object> headers = new HashMap<>();
		headers.put("h", "1");
		ContractVerifierMessage message = messaging.create("body", headers);
		assertThat(message.getPayload()).isEqualTo("body");
		assertThat(message.getHeaders()).containsEntry("h", "1");
	}

	private static final class CapturingSender implements MessageVerifierSender<Object> {

		@Nullable Object payload;

		@Nullable Map<String, Object> headers;

		@Nullable String destination;

		@Nullable YamlContract contract;

		@Override
		public void send(Object message, String destination, @Nullable YamlContract contract) {
			this.payload = message;
			this.destination = destination;
			this.contract = contract;
		}

		@Override
		public <T> void send(T payload, @Nullable Map<String, Object> headers, String destination,
				@Nullable YamlContract contract) {
			this.payload = payload;
			this.headers = headers;
			this.destination = destination;
			this.contract = contract;
		}

	}

	private static final class StubReceiver implements MessageVerifierReceiver<Object> {

		private final @Nullable Object toReturn;

		@Nullable String destination;

		@Nullable YamlContract contract;

		StubReceiver(@Nullable Object toReturn) {
			this.toReturn = toReturn;
		}

		@Override
		public @Nullable Object receive(String destination, long timeout, TimeUnit timeUnit,
				@Nullable YamlContract contract) {
			return receive(destination, contract);
		}

		@Override
		public @Nullable Object receive(String destination, @Nullable YamlContract contract) {
			this.destination = destination;
			this.contract = contract;
			return this.toReturn;
		}

	}

	private static final class SimpleContractMessage implements ContractMessage {

		private final Object payload;

		private final Map<String, Object> headers;

		SimpleContractMessage(Object payload, Map<String, Object> headers) {
			this.payload = payload;
			this.headers = headers;
		}

		@Override
		public Object getPayload() {
			return this.payload;
		}

		@Override
		public Map<String, Object> getHeaders() {
			return this.headers;
		}

	}

}
