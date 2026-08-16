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

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link NamesUtil} accessors not covered elsewhere.
 */
class NamesUtilMutationTests {

	@Test
	void hasSeparator_true_and_false() {
		assertThat(NamesUtil.hasSeparator("a.b", ".")).isTrue();
		assertThat(NamesUtil.hasSeparator("ab", ".")).isFalse();
	}

	@Test
	void hasSeparator_boundary_first_character() {
		// separator at index 0 -> indexOf == 0 which is still > -1 (true)
		assertThat(NamesUtil.hasSeparator(".ab", ".")).isTrue();
	}

	@Test
	void hasDot_true_and_false() {
		assertThat(NamesUtil.hasDot("a.b")).isTrue();
		assertThat(NamesUtil.hasDot("ab")).isFalse();
	}

	@Test
	void camelCase_lowercases_first_char() {
		assertThat(NamesUtil.camelCase("HelloWorld")).isEqualTo("helloWorld");
		assertThat(NamesUtil.camelCase("")).isEmpty();
	}

	@Test
	void capitalize_uppercases_first_char() {
		assertThat(NamesUtil.capitalize("helloWorld")).isEqualTo("HelloWorld");
		assertThat(NamesUtil.capitalize("")).isEmpty();
	}

	@Test
	void defaultContractName_appends_index_when_multiple() {
		File file = new File("myContract.groovy");
		assertThat(NamesUtil.defaultContractName(file, List.of("a", "b"), 0)).isEqualTo("myContract_0");
		assertThat(NamesUtil.defaultContractName(file, List.of("a"), 0)).isEqualTo("myContract");
		assertThat(NamesUtil.defaultContractName(file, List.of("a"), 2)).isEqualTo("myContract_2");
	}

}
