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

package sh.stubborn.contract.verifier.wiremock;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class WireMockToDslConverterMutationTests {

	@Test
	void priority_of_zero_is_not_rendered() {
		String wireMockStub = """
				{
				  "priority" : 0,
				  "request" : { "url" : "/test", "method" : "POST" },
				  "response" : { "status" : 200 }
				}
				""";
		String dsl = WireMockToDslConverter.fromWireMockStub(wireMockStub);
		then(dsl).doesNotContain("priority");
	}

	@Test
	void positive_priority_is_rendered() {
		String wireMockStub = """
				{
				  "priority" : 3,
				  "request" : { "url" : "/test", "method" : "POST" },
				  "response" : { "status" : 200 }
				}
				""";
		String dsl = WireMockToDslConverter.fromWireMockStub(wireMockStub);
		then(dsl).contains("priority 3");
	}

	@Test
	void request_url_path_is_rendered() {
		String wireMockStub = """
				{
				  "request" : { "urlPath" : "/the/path", "method" : "GET" },
				  "response" : { "status" : 200 }
				}
				""";
		String dsl = WireMockToDslConverter.fromWireMockStub(wireMockStub);
		then(dsl).contains("url '/the/path'");
	}

	@Test
	void request_url_path_pattern_is_rendered() {
		String wireMockStub = """
				{
				  "request" : { "urlPathPattern" : "/the/path/[0-9]+", "method" : "GET" },
				  "response" : { "status" : 200 }
				}
				""";
		String dsl = WireMockToDslConverter.fromWireMockStub(wireMockStub);
		then(dsl).contains("urlPath $(consumer(regex('/the/path/[0-9]+'))");
	}

	@Test
	void request_body_ignores_non_object_body_patterns() {
		String wireMockStub = """
				{
				  "request" : {
				    "url" : "/test",
				    "method" : "POST",
				    "bodyPatterns" : [ { "equalTo" : "expected-body" }, "not-an-object" ]
				  },
				  "response" : { "status" : 200 }
				}
				""";
		String dsl = WireMockToDslConverter.fromWireMockStub(wireMockStub);
		then(dsl).contains("body ('expected-body')");
	}

}
