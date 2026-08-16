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
 * Mutation-focused tests for {@link JsonPathMatcherUtils}' empty-container cleanup
 * ({@code removeTrailingContainers} / {@code containsOnlyEmptyElements}).
 */
@SuppressWarnings("unchecked")
class JsonPathMatcherUtilsCleanupMutationTests {

	private static Map<String, Object> obj(String key, Object value) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put(key, value);
		return map;
	}

	@Test
	void array_becoming_all_empty_is_removed() {
		List<Map<String, Object>> items = new ArrayList<>();
		items.add(obj("id", 1));
		items.add(obj("id", 2));
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("items", items);

		BodyMatchers matchers = new BodyMatchers();
		matchers.jsonPath("$.items[0].id", matchers.byRegex("\\d+"));
		matchers.jsonPath("$.items[1].id", matchers.byRegex("\\d+"));

		Map<String, Object> result = (Map<String, Object>) JsonPathMatcherUtils.removeMatchingJsonPaths(json, matchers);
		// every element became empty so the whole "items" container is removed
		assertThat(result).doesNotContainKey("items");
	}

	@Test
	void array_with_remaining_content_is_kept() {
		List<Map<String, Object>> items = new ArrayList<>();
		items.add(obj("id", 1));
		Map<String, Object> withName = new LinkedHashMap<>();
		withName.put("id", 2);
		withName.put("name", "keep");
		items.add(withName);
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("items", items);

		BodyMatchers matchers = new BodyMatchers();
		matchers.jsonPath("$.items[0].id", matchers.byRegex("\\d+"));
		matchers.jsonPath("$.items[1].id", matchers.byRegex("\\d+"));

		Map<String, Object> result = (Map<String, Object>) JsonPathMatcherUtils.removeMatchingJsonPaths(json, matchers);
		// one element still holds "name", so the container survives
		assertThat(result).containsKey("items");
		List<Map<String, Object>> resultItems = (List<Map<String, Object>>) result.get("items");
		assertThat(resultItems).anySatisfy((m) -> assertThat(m).containsEntry("name", "keep"));
	}

	@Test
	void no_matchers_returns_clone_untouched() {
		Map<String, Object> json = obj("a", "b");
		Object result = JsonPathMatcherUtils.removeMatchingJsonPaths(json, new BodyMatchers());
		assertThat((Map<String, Object>) result).containsEntry("a", "b");
	}

}
