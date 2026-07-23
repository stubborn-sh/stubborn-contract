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
import java.util.regex.Pattern;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.toomuchcoding.jsonassert.JsonAssertion;
import groovy.json.JsonOutput;
import groovy.json.JsonSlurper;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import sh.stubborn.contract.spec.internal.BodyMatcher;
import sh.stubborn.contract.spec.internal.MatchingType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonToJsonPathsConverterTests {

	private final JsonSlurper slurper = new JsonSlurper();

	@AfterEach
	void restoreSystemProperties() {
		System.clearProperty("spring.cloud.contract.verifier.assert.size");
	}

	// ==================== List-as-root tests ====================

	static List<String> jsonWithListAsRoot() {
		return List.of("""
				[ {
						"some" : {
							"nested" : {
								"json" : "with value",
								"anothervalue": 4,
								"withlist" : [
									{ "name" :"name1"} , {"name": "name2"}, {"anothernested": { "name": "name3"} }
								]
							}
						}
					},
					{
						"someother" : {
							"nested" : {
								"json" : "with value",
								"anothervalue": 4,
								"withlist" : [
									{ "name" :"name1"} , {"name": "name2"}
								]
							}
						}
					}
				]
				""", """
				[{
					"someother" : {
						"nested" : {
							"json" : "with value",
							"anothervalue": 4,
							"withlist" : [
								{ "name" :"name1"} , {"name": "name2"}
							]
						}
					}
				},
				 {
					"some" : {
						"nested" : {
							"json" : "with value",
							"anothervalue": 4,
							"withlist" : [
								 {"name": "name2"}, {"anothernested": { "name": "name3"} }, { "name" :"name1"}
							]
						}
					}
				}]""");
	}

	@ParameterizedTest
	@MethodSource("jsonWithListAsRoot")
	void shouldConvertAJsonWithListAsRootToAMapOfPathToValue(String json) {
		JsonPaths pathAndValues = new JsonToJsonPathsConverter()
			.transformToJsonPathWithTestsSideValues(slurper.parseText(json));

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array().field(\"['some']\").field(\"['nested']\").field(\"['json']\").isEqualTo(\"with value\")");
			assertThat(entry.jsonPath()).isEqualTo("$[*].['some'].['nested'][?(@.['json'] == 'with value')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array().field(\"['some']\").field(\"['nested']\").field(\"['anothervalue']\").isEqualTo(4)");
			assertThat(entry.jsonPath()).isEqualTo("$[*].['some'].['nested'][?(@.['anothervalue'] == 4)]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array().field(\"['some']\").field(\"['nested']\").array(\"['withlist']\").contains(\"['name']\").isEqualTo(\"name1\")");
			assertThat(entry.jsonPath())
				.isEqualTo("$[*].['some'].['nested'].['withlist'][*][?(@.['name'] == 'name1')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array().field(\"['some']\").field(\"['nested']\").array(\"['withlist']\").contains(\"['name']\").isEqualTo(\"name2\")");
			assertThat(entry.jsonPath())
				.isEqualTo("$[*].['some'].['nested'].['withlist'][*][?(@.['name'] == 'name2')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array().field(\"['some']\").field(\"['nested']\").array(\"['withlist']\").field(\"['anothernested']\").field(\"['name']\").isEqualTo(\"name3\")");
			assertThat(entry.jsonPath())
				.isEqualTo("$[*].['some'].['nested'].['withlist'][*].['anothernested'][?(@.['name'] == 'name3')]");
		});

		assertThatJsonPathsInMapAreValid(json, pathAndValues);
	}

	// ==================== Map-as-root tests ====================

	@Test
	void shouldConvertAJsonWithAMapAsRootToAMapOfPathToValue() {
		String json = """
				 {
						"some" : {
							"nested" : {
								"json" : "with value",
								"anothervalue": 4,
								"withlist" : [
									{ "name" :"name1"} , {"name": "name2"}
								]
							}
						}
					}
				""";
		JsonPaths pathAndValues = new JsonToJsonPathsConverter()
			.transformToJsonPathWithTestsSideValues(slurper.parseText(json));

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method())
				.isEqualTo(".field(\"['some']\").field(\"['nested']\").field(\"['json']\").isEqualTo(\"with value\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['some'].['nested'][?(@.['json'] == 'with value')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method())
				.isEqualTo(".field(\"['some']\").field(\"['nested']\").field(\"['anothervalue']\").isEqualTo(4)");
			assertThat(entry.jsonPath()).isEqualTo("$.['some'].['nested'][?(@.['anothervalue'] == 4)]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".field(\"['some']\").field(\"['nested']\").array(\"['withlist']\").contains(\"['name']\").isEqualTo(\"name1\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['some'].['nested'].['withlist'][*][?(@.['name'] == 'name1')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".field(\"['some']\").field(\"['nested']\").array(\"['withlist']\").contains(\"['name']\").isEqualTo(\"name2\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['some'].['nested'].['withlist'][*][?(@.['name'] == 'name2')]");
		});

		assertThatJsonPathsInMapAreValid(json, pathAndValues);
	}

	@Test
	void shouldConvertAJsonWithAList() {
		String json = """
				 {
						"items" : ["HOP"]
					}
				""";
		JsonPaths pathAndValues = new JsonToJsonPathsConverter()
			.transformToJsonPathWithTestsSideValues(slurper.parseText(json));

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".array(\"['items']\").arrayField().isEqualTo(\"HOP\").value()");
			assertThat(entry.jsonPath()).isEqualTo("$.['items'][?(@ == 'HOP')]");
		});

		assertThatJsonPathsInMapAreValid(json, pathAndValues);
	}

	@Test
	void shouldConvertAJsonWithNullAndBooleanValues() {
		String json = """
				 {
						"property1" : null,
						"property2" : true
					}
				""";
		JsonPaths pathAndValues = new JsonToJsonPathsConverter()
			.transformToJsonPathWithTestsSideValues(slurper.parseText(json));

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['property1']\").isNull()");
			assertThat(entry.jsonPath()).isEqualTo("$[?(@.['property1'] == null)]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['property2']\").isEqualTo(true)");
			assertThat(entry.jsonPath()).isEqualTo("$[?(@.['property2'] == true)]");
		});
	}

	@Test
	void shouldConvertNumbersMap() {
		String json = """
				 {
				     "extensions": {"7":28.00,"14":41.00,"30":60.00}
				     }
				""";
		JsonPaths pathAndValues = new JsonToJsonPathsConverter()
			.transformToJsonPathWithTestsSideValues(slurper.parseText(json));

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['extensions']\").field(\"['7']\").isEqualTo(28.00)");
			assertThat(entry.jsonPath()).isEqualTo("$.['extensions'][?(@.['7'] == 28.00)]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['extensions']\").field(\"['14']\").isEqualTo(41.00)");
			assertThat(entry.jsonPath()).isEqualTo("$.['extensions'][?(@.['14'] == 41.00)]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['extensions']\").field(\"['30']\").isEqualTo(60.00)");
			assertThat(entry.jsonPath()).isEqualTo("$.['extensions'][?(@.['30'] == 60.00)]");
		});

		assertThatJsonPathsInMapAreValid(json, pathAndValues);
	}

	@Test
	void shouldConvertAJsonWithAListOfErrors() {
		String json = """
				 {
						"errors" : [
							{ "property" : "email", "message" : "inconsistent value" },
							{ "property" : "email", "message" : "inconsistent value2" }
						]
					}
				""";
		JsonPaths pathAndValues = new JsonToJsonPathsConverter()
			.transformToJsonPathWithTestsSideValues(slurper.parseText(json));

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['errors']\").contains(\"['property']\").isEqualTo(\"email\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['errors'][*][?(@.['property'] == 'email')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['errors']\").contains(\"['message']\").isEqualTo(\"inconsistent value\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['errors'][*][?(@.['message'] == 'inconsistent value')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['errors']\").contains(\"['message']\").isEqualTo(\"inconsistent value2\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['errors'][*][?(@.['message'] == 'inconsistent value2')]");
		});

		assertThatJsonPathsInMapAreValid(json, pathAndValues);
	}

	@Test
	void shouldConvertAMapJsonWithARegexPattern() {
		List<Map<String, Object>> json = List.of(
				Map.of("some",
						Map.of("nested",
								Map.of("json", "with value", "anothervalue", 4, "withlist",
										List.of(Map.of("name", "name2"), Map.of("name", "name1"),
												Map.of("anothernested", Map.of("name", Pattern.compile("[a-zA-Z]+"))),
												Map.of("age", "123456789"))))),
				Map.of("someother", Map.of("nested", Map.of("json", "with value", "anothervalue", 4, "withlist",
						List.of(Map.of("name", "name2"), Map.of("name", "name1"))))));

		JsonPaths pathAndValues = new JsonToJsonPathsConverter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array().field(\"['some']\").field(\"['nested']\").field(\"['json']\").isEqualTo(\"with value\")");
			assertThat(entry.jsonPath()).isEqualTo("$[*].['some'].['nested'][?(@.['json'] == 'with value')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array().field(\"['some']\").field(\"['nested']\").field(\"['anothervalue']\").isEqualTo(4)");
			assertThat(entry.jsonPath()).isEqualTo("$[*].['some'].['nested'][?(@.['anothervalue'] == 4)]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array().field(\"['some']\").field(\"['nested']\").array(\"['withlist']\").contains(\"['name']\").isEqualTo(\"name1\")");
			assertThat(entry.jsonPath())
				.isEqualTo("$[*].['some'].['nested'].['withlist'][*][?(@.['name'] == 'name1')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array().field(\"['some']\").field(\"['nested']\").array(\"['withlist']\").contains(\"['name']\").isEqualTo(\"name2\")");
			assertThat(entry.jsonPath())
				.isEqualTo("$[*].['some'].['nested'].['withlist'][*][?(@.['name'] == 'name2')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array().field(\"['some']\").field(\"['nested']\").array(\"['withlist']\").field(\"['anothernested']\").field(\"['name']\").matches(\"[a-zA-Z]+\")");
			assertThat(entry.jsonPath())
				.isEqualTo("$[*].['some'].['nested'].['withlist'][*].['anothernested'][?(@.['name'] =~ /[a-zA-Z]+/)]");
		});

		// Mutate and re-verify (mimics the Groovy `when: json.some...name = "Kowalski"`
		// block)
		// The pathAndValues are already built; we verify the paths are still valid
		// against mutated JSON
		// by rebuilding a JSON string with "Kowalski" substituted for the regex
		String mutatedJson = JsonOutput.prettyPrint(JsonOutput.toJson(List.of(
				Map.of("some", Map.of("nested", Map.of("json", "with value", "anothervalue", 4, "withlist",
						List.of(Map.of("name", "name2"), Map.of("name", "name1"),
								Map.of("anothernested", Map.of("name", "Kowalski")), Map.of("age", "123456789"))))),
				Map.of("someother", Map.of("nested", Map.of("json", "with value", "anothervalue", 4, "withlist",
						List.of(Map.of("name", "name2"), Map.of("name", "name1"))))))));
		assertThatJsonPathsInMapAreValid(mutatedJson, pathAndValues);
	}

	// ==================== Simple response body tests ====================

	@Test
	void shouldGenerateAssertionsForSimpleResponseBody() {
		String json = """
				{
				"property1": "a",
				"property2": "b"
				}""";
		JsonPaths pathAndValues = new JsonToJsonPathsConverter()
			.transformToJsonPathWithTestsSideValues(slurper.parseText(json));

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['property1']\").isEqualTo(\"a\")");
			assertThat(entry.jsonPath()).isEqualTo("$[?(@.['property1'] == 'a')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['property2']\").isEqualTo(\"b\")");
			assertThat(entry.jsonPath()).isEqualTo("$[?(@.['property2'] == 'b')]");
		});
		assertThat(pathAndValues).hasSize(2);
	}

	@Test
	void shouldGenerateAssertionsForNullAndBooleanValues() {
		String json = """
				{
				"property1": "true",
				"property2": null,
				"property3": false
				}""";
		JsonPaths pathAndValues = new JsonToJsonPathsConverter()
			.transformToJsonPathWithTestsSideValues(slurper.parseText(json));

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['property1']\").isEqualTo(\"true\")");
			assertThat(entry.jsonPath()).isEqualTo("$[?(@.['property1'] == 'true')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['property2']\").isNull()");
			assertThat(entry.jsonPath()).isEqualTo("$[?(@.['property2'] == null)]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['property3']\").isEqualTo(false)");
			assertThat(entry.jsonPath()).isEqualTo("$[?(@.['property3'] == false)]");
		});
		assertThat(pathAndValues).hasSize(3);
	}

	@Test
	void shouldGenerateAssertionsForSimpleResponseBodyConstructedFromMapWithAList() {
		Map<String, Object> json = Map.of("property1", "a", "property2",
				List.of(Map.of("a", "sth"), Map.of("b", "sthElse")));
		JsonPaths pathAndValues = new JsonToJsonPathsConverter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['property1']\").isEqualTo(\"a\")");
			assertThat(entry.jsonPath()).isEqualTo("$[?(@.['property1'] == 'a')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".array(\"['property2']\").contains(\"['a']\").isEqualTo(\"sth\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['property2'][*][?(@.['a'] == 'sth')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['property2']\").contains(\"['b']\").isEqualTo(\"sthElse\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['property2'][*][?(@.['b'] == 'sthElse')]");
		});
		assertThat(pathAndValues).hasSize(3);
	}

	@Test
	void shouldGenerateAssertionsForSimpleResponseBodyConstructedFromMapWithAListWithArraySizeCheck() {
		System.setProperty("spring.cloud.contract.verifier.assert.size", "true");
		Map<String, Object> json = Map.of("property1", "a", "property2",
				List.of(Map.of("a", "sth"), Map.of("b", "sthElse")));
		JsonPaths pathAndValues = new JsonToJsonPathsConverter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['property1']\").isEqualTo(\"a\")");
			assertThat(entry.jsonPath()).isEqualTo("$[?(@.['property1'] == 'a')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['property2']\").elementWithIndex(0).field(\"['a']\").isEqualTo(\"sth\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['property2'][0][?(@.['a'] == 'sth')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['property2']\").elementWithIndex(1).field(\"['b']\").isEqualTo(\"sthElse\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['property2'][1][?(@.['b'] == 'sthElse')]");
		});
		assertThat(pathAndValues).hasSize(3);
	}

	@Test
	void shouldGenerateAssertionsForAResponseBodyContainingMapWithIntegersAsKeys() {
		Map<String, Object> json = Map.of("property", Map.of(14, 0.0, 7, 0.0));
		JsonPaths pathAndValues = new JsonToJsonPathsConverter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['property']\").field(7).isEqualTo(0.0)");
			assertThat(entry.jsonPath()).isEqualTo("$.['property'][?(@.7 == 0.0)]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['property']\").field(14).isEqualTo(0.0)");
			assertThat(entry.jsonPath()).isEqualTo("$.['property'][?(@.14 == 0.0)]");
		});
		assertThat(pathAndValues).hasSize(2);
	}

	@Test
	void shouldGenerateAssertionsForArrayInResponseBody() {
		String json = """
				[
				{
					"property1": "a"
				},
				{
					"property2": "b"
				}]""";
		JsonPaths pathAndValues = new JsonToJsonPathsConverter()
			.transformToJsonPathWithTestsSideValues(slurper.parseText(json));

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".array().contains(\"['property1']\").isEqualTo(\"a\")");
			assertThat(entry.jsonPath()).isEqualTo("$[*][?(@.['property1'] == 'a')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".array().contains(\"['property2']\").isEqualTo(\"b\")");
			assertThat(entry.jsonPath()).isEqualTo("$[*][?(@.['property2'] == 'b')]");
		});
		assertThat(pathAndValues).hasSize(2);
	}

	@Test
	void shouldGenerateAssertionsForArrayInResponseBodyWithArraySizeCheck() {
		System.setProperty("spring.cloud.contract.verifier.assert.size", "true");
		String json = """
				[
				{
					"property1": "a"
				},
				{
					"property2": "b"
				}]""";
		JsonPaths pathAndValues = new JsonToJsonPathsConverter()
			.transformToJsonPathWithTestsSideValues(slurper.parseText(json));

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method())
				.isEqualTo(".array().elementWithIndex(0).field(\"['property1']\").isEqualTo(\"a\")");
			assertThat(entry.jsonPath()).isEqualTo("$[*][0][?(@.['property1'] == 'a')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method())
				.isEqualTo(".array().elementWithIndex(1).field(\"['property2']\").isEqualTo(\"b\")");
			assertThat(entry.jsonPath()).isEqualTo("$[*][1][?(@.['property2'] == 'b')]");
		});
		assertThat(pathAndValues).hasSize(2);
	}

	@Test
	void shouldGenerateAssertionsForArrayInsideResponseBodyElement() {
		String json = """
				{
				"property1": [
				{ "property2": "test1"},
				{ "property3": "test2"}
				]
				}""";
		JsonPaths pathAndValues = new JsonToJsonPathsConverter()
			.transformToJsonPathWithTestsSideValues(slurper.parseText(json));

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['property1']\").contains(\"['property2']\").isEqualTo(\"test1\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['property1'][*][?(@.['property2'] == 'test1')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['property1']\").contains(\"['property3']\").isEqualTo(\"test2\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['property1'][*][?(@.['property3'] == 'test2')]");
		});
		assertThat(pathAndValues).hasSize(2);
	}

	@Test
	void shouldGenerateAssertionsForArrayInsideResponseBodyElementWithArraySizeCheck() {
		System.setProperty("spring.cloud.contract.verifier.assert.size", "true");
		String json = """
				{
				"property1": [
				{ "property2": "test1"},
				{ "property3": "test2"}
				]
				}""";
		JsonPaths pathAndValues = new JsonToJsonPathsConverter()
			.transformToJsonPathWithTestsSideValues(slurper.parseText(json));

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array(\"['property1']\").elementWithIndex(0).field(\"['property2']\").isEqualTo(\"test1\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['property1'][0][?(@.['property2'] == 'test1')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array(\"['property1']\").elementWithIndex(1).field(\"['property3']\").isEqualTo(\"test2\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['property1'][1][?(@.['property3'] == 'test2')]");
		});
		assertThat(pathAndValues).hasSize(2);
	}

	@Test
	void shouldGenerateAssertionsForNestedObjectsInResponseBody() {
		String json = """
				{
				"property1": "a",
				"property2": {"property3": "b"}
				}""";
		JsonPaths pathAndValues = new JsonToJsonPathsConverter()
			.transformToJsonPathWithTestsSideValues(slurper.parseText(json));

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['property2']\").field(\"['property3']\").isEqualTo(\"b\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['property2'][?(@.['property3'] == 'b')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['property1']\").isEqualTo(\"a\")");
			assertThat(entry.jsonPath()).isEqualTo("$[?(@.['property1'] == 'a')]");
		});
		assertThat(pathAndValues).hasSize(2);
	}

	@Test
	void shouldGenerateRegexAssertionsForMapObjectsInResponseBody() {
		Map<String, Object> json = Map.of("property1", "a", "property2", Pattern.compile("[0-9]{3}"));
		JsonPaths pathAndValues = new JsonToJsonPathsConverter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['property2']\").matches(\"[0-9]{3}\")");
			assertThat(entry.jsonPath()).isEqualTo("$[?(@.['property2'] =~ /[0-9]{3}/)]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['property1']\").isEqualTo(\"a\")");
			assertThat(entry.jsonPath()).isEqualTo("$[?(@.['property1'] == 'a')]");
		});
		assertThat(pathAndValues).hasSize(2);
	}

	@Test
	void shouldGenerateEscapedRegexAssertionsForStringObjectsInResponseBody() {
		Map<String, Object> json = Map.of("property2", Pattern.compile("\\d+"));
		JsonPaths pathAndValues = new JsonToJsonPathsConverter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['property2']\").matches(\"\\\\d+\")");
			assertThat(entry.jsonPath()).isEqualTo("$[?(@.['property2'] =~ /\\d+/)]");
		});
		assertThat(pathAndValues).hasSize(1);
	}

	@Test
	void shouldWorkWithMoreComplexStuffAndJsonPaths() {
		Map<String, Object> json = Map.of("errors",
				List.of(Map.of("property", "bank_account_number", "message", "incorrect_format")));
		JsonPaths pathAndValues = new JsonToJsonPathsConverter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['errors']\").contains(\"['property']\").isEqualTo(\"bank_account_number\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['errors'][*][?(@.['property'] == 'bank_account_number')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['errors']\").contains(\"['message']\").isEqualTo(\"incorrect_format\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['errors'][*][?(@.['message'] == 'incorrect_format')]");
		});
		assertThat(pathAndValues).hasSize(2);
	}

	@Test
	void shouldWorkWithMoreComplexStuffAndJsonPathsWithArraySizeCheck() {
		System.setProperty("spring.cloud.contract.verifier.assert.size", "true");
		Map<String, Object> json = Map.of("errors",
				List.of(Map.of("property", "bank_account_number", "message", "incorrect_format")));
		JsonPaths pathAndValues = new JsonToJsonPathsConverter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array(\"['errors']\").elementWithIndex(0).field(\"['property']\").isEqualTo(\"bank_account_number\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['errors'][0][?(@.['property'] == 'bank_account_number')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array(\"['errors']\").elementWithIndex(0).field(\"['message']\").isEqualTo(\"incorrect_format\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['errors'][0][?(@.['message'] == 'incorrect_format')]");
		});
		assertThat(pathAndValues).hasSize(2);
	}

	@Test
	void shouldManageToParseADoubleArray() {
		String json = """
				[{
					"place":
					{
						"bounding_box":
						{
							"coordinates":
								[[
									[-77.119759,38.995548],
									[-76.909393,38.791645]
								]]
						}
					}
				}]
				""";
		JsonPaths pathAndValues = new JsonToJsonPathsConverter()
			.transformToJsonPathWithTestsSideValues(slurper.parseText(json));

		DocumentContext context = JsonPath.parse(json);
		for (var entry : pathAndValues) {
			assertThat(context.read(entry.jsonPath(), JSONArray.class)).isNotEmpty();
		}
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array().field(\"['place']\").field(\"['bounding_box']\").array(\"['coordinates']\").array().array().arrayField().isEqualTo(38.995548)");
			assertThat(entry.jsonPath())
				.isEqualTo("$[*].['place'].['bounding_box'].['coordinates'][*][*][?(@ == 38.995548)]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array().field(\"['place']\").field(\"['bounding_box']\").array(\"['coordinates']\").array().array().arrayField().isEqualTo(-77.119759)");
			assertThat(entry.jsonPath())
				.isEqualTo("$[*].['place'].['bounding_box'].['coordinates'][*][*][?(@ == -77.119759)]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array().field(\"['place']\").field(\"['bounding_box']\").array(\"['coordinates']\").array().array().arrayField().isEqualTo(-76.909393)");
			assertThat(entry.jsonPath())
				.isEqualTo("$[*].['place'].['bounding_box'].['coordinates'][*][*][?(@ == -76.909393)]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array().field(\"['place']\").field(\"['bounding_box']\").array(\"['coordinates']\").array().array().arrayField().isEqualTo(38.791645)");
			assertThat(entry.jsonPath())
				.isEqualTo("$[*].['place'].['bounding_box'].['coordinates'][*][*][?(@ == 38.791645)]");
		});
		assertThat(pathAndValues).hasSize(4);

		for (var entry : pathAndValues) {
			JsonAssertion.assertThat(json).matchesJsonPath(entry.jsonPath());
		}
	}

	@Test
	void shouldManageToParseADoubleArrayWithArraySizeCheck() {
		System.setProperty("spring.cloud.contract.verifier.assert.size", "true");
		String json = """
				[{
					"place":
					{
						"bounding_box":
						{
							"coordinates":
								[[
									[-77.119759,38.995548],
									[-76.909393,38.791645]
								]]
						}
					}
				}]
				""";
		JsonPaths pathAndValues = new JsonToJsonPathsConverter()
			.transformToJsonPathWithTestsSideValues(slurper.parseText(json));

		DocumentContext context = JsonPath.parse(json);
		for (var entry : pathAndValues) {
			assertThat((Object) context.read(entry.jsonPath())).isNotNull();
		}

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array().elementWithIndex(0).field(\"['place']\").field(\"['bounding_box']\").array(\"['coordinates']\").elementWithIndex(0).elementWithIndex(0).hasSize(2)");
			assertThat(entry.jsonPath()).isEqualTo("$[*][0].['place'].['bounding_box'].['coordinates'][0][0]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array().elementWithIndex(0).field(\"['place']\").field(\"['bounding_box']\").array(\"['coordinates']\").elementWithIndex(0).elementWithIndex(0).elementWithIndex(0).isEqualTo(-77.119759)");
			assertThat(entry.jsonPath()).isEqualTo("$[*][0].['place'].['bounding_box'].['coordinates'][0][0][0]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array().elementWithIndex(0).field(\"['place']\").field(\"['bounding_box']\").array(\"['coordinates']\").elementWithIndex(0).elementWithIndex(0).elementWithIndex(1).isEqualTo(38.995548)");
			assertThat(entry.jsonPath()).isEqualTo("$[*][0].['place'].['bounding_box'].['coordinates'][0][0][1]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array().elementWithIndex(0).field(\"['place']\").field(\"['bounding_box']\").array(\"['coordinates']\").elementWithIndex(0).elementWithIndex(1).hasSize(2)");
			assertThat(entry.jsonPath()).isEqualTo("$[*][0].['place'].['bounding_box'].['coordinates'][0][1]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array().elementWithIndex(0).field(\"['place']\").field(\"['bounding_box']\").array(\"['coordinates']\").elementWithIndex(0).elementWithIndex(1).elementWithIndex(0).isEqualTo(-76.909393)");
			assertThat(entry.jsonPath()).isEqualTo("$[*][0].['place'].['bounding_box'].['coordinates'][0][1][0]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(
					".array().elementWithIndex(0).field(\"['place']\").field(\"['bounding_box']\").array(\"['coordinates']\").elementWithIndex(0).elementWithIndex(1).elementWithIndex(1).isEqualTo(38.791645)");
			assertThat(entry.jsonPath()).isEqualTo("$[*][0].['place'].['bounding_box'].['coordinates'][0][1][1]");
		});
		assertThat(pathAndValues).hasSize(6);
	}

	// ==================== convertJsonPathAndRegexToAJsonPath tests ====================

	@Test
	void shouldConvertAJsonPathWithRegexToARegexCheckingJsonPath() {
		String jsonPath = "$.a.b.c.d";
		String regexPattern = ".*";
		assertThat(JsonToJsonPathsConverter
			.convertJsonPathAndRegexToAJsonPath(matcher(MatchingType.REGEX, jsonPath, regexPattern)))
			.isEqualTo("$.a.b.c[?(@.d =~ /(.*)/)]");
	}

	@Test
	void shouldConvertAJsonPathWithRegexToARegexCheckingJsonPathThatHasASlashInIt() {
		String jsonPath = "$.a.b.c.d";
		String regexPattern = "/.*/";
		assertThat(JsonToJsonPathsConverter
			.convertJsonPathAndRegexToAJsonPath(matcher(MatchingType.REGEX, jsonPath, regexPattern)))
			.isEqualTo("$.a.b.c[?(@.d =~ /(\\\\/.*\\\\/)/)]");
	}

	@Test
	void shouldConvertAJsonPathWithValueToAnEqualityCheckingJsonPathWithoutQuotesForNumbers() {
		String jsonPath = "$.a.b.c.d";
		Map<String, Object> body = Map.of("a", Map.of("b", Map.of("c", Map.of("d", 1234))));
		assertThat(JsonToJsonPathsConverter
			.convertJsonPathAndRegexToAJsonPath(matcher(MatchingType.EQUALITY, jsonPath, null), body))
			.isEqualTo("$.a.b.c[?(@.d == 1234)]");
	}

	@Test
	void shouldConvertAJsonPathWithValueToAnEqualityCheckingForAKeyWithDots() {
		String jsonPath = "$.a.b.c.['d.e']";
		Map<String, Object> body = Map.of("a", Map.of("b", Map.of("c", Map.of("d.e", 1234))));
		assertThat(JsonToJsonPathsConverter
			.convertJsonPathAndRegexToAJsonPath(matcher(MatchingType.EQUALITY, jsonPath, null), body))
			.isEqualTo("$.a.b.c[?(@.['d.e'] == 1234)]");
	}

	@Test
	void shouldConvertAJsonPathWithValueToAnEqualityCheckingJsonPathWithQuotesForStrings() {
		String jsonPath = "$.a.b.c.d";
		Map<String, Object> body = Map.of("a", Map.of("b", Map.of("c", Map.of("d", "foo"))));
		assertThat(JsonToJsonPathsConverter
			.convertJsonPathAndRegexToAJsonPath(matcher(MatchingType.EQUALITY, jsonPath, null), body))
			.isEqualTo("$.a.b.c[?(@.d == 'foo')]");
	}

	@Test
	void shouldReturnThePathIfNoValueIsProvided() {
		String jsonPath = "$.a.b.c.d";
		assertThat(JsonToJsonPathsConverter
			.convertJsonPathAndRegexToAJsonPath(matcher(MatchingType.REGEX, jsonPath, null))).isEqualTo("$.a.b.c.d");
	}

	@Test
	void shouldThrowAnExceptionWhenNullBodyIsPassedToCheckForEquality() {
		String jsonPath = "$.a.b.c.d";
		assertThatThrownBy(() -> JsonToJsonPathsConverter
			.convertJsonPathAndRegexToAJsonPath(matcher(MatchingType.EQUALITY, jsonPath, null), (Object) null))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Body");
	}

	@Test
	void shouldThrowAnExceptionWhenNonexistingJsonPathIsPassedToCheckForEquality() {
		String jsonPath = "$.a.b.c.d";
		Map<String, Object> body = Map.of("foo", "bar");
		assertThatThrownBy(() -> JsonToJsonPathsConverter
			.convertJsonPathAndRegexToAJsonPath(matcher(MatchingType.EQUALITY, jsonPath, null), body))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("not found");
	}

	// ==================== Empty map / object tests ====================

	@Test
	void shouldGenerateAssertionForEmptyMap() {
		Map<String, Object> json = Map.of("aMap", Map.of("foo", "bar"), "anEmptyMap", Map.of());
		JsonPaths pathAndValues = new JsonToJsonPathsConverter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['aMap']\").field(\"['foo']\").isEqualTo(\"bar\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['aMap'][?(@.['foo'] == 'bar')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['anEmptyMap']\").isEmpty()");
			assertThat(entry.jsonPath()).isEqualTo("$.['anEmptyMap']");
		});
		assertThat(pathAndValues).hasSize(2);
	}

	@Test
	void shouldGenerateAssertionForEmptyObject() {
		String json = """
				{
				"aMap": {"foo": "bar"},
				"anEmptyMap": {}
				}""";
		JsonPaths pathAndValues = new JsonToJsonPathsConverter()
			.transformToJsonPathWithTestsSideValues(slurper.parseText(json));

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['aMap']\").field(\"['foo']\").isEqualTo(\"bar\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['aMap'][?(@.['foo'] == 'bar')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".field(\"['anEmptyMap']\").isEmpty()");
			assertThat(entry.jsonPath()).isEqualTo("$.['anEmptyMap']");
		});
		assertThat(pathAndValues).hasSize(2);
	}

	// ==================== Array size check (assert.size=true) tests ====================

	@Test
	void shouldGenerateOrderedAssertionsForPrimitiveArrayWhenAssertSizeIsEnabled() {
		System.setProperty("spring.cloud.contract.verifier.assert.size", "true");
		Map<String, Object> json = Map.of("items", List.of("first", "second", "third"));
		JsonPaths pathAndValues = new JsonToJsonPathsConverter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".array(\"['items']\").hasSize(3)");
			assertThat(entry.jsonPath()).isEqualTo("$.['items']");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".array(\"['items']\").elementWithIndex(0).isEqualTo(\"first\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['items'][0]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".array(\"['items']\").elementWithIndex(1).isEqualTo(\"second\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['items'][1]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method()).isEqualTo(".array(\"['items']\").elementWithIndex(2).isEqualTo(\"third\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['items'][2]");
		});
		assertThat(pathAndValues).hasSize(4);
	}

	@Test
	void shouldGenerateOrderedAssertionsForArrayOfObjectsWhenAssertSizeIsEnabled() {
		System.setProperty("spring.cloud.contract.verifier.assert.size", "true");
		Map<String, Object> json = Map.of("users",
				List.of(Map.of("name", "Alice", "age", 30), Map.of("name", "Bob", "age", 25)));
		JsonPaths pathAndValues = new JsonToJsonPathsConverter().transformToJsonPathWithTestsSideValues(json);

		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['users']\").elementWithIndex(0).field(\"['name']\").isEqualTo(\"Alice\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['users'][0][?(@.['name'] == 'Alice')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['users']\").elementWithIndex(0).field(\"['age']\").isEqualTo(30)");
			assertThat(entry.jsonPath()).isEqualTo("$.['users'][0][?(@.['age'] == 30)]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['users']\").elementWithIndex(1).field(\"['name']\").isEqualTo(\"Bob\")");
			assertThat(entry.jsonPath()).isEqualTo("$.['users'][1][?(@.['name'] == 'Bob')]");
		});
		assertThat(pathAndValues).anySatisfy((entry) -> {
			assertThat(entry.method())
				.isEqualTo(".array(\"['users']\").elementWithIndex(1).field(\"['age']\").isEqualTo(25)");
			assertThat(entry.jsonPath()).isEqualTo("$.['users'][1][?(@.['age'] == 25)]");
		});
	}

	// ==================== Helpers ====================

	private static BodyMatcher matcher(MatchingType matchingType, String jsonPath, Object value) {
		return new BodyMatcher() {
			@Override
			public MatchingType matchingType() {
				return matchingType;
			}

			@Override
			public String path() {
				return jsonPath;
			}

			@Override
			public Object value() {
				return value;
			}

			@Override
			public Integer minTypeOccurrence() {
				return null;
			}

			@Override
			public Integer maxTypeOccurrence() {
				return null;
			}
		};
	}

	private static void assertThatJsonPathsInMapAreValid(String json, JsonPaths pathAndValues) {
		DocumentContext parsedJson = JsonPath.using(Configuration.builder().options(Option.ALWAYS_RETURN_LIST).build())
			.parse(json);
		for (var entry : pathAndValues) {
			assertThat(parsedJson.read(entry.jsonPath(), JSONArray.class)).isNotEmpty();
		}
	}

}
