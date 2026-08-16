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

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.internal.DslProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link BodyExtractor} value extraction and {@link NamesUtil}
 * empty-string guards.
 */
@SuppressWarnings("NullAway")
class BodyExtractorAndNamesMutationTests {

	@Test
	@SuppressWarnings("unchecked")
	void extract_server_value_resolves_dsl_property_server_side() {
		Map<String, Object> body = Map.of("k", new DslProperty<>("client", "server"));
		Map<String, Object> result = (Map<String, Object>) BodyExtractor.extractServerValueFromBody(body);
		assertThat(result).containsEntry("k", "server");
	}

	@Test
	@SuppressWarnings("unchecked")
	void extract_client_value_resolves_dsl_property_client_side() {
		Map<String, Object> body = Map.of("k", new DslProperty<>("client", "server"));
		Map<String, Object> result = (Map<String, Object>) BodyExtractor.extractClientValueFromBody(body);
		assertThat(result).containsEntry("k", "client");
	}

	@Test
	void extract_server_value_of_null_is_null() {
		assertThat(BodyExtractor.extractServerValueFromBody(null)).isNull();
	}

	@Test
	void extract_client_value_of_null_is_null() {
		assertThat(BodyExtractor.extractClientValueFromBody(null)).isNull();
	}

	@Test
	void camel_case_null_stays_null_non_empty_lowercases_first_char() {
		assertThat(NamesUtil.camelCase(null)).isNull();
		assertThat(NamesUtil.camelCase("Foo")).isEqualTo("foo");
	}

	@Test
	void capitalize_null_stays_null_non_empty_uppercases_first_char() {
		assertThat(NamesUtil.capitalize(null)).isNull();
		assertThat(NamesUtil.capitalize("foo")).isEqualTo("Foo");
	}

}
