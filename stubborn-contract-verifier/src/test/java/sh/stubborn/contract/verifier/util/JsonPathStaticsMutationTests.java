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

import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.internal.BodyMatcher;
import sh.stubborn.contract.spec.internal.MatchingType;
import sh.stubborn.contract.spec.internal.MatchingTypeValue;
import sh.stubborn.contract.spec.internal.PathBodyMatcher;
import sh.stubborn.contract.spec.internal.RegexProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for the delegating static helpers of
 * {@link JsonToJsonPathsConverter} / {@link JsonPathMatcherUtils}.
 */
class JsonPathStaticsMutationTests {

	@Test
	void readElement_navigates_nested_json() {
		Map<String, Object> json = Map.of("a", Map.of("b", "c"));
		assertThat(JsonToJsonPathsConverter.readElement(json, "$.a.b")).isEqualTo("c");
	}

	@Test
	void generatedValueIfNeeded_generates_for_regex_property() {
		RegexProperty regexProperty = new RegexProperty(Pattern.compile("[0-9]{3}"));
		Object generated = JsonToJsonPathsConverter.generatedValueIfNeeded(regexProperty);
		assertThat(generated.toString()).matches("[0-9]{3}");
	}

	@Test
	void generatedValueIfNeeded_passes_through_plain_value() {
		assertThat(JsonToJsonPathsConverter.generatedValueIfNeeded("plain")).isEqualTo("plain");
	}

	@Test
	void convertJsonPathAndRegexToAJsonPath_regex_builds_condition() {
		BodyMatcher matcher = new PathBodyMatcher("$.a.b", new MatchingTypeValue(MatchingType.REGEX, "[0-9]+"));
		assertThat(JsonToJsonPathsConverter.convertJsonPathAndRegexToAJsonPath(matcher))
			.isEqualTo("$.a[?(@.b =~ /([0-9]+)/)]");
	}

	@Test
	void convertJsonPathAndRegexToAJsonPath_null_value_non_equality_returns_path() {
		BodyMatcher matcher = new PathBodyMatcher("$.a.b", new MatchingTypeValue(MatchingType.REGEX, null));
		assertThat(JsonToJsonPathsConverter.convertJsonPathAndRegexToAJsonPath(matcher)).isEqualTo("$.a.b");
	}

	@Test
	void no_array_size_check_transform_produces_paths() {
		Map<String, Object> json = Map.of("a", "b");
		JsonPaths paths = JsonToJsonPathsConverter.transformToJsonPathWithStubsSideValuesAndNoArraySizeCheck(json);
		assertThat(paths).isNotEmpty();
		assertThat(paths).anySatisfy((entry) -> assertThat(entry.jsonPath()).contains("['a']"));
	}

}
