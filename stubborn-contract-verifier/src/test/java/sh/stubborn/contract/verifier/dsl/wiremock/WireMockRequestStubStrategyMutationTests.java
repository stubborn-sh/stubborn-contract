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

package sh.stubborn.contract.verifier.dsl.wiremock;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.github.tomakehurst.wiremock.matching.RequestPattern;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.verifier.converter.YamlContractConverter;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.util.ContentType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link WireMockRequestStubStrategy}. Each test pins the
 * exact generated WireMock request JSON for a specific request shape.
 */
class WireMockRequestStubStrategyMutationTests {

	private static String requestJson(String yaml, ContentType contentType) throws IOException {
		File tmp = File.createTempFile("req" + System.nanoTime(), ".yml");
		Files.writeString(tmp.toPath(), yaml, StandardCharsets.UTF_8);
		Contract contract = new YamlContractConverter().convertFrom(tmp).iterator().next();
		SingleContractMetadata meta = Mockito.mock(SingleContractMetadata.class);
		Mockito.lenient().when(meta.getEvaluatedInputStubContentType()).thenReturn(contentType);
		RequestPattern pattern = new WireMockRequestStubStrategy(contract, meta).buildClientRequestContent();
		return pattern.toString().replaceAll("\\s+", "");
	}

	@Test
	void url_path_query_headers_cookies_use_equal_to() throws IOException {
		String json = requestJson("""
				request:
				  method: GET
				  urlPath: /foo
				  queryParameters:
				    a: b
				  headers:
				    X-Head: hval
				  cookies:
				    sess: xyz
				response:
				  status: 200
				""", ContentType.JSON);

		assertThat(json).contains("\"urlPath\":\"/foo\"");
		assertThat(json).contains("\"method\":\"GET\"");
		assertThat(json).contains("\"X-Head\":{\"equalTo\":\"hval\"}");
		assertThat(json).contains("\"a\":{\"equalTo\":\"b\"}");
		assertThat(json).contains("\"sess\":{\"equalTo\":\"xyz\"}");
	}

	@Test
	void plain_url_without_query_uses_url_equal_to() throws IOException {
		String json = requestJson("""
				request:
				  method: DELETE
				  url: /bar
				response:
				  status: 200
				""", ContentType.JSON);

		assertThat(json).isEqualTo("{\"urlPath\":\"/bar\",\"method\":\"DELETE\"}");
	}

	@Test
	void url_with_regex_matcher_uses_url_matching() throws IOException {
		String json = requestJson("""
				request:
				  method: GET
				  url: /id/1
				  matchers:
				    url:
				      regex: "/id/[0-9]+"
				response:
				  status: 200
				""", ContentType.JSON);

		assertThat(json).contains("\"urlPathPattern\":\"/id/[0-9]+\"");
	}

	@Test
	void json_body_generates_json_path_matchers_including_regex_matcher() throws IOException {
		String json = requestJson("""
				request:
				  method: POST
				  url: /json
				  headers:
				    Content-Type: application/json
				  body:
				    name: foo
				    age: 20
				  matchers:
				    body:
				      - path: $.name
				        type: by_regex
				        value: "[a-z]+"
				response:
				  status: 200
				""", ContentType.JSON);

		assertThat(json).contains("\"matchesJsonPath\":\"$[?(@.['age']==20)]\"");
		assertThat(json).contains("\"matchesJsonPath\":\"$[?(@.name=~/([a-z]+)/)]\"");
	}

	@Test
	void xml_body_generates_xpath_matchers() throws IOException {
		String json = requestJson("""
				request:
				  method: POST
				  url: /xml
				  headers:
				    Content-Type: application/xml
				  body: "<root><a>1</a></root>"
				response:
				  status: 200
				""", ContentType.XML);

		assertThat(json).contains("\"matchesXPath\":{\"expression\":\"/root/a/text()\",\"equalTo\":\"1\"}");
	}

	@Test
	void query_parameter_matching_type_produces_matching_pattern() throws IOException {
		String json = requestJson("""
				request:
				  method: GET
				  urlPath: /q
				  queryParameters:
				    a: "1"
				  matchers:
				    queryParameters:
				      - key: a
				        type: matching
				        value: "[0-9]+"
				response:
				  status: 200
				""", ContentType.JSON);

		assertThat(json).contains("\"a\":{\"matches\":\"[0-9]+\"}");
	}

}
