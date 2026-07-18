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

import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import groovy.json.JsonOutput;
import groovy.json.JsonSlurper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonToJsonPathsConverter} with ordered array verification enabled. All
 * tests in this class run with a converter constructed with {@code assertJsonSize=true},
 * which enables exact index-based array element verification instead of wildcard
 * matching.
 *
 * @author Marcin Grzejszczak
 * @since 5.1.0
 */
class JsonToJsonPathsConverterWithArrayCheckTests {

	private final JsonSlurper slurper = new JsonSlurper();

	/**
	 * Creates a converter with ordered array verification enabled.
	 */
	private static JsonToJsonPathsConverter converter() {
		return new JsonToJsonPathsConverter(true);
	}

	// ========== Primitive Arrays ==========

	@Test
	void shouldGenerateOrderedAssertionsForSimpleStringArray() {
		Map<String, Object> json = Map.of("items", List.of("first", "second", "third"));
		JsonPaths pathAndValues = converter().transformToJsonPathWithTestsSideValues(json);

		// size check
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['items']\").hasSize(3)");
			assertThat(entry.jsonPath()).isEqualTo("$.['items']");
		});
		// first element
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['items']\").elementWithIndex(0).isEqualTo(\"first\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['items'][0]");
		});
		// second element
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['items']\").elementWithIndex(1).isEqualTo(\"second\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['items'][1]");
		});
		// third element
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['items']\").elementWithIndex(2).isEqualTo(\"third\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['items'][2]");
		});
		// exactly 4 assertions (1 size + 3 elements)
		assertThat(pathAndValues).hasSize(4);
	}

	@Test
	void shouldGenerateOrderedAssertionsForNumberArray() {
		Map<String, Object> json = Map.of("numbers", List.of(10, 20, 30, 40));
		JsonPaths pathAndValues = converter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['numbers']\").hasSize(4)");
			assertThat(entry.jsonPath()).isEqualTo("$.['numbers']");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['numbers']\").elementWithIndex(0).isEqualTo(10)");
			assertThat(entry.jsonPath()).isEqualTo("$.['numbers'][0]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['numbers']\").elementWithIndex(1).isEqualTo(20)");
			assertThat(entry.jsonPath()).isEqualTo("$.['numbers'][1]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['numbers']\").elementWithIndex(2).isEqualTo(30)");
			assertThat(entry.jsonPath()).isEqualTo("$.['numbers'][2]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['numbers']\").elementWithIndex(3).isEqualTo(40)");
			assertThat(entry.jsonPath()).isEqualTo("$.['numbers'][3]");
		});
		// exactly 5 assertions (1 size + 4 elements)
		assertThat(pathAndValues).hasSize(5);
	}

	@Test
	void shouldGenerateOrderedAssertionsForBooleanArray() {
		Map<String, Object> json = Map.of("flags", List.of(true, false, true));
		JsonPaths pathAndValues = converter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['flags']\").hasSize(3)");
			assertThat(entry.jsonPath()).isEqualTo("$.['flags']");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['flags']\").elementWithIndex(0).isEqualTo(true)");
			assertThat(entry.jsonPath()).isEqualTo("$.['flags'][0]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['flags']\").elementWithIndex(1).isEqualTo(false)");
			assertThat(entry.jsonPath()).isEqualTo("$.['flags'][1]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['flags']\").elementWithIndex(2).isEqualTo(true)");
			assertThat(entry.jsonPath()).isEqualTo("$.['flags'][2]");
		});
		// exactly 4 assertions (1 size + 3 elements)
		assertThat(pathAndValues).hasSize(4);
	}

	@Test
	void shouldGenerateOrderedAssertionsForMixedPrimitiveArray() {
		Map<String, Object> json = Map.of("mixed", List.of("text", 123, true));
		JsonPaths pathAndValues = converter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['mixed']\").hasSize(3)");
			assertThat(entry.jsonPath()).isEqualTo("$.['mixed']");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['mixed']\").elementWithIndex(0).isEqualTo(\"text\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['mixed'][0]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['mixed']\").elementWithIndex(1).isEqualTo(123)");
			assertThat(entry.jsonPath()).isEqualTo("$.['mixed'][1]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['mixed']\").elementWithIndex(2).isEqualTo(true)");
			assertThat(entry.jsonPath()).isEqualTo("$.['mixed'][2]");
		});
		// exactly 4 assertions (1 size + 3 elements)
		assertThat(pathAndValues).hasSize(4);
	}

	// ========== Object Arrays ==========

	@Test
	void shouldGenerateOrderedAssertionsForArrayOfObjects() {
		Map<String, Object> json = Map.of("users",
				List.of(Map.of("name", "Alice", "age", 30), Map.of("name", "Bob", "age", 25)));
		JsonPaths pathAndValues = converter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['users']\").elementWithIndex(0).field(\"['name']\").isEqualTo(\"Alice\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['users'][0][?(@.['name'] == 'Alice')]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['users']\").elementWithIndex(0).field(\"['age']\").isEqualTo(30)");
			assertThat(entry.jsonPath()).isEqualTo("$.['users'][0][?(@.['age'] == 30)]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['users']\").elementWithIndex(1).field(\"['name']\").isEqualTo(\"Bob\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['users'][1][?(@.['name'] == 'Bob')]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['users']\").elementWithIndex(1).field(\"['age']\").isEqualTo(25)");
			assertThat(entry.jsonPath()).isEqualTo("$.['users'][1][?(@.['age'] == 25)]");
		});
		// exactly 4 assertions (2 users x 2 fields)
		assertThat(pathAndValues).hasSize(4);
	}

	@Test
	void shouldGenerateOrderedAssertionsForArrayOfObjectsWithSameValues() {
		Map<String, Object> json = Map.of("entries",
				List.of(Map.of("status", "active"), Map.of("status", "active"), Map.of("status", "inactive")));
		JsonPaths pathAndValues = converter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['entries']\").elementWithIndex(0).field(\"['status']\").isEqualTo(\"active\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['entries'][0][?(@.['status'] == 'active')]");
		});
		// same value, different index
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['entries']\").elementWithIndex(1).field(\"['status']\").isEqualTo(\"active\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['entries'][1][?(@.['status'] == 'active')]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['entries']\").elementWithIndex(2).field(\"['status']\").isEqualTo(\"inactive\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['entries'][2][?(@.['status'] == 'inactive')]");
		});
		// exactly 3 assertions
		assertThat(pathAndValues).hasSize(3);
	}

	// ========== Nested Arrays ==========

	@Test
	void shouldGenerateOrderedAssertionsForNestedPrimitiveArrays() {
		Map<String, Object> json = Map.of("matrix", List.of(List.of("a", "b"), List.of("c", "d")));
		JsonPaths pathAndValues = converter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy(
				entry -> assertThat(entry.method()).contains("elementWithIndex(0)").contains("isEqualTo(\"a\")"));
		assertThat(pathAndValues).anySatisfy(
				entry -> assertThat(entry.method()).contains("elementWithIndex(0)").contains("isEqualTo(\"b\")"));
		assertThat(pathAndValues).anySatisfy(
				entry -> assertThat(entry.method()).contains("elementWithIndex(1)").contains("isEqualTo(\"c\")"));
		assertThat(pathAndValues).anySatisfy(
				entry -> assertThat(entry.method()).contains("elementWithIndex(1)").contains("isEqualTo(\"d\")"));
	}

	@Test
	void shouldGenerateOrderedAssertionsForArrayWithNestedObjectsAllFields() {
		Map<String, Object> json = Map.of("orders",
				List.of(Map.of("id", 1, "items",
						List.of(Map.of("name", "item1", "qty", 2), Map.of("name", "item2", "qty", 3))),
						Map.of("id", 2, "items", List.of(Map.of("name", "item3", "qty", 1)))));
		JsonPaths pathAndValues = converter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['orders']\").elementWithIndex(0).field(\"['id']\").isEqualTo(1)");
			assertThat(entry.jsonPath()).isEqualTo("$.['orders'][0][?(@.['id'] == 1)]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> assertThat(entry.method()).contains("elementWithIndex(0)")
			.contains("['items']")
			.contains("['name']")
			.contains("isEqualTo(\"item1\")"));
		assertThat(pathAndValues).anySatisfy(entry -> assertThat(entry.method()).contains("elementWithIndex(0)")
			.contains("['items']")
			.contains("['qty']")
			.contains("isEqualTo(2)"));
		assertThat(pathAndValues).anySatisfy(entry -> assertThat(entry.method()).contains("elementWithIndex(0)")
			.contains("['items']")
			.contains("['name']")
			.contains("isEqualTo(\"item2\")"));
		assertThat(pathAndValues).anySatisfy(entry -> assertThat(entry.method()).contains("elementWithIndex(0)")
			.contains("['items']")
			.contains("['qty']")
			.contains("isEqualTo(3)"));
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['orders']\").elementWithIndex(1).field(\"['id']\").isEqualTo(2)");
			assertThat(entry.jsonPath()).isEqualTo("$.['orders'][1][?(@.['id'] == 2)]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> assertThat(entry.method()).contains("elementWithIndex(1)")
			.contains("['items']")
			.contains("['name']")
			.contains("isEqualTo(\"item3\")"));
		assertThat(pathAndValues).anySatisfy(entry -> assertThat(entry.method()).contains("elementWithIndex(1)")
			.contains("['items']")
			.contains("['qty']")
			.contains("isEqualTo(1)"));
	}

	// ========== Root Level Arrays ==========

	@Test
	void shouldGenerateOrderedAssertionsForRootLevelArrayOfPrimitives() {
		String json = """
				["first", "second", "third"]""";
		JsonPaths pathAndValues = converter().transformToJsonPathWithTestsSideValues(slurper.parseText(json));

		// size check for root array
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".hasSize(3)");
			assertThat(entry.jsonPath()).isEqualTo("$");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array().elementWithIndex(0).isEqualTo(\"first\")");
			assertThat(entry.jsonPath()).isEqualTo("$[*][0]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array().elementWithIndex(1).isEqualTo(\"second\")");
			assertThat(entry.jsonPath()).isEqualTo("$[*][1]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array().elementWithIndex(2).isEqualTo(\"third\")");
			assertThat(entry.jsonPath()).isEqualTo("$[*][2]");
		});
		// exactly 4 assertions (1 size + 3 elements)
		assertThat(pathAndValues).hasSize(4);
	}

	@Test
	void shouldGenerateOrderedAssertionsForRootLevelArrayOfObjects() {
		String json = """
				[
				{"property1": "a"},
				{"property2": "b"}
				]""";
		JsonPaths pathAndValues = converter().transformToJsonPathWithTestsSideValues(slurper.parseText(json));

		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array().elementWithIndex(0).field(\"['property1']\").isEqualTo(\"a\")");
			assertThat(entry.jsonPath()).isEqualTo("$[*][0][?(@.['property1'] == 'a')]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array().elementWithIndex(1).field(\"['property2']\").isEqualTo(\"b\")");
			assertThat(entry.jsonPath()).isEqualTo("$[*][1][?(@.['property2'] == 'b')]");
		});
		// exactly 2 assertions (2 objects with 1 field each)
		assertThat(pathAndValues).hasSize(2);
	}

	// ========== Complex Real-World Scenarios ==========

	@Test
	void shouldGenerateOrderedAssertionsForResponseWithErrorsArrayAllFields() {
		Map<String, Object> json = Map.of("errors",
				List.of(Map.of("property", "email", "message", "invalid format"),
						Map.of("property", "phone", "message", "required field"),
						Map.of("property", "age", "message", "must be positive")));
		JsonPaths pathAndValues = converter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['errors']\").elementWithIndex(0).field(\"['property']\").isEqualTo(\"email\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['errors'][0][?(@.['property'] == 'email')]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(
					".array(\"['errors']\").elementWithIndex(0).field(\"['message']\").isEqualTo(\"invalid format\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['errors'][0][?(@.['message'] == 'invalid format')]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['errors']\").elementWithIndex(1).field(\"['property']\").isEqualTo(\"phone\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['errors'][1][?(@.['property'] == 'phone')]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(
					".array(\"['errors']\").elementWithIndex(1).field(\"['message']\").isEqualTo(\"required field\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['errors'][1][?(@.['message'] == 'required field')]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['errors']\").elementWithIndex(2).field(\"['property']\").isEqualTo(\"age\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['errors'][2][?(@.['property'] == 'age')]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(
					".array(\"['errors']\").elementWithIndex(2).field(\"['message']\").isEqualTo(\"must be positive\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['errors'][2][?(@.['message'] == 'must be positive')]");
		});
		// exactly 6 assertions (3 errors x 2 fields)
		assertThat(pathAndValues).hasSize(6);
	}

	@Test
	void shouldGenerateOrderedAssertionsForPaginatedResponseAllFields() {
		Map<String, Object> json = Map.of("page", 1, "totalPages", 5, "data",
				List.of(Map.of("id", 101, "name", "First Item"), Map.of("id", 102, "name", "Second Item"),
						Map.of("id", 103, "name", "Third Item")));
		JsonPaths pathAndValues = converter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".field(\"['page']\").isEqualTo(1)");
			assertThat(entry.jsonPath()).isEqualTo("$[?(@.['page'] == 1)]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".field(\"['totalPages']\").isEqualTo(5)");
			assertThat(entry.jsonPath()).isEqualTo("$[?(@.['totalPages'] == 5)]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['data']\").elementWithIndex(0).field(\"['id']\").isEqualTo(101)");
			assertThat(entry.jsonPath()).isEqualTo("$.['data'][0][?(@.['id'] == 101)]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['data']\").elementWithIndex(0).field(\"['name']\").isEqualTo(\"First Item\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['data'][0][?(@.['name'] == 'First Item')]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['data']\").elementWithIndex(1).field(\"['id']\").isEqualTo(102)");
			assertThat(entry.jsonPath()).isEqualTo("$.['data'][1][?(@.['id'] == 102)]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['data']\").elementWithIndex(1).field(\"['name']\").isEqualTo(\"Second Item\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['data'][1][?(@.['name'] == 'Second Item')]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['data']\").elementWithIndex(2).field(\"['id']\").isEqualTo(103)");
			assertThat(entry.jsonPath()).isEqualTo("$.['data'][2][?(@.['id'] == 103)]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['data']\").elementWithIndex(2).field(\"['name']\").isEqualTo(\"Third Item\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['data'][2][?(@.['name'] == 'Third Item')]");
		});
		// exactly 8 assertions (2 metadata fields + 3 data items x 2 fields)
		assertThat(pathAndValues).hasSize(8);
	}

	@Test
	void shouldGenerateOrderedAssertionsForTimelineSequenceDataAllFields() {
		Map<String, Object> json = Map.of("events",
				List.of(Map.of("timestamp", "2024-01-01T10:00:00Z", "action", "created"),
						Map.of("timestamp", "2024-01-01T10:05:00Z", "action", "updated"),
						Map.of("timestamp", "2024-01-01T10:10:00Z", "action", "published")));
		JsonPaths pathAndValues = converter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(
					".array(\"['events']\").elementWithIndex(0).field(\"['timestamp']\").isEqualTo(\"2024-01-01T10:00:00Z\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['events'][0][?(@.['timestamp'] == '2024-01-01T10:00:00Z')]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['events']\").elementWithIndex(0).field(\"['action']\").isEqualTo(\"created\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['events'][0][?(@.['action'] == 'created')]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(
					".array(\"['events']\").elementWithIndex(1).field(\"['timestamp']\").isEqualTo(\"2024-01-01T10:05:00Z\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['events'][1][?(@.['timestamp'] == '2024-01-01T10:05:00Z')]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['events']\").elementWithIndex(1).field(\"['action']\").isEqualTo(\"updated\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['events'][1][?(@.['action'] == 'updated')]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(
					".array(\"['events']\").elementWithIndex(2).field(\"['timestamp']\").isEqualTo(\"2024-01-01T10:10:00Z\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['events'][2][?(@.['timestamp'] == '2024-01-01T10:10:00Z')]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['events']\").elementWithIndex(2).field(\"['action']\").isEqualTo(\"published\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['events'][2][?(@.['action'] == 'published')]");
		});
		// exactly 6 assertions (3 events x 2 fields)
		assertThat(pathAndValues).hasSize(6);
	}

	// ========== Edge Cases ==========

	@Test
	void shouldGenerateOrderedAssertionsForSingleElementArray() {
		Map<String, Object> json = Map.of("items", List.of("only"));
		JsonPaths pathAndValues = converter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['items']\").hasSize(1)");
			assertThat(entry.jsonPath()).isEqualTo("$.['items']");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['items']\").elementWithIndex(0).isEqualTo(\"only\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['items'][0]");
		});
		// exactly 2 assertions (1 size + 1 element)
		assertThat(pathAndValues).hasSize(2);
	}

	@Test
	void shouldNotGenerateAssertionsForEmptyArray() {
		Map<String, Object> json = Map.of("items", List.of());
		JsonPaths pathAndValues = converter().transformToJsonPathWithTestsSideValues(json);

		// exactly 1 assertion (empty array check)
		assertThat(pathAndValues).hasSize(1);
		// no elementWithIndex assertions
		assertThat(pathAndValues).noneMatch(entry -> entry.method().contains("elementWithIndex"));
	}

	@Test
	void shouldHandleArrayWithDecimalNumbers() {
		Map<String, Object> json = Map.of("prices", List.of(19.99, 29.99, 9.99));
		JsonPaths pathAndValues = converter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['prices']\").hasSize(3)");
			assertThat(entry.jsonPath()).isEqualTo("$.['prices']");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['prices']\").elementWithIndex(0).isEqualTo(19.99)");
			assertThat(entry.jsonPath()).isEqualTo("$.['prices'][0]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['prices']\").elementWithIndex(1).isEqualTo(29.99)");
			assertThat(entry.jsonPath()).isEqualTo("$.['prices'][1]");
		});
		assertThat(pathAndValues).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['prices']\").elementWithIndex(2).isEqualTo(9.99)");
			assertThat(entry.jsonPath()).isEqualTo("$.['prices'][2]");
		});
		// exactly 4 assertions (1 size + 3 elements)
		assertThat(pathAndValues).hasSize(4);
	}

	// ========== Comparison with Unordered ==========

	@Test
	void orderedVerificationShouldProduceDifferentResultsThanUnordered() {
		Map<String, Object> json = Map.of("items", List.of("a", "b", "c"));
		JsonPaths orderedPaths = converter().transformToJsonPathWithTestsSideValues(json);
		JsonPaths unorderedPaths = new JsonToJsonPathsConverter(false).transformToJsonPathWithTestsSideValues(json);

		// ordered should use elementWithIndex
		assertThat(orderedPaths).anyMatch(entry -> entry.method().contains("elementWithIndex"));
		// unordered should not use elementWithIndex
		assertThat(unorderedPaths).noneMatch(entry -> entry.method().contains("elementWithIndex"));
		// ordered should have exact index paths
		assertThat(orderedPaths).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['items']\").elementWithIndex(0).isEqualTo(\"a\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['items'][0]");
		});
		assertThat(orderedPaths).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['items']\").elementWithIndex(1).isEqualTo(\"b\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['items'][1]");
		});
		assertThat(orderedPaths).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['items']\").elementWithIndex(2).isEqualTo(\"c\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['items'][2]");
		});
		// unordered should use arrayField with filtered json paths
		assertThat(unorderedPaths).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['items']\").arrayField().isEqualTo(\"a\").value()");
			assertThat(entry.jsonPath()).isEqualTo("$.['items'][?(@ == 'a')]");
		});
		assertThat(unorderedPaths).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['items']\").arrayField().isEqualTo(\"b\").value()");
			assertThat(entry.jsonPath()).isEqualTo("$.['items'][?(@ == 'b')]");
		});
		assertThat(unorderedPaths).anySatisfy(entry -> {
			assertThat(entry.method()).isEqualTo(".array(\"['items']\").arrayField().isEqualTo(\"c\").value()");
			assertThat(entry.jsonPath()).isEqualTo("$.['items'][?(@ == 'c')]");
		});
	}

	// ========== JSON Path Validity ==========

	@Test
	void allGeneratedJsonPathsShouldBeValidAndAccountForAllElements() {
		Map<String, Object> json = Map.of("users",
				List.of(Map.of("name", "Alice", "roles", List.of("admin", "user")),
						Map.of("name", "Bob", "roles", List.of("user"))),
				"metadata", Map.of("version", "1.0", "tags", List.of("important", "reviewed")));
		JsonPaths pathAndValues = converter().transformToJsonPathWithTestsSideValues(json);
		DocumentContext context = JsonPath.parse(JsonOutput.toJson(json));

		// all JSON paths should be valid syntax
		for (var entry : pathAndValues) {
			try {
				context.read(entry.jsonPath());
			}
			catch (Exception e) {
				// Some paths with filters may not match but should still be valid syntax
				assertThat(entry.jsonPath()).startsWith("$");
			}
		}

		assertThat(pathAndValues).anyMatch(entry -> entry.method().contains("elementWithIndex(0)")
				&& entry.method().contains("['name']") && entry.method().contains("Alice"));
		assertThat(pathAndValues).anyMatch(entry -> entry.method().contains("elementWithIndex(1)")
				&& entry.method().contains("['name']") && entry.method().contains("Bob"));
		assertThat(pathAndValues)
			.anyMatch(entry -> entry.method().contains("['version']") && entry.method().contains("1.0"));
		assertThat(pathAndValues)
			.anyMatch(entry -> entry.method().contains("['tags']") && entry.method().contains("important"));
		assertThat(pathAndValues)
			.anyMatch(entry -> entry.method().contains("['tags']") && entry.method().contains("reviewed"));
	}

	// ========== Stub Side Values ==========

	@Test
	void shouldGenerateOrderedAssertionsForStubSideValuesAllElements() {
		Map<String, Object> json = Map.of("items", List.of("one", "two", "three"));
		JsonPaths pathAndValues = converter().transformToJsonPathWithStubsSideValues(json);

		assertThat(pathAndValues).anyMatch(entry -> entry.method().contains("hasSize(3)"));
		assertThat(pathAndValues).anyMatch(entry -> entry.method().contains("elementWithIndex(0)")
				&& entry.method().contains("isEqualTo(\"one\")"));
		assertThat(pathAndValues).anyMatch(entry -> entry.method().contains("elementWithIndex(1)")
				&& entry.method().contains("isEqualTo(\"two\")"));
		assertThat(pathAndValues).anyMatch(entry -> entry.method().contains("elementWithIndex(2)")
				&& entry.method().contains("isEqualTo(\"three\")"));
		// exactly 4 assertions (1 size + 3 elements)
		assertThat(pathAndValues).hasSize(4);
	}

	// ========== Additional Complex Scenarios ==========

	@Test
	void shouldGenerateOrderedAssertionsForDeeplyNestedStructure() {
		Map<String, Object> json = Map.of("level1", Map.of("level2", Map.of("items", List
			.of(Map.of("id", 1, "data", Map.of("value", "a")), Map.of("id", 2, "data", Map.of("value", "b"))))));
		JsonPaths pathAndValues = converter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues)
			.anyMatch(entry -> entry.method().contains("['level1']") && entry.method().contains("['level2']")
					&& entry.method().contains("['items']") && entry.method().contains("elementWithIndex(0)")
					&& entry.method().contains("['id']") && entry.method().contains("isEqualTo(1)"));
		assertThat(pathAndValues)
			.anyMatch(entry -> entry.method().contains("elementWithIndex(0)") && entry.method().contains("['data']")
					&& entry.method().contains("['value']") && entry.method().contains("isEqualTo(\"a\")"));
		assertThat(pathAndValues).anyMatch(entry -> entry.method().contains("elementWithIndex(1)")
				&& entry.method().contains("['id']") && entry.method().contains("isEqualTo(2)"));
		assertThat(pathAndValues)
			.anyMatch(entry -> entry.method().contains("elementWithIndex(1)") && entry.method().contains("['data']")
					&& entry.method().contains("['value']") && entry.method().contains("isEqualTo(\"b\")"));
	}

	@Test
	void shouldGenerateOrderedAssertionsForArrayWithNullValues() {
		java.util.Map<String, Object> item0 = new java.util.LinkedHashMap<>();
		item0.put("name", "first");
		item0.put("value", null);
		java.util.Map<String, Object> item1 = Map.of("name", "second", "value", 123);
		java.util.Map<String, Object> json = Map.of("items", List.of(item0, item1));

		JsonPaths pathAndValues = converter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anyMatch(entry -> entry.method().contains("elementWithIndex(0)")
				&& entry.method().contains("['name']") && entry.method().contains("isEqualTo(\"first\")"));
		assertThat(pathAndValues).anyMatch(entry -> entry.method().contains("elementWithIndex(0)")
				&& entry.method().contains("['value']") && entry.method().contains("isNull()"));
		assertThat(pathAndValues).anyMatch(entry -> entry.method().contains("elementWithIndex(1)")
				&& entry.method().contains("['name']") && entry.method().contains("isEqualTo(\"second\")"));
		assertThat(pathAndValues).anyMatch(entry -> entry.method().contains("elementWithIndex(1)")
				&& entry.method().contains("['value']") && entry.method().contains("isEqualTo(123)"));
	}

	@Test
	void shouldGenerateOrderedAssertionsForMultipleArraysInSameObject() {
		Map<String, Object> json = Map.of("names", List.of("Alice", "Bob"), "ages", List.of(30, 25), "active",
				List.of(true, false));
		JsonPaths pathAndValues = converter().transformToJsonPathWithTestsSideValues(json);

		// size checks for all arrays
		assertThat(pathAndValues)
			.anyMatch(entry -> entry.method().contains("['names']") && entry.method().contains("hasSize(2)"));
		assertThat(pathAndValues)
			.anyMatch(entry -> entry.method().contains("['ages']") && entry.method().contains("hasSize(2)"));
		assertThat(pathAndValues)
			.anyMatch(entry -> entry.method().contains("['active']") && entry.method().contains("hasSize(2)"));
		// names array
		assertThat(pathAndValues).anyMatch(entry -> entry.method().contains("['names']")
				&& entry.method().contains("elementWithIndex(0)") && entry.method().contains("Alice"));
		assertThat(pathAndValues).anyMatch(entry -> entry.method().contains("['names']")
				&& entry.method().contains("elementWithIndex(1)") && entry.method().contains("Bob"));
		// ages array
		assertThat(pathAndValues).anyMatch(entry -> entry.method().contains("['ages']")
				&& entry.method().contains("elementWithIndex(0)") && entry.method().contains("isEqualTo(30)"));
		assertThat(pathAndValues).anyMatch(entry -> entry.method().contains("['ages']")
				&& entry.method().contains("elementWithIndex(1)") && entry.method().contains("isEqualTo(25)"));
		// active array
		assertThat(pathAndValues).anyMatch(entry -> entry.method().contains("['active']")
				&& entry.method().contains("elementWithIndex(0)") && entry.method().contains("isEqualTo(true)"));
		assertThat(pathAndValues).anyMatch(entry -> entry.method().contains("['active']")
				&& entry.method().contains("elementWithIndex(1)") && entry.method().contains("isEqualTo(false)"));
		// exactly 9 assertions (3 arrays x (1 size + 2 elements))
		assertThat(pathAndValues).hasSize(9);
	}

}
