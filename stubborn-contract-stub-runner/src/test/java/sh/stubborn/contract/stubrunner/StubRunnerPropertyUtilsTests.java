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

package sh.stubborn.contract.stubrunner;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class StubRunnerPropertyUtilsTests {

	@BeforeEach
	void setUp() {
		StubRunnerPropertyUtils.FETCHER = new PropertyFetcher();
	}

	@AfterEach
	void tearDown() {
		StubRunnerPropertyUtils.FETCHER = new PropertyFetcher();
	}

	@AfterAll
	static void cleanupSpec() {
		StubRunnerPropertyUtils.FETCHER = new PropertyFetcher();
	}

	static Stream<Arguments> isPropertySetArgs() {
		return Stream.of(Arguments.of("foo.bar-baz", null, null, "FOO_BAR_BAZ", false),
				Arguments.of("foo.bar-baz", null, "true", "FOO_BAR_BAZ", true),
				Arguments.of("foo.bar-baz", null, "false", "FOO_BAR_BAZ", false),
				Arguments.of("foo.bar-baz", "false", "true", "FOO_BAR_BAZ", false),
				Arguments.of("foo.bar-baz", "true", "true", "FOO_BAR_BAZ", true));
	}

	@ParameterizedTest
	@MethodSource("isPropertySetArgs")
	void shouldReturnExpectedResultWhenCheckingIfQueriedPropIsSet(String queriedProp, String systemProperty,
			String envVariable, String expectedEnvVar, boolean expectedResult) {
		StubRunnerPropertyUtils.FETCHER = new PropertyFetcher() {
			@Override
			public String systemProp(String prop) {
				return systemProperty;
			}

			@Override
			public String envVar(String prop) {
				assertThat(prop).matches(p -> p.equals(expectedEnvVar)
						|| p.equals("SPRING_CLOUD_CONTRACT_STUBRUNNER_PROPERTIES_" + expectedEnvVar));
				return envVariable;
			}
		};

		boolean result = StubRunnerPropertyUtils.isPropertySet(queriedProp);

		assertThat(result).isEqualTo(expectedResult);
	}

	static Stream<Arguments> getPropertyArgs() {
		return Stream.of(
				Arguments.of("foo.bar-baz", Map.of("foo.bar-baz", "faz"), "ab", "bc", "faz",
						"spring.cloud.contract.stubrunner.properties.foo.bar-baz",
						"SPRING_CLOUD_CONTRACT_STUBRUNNER_PROPERTIES_FOO_BAR_BAZ"),
				Arguments.of("foo.bar-baz", Map.of(), "ab", "bc", "ab",
						"spring.cloud.contract.stubrunner.properties.foo.bar-baz",
						"SPRING_CLOUD_CONTRACT_STUBRUNNER_PROPERTIES_FOO_BAR_BAZ"),
				Arguments.of("foo.bar-baz", Map.of(), "", "bc", "bc",
						"spring.cloud.contract.stubrunner.properties.foo.bar-baz",
						"SPRING_CLOUD_CONTRACT_STUBRUNNER_PROPERTIES_FOO_BAR_BAZ"),
				Arguments.of("foo.bar-baz", null, "", "bc", "bc",
						"spring.cloud.contract.stubrunner.properties.foo.bar-baz",
						"SPRING_CLOUD_CONTRACT_STUBRUNNER_PROPERTIES_FOO_BAR_BAZ"));
	}

	@ParameterizedTest
	@MethodSource("getPropertyArgs")
	void shouldReturnExpectedResultWhenQueriedForProp(String queriedProp, Map<String, String> map,
			String systemProperty, String envVariable, String expectedResult, String assertedSystemProp,
			String assertedEnvVar) {
		StubRunnerPropertyUtils.FETCHER = new PropertyFetcher() {
			@Override
			public String systemProp(String prop) {
				assertThat(prop).matches(p -> p.equals(assertedSystemProp)
						|| p.equals(assertedSystemProp.replace("spring.cloud.contract.stubrunner.properties.", "")));
				return systemProperty;
			}

			@Override
			public String envVar(String prop) {
				assertThat(prop).matches(p -> p.equals(assertedEnvVar)
						|| p.equals(assertedEnvVar.replace("SPRING_CLOUD_CONTRACT_STUBRUNNER_PROPERTIES_", "")));
				return envVariable;
			}
		};

		String result = StubRunnerPropertyUtils.getProperty(map, queriedProp);

		assertThat(result).isEqualTo(expectedResult);
	}

	static Stream<Arguments> hasPropertyArgs() {
		return Stream.of(Arguments.of("foo.bar-baz", Map.of("foo.bar-baz", "faz"), "ab", "bc", true),
				Arguments.of("foo.bar-baz", Map.of(), "ab", "bc", true),
				Arguments.of("foo.bar-baz", Map.of(), "", "bc", true),
				Arguments.of("foo.bar-baz", null, "", "bc", true),
				Arguments.of("foo.bar-baz", null, null, null, false));
	}

	@ParameterizedTest
	@MethodSource("hasPropertyArgs")
	void shouldReturnExpectedResultWhenPropIsSetForQueriedProp(String queriedProp, Map<String, String> map,
			String systemProperty, String envVariable, boolean expectedResult) {
		StubRunnerPropertyUtils.FETCHER = new PropertyFetcher() {
			@Override
			public String systemProp(String prop) {
				return systemProperty;
			}

			@Override
			public String envVar(String prop) {
				return envVariable;
			}
		};

		boolean result = StubRunnerPropertyUtils.hasProperty(map, queriedProp);

		assertThat(result).isEqualTo(expectedResult);
	}

}
