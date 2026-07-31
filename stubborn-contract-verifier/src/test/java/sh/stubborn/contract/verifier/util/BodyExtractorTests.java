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
import java.net.URI;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.internal.DslProperty;
import sh.stubborn.contract.spec.internal.FromFileProperty;

import static org.assertj.core.api.Assertions.assertThat;

class BodyExtractorTests {

	@Test
	void should_extract_body_from_json_file() throws Exception {
		URI uri = BodyExtractorTests.class.getResource("/classpath/response.json").toURI();
		FromFileProperty jsonFromFile = new FromFileProperty(new File(uri), String.class);
		assertThat(BodyExtractor.extractClientValueFromBody(jsonFromFile)).isEqualTo(Map.of("status", "RESPONSE"));
	}

	@Test
	void should_extract_test_value_and_trim_wrapping_quotes_for_plain_string() {
		// JsonOutput wraps the string in quotes; trimRepeatedQuotes strips them.
		assertThat(BodyExtractor.extractTestValueFrom("hello")).isEqualTo("hello");
	}

	@Test
	void should_extract_test_value_without_trimming_for_json_object() {
		// A JSON object does not start with a quote, so trimRepeatedQuotes leaves it
		// intact.
		assertThat(BodyExtractor.extractTestValueFrom(Map.of("status", "OK"))).isEqualTo("{\"status\":\"OK\"}");
	}

	@Test
	void should_resolve_server_value_from_dsl_property() {
		assertThat(BodyExtractor.extractServerValueFromBody(new DslProperty<>("client", "server"))).isEqualTo("server");
	}

	@Test
	void should_resolve_client_value_from_dsl_property() {
		assertThat(BodyExtractor.extractClientValueFromBody(new DslProperty<>("client", "server"))).isEqualTo("client");
	}

	@Test
	void should_extract_stub_value_from_dsl_property() {
		assertThat(BodyExtractor.extractStubValueFrom(new DslProperty<>("client", "server"))).isEqualTo("client");
	}

	@Test
	void should_generate_concrete_value_for_dynamic_client_body() {
		// The else-branch turns a Pattern into a concrete value via RegexProperty.
		Object value = BodyExtractor.extractClientValueFromBody(Pattern.compile("[0-9]{3}"));
		assertThat(value).asString().matches("[0-9]{3}");
	}

}
