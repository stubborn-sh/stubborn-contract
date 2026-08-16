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
import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessageMetadata.MessageType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link ContractVerifierMessageMetadata}.
 */
class ContractVerifierMessageMetadataMutationTests {

	@Test
	void constructor_sets_message_type() {
		ContractVerifierMessageMetadata metadata = new ContractVerifierMessageMetadata(MessageType.INPUT);
		assertThat(metadata.getMessageType()).isEqualTo(MessageType.INPUT);
	}

	@Test
	void default_constructor_has_null_type() {
		assertThat(new ContractVerifierMessageMetadata().getMessageType()).isNull();
	}

	@Test
	void setter_updates_type() {
		ContractVerifierMessageMetadata metadata = new ContractVerifierMessageMetadata();
		metadata.setMessageType(MessageType.OUTPUT);
		assertThat(metadata.getMessageType()).isEqualTo(MessageType.OUTPUT);
	}

	@Test
	void key_and_description() {
		ContractVerifierMessageMetadata metadata = new ContractVerifierMessageMetadata();
		assertThat(metadata.key()).isEqualTo("verifierMessage");
		assertThat(ContractVerifierMessageMetadata.METADATA_KEY).isEqualTo("verifierMessage");
		assertThat(metadata.description())
			.isEqualTo("Internal metadata entries used by the framework, related to messaging");
	}

	@Test
	void fromMetadata_returns_stored_metadata() {
		ContractVerifierMessageMetadata stored = new ContractVerifierMessageMetadata(MessageType.SETUP);
		Map<String, Object> map = new HashMap<>();
		map.put(ContractVerifierMessageMetadata.METADATA_KEY, stored);
		ContractVerifierMessageMetadata result = ContractVerifierMessageMetadata.fromMetadata(map);
		assertThat(result.getMessageType()).isEqualTo(MessageType.SETUP);
	}

	@Test
	void fromMetadata_returns_default_when_absent() {
		ContractVerifierMessageMetadata result = ContractVerifierMessageMetadata.fromMetadata(new HashMap<>());
		assertThat(result.getMessageType()).isNull();
	}

}
