package com.toomuchcoding.jsonassert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author Marcin Grzejszczak
 */
class JsonAssertionTests {

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

	// {"property1":"a","property2":[{"a":"sth"},{"b":"sthElse"}]}
	static final String JSON4 = "{\"property1\":\"a\",\"property2\":[{\"a\":\"sth\"},{\"b\":\"sthElse\"}]}";

	// {"property":{"14":0.0,"7":0.0}}
	static final String JSON5 = "{\"property\":{\"14\":0.0,\"7\":0.0}}";

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

	// {"property1":"a","property2":123}
	static final String JSON9 = "{\"property1\":\"a\",\"property2\":123}";

	// {"errors":[{"property":"bank_account_number","message":"incorrect_format"}]}
	static final String JSON10 = "{\"errors\":[{\"property\":\"bank_account_number\",\"message\":\"incorrect_format\"}]}";

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

	static final String JSON12 = """
				[{
					"place":
					{
						"bounding_box":
						{
							"coordinates":
								[[[
									[-77.119759,38.995548],
									[-76.909393,38.791645]
								]]]
						}
					}
				}]
			""";

	static final String JSON_ARRAY_IN_ARRAY = """
				{
					"coordinates":
						[
							[-77.119759,38.995548],
							[-76.909393,38.791645]
						]
					}
				}
			""";

	@ParameterizedTest
	@MethodSource("provideMapAsRootJsonPaths")
	void shouldConvertJsonWithMapAsRootToMapOfPathToValue(JsonVerifiable verifiable, String expectedJsonPath) {
		assertThat(verifiable.jsonPath()).isEqualTo(expectedJsonPath);
		assertThatNoException().isThrownBy(() -> JsonPath.parse(JSON1).read(expectedJsonPath, JSONArray.class));
	}

	static Stream<Arguments> provideMapAsRootJsonPaths() {
		return Stream.of(Arguments.of(
				JsonAssertion.assertThat(JSON1).field("some").field("nested").field("anothervalue").isEqualTo(4),
				"$.some.nested[?(@.anothervalue == 4)]"),
				Arguments.of(JsonAssertion.assertThat(JSON1).field("some").field("nested").field("anothervalue"),
						"$.some.nested.anothervalue"),
				Arguments.of(JsonAssertion.assertThatJson(JSON1)
					.field("some")
					.field("nested")
					.field("anothervalue")
					.isEqualTo(4), "$.some.nested[?(@.anothervalue == 4)]"),
				Arguments.of(JsonAssertion.assertThat(JSON1)
					.field("some")
					.field("nested")
					.array("withlist")
					.contains("name")
					.isEqualTo("name1"), "$.some.nested.withlist[*][?(@.name == 'name1')]"),
				Arguments.of(JsonAssertion.assertThat(JSON1)
					.field("some")
					.field("nested")
					.array("withlist")
					.contains("name")
					.isEqualTo("name2"), "$.some.nested.withlist[*][?(@.name == 'name2')]"),
				Arguments.of(JsonAssertion.assertThat(JSON1)
					.field("some")
					.field("nested")
					.field("json")
					.isEqualTo("with \"val'ue"), "$.some.nested[?(@.json == 'with \"val\\'ue')]"),
				Arguments.of(JsonAssertion.assertThat(JSON1).field("some", "nested", "json").isEqualTo("with \"val'ue"),
						"$.some.nested[?(@.json == 'with \"val\\'ue')]"));
	}

	@ParameterizedTest
	@MethodSource("provideSimpleResponseBodyJsonPaths")
	void shouldGenerateAssertionsForSimpleResponseBody(JsonVerifiable verifiable, String expectedJsonPath) {
		assertThat(verifiable.jsonPath()).isEqualTo(expectedJsonPath);
		assertThatNoException().isThrownBy(() -> JsonPath.parse(JSON2).read(expectedJsonPath, JSONArray.class));
	}

	static Stream<Arguments> provideSimpleResponseBodyJsonPaths() {
		return Stream.of(
				Arguments.of(JsonAssertion.assertThat(JSON2).field("property1").isEqualTo("a"),
						"$[?(@.property1 == 'a')]"),
				Arguments.of(JsonAssertion.assertThat(JSON2).field("property2").isEqualTo("b"),
						"$[?(@.property2 == 'b')]"));
	}

	@ParameterizedTest
	@MethodSource("provideNullAndBooleanJsonPaths")
	void shouldGenerateAssertionsForNullAndBooleanValues(JsonVerifiable verifiable, String expectedJsonPath) {
		assertThat(verifiable.jsonPath()).isEqualTo(expectedJsonPath);
		assertThatNoException().isThrownBy(() -> JsonPath.parse(JSON3).read(expectedJsonPath, JSONArray.class));
	}

