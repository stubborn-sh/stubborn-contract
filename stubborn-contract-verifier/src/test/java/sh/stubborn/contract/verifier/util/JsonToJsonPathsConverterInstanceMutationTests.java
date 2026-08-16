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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for the instance transform overloads of
 * {@link JsonToJsonPathsConverter}, including the empty-json handling.
 */
class JsonToJsonPathsConverterInstanceMutationTests {

	private final JsonToJsonPathsConverter converter = new JsonToJsonPathsConverter();

	@Test
	void tests_side_values_produces_path_for_non_empty_map() {
		JsonPaths paths = this.converter.transformToJsonPathWithTestsSideValues(Map.of("a", "b"));
		assertThat(paths).isNotEmpty();
		assertThat(paths).anySatisfy((entry) -> assertThat(entry.jsonPath()).contains("['a']"));
	}

	@Test
	void stubs_side_values_produces_path_for_non_empty_map() {
		JsonPaths paths = this.converter.transformToJsonPathWithStubsSideValues(Map.of("a", "b"));
		assertThat(paths).isNotEmpty();
		assertThat(paths).anySatisfy((entry) -> assertThat(entry.jsonPath()).contains("['a']"));
	}

	@Test
	void stubs_side_values_with_empty_check_flag() {
		assertThat(this.converter.transformToJsonPathWithStubsSideValues(Map.of(), false)).isEmpty();
		assertThat(this.converter.transformToJsonPathWithStubsSideValues(Map.of(), true)).hasSize(1);
	}

	@Test
	void empty_map_without_check_is_empty_with_check_has_one_entry() {
		assertThat(this.converter.transformToJsonPathWithTestsSideValues(Map.of(), false)).isEmpty();
		assertThat(this.converter.transformToJsonPathWithTestsSideValues(Map.of(), true)).hasSize(1);
	}

	@Test
	void empty_list_is_empty_non_empty_list_is_not() {
		assertThat(this.converter.transformToJsonPathWithTestsSideValues(List.of(), false)).isEmpty();
		assertThat(this.converter.transformToJsonPathWithTestsSideValues(List.of("x"), false)).isNotEmpty();
	}

	@Test
	void empty_string_is_empty_non_empty_json_string_is_not() {
		assertThat(this.converter.transformToJsonPathWithTestsSideValues("", false)).isEmpty();
		assertThat(this.converter.transformToJsonPathWithTestsSideValues("{\"a\":1}", false)).isNotEmpty();
	}

	@Test
	void null_json_yields_no_paths() {
		assertThat(this.converter.transformToJsonPathWithTestsSideValues(null, true)).isEmpty();
	}

}
