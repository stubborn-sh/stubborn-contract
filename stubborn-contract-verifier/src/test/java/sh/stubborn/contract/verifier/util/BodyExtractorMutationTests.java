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
 * Mutation-focused tests for {@link BodyExtractor}.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
class BodyExtractorMutationTests {

	@Test
	void extractTestValueFrom_map_uses_server_side() {
		String json = BodyExtractor.extractTestValueFrom(Map.of("a", new DslProperty("cli", "srv")));
		assertThat(json).isEqualTo("{\"a\":\"srv\"}");
	}

	@Test
	void extractStubValueFrom_map_uses_client_side() {
		String json = BodyExtractor.extractStubValueFrom(Map.of("a", new DslProperty("cli", "srv")));
		assertThat(json).isEqualTo("{\"a\":\"cli\"}");
	}

	@Test
	void extractTestValueFrom_string_trims_quotes() {
		assertThat(BodyExtractor.extractTestValueFrom("hello")).isEqualTo("hello");
	}

	@Test
	void extractServerValueFromBody_null_is_null() {
		assertThat(BodyExtractor.extractServerValueFromBody(null)).isNull();
	}

	@Test
	void extractServerValueFromBody_map_resolves_server() {
		Object result = BodyExtractor.extractServerValueFromBody(Map.of("a", new DslProperty("cli", "srv")));
		assertThat((Map) result).containsEntry("a", "srv");
	}

	@Test
	void extractClientValueFromBody_null_is_null() {
		assertThat(BodyExtractor.extractClientValueFromBody(null)).isNull();
	}

	@Test
	void extractClientValueFromBody_dslProperty_resolves_client() {
		Object result = BodyExtractor.extractClientValueFromBody(new DslProperty("cli", "srv"));
		assertThat(result).isEqualTo("cli");
	}

	@Test
	void extractClientValueFromBody_map_resolves_client() {
		Object result = BodyExtractor.extractClientValueFromBody(Map.of("a", new DslProperty("cli", "srv")));
		assertThat((Map) result).containsEntry("a", "cli");
	}

}
