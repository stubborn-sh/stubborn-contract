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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.config.TestMode;
import sh.stubborn.contract.verifier.converter.YamlContractConverter;
import sh.stubborn.contract.verifier.dsl.wiremock.WireMockStubVerifier;
import sh.stubborn.contract.verifier.file.ContractMetadata;
import sh.stubborn.contract.spec.internal.RegexPatterns;
import sh.stubborn.contract.verifier.util.ContractVerifierDslConverter;
import sh.stubborn.contract.verifier.util.SyntaxChecker;

import static org.assertj.core.api.Assertions.assertThat;

class MethodBodyBuilderTests implements WireMockStubVerifier {

	ContractVerifierConfigProperties properties;

	SingleTestGenerator.GeneratedClassData generatedClassData;

	@BeforeEach
	void setup() throws URISyntaxException {
		File resourceDir = new File(MethodBodyBuilderTests.class.getResource(".").toURI());
		properties = new ContractVerifierConfigProperties();
		properties.setAssertJsonSize(true);
		properties.setGeneratedTestSourcesDir(resourceDir);
		properties.setGeneratedTestResourcesDir(resourceDir);
		generatedClassData = new SingleTestGenerator.GeneratedClassData("foo", "com.example", resourceDir.toPath());
	}

	private String singleTestGenerator(Contract contractDsl) {
		return new JavaTestGenerator() {
			@Override
			ClassBodyBuilder classBodyBuilder(BlockBuilder builder, GeneratedClassMetaData metaData,
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
		}.buildClass(properties, Collections.singletonList(contractMetadata(contractDsl)), "foo", generatedClassData);
	}

	private ContractMetadata contractMetadata(Contract contractDsl) {
		return new ContractMetadata(new File(".").toPath(), false, 0, null, contractDsl);
	}

	// --- parameterized test argument sources ---

	static Stream<Arguments> standardBuilders() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC),
				Arguments.of("jaxrs-spock", TestFramework.SPOCK, TestMode.JAXRSCLIENT),
				Arguments.of("jaxrs", TestFramework.JUNIT5, TestMode.JAXRSCLIENT),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT),
				Arguments.of("testNG", TestFramework.TESTNG, null));
	}

	static Stream<Arguments> withoutWebclientBuilders() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC),
				Arguments.of("jaxrs-spock", TestFramework.SPOCK, TestMode.JAXRSCLIENT),
				Arguments.of("jaxrs", TestFramework.JUNIT5, TestMode.JAXRSCLIENT),
				Arguments.of("testNG", TestFramework.TESTNG, null));
	}

	static Stream<Arguments> onlySpockAndMockmvc() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC));
	}

	static Stream<Arguments> withoutTestNG() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC),
				Arguments.of("jaxrs-spock", TestFramework.SPOCK, TestMode.JAXRSCLIENT),
				Arguments.of("jaxrs", TestFramework.JUNIT5, TestMode.JAXRSCLIENT),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT));
	}

	static Stream<Arguments> withoutTestNGAndWebclient() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC),
				Arguments.of("jaxrs-spock", TestFramework.SPOCK, TestMode.JAXRSCLIENT),
				Arguments.of("jaxrs", TestFramework.JUNIT5, TestMode.JAXRSCLIENT));
	}

	private void applyBuilder(TestFramework framework, TestMode mode) {
		if (framework != null) {
			properties.setTestFramework(framework);
		}
		if (mode != null) {
			properties.setTestMode(mode);
		}
	}

	// --- tests ---

	@ParameterizedTest(name = "should work with execute and arrays [{0}]")
	@MethodSource("standardBuilders")
	void should_work_with_execute_and_arrays(String methodBuilderName, TestFramework framework, TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.urlPath("/foo");
				r.headers(h -> {
					h.accept(h.applicationJson());
					h.contentType(h.applicationJson());
				});
			});
			c.response(resp -> {
				resp.status(resp.OK());
				resp.body(Map.of("myArray", List.of(
						Map.of("notABugGeneratedHere",
								resp.$(resp.c("foo"),
										resp.p(resp.execute("assertThat((String)$it).isEqualTo(\"foo\")"))),
								"anotherArrayNeededForBug",
								List.of(Map.of("optionalNotEmpty",
										resp.$(resp.c("foo"),
												resp.p(resp.execute("assertThat((String)$it).isEqualTo(\"12\")"))))),
								"yetAnotherArrayNeededForBug",
								List.of(Map.of("optionalNotEmpty",
										resp.$(resp.c("foo"),
												resp.p(resp.execute("assertThat((String)$it).isEqualTo(\"22\")")))))),
						Map.of("anotherArrayNeededForBug2", List.of(Map.of("optionalNotEmpty", resp.$(resp.c("foo"),
								resp.p(resp.execute("assertThat((String)$it).isEqualTo(\"122\")")))))))));
				resp.headers(h -> h.contentType(h.applicationJson()));
			});
		});

		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("$.myArray.[0].anotherArrayNeededForBug.[0].optionalNotEmpty");
		assertThat(test).doesNotContain("cursor");
		assertThat(test).doesNotContain("REGEXP>>");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should work patterns in GString [{0}]")
	@MethodSource("standardBuilders")
	void should_work_patterns_in_GString(String methodBuilderName, TestFramework framework, TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url(r.$(r.consumer(r.regex("/\\d+")), r.producer("/123")));
			});
			c.response(resp -> {
				resp.status(200);
				resp.body(Map.of("ok", true));
			});
		});

		String test = singleTestGenerator(contractDsl);

		assertThat(test).doesNotContain("d+");
		assertThat(test).doesNotContain("REGEXP>>");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should always escape generated chars [{0}]")
	@MethodSource("standardBuilders")
	void should_always_escape_generated_chars(String methodBuilderName, TestFramework framework, TestMode mode) {
		for (int i = 1; i <= 200; i++) {
			applyBuilder(framework, mode);
			Contract contractDsl = Contract.make(c -> {
				c.request(r -> {
					r.method("GET");
					r.urlPath("/v1/users", up -> {
						up.queryParameters(qp -> qp.parameter("userId", r.value(r.regex(r.nonBlank()))));
					});
				});
				c.response(resp -> {
					resp.status(200);
					resp.body(Map.of("ok", resp.value(resp.regex(resp.nonBlank()))));
				});
			});
			String test = singleTestGenerator(contractDsl);

			assertThat(test).doesNotContain("REGEXP>>");
			SyntaxChecker.tryToCompile(methodBuilderName, test);
		}
	}

	@ParameterizedTest(name = "should work with execute and keys with dots [{0}]")
	@MethodSource("standardBuilders")
	void should_work_with_execute_and_keys_with_dots(String methodBuilderName, TestFramework framework, TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.urlPath("/foo");
			});
			c.response(resp -> {
				resp.status(resp.OK());
				resp.body(Map.of("foo", Map.of("my.dotted.response",
						resp.$(resp.c("foo"), resp.p(resp.execute("\"foo\".equals($it)"))))));
				resp.headers(h -> h.contentType(h.applicationJson()));
			});
		});

		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("$.foo.['my.dotted.response']");
		assertThat(test).doesNotContain("cursor");
		assertThat(test).doesNotContain("REGEXP>>");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should fail on nonexistent field [{0}]")
	@MethodSource("standardBuilders")
	void should_fail_on_nonexistent_field(String methodBuilderName, TestFramework framework, TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("/something");
				r.headers(h -> h.contentType(h.applicationJson()));
			});
			c.response(resp -> {
				resp.status(resp.OK());
				resp.headers(h -> h.contentType(h.applicationJson()));
				resp.body(Map.of("doesNotExist", resp.$(resp.p(resp.anyAlphaUnicode()), resp.c("123"))));
			});
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should allow to use execute in request body [{0}]")
	@MethodSource("standardBuilders")
	void should_allow_to_use_execute_in_request_body(String methodBuilderName, TestFramework framework, TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("/something");
				r.body(r.$(r.c("foo"), r.p(r.execute("hashCode()"))));
			});
			c.response(resp -> resp.status(resp.OK()));
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompile(methodBuilderName, test);
		assertThat(test).doesNotContain("executionCommand");
	}

	@ParameterizedTest(name = "should assert the response headers properly [{0}]")
	@MethodSource("standardBuilders")
	void should_assert_the_response_headers_properly(String methodBuilderName, TestFramework framework, TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("POST");
				r.urlPath("/documents/app_statement_v1");
				r.headers(h -> h.contentType(h.applicationPdf()));
				r.body(Map.of("PESEL", "77100604360", "CLIENT_NAME", "STANISLAW STASZIC", "STATEMENT_NUMBER",
						"00200001/C4/2017/1"));
			});
			c.response(resp -> {
				resp.status(resp.OK());
				resp.headers(h -> {
					h.contentType(h.applicationPdf());
					h.header("Content-Length", 4);
				});
			});
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should put L on long values [{0}]")
	@MethodSource("standardBuilders")
	void should_put_L_on_long_values(String methodBuilderName, TestFramework framework, TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("test");
			});
			c.response(resp -> {
				resp.status(resp.OK());
				resp.body(Map.of("createdAt", 1502766000000L, "updatedAt", 1499476115000L));
			});
		});

		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("assertThatJson(parsedJson).field(\"['createdAt']\").isEqualTo(1502766000000L)");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['updatedAt']\").isEqualTo(1499476115000L)");
		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should not put an absent header to the request [{0}]")
	@MethodSource("standardBuilders")
	void should_not_put_an_absent_header_to_the_request(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("/mytest");
				r.headers(h -> {
					h.header("header-before", r.anyNonBlankString());
					h.header("myheader", r.absent());
					h.header("header-after", r.anyNonBlankString());
				});
			});
			c.response(resp -> resp.status(resp.OK()));
		});

		String test = singleTestGenerator(contractDsl);

		assertThat(test).doesNotContain("myheader");
		assertThat(test).contains("header-before");
		assertThat(test).contains("header-after");
		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should reference request from body when body is a string [{0}]")
	@MethodSource("standardBuilders")
	void should_reference_request_from_body_when_body_is_a_string(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("/mytest");
				r.body("{ \"name\": \"My name\" }");
			});
			c.response(resp -> {
				resp.status(resp.OK());
				resp.body(resp.fromRequest().body("$.name"));
			});
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should reference request from body without escaping of non-string [{0}]")
	@MethodSource("standardBuilders")
	void should_reference_request_from_body_without_escaping_of_non_string(String methodBuilderName,
			TestFramework framework, TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("/mytest", u -> {
					u.queryParameters(qp -> {
						qp.parameter("foo", "bar");
						qp.parameter("number", 1);
					});
				});
				r.body("{ \"name\": \"My name\" }");
			});
			c.response(resp -> {
				resp.status(resp.OK());
				resp.body(Map.of("foo", resp.fromRequest().query("foo"), "number", resp.fromRequest().query("number")));
			});
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['foo']\").isEqualTo(\"bar\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['number']\").isEqualTo(1)");
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should generate proper type for large numbers [{0}]")
	@MethodSource("standardBuilders")
	void should_generate_proper_type_for_large_numbers(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("PUT");
				r.urlPath("/example/create");
				r.headers(h -> h.contentType(h.applicationJson()));
				r.body(Map.of("name", r.$(r.consumer(r.regex(".+")), r.producer("string-1")), "updatedTs",
						r.$(r.consumer(r.regex(r.regex("1531916906000").asLong()))), "isDisabled",
						r.$(r.consumer(r.regex(r.anyBoolean())), r.producer(true))));
			});
			c.response(resp -> {
				resp.status(200);
				resp.headers(h -> h.contentType(h.applicationJsonUtf8()));
				resp.body(Map.of("id", resp.$(resp.consumer(2222L), resp.producer(resp.regex("\\d+"))), "name",
						resp.fromRequest().body("name"), "updatedTs", resp.fromRequest().body("updatedTs"),
						"isDisabled", resp.fromRequest().body("isDisabled")));
			});
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['updatedTs']\").isEqualTo(1531916906000L)");
		assertThat(test).doesNotContain("\"updatedTs\":\"1531916906000\"");
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should work for '/' url for [{0}]")
	@MethodSource("standardBuilders")
	void should_work_for_root_url(String methodBuilderName, TestFramework framework, TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.description("Represents a request to the shouldReturnName service\n\n"
					+ "given:\n\ta request to the shouldReturnName service\nwhen:\n\tit is a GET\nthen:\n\treturn Ryan");
			c.request(r -> {
				r.method("GET");
				r.url("/");
			});
			c.response(resp -> {
				resp.status(resp.OK());
				resp.body("Ryan");
				resp.headers(h -> h.contentType(h.textHtml()));
			});
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should use fixed delay milliseconds in the generated test [{0}]")
	@MethodSource("onlySpockAndMockmvc")
	void should_use_fixed_delay_milliseconds_in_generated_test(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("test");
			});
			c.response(resp -> {
				resp.status(resp.OK());
				resp.async();
				resp.fixedDelayMilliseconds(10000);
				resp.body(Map.of("a", "foo"));
			});
		});

		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains(".timeout(10000)");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should not escape a form URL encoded request body [{0}]")
	@MethodSource("standardBuilders")
	void should_not_escape_a_form_URL_encoded_request_body(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("POST");
				r.url("/api/form-endpoint");
				r.headers(h -> h.header("Content-Type", "application/x-www-form-urlencoded"));
				r.body("a=abc&b=123");
			});
			c.response(resp -> resp.status(resp.OK()));
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test).doesNotContain("a=abc&amp;b=123");
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should not escape a form URL encoded request body with oauth token [{0}]")
	@MethodSource("standardBuilders")
	void should_not_escape_a_form_URL_encoded_request_body_with_oauth_token(String methodBuilderName,
			TestFramework framework, TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("POST");
				r.urlPath("/oauth/token");
				r.headers(h -> {
					h.header(h.authorization(), r.anyNonBlankString());
					h.contentType("application/x-www-form-urlencoded; charset=UTF-8");
					h.header(h.accept(), r.anyNonBlankString());
				});
				r.body("username=user&password=password&grant_type=password");
			});
			c.response(resp -> {
				resp.status(200);
				resp.headers(h -> h.contentType(h.applicationJsonUtf8()));
				resp.body(Map.of("refresh_token", "RANDOM_REFRESH_TOKEN", "access_token", "RANDOM_ACCESS_TOKEN",
						"token_type", "bearer", "expires_in", 3600, "scope", List.of("task"), "user",
						Map.of("id", 1, "username", "user", "name", "User")));
			});
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test).contains("username=user&password=password&grant_type=password");
		assertThat(test).doesNotContain("&amp;");
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should not escape a form URL encoded request body another try [{0}]")
	@MethodSource("standardBuilders")
	void should_not_escape_a_form_URL_encoded_request_body_another_try(String methodBuilderName,
			TestFramework framework, TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("POST");
				r.urlPath("/oauth/token");
				r.headers(h -> {
					h.header(h.authorization(), r.anyNonBlankString());
					h.contentType("application/x-www-form-urlencoded; charset=UTF-8");
					h.header(h.accept(), r.anyNonBlankString());
				});
				r.body("username=user&password=password&grant_type=password");
			});
			c.response(resp -> {
				resp.status(200);
				resp.headers(h -> h.contentType(h.applicationJsonUtf8()));
				resp.body(Map.of("refresh_token", "RANDOM_REFRESH_TOKEN", "access_token", "RANDOM_ACCESS_TOKEN",
						"token_type", "bearer", "expires_in", 3600, "scope", List.of("task"), "user",
						Map.of("id", 1, "username", "user", "name", "User")));
			});
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test).doesNotContain("&amp;");
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should work with files that have new lines [{0}]")
	@MethodSource("standardBuilders")
	void should_work_with_files_that_have_new_lines(String methodBuilderName, TestFramework framework, TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("PUT");
				r.headers(h -> h.contentType(h.applicationJson()));
				r.body(r.file("classpath/request.json"));
				r.url("/1");
			});
			c.response(resp -> {
				resp.status(resp.OK());
				resp.body(resp.file("classpath/response.json"));
				resp.headers(h -> h.contentType(h.applicationJson()));
			});
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['status']\").isEqualTo(\"RESPONSE\")");
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@Test
	void classToCheck_should_return_class_of_object() {
		ClassVerifier verifier = new ClassVerifier() {
		};
		Map<String, String> map = new LinkedHashMap<>();
		Integer number = Integer.valueOf(42);
		List<String> list = new ArrayList<>();
		Set<String> set = new java.util.HashSet<>();

		assertThat(verifier.classToCheck(map)).isEqualTo(Map.class);
		assertThat(verifier.classToCheck(number)).isEqualTo(Integer.class);
		assertThat(verifier.classToCheck(list)).isEqualTo(List.class);
		assertThat(verifier.classToCheck(set)).isEqualTo(Set.class);
	}

	@ParameterizedTest(name = "should assert null values without matchers [{0}]")
	@MethodSource("standardBuilders")
	void should_assert_null_values_without_matchers(String methodBuilderName, TestFramework framework, TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("test");
			});
			c.response(resp -> {
				resp.status(resp.OK());
				resp.body(Collections.singletonMap("nullValue", null));
			});
		});

		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("assertThatJson(parsedJson).field(\"['nullValue']\").isNull()");
		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should not escape a regex pattern when matching raw body value [{0}]")
	@MethodSource("standardBuilders")
	void should_not_escape_a_regex_pattern_when_matching_raw_body_value(String methodBuilderName,
			TestFramework framework, TestMode mode) {
		applyBuilder(framework, mode);
		String pattern = "\\d+\\w?";
		String escapedPattern = "\\\\d+\\\\w?";
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("/api/arbitrary-url");
			});
			c.response(resp -> {
				resp.status(resp.OK());
				resp.body(resp.value(resp.stub("1"), resp.test(resp.regex(pattern))));
			});
		});

		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains(escapedPattern);
	}

	@ParameterizedTest(name = "should work with binary payload [{0}]")
	@MethodSource("standardBuilders")
	void should_work_with_binary_payload(String methodBuilderName, TestFramework framework, TestMode mode)
			throws Exception {
		applyBuilder(framework, mode);
		File root = new File("src/test/resources/body_builder/");
		Contract contractDsl = ContractVerifierDslConverter
			.convertAsCollection(root, new File(root, "worksWithPdf.groovy"))
			.stream()
			.findFirst()
			.get();

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should not create an unnecessary empty collection check [{0}]")
	@MethodSource("standardBuilders")
	void should_not_create_an_unnecessary_empty_collection_check(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.name("get_new_toy_specs");
			c.description(
					"Given: A new toy request is submitted\nWhen: I receive the response\nThen: I would receive the toy specs");
			c.request(r -> {
				r.method("GET");
				r.urlPath("/toys", up -> {
					up.queryParameters(qp -> qp.parameter("uuid", "d4d724c4-e36e-4fd2-9baa-af7f5df17399"));
				});
			});
			c.response(resp -> {
				resp.status(200);
				resp.body(Map.of("toyUuid", "d4d724c4-e36e-4fd2-9baa-af7f5df17399", "toyDescription",
						Map.of("name", "Super Whiz Bang Toy", "stockNum", 1234, "manufacturer", "Toy Comp"),
						"toyDetails", List.of(Map.of("inventory", 42, "description", "Toy of the year!!", "dimensions",
								Map.of("height", 45.8, "weight", 12.3, "width", 8.6, "length", 9.3)))));
				resp.bodyMatchers(bm -> {
					bm.jsonPath("$.toyDetails[*].dimensions.height", bm.byRegex(RegexPatterns.nonBlank()));
					bm.jsonPath("$.toyDetails[*].dimensions.weight", bm.byRegex(RegexPatterns.nonBlank()));
					bm.jsonPath("$.toyDetails[*].dimensions.width", bm.byRegex(RegexPatterns.nonBlank()));
					bm.jsonPath("$.toyDetails[*].dimensions.length", bm.byRegex(RegexPatterns.nonBlank()));
				});
				resp.headers(h -> h.contentType(h.applicationJson()));
			});
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test)
			.doesNotContain("assertThatJson(parsedJson).array(\"['toyDetails']\").field(\"['dimensions']\").isEmpty()");
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should work with escaped quotes [{0}]")
	@MethodSource("withoutTestNG")
	void should_work_with_escaped_quotes(String methodBuilderName, TestFramework framework, TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("/test");
				r.headers(h -> {
					h.accept(h.applicationJson());
					h.header("X-Authorization",
							"eyJhbGciOiJIUzI1NiJ9.eyJtZW1iZXJObyI6IjEyMzQ1In0.VdYumw6QkfxaBgFUZNyza1VfNKiZ2WW4JaxIKe-G8HA");
				});
			});
			c.response(resp -> {
				resp.status(resp.OK());
				resp.body(Map.of("test", "\"escaped\""));
				resp.headers(h -> h.contentType(h.applicationJson()));
			});
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test).doesNotContain(".isEqualTo(\"\\\\\"escaped\\\\\"\")");
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should not leave unnecessary isEmpty when using matchers [{0}]")
	@MethodSource("withoutTestNGAndWebclient")
	void should_not_leave_unnecessary_isEmpty_when_using_matchers(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("/test");
				r.headers(h -> {
					h.accept(h.applicationJson());
					h.header("X-Authorization",
							"eyJhbGciOiJIUzI1NiJ9.eyJtZW1iZXJObyI6IjEyMzQ1In0.VdYumw6QkfxaBgFUZNyza1VfNKiZ2WW4JaxIKe-G8HA");
				});
			});
			c.response(resp -> {
				resp.status(resp.OK());
				resp.body(List.of(Map.of("test", "testJson"), Map.of("test", "testJson")));
				resp.headers(h -> h.contentType(h.applicationJson()));
				resp.bodyMatchers(bm -> {
					bm.jsonPath("$", bm.byType(t -> {
						t.minOccurrence(2);
						t.maxOccurrence(2);
					}));
					bm.jsonPath("$[*].test", bm.byType(t -> {
						t.minOccurrence(2);
						t.maxOccurrence(2);
					}));
				});
			});
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test).doesNotContain("assertThatJson(parsedJson).array().isEmpty()");
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should not parse json in a json [{0}]")
	@MethodSource("withoutTestNGAndWebclient")
	void should_not_parse_json_in_a_json(String methodBuilderName, TestFramework framework, TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.name("insertSomething_ShouldReturnHttp200");
			c.description("POST should do sth");
			c.request(r -> {
				r.method("POST");
				r.url("/foo");
				r.body(Map.of("value", "{}"));
				r.headers(h -> h.contentType(h.applicationJson()));
			});
			c.response(resp -> {
				resp.status(200);
				resp.headers(h -> h.contentType(h.applicationJson()));
				resp.body(Map.of("value", "{}"));
			});
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test).doesNotContain(":{}}}");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['value']\").isEqualTo(\"{}\")");
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should not leave empty arrays [{0}]")
	@MethodSource("withoutTestNG")
	void should_not_leave_empty_arrays(String methodBuilderName, TestFramework framework, TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("/list");
			});
			c.response(resp -> {
				resp.status(200);
				resp.body(Map.of("content", Map.of("one", "two", "two", "two", "three", Map.of("six", "seven"))));
				resp.bodyMatchers(bm -> {
					bm.jsonPath("$.content.three.six", bm.byRegex(".*seven.*"));
					bm.jsonPath("$.content.one", bm.byRegex(".*two.*"));
				});
			});
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test).doesNotContain(".isEmpty()");
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should not leave empty arrays in a simple structure [{0}]")
	@MethodSource("withoutTestNG")
	void should_not_leave_empty_arrays_in_a_simple_structure(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("/list");
			});
			c.response(resp -> {
				resp.status(200);
				resp.body(Map.of("content", Map.of("three", Map.of("six", "seven"))));
				resp.bodyMatchers(bm -> {
					bm.jsonPath("$.content.three.six", bm.byRegex(".*seven.*"));
					bm.jsonPath("$.content.one", bm.byRegex(".*two.*"));
				});
			});
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test).doesNotContain(".isEmpty()");
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should not unnecessarily escape non json body [{0}]")
	@MethodSource("withoutTestNG")
	void should_not_unnecessarily_escape_non_json_body(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url(r.$(r.consumer(r.regex("/api/v1/files/" + r.uuid())),
						r.producer("/api/v1/files/b0683f29-741a-4178-b5c6-6e62202e3cf1")));
			});
			c.response(resp -> {
				resp.status(resp.OK());
				resp.body(resp.$(resp.consumer("some-content"), resp.producer(resp.regex(resp.nonBlank()))));
				resp.headers(h -> {
					h.header(h.contentLength(),
							resp.$(resp.consumer(2647691), resp.producer(resp.regex(resp.positiveInt()))));
					h.header(h.contentType(), resp.$(resp.consumer(h.applicationOctetStream()),
							resp.producer(resp.regex(resp.nonBlank()))));
				});
			});
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompile(methodBuilderName, test);
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should not escape headers as jsons [{0}]")
	@MethodSource("standardBuilders")
	void should_not_escape_headers_as_jsons(String methodBuilderName, TestFramework framework, TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.name("my name");
			c.request(r -> {
				r.method("POST");
				r.urlPath("/my-url");
				r.headers(h -> {
					h.contentType(h.applicationJson());
					h.accept(h.applicationJson());
					h.header("my-json-header", " { \"value\": \"123\" } ");
				});
			});
			c.response(resp -> resp.status(resp.OK()));
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test).doesNotContain("[value:123]");
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should work with large numbers [{0}]")
	@MethodSource("standardBuilders")
	void should_work_with_large_numbers(String methodBuilderName, TestFramework framework, TestMode mode)
			throws Exception {
		applyBuilder(framework, mode);
		String yaml = "request:\n" + "  url: /numbers\n" + "  queryParameters:\n" + "    page: 0\n" + "    size: 2\n"
				+ "  method: GET\n" + "  headers:\n" + "    Content-Type: application/json\n" + "\n" + "response:\n"
				+ "  status: 200\n" + "  headers:\n" + "    Content-Type: application/json;charset=UTF-8\n"
				+ "  body:\n" + "    - number: 1541609556000\n" + "    - number: 1541609316000\n" + "  matchers:\n"
				+ "    body:\n" + "      - path: $.[0].number\n" + "        type: by_equality\n"
				+ "      - path: $.[1].number\n" + "        type: by_equality\n";
		File tmpFile = File.createTempFile("foo", ".yml");
		Files.writeString(tmpFile.toPath(), yaml);
		Contract contractDsl = new YamlContractConverter().convertFrom(tmpFile).stream().findFirst().get();

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test).doesNotContain("(1541609556000)");
		assertThat(test).contains("(1541609556000L)");
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should work with empty arrays after doing array matching [{0}]")
	@MethodSource("standardBuilders")
	void should_work_with_empty_arrays_after_doing_array_matching(String methodBuilderName, TestFramework framework,
			TestMode mode) throws Exception {
		applyBuilder(framework, mode);
		String yaml = "name: GET sample\n" + "description: sample\n" + "request:\n" + "  method: GET\n"
				+ "  urlPath: /sample\n" + "response:\n" + "  status: 200\n" + "  body:\n" + "    - foo: \"sample1\"\n"
				+ "      bar: true\n" + "    - foo: \"sample2\"\n" + "      bar: false\n" + "  headers:\n"
				+ "    Content-Type: application/json\n" + "  matchers:\n" + "    body:\n" + "      - path: $..foo\n"
				+ "        type: by_regex\n" + "        value: .*\n" + "      - path: $..bar\n"
				+ "        type: by_regex\n" + "        predefined: any_boolean\n";
		File tmpFile = File.createTempFile("foo", ".yml");
		Files.writeString(tmpFile.toPath(), yaml);
		Contract contractDsl = new YamlContractConverter().convertFrom(tmpFile).stream().findFirst().get();

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test).doesNotContain("assertThatJson(parsedJson).array().isEmpty()");
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should work with query parameters that need to be escaped [{0}]")
	@MethodSource("withoutTestNG")
	void should_work_with_query_parameters_that_need_to_be_escaped(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("POST");
				r.urlPath("/rest/something", up -> {
					up.queryParameters(qp -> qp.parameter("quote", r.equalTo("\"")));
				});
			});
			c.response(resp -> resp.status(resp.OK()));
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test).doesNotContain(".queryParam(\"quote\",\"\"\")");
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should call execute in queryParameters [{0}]")
	@MethodSource("standardBuilders")
	void should_call_execute_in_queryParameters(String methodBuilderName, TestFramework framework, TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("POST");
				r.urlPath("/rest/something", up -> {
					up.queryParameters(qp -> qp.parameter("someHashCode",
							r.$(r.consumer(r.regex(r.anInteger())), r.producer(r.execute("hashCode()")))));
				});
			});
			c.response(resp -> resp.status(resp.OK()));
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test.contains(".queryParam(\"someHashCode\",hashCode())")
				|| test.contains(".queryParam(\"someHashCode\", hashCode())"))
			.isTrue();
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should work with the timeout flag for groovy [{0}]")
	@MethodSource("onlySpockAndMockmvcAndTestNG")
	void should_work_with_the_timeout_flag_for_groovy(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("/hello");
			});
			c.response(resp -> {
				resp.status(200);
				resp.fixedDelayMilliseconds(5000);
			});
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test).contains("timeout");
		stubMappingIsValidWireMockStub(contractDsl);
	}

	static Stream<Arguments> onlySpockAndMockmvcAndTestNG() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC), Arguments.of("testNG", TestFramework.TESTNG, null));
	}

	@ParameterizedTest(name = "should work with the timeout flag for yaml [{0}]")
	@MethodSource("onlySpockAndMockmvcAndTestNG")
	void should_work_with_the_timeout_flag_for_yaml(String methodBuilderName, TestFramework framework, TestMode mode)
			throws Exception {
		applyBuilder(framework, mode);
		String yaml = "request:\n" + "  method: GET\n" + "  url: /hello\n" + "  queryParameters:\n" + "    name: LuLu\n"
				+ "response:\n" + "  status: 200\n" + "  fixedDelayMilliseconds: 5000\n" + "  body: \"Hello LuLu\"\n"
				+ "  async: true\n";
		File tmpFile = File.createTempFile("foo", ".yml");
		Files.writeString(tmpFile.toPath(), yaml);
		Contract contractDsl = new YamlContractConverter().convertFrom(tmpFile).stream().findFirst().get();

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test).contains("timeout");
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should work with body having new lines [{0}]")
	@MethodSource("withoutTestNG")
	void should_work_with_body_having_new_lines(String methodBuilderName, TestFramework framework, TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("/foo");
			});
			c.response(resp -> {
				resp.status(resp.OK());
				resp.headers(h -> h.contentType("application/x-research-info-systems;charset=UTF-8"));
				resp.body("1\n2\n3\n");
			});
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should resolve headers from request correctly [{0}]")
	@MethodSource("withoutTestNG")
	void should_resolve_headers_from_request_correctly(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("PUT");
				r.url("/frauds/name");
				r.body(Map.of("name", r.$(r.anyAlphaUnicode())));
				r.headers(h -> h.contentType("application/json"));
			});
			c.response(resp -> {
				resp.status(resp.OK());
				resp.body(Map.of("result", "Don't worry you're not a fraud"));
				resp.headers(h -> h.contentType("application/json;charset=UTF-8"));
			});
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['result']\").isEqualTo(\"Don't worry");
		assertThat(test).contains("you're not a fraud");
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should work with an array of uuids [{0}]")
	@MethodSource("withoutTestNG")
	void should_work_with_an_array_of_uuids(String methodBuilderName, TestFramework framework, TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.description("TEST ARRAY");
			c.request(r -> {
				r.method("POST");
				r.urlPath(r.$(r.c("/TEST"), r.p("/TEST")));
				r.body(List.of(r.$(r.c(r.anyUuid()), r.p("00000000-0000-0000-0000-000000000002")),
						r.$(r.c(r.anyUuid()), r.p("00000000-0000-0000-0000-000000000001"))));
			});
			c.response(resp -> resp.status(resp.OK()));
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test).doesNotContain("singleValue");
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should work with anyOf that contains special chars [{0}]")
	@MethodSource("withoutTestNG")
	void should_work_with_anyOf_that_contains_special_chars(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.name("anyOf test");
			c.request(r -> {
				r.method("POST");
				r.url("hello");
				r.body(Map.of("type", r.anyOf("VAL", "VAL+VAL")));
			});
			c.response(resp -> resp.status(resp.OK()));
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test).doesNotContain("singleValue");
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should correctly process optional of DslProperty parameters [{0}]")
	@MethodSource("standardBuilders")
	void should_correctly_process_optional_of_DslProperty_parameters(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("/api/foo");
				r.headers(h -> {
					h.header("Content-Type", "application/json");
					h.header("Accept", "application/json");
				});
				r.body(Map.of("key1", r.$(r.client(r.optional(r.anyOf("foo", "bar"))), r.server("bar")), "key2",
						r.$(r.client(r.optional(r.anyNonBlankString())), r.server("bar")), "key3",
						r.$(r.client(r.optional(r.anyEmail())), r.server("foo@bar.com")), "key4",
						r.$(r.optional(r.anyNumber()))));
			});
			c.response(resp -> {
				resp.status(resp.OK());
				resp.headers(h -> h.header("Content-Type", "application/json"));
				resp.body(Map.of("key1",
						resp.$(resp.client("bar"), resp.server(resp.optional(resp.anyOf("foo", "bar")))), "key2",
						resp.$(resp.client("bar"), resp.server(resp.optional(resp.anyNonBlankString()))), "key3",
						resp.$(resp.client("foo@bar.com"), resp.server(resp.optional(resp.anyEmail()))), "key4",
						resp.$(resp.optional(resp.anyNumber()))));
			});
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompile(methodBuilderName, test);
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(name = "should correctly work with Form URL encoded request body [{0}]")
	@MethodSource("standardBuilders")
	void should_correctly_work_with_Form_URL_encoded_request_body(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		applyBuilder(framework, mode);
		Contract contractDsl = Contract.make(c -> {
			c.request(r -> {
				r.method("POST");
				r.url("/exportData");
				r.headers(h -> h.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"));
				r.body("fromDt=16-Aug-2023&toDt=25-Aug-2023");
			});
			c.response(resp -> resp.status(200));
		});

		String test = singleTestGenerator(contractDsl);

		SyntaxChecker.tryToCompile(methodBuilderName, test);
		stubMappingIsValidWireMockStub(contractDsl);
	}

}
