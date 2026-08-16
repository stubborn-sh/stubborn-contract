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

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Mutation-focused tests for {@link YamlToContracts#predefinedToPattern}.
 */
class YamlToContractsPredefinedMutationTests {

	private final YamlToContracts converter = new YamlToContracts();

	@Test
	void every_predefined_regex_maps_to_a_non_blank_pattern() {
		for (YamlContract.PredefinedRegex predefined : YamlContract.PredefinedRegex.values()) {
			Pattern pattern = this.converter.predefinedToPattern(predefined);
			assertThat(pattern).as("pattern for %s", predefined).isNotNull();
			assertThat(pattern.pattern()).as("pattern text for %s", predefined).isNotBlank();
		}
	}

	@Test
	void predefined_patterns_match_representative_samples() {
		assertThat(this.converter.predefinedToPattern(YamlContract.PredefinedRegex.number).matcher("123").matches())
			.isTrue();
		assertThat(
				this.converter.predefinedToPattern(YamlContract.PredefinedRegex.any_boolean).matcher("true").matches())
			.isTrue();
		assertThat(
				this.converter.predefinedToPattern(YamlContract.PredefinedRegex.any_double).matcher("12.34").matches())
			.isTrue();
		assertThat(this.converter.predefinedToPattern(YamlContract.PredefinedRegex.ip_address)
			.matcher("192.168.0.1")
			.matches()).isTrue();
		assertThat(this.converter.predefinedToPattern(YamlContract.PredefinedRegex.uuid)
			.matcher("00000000-0000-0000-0000-000000000000")
			.matches()).isTrue();
	}

	@Test
	void null_predefined_throws() {
		assertThatThrownBy(() -> this.converter.predefinedToPattern(null)).isInstanceOf(IllegalStateException.class);
	}

}