	static Stream<Arguments> provideNullAndBooleanJsonPaths() {
		return Stream.of(
				Arguments.of(JsonAssertion.assertThat(JSON3).field("property1").isEqualTo("true"),
						"$[?(@.property1 == 'true')]"),
				Arguments.of(JsonAssertion.assertThat(JSON3).field("property2").isNull(), "$[?(@.property2 == null)]"),
				Arguments.of(JsonAssertion.assertThat(JSON3).field("property3").isEqualTo(false),
						"$[?(@.property3 == false)]"));
	}

	@ParameterizedTest
	@MethodSource("provideMapWithListJsonPaths")
	void shouldGenerateAssertionsForSimpleResponseBodyConstructedFromMapWithList(JsonVerifiable verifiable,
			String expectedJsonPath) {
		assertThat(verifiable.jsonPath()).isEqualTo(expectedJsonPath);
		assertThatNoException().isThrownBy(() -> JsonPath.parse(JSON4).read(expectedJsonPath, JSONArray.class));
	}

	static Stream<Arguments> provideMapWithListJsonPaths() {
		return Stream.of(
				Arguments.of(JsonAssertion.assertThat(JSON4).field("property1").isEqualTo("a"),
						"$[?(@.property1 == 'a')]"),
				Arguments.of(JsonAssertion.assertThat(JSON4).array("property2").contains("a").isEqualTo("sth"),
						"$.property2[*][?(@.a == 'sth')]"),
				Arguments.of(JsonAssertion.assertThat(JSON4).array("property2").contains("b").isEqualTo("sthElse"),
						"$.property2[*][?(@.b == 'sthElse')]"));
	}

	@ParameterizedTest
	@MethodSource("provideIntegerKeyMapJsonPaths")
	void shouldGenerateAssertionsForResponseBodyContainingMapWithIntegersAsKeys(JsonVerifiable verifiable,
			String expectedJsonPath) {
		assertThat(verifiable.jsonPath()).isEqualTo(expectedJsonPath);
		assertThatNoException().isThrownBy(() -> JsonPath.parse(JSON5).read(expectedJsonPath, JSONArray.class));
	}

	static Stream<Arguments> provideIntegerKeyMapJsonPaths() {
		return Stream.of(
				Arguments.of(JsonAssertion.assertThat(JSON5).field("property").field(7).isEqualTo(0.0),
						"$.property[?(@.7 == 0.0)]"),
				Arguments.of(JsonAssertion.assertThat(JSON5).field("property").field(14).isEqualTo(0.0),
						"$.property[?(@.14 == 0.0)]"));
	}

	@ParameterizedTest
	@MethodSource("provideArrayInBodyJsonPaths")
	void shouldGenerateAssertionsForArrayInResponseBody(JsonVerifiable verifiable, String expectedJsonPath) {
		assertThat(verifiable.jsonPath()).isEqualTo(expectedJsonPath);
		assertThatNoException().isThrownBy(() -> JsonPath.parse(JSON6).read(expectedJsonPath, JSONArray.class));
	}

	static Stream<Arguments> provideArrayInBodyJsonPaths() {
		return Stream.of(
				Arguments.of(JsonAssertion.assertThat(JSON6).array().contains("property1").isEqualTo("a"),
						"$[*][?(@.property1 == 'a')]"),
				Arguments.of(JsonAssertion.assertThat(JSON6).array().contains("property2").isEqualTo("b"),
						"$[*][?(@.property2 == 'b')]"));
	}

	@ParameterizedTest
	@MethodSource("provideArrayInsideBodyElementJsonPaths")
	void shouldGenerateAssertionsForArrayInsideResponseBodyElement(JsonVerifiable verifiable, String expectedJsonPath) {
		assertThat(verifiable.jsonPath()).isEqualTo(expectedJsonPath);
		assertThatNoException().isThrownBy(() -> JsonPath.parse(JSON7).read(expectedJsonPath, JSONArray.class));
	}

	static Stream<Arguments> provideArrayInsideBodyElementJsonPaths() {
		return Stream.of(
				Arguments.of(
						JsonAssertion.assertThat(JSON7).array("property1").contains("property2").isEqualTo("test1"),
						"$.property1[*][?(@.property2 == 'test1')]"),
				Arguments.of(
						JsonAssertion.assertThat(JSON7).array("property1").contains("property3").isEqualTo("test2"),
						"$.property1[*][?(@.property3 == 'test2')]"));
	}

	@ParameterizedTest
	@MethodSource("provideNestedObjectsJsonPaths")
	void shouldGenerateAssertionsForNestedObjectsInResponseBody(JsonVerifiable verifiable, String expectedJsonPath) {
		assertThat(verifiable.jsonPath()).isEqualTo(expectedJsonPath);
		assertThatNoException().isThrownBy(() -> JsonPath.parse(JSON8).read(expectedJsonPath, JSONArray.class));
	}

