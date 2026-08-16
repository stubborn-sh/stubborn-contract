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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Mutation-focused tests for {@link ContractVerifierMessage}.
 */
class ContractVerifierMessageMutationTests {

	@Test
	void constructor_copies_headers_and_payload() {
		Map<String, Object> headers = new HashMap<>();
		headers.put("h", "1");
		ContractVerifierMessage message = new ContractVerifierMessage("payload", headers);
		assertThat(message.getPayload()).isEqualTo("payload");
		assertThat(message.getHeaders()).containsEntry("h", "1");
		assertThat(message.getHeader("h")).isEqualTo("1");
	}

	@Test
	void constructor_with_null_headers_yields_empty_map() {
		ContractVerifierMessage message = new ContractVerifierMessage("payload", null);
		assertThat(message.getHeaders()).isEmpty();
	}

	@Test
	void default_constructor_and_setters() {
		ContractVerifierMessage message = new ContractVerifierMessage();
		assertThat(message.getPayload()).isNull();
		message.setPayload("body");
		assertThat(message.getPayload()).isEqualTo("body");
		Map<String, Object> headers = new HashMap<>();
		headers.put("k", "v");
		message.setHeaders(headers);
		assertThat(message.getHeaders()).containsEntry("k", "v");
		assertThat(message.getHeader("k")).isEqualTo("v");
		assertThat(message.getHeader("missing")).isNull();
	}

	@Test
	void getPayloadAsByteArray_returns_bytes() {
		byte[] payload = { 1, 2, 3 };
		ContractVerifierMessage message = new ContractVerifierMessage(payload, null);
		assertThat(message.getPayloadAsByteArray()).containsExactly(1, 2, 3);
	}

	@Test
	void getPayloadAsByteArray_throws_when_not_bytes() {
		ContractVerifierMessage message = new ContractVerifierMessage("not bytes", null);
		assertThatThrownBy(message::getPayloadAsByteArray).isInstanceOf(IllegalStateException.class);
	}

}
