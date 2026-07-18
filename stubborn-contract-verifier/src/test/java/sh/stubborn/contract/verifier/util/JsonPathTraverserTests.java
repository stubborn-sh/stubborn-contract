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
import java.util.List;
import java.util.function.Function;

import com.toomuchcoding.jsonassert.JsonAssertion;
import groovy.json.JsonOutput;
import groovy.json.JsonSlurper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonPathTraverser}.
 */
class JsonPathTraverserTests {

	private final JsonSlurper slurper = new JsonSlurper();

	private final Function<String, Object> parsingFunction = (String s) -> slurper.parseText(s);

	@Test
	void should_traverse_simple_object_without_ordered_verification() {
		Object json = slurper.parseText("""
				{
					"name": "John",
					"age": 30
				}
				""");
		JsonPathTraverser traverser = new JsonPathTraverser(false, parsingFunction);
		MethodBufferingJsonVerifiable rootKey = createRootVerifiable(json);
		List<MethodBufferingJsonVerifiable> collected = new ArrayList<>();
		traverser.traverse(json, rootKey, collected::add);
		assertThat(collected).anyMatch(v -> v.jsonPath().equals("$[?(@.['name'] == 'John')]"));
		assertThat(collected).anyMatch(v -> v.jsonPath().equals("$[?(@.['age'] == 30)]"));
	}

