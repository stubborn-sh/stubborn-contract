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

package sh.stubborn.contract.verifier.builder;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.config.TestMode;
import sh.stubborn.contract.verifier.dsl.wiremock.WireMockStubStrategy;
import sh.stubborn.contract.verifier.dsl.wiremock.WireMockStubVerifier;
import sh.stubborn.contract.verifier.file.ContractMetadata;
import sh.stubborn.contract.verifier.util.SyntaxChecker;

import static org.assertj.core.api.Assertions.assertThat;

class JaxRsClientMethodBuilderTests implements WireMockStubVerifier {

	ContractVerifierConfigProperties properties;

	final SingleTestGenerator.GeneratedClassData generatedClassData = new SingleTestGenerator.GeneratedClassData("foo",
			"com.example", new File(".").toPath());

	final GeneratedClassDataForMethod generatedClassDataForMethod = new GeneratedClassDataForMethod(
			new SingleTestGenerator.GeneratedClassData("foo", "bar", new File(".").toPath()), "method");

	// tag::contract_with_cookies[]
	final Contract contractDslWithCookiesValue = Contract.make(c -> {
		c.request(r -> {
			r.method("GET");
			r.url("/foo");
			r.headers(h -> {
				h.header("Accept", "application/json");
			});
			r.cookies(cookies -> {
				cookies.cookie("cookie-key", "cookie-value");
			});
		});
		c.response(r -> {
			r.status(200);
			r.headers(h -> {
				h.header("Content-Type", "application/json");
			});
			r.cookies(cookies -> {
				cookies.cookie("cookie-key", "new-cookie-value");
			});
			r.body(java.util.Map.of("status", "OK"));
		});
	});

	// end::contract_with_cookies[]

	final Contract contractDslWithCookiesPattern = Contract.make(c -> {
		c.request(r -> {
			r.method("GET");
			r.url("/foo");
			r.headers(h -> {
				h.header("Accept", "application/json");
			});
			r.cookies(cookies -> {
				cookies.cookie("cookie-key", r.regex("[A-Za-z]+"));
			});
		});
		c.response(r -> {
			r.status(200);
			r.headers(h -> {
				h.header("Content-Type", "application/json");
			});
			r.cookies(cookies -> {
				cookies.cookie("cookie-key", r.regex("[A-Za-z]+"));
			});
			r.body(java.util.Map.of("status", "OK"));
		});
	});

	final Contract contractDslWithAbsentCookies = Contract.make(c -> {
		c.request(r -> {
			r.method("GET");
			r.url("/foo");
			r.cookies(cookies -> {
				cookies.cookie("cookie-key", r.absent());
			});
		});
		c.response(r -> {
			r.status(200);
			r.body(java.util.Map.of("status", "OK"));
		});
	});

	@BeforeEach
	void setup() {
		properties = new ContractVerifierConfigProperties();
		properties.setAssertJsonSize(true);
		properties.setTestMode(TestMode.JAXRSCLIENT);
	}

	private String singleTestGenerator(Contract contractDsl) {
		return new JavaTestGenerator() {
			@Override
			public ClassBodyBuilder classBodyBuilder(BlockBuilder builder, GeneratedClassMetaData metaData,
					SingleMethodBuilder methodBuilder) {
				return super.classBodyBuilder(builder, metaData, methodBuilder).field(new Field() {
					@Override
					public boolean accept() {
						return metaData.configProperties.getTestMode() == TestMode.JAXRSCLIENT;
					}

					@Override
					public Field call() {
						builder.addLine("WebTarget webTarget");
						return this;
					}
				});
			}
		}.buildClass(properties, List.of(contractMetadata(contractDsl)), "foo", generatedClassData);
	}

	private ContractMetadata contractMetadata(Contract contractDsl) {
		return new ContractMetadata(new File(".").toPath(), false, 0, null, contractDsl);
	}

	// =========================================================================
	// Simple response body assertions
	// =========================================================================

	record MethodBuilderParams(String methodBuilderName, TestFramework framework) {
	}

	static java.util.stream.Stream<MethodBuilderParams> jaxrsParams() {
		return java.util.stream.Stream.of(new MethodBuilderParams("jaxrs-spock", TestFramework.SPOCK),
				new MethodBuilderParams("jaxrs", TestFramework.JUNIT5));
	}

	static java.util.stream.Stream<MethodBuilderParams> jaxrsOnlyParams() {
		return java.util.stream.Stream.of(new MethodBuilderParams("jaxrs", TestFramework.JUNIT5));
	}

	static java.util.stream.Stream<MethodBuilderParams> jaxrsSpockOnlyParams() {
		return java.util.stream.Stream.of(new MethodBuilderParams("jaxrs-spock", TestFramework.SPOCK));
	}