	static Stream<Arguments> provideNestedObjectsJsonPaths() {
		return Stream.of(
				Arguments.of(JsonAssertion.assertThat(JSON8).field("property2").field("property3").isEqualTo("b"),
						"$.property2[?(@.property3 == 'b')]"),
				Arguments.of(JsonAssertion.assertThat(JSON8).field("property1").isEqualTo("a"),
						"$[?(@.property1 == 'a')]"));
	}

	@ParameterizedTest
	@MethodSource("provideRegexMapJsonPaths")
	void shouldGenerateRegexAssertionsForMapObjectsInResponseBody(JsonVerifiable verifiable, String expectedJsonPath) {
		assertThat(verifiable.jsonPath()).isEqualTo(expectedJsonPath);
		assertThatNoException().isThrownBy(() -> JsonPath.parse(JSON9).read(expectedJsonPath, JSONArray.class));
	}

	static Stream<Arguments> provideRegexMapJsonPaths() {
		return Stream.of(
				Arguments.of(JsonAssertion.assertThat(JSON9).field("property2").matches("[0-9]{3}"),
						"$[?(@.property2 =~ /[0-9]{3}/)]"),
				Arguments.of(JsonAssertion.assertThat(JSON9).field("property1").isEqualTo("a"),
						"$[?(@.property1 == 'a')]"));
	}

	@Test
	void shouldGenerateEscapedRegexAssertionsForStringObjectsInResponseBody() {
		String json = "{\"property2\":123123}";
		JsonVerifiable verifiable = JsonAssertion.assertThat(json).field("property2").matches("\\d+");
		assertThat(verifiable.jsonPath()).isEqualTo("$[?(@.property2 =~ /\\d+/)]");
		assertThatNoException().isThrownBy(() -> JsonPath.parse(json).read(verifiable.jsonPath(), JSONArray.class));
	}

	@ParameterizedTest
	@MethodSource("provideComplexJsonPaths")
	void shouldWorkWithMoreComplexStuffAndJsonpaths(JsonVerifiable verifiable, String expectedJsonPath) {
		assertThat(verifiable.jsonPath()).isEqualTo(expectedJsonPath);
		assertThatNoException().isThrownBy(() -> JsonPath.parse(JSON10).read(expectedJsonPath, JSONArray.class));
	}

	static Stream<Arguments> provideComplexJsonPaths() {
		return Stream.of(Arguments.of(
				JsonAssertion.assertThat(JSON10).array("errors").contains("property").isEqualTo("bank_account_number"),
				"$.errors[*][?(@.property == 'bank_account_number')]"),
				Arguments.of(JsonAssertion.assertThat(JSON10)
					.array("errors")
					.contains("message")
					.isEqualTo("incorrect_format"), "$.errors[*][?(@.message == 'incorrect_format')]"));
	}

	@ParameterizedTest
	@MethodSource("provideTripleArrayJsonPaths")
	void shouldManageToParseTripleArray(JsonVerifiable verifiable, String expectedJsonPath) {
		assertThat(verifiable.jsonPath()).isEqualTo(expectedJsonPath);
		assertThatNoException().isThrownBy(() -> JsonPath.parse(JSON11).read(expectedJsonPath, JSONArray.class));
	}

	static Stream<Arguments> provideTripleArrayJsonPaths() {
		return Stream.of(
				Arguments.of(JsonAssertion.assertThat(JSON11)
					.array()
					.field("place")
					.field("bounding_box")
					.array("coordinates")
					.array()
					.array()
					.arrayField()
					.contains(38.995548)
					.value(), "$[*].place.bounding_box.coordinates[*][*][?(@ == 38.995548)]"),
				Arguments.of(JsonAssertion.assertThat(JSON11)
					.array()
					.field("place")
					.field("bounding_box")
					.array("coordinates")
					.array()
					.array()
					.arrayField()
					.contains(-77.119759)
					.value(), "$[*].place.bounding_box.coordinates[*][*][?(@ == -77.119759)]"),
				Arguments.of(JsonAssertion.assertThat(JSON11)
					.array()
					.field("place")
					.field("bounding_box")
					.array("coordinates")
					.array()
					.array()
					.arrayField()
					.contains(-76.909393)
					.value(), "$[*].place.bounding_box.coordinates[*][*][?(@ == -76.909393)]"),
				Arguments.of(JsonAssertion.assertThat(JSON11)
					.array()
					.field("place")
					.field("bounding_box")
					.array("coordinates")
					.array()
					.array()
					.arrayField()
					.contains(38.791645)
					.value(), "$[*].place.bounding_box.coordinates[*][*][?(@ == 38.791645)]"));
	}

	@ParameterizedTest
	@MethodSource("provideQuadripleArrayJsonPaths")
	void shouldManageToParseQuadripleArray(JsonVerifiable verifiable, String expectedJsonPath) {
		assertThat(verifiable.jsonPath()).isEqualTo(expectedJsonPath);
		assertThatNoException().isThrownBy(() -> JsonPath.parse(JSON12).read(expectedJsonPath, JSONArray.class));
	}

