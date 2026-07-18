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

package sh.stubborn.contract.verifier.converter;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class YamlContractTests {

	static Stream<Arguments> matchingTypeCases() {
		return Stream.of(Arguments.of("equalTo", YamlContract.MatchingType.equal_to),
				Arguments.of("equalToJson", YamlContract.MatchingType.equal_to_json),
				Arguments.of("containing", YamlContract.MatchingType.containing), Arguments.of("unknown", null));
	}

	@ParameterizedTest
	@MethodSource("matchingTypeCases")
	void should_convert_to_matching_type_from_string(String string, YamlContract.MatchingType expectedType) {
		assertThat(YamlContract.MatchingType.from(string)).isEqualTo(expectedType);
	}

}
