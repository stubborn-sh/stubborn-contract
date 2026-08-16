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
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link JsonPathTraverser} exercised via
 * {@link JsonToJsonPathsConverter}. Assertions pin the exact generated JSON path and the
 * exact assertion method chain so that negated conditionals and stripped return values
 * produce a detectable difference.
 */
class JsonPathTraverserMutationTests {

	private final JsonToJsonPathsConverter unordered = new JsonToJsonPathsConverter();

	private final JsonToJsonPathsConverter ordered = new JsonToJsonPathsConverter(true);

	private static List<String> pathsAndMethods(JsonPaths paths) {
		List<String> result = new ArrayList<>();
		for (MethodBufferingJsonVerifiable v : paths) {
			result.add(v.jsonPath() + " || " + v.method());
		}
		return result;
	}

	private List<String> unordered(Object json) {
		return pathsAndMethods(this.unordered.transformToJsonPathWithTestsSideValues(json));
	}

	private List<String> ordered(Object json) {
		return pathsAndMethods(this.ordered.transformToJsonPathWithTestsSideValues(json));
	}

	@Test
	void nested_object_produces_field_chains() {
		Map<String, Object> inner = new LinkedHashMap<>();
		inner.put("street", "Kingston");
		inner.put("num", 20);
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("address", inner);
		json.put("name", "foo");

		assertThat(unordered(json)).containsExactlyInAnyOrder(
				"$.['address'][?(@.['street'] == 'Kingston')] || .field(\"['address']\").field(\"['street']\").isEqualTo(\"Kingston\")",
				"$.['address'][?(@.['num'] == 20)] || .field(\"['address']\").field(\"['num']\").isEqualTo(20)",
				"$[?(@.['name'] == 'foo')] || .field(\"['name']\").isEqualTo(\"foo\")");
	}

	@Test
	void unordered_primitive_number_array_uses_arrayField() {
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("nums", List.of(1, 2, 3));

		assertThat(unordered(json)).containsExactlyInAnyOrder(
				"$.['nums'][?(@ == 1)] || .array(\"['nums']\").arrayField().isEqualTo(1)",
				"$.['nums'][?(@ == 2)] || .array(\"['nums']\").arrayField().isEqualTo(2)",
				"$.['nums'][?(@ == 3)] || .array(\"['nums']\").arrayField().isEqualTo(3)");
	}

	@Test
	void unordered_primitive_string_array_appends_value_call() {
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("names", List.of("a", "b"));

		assertThat(unordered(json)).containsExactlyInAnyOrder(
				"$.['names'][?(@ == 'a')] || .array(\"['names']\").arrayField().isEqualTo(\"a\").value()",
				"$.['names'][?(@ == 'b')] || .array(\"['names']\").arrayField().isEqualTo(\"b\").value()");
	}

	@Test
	void unordered_complex_array_uses_contains() {
		List<Map<String, Object>> objs = new ArrayList<>();
		objs.add(Map.of("id", 1));
		objs.add(Map.of("id", 2));
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("items", objs);

		assertThat(unordered(json)).containsExactlyInAnyOrder(
				"$.['items'][*][?(@.['id'] == 1)] || .array(\"['items']\").contains(\"['id']\").isEqualTo(1)",
				"$.['items'][*][?(@.['id'] == 2)] || .array(\"['items']\").contains(\"['id']\").isEqualTo(2)");
	}

	@Test
	void boolean_and_long_values_keep_type_specific_assertions() {
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("flag", true);
		json.put("count", 7L);

		assertThat(unordered(json)).containsExactlyInAnyOrder(
				"$[?(@.['flag'] == true)] || .field(\"['flag']\").isEqualTo(true)",
				"$[?(@.['count'] == 7)] || .field(\"['count']\").isEqualTo(7L)");
	}

	@Test
	void pattern_value_uses_matches() {
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("code", Pattern.compile("[0-9]+"));

		assertThat(unordered(json))
			.containsExactly("$[?(@.['code'] =~ /[0-9]+/)] || .field(\"['code']\").matches(\"[0-9]+\")");
	}

	@Test
	void empty_containers_emit_isEmpty_field_and_isEmpty_array() {
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("emptyMap", new LinkedHashMap<>());
		json.put("emptyList", new ArrayList<>());

		assertThat(unordered(json)).containsExactlyInAnyOrder("$.['emptyMap'] || .field(\"['emptyMap']\").isEmpty()",
				"$.['emptyList'][*] || .array(\"['emptyList']\").isEmpty()");
	}

