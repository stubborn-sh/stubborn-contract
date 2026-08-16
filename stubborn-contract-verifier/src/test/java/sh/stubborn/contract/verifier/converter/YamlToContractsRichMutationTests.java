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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.DslProperty;
import sh.stubborn.contract.spec.internal.ExecutionProperty;
import sh.stubborn.contract.spec.internal.MatchingStrategy;
import sh.stubborn.contract.spec.internal.Response;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link YamlToContracts}: the value-mapping helpers exercised
 * directly and the private request/response mapping exercised through
 * {@link YamlToContracts#convertFrom}.
 */
@SuppressWarnings("NullAway")
class YamlToContractsRichMutationTests {

	private final YamlToContracts converter = new YamlToContracts();

	@TempDir
	java.nio.file.Path temp;

	private Contract convert(String yaml) throws IOException {
		File file = this.temp.resolve("c" + System.nanoTime() + ".yml").toFile();
		Files.writeString(file.toPath(), yaml, StandardCharsets.UTF_8);
		Collection<Contract> contracts = this.converter.convertFrom(file);
		return contracts.iterator().next();
	}

	// ===== urlValue =====

	@Test
	void url_value_with_command_uses_execution_property_as_server() {
		YamlContract.KeyValueMatcher matcher = new YamlContract.KeyValueMatcher();
		matcher.command = "hasUrl()";
		DslProperty<?> result = this.converter.urlValue("/x", matcher);
		assertThat(result.getClientValue()).isEqualTo("/x");
		assertThat(result.getServerValue()).isInstanceOf(ExecutionProperty.class);
		assertThat(((ExecutionProperty) result.getServerValue()).getExecutionCommand()).isEqualTo("hasUrl()");
	}

	@Test
	void url_value_with_regex_uses_pattern_as_client() {
		YamlContract.KeyValueMatcher matcher = new YamlContract.KeyValueMatcher();
		matcher.regex = "/x/[0-9]+";
		DslProperty<?> result = this.converter.urlValue("/x/1", matcher);
		assertThat(result.getClientValue()).isInstanceOf(Pattern.class);
		assertThat(((Pattern) result.getClientValue()).pattern()).isEqualTo("/x/[0-9]+");
		assertThat(result.getServerValue()).isEqualTo("/x/1");
	}

	@Test
	void url_value_with_predefined_uses_predefined_pattern_as_client() {
		YamlContract.KeyValueMatcher matcher = new YamlContract.KeyValueMatcher();
		matcher.predefined = YamlContract.PredefinedRegex.number;
		DslProperty<?> result = this.converter.urlValue("123", matcher);
		assertThat(result.getClientValue()).isInstanceOf(Pattern.class);
		assertThat(result.getServerValue()).isEqualTo("123");
	}

	@Test
	void url_value_without_matcher_is_symmetric() {
		DslProperty<?> result = this.converter.urlValue("/plain", null);
		assertThat(result.getClientValue()).isEqualTo("/plain");
		assertThat(result.getServerValue()).isEqualTo("/plain");
	}

	// ===== serverValue(value, TestHeaderMatcher) =====

	@Test
	void server_value_regex_returns_pattern() {
		YamlContract.TestHeaderMatcher matcher = new YamlContract.TestHeaderMatcher();
		matcher.regex = "[a-z]+";
		Object result = this.converter.serverValue("abc", matcher, "k");
		assertThat(result).isInstanceOf(Pattern.class);
		assertThat(((Pattern) result).pattern()).isEqualTo("[a-z]+");
	}

	@Test
	void server_value_command_returns_execution_property() {
		YamlContract.TestHeaderMatcher matcher = new YamlContract.TestHeaderMatcher();
		matcher.command = "foo()";
		Object result = this.converter.serverValue("v", matcher, "k");
		assertThat(result).isInstanceOf(ExecutionProperty.class);
	}

	@Test
	void server_value_no_matcher_returns_raw_value() {
		Object result = this.converter.serverValue("raw", (YamlContract.TestHeaderMatcher) null, "k");
		assertThat(result).isEqualTo("raw");
	}

	// ===== serverCookieValue =====

	@Test
	void server_cookie_value_command_puts_execution_property_on_client_side() {
		YamlContract.TestCookieMatcher matcher = new YamlContract.TestCookieMatcher();
		matcher.command = "cookieValue()";
		DslProperty<?> result = this.converter.serverCookieValue("v", matcher, "k");
		assertThat(result.getClientValue()).isInstanceOf(ExecutionProperty.class);
		assertThat(result.getServerValue()).isEqualTo("v");
	}

	@Test
	void server_cookie_value_regex_puts_pattern_on_server_side() {
		YamlContract.TestCookieMatcher matcher = new YamlContract.TestCookieMatcher();
		matcher.regex = "[a-z]+";
		DslProperty<?> result = this.converter.serverCookieValue("abc", matcher, "k");
		assertThat(result.getClientValue()).isEqualTo("abc");
		assertThat(result.getServerValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void server_cookie_value_no_matcher_is_symmetric() {
		DslProperty<?> result = this.converter.serverCookieValue("v", null, "k");
		assertThat(result.getClientValue()).isEqualTo("v");
		assertThat(result.getServerValue()).isEqualTo("v");
	}

	// ===== queryParamValue =====

	private Object queryParam(YamlContract.MatchingType type, Object value) {
		YamlContract yaml = new YamlContract();
		YamlContract.Request request = new YamlContract.Request();
		YamlContract.QueryParameterMatcher matcher = new YamlContract.QueryParameterMatcher();
		matcher.key = "q";
		matcher.type = type;
		matcher.value = value;
		request.matchers.queryParameters.add(matcher);
		yaml.request = request;
		return this.converter.queryParamValue(yaml, "q", "orig");
	}

	@Test
	void query_param_value_without_request_returns_value() {
		assertThat(this.converter.queryParamValue(new YamlContract(), "q", "orig")).isEqualTo("orig");
	}

	@Test
	void query_param_value_without_matcher_returns_value() {
		YamlContract yaml = new YamlContract();
		yaml.request = new YamlContract.Request();
		assertThat(this.converter.queryParamValue(yaml, "q", "orig")).isEqualTo("orig");
	}

	@Test
	void query_param_value_matcher_types_map_to_matching_strategies() {
		assertThat(((MatchingStrategy) ((DslProperty<?>) queryParam(YamlContract.MatchingType.equal_to, "v"))
			.getClientValue()).getType()).isEqualTo(MatchingStrategy.Type.EQUAL_TO);
		assertThat(((MatchingStrategy) ((DslProperty<?>) queryParam(YamlContract.MatchingType.containing, "v"))
			.getClientValue()).getType()).isEqualTo(MatchingStrategy.Type.CONTAINS);
		assertThat(((MatchingStrategy) ((DslProperty<?>) queryParam(YamlContract.MatchingType.matching, "v"))
			.getClientValue()).getType()).isEqualTo(MatchingStrategy.Type.MATCHING);
		assertThat(((MatchingStrategy) ((DslProperty<?>) queryParam(YamlContract.MatchingType.not_matching, "v"))
			.getClientValue()).getType()).isEqualTo(MatchingStrategy.Type.NOT_MATCHING);
		assertThat(((MatchingStrategy) ((DslProperty<?>) queryParam(YamlContract.MatchingType.equal_to_json, "{}"))
			.getClientValue()).getType()).isEqualTo(MatchingStrategy.Type.EQUAL_TO_JSON);
		assertThat(((MatchingStrategy) ((DslProperty<?>) queryParam(YamlContract.MatchingType.equal_to_xml, "<a/>"))
			.getClientValue()).getType()).isEqualTo(MatchingStrategy.Type.EQUAL_TO_XML);

		DslProperty<?> absent = (DslProperty<?>) queryParam(YamlContract.MatchingType.absent, null);
		assertThat(((MatchingStrategy) absent.getClientValue()).getType()).isEqualTo(MatchingStrategy.Type.ABSENT);
		assertThat(absent.getServerValue()).isNull();
	}

	// ===== convertFrom private mappings =====

	@Test
	void metadata_and_in_progress_are_mapped() throws IOException {
		Contract contract = convert("""
				inProgress: true
				metadata:
				  some: value
				request:
				  method: GET
				  url: /x
				response:
				  status: 200
				""");
		assertThat(contract.getInProgress()).isTrue();
		assertThat(contract.getMetadata()).containsEntry("some", "value");
	}

	@Test
	void not_in_progress_by_default() throws IOException {
		Contract contract = convert("""
				request:
				  method: GET
				  url: /x
				response:
				  status: 200
				""");
		assertThat(contract.getInProgress()).isFalse();
	}

	@Test
	void response_async_flag_is_mapped() throws IOException {
		Contract contract = convert("""
				request:
				  method: GET
				  url: /x
				response:
				  status: 200
				  async: true
				  body:
				    foo: bar
				""");
		assertThat(contract.getResponse().getAsync()).isTrue();
	}

	@Test
	void response_body_regex_matcher_sets_pattern_server_value() throws IOException {
		Contract contract = convert("""
				request:
				  method: GET
				  url: /x
				response:
				  status: 200
				  body: "abc"
				  matchers:
				    body:
				      - type: by_regex
				        value: "[a-z]+"
				""");
		Response response = contract.getResponse();
		assertThat(response.getBody().getServerValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void response_body_command_matcher_sets_execution_property_server_value() throws IOException {
		Contract contract = convert("""
				request:
				  method: GET
				  url: /x
				response:
				  status: 200
				  body: "abc"
				  matchers:
				    body:
				      - type: by_command
				        value: "assertThatBodyIsValid($it)"
				""");
		Response response = contract.getResponse();
		assertThat(response.getBody().getServerValue()).isInstanceOf(ExecutionProperty.class);
	}

}
