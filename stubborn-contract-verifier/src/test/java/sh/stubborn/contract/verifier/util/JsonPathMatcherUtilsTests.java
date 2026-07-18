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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.internal.BodyMatchers;
import sh.stubborn.contract.spec.internal.BodyMatcher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonPathMatcherUtils}.
 *
 * @author Marcin Grzejszczak
 * @since 5.1.0
 */
class JsonPathMatcherUtilsTests {

	@Test
	void should_read_element_from_JSON_by_path() {
		Map<String, Object> person = new LinkedHashMap<>();
		person.put("name", "John");
		person.put("age", 30);
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("person", person);

		Object name = JsonPathMatcherUtils.readElement(json, "$.person.name");
		Object age = JsonPathMatcherUtils.readElement(json, "$.person.age");

		assertThat(name).isEqualTo("John");
		assertThat(age).isEqualTo(30);
	}

	@Test
	void should_read_nested_array_element_from_JSON() {
		Map<String, Object> first = new LinkedHashMap<>();
		first.put("id", 1);
		first.put("name", "first");
		Map<String, Object> second = new LinkedHashMap<>();
		second.put("id", 2);
		second.put("name", "second");
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("items", List.of(first, second));

		Object firstId = JsonPathMatcherUtils.readElement(json, "$.items[0].id");
		Object secondName = JsonPathMatcherUtils.readElement(json, "$.items[1].name");

		assertThat(firstId).isEqualTo(1);
		assertThat(secondName).isEqualTo("second");
	}

	@Test
	void should_remove_matching_JSON_paths_from_body() {
		Map<String, Object> person = new LinkedHashMap<>();
		person.put("name", "John");
		person.put("age", 30);
		person.put("email", "john@example.com");
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("person", person);
		BodyMatchers bodyMatchers = new BodyMatchers();
		bodyMatchers.jsonPath("$.person.email", bodyMatchers.byRegex(".*"));

		@SuppressWarnings("unchecked")
		Map<String, Object> result = (Map<String, Object>) JsonPathMatcherUtils.removeMatchingJsonPaths(json,
				bodyMatchers);

		@SuppressWarnings("unchecked")
		Map<String, Object> resultPerson = (Map<String, Object>) result.get("person");
		assertThat(resultPerson.get("name")).isEqualTo("John");
		assertThat(resultPerson.get("age")).isEqualTo(30);
		assertThat(resultPerson.get("email")).isNull();
	}

	@Test
	void should_return_original_JSON_when_no_matchers_provided() {
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("name", "John");

		@SuppressWarnings("unchecked")
		Map<String, Object> result = (Map<String, Object>) JsonPathMatcherUtils.removeMatchingJsonPaths(json, null);

		assertThat(result.get("name")).isEqualTo("John");
	}

	@Test
	void should_return_original_JSON_when_matchers_have_no_entries() {
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("name", "John");
		BodyMatchers bodyMatchers = new BodyMatchers();

		@SuppressWarnings("unchecked")
		Map<String, Object> result = (Map<String, Object>) JsonPathMatcherUtils.removeMatchingJsonPaths(json,
				bodyMatchers);

		assertThat(result.get("name")).isEqualTo("John");
	}

	@Test
	void should_convert_JSON_path_with_regex_to_filter_expression() {
		BodyMatchers bodyMatchers = new BodyMatchers();
		bodyMatchers.jsonPath("$.person.name", bodyMatchers.byRegex("[A-Z][a-z]+"));
		BodyMatcher bodyMatcher = bodyMatchers.matchers().get(0);

		String result = JsonPathMatcherUtils.convertJsonPathAndRegexToAJsonPath(bodyMatcher);

		assertThat(result).isEqualTo("$.person[?(@.name =~ /([A-Z][a-z]+)/)]");
	}

