package com.toomuchcoding.jsonassert;

import static com.toomuchcoding.jsonassert.JsonAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/**
 * @author Marcin Grzejszczak
 */
class JsonAssertionWithAssertJJava8Tests {

	static final String JSON1 = """
				{
					"some" : {
						"nested" : {
							"json" : "with \\"val'ue",
							"anothervalue": 4,
							"withlist" : [
								{ "name" :"name1"} , {"name": "name2"}
							]
						}
					}
				}
			""";

	static final String JSON2 = """
			{
				"property1": "a",
				"property2": "b"
			}""";

	static final String JSON3 = """
			{
				"property1": "true",
				"property2": null,
				"property3": false
			}""";

	static final String JSON6 = """
			[
				{
					"property1": "a"
				},
				{
					"property2": "b"
				}]""";

	static final String JSON7 = """
			{
				"property1": [
				{ "property2": "test1"},
				{ "property3": "test2"}
				]
			}""";

	static final String JSON8 = """
			{
				"property1": "a",
				"property2": {"property3": "b"}
			}""";

	static final String JSON11 = """
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

	@Test
	void shouldConvertJsonWithMapAsRootToMapOfPathToValue() {
		assertThat(JsonPath.parse(JSON1)).field("some").field("nested").field("anothervalue").isEqualTo(4);
		BDDJsonAssertions.then(JsonPath.parse(JSON1)).field("some").field("nested").field("anothervalue").isEqualTo(4);
		assertThat(JsonPath.parse(JSON1)).field("some")
			.field("nested")
			.array("withlist")
			.contains("name")
			.isEqualTo("name1");
		assertThat(JsonPath.parse(JSON1)).field("some")
			.field("nested")
			.array("withlist")
			.contains("name")
			.isEqualTo("name2");
		assertThat(JsonPath.parse(JSON1)).field("some").field("nested").field("json").isEqualTo("with \"val'ue");
		assertThat(JsonPath.parse(JSON1)).field("some", "nested", "json").isEqualTo("with \"val'ue");
	}

	@Test
	void shouldGenerateAssertionsForSimpleResponseBody() {
		assertThat(JsonPath.parse(JSON2)).field("property1").isEqualTo("a");
		assertThat(JsonPath.parse(JSON2)).field("property2").isEqualTo("b");
	}

	@Test
	void shouldGenerateAssertionsForNullAndBooleanValues() {
		assertThat(JsonPath.parse(JSON3)).field("property1").isEqualTo("true");
		assertThat(JsonPath.parse(JSON3)).field("property2").isNull();
		assertThat(JsonPath.parse(JSON3)).field("property3").isEqualTo(false);
	}

	@Test
	void shouldGenerateAssertionsForArrayInResponseBody() {
		assertThat(JsonPath.parse(JSON6)).array().contains("property1").isEqualTo("a");
		assertThat(JsonPath.parse(JSON6)).array().contains("property2").isEqualTo("b");
	}

	@Test
	void shouldGenerateAssertionsForArrayInsideResponseBodyElement() {
		assertThat(JsonPath.parse(JSON7)).array("property1").contains("property2").isEqualTo("test1");
		assertThat(JsonPath.parse(JSON7)).array("property1").contains("property3").isEqualTo("test2");
	}

	@Test
	void shouldGenerateAssertionsForNestedObjectsInResponseBody() {
		assertThat(JsonPath.parse(JSON8)).field("property2").field("property3").isEqualTo("b");
		assertThat(JsonPath.parse(JSON8)).field("property1").isEqualTo("a");
	}

	@Test
	void shouldManageToParseTripleArray() {
		assertThat(JsonPath.parse(JSON11)).array()
			.field("place")
			.field("bounding_box")
			.array("coordinates")
			.array()
			.array()
			.arrayField()
			.contains(38.995548)
			.value();
		assertThat(JsonPath.parse(JSON11)).array()
			.field("place")
			.field("bounding_box")
			.array("coordinates")
			.array()
			.array()
			.arrayField()
			.contains(-77.119759)
			.value();
		assertThat(JsonPath.parse(JSON11)).array()
			.field("place")
			.field("bounding_box")
			.array("coordinates")
			.array()
			.array()
			.arrayField()
			.contains(-76.909393)
			.value();
		assertThat(JsonPath.parse(JSON11)).array()
			.field("place")
			.field("bounding_box")
			.array("coordinates")
			.array()
			.array()
			.arrayField()
			.contains(38.791645)
			.value();
	}

	@ParameterizedTest
	@MethodSource("provideJsonListRootJsons")
	void shouldConvertJsonWithListAsRootToMapOfPathToValue(String json) {
		assertThat(JsonPath.parse(json)).array().field("some").field("nested").field("json").isEqualTo("with value");
		assertThat(JsonPath.parse(json)).array().field("some").field("nested").field("anothervalue").isEqualTo(4);
		assertThat(JsonPath.parse(json)).array()
			.field("some")
			.field("nested")
			.array("withlist")
			.contains("name")
			.isEqualTo("name1");
		assertThat(JsonPath.parse(json)).array()
			.field("some")
			.field("nested")
			.array("withlist")
			.contains("name")
			.isEqualTo("name2");
		assertThat(JsonPath.parse(json)).array()
			.field("some")
			.field("nested")
			.array("withlist")
			.field("anothernested")
			.field("name")
			.isEqualTo("name3");
	}

	static Stream<Arguments> provideJsonListRootJsons() {
		return Stream.of(Arguments.of("""
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
				"""), Arguments.of("""
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
				}
				]"""));
	}

	@Test
	void shouldRunJsonPathWhenProvidedManually() {
		String json = """
				{
					"property1": "a",
					"property2": {"property3": "b"}
				}""";
		String jsonPath = "$[?(@.property1 == 'a')]";
		assertThat(JsonPath.parse(json)).matchesJsonPath(jsonPath);
	}

	@Test
	void shouldThrowExceptionWhenJsonPathIsNotMatchedAgainstProvidedJsonPath() {
		String json = """
				{
					"property1": "a",
					"property2": {"property3": "b"}
				}""";
		String jsonPath = "$[?(@.property1 == 'c')]";
		assertThatThrownBy(() -> assertThat(JsonPath.parse(json)).matchesJsonPath(jsonPath))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("Expected JSON to match JSON Path");
	}

	@Test
	void shouldThrowExceptionWhenJsonPathIsNotMatched() {
		String json = """
				{
					"property1": "a",
					"property2": {"property3": "b"}
				}""";
		assertThatThrownBy(() -> assertThat(JsonPath.parse(json)).field("property2").field("property3").isEqualTo("c"))
			.isInstanceOf(AssertionError.class)
			.hasMessage("Expected JSON to match JSON Path <$.property2[?(@.property3 == 'c')]> but it didn't");
	}

	@Test
	void shouldGenerateEscapedRegexAssertionsForBooleanObjectsInResponseBody() {
		String json = "{\"property2\":true}";
		assertThat(JsonPath.parse(json)).field("property2").matches("true|false");
	}

	@Test
	void shouldGenerateEscapedRegexAssertionsForNumbersObjectsInResponseBody() {
		String json = "{\"property2\":50}";
		assertThat(JsonPath.parse(json)).field("property2").matches("[0-9]{2}");
	}

	@Test
	void shouldAllowToCheckIfSizeIsEmpty() {
		String json = "{ \"coordinates\" : [], \"foo\": [\"bar\", \"baz\"] }";
		assertThat(JsonPath.parse(json)).array("coordinates").isEmpty();
		assertThatThrownBy(() -> assertThat(JsonPath.parse(json)).array("foo").isEmpty())
			.isInstanceOf(AssertionError.class)
			.hasMessage("Expected JSON to with JSON Path <$.foo[*]> to be empty");
	}

	@Test
	void shouldReadTypesOfObjects() {
		String json = "{ \"foo\" : 46 }";
		assertThatNoException()
			.isThrownBy(() -> assertThat(JsonPath.parse(json)).field("foo").isInstanceOf(Number.class));
		assertThatThrownBy(() -> assertThat(JsonPath.parse(json)).field("foo").isInstanceOf(String.class))
			.isInstanceOf(AssertionError.class)
			.hasMessage("For JSON path [$.foo] instance of [Integer] is not assignable from [String]");
	}

}