	static Stream<Arguments> provideQuadripleArrayJsonPaths() {
		return Stream.of(
				Arguments.of(JsonAssertion.assertThat(JSON12)
					.array()
					.field("place")
					.field("bounding_box")
					.array("coordinates")
					.array()
					.array()
					.array()
					.arrayField()
					.contains(38.995548)
					.value(), "$[*].place.bounding_box.coordinates[*][*][*][?(@ == 38.995548)]"),
				Arguments.of(JsonAssertion.assertThat(JSON12)
					.array()
					.field("place")
					.field("bounding_box")
					.array("coordinates")
					.array()
					.array()
					.array()
					.arrayField()
					.contains(-77.119759)
					.value(), "$[*].place.bounding_box.coordinates[*][*][*][?(@ == -77.119759)]"),
				Arguments.of(JsonAssertion.assertThat(JSON12)
					.array()
					.field("place")
					.field("bounding_box")
					.array("coordinates")
					.array()
					.array()
					.array()
					.arrayField()
					.contains(-76.909393)
					.value(), "$[*].place.bounding_box.coordinates[*][*][*][?(@ == -76.909393)]"),
				Arguments.of(JsonAssertion.assertThat(JSON12)
					.array()
					.field("place")
					.field("bounding_box")
					.array("coordinates")
					.array()
					.array()
					.array()
					.arrayField()
					.contains(38.791645)
					.value(), "$[*].place.bounding_box.coordinates[*][*][*][?(@ == 38.791645)]"));
	}

	@ParameterizedTest
	@MethodSource("provideArrayInArrayJsonPaths")
	void shouldManageToParseArrayInArray(JsonVerifiable verifiable, String expectedJsonPath) {
		assertThat(verifiable.jsonPath()).isEqualTo(expectedJsonPath);
		assertThatNoException()
			.isThrownBy(() -> JsonPath.parse(JSON_ARRAY_IN_ARRAY).read(expectedJsonPath, JSONArray.class));
	}

	static Stream<Arguments> provideArrayInArrayJsonPaths() {
		return Stream.of(
				Arguments.of(JsonAssertion.assertThat(JSON_ARRAY_IN_ARRAY)
					.array("coordinates")
					.array()
					.arrayField()
					.contains(38.995548)
					.value(), "$.coordinates[*][?(@ == 38.995548)]"),
				Arguments.of(JsonAssertion.assertThat(JSON_ARRAY_IN_ARRAY)
					.array("coordinates")
					.array()
					.arrayField()
					.contains(-77.119759)
					.value(), "$.coordinates[*][?(@ == -77.119759)]"),
				Arguments.of(JsonAssertion.assertThat(JSON_ARRAY_IN_ARRAY)
					.array("coordinates")
					.array()
					.arrayField()
					.contains(-76.909393)
					.value(), "$.coordinates[*][?(@ == -76.909393)]"),
				Arguments.of(JsonAssertion.assertThat(JSON_ARRAY_IN_ARRAY)
					.array("coordinates")
					.array()
					.arrayField()
					.contains(38.791645)
					.value(), "$.coordinates[*][?(@ == 38.791645)]"));
	}

	@ParameterizedTest
	@MethodSource("provideJsonListRootJsons")
	void shouldConvertJsonWithListAsRootToMapOfPathToValue(String json) {
		assertThat(JsonAssertion.assertThat(json)
			.array()
			.field("some")
			.field("nested")
			.field("json")
			.isEqualTo("with value")
			.jsonPath()).isEqualTo("$[*].some.nested[?(@.json == 'with value')]");
		assertThat(JsonAssertion.assertThat(json)
			.array()
			.field("some")
			.field("nested")
			.field("anothervalue")
			.isEqualTo(4)
			.jsonPath()).isEqualTo("$[*].some.nested[?(@.anothervalue == 4)]");
		assertThat(JsonAssertion.assertThat(json)
			.array()
			.field("some")
			.field("nested")
			.array("withlist")
			.contains("name")
			.isEqualTo("name1")
			.jsonPath()).isEqualTo("$[*].some.nested.withlist[*][?(@.name == 'name1')]");
		assertThat(JsonAssertion.assertThat(json)
			.array()
			.field("some")
			.field("nested")
			.array("withlist")
			.contains("name")
			.isEqualTo("name2")
			.jsonPath()).isEqualTo("$[*].some.nested.withlist[*][?(@.name == 'name2')]");
		assertThat(JsonAssertion.assertThat(json)
			.array()
			.field("some")
			.field("nested")
			.array("withlist")
			.field("anothernested")
			.field("name")
			.isEqualTo("name3")
			.jsonPath()).isEqualTo("$[*].some.nested.withlist[*].anothernested[?(@.name == 'name3')]");
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
		JsonAssertion.assertThat(json).matchesJsonPath(jsonPath);
		assertThatNoException().isThrownBy(() -> JsonPath.parse(json).read(jsonPath, JSONArray.class));
	}

