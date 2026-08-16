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

package sh.stubborn.contract.verifier.messaging.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.verifier.messaging.util.ContractVerifierMessagingUtil.ContractVerifierHeaders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link ContractVerifierMessagingUtil}.
 */
class ContractVerifierMessagingUtilMutationTests {

	@Test
	void headers_builder_is_fluent_and_stores() {
		ContractVerifierHeaders headers = ContractVerifierMessagingUtil.headers();
		ContractVerifierHeaders returned = headers.header("k", "v");
		assertThat(returned).isSameAs(headers);
		assertThat(headers).containsEntry("k", "v");
		assertThat(headers.get("k")).isEqualTo("v");
		assertThat(headers.size()).isEqualTo(1);
		assertThat(headers.isEmpty()).isFalse();
		assertThat(headers.containsKey("k")).isTrue();
		assertThat(headers.containsValue("v")).isTrue();
		assertThat(headers.keySet()).containsExactly("k");
		assertThat(headers.values()).containsExactly("v");
		assertThat(headers.entrySet()).hasSize(1);
	}

	@Test
	void headers_remove_put_clear_putAll() {
		ContractVerifierHeaders headers = ContractVerifierMessagingUtil.headers();
		assertThat(headers.put("a", "1")).isNull();
		Map<String, Object> other = new HashMap<>();
		other.put("b", "2");
		headers.putAll(other);
		assertThat(headers).containsEntry("a", "1").containsEntry("b", "2");
		assertThat(headers.remove("a")).isEqualTo("1");
		assertThat(headers.containsKey("a")).isFalse();
		headers.clear();
		assertThat(headers.isEmpty()).isTrue();
	}

	@Test
	void headers_negative_lookups_and_put_returns_previous() {
		ContractVerifierHeaders headers = ContractVerifierMessagingUtil.headers();
		headers.header("k", "v");
		assertThat(headers.containsKey("absent")).isFalse();
		assertThat(headers.containsValue("absent")).isFalse();
		assertThat(headers.put("k", "w")).isEqualTo("v");
		assertThat(headers).isNotEqualTo(new HashMap<>());
	}

	@Test
	void headers_equals_and_hashCode_match_delegate() {
		ContractVerifierHeaders headers = ContractVerifierMessagingUtil.headers();
		headers.header("k", "v");
		Map<String, Object> equivalent = new HashMap<>();
		equivalent.put("k", "v");
		assertThat(headers).isEqualTo(equivalent);
		assertThat(headers.hashCode()).isEqualTo(equivalent.hashCode());
	}

}
