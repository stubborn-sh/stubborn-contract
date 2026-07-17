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

import java.io.File;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;
import java.util.regex.Pattern;

import sh.stubborn.contract.spec.internal.RegexProperty;
import sh.stubborn.contract.verifier.util.ContractVerifierDslConverter;

import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

class WireMockToDslConverterTests {

	private Contract fromGroovyDsl(String groovyDsl) {
		Collection<Contract> contracts = ContractVerifierDslConverter.convertAsCollection(new File("/"),
				"sh.stubborn.contract.spec.Contract.make {\n" + groovyDsl + "\n}");
		return contracts.iterator().next();
	}

	@Test
	void should_produce_a_groovy_dsl_from_wiremock_stub() throws Exception {
		String wireMockStub = """
				{
					"request": {
						"method": "GET",
						"url": "/path",
						"headers" : {
							"X-Custom-Header": {
								"contains": "2134"
							},
							"Accept": {
								"matches": "text/.*"
							}
						}
					},
					"response": {
						"status": 200,
						"body": '{"id": { "value": "132" }, "surname": "Kowalsky", "name": "Jan", "created": "2014-02-02 12:23:43" }',
						"headers": {
							"Content-Type": "text/plain"
						}
					}
				}
				""";
		Contract expected = fromGroovyDsl(
				"""
						request {
							method 'GET'
							url '/path'
							headers {
								header('X-Custom-Header': $(
										consumer(regex('^.*2134.*$')),
										producer('121345')
								))
								header('Accept': $(
										consumer(regex('text/.*')),
										producer('text/plain')
								))
							}
						}
						response {
							status OK()
							body('''{"id": {"value": "132"}, "surname": "Kowalsky", "name": "Jan", "created": "2014-02-02 12:23:43"}''')
							headers {
								header 'Content-Type': 'text/plain'
							}
						}
						""");
		String groovyDsl = WireMockToDslConverter.fromWireMockStub(wireMockStub);
		Contract actual = fromGroovyDsl(groovyDsl);
		assertThat(actual.getRequest().getMethod()).isEqualTo(expected.getRequest().getMethod());
		assertThat(actual.getRequest().getUrl()).isEqualTo(expected.getRequest().getUrl());
		String actualCustomHeaderPattern = findHeaderClientPattern(actual, "X-Custom-Header");
		String expectedCustomHeaderPattern = findHeaderClientPattern(expected, "X-Custom-Header");
		assertThat(actualCustomHeaderPattern).isEqualTo(expectedCustomHeaderPattern);
		String actualAcceptPattern = findHeaderClientPattern(actual, "Accept");
		String expectedAcceptPattern = findHeaderClientPattern(expected, "Accept");
		assertThat(actualAcceptPattern).isEqualTo(expectedAcceptPattern);
		assertThat(StringUtils.trimAllWhitespace(actual.getResponse().toString()))
			.isEqualTo(StringUtils.trimAllWhitespace(expected.getResponse().toString()));
	}

	@Test
	void should_convert_wiremock_stub_with_response_body_containing_json_with_escaped_double_quotes() throws Exception {
		String wireMockStub = """
				{
					"request": {
						"method": "DELETE",
						"urlPattern": "1",
						"headers": {
							"Content-Type": {
								"equalTo": "application/vnd.mymoid-adapter.v2+json; charset=UTF-8"
							}
						}
					},
					"response": {
						"status": 200,
						"body": "{\\"status\\": \\"OK\\"}",
						"headers": {
							"Content-Type": "application/json"
						}
					}
				}
				""";
		Contract expected = fromGroovyDsl("""
				request {
					method 'DELETE'
					url $(consumer(~/1/), producer('1'))
					headers {
						header('Content-Type': 'application/vnd.mymoid-adapter.v2+json; charset=UTF-8')
					}
				}
				response {
					status OK()
					body('''{"status": "OK"}''')
					headers {
						header 'Content-Type': 'application/json'
					}
				}
				""");
		String groovyDsl = WireMockToDslConverter.fromWireMockStub(wireMockStub);
		Contract actual = ContractVerifierDslConverter
			.convertAsCollection(new File("/"), "sh.stubborn.contract.spec.Contract.make {\n" + groovyDsl + "\n}")
			.iterator()
			.next();
		String actualPattern = patternOf(actual.getRequest().getUrl().getClientValue());
		String expectedPattern = patternOf(expected.getRequest().getUrl().getClientValue());
		assertThat(actualPattern).isEqualTo(expectedPattern);
	}