	@Test
	void shouldThrowExceptionWhenJsonPathIsNotMatched() {
		String json = """
				{
					"property1": "a",
					"property2": {"property3": "b"}
				}""";
		String jsonPath = "$[?(@.property1 == 'c')]";
		assertThatThrownBy(() -> JsonAssertion.assertThat(json).matchesJsonPath(jsonPath))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Parsed JSON")
			.hasMessageContaining("doesn't match");
	}

	@Test
	void shouldNotThrowExceptionWhenJsonPathIsNotMatchedAndSystemPropOverridesCheck() {
		String json = """
				{
					"property1": "a",
					"property2": {"property3": "b"}
				}""";
		String jsonPath = "$[?(@.property1 == 'c')]";
		assertThatNoException()
			.isThrownBy(() -> JsonAssertion.assertThat(json).withoutThrowingException().matchesJsonPath(jsonPath));
	}

	@Test
	void shouldGenerateEscapedRegexAssertionsForBooleanObjectsInResponseBody() {
		String json = "{\"property2\":true}";
		JsonVerifiable verifiable = JsonAssertion.assertThat(json).field("property2").matches("true|false");
		assertThat(verifiable.jsonPath()).isEqualTo("$[?(@.property2 =~ /true|false/)]");
		assertThatNoException().isThrownBy(() -> JsonPath.parse(json).read(verifiable.jsonPath(), JSONArray.class));
	}

	@Test
	void shouldGenerateEscapedRegexAssertionsForNumbersObjectsInResponseBody() {
		String json = "{\"property2\":50}";
		JsonVerifiable verifiable = JsonAssertion.assertThat(json).field("property2").matches("[0-9]{2}");
		assertThat(verifiable.jsonPath()).isEqualTo("$[?(@.property2 =~ /[0-9]{2}/)]");
		assertThatNoException().isThrownBy(() -> JsonPath.parse(json).read(verifiable.jsonPath(), JSONArray.class));
	}

	@Test
	void shouldEscapeRegularExpressionProperly() {
		String json = """
					{
						"path" : "/api/12",
						"correlationId" : 123456
					}
				""";
		DocumentContext parsedJson = JsonPath.parse(json);
		JsonVerifiable verifiable = JsonAssertion.assertThatJson(parsedJson).field("path").matches("^/api/[0-9]{2}$");
		assertThat(verifiable.jsonPath()).isEqualTo("$[?(@.path =~ /^\\/api\\/[0-9]{2}$/)]");
		assertThatNoException().isThrownBy(() -> JsonPath.parse(json).read(verifiable.jsonPath(), JSONArray.class));
	}

	@Test
	void shouldEscapeSingleQuotesInQuotedString() {
		String json = """
					{
						"text" : "text with 'quotes' inside"
					}
				""";
		DocumentContext parsedJson = JsonPath.parse(json);
		JsonVerifiable verifiable = JsonAssertion.assertThatJson(parsedJson)
			.field("text")
			.isEqualTo("text with 'quotes' inside");
		assertThat(verifiable.jsonPath()).isEqualTo("$[?(@.text == 'text with \\'quotes\\' inside')]");
		assertThatNoException().isThrownBy(() -> JsonPath.parse(json).read(verifiable.jsonPath(), JSONArray.class));
	}

	@Test
	void shouldEscapeDoubleQuotesInQuotedString() {
		String json = """
					{
						"text" : "text with \\"quotes\\" inside"
					}
				""";
		DocumentContext parsedJson = JsonPath.parse(json);
		JsonVerifiable verifiable = JsonAssertion.assertThatJson(parsedJson)
			.field("text")
			.isEqualTo("text with \"quotes\" inside");
		assertThat(verifiable.jsonPath()).isEqualTo("$[?(@.text == 'text with \"quotes\" inside')]");
		assertThatNoException().isThrownBy(() -> JsonPath.parse(json).read(verifiable.jsonPath(), JSONArray.class));
	}

	@Test
	void shouldResolveValueOfJsonViaJsonPath() {
		String json = """
					[ {
						"some" : {
							"nested" : {
								"json" : "with value",
								"anothervalue": 4,
								"withlist" : [
									{ "name" :"name1"} ,
									{"name": "name2"},
									{"anothernested": { "name": "name3"} }
								]
							}
						}
					},
					{
						"someother" : {
							"nested" : {
								"json" : true,
								"anothervalue": 4,
								"withlist" : [
									{ "name" :"name1"} , {"name": "name2"}
								],
								"withlist2" : [
									"a", "b"
								]
							}
						}
					}
					]
				""";
		assertThat(com.toomuchcoding.jsonassert.JsonPath.builder(json)
			.array()
			.field("some")
			.field("nested")
			.field("json")
			.read(String.class)).isEqualTo("with value");
		assertThat(com.toomuchcoding.jsonassert.JsonPath.builder(json)
			.array()
			.field("some")
			.field("nested")
			.field("anothervalue")
			.read(Integer.class)).isEqualTo(4);
		assertThat(JsonAssertion.assertThat(json)
			.array()
			.field("some")
			.field("nested")
			.array("withlist")
			.field("name")
			.read(List.class)).isEqualTo(List.of("name1", "name2"));
		assertThat(JsonAssertion.assertThat(json)
			.array()
			.field("someother")
			.field("nested")
			.array("withlist2")
			.read(List.class)).isEqualTo(List.of("a", "b"));
		assertThat(JsonAssertion.assertThat(json)
			.array()
			.field("someother")
			.field("nested")
			.field("json")
			.read(Boolean.class)).isEqualTo(true);
	}

