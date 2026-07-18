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

package sh.stubborn.contract.verifier.builder;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import sh.stubborn.contract.spec.internal.DslProperty;
import sh.stubborn.contract.spec.internal.MatchingStrategy;
import sh.stubborn.contract.spec.internal.OptionalProperty;
import sh.stubborn.contract.spec.internal.QueryParameter;
import sh.stubborn.contract.spec.internal.RegexProperty;

import static org.assertj.core.api.Assertions.assertThat;

class QueryParamsResolverTests {

	private final QueryParamsResolver resolver = new QueryParamsResolver() {
	};

	static Stream<Arguments> queryParameterCases() {
		return Stream.of(Arguments.of(new DslProperty<>("client", "server"), "server"),
				Arguments.of(new DslProperty<>(null, "server"), "server"),
				Arguments.of(new DslProperty<>("client", null), "null"),
				Arguments.of(new DslProperty<>(null, null), "null"));
	}

	@ParameterizedTest
	@MethodSource("queryParameterCases")
	void should_return_serverValue_for_QueryParameter(DslProperty<String> dslProperty, String expected) {
		Object parameter = new QueryParameter("some_param", dslProperty);
		assertThat(resolver.resolveParamValue(parameter)).isEqualTo(expected);
	}

	static Stream<Arguments> optionalPropertyCases() {
		return Stream.of(Arguments.of("server", "(server)?"), Arguments.of(null, "()?"),
				Arguments.of(new RegexProperty(Pattern.compile(".*")), "(.*)?"),
				Arguments.of(new RegexProperty(null, Pattern.compile(".*")), "(.*)?"),
				Arguments.of(new RegexProperty(Pattern.compile(".*"), null), "(.*)?"),
				Arguments.of(new RegexProperty("", Pattern.compile(".*")), "(.*)?"),
				Arguments.of(new RegexProperty(Pattern.compile(".*"), ""), "(.*)?"));
	}

	@ParameterizedTest
	@MethodSource("optionalPropertyCases")
	void should_return_optionalPattern_for_OptionalProperty(Object value, String expected) {
		Object parameter = new OptionalProperty(value);
		assertThat(resolver.resolveParamValue(parameter)).isEqualTo(expected);
	}

	static Stream<Arguments> matchingStrategyCases() {
		return Stream.of(Arguments.of("server", "server"), Arguments.of(null, "null"),
				Arguments.of(new DslProperty<>("client", "server"), "server"),
				Arguments.of(new DslProperty<>(null, "server"), "server"),
				Arguments.of(new DslProperty<>("client", null), "null"),
				Arguments.of(new DslProperty<>(null, null), "null"));
	}

	@ParameterizedTest
	@MethodSource("matchingStrategyCases")
	void should_return_serverValue_for_MatchingStrategy(Object value, String expected) {
		Object parameter = new MatchingStrategy(value, MatchingStrategy.Type.EQUAL_TO);
		assertThat(resolver.resolveParamValue(parameter)).isEqualTo(expected);
	}

	static Stream<Arguments> dslPropertyCases() {
		return Stream.of(Arguments.of(new DslProperty<>("client", "server"), "server"),
				Arguments.of(new DslProperty<>(null, "server"), "server"),
				Arguments.of(new DslProperty<>("client", null), "null"),
				Arguments.of(new DslProperty<>(null, null), "null"));
	}

	@ParameterizedTest
	@MethodSource("dslPropertyCases")
	void should_return_serverValue_for_DslProperty(DslProperty<String> parameter, String expected) {
		assertThat(resolver.resolveParamValue(parameter)).isEqualTo(expected);
	}

	@Test
	void should_return_null_for_null() {
		assertThat(resolver.resolveParamValue(null)).isEqualTo("null");
	}

}