	@ParameterizedTest(name = "should generate assertions for simple response body with {0}")
	@MethodSource("jaxrsParams")
	void should_generate_assertions_for_simple_response_body(MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("test");
			});
			c.response(r -> {
				r.status(r.OK());
				r.body("{\"property1\": \"a\",\"property2\": \"b\"}");
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property2']\").isEqualTo(\"b\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
	}

	@ParameterizedTest(name = "should generate assertions for null and boolean values with {0}")
	@MethodSource("jaxrsParams")
	void should_generate_assertions_for_null_and_boolean_values(MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("test");
			});
			c.response(r -> {
				r.status(r.OK());
				r.body("{\"property1\": \"true\",\"property2\": null,\"property3\": false}");
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property3']\").isEqualTo(false)");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property2']\").isNull()");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"true\")");
		stubMappingIsValidWireMockStub(
				new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, contractDsl), contractDsl)
					.toWireMockClientStub());
		SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
	}

	@ParameterizedTest(
			name = "should generate assertions for simple response body constructed from map with a list with {0}")
	@MethodSource("jaxrsParams")
	void should_generate_assertions_for_response_body_from_map_with_list(MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("test");
			});
			c.response(r -> {
				r.status(r.OK());
				r.body(java.util.Map.of("property1", "a", "property2",
						java.util.List.of(java.util.Map.of("a", "sth"), java.util.Map.of("b", "sthElse"))));
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).array(\"['property2']\").elementWithIndex(0).field(\"['a']\").isEqualTo(\"sth\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).array(\"['property2']\").elementWithIndex(1).field(\"['b']\").isEqualTo(\"sthElse\")");
		stubMappingIsValidWireMockStub(
				new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, contractDsl), contractDsl)
					.toWireMockClientStub());
		SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
	}

	@ParameterizedTest(
			name = "should generate assertions for simple response body from map with a list with {0} with array size check")
	@MethodSource("jaxrsParams")
	void should_generate_assertions_for_response_body_from_map_with_list_with_array_size_check(
			MethodBuilderParams params) {
		String prev = System.getProperty("spring.cloud.contract.verifier.assert.size");
		try {
			System.setProperty("spring.cloud.contract.verifier.assert.size", "true");
			Contract contractDsl = Contract.make(c -> {
				c.request(r -> {
					r.method("GET");
					r.url("test");
				});
				c.response(r -> {
					r.status(r.OK());
					r.body(java.util.Map.of("property1", "a", "property2",
							java.util.List.of(java.util.Map.of("a", "sth"), java.util.Map.of("b", "sthElse"))));
				});
			});
			properties.setTestFramework(params.framework());
			String test = singleTestGenerator(contractDsl);

			assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")");
			assertThat(test).contains(
					"assertThatJson(parsedJson).array(\"['property2']\").elementWithIndex(0).field(\"['a']\").isEqualTo(\"sth\")");
			assertThat(test).contains(
					"assertThatJson(parsedJson).array(\"['property2']\").elementWithIndex(1).field(\"['b']\").isEqualTo(\"sthElse\")");
			stubMappingIsValidWireMockStub(new WireMockStubStrategy("Test",
					new ContractMetadata(null, false, 0, null, contractDsl), contractDsl)
				.toWireMockClientStub());
			SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
		}
		finally {
			if (prev == null) {
				System.clearProperty("spring.cloud.contract.verifier.assert.size");
			}
			else {
				System.setProperty("spring.cloud.contract.verifier.assert.size", prev);
			}
		}
	}

	@ParameterizedTest(name = "should generate proper request when body constructed from map with a list with {0}")
	@MethodSource("jaxrsParams")
	void should_generate_proper_request_when_body_from_map_with_list(MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("test");
				r.body(java.util.Map.of("items", java.util.List.of("HOP")));
			});
			c.response(r -> {
				r.status(r.OK());
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("entity(\"{\\\"items\\\":[\\\"HOP\\\"]}\", \"application/json\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
	}

	@ParameterizedTest(name = "should generate proper request when body constructed from GString with {0}")
	@MethodSource("jaxrsParams")
	void should_generate_proper_request_when_body_from_gstring(MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("test");
				r.body("property1=VAL1");
			});
			c.response(r -> {
				r.status(r.OK());
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("entity(\"property1=VAL1\", \"application/octet-stream\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
	}

	@ParameterizedTest(name = "should generate assertions for array in response body with {0}")
	@MethodSource("jaxrsParams")
	void should_generate_assertions_for_array_in_response_body(MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("test");
			});
			c.response(r -> {
				r.status(r.OK());
				r.body("[{\"property1\": \"a\"},{\"property2\": \"b\"}]");
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains(
				"assertThatJson(parsedJson).array().elementWithIndex(0).field(\"['property1']\").isEqualTo(\"a\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).array().elementWithIndex(1).field(\"['property2']\").isEqualTo(\"b\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
	}

	@ParameterizedTest(name = "should generate assertions for array inside response body element with {0}")
	@MethodSource("jaxrsParams")
	void should_generate_assertions_for_array_inside_response_body_element(MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("test");
			});
			c.response(r -> {
				r.status(r.OK());
				r.body("{\"property1\": [{\"property2\": \"test1\"},{\"property3\": \"test2\"}]}");
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains(
				"assertThatJson(parsedJson).array(\"['property1']\").elementWithIndex(0).field(\"['property2']\").isEqualTo(\"test1\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).array(\"['property1']\").elementWithIndex(1).field(\"['property3']\").isEqualTo(\"test2\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
	}

	@ParameterizedTest(name = "should generate assertions for nested objects in response body with {0}")
	@MethodSource("jaxrsParams")
	void should_generate_assertions_for_nested_objects_in_response_body(MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("test");
			});
			c.response(r -> {
				r.status(r.OK());
				r.body("{\"property1\": \"a\",\"property2\": {\"property3\": \"b\"}}");
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		assertThat(test)
			.contains("assertThatJson(parsedJson).field(\"['property2']\").field(\"['property3']\").isEqualTo(\"b\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
	}

	@ParameterizedTest(name = "should generate regex assertions for map objects in response body with {0}")
	@MethodSource("jaxrsParams")
	void should_generate_regex_assertions_for_map_objects_in_response_body(MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("test");
			});
			c.response(r -> {
				r.status(r.OK());
				r.body(java.util.Map.of("property1", "a", "property2",
						r.$(r.consumer("123"), r.producer(r.regex("[0-9]{3}")))));
				r.headers(h -> {
					h.header("Content-Type", "application/json");
				});
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property2']\").matches(\"[0-9]{3}\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
	}

	@ParameterizedTest(name = "should generate regex assertions for string objects in response body with {0}")
	@MethodSource("jaxrsParams")
	void should_generate_regex_assertions_for_string_objects_in_response_body(MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("test");
			});
			c.response(r -> {
				r.status(r.OK());
				// The body uses a value with consumer/producer - we embed via map
				// approach
				r.body(java.util.Map.of("property1", "a", "property2",
						r.$(r.consumer("123"), r.producer(r.regex("[0-9]{3}")))));
				r.headers(h -> {
					h.header("Content-Type", "application/json");
				});
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property2']\").matches(\"[0-9]{3}\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
	}

	@ParameterizedTest(name = "should ignore Accept header and use request method with {0}")
	@MethodSource("jaxrsParams")
	void should_ignore_accept_header_and_use_request_method(MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("test");
				r.headers(h -> {
					h.header("Accept", "text/plain");
				});
			});
			c.response(r -> {
				r.status(r.OK());
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("request(\"text/plain\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
	}

	@ParameterizedTest(name = "should ignore Content-Type header and use entity method with {0}")
	@MethodSource("jaxrsParams")
	void should_ignore_content_type_header_and_use_entity_method(MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("test");
				r.headers(h -> {
					h.header("Content-Type", "text/plain");
					h.header("Timer", "123");
				});
				r.body("");
			});
			c.response(r -> {
				r.status(r.OK());
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		if (params.framework() == TestFramework.SPOCK) {
			assertThat(test).contains("entity(\"\", \"text/plain\")");
			assertThat(test).contains("header(\"Timer\", '''123''')");
		}
		else {
			assertThat(test).contains("entity(\"\", \"text/plain\")");
			assertThat(test).contains("header(\"Timer\", \"123\")");
		}
		assertThat(test).doesNotContain("Content Type");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
	}

	@Test
	void should_generate_a_call_with_url_path_and_query_parameters_with_jaxrs() {
		Contract contractDsl = buildQueryParamContractWithNameWithDoubleQuote();
		properties.setTestFramework(TestFramework.JUNIT5);
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("queryParam(\"limit\", \"10\"");
		assertThat(test).contains("queryParam(\"offset\", \"20\"");
		assertThat(test).contains("queryParam(\"filter\", \"email\"");
		assertThat(test).contains("queryParam(\"sort\", \"name\"");
		assertThat(test).contains("queryParam(\"search\", \"55\"");
		assertThat(test).contains("queryParam(\"age\", \"99\"");
		assertThat(test).contains("queryParam(\"name\", \"Denis.Stepanov\"");
		assertThat(test).contains("queryParam(\"nameWithDoubleQuote\", \"\\\"quote\\\"\"");
		assertThat(test).contains("queryParam(\"nameWithQuote\", \"'quote'\"");
		assertThat(test).contains("queryParam(\"email\", \"bob@email.com\"");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property2']\").isEqualTo(\"b\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile("jaxrs", test);
	}

	@Test
	void should_generate_a_call_with_url_path_and_query_parameters_with_jaxrs_spock() {
		Contract contractDsl = buildQueryParamContractWithNameWithDoubleQuote();
		properties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("queryParam(\"limit\", '10'");
		assertThat(test).contains("queryParam(\"offset\", '20'");
		assertThat(test).contains("queryParam(\"filter\", 'email'");
		assertThat(test).contains("queryParam(\"sort\", 'name'");
		assertThat(test).contains("queryParam(\"search\", '55'");
		assertThat(test).contains("queryParam(\"age\", '99'");
		assertThat(test).contains("queryParam(\"name\", 'Denis.Stepanov'");
		assertThat(test).contains("queryParam(\"nameWithDoubleQuote\", '''\"quote\"'''");
		assertThat(test).contains("queryParam(\"email\", 'bob@email.com'");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property2']\").isEqualTo(\"b\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile("jaxrs-spock", test);
	}

	private Contract buildQueryParamContractWithNameWithDoubleQuote() {
		return Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.urlPath("/users", up -> {
					up.queryParameters(qp -> {
						qp.parameter("limit", r.$(r.consumer(r.equalTo("20")), r.producer(r.equalTo("10"))));
						qp.parameter("offset", r.$(r.consumer(r.containing("20")), r.producer(r.equalTo("20"))));
						qp.parameter("filter", "email");
						qp.parameter("sort", r.equalTo("name"));
						qp.parameter("search", r.$(r.consumer(r.notMatching("^/[0-9]{2}$")), r.producer("55")));
						qp.parameter("age", r.$(r.consumer(r.notMatching("^\\w*$")), r.producer("99")));
						qp.parameter("name", r.$(r.consumer(r.matching("Denis.*")), r.producer("Denis.Stepanov")));
						qp.parameter("nameWithDoubleQuote", "\"quote\"");
						qp.parameter("nameWithQuote", "'quote'");
						qp.parameter("email", "bob@email.com");
						qp.parameter("hello", r.$(r.consumer(r.matching("Denis.*")), r.producer(r.absent())));
						qp.parameter("hello", r.absent());
					});
				});
			});
			c.response(r -> {
				r.status(r.OK());
				r.body("{\"property1\": \"a\",\"property2\": \"b\"}");
			});
		});
	}

	@ParameterizedTest(
			name = "should generate a call with url path and query parameters with url containing a pattern with {0}")
	@MethodSource("jaxrsParams")
	void should_generate_a_call_with_url_path_and_query_parameters_with_url_containing_a_pattern(
			MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url(r.$(r.consumer(r.regex("/foo/[0-9]+")), r.producer("/foo/123456")), u -> {
					u.queryParameters(qp -> {
						qp.parameter("limit", r.$(r.consumer(r.equalTo("20")), r.producer(r.equalTo("10"))));
						qp.parameter("offset", r.$(r.consumer(r.containing("20")), r.producer(r.equalTo("20"))));
						qp.parameter("filter", "email");
						qp.parameter("sort", r.equalTo("name"));
						qp.parameter("search", r.$(r.consumer(r.notMatching("^/[0-9]{2}$")), r.producer("55")));
						qp.parameter("age", r.$(r.consumer(r.notMatching("^\\w*$")), r.producer("99")));
						qp.parameter("name", r.$(r.consumer(r.matching("Denis.*")), r.producer("Denis.Stepanov")));
						qp.parameter("email", "bob@email.com");
						qp.parameter("hello", r.$(r.consumer(r.matching("Denis.*")), r.producer(r.absent())));
						qp.parameter("hello", r.absent());
					});
				});
			});
			c.response(r -> {
				r.status(r.OK());
				r.body("{\"property1\": \"a\",\"property2\": \"b\"}");
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		String limitParam = "queryParam(\"limit\", '10'";
		String offsetParam = "queryParam(\"offset\", '20'";
		String filterParam = "queryParam(\"filter\", 'email'";
		String sortParam = "queryParam(\"sort\", 'name'";
		String searchParam = "queryParam(\"search\", '55'";
		String ageParam = "queryParam(\"age\", '99'";
		String nameParam = "queryParam(\"name\", 'Denis.Stepanov'";
		String emailParam = "queryParam(\"email\", 'bob@email.com'";
		if (params.framework() == TestFramework.JUNIT5) {
			limitParam = limitParam.replace("'", "\"");
			offsetParam = offsetParam.replace("'", "\"");
			filterParam = filterParam.replace("'", "\"");
			sortParam = sortParam.replace("'", "\"");
			searchParam = searchParam.replace("'", "\"");
			ageParam = ageParam.replace("'", "\"");
			nameParam = nameParam.replace("'", "\"");
			emailParam = emailParam.replace("'", "\"");
		}
		assertThat(test).contains(limitParam);
		assertThat(test).contains(offsetParam);
		assertThat(test).contains(filterParam);
		assertThat(test).contains(sortParam);
		assertThat(test).contains(searchParam);
		assertThat(test).contains(ageParam);
		assertThat(test).contains(nameParam);
		assertThat(test).contains(emailParam);
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property2']\").isEqualTo(\"b\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
	}

	@ParameterizedTest(name = "should generate test for empty body with {0}")
	@MethodSource("jaxrsParams")
	void should_generate_test_for_empty_body(MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("POST");
				r.url("/ws/payments");
				r.body("");
			});
			c.response(r -> {
				r.status(406);
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("entity(\"\", \"application/octet-stream\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
	}

	@ParameterizedTest(
			name = "should not parse the response body if there is no response body specified in the contract with {0}")
	@MethodSource("jaxrsParams")
	void should_not_parse_response_body_if_no_response_body_specified(MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("HEAD");
				r.url("head");
			});
			c.response(r -> {
				r.status(r.OK());
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		if (params.framework() == TestFramework.SPOCK) {
			assertThat(test).doesNotContain("String responseAsString = response.readEntity(String)");
		}
		else {
			assertThat(test).doesNotContain("String responseAsString = response.readEntity(String.class);");
		}
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
	}

	@ParameterizedTest(name = "should generate test for String in response body with {0}")
	@MethodSource("jaxrsParams")
	void should_generate_test_for_string_in_response_body(MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("POST");
				r.url("test");
			});
			c.response(r -> {
				r.status(r.OK());
				r.body("test");
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		if (params.framework() == TestFramework.SPOCK) {
			assertThat(test).contains("String responseAsString = response.readEntity(String)");
			assertThat(test).contains("responseBody == \"test\"");
		}
		else {
			assertThat(test).contains("String responseBody = responseAsString;");
			assertThat(test).contains("assertThat(responseBody).isEqualTo(\"test\");");
		}
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
	}

	@ParameterizedTest(name = "should generate test with uppercase method name with {0}")
	@MethodSource("jaxrsParams")
	void should_generate_test_with_uppercase_method_name(MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("get");
				r.url("/v1/some_cool_requests/e86df6f693de4b35ae648464c5b0dc08");
			});
			c.response(r -> {
				r.status(r.OK());
				r.headers(h -> {
					h.contentType(h.applicationJson());
				});
				r.body("{\"id\":\"789fgh\",\"other_data\":1268}");
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("build(\"GET\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompileWithoutCompileStatic(params.methodBuilderName(), test);
	}

	@Test
	void should_generate_a_call_with_url_path_and_query_parameters_with_junit_docs_example() {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.urlPath("/users", up -> {
					up.queryParameters(qp -> {
						qp.parameter("limit", r.$(r.consumer(r.equalTo("20")), r.producer(r.equalTo("10"))));
						qp.parameter("offset", r.$(r.consumer(r.containing("20")), r.producer(r.equalTo("20"))));
						qp.parameter("filter", "email");
						qp.parameter("sort", r.equalTo("name"));
						qp.parameter("search", r.$(r.consumer(r.notMatching("^/[0-9]{2}$")), r.producer("55")));
						qp.parameter("age", r.$(r.consumer(r.notMatching("^\\w*$")), r.producer("99")));
						qp.parameter("name", r.$(r.consumer(r.matching("Denis.*")), r.producer("Denis.Stepanov")));
						qp.parameter("email", "bob@email.com");
						qp.parameter("hello", r.$(r.consumer(r.matching("Denis.*")), r.producer(r.absent())));
						qp.parameter("hello", r.absent());
					});
				});
			});
			c.response(r -> {
				r.status(r.OK());
				r.body("{\"property1\": \"a\"}");
			});
		});
		properties.setTestFramework(TestFramework.JUNIT5);
		String test = singleTestGenerator(contractDsl);

		assertThat(test.trim()).isEqualTo("""
				package com.example;

				import com.jayway.jsonpath.DocumentContext;
				import com.jayway.jsonpath.JsonPath;
				import org.junit.jupiter.api.Test;
				import org.junit.jupiter.api.extension.ExtendWith;
				import javax.ws.rs.client.Entity;
				import javax.ws.rs.core.Response;

				import static sh.stubborn.contract.verifier.assertion.SpringCloudContractAssertions.assertThat;
				import static sh.stubborn.contract.verifier.util.ContractVerifierUtil.*;
				import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson;
				import static javax.ws.rs.client.Entity.*;

				@SuppressWarnings("rawtypes")
				public class FooTest {
				\tWebTarget webTarget;

				\t@Test
				\tpublic void validate_() throws Exception {

				\t\t// when:
				\t\t\tResponse response = webTarget
				\t\t\t\t\t\t\t.path("/users")
				\t\t\t\t\t\t\t.queryParam("limit", "10")
				\t\t\t\t\t\t\t.queryParam("offset", "20")
				\t\t\t\t\t\t\t.queryParam("filter", "email")
				\t\t\t\t\t\t\t.queryParam("sort", "name")
				\t\t\t\t\t\t\t.queryParam("search", "55")
				\t\t\t\t\t\t\t.queryParam("age", "99")
				\t\t\t\t\t\t\t.queryParam("name", "Denis.Stepanov")
				\t\t\t\t\t\t\t.queryParam("email", "bob@email.com")
				\t\t\t\t\t\t\t.request()
				\t\t\t\t\t\t\t.build("GET")
				\t\t\t\t\t\t\t.invoke();
				\t\t\tString responseAsString = response.readEntity(String.class);

				\t\t// then:
				\t\t\tassertThat(response.getStatus()).isEqualTo(200);

				\t\t// and:
				\t\t\tDocumentContext parsedJson = JsonPath.parse(responseAsString);
				\t\t\tassertThatJson(parsedJson).field("['property1']").isEqualTo("a");
				\t}

				}""");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompileJava("jaxrs", test);
	}

	@Test
	void should_execute_custom_method_for_complex_structures_on_response_side() {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.urlPath("/get");
			});
			c.response(r -> {
				r.status(r.OK());
				r.body(java.util.Map.of("fraudCheckStatus", "OK", "rejectionReason", java.util.Map.of("title", r
					.$(r.consumer((Object) null), r.producer(r.execute("assertThatRejectionReasonIsNull($it)"))))));
			});
		});
		properties.setTestFramework(TestFramework.JUNIT5);
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("assertThatRejectionReasonIsNull(parsedJson.read(\"$.rejectionReason.title\"));");
	}

	private static final String SAMPLE_JSON = "\n  [\n    {\n      \"name\"  : \"iPhone\",\n"
			+ "      \"number\": \"0123-4567-8888\"\n    },\n    {\n      \"name\"  : \"home\",\n"
			+ "      \"number\": \"0123-4567-8910\"\n    }\n  ]\n";

	@Test
	void should_execute_custom_method_for_more_complex_structures_on_response_side_when_using_spock() {
		Contract contractDsl = buildContractWithExecuteMethod();
		properties.setTestFramework(TestFramework.JUNIT5);
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("assertThatUserNameIsNotNull(parsedJson.read(\"$.[0].name\")");
		assertThat(test).contains("assertThatUserNameIsNotNull(parsedJson.read(\"$.[1].name\")");

		DocumentContext context = JsonPath.parse(SAMPLE_JSON);
		assertThat((String) context.read("$.[0].name")).isEqualTo("iPhone");
	}

	@Test
	void should_execute_custom_method_for_more_complex_structures_on_response_side_when_using_junit() {
		Contract contractDsl = buildContractWithExecuteMethod();
		properties.setTestFramework(TestFramework.JUNIT5);
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("assertThatUserNameIsNotNull(parsedJson.read(\"$.[0].name\")");
		assertThat(test).contains("assertThatUserNameIsNotNull(parsedJson.read(\"$.[1].name\")");
	}

	private Contract buildContractWithExecuteMethod() {
		return Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.urlPath("/get");
			});
			c.response(r -> {
				r.status(r.OK());
				r.body(java.util.List.of(
						java.util.Map.of("name",
								r.$(r.consumer("userName 1"),
										r.producer(r.execute("assertThatUserNameIsNotNull($it)")))),
						java.util.Map.of("name", r.$(r.consumer("userName 2"),
								r.producer(r.execute("assertThatUserNameIsNotNull($it)"))))));
			});
		});
	}

	@Test
	void should_support_body_matching_in_response() {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("/get");
			});
			c.response(r -> {
				r.status(r.OK());
				r.body(r.$(r.stub("HELLO FROM STUB"), r.server(r.regex(".*"))));
			});
		});
		properties.setTestFramework(TestFramework.JUNIT5);
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("assertThat(responseBody).matches(\".*\");");
		SyntaxChecker.tryToCompileJava("jaxrs", test);
	}

	@Test
	void should_support_body_matching_in_response_in_spock() {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("/get");
			});
			c.response(r -> {
				r.status(r.OK());
				r.body(r.$(r.stub("HELLO FROM STUB"), r.server(r.regex(".*"))));
			});
		});
		properties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("responseBody ==~ java.util.regex.Pattern.compile(\".*\")");
		SyntaxChecker.tryToCompileGroovy("jaxrs", test);
	}

	@Test
	void should_support_custom_method_execution_in_response() {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("/get");
			});
			c.response(r -> {
				r.status(r.OK());
				r.body(r.$(r.stub("HELLO FROM STUB"), r.server(r.execute("foo($it)"))));
			});
		});
		properties.setTestFramework(TestFramework.JUNIT5);
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("foo(responseBody);");
	}

	@Test
	void should_support_custom_method_execution_in_response_in_spock() {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("/get");
			});
			c.response(r -> {
				r.status(r.OK());
				r.body(r.$(r.stub("HELLO FROM STUB"), r.server(r.execute("foo($it)"))));
			});
		});
		properties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("foo(responseBody)");
		// no static compilation due to bug in Groovy
		// https://issues.apache.org/jira/browse/GROOVY-8055
		SyntaxChecker.tryToCompileGroovy("jaxrs", test, false);
	}

	@ParameterizedTest(name = "should allow c/p version of consumer producer with {0}")
	@MethodSource("jaxrsParams")
	void should_allow_cp_version_of_consumer_producer(MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("test");
			});
			c.response(r -> {
				r.status(r.OK());
				r.body(java.util.Map.of("property1", "a", "property2", r.$(r.c("123"), r.p(r.regex("[0-9]{3}")))));
				r.headers(h -> {
					h.header("Content-Type", "application/json");
				});
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property2']\").matches(\"[0-9]{3}\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
	}

	@ParameterizedTest(name = "should allow easier way of providing dynamic values for [{0}]")
	@MethodSource("jaxrsParams")
	void should_allow_easier_way_of_providing_dynamic_values(MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.urlPath("/get");
				r.body(java.util.Map.ofEntries(java.util.Map.entry("alpha", r.$(r.anyAlphaUnicode())),
						java.util.Map.entry("number", r.$(r.anyNumber())),
						java.util.Map.entry("positiveInt", r.$(r.positiveInt())),
						java.util.Map.entry("aDouble", r.$(r.anyDouble())),
						java.util.Map.entry("aBoolean", r.$(r.aBoolean())),
						java.util.Map.entry("ip", r.$(r.anyIpAddress())),
						java.util.Map.entry("hostname", r.$(r.anyHostname())),
						java.util.Map.entry("email", r.$(r.anyEmail())), java.util.Map.entry("url", r.$(r.anyUrl())),
						java.util.Map.entry("httpsUrl", r.$(r.anyHttpsUrl())),
						java.util.Map.entry("uuid", r.$(r.anyUuid())), java.util.Map.entry("date", r.$(r.anyDate())),
						java.util.Map.entry("dateTime", r.$(r.anyDateTime())),
						java.util.Map.entry("time", r.$(r.anyTime())),
						java.util.Map.entry("iso8601WithOffset", r.$(r.anyIso8601WithOffset())),
						java.util.Map.entry("nonBlankString", r.$(r.anyNonBlankString())),
						java.util.Map.entry("nonEmptyString", r.$(r.anyNonEmptyString())),
						java.util.Map.entry("anyOf", r.$(r.anyOf("foo", "bar")))));
				r.headers(h -> {
					h.contentType(h.applicationJson());
				});
			});
			c.response(r -> {
				r.status(r.OK());
				r.body(java.util.Map.ofEntries(java.util.Map.entry("alpha", r.$(r.anyAlphaUnicode())),
						java.util.Map.entry("number", r.$(r.anyNumber())),
						java.util.Map.entry("positiveInt", r.$(r.positiveInt())),
						java.util.Map.entry("aDouble", r.$(r.anyDouble())),
						java.util.Map.entry("aBoolean", r.$(r.aBoolean())),
						java.util.Map.entry("ip", r.$(r.anyIpAddress())),
						java.util.Map.entry("hostname", r.$(r.anyHostname())),
						java.util.Map.entry("email", r.$(r.anyEmail())), java.util.Map.entry("url", r.$(r.anyUrl())),
						java.util.Map.entry("httpsUrl", r.$(r.anyHttpsUrl())),
						java.util.Map.entry("uuid", r.$(r.anyUuid())), java.util.Map.entry("date", r.$(r.anyDate())),
						java.util.Map.entry("dateTime", r.$(r.anyDateTime())),
						java.util.Map.entry("time", r.$(r.anyTime())),
						java.util.Map.entry("iso8601WithOffset", r.$(r.anyIso8601WithOffset())),
						java.util.Map.entry("nonBlankString", r.$(r.anyNonBlankString())),
						java.util.Map.entry("nonEmptyString", r.$(r.anyNonEmptyString())),
						java.util.Map.entry("anyOf", r.$(r.anyOf("foo", "bar")))));
				r.headers(h -> {
					h.contentType(h.applicationJson());
				});
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		String endOfLineRegexSymbol = params.framework() == TestFramework.SPOCK ? "\\$" : "$";

		assertThat(test).contains("assertThatJson(parsedJson).field(\"['aBoolean']\").matches(\"(true|false)\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['alpha']\").matches(\"[\\\\p{L}]*\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).field(\"['hostname']\").matches(\"((http[s]?|ftp):/)/?([^:/\\\\s]+)(:[0-9]{1,5})?\")");
		assertThat(test)
			.contains("assertThatJson(parsedJson).field(\"['number']\").matches(\"-?(\\\\d*\\\\.\\\\d+|\\\\d+)\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['positiveInt']\").matches(\"([1-9]\\\\d*)\")");
		assertThat(test)
			.contains("assertThatJson(parsedJson).field(\"['aDouble']\").matches(\"-?(\\\\d*\\\\.\\\\d+)\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).field(\"['email']\").matches(\"[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,6}\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).field(\"['ip']\").matches(\"([01]?\\\\d\\\\d?|2[0-4]\\\\d|25[0-5])\\\\.([01]?\\\\d\\\\d?|2[0-4]\\\\d|25[0-5])\\\\.([01]?\\\\d\\\\d?|2[0-4]\\\\d|25[0-5])\\\\.([01]?\\\\d\\\\d?|2[0-4]\\\\d|25[0-5])\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).field(\"['uuid']\").matches(\"[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).field(\"['date']\").matches(\"(\\\\d\\\\d\\\\d\\\\d)-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).field(\"['dateTime']\").matches(\"([0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).field(\"['time']\").matches(\"(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).field(\"['iso8601WithOffset']\").matches(\"([0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])(\\\\.\\\\d+)?(Z|[+-][01]\\\\d:[0-5]\\\\d)\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).field(\"['nonBlankString']\").matches(\"^\\\\s*\\\\S[\\\\S\\\\s]*\")");
		assertThat(test)
			.contains("assertThatJson(parsedJson).field(\"['nonEmptyString']\").matches(\"[\\\\S\\\\s]+\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['anyOf']\").matches(\"^foo"
				+ endOfLineRegexSymbol + "|^bar" + endOfLineRegexSymbol + "\")");
		assertThat(test).doesNotContain("cursor");
		assertThat(test).doesNotContain("REGEXP>>");
		SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
	}

	@ParameterizedTest(name = "should resolve Optional object when used in query parameters for [{0}]")
	@MethodSource("jaxrsParams")
	void should_resolve_optional_object_when_used_in_query_parameters(MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.urlPath("/blacklist", up -> {
					up.queryParameters(qp -> {
						qp.parameter("isActive", r.value(r.consumer(r.optional(r.regex("(true|false)")))));
						qp.parameter("limit", r.value(r.consumer(r.optional(r.regex("([0-9]{1,10})")))));
						qp.parameter("offset", r.value(r.consumer(r.optional(r.regex("([0-9]{1,10})")))));
					});
				});
				r.headers(h -> {
					h.header("Content-Type", "application/json");
				});
			});
			c.response(r -> {
				r.status(200);
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		assertThat(test).doesNotContain("sh.stubborn.contract.spec.internal.OptionalProperty");
		assertThat(test).contains("(([0-9]{1,10}))?");
		SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
	}

	@ParameterizedTest(name = "should keep the custom content type that includes the +json suffix [{0}]")
	@MethodSource("jaxrsParams")
	void should_keep_the_custom_content_type_that_includes_json_suffix(MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("POST");
				r.url("/ping");
				r.headers(h -> {
					h.header("Content-Type", "application/my-content-type+json");
				});
				r.body(r.$(r.test(r.value("test")), r.stub(r.anyNonEmptyString())));
			});
			c.response(r -> {
				r.status(200);
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("\"application/my-content-type+json\")");
		assertThat(test).doesNotContain("\"application/json\")");
		SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
	}

	@ParameterizedTest(name = "should not produce any additional quotes for [{0}]")
	@MethodSource("jaxrsOnlyParams")
	void should_not_produce_any_additional_quotes(MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("POST");
				r.url("/v2/applications/a-TEST-upload/documents");
				r.headers(h -> {
					h.header("Authorization", "foo");
					h.header("Content-Type", "multipart/form-data;boundary=Boundary_1_1831312172_1491482784697");
				});
				r.body("\n--Boundary_1_1831312172_1491482784697\n"
						+ "Content-Disposition: form-data; name=\"file\"\n\nDATA\n"
						+ "--Boundary_1_1831312172_1491482784697--\n");
			});
			c.response(r -> {
				r.status(400);
				r.body("File name is required");
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		assertThat(test).doesNotContain("entity(\"\\\"\\n");
		assertThat(test).contains(
				"entity(\"\\n--Boundary_1_1831312172_1491482784697\\nContent-Disposition: form-data; name=\\\"file\\\"\\n\\nDATA\\n--Boundary_1_1831312172_1491482784697--\\n\", \"multipart/form-data;boundary=Boundary_1_1831312172_1491482784697\"))");
		SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
	}

	@ParameterizedTest(name = "should not produce any additional quotes for json body [{0}]")
	@MethodSource("jaxrsOnlyParams")
	void should_not_produce_any_additional_quotes_for_json_body(MethodBuilderParams params) {
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("POST");
				r.url("/foo");
				r.headers(h -> {
					h.header("Content-Type", "application/json");
				});
				r.body("{ \"foo\": \"bar\"}");
			});
			c.response(r -> {
				r.status(400);
				r.body("File name is required");
			});
		});
		properties.setTestFramework(params.framework());
		String test = singleTestGenerator(contractDsl);

		assertThat(test).doesNotContain("entity(\"\\\"\\n");
		assertThat(test).contains(".build(\"POST\", entity(\"{\\\"foo\\\":\\\"bar\\\"}\", \"application/json\"))");
		SyntaxChecker.tryToCompile(params.methodBuilderName(), test);
	}

	// =========================================================================
	// Cookie tests - JUnit
	// =========================================================================

	@Test
	void should_generate_test_for_cookies_with_string_value_in_jaxrs_junit_test() {
		properties.setTestFramework(TestFramework.JUNIT5);
		String test = singleTestGenerator(contractDslWithCookiesValue);

		assertThat(test).contains(".cookie(\"cookie-key\", \"cookie-value\")");
		assertThat(test).contains("assertThat(response.getCookies().get(\"cookie-key\")).isNotNull();");
		assertThat(test).contains(
				"assertThat(response.getCookies().get(\"cookie-key\").getValue()).isEqualTo(\"new-cookie-value\");");
		SyntaxChecker.tryToCompile("jaxrs", test);
	}

	@Test
	void should_generate_test_for_cookies_with_pattern_in_jaxrs_junit_test() {
		properties.setTestFramework(TestFramework.JUNIT5);
		String test = singleTestGenerator(contractDslWithCookiesPattern);

		assertThat(test).doesNotContain(".cookie(\"cookie-key\", \"[A-Za-z]+\")");
		assertThat(test).contains(".cookie(\"cookie-key\", \"");
		assertThat(test).contains("assertThat(response.getCookies().get(\"cookie-key\")).isNotNull();");
		assertThat(test)
			.contains("assertThat(response.getCookies().get(\"cookie-key\").getValue()).matches(\"[A-Za-z]+\");");
		SyntaxChecker.tryToCompile("jaxrs", test);
	}

	@Test
	void should_not_generate_cookie_assertions_with_absent_value_in_jaxrs_junit_test() {
		properties.setTestFramework(TestFramework.JUNIT5);
		String test = singleTestGenerator(contractDslWithAbsentCookies);

		assertThat(test).doesNotContain("cookie");
		SyntaxChecker.tryToCompile("jaxrs", test);
	}

	// =========================================================================
	// Cookie tests - Spock
	// =========================================================================

	@Test
	void should_generate_test_for_cookies_with_string_value_in_jaxrs_spock_test() {
		properties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDslWithCookiesValue);

		assertThat(test).contains(".cookie('cookie-key', 'cookie-value')");
		assertThat(test).contains("response.getCookies().get(\"cookie-key\") != null");
		assertThat(test).contains("response.getCookies().get(\"cookie-key\").getValue() == \"new-cookie-value\"");
		SyntaxChecker.tryToCompile("jaxrs-spock", test);
	}

	@Test
	void should_generate_test_for_cookies_with_pattern_in_jaxrs_spock_test() {
		properties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDslWithCookiesPattern);

		assertThat(test).doesNotContain(".cookie('cookie-key', '[A-Za-z]+')");
		assertThat(test).contains(".cookie('cookie-key', '");
		assertThat(test).contains("response.getCookies().get(\"cookie-key\") != null");
		assertThat(test).contains(
				"response.getCookies().get(\"cookie-key\").getValue() ==~ java.util.regex.Pattern.compile(\"[A-Za-z]+\")");
		SyntaxChecker.tryToCompile("jaxrs-spock", test);
	}

	@Test
	void should_not_generate_cookie_assertions_with_absent_value_in_jaxrs_spock_test() {
		properties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDslWithAbsentCookies);

		assertThat(test).doesNotContain("cookie");
		SyntaxChecker.tryToCompile("jaxrs-spock", test);
	}

}