	@Test
	void shouldAssertJsonWithOnlyTopListElements() {
		String json = "[\"Java\", \"Java8\", \"Spring\", \"SpringBoot\", \"Stream\"]";
		JsonAssertion.assertThatJson(json).arrayField().contains("Java8").value();
		JsonAssertion.assertThatJson(json).arrayField().contains("Spring").value();
		JsonAssertion.assertThatJson(json).arrayField().contains("Java").value();
		JsonAssertion.assertThatJson(json).arrayField().contains("Stream").value();
		JsonAssertion.assertThatJson(json).arrayField().contains("SpringBoot").value();
	}

	@Test
	void shouldMatchArrayContainingArrayOfPrimitives() {
		String json = """
				{"first_name":"existing",
					"partners":[
						{ "role":"AGENT",
							"payment_methods":[ "BANK", "CASH" ]
						}
					]
				}""";
		JsonVerifiable verifiable = JsonAssertion.assertThatJson(json)
			.array("partners")
			.array("payment_methods")
			.arrayField()
			.isEqualTo("BANK")
			.value();
		assertThat(verifiable.jsonPath()).isEqualTo("$.partners[*].payment_methods[?(@ == 'BANK')]");
		assertThatNoException().isThrownBy(() -> JsonPath.parse(json).read(verifiable.jsonPath(), JSONArray.class));
	}

	@Test
	void shouldMatchPatternInArray() {
		String json = "{ \"authorities\": [\"ROLE_ADMIN\"] }";
		JsonVerifiable verifiable = JsonAssertion.assertThatJson(json)
			.array("authorities")
			.arrayField()
			.matches("^[a-zA-Z0-9_\\- ]+$")
			.value();
		assertThat(verifiable.jsonPath()).isEqualTo("$.authorities[?(@ =~ /^[a-zA-Z0-9_\\- ]+$/)]");
		assertThatNoException().isThrownBy(() -> JsonPath.parse(json).read(verifiable.jsonPath(), JSONArray.class));
	}

	@Test
	void shouldManageToParseArrayWithStringValues() {
		String json = "{ \"some_list\" : [\"name1\", \"name2\"] }";
		JsonVerifiable v1 = JsonAssertion.assertThat(JsonPath.parse(json))
			.array("some_list")
			.arrayField()
			.isEqualTo("name1");
		JsonVerifiable v2 = JsonAssertion.assertThat(JsonPath.parse(json))
			.array("some_list")
			.arrayField()
			.isEqualTo("name2");
		assertThat(v1.jsonPath()).isEqualTo("$.some_list[?(@ == 'name1')]");
		assertThat(v2.jsonPath()).isEqualTo("$.some_list[?(@ == 'name2')]");
		assertThatNoException().isThrownBy(() -> JsonPath.parse(json).read(v1.jsonPath(), JSONArray.class));
		assertThatNoException().isThrownBy(() -> JsonPath.parse(json).read(v2.jsonPath(), JSONArray.class));
	}

	@Test
	void shouldParseAnArrayOfArraysThatAreRootElements() {
		String json = "[[\"Programming\", \"Java\"], [\"Programming\", \"Java\", \"Spring\", \"Boot\"]]";
		JsonVerifiable v1 = JsonAssertion.assertThatJson(JsonPath.parse(json))
			.array()
			.arrayField()
			.isEqualTo("Java")
			.value();
		assertThat(v1.jsonPath()).isEqualTo("$[*][?(@ == 'Java')]");
		assertThatNoException().isThrownBy(() -> JsonPath.parse(json).read(v1.jsonPath(), JSONArray.class));
	}

	@Test
	void shouldAllowToCheckArraySize() {
		String json = "{ \"some_list\" : [\"name1\", \"name2\"] }";
		JsonAssertion.assertThat(json).array("some_list").hasSize(2);
		assertThatThrownBy(() -> JsonAssertion.assertThat(json).array("some_list").hasSize(5))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage(
					"Parsed JSON <{\"some_list\":[\"name1\",\"name2\"]}> doesn't have the size <5> for JSON path <$.some_list[*]>. The size is <2>");
	}