	@Test
	void should_convert_wiremock_stub_with_response_body_containing_integer() throws Exception {
		String wireMockStub = """
				{
				  "request": {
					"method": "POST",
					"url": "/charge/count",
					"headers": {
					  "Content-Type": {
						"equalTo": "application/vnd.creditcard-reporter.v1+json"
					  }
					}
				  },
				  "response": {
					"status": 200,
					"body": 200,
					"headers": {
					  "Content-Type": "application/json"
					}
				  }
				}
				""";
		Contract expected = fromGroovyDsl("""
				request {
					method 'POST'
					url '/charge/count'
					headers {
						header('Content-Type': 'application/vnd.creditcard-reporter.v1+json')
					}
				}
				response {
					status OK()
					body(200)
					headers {
						header 'Content-Type': 'application/json'
					}
				}
				""");
		String groovyDsl = WireMockToDslConverter.fromWireMockStub(wireMockStub);
		Contract actual = fromGroovyDsl(groovyDsl);
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void should_convert_wiremock_stub_with_response_body_as_a_list() throws Exception {
		String wireMockStub = """
				{
				  "request": {
					"method": "POST",
					"url": "/charge/count",
					"headers": {
					  "Content-Type": {
						"equalTo": "application/vnd.creditcard-reporter.v1+json"
					  }
					}
				  },
				  "response": {
					"status": 200,
					"body": '[ {"a":1, "c":"3"}, "b", "a" ]',
					"headers": {
					  "Content-Type": "application/json"
					}
				  }
				}
				""";
		Contract expected = fromGroovyDsl("""
				request {
					method 'POST'
					url '/charge/count'
					headers {
						header('Content-Type': 'application/vnd.creditcard-reporter.v1+json')
					}
				}
				response {
					status OK()
					body('''[
				    {
				        "a": 1,
				        "c": "3"
				    },
				    "b",
				    "a"
				]''')
					headers {
						header 'Content-Type': 'application/json'
					}
				}
				""");
		String groovyDsl = WireMockToDslConverter.fromWireMockStub(wireMockStub);
		Contract actual = fromGroovyDsl(groovyDsl);
		assertThat(StringUtils.trimAllWhitespace(actual.toString()))
			.isEqualTo(StringUtils.trimAllWhitespace(expected.toString()));
	}

	@Test
	void should_convert_wiremock_stub_with_response_body_containing_a_nested_list() throws Exception {
		String wireMockStub = """
				{
				  "request": {
					"method": "POST",
					"url": "/charge/search?pageNumber=0&size=2147483647",
					"headers": {
					  "Content-Type": {
						"equalTo": "application/vnd.creditcard-reporter.v1+json"
					  }
					}
				  },
				  "response": {
					"status": 200,
					"body": '[{"amount":1.01, "name":"Name", "info":{"title":"title1", "payload":null}, "booleanvalue":true, "user":null}, {"amount":2.01, "name":"Name2", "info":{"title":"title2", "payload":null}, "booleanvalue":true, "user":null}]'
					}
				}
				""";
		Contract expected = fromGroovyDsl("""
				request {
					method 'POST'
					url '/charge/search?pageNumber=0&size=2147483647'
					headers {
						header('Content-Type': 'application/vnd.creditcard-reporter.v1+json')
					}
				}
				response {
					status OK()
					body('''[
				    {
				        "amount": 1.01,
				        "name": "Name",
				        "info": {
				            "title": "title1",
				            "payload": null
				        },
				        "booleanvalue": true,
				        "user": null
				    },
				    {
				        "amount": 2.01,
				        "name": "Name2",
				        "info": {
				            "title": "title2",
				            "payload": null
				        },
				        "booleanvalue": true,
				        "user": null
				    }
				]''')
				}
				""");
		String groovyDsl = WireMockToDslConverter.fromWireMockStub(wireMockStub);
		Contract actual = fromGroovyDsl(groovyDsl);
		assertThat(StringUtils.trimAllWhitespace(actual.toString()))
			.isEqualTo(StringUtils.trimAllWhitespace(expected.toString()));
	}

	@Test
	void should_convert_wiremock_stub_with_request_body_checking_equality_to_json() throws Exception {
		String wireMockStub = """
				{
				  "request": {
					"method": "POST",
					"url": "/test",
					"bodyPatterns": [{
						"equalTo": '{"property1":"abc", "property2":"2017-01", "property3":"666", "property4":1428566412}'
					}]
				  },
				  "response": {
					"status": 200
					}
				}
				""";
		Contract expected = fromGroovyDsl("""
				request {
					method 'POST'
					url '/test'
					body('{"property1":"abc", "property2":"2017-01", "property3":"666", "property4":1428566412}')
				}
				response {
					status OK()
				}
				""");
		String groovyDsl = WireMockToDslConverter.fromWireMockStub(wireMockStub);
		Contract actual = fromGroovyDsl(groovyDsl);
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void should_convert_wiremock_stub_with_request_body_checking_matching_to_json() throws Exception {
		String wireMockStub = """
				{
				  "request": {
				    "method": "POST",
				    "url": "/test",
				    "bodyPatterns": [{
				        "matches": "1"
				    }]
				  },
				  "response": {
				    "status": 200
				    }
				}
				""";
		Contract expected = fromGroovyDsl("""
				request {
					method 'POST'
					url '/test'
					body $(consumer(~/1/), producer('1'))
				}
				response {
					status OK()
				}
				""");
		String groovyDsl = WireMockToDslConverter.fromWireMockStub(wireMockStub);
		Contract actual = fromGroovyDsl(groovyDsl);
		assertThat(patternOf(actual.getRequest().getBody().getClientValue()))
			.isEqualTo(patternOf(expected.getRequest().getBody().getClientValue()));
	}

	@Test
	void should_convert_wiremock_stub_with_request_body_with_equal_to_json() throws Exception {
		String wireMockStub = """
				{
				  "request" : {
					"url" : "/test",
					"method" : "POST",
					"bodyPatterns" : [ {
					  "equalToJson" : '{"pan":"4855141150107894", "expirationDate":"2017-01", "dcvx":"178"}',
					  "jsonCompareMode" : "LENIENT"
					} ]
				  },
				  "response" : {
					"status" : 200
				  }
				}
				""";
		Contract expected = fromGroovyDsl("""
				request {
					method 'POST'
					url '/test'
					body '{"pan":"4855141150107894", "expirationDate":"2017-01", "dcvx":"178"}'
				}
				response {
					status OK()
				}
				""");
		String groovyDsl = WireMockToDslConverter.fromWireMockStub(wireMockStub);
		Contract actual = fromGroovyDsl(groovyDsl);
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void should_convert_wiremock_stub_with_request_body_with_equal_to() throws Exception {
		String wireMockStub = """
				{
				  "request" : {
					"url" : "/test",
					"method" : "POST",
					"bodyPatterns" : [ {
					  "equalTo" : '{"pan":"4855141150107894", "expirationDate":"2017-01", "dcvx":"178"}'
					} ]
				  },
				  "response" : {
					"status" : 200
				  }
				}
				""";
		Contract expected = fromGroovyDsl("""
				request {
					method 'POST'
					url '/test'
					body '{"pan":"4855141150107894", "expirationDate":"2017-01", "dcvx":"178"}'
				}
				response {
					status OK()
				}
				""");
		String groovyDsl = WireMockToDslConverter.fromWireMockStub(wireMockStub);
		Contract actual = fromGroovyDsl(groovyDsl);
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void should_convert_wiremock_stub_with_request_body_with_matches() throws Exception {
		String wireMockStub = """
				{
				  "request" : {
					"url" : "/test",
					"method" : "POST",
					"bodyPatterns" : [ {
					  "matches" : "1"
					} ]
				  },
				  "response" : {
					"status" : 200
				  }
				}
				""";
		Contract expected = fromGroovyDsl("""
				request {
					method 'POST'
					url '/test'
					body $(consumer(~/1/), producer('1'))
				}
				response {
					status OK()
				}
				""");
		String groovyDsl = WireMockToDslConverter.fromWireMockStub(wireMockStub);
		Contract actual = fromGroovyDsl(groovyDsl);
		assertThat(patternOf(actual.getRequest().getBody().getClientValue()))
			.isEqualTo(patternOf(expected.getRequest().getBody().getClientValue()));
	}

	@Test
	void should_convert_wiremock_stub_with_priorities() throws Exception {
		String wireMockStub = """
				{
				  "priority" : 2,
				  "request" : {
					"url" : "/test",
					"method" : "POST"
				  },
				  "response" : {
					"status" : 200
				  }
				}
				""";
		Contract expected = fromGroovyDsl("""
				priority 2
				request {
					method 'POST'
					url '/test'
				}
				response {
					status OK()
				}
				""");
		String groovyDsl = WireMockToDslConverter.fromWireMockStub(wireMockStub);
		Contract actual = fromGroovyDsl(groovyDsl);
		assertThat(actual).isEqualTo(expected);
	}

	private static String findHeaderClientPattern(Contract contract, String headerName) {
		return contract.getRequest()
			.getHeaders()
			.getEntries()
			.stream()
			.filter(h -> h.getName().equals(headerName))
			.findFirst()
			.map(h -> patternOf(h.getClientValue()))
			.orElseThrow();
	}

	private static String patternOf(Object value) {
		if (value instanceof RegexProperty rp) {
			return rp.pattern();
		}
		if (value instanceof Pattern p) {
			return p.pattern();
		}
		throw new IllegalArgumentException("Not a pattern: " + value);
	}

}
