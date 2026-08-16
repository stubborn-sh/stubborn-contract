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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.internal.BodyMatchers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Additional mutation-focused tests for {@link JsonPathMatcherUtils}' empty-container
 * cleanup: the recursive {@code ..} path handling ({@code rootContainsOnlyEmpty}), nested
 * non-empty siblings, and root-array collapse.
 */
@SuppressWarnings("unchecked")
class JsonPathMatcherUtilsCleanup2MutationTests {

	private static Map<String, Object> map(Object... kv) {
		Map<String, Object> m = new LinkedHashMap<>();
		for (int i = 0; i < kv.length; i += 2) {
			m.put((String) kv[i], kv[i + 1]);
		}
		return m;
	}

	@Test
	void object_with_non_empty_sibling_map_survives_cleanup() {
		Map<String, Object> json = map("a", map("keep", map("x", 1), "del", "v"));

		BodyMatchers matchers = new BodyMatchers();
		matchers.jsonPath("$.a.del", matchers.byRegex("v"));

		Map<String, Object> result = (Map<String, Object>) JsonPathMatcherUtils.removeMatchingJsonPaths(json, matchers);
		Map<String, Object> a = (Map<String, Object>) result.get("a");
		assertThat(a).doesNotContainKey("del");
		assertThat((Map<String, Object>) a.get("keep")).containsEntry("x", 1);
	}

	@Test
	void recursive_double_dot_on_map_root_leaves_empty_object_shells() {
		// the root is a Map (not Iterable) so rootContainsOnlyEmpty short-circuits to
		// false
		Map<String, Object> json = map("a", map("del", 1), "b", map("del", 2));

		BodyMatchers matchers = new BodyMatchers();
		matchers.jsonPath("$..del", matchers.byRegex("\\d+"));

		Map<String, Object> result = (Map<String, Object>) JsonPathMatcherUtils.removeMatchingJsonPaths(json, matchers);
		assertThat(result).containsOnlyKeys("a", "b");
		assertThat((Map<String, Object>) result.get("a")).isEmpty();
		assertThat((Map<String, Object>) result.get("b")).isEmpty();
	}

	@Test
	void recursive_double_dot_on_list_root_collapses_when_all_become_empty() {
		List<Map<String, Object>> json = new ArrayList<>();
		json.add(map("del", 1));
		json.add(map("del", 2));

		BodyMatchers matchers = new BodyMatchers();
		matchers.jsonPath("$..del", matchers.byRegex("\\d+"));

		List<Object> result = (List<Object>) JsonPathMatcherUtils.removeMatchingJsonPaths(json, matchers);
		assertThat(result).isEmpty();
	}

	@Test
	void recursive_double_dot_on_list_root_keeps_when_a_sibling_is_not_empty() {
		List<Map<String, Object>> json = new ArrayList<>();
		json.add(map("del", 1));
		json.add(map("keep", 9));

		BodyMatchers matchers = new BodyMatchers();
		matchers.jsonPath("$..del", matchers.byRegex("\\d+"));

		List<Object> result = (List<Object>) JsonPathMatcherUtils.removeMatchingJsonPaths(json, matchers);
		assertThat(result).anySatisfy((e) -> assertThat((Map<String, Object>) e).containsEntry("keep", 9));
	}

	@Test
	void root_array_of_emptied_objects_is_collapsed() {
		List<Map<String, Object>> json = new ArrayList<>();
		json.add(map("del", 1));

		BodyMatchers matchers = new BodyMatchers();
		matchers.jsonPath("$[0].del", matchers.byRegex("\\d+"));

		List<Object> result = (List<Object>) JsonPathMatcherUtils.removeMatchingJsonPaths(json, matchers);
		assertThat(result).isEmpty();
	}

	@Test
	void object_whose_map_value_holds_a_non_empty_list_survives() {
		Map<String, Object> json = map("a", map("keep", List.of(1, 2), "del", "v"));

		BodyMatchers matchers = new BodyMatchers();
		matchers.jsonPath("$.a.del", matchers.byRegex("v"));

		Map<String, Object> result = (Map<String, Object>) JsonPathMatcherUtils.removeMatchingJsonPaths(json, matchers);
		Map<String, Object> a = (Map<String, Object>) result.get("a");
		assertThat((List<Object>) a.get("keep")).containsExactly(1, 2);
	}

}