	@Test
	void shouldAllowToCheckSizeOfRootArray() {
		String json = "[\"name1\", \"name2\"]";
		JsonAssertion.assertThat(json).hasSize(2);
		assertThatThrownBy(() -> JsonAssertion.assertThat(json).hasSize(5)).isInstanceOf(IllegalStateException.class)
			.hasMessage(
					"Parsed JSON <[\"name1\",\"name2\"]> doesn't have the size <5> for JSON path <$>. The size is <2>");
	}

	@Test
	void shouldAllowToCheckSizeOfNestedArray() {
		String json = """
				[ {
					"some" : {
						"nested" : {
							"json" : "with value",
							"anothervalue": 4,
							"withlist" : [
								{ "name" :"name1"} ,
								{"name": "name2"},
								{"anothernested": { "name": "name3"} }
							]
						}
					}
				},
				{
					"someother" : {
						"nested" : {
							"json" : true,
							"anothervalue": 4,
							"withlist" : [
								{ "name" :"name1"} , {"name": "name2"}
							],
							"withlist2" : [
								"a", "b"
							]
						}
					}
				}
				]""";
		JsonAssertion.assertThat(json).array().field("someother").field("nested").array("withlist2").hasSize(2);
		assertThatThrownBy(() -> JsonAssertion.assertThat(json)
			.array()
			.field("someother")
			.field("nested")
			.array("withlist2")
			.hasSize(5)).isInstanceOf(IllegalStateException.class)
			.hasMessage(
					"Parsed JSON <[{\"some\":{\"nested\":{\"json\":\"with value\",\"anothervalue\":4,\"withlist\":[{\"name\":\"name1\"},{\"name\":\"name2\"},{\"anothernested\":{\"name\":\"name3\"}}]}}},{\"someother\":{\"nested\":{\"json\":true,\"anothervalue\":4,\"withlist\":[{\"name\":\"name1\"},{\"name\":\"name2\"}],\"withlist2\":[\"a\",\"b\"]}}}]> doesn't have the size <5> for JSON path <$[*].someother.nested.withlist2[*]>. The size is <2>");
	}

	@Test
	void shouldAllowToCheckSizeOfNamedArray() {
		String json = "{\"property1\":\"a\",\"property2\":[{\"a\":\"sth\"},{\"b\":\"sthElse\"}]}";
		JsonAssertion.assertThat(json).array("property2").hasSize(2);
		assertThatThrownBy(() -> JsonAssertion.assertThat(json).array("property2").hasSize(5))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage(
					"Parsed JSON <{\"property1\":\"a\",\"property2\":[{\"a\":\"sth\"},{\"b\":\"sthElse\"}]}> doesn't have the size <5> for JSON path <$.property2[*]>. The size is <2>");
	}

	@Test
	void shouldAllowToCheckSizeOfTwoNamelessArrays() {
		String json = "[[\"Programming\", \"Java\"], [\"Programming\", \"Java\", \"Spring\", \"Boot\"], [\"Programming\", \"Java\", \"Spring\", \"Boot\", \"Master\"]]";
		JsonAssertion.assertThat(json).hasSize(3);
		JsonAssertion.assertThat(json).elementWithIndex(0).hasSize(2);
		JsonAssertion.assertThat(json).elementWithIndex(1).hasSize(4);
		assertThatThrownBy(() -> JsonAssertion.assertThat(json).array().hasSize(4))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage(
					"Parsed JSON <[[\"Programming\",\"Java\"],[\"Programming\",\"Java\",\"Spring\",\"Boot\"],[\"Programming\",\"Java\",\"Spring\",\"Boot\",\"Master\"]]> doesn't have the size <4> for JSON path <$[*]>. The size is <3>");
	}

	@Test
	void shouldAllowToCheckSizeOfTwoNamelessArraysInNamelessArray() {
		String json = "[[[\"Programming\", \"Java\"], [\"Programming\", \"Java\", \"Spring\", \"Boot\"]]]";
		JsonAssertion.assertThat(json).hasSize(1);
		JsonAssertion.assertThat(json).elementWithIndex(0).elementWithIndex(0).hasSize(2);
		JsonAssertion.assertThat(json).elementWithIndex(0).elementWithIndex(1).hasSize(4);
		assertThatThrownBy(() -> JsonAssertion.assertThat(json).elementWithIndex(0).elementWithIndex(1).hasSize(5))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage(
					"Parsed JSON <[[[\"Programming\",\"Java\"],[\"Programming\",\"Java\",\"Spring\",\"Boot\"]]]> doesn't have the size <5> for JSON path <$[0][1]>. The size is <4>");
	}

