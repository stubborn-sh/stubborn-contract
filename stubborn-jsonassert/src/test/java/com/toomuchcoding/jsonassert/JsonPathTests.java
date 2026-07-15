package com.toomuchcoding.jsonassert;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/**
 * @author Marcin Grzejszczak
 */
class JsonPathTests {

	@ParameterizedTest
	@MethodSource("provideJsonPaths")
	void shouldGenerateProperJsonPaths(String jsonPath, String expectedJsonPath) {
		assertThat(jsonPath).isEqualTo(expectedJsonPath);
	}

	static Stream<Arguments> provideJsonPaths() {
		return Stream.of(
				Arguments.of(
						JsonPath.builder().field("some").field("nested").field("anothervalue").isEqualTo(4).jsonPath(),
						"$.some.nested[?(@.anothervalue == 4)]"),
				Arguments.of(
						JsonPath.builder().field("some").field("nested").field("anothervalue").isEqualTo(4).jsonPath(),
						"$.some.nested[?(@.anothervalue == 4)]"),
				Arguments.of(JsonPath.builder()
					.field("some")
					.field("nested")
					.array("withlist")
					.contains("name")
					.isEqualTo("name1")
					.jsonPath(), "$.some.nested.withlist[*][?(@.name == 'name1')]"),
				Arguments.of(JsonPath.builder()
					.field("some")
					.field("nested")
					.array("withlist")
					.contains("name")
					.isEqualTo("name2")
					.jsonPath(), "$.some.nested.withlist[*][?(@.name == 'name2')]"),
				Arguments.of(JsonPath.builder()
					.field("some")
					.field("nested")
					.field("json")
					.isEqualTo("with \"val'ue")
					.jsonPath(), "$.some.nested[?(@.json == 'with \"val\\'ue')]"),
				Arguments.of(JsonPath.builder().field("some", "nested", "json").isEqualTo("with \"val'ue").jsonPath(),
						"$.some.nested[?(@.json == 'with \"val\\'ue')]"));
	}

}
