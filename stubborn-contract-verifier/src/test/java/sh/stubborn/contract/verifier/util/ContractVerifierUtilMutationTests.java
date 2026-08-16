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

package sh.stubborn.contract.verifier.util;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Mutation-focused tests for {@link ContractVerifierUtil}.
 */
class ContractVerifierUtilMutationTests {

	@Test
	void fileToBytes_reads_existing_resource() {
		byte[] bytes = ContractVerifierUtil.fileToBytes(this, "/yml/contract.yml");
		assertThat(bytes).isNotEmpty();
		assertThat(new String(bytes)).contains("request");
	}

	@Test
	void fileToBytes_throws_for_missing_resource() {
		assertThatThrownBy(() -> ContractVerifierUtil.fileToBytes(this, "/yml/does-not-exist.yml"))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void contract_reads_yaml_contract() {
		Object contract = ContractVerifierUtil.contract(this, "/yml/contract.yml");
		assertThat(contract).isNotNull();
	}

	@Test
	void map_is_fluent_and_stores() {
		ContractVerifierUtil.ContractVerifierMap map = ContractVerifierUtil.map();
		ContractVerifierUtil.ContractVerifierMap returned = map.entry("k", "v");
		assertThat(returned).isSameAs(map);
		assertThat(map).containsEntry("k", "v");
		assertThat(map.get("k")).isEqualTo("v");
		assertThat(map.size()).isEqualTo(1);
		assertThat(map.isEmpty()).isFalse();
		assertThat(map.containsKey("k")).isTrue();
		assertThat(map.containsValue("v")).isTrue();
		assertThat(map.keySet()).containsExactly("k");
		assertThat(map.values()).containsExactly("v");
		assertThat(map.entrySet()).hasSize(1);
	}

	@Test
	void map_negative_lookups_and_put_returns_previous() {
		ContractVerifierUtil.ContractVerifierMap map = ContractVerifierUtil.map();
		map.entry("k", "v");
		assertThat(map.containsKey("absent")).isFalse();
		assertThat(map.containsValue("absent")).isFalse();
		assertThat(map.put("k", "w")).isEqualTo("v");
		assertThat(map).isNotEqualTo(new HashMap<>());
	}

	@Test
	void map_remove_clear_and_equals() {
		ContractVerifierUtil.ContractVerifierMap map = ContractVerifierUtil.map();
		map.entry("a", "1").entry("b", "2");
		assertThat(map.remove("a")).isEqualTo("1");
		assertThat(map.containsKey("a")).isFalse();
		HashMap<Object, Object> equivalent = new HashMap<>();
		equivalent.put("b", "2");
		assertThat(map).isEqualTo(equivalent);
		assertThat(map.hashCode()).isEqualTo(equivalent.hashCode());
		map.clear();
		assertThat(map.isEmpty()).isTrue();
	}

}