	@Test
	void shouldAllowToCheckArraySizeOfNamelessArray() {
		String json = "{ \"coordinates\" : [[\n\t\t\t\t\t\t\t\t\t\t\t[\"name1\", \"name2\"],\n\t\t\t\t\t\t\t\t\t\t\t[\"name3\", \"name4\"]\n\t\t\t\t\t\t\t\t\t\t]] }";
		JsonAssertion.assertThat(json).array("coordinates").array().hasSize(2);
		assertThatThrownBy(() -> JsonAssertion.assertThat(json).array("coordinates").array().hasSize(5))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("doesn't have the size <5> for JSON path <$.coordinates[*][*]>. The size is <2>");
	}

	@ParameterizedTest
	@MethodSource("provideAllJsons")
	void shouldFailOnNonExistentPath(String json) {
		assertThatThrownBy(() -> JsonAssertion.assertThat(json)
			.field("non")
			.field("existant")
			.field("field")
			.isEqualTo("imaginary value")).isInstanceOf(RuntimeException.class)
			.hasCauseInstanceOf(PathNotFoundException.class);
	}

	static Stream<Arguments> provideAllJsons() {
		return Stream.of(Arguments.of(JSON1), Arguments.of(JSON2), Arguments.of(JSON3), Arguments.of(JSON4),
				Arguments.of(JSON5), Arguments.of(JSON6), Arguments.of(JSON7), Arguments.of(JSON8), Arguments.of(JSON9),
				Arguments.of(JSON10), Arguments.of(JSON11));
	}

	@Test
	void shouldAllowToCheckIfSizeIsEmpty() {
		String json = "{ \"coordinates\" : [], \"foo\": [\"bar\", \"baz\"] }";
		JsonAssertion.assertThat(json).array("coordinates").isEmpty();
		assertThatThrownBy(() -> JsonAssertion.assertThat(json).array("foo").isEmpty())
			.isInstanceOf(IllegalStateException.class)
			.hasMessage(
					"Parsed JSON [{\"coordinates\":[],\"foo\":[\"bar\",\"baz\"]}] with the JSON path [$.foo[*]] is not empty!");
	}

	@Test
	void shouldIgnoreExceptionOnEmptyCheck() {
		String json = "{ \"coordinates\" : [], \"foo\": [\"bar\", \"baz\"] }";
		assertThatNoException()
			.isThrownBy(() -> JsonAssertion.assertThat(json).withoutThrowingException().array("foo").isEmpty());
	}

	@Test
	void shouldThrowExceptionWhenEmptyArrayIsReturned() {
		String json = "{}";
		assertThatThrownBy(() -> JsonAssertion.assertThatJson(json).field("doesNotExist").matches("[\\p{L}]*"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Parsed JSON [{}] doesn't match the JSON path [$[?(@.doesNotExist =~ /[\\p{L}]*/)]]");
		assertThatThrownBy(() -> JsonAssertion.assertThatJson(json).array("c").matches("[\\p{L}]*"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Parsed JSON [{}] doesn't match the JSON path [$[?(@.c =~ /[\\p{L}]*/)]]");
	}

	@Test
	void shouldReadTypesOfObjects() {
		String json = "{ \"foo\": 46 }";
		assertThatNoException()
			.isThrownBy(() -> JsonAssertion.assertThatJson(json).field("foo").isInstanceOf(Number.class));
		assertThatThrownBy(() -> JsonAssertion.assertThatJson(json).field("foo").isInstanceOf(String.class))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("For JSON path [$.foo] instance of [Integer] is not assignable from [String]");
	}

	@Test
	void shouldReadBigNumbers() {
		String json = "{ \"largeNum\": 55534673.56, \"bigInt\": 2147483647, \"decimals\": 0.1287361923123}";
		assertThatNoException()
			.isThrownBy(() -> JsonAssertion.assertThatJson(json).field("largeNum").isEqualTo((double) 55534673.56));
		assertThatNoException()
			.isThrownBy(() -> JsonAssertion.assertThatJson(json).field("bigInt").isEqualTo(Integer.MAX_VALUE));
		assertThatNoException()
			.isThrownBy(() -> JsonAssertion.assertThatJson(json).field("decimals").isEqualTo((double) 0.1287361923123));
	}

	@Test
	void shouldMatchEmptyBraces() {
		String json = "{ \"field1\": {}, \"field2\": [] }";
		assertThatNoException().isThrownBy(() -> JsonAssertion.assertThatJson(json).field("field1").isEmpty());
		assertThatNoException().isThrownBy(() -> JsonAssertion.assertThatJson(json).field("field2").isEmpty());
	}

	@Test
	void shouldWorkWithScientificNotation() {
		String json = "{ \"n\": 1.12E-12}";
		assertThatNoException().isThrownBy(() -> JsonAssertion.assertThatJson(json).field("n").isEqualTo(1.12E-12));
	}

	@Test
	void shouldWorkWithMatchers() {
		String json = "{ \"value\": -1}";
		assertThatNoException()
			.isThrownBy(() -> JsonAssertion.assertThatJson(json).field("['value']").matches("-?(\\d+)"));
	}

}
