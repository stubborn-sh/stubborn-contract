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
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link MetadataUtil}.
 */
class MetadataUtilMutationTests {

	public static class Bean {

		private String name;

		private int count;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getCount() {
			return this.count;
		}

		public void setCount(int count) {
			this.count = count;
		}

	}

	@Test
	void fromMetadata_returns_same_object_when_metadata_null() {
		Bean target = new Bean();
		target.setName("orig");
		assertThat(MetadataUtil.fromMetadata(null, "key", target)).isSameAs(target);
	}

	@Test
	void fromMetadata_returns_same_object_when_key_missing() {
		Bean target = new Bean();
		target.setName("orig");
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("other", "x");
		assertThat(MetadataUtil.fromMetadata(metadata, "key", target)).isSameAs(target);
	}

	@Test
	void fromMetadata_merges_when_key_present() {
		Bean target = new Bean();
		target.setName("orig");
		target.setCount(5);
		Bean patch = new Bean();
		patch.setCount(99);
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("key", patch);
		Bean merged = MetadataUtil.fromMetadata(metadata, "key", target);
		assertThat(merged.getCount()).isEqualTo(99);
		assertThat(merged.getName()).isEqualTo("orig");
	}

	@Test
	void merge_returns_same_object_when_patch_null() {
		Bean target = new Bean();
		assertThat(MetadataUtil.merge(target, null)).isSameAs(target);
	}

	@Test
	void merge_applies_non_default_and_skips_default_values() {
		Bean target = new Bean();
		target.setName("original");
		target.setCount(5);
		Bean patch = new Bean();
		// name stays default (null) -> filtered out -> target keeps "original"
		patch.setCount(99);
		Bean merged = MetadataUtil.merge(target, patch);
		assertThat(merged.getName()).isEqualTo("original");
		assertThat(merged.getCount()).isEqualTo(99);
	}

	@Test
	void merge_with_map_patch_updates_map_target() {
		Map<String, Object> target = new HashMap<>();
		target.put("a", "1");
		Map<String, Object> patch = new HashMap<>();
		patch.put("b", "2");
		@SuppressWarnings("unchecked")
		Map<String, Object> merged = (Map<String, Object>) MetadataUtil.merge(target, patch);
		assertThat(merged).containsEntry("a", "1").containsEntry("b", "2");
	}

	@Test
	void metadataMap_entry_is_fluent_and_stores() {
		MetadataUtil.MetadataMap map = MetadataUtil.map();
		MetadataUtil.MetadataMap returned = map.entry("k", "v");
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
	void metadataMap_remove_put_clear_putAll() {
		MetadataUtil.MetadataMap map = MetadataUtil.map();
		assertThat(map.put("a", "1")).isNull();
		Map<String, Object> other = new HashMap<>();
		other.put("b", "2");
		map.putAll(other);
		assertThat(map).containsEntry("a", "1").containsEntry("b", "2");
		assertThat(map.remove("a")).isEqualTo("1");
		assertThat(map.containsKey("a")).isFalse();
		map.clear();
		assertThat(map.isEmpty()).isTrue();
	}

	@Test
	void metadataMap_negative_lookups_and_put_returns_previous() {
		MetadataUtil.MetadataMap map = MetadataUtil.map();
		map.entry("k", "v");
		assertThat(map.containsKey("absent")).isFalse();
		assertThat(map.containsValue("absent")).isFalse();
		assertThat(map.put("k", "w")).isEqualTo("v");
		assertThat(map).isNotEqualTo(new java.util.HashMap<>());
	}

	@Test
	void metadataMap_equals_and_hashCode_match_delegate() {
		MetadataUtil.MetadataMap map = MetadataUtil.map();
		map.entry("k", "v");
		Map<String, Object> equivalent = new HashMap<>();
		equivalent.put("k", "v");
		assertThat(map).isEqualTo(equivalent);
		assertThat(map.hashCode()).isEqualTo(equivalent.hashCode());
	}

}