	@Test
	void list_of_primitive_lists_unordered() {
		List<List<Integer>> lol = new ArrayList<>();
		lol.add(List.of(1, 2));
		lol.add(List.of(3, 4));
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("matrix", lol);

		assertThat(unordered(json)).containsExactlyInAnyOrder(
				"$.['matrix'][*][?(@ == 1)] || .array(\"['matrix']\").array().arrayField().isEqualTo(1)",
				"$.['matrix'][*][?(@ == 2)] || .array(\"['matrix']\").array().arrayField().isEqualTo(2)",
				"$.['matrix'][*][?(@ == 3)] || .array(\"['matrix']\").array().arrayField().isEqualTo(3)",
				"$.['matrix'][*][?(@ == 4)] || .array(\"['matrix']\").array().arrayField().isEqualTo(4)");
	}

	@Test
	void root_primitive_list_unordered() {
		assertThat(unordered(List.of(1, 2, 3))).containsExactlyInAnyOrder("$[?(@ == 1)] || .arrayField().isEqualTo(1)",
				"$[?(@ == 2)] || .arrayField().isEqualTo(2)", "$[?(@ == 3)] || .arrayField().isEqualTo(3)");
	}

	@Test
	void root_string_list_unordered_appends_value() {
		assertThat(unordered(List.of("x", "y"))).containsExactlyInAnyOrder(
				"$[?(@ == 'x')] || .arrayField().isEqualTo(\"x\").value()",
				"$[?(@ == 'y')] || .arrayField().isEqualTo(\"y\").value()");
	}

	// ===== ordered array verification =====

	@Test
	void ordered_primitive_array_adds_size_check_and_indexed_elements() {
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("nums", List.of(1, 2, 3));

		assertThat(ordered(json)).containsExactlyInAnyOrder("$.['nums'] || .array(\"['nums']\").hasSize(3)",
				"$.['nums'][0] || .array(\"['nums']\").elementWithIndex(0).isEqualTo(1)",
				"$.['nums'][1] || .array(\"['nums']\").elementWithIndex(1).isEqualTo(2)",
				"$.['nums'][2] || .array(\"['nums']\").elementWithIndex(2).isEqualTo(3)");
	}

	@Test
	void ordered_complex_array_uses_index_and_field_no_size_check() {
		List<Map<String, Object>> objs = new ArrayList<>();
		objs.add(Map.of("id", 1));
		objs.add(Map.of("id", 2));
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("items", objs);

		assertThat(ordered(json)).containsExactlyInAnyOrder(
				"$.['items'][0][?(@.['id'] == 1)] || .array(\"['items']\").elementWithIndex(0).field(\"['id']\").isEqualTo(1)",
				"$.['items'][1][?(@.['id'] == 2)] || .array(\"['items']\").elementWithIndex(1).field(\"['id']\").isEqualTo(2)");
	}

	@Test
	void ordered_list_of_lists_nests_indexes_and_size_checks() {
		List<List<Integer>> lol = new ArrayList<>();
		lol.add(List.of(1, 2));
		lol.add(List.of(3, 4));
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("matrix", lol);

		assertThat(ordered(json)).containsExactlyInAnyOrder(
				"$.['matrix'][0] || .array(\"['matrix']\").elementWithIndex(0).hasSize(2)",
				"$.['matrix'][0][0] || .array(\"['matrix']\").elementWithIndex(0).elementWithIndex(0).isEqualTo(1)",
				"$.['matrix'][0][1] || .array(\"['matrix']\").elementWithIndex(0).elementWithIndex(1).isEqualTo(2)",
				"$.['matrix'][1] || .array(\"['matrix']\").elementWithIndex(1).hasSize(2)",
				"$.['matrix'][1][0] || .array(\"['matrix']\").elementWithIndex(1).elementWithIndex(0).isEqualTo(3)",
				"$.['matrix'][1][1] || .array(\"['matrix']\").elementWithIndex(1).elementWithIndex(1).isEqualTo(4)");
	}

	@Test
	void ordered_root_primitive_list_size_and_array_indexes() {
		assertThat(ordered(List.of(5, 6))).containsExactlyInAnyOrder("$ || .hasSize(2)",
				"$[*][0] || .array().elementWithIndex(0).isEqualTo(5)",
				"$[*][1] || .array().elementWithIndex(1).isEqualTo(6)");
	}

}