	@Test
	void should_convert_JSON_path_with_equality_to_filter_expression() {
		Map<String, Object> person = new LinkedHashMap<>();
		person.put("name", "John");
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("person", person);
		BodyMatchers bodyMatchers = new BodyMatchers();
		bodyMatchers.jsonPath("$.person.name", bodyMatchers.byEquality());
		BodyMatcher bodyMatcher = bodyMatchers.matchers().get(0);

		String result = JsonPathMatcherUtils.convertJsonPathAndRegexToAJsonPath(bodyMatcher, json);

		assertThat(result).isEqualTo("$.person[?(@.name == 'John')]");
	}

	@Test
	void should_convert_JSON_path_with_numeric_equality() {
		Map<String, Object> person = new LinkedHashMap<>();
		person.put("age", 30);
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("person", person);
		BodyMatchers bodyMatchers = new BodyMatchers();
		bodyMatchers.jsonPath("$.person.age", bodyMatchers.byEquality());
		BodyMatcher bodyMatcher = bodyMatchers.matchers().get(0);

		String result = JsonPathMatcherUtils.convertJsonPathAndRegexToAJsonPath(bodyMatcher, json);

		assertThat(result).isEqualTo("$.person[?(@.age == 30)]");
	}

	@Test
	void should_convert_JSON_path_with_type_matching_and_min_occurrence() {
		BodyMatchers bodyMatchers = new BodyMatchers();
		bodyMatchers.jsonPath("$.items", bodyMatchers.byType((t) -> t.minOccurrence(2)));
		BodyMatcher bodyMatcher = bodyMatchers.matchers().get(0);

		String result = JsonPathMatcherUtils.convertJsonPathAndRegexToAJsonPath(bodyMatcher);

		assertThat(result).isEqualTo("$[?(@.items.size() >= 2)]");
	}

	@Test
	void should_convert_JSON_path_with_type_matching_and_max_occurrence() {
		BodyMatchers bodyMatchers = new BodyMatchers();
		bodyMatchers.jsonPath("$.items", bodyMatchers.byType((t) -> t.maxOccurrence(5)));
		BodyMatcher bodyMatcher = bodyMatchers.matchers().get(0);

		String result = JsonPathMatcherUtils.convertJsonPathAndRegexToAJsonPath(bodyMatcher);

		assertThat(result).isEqualTo("$[?(@.items.size() <= 5)]");
	}

	@Test
	void should_convert_JSON_path_with_type_matching_with_min_and_max_occurrence() {
		BodyMatchers bodyMatchers = new BodyMatchers();
		bodyMatchers.jsonPath("$.items", bodyMatchers.byType((t) -> {
			t.minOccurrence(2);
			t.maxOccurrence(5);
		}));
		BodyMatcher bodyMatcher = bodyMatchers.matchers().get(0);

		String result = JsonPathMatcherUtils.convertJsonPathAndRegexToAJsonPath(bodyMatcher);

		assertThat(result).isEqualTo("$[?(@.items.size() >= 2 && @.items.size() <= 5)]");
	}

	@Test
	void should_return_original_value_for_non_RegexProperty() {
		String value = "test value";

		Object result = JsonPathMatcherUtils.generatedValueIfNeeded(value);

		assertThat(result).isEqualTo("test value");
	}

	@Test
	void should_return_original_value_for_numeric_value() {
		int value = 42;

		Object result = JsonPathMatcherUtils.generatedValueIfNeeded(value);

		assertThat(result).isEqualTo(42);
	}

	@Test
	void should_clone_body_correctly() {
		Map<String, Object> original = new LinkedHashMap<>();
		original.put("name", "John");
		original.put("age", 30);
		original.put("items", List.of(1, 2, 3));

		Object cloned = JsonPathMatcherUtils.cloneBody(original);

		assertThat(cloned).isEqualTo(original);
		assertThat(cloned).isNotSameAs(original);
	}