	@Test
	void should_traverse_primitive_array_with_ordered_verification() {
		Object json = slurper.parseText("""
				{
					"numbers": [1, 2, 3]
				}
				""");
		JsonPathTraverser traverser = new JsonPathTraverser(true, parsingFunction);
		MethodBufferingJsonVerifiable rootKey = createRootVerifiable(json);
		List<MethodBufferingJsonVerifiable> collected = new ArrayList<>();
		traverser.traverse(json, rootKey, collected::add);
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("[0]"));
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("[1]"));
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("[2]"));
	}

	@Test
	void should_traverse_primitive_array_without_ordered_verification() {
		Object json = slurper.parseText("""
				{
					"numbers": [1, 2, 3]
				}
				""");
		JsonPathTraverser traverser = new JsonPathTraverser(false, parsingFunction);
		MethodBufferingJsonVerifiable rootKey = createRootVerifiable(json);
		List<MethodBufferingJsonVerifiable> collected = new ArrayList<>();
		traverser.traverse(json, rootKey, collected::add);
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("@ == 1"));
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("@ == 2"));
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("@ == 3"));
		assertThat(collected).noneMatch(v -> v.jsonPath().contains("[0]"));
		assertThat(collected).noneMatch(v -> v.jsonPath().contains("[1]"));
		assertThat(collected).noneMatch(v -> v.jsonPath().contains("[2]"));
	}

	@Test
	void should_traverse_object_array_with_ordered_verification() {
		Object json = slurper.parseText("""
				{
					"items": [
						{"id": 1, "name": "first"},
						{"id": 2, "name": "second"}
					]
				}
				""");
		JsonPathTraverser traverser = new JsonPathTraverser(true, parsingFunction);
		MethodBufferingJsonVerifiable rootKey = createRootVerifiable(json);
		List<MethodBufferingJsonVerifiable> collected = new ArrayList<>();
		traverser.traverse(json, rootKey, collected::add);
		assertThat(collected)
			.anyMatch(v -> v.jsonPath().contains("[0]") && v.jsonPath().contains("id") && v.jsonPath().contains("1"));
		assertThat(collected).anyMatch(
				v -> v.jsonPath().contains("[0]") && v.jsonPath().contains("name") && v.jsonPath().contains("first"));
		assertThat(collected)
			.anyMatch(v -> v.jsonPath().contains("[1]") && v.jsonPath().contains("id") && v.jsonPath().contains("2"));
		assertThat(collected).anyMatch(
				v -> v.jsonPath().contains("[1]") && v.jsonPath().contains("name") && v.jsonPath().contains("second"));
	}

	@Test
	void should_traverse_object_array_without_ordered_verification() {
		Object json = slurper.parseText("""
				{
					"items": [
						{"id": 1, "name": "first"},
						{"id": 2, "name": "second"}
					]
				}
				""");
		JsonPathTraverser traverser = new JsonPathTraverser(false, parsingFunction);
		MethodBufferingJsonVerifiable rootKey = createRootVerifiable(json);
		List<MethodBufferingJsonVerifiable> collected = new ArrayList<>();
		traverser.traverse(json, rootKey, collected::add);
		assertThat(collected)
			.anyMatch(v -> v.jsonPath().contains("[*]") && v.jsonPath().contains("id") && v.jsonPath().contains("1"));
		assertThat(collected).anyMatch(
				v -> v.jsonPath().contains("[*]") && v.jsonPath().contains("name") && v.jsonPath().contains("first"));
		assertThat(collected)
			.anyMatch(v -> v.jsonPath().contains("[*]") && v.jsonPath().contains("id") && v.jsonPath().contains("2"));
		assertThat(collected).anyMatch(
				v -> v.jsonPath().contains("[*]") && v.jsonPath().contains("name") && v.jsonPath().contains("second"));
	}

	@Test
	void should_traverse_nested_objects() {
		Object json = slurper.parseText("""
				{
					"person": {
						"address": {
							"city": "NYC",
							"zip": "10001"
						}
					}
				}
				""");
		JsonPathTraverser traverser = new JsonPathTraverser(false, parsingFunction);
		MethodBufferingJsonVerifiable rootKey = createRootVerifiable(json);
		List<MethodBufferingJsonVerifiable> collected = new ArrayList<>();
		traverser.traverse(json, rootKey, collected::add);
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("city") && v.jsonPath().contains("NYC"));
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("zip") && v.jsonPath().contains("10001"));
	}

	@Test
	void should_handle_empty_map() {
		Object json = slurper.parseText("""
				{
					"empty": {}
				}
				""");
		JsonPathTraverser traverser = new JsonPathTraverser(false, parsingFunction);
		MethodBufferingJsonVerifiable rootKey = createRootVerifiable(json);
		List<MethodBufferingJsonVerifiable> collected = new ArrayList<>();
		traverser.traverse(json, rootKey, collected::add);
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("empty"));
	}

	@Test
	void should_handle_empty_array() {
		Object json = slurper.parseText("""
				{
					"items": []
				}
				""");
		JsonPathTraverser traverser = new JsonPathTraverser(false, parsingFunction);
		MethodBufferingJsonVerifiable rootKey = createRootVerifiable(json);
		List<MethodBufferingJsonVerifiable> collected = new ArrayList<>();
		traverser.traverse(json, rootKey, collected::add);
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("items"));
	}

	@Test
	void should_traverse_string_array_with_ordered_verification() {
		Object json = slurper.parseText("""
				{
					"tags": ["red", "green", "blue"]
				}
				""");
		JsonPathTraverser traverser = new JsonPathTraverser(true, parsingFunction);
		MethodBufferingJsonVerifiable rootKey = createRootVerifiable(json);
		List<MethodBufferingJsonVerifiable> collected = new ArrayList<>();
		traverser.traverse(json, rootKey, collected::add);
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("[0]"));
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("[1]"));
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("[2]"));
	}

	@Test
	void should_traverse_nested_array_with_ordered_verification() {
		Object json = slurper.parseText("""
				{
					"matrix": [[1, 2], [3, 4]]
				}
				""");
		JsonPathTraverser traverser = new JsonPathTraverser(true, parsingFunction);
		MethodBufferingJsonVerifiable rootKey = createRootVerifiable(json);
		List<MethodBufferingJsonVerifiable> collected = new ArrayList<>();
		traverser.traverse(json, rootKey, collected::add);
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("[0]"));
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("[1]"));
	}

	@Test
	void should_traverse_root_level_array_with_ordered_verification() {
		Object json = slurper.parseText("""
				[
					{"id": 1},
					{"id": 2}
				]
				""");
		JsonPathTraverser traverser = new JsonPathTraverser(true, parsingFunction);
		MethodBufferingJsonVerifiable rootKey = createRootVerifiable(json);
		List<MethodBufferingJsonVerifiable> collected = new ArrayList<>();
		traverser.traverse(json, rootKey, collected::add);
		assertThat(collected)
			.anyMatch(v -> v.jsonPath().contains("[0]") && v.jsonPath().contains("id") && v.jsonPath().contains("1"));
		assertThat(collected)
			.anyMatch(v -> v.jsonPath().contains("[1]") && v.jsonPath().contains("id") && v.jsonPath().contains("2"));
	}

	@Test
	void should_traverse_root_level_array_without_ordered_verification() {
		Object json = slurper.parseText("""
				[
					{"id": 1},
					{"id": 2}
				]
				""");
		JsonPathTraverser traverser = new JsonPathTraverser(false, parsingFunction);
		MethodBufferingJsonVerifiable rootKey = createRootVerifiable(json);
		List<MethodBufferingJsonVerifiable> collected = new ArrayList<>();
		traverser.traverse(json, rootKey, collected::add);
		assertThat(collected)
			.anyMatch(v -> v.jsonPath().contains("[*]") && v.jsonPath().contains("id") && v.jsonPath().contains("1"));
		assertThat(collected)
			.anyMatch(v -> v.jsonPath().contains("[*]") && v.jsonPath().contains("id") && v.jsonPath().contains("2"));
	}

	@Test
	void should_add_size_check_for_primitive_arrays_with_ordered_verification() {
		Object json = slurper.parseText("""
				{
					"numbers": [1, 2, 3]
				}
				""");
		JsonPathTraverser traverser = new JsonPathTraverser(true, parsingFunction);
		MethodBufferingJsonVerifiable rootKey = createRootVerifiable(json);
		List<MethodBufferingJsonVerifiable> collected = new ArrayList<>();
		traverser.traverse(json, rootKey, collected::add);
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("hasSize") || v.method().contains("hasSize"));
	}

	@Test
	void should_not_add_size_check_without_ordered_verification() {
		Object json = slurper.parseText("""
				{
					"numbers": [1, 2, 3]
				}
				""");
		JsonPathTraverser traverser = new JsonPathTraverser(false, parsingFunction);
		MethodBufferingJsonVerifiable rootKey = createRootVerifiable(json);
		List<MethodBufferingJsonVerifiable> collected = new ArrayList<>();
		traverser.traverse(json, rootKey, collected::add);
		assertThat(collected).noneMatch(v -> v.method().contains("hasSize"));
	}

	@Test
	void should_traverse_mixed_primitive_types_in_array_with_ordered_verification() {
		Object json = slurper.parseText("""
				{
					"mixed": ["text", 42, true, 3.14]
				}
				""");
		JsonPathTraverser traverser = new JsonPathTraverser(true, parsingFunction);
		MethodBufferingJsonVerifiable rootKey = createRootVerifiable(json);
		List<MethodBufferingJsonVerifiable> collected = new ArrayList<>();
		traverser.traverse(json, rootKey, collected::add);
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("[0]"));
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("[1]"));
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("[2]"));
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("[3]"));
	}

	@Test
	void should_traverse_deeply_nested_structure() {
		Object json = slurper.parseText("""
				{
					"level1": {
						"level2": {
							"level3": {
								"level4": {
									"value": "deep"
								}
							}
						}
					}
				}
				""");
		JsonPathTraverser traverser = new JsonPathTraverser(false, parsingFunction);
		MethodBufferingJsonVerifiable rootKey = createRootVerifiable(json);
		List<MethodBufferingJsonVerifiable> collected = new ArrayList<>();
		traverser.traverse(json, rootKey, collected::add);
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("value") && v.jsonPath().contains("deep"));
	}

	@Test
	void should_handle_boolean_values() {
		Object json = slurper.parseText("""
				{
					"active": true,
					"deleted": false
				}
				""");
		JsonPathTraverser traverser = new JsonPathTraverser(false, parsingFunction);
		MethodBufferingJsonVerifiable rootKey = createRootVerifiable(json);
		List<MethodBufferingJsonVerifiable> collected = new ArrayList<>();
		traverser.traverse(json, rootKey, collected::add);
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("active") && v.jsonPath().contains("true"));
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("deleted") && v.jsonPath().contains("false"));
	}

	@Test
	void should_traverse_array_of_arrays_at_root_level_with_ordered_verification() {
		Object json = slurper.parseText("[[1, 2], [3, 4], [5, 6]]");
		JsonPathTraverser traverser = new JsonPathTraverser(true, parsingFunction);
		MethodBufferingJsonVerifiable rootKey = createRootVerifiable(json);
		List<MethodBufferingJsonVerifiable> collected = new ArrayList<>();
		traverser.traverse(json, rootKey, collected::add);
		assertThat(collected).isNotEmpty();
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("[0]"));
	}

	@Test
	void should_handle_special_characters_in_keys() {
		Object json = slurper.parseText("""
				{
					"special-key": "value1",
					"key.with.dots": "value2"
				}
				""");
		JsonPathTraverser traverser = new JsonPathTraverser(false, parsingFunction);
		MethodBufferingJsonVerifiable rootKey = createRootVerifiable(json);
		List<MethodBufferingJsonVerifiable> collected = new ArrayList<>();
		traverser.traverse(json, rootKey, collected::add);
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("special-key") && v.jsonPath().contains("value1"));
		assertThat(collected).anyMatch(v -> v.jsonPath().contains("key.with.dots") && v.jsonPath().contains("value2"));
	}

	private MethodBufferingJsonVerifiable createRootVerifiable(Object json) {
		return new DelegatingJsonVerifiable(
				JsonAssertion.assertThat(JsonOutput.toJson(json)).withoutThrowingException());
	}

}