	@Test
	void should_handle_bracket_notation_in_path_for_equality() {
		Map<String, Object> person = new LinkedHashMap<>();
		person.put("first-name", "John");
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("person", person);
		BodyMatchers bodyMatchers = new BodyMatchers();
		bodyMatchers.jsonPath("$.person['first-name']", bodyMatchers.byEquality());
		BodyMatcher bodyMatcher = bodyMatchers.matchers().get(0);

		String result = JsonPathMatcherUtils.convertJsonPathAndRegexToAJsonPath(bodyMatcher, json);

		assertThat(result).isEqualTo("$[?(@.person['first-name'] == 'John')]");
	}

	@Test
	void should_remove_array_element_matching_path() {
		Map<String, Object> first = new LinkedHashMap<>();
		first.put("id", 1);
		first.put("name", "first");
		Map<String, Object> second = new LinkedHashMap<>();
		second.put("id", 2);
		second.put("name", "second");
		// Use mutable list so the remove operation can work
		java.util.List<Map<String, Object>> items = new java.util.ArrayList<>();
		items.add(first);
		items.add(second);
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("items", items);
		BodyMatchers bodyMatchers = new BodyMatchers();
		bodyMatchers.jsonPath("$.items[0].id", bodyMatchers.byRegex("\\d+"));

		@SuppressWarnings("unchecked")
		Map<String, Object> result = (Map<String, Object>) JsonPathMatcherUtils.removeMatchingJsonPaths(json,
				bodyMatchers);

		@SuppressWarnings("unchecked")
		java.util.List<Map<String, Object>> resultItems = (java.util.List<Map<String, Object>>) result.get("items");
		assertThat(resultItems.get(0).get("id")).isNull();
		assertThat(resultItems.get(0).get("name")).isEqualTo("first");
		assertThat(resultItems.get(1).get("id")).isEqualTo(2);
		assertThat(resultItems.get(1).get("name")).isEqualTo("second");
	}

	@Test
	void should_handle_regex_with_forward_slashes() {
		BodyMatchers bodyMatchers = new BodyMatchers();
		bodyMatchers.jsonPath("$.url", bodyMatchers.byRegex("http://example.com/path"));
		BodyMatcher bodyMatcher = bodyMatchers.matchers().get(0);

		String result = JsonPathMatcherUtils.convertJsonPathAndRegexToAJsonPath(bodyMatcher);

		int[] expected = { 36, 91, 63, 40, 64, 46, 117, 114, 108, 32, 61, 126, 32, 47, 40, 104, 116, 116, 112, 58, 92,
				92, 47, 92, 92, 47, 101, 120, 97, 109, 112, 108, 101, 46, 99, 111, 109, 92, 92, 47, 112, 97, 116, 104,
				41, 47, 41, 93 };
		assertThat(Arrays.equals(result.chars().toArray(), expected)).isTrue();
	}

	@Test
	void should_read_root_level_array() {
		Map<String, Object> first = new LinkedHashMap<>();
		first.put("id", 1);
		Map<String, Object> second = new LinkedHashMap<>();
		second.put("id", 2);
		List<Map<String, Object>> json = List.of(first, second);

		Object firstId = JsonPathMatcherUtils.readElement(json, "$[0].id");
		Object secondId = JsonPathMatcherUtils.readElement(json, "$[1].id");

		assertThat(firstId).isEqualTo(1);
		assertThat(secondId).isEqualTo(2);
	}

	@Test
	void should_handle_deeply_nested_paths() {
		Map<String, Object> level3 = new LinkedHashMap<>();
		level3.put("value", "deep");
		Map<String, Object> level2 = new LinkedHashMap<>();
		level2.put("level3", level3);
		Map<String, Object> level1 = new LinkedHashMap<>();
		level1.put("level2", level2);
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("level1", level1);

		Object result = JsonPathMatcherUtils.readElement(json, "$.level1.level2.level3.value");

		assertThat(result).isEqualTo("deep");
	}

}
