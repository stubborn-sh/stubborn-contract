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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.config.TestMode;
import sh.stubborn.contract.verifier.util.ContractVerifierDslConverter;
import sh.stubborn.contract.verifier.dsl.wiremock.WireMockStubVerifier;
import sh.stubborn.contract.verifier.file.ContractMetadata;
import sh.stubborn.contract.verifier.util.SyntaxChecker;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jakub Kubrynski
 * @author Marcin Grzejszczak
 */
class SpringTestMethodBodyBuildersTests implements WireMockStubVerifier {

	ContractVerifierConfigProperties configProperties;

	static final SingleTestGenerator.GeneratedClassData GENERATED_CLASS_DATA = new SingleTestGenerator.GeneratedClassData(
			"foo", "com.example", new File("./target/generated-test-sources").toPath());

	static final Contract CONTRACT_DSL_WITH_COOKIES_VALUE = parseContract("""
			Contract.make {
				request {
					method "GET"
					url "/foo"
					headers { header 'Accept': 'application/json' }
					cookies { cookie 'cookie-key': 'cookie-value' }
				}
				response {
					status 200
					headers { header 'Content-Type': 'application/json' }
					cookies { cookie 'cookie-key': 'new-cookie-value' }
					body([status: 'OK'])
				}
			}
			""");

	static final Contract CONTRACT_DSL_WITH_COOKIES_PATTERN = parseContract("""
			Contract.make {
				request {
					method "GET"
					url "/foo"
					headers { header 'Accept': 'application/json' }
					cookies { cookie 'cookie-key': regex('[A-Za-z]+') }
				}
				response {
					status 200
					headers { header 'Content-Type': 'application/json' }
					cookies { cookie 'cookie-key': regex('[A-Za-z]+') }
					body([status: 'OK'])
				}
			}
			""");

	static final Contract CONTRACT_DSL_WITH_ABSENT_COOKIES = parseContract("""
			Contract.make {
				request {
					method "GET"
					url "/foo"
					cookies { cookie 'cookie-key': absent() }
				}
				response {
					status 200
					body([status: 'OK'])
				}
			}
			""");

	static final Contract DSL_WITH_OPTIONALS_IN_STRING = parseContract(
			"""
					Contract.make {
						priority 1
						request {
							method POST()
							url '/users/password'
							headers { contentType(applicationJson()) }
							body(
								email: $(consumer(optional(regex(email()))), producer('abc@abc.com')),
								callback_url: $(consumer(regex(hostname())), producer('http://partners.com'))
							)
						}
						response {
							status 404
							headers { contentType(applicationJson()) }
							body(
								code: value(consumer("123123"), producer(optional("123123"))),
								message: "User not found by email = [${value(producer(regex(email())), consumer('not.existing@user.com'))}]"
							)
						}
					}
					""");

	static final Contract DSL_WITH_OPTIONALS = parseContract(
			"""
					Contract.make {
						priority 1
						request {
							method POST()
							url '/users/password'
							headers { contentType(applicationJson()) }
							body(
								\""" {
									"email" : "${value(consumer(optional(regex(email()))), producer('abc@abc.com'))}",
									"callback_url" : "${value(consumer(regex(hostname())), producer('http://partners.com'))}"
								}\"""
							)
						}
						response {
							status 404
							headers { contentType(applicationJson()) }
							body(
								\""" {
									"code" : "${value(consumer(123123), producer(optional(123123)))}",
									"message" : "User not found by email = [${value(producer(regex(email())), consumer('not.existing@user.com'))}]"
								}\"""
							)
						}
					}
					""");

	static final Contract DSL_WITH_ONLY_ONE_SIDE_FOR_DOCS = parseContract("""
			Contract.make {
				request {
					method 'PUT'
					url value(consumer(regex('/foo/[0-9]{5}')))
					body([
						requestElement: $(consumer(regex('[0-9]{5}')))
					])
					headers {
						header('header', $(consumer(regex('application\\\\/vnd\\\\.fraud\\\\.v1\\\\+json;.*'))))
					}
				}
				response {
					status OK()
					body([
						responseElement: $(producer(regex('[0-9]{7}')))
					])
					headers {
						contentType("application/vnd.fraud.v1+json")
					}
				}
			}
			""");

	@BeforeEach
	void setup() {
		configProperties = new ContractVerifierConfigProperties();
		configProperties.setAssertJsonSize(true);
		configProperties.setGeneratedTestResourcesDir(new File("./target/generated-test-resources"));
		configProperties.setGeneratedTestSourcesDir(new File("./target/generated-test-sources"));
		configProperties.getGeneratedTestResourcesDir().mkdirs();
		configProperties.getGeneratedTestSourcesDir().mkdirs();
	}

	@AfterEach
	void clearSystemProperties() {
		System.clearProperty("spring.cloud.contract.verifier.assert.size");
	}

	private static Contract parseContract(String groovyDsl) {
		return ContractVerifierDslConverter
			.convertAsCollection(new File("/"), "import sh.stubborn.contract.spec.Contract\n" + groovyDsl)
			.iterator()
			.next();
	}

	private String singleTestGenerator(Contract contractDsl) {
		return singleTestGenerator(Collections.singletonList(contractDsl));
	}

	private String singleTestGenerator(Collection<Contract> contractDsls) {
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
		}.buildClass(configProperties, Collections.singletonList(contractMetadata(contractDsls)), "foo",
				GENERATED_CLASS_DATA);
	}

	private ContractMetadata contractMetadata(Collection<Contract> contractDsls) {
		return new ContractMetadata(new File("./target/").toPath(), false, 0, null, contractDsls);
	}

	private void applyBuilder(TestFramework framework, TestMode testMode) {
		if (framework != null)
			configProperties.setTestFramework(framework);
		if (testMode != null)
			configProperties.setTestMode(testMode);
	}

	static Stream<Arguments> springBuilders() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("testng", TestFramework.TESTNG, null), Arguments.of("mockmvc", null, TestMode.MOCKMVC),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT));
	}

	static Stream<Arguments> threeBuilders() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("testng", TestFramework.TESTNG, null), Arguments.of("mockmvc", null, TestMode.MOCKMVC));
	}

	static Stream<Arguments> twoBuilders() {
		return Stream.of(Arguments.of("mockmvc", null, TestMode.MOCKMVC),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT));
	}

	static Stream<Arguments> sixBuilders() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("testng", TestFramework.TESTNG, null), Arguments.of("mockmvc", null, TestMode.MOCKMVC),
				Arguments.of("jaxrs-spock", TestFramework.SPOCK, TestMode.JAXRSCLIENT),
				Arguments.of("jaxrs", TestFramework.JUNIT5, TestMode.JAXRSCLIENT),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT));
	}

	static Stream<Arguments> sevenBuilders() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("testng", TestFramework.TESTNG, null), Arguments.of("mockmvc", null, TestMode.MOCKMVC),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT),
				Arguments.of("spock-mockmvc", TestFramework.SPOCK, TestMode.MOCKMVC),
				Arguments.of("spock-explicit", TestFramework.SPOCK, TestMode.EXPLICIT),
				Arguments.of("spock-webclient", TestFramework.SPOCK, TestMode.WEBTESTCLIENT));
	}

	static Stream<Arguments> notInProgressBuilders() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("testng", TestFramework.TESTNG, null), Arguments.of("junit5", TestFramework.JUNIT5, null),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC),
				Arguments.of("jaxrs-spock", TestFramework.SPOCK, TestMode.JAXRSCLIENT),
				Arguments.of("jaxrs", TestFramework.JUNIT5, TestMode.JAXRSCLIENT),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT));
	}

	static Stream<Arguments> largeNumbersBuilders() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("testng", TestFramework.TESTNG, null), Arguments.of("junit", TestFramework.JUNIT5, null),
				Arguments.of("junit5", TestFramework.JUNIT5, null));
	}

	static Stream<Arguments> optionalContracts() {
		return Stream.of(Arguments.of(DSL_WITH_OPTIONALS), Arguments.of(DSL_WITH_OPTIONALS_IN_STRING));
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_assertions_for_simple_response_body_with(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method GET(); url "test" }
					response {
						status OK()
						body '{"property1": "a","property2": "b"}'
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property2']\").isEqualTo(\"b\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_assertions_for_null_and_boolean_values_with(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method GET(); url 'test' }
					response {
						status OK()
						body '{"property1": "true","property2": null,"property3": false}'
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"true\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property2']\").isNull()");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property3']\").isEqualTo(false)");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_assertions_for_simple_response_body_constructed_from_map_with_a_list_with(
			String methodBuilderName, TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method "GET"; url "test" }
					response {
						status OK()
						body(property1: 'a', property2: [[a: 'sth'], [b: 'sthElse']])
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).array(\"['property2']\").elementWithIndex(0).field(\"['a']\").isEqualTo(\"sth\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).array(\"['property2']\").elementWithIndex(1).field(\"['b']\").isEqualTo(\"sthElse\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_assertions_for_simple_response_body_constructed_from_map_with_a_list_with_array_size_check(
			String methodBuilderName, TestFramework framework, TestMode testMode) {
		System.setProperty("spring.cloud.contract.verifier.assert.size", "true");
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'GET'; url 'test' }
					response {
						status OK()
						body(property1: 'a', property2: [[a: 'sth'], [b: 'sthElse']])
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).array(\"['property2']\").elementWithIndex(0).field(\"['a']\").isEqualTo(\"sth\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).array(\"['property2']\").elementWithIndex(1).field(\"['b']\").isEqualTo(\"sthElse\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_proper_request_when_body_constructed_from_map_with_a_list(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method "GET"; url "test"; body(items: ['HOP']) }
					response { status OK() }
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		if (framework == TestFramework.SPOCK) {
			assertThat(test).contains(".body('''{\"items\":[\"HOP\"]}''')");
		}
		else {
			assertThat(test).contains(".body(\"{\\\"items\\\":[\\\"HOP\\\"]}\")");
		}
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_proper_request_when_body_constructed_from_GString_with(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'GET'; url 'test'; body('property1=VAL1') }
					response { status OK() }
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		if (framework == TestFramework.SPOCK) {
			assertThat(test).contains(".body('''property1=VAL1''')");
		}
		else {
			assertThat(test).contains(".body(\"property1=VAL1\")");
		}
		SyntaxChecker.tryToCompile(methodBuilderName, test);
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_assertions_for_a_response_body_containing_map_with_integers_as_keys_with(
			String methodBuilderName, TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'GET'; url 'test' }
					response { status OK(); body(property: [14: 0.0, 7: 0.0]) }
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property']\").field(7).isEqualTo(0.0)");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property']\").field(14).isEqualTo(0.0)");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_assertions_for_array_in_response_body_with(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'GET'; url 'test' }
					response { status OK(); body '[{"property1":"a"},{"property2":"b"}]' }
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(
				"assertThatJson(parsedJson).array().elementWithIndex(0).field(\"['property1']\").isEqualTo(\"a\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).array().elementWithIndex(1).field(\"['property2']\").isEqualTo(\"b\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_assertions_for_array_inside_response_body_element_with(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method "GET"; url "test" }
					response {
						status OK()
						body '{"property1":[{"property2":"test1"},{"property3":"test2"}]}'
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(
				"assertThatJson(parsedJson).array(\"['property1']\").elementWithIndex(0).field(\"['property2']\").isEqualTo(\"test1\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).array(\"['property1']\").elementWithIndex(1).field(\"['property3']\").isEqualTo(\"test2\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_assertions_for_nested_objects_in_response_body_with(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method "GET"; url "test" }
					response {
						status OK()
						body '{"property1":"a","property2":{"property3":"b"}}'
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test)
			.contains("assertThatJson(parsedJson).field(\"['property2']\").field(\"['property3']\").isEqualTo(\"b\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_assertions_for_a_response_body_containing_an_empty_list_with(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method "GET"; url "/url" }
					response { status OK(); body("[]") }
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatJson(parsedJson).isEmpty()");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_assertions_for_a_response_body_containing_an_empty_map_with(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method "GET"; url "/url" }
					response { status OK(); body("{}") }
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatJson(parsedJson).isEmpty()");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_regex_assertions_for_map_objects_in_response_body_with(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method "GET"; url "test" }
					response {
						status OK()
						body(property1: "a", property2: value(consumer('123'), producer(regex('[0-9]{3}'))))
						headers { contentType(applicationJson()) }
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property2']\").matches(\"[0-9]{3}\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_regex_assertions_for_string_objects_in_response_body_with(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'GET'; url 'test' }
					response {
						status OK()
						body(""" + "\"\"\"" + """
				{"property1":"a","property2":"${value(consumer('123'), producer(regex('[0-9]{3}')))}"}
				""" + "\"\"\"" + """
				)
						headers { contentType(applicationJson()) }
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property2']\").matches(\"[0-9]{3}\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_escaped_regex_assertions_for_string_objects_in_response_body_with(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'GET'; url 'test' }
					response {
						status OK()
						body(""" + "\"\"\"" + """
				{"property":"  ${value(consumer('123'), producer(regex('\\\\d+')))}"}
				""" + "\"\"\"" + """
				)
						headers { contentType(applicationJson()) }
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property']\").matches(\"\\\\d+\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_a_call_with_an_url_path_and_query_parameters_with(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request {
						method 'GET'
						urlPath("/users/${value(regex("1"))}") {
							queryParameters {
								parameter 'limit': $(consumer(equalTo('20')), producer(equalTo('10')))
								parameter 'offset': $(consumer(containing("20")), producer(equalTo('20')))
								parameter 'filter': 'email'
								parameter 'sort': equalTo("name")
								parameter 'search': $(consumer(notMatching(~/^\\/[0-9]{2}$/)), producer('55'))
								parameter 'age': $(consumer(notMatching("^\\\\w*\\$")), producer('99'))
								parameter 'name': $(consumer(matching('Denis.*')), producer('Denis.Stepanov'))
								parameter 'email': 'bob@email.com'
								parameter 'hello': $(consumer(matching('Denis.*')), producer(absent()))
								parameter 'hello': absent()
							}
						}
					}
					response {
						status OK()
						body '{"property1":"a","property2":"b"}'
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(".queryParam(\"limit\",\"10\")");
		assertThat(test).contains(".queryParam(\"offset\",\"20\")");
		assertThat(test).contains(".queryParam(\"filter\",\"email\")");
		assertThat(test).contains(".queryParam(\"sort\",\"name\")");
		assertThat(test).contains(".queryParam(\"search\",\"55\")");
		assertThat(test).contains(".queryParam(\"age\",\"99\")");
		assertThat(test).contains(".queryParam(\"name\",\"Denis.Stepanov\")");
		assertThat(test).contains(".queryParam(\"email\",\"bob@email.com\")");
		assertThat(test).contains(".get(\"/users/1\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property2']\").isEqualTo(\"b\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_a_call_with_an_url_path_and_query_parameters_with_url_containing_a_pattern_with(
			String methodBuilderName, TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request {
						method 'GET'
						url($(consumer(regex('/foo/[0-9]+')), producer('/foo/123456'))) {
							queryParameters {
								parameter 'limit': $(consumer(equalTo('20')), producer(equalTo('10')))
								parameter 'offset': $(consumer(containing('20')), producer(equalTo('20')))
								parameter 'filter': 'email'
								parameter 'sort': equalTo('name')
								parameter 'search': $(consumer(notMatching(~/^\\/[0-9]{2}$/)), producer('55'))
								parameter 'age': $(consumer(notMatching("^\\\\w*\\$")), producer('99'))
								parameter 'name': $(consumer(matching('Denis.*')), producer('Denis.Stepanov'))
								parameter 'email': 'bob@email.com'
								parameter 'hello': $(consumer(matching('Denis.*')), producer(absent()))
								parameter 'hello': absent()
							}
						}
					}
					response {
						status OK()
						body '{"property1":"a","property2":"b"}'
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(".queryParam(\"limit\",\"10\")");
		assertThat(test).contains(".queryParam(\"offset\",\"20\")");
		assertThat(test).contains(".queryParam(\"filter\",\"email\")");
		assertThat(test).contains(".queryParam(\"sort\",\"name\")");
		assertThat(test).contains(".queryParam(\"search\",\"55\")");
		assertThat(test).contains(".queryParam(\"age\",\"99\")");
		assertThat(test).contains(".queryParam(\"name\",\"Denis.Stepanov\")");
		assertThat(test).contains(".queryParam(\"email\",\"bob@email.com\")");
		assertThat(test).contains(".get(\"/foo/123456\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property2']\").isEqualTo(\"b\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_test_for_empty_body_with(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method(POST()); url('/ws/payments'); body("") }
					response { status 406 }
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		if (framework == TestFramework.SPOCK) {
			assertThat(test).contains(".body('''''')");
		}
		else {
			assertThat(test).contains(".body(\"\")");
		}
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_test_for_String_in_response_body_with(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'POST'; url 'test' }
					response { status OK(); body 'test' }
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		if (framework == TestFramework.SPOCK) {
			assertThat(test).contains("String responseBody = response.body.asString()");
			assertThat(test).contains("responseBody == 'test'");
		}
		else {
			assertThat(test).contains("String responseBody = response.getBody().asString();");
			assertThat(test).contains("assertThat(responseBody).isEqualTo(\"test\");");
		}
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_regex_test_for_String_in_response_header_with(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract(
				"""
						Contract.make {
							request {
								method 'POST'
								url $(consumer(regex('/partners/[0-9]+/users')), producer('/partners/1000/users'))
								headers { contentType(applicationJson()) }
								body(first_name: 'John', last_name: 'Smith', personal_id: '12345678901',
									phone_number: '500500500', invitation_token: '00fec7141bb94793bfe7ae1d0f39bda0', password: 'john')
							}
							response {
								status 201
								headers {
									header 'Location': $(consumer('http://localhost/partners/1000/users/1001'),
										producer(regex('http://localhost/partners/[0-9]+/users/[0-9]+')))
								}
							}
						}
						""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		if (framework == TestFramework.SPOCK) {
			assertThat(test).contains(
					"response.header(\"Location\") ==~ java.util.regex.Pattern.compile('http://localhost/partners/[0-9]+/users/[0-9]+')");
		}
		else {
			assertThat(test).contains(
					"assertThat(response.header(\"Location\")).matches(\"http://localhost/partners/[0-9]+/users/[0-9]+\");");
		}
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_regex_with_helper_method_with(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract(
				"""
						Contract.make {
							request {
								method 'POST'
								url $(consumer(regex('/partners/[0-9]+/users')), producer('/partners/1000/users'))
								headers { contentType(applicationJson()) }
								body(first_name: 'John', last_name: 'Smith', personal_id: '12345678901',
									phone_number: '500500500', invitation_token: '00fec7141bb94793bfe7ae1d0f39bda0', password: 'john')
							}
							response {
								status 201
								headers {
									header 'Location': $(consumer('http://localhost/partners/1000/users/1001'),
										producer(regex("^${hostname()}/partners/[0-9]+/users/[0-9]+")))
								}
							}
						}
						""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		if (framework == TestFramework.SPOCK) {
			assertThat(test).contains(
					"response.header(\"Location\") ==~ java.util.regex.Pattern.compile('^((http[s]?|ftp):/)/?([^:/\\\\s]+)(:[0-9]{1,5})?/partners/[0-9]+/users/[0-9]+')");
		}
		else {
			assertThat(test).contains(
					"assertThat(response.header(\"Location\")).matches(\"^((http[s]?|ftp):/)/?([^:/\\\\s]+)(:[0-9]{1,5})?/partners/[0-9]+/users/[0-9]+\");");
		}
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_work_with_more_complex_stuff_and_jsonpaths_with(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					priority 10
					request {
						method 'POST'
						url '/validation/client'
						headers { contentType(applicationJson()) }
						body(bank_account_number: '0014282912345698765432161182', email: 'foo@bar.com',
							phone_number: '100299300', personal_id: 'ABC123456')
					}
					response {
						status OK()
						body(errors: [[property: "bank_account_number", message: "incorrect_format"]])
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(
				"assertThatJson(parsedJson).array(\"['errors']\").elementWithIndex(0).field(\"['property']\").isEqualTo(\"bank_account_number\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).array(\"['errors']\").elementWithIndex(0).field(\"['message']\").isEqualTo(\"incorrect_format\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_work_properly_with_GString_url_with(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract(
				"""
						Contract.make {
							request {
								method PUT()
								url "/partners/${value(consumer(regex('^[0-9]*\\$')), producer('11'))}/agents/11/customers/09665703Z"
								headers { contentType(applicationJson()) }
								body(first_name: 'Josef')
							}
							response { status 422 }
						}
						""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("/partners/11/agents/11/customers/09665703Z");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_resolve_configProperties_in_GString_with_regular_expression_with(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract(
				"""
						Contract.make {
							priority 1
							request {
								method POST()
								url '/users/password'
								headers { contentType(applicationJson()) }
								body(
									email: $(consumer(regex(email())), producer('not.existing@user.com')),
									callback_url: $(consumer(regex(hostname())), producer('http://partners.com'))
								)
							}
							response {
								status 404
								headers { contentType(applicationJson()) }
								body(
									code: 4,
									message: "User not found by email = [${value(producer(regex(email())), consumer('not.existing@user.com'))}]"
								)
							}
						}
						""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(
				"assertThatJson(parsedJson).field(\"['message']\").matches(\"User not found by email = \\\\\\\\[[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\\\\\.[a-zA-Z]{2,6}\\\\\\\\]\")");

		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("optionalContracts")
	void should_not_omit_the_optional_field_in_the_test_creation_with_MockMvcSpockMethodBodyBuilder(
			Contract contractDsl) {
		configProperties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("\"email\":\"abc@abc.com\"");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['code']\").matches(\"(123123)?\")");
		assertThat(test).doesNotContain("REGEXP");
		assertThat(test).doesNotContain("OPTIONAL");
		assertThat(test).doesNotContain("OptionalProperty");
		SyntaxChecker.tryToCompileGroovy("spock", test);
	}

	@ParameterizedTest
	@MethodSource("optionalContracts")
	void should_not_omit_the_optional_field_in_the_test_creation_with_MockMvcJUnitMethodBodyBuilder(
			Contract contractDsl) {
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("\\\"email\\\":\\\"abc@abc.com\\\"");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['code']\").matches(\"(123123)?\");");
		assertThat(test).doesNotContain("REGEXP");
		assertThat(test).doesNotContain("OPTIONAL");
		assertThat(test).doesNotContain("OptionalProperty");
		SyntaxChecker.tryToCompileJava("mockmvc", test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_make_the_execute_method_work_with(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract(
				"""
						Contract.make {
							request {
								method \"\"\"PUT\"\"\"
								url \"\"\"/fraudcheck\"\"\"
								body(\"\"\"
									{
									"clientPesel":"${value(consumer(regex('[0-9]{10}')), producer('1234567890'))}",
									"loanAmount":123.123
									}
								\"\"\")
								headers {
									header(\"\"\"Content-Type\"\"\", \"\"\"application/vnd.fraud.v1+json\"\"\")
								}
							}
							response {
								status OK()
								body(\"\"\"{
						"fraudCheckStatus": "OK",
						"rejectionReason": ${value(consumer(null), producer(execute('assertThatRejectionReasonIsNull(\\$it)')))}
						}\"\"\")
								headers {
									header('Content-Type': 'application/vnd.fraud.v1+json')
									header 'Location': value(consumer(null), producer(execute('assertThatLocationIsNull(\\$it)')))
								}
							}
						}
						""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		if (framework == TestFramework.SPOCK) {
			assertThat(test).contains("assertThatRejectionReasonIsNull(parsedJson.read(\"\\$.rejectionReason\"))");
		}
		else {
			assertThat(test).contains("assertThatRejectionReasonIsNull(parsedJson.read(\"$.rejectionReason\"))");
		}
		assertThat(test).contains("assertThatLocationIsNull(response.header(\"Location\"))");
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_support_inner_map_and_list_definitions_with(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request {
						method "PUT"
						url "/v1/payments/e86df6f693de4b35ae648464c5b0dc09/client_data"
						headers { contentType(applicationJson()) }
						body(
							client: [
								first_name: $(consumer(regex(onlyAlphaUnicode())), producer('Denis')),
								last_name: $(consumer(regex(onlyAlphaUnicode())), producer('FakeName')),
								email: $(consumer(regex(email())), producer('fakemail@fakegmail.com')),
								fax: $(consumer(~/[+\\w]*/), producer('+xx001213214')),
								phone: $(consumer(~/[+\\w]*/), producer('2223311')),
								data_of_birth: $(consumer(~/.*/), producer('2002-10-22T00:00:00Z'))
							],
							client_id_card: [
								id: $(consumer(~/.*/), producer('ABC12345')),
								date_of_issue: $(consumer(~/.*/), producer('2002-10-02T00:00:00Z')),
								address: [
									street: $(consumer(~/.*/), producer('Light Street')),
									city: $(consumer(~/.*/), producer('Fire')),
									region: $(consumer(~/.*/), producer('Skys')),
									country: $(consumer(~/.*/), producer('HG')),
									zip: $(consumer(~/[\\d.]*/), producer('658965'))
								]
							],
							incomes_and_expenses: [
								monthly_income: $(consumer(~/[\\d.]*/), producer('0.0')),
								monthly_loan_repayments: $(consumer(~/[\\d.]*/), producer('100')),
								monthly_living_expenses: $(consumer(~/[\\d.]*/), producer('22'))
							],
							additional_info: [
								allow_to_contact: $(consumer(optional(regex(anyBoolean()))), producer('true'))
							]
						)
					}
					response {
						status OK()
						headers { contentType(applicationJson()) }
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		if (framework == TestFramework.SPOCK) {
			assertThat(test).contains("\"street\":\"Light Street\"");
		}
		else {
			assertThat(test).contains("\\\"street\\\":\\\"Light Street\\\"");
		}
		assertThat(test).doesNotContain("clientValue");
		assertThat(test).doesNotContain("cursor");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_work_with_optional_fields_that_have_null(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request {
						method "PUT"
						url "/v1/payments/e86df6f693de4b35ae648464c5b0dc09/client_data"
						headers { contentType(applicationJson()) }
					}
					response {
						status OK()
						headers { contentType(applicationJson()) }
						body(code: $(optional(regex('123123'))))
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void shouldnt_generate_unicode_escape_characters_with(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request {
						method "PUT"
						url '/v1/payments/e86df6f693de4b35ae648464c5b0dc09/енев'
						headers { contentType(applicationJson()) }
						body(client: [
							first_name: $(consumer(~/[\\p{L}]*/), producer('Пенева')),
							last_name: $(consumer(~/[\\p{L}]*/), producer('Пенева'))
						])
					}
					response {
						status OK()
						headers { contentType(applicationJson()) }
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).doesNotContain("\\u041f");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_proper_test_code_when_having_multiline_body_with(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'PUT'; url '/multiline'; body('''hello,
				World.''') }
					response { status OK() }
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		if (framework == TestFramework.SPOCK) {
			assertThat(test).contains("'''hello,\nWorld.'''");
		}
		else {
			assertThat(test).contains("\"hello,\\nWorld.\"");
		}
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_proper_test_code_when_having_multipart_parameters_with(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request {
						method 'PUT'
						url '/multipart'
						headers { contentType('multipart/form-data;boundary=AaB03x') }
						multipart(
							formParameter: $(c(regex('".+"')), p('"formParameterValue"')),
							someBooleanParameter: $(c(regex(anyBoolean())), p('true')),
							file: named(
								name: $(c(regex(nonEmpty())), p('filename.csv')),
								content: $(c(regex(nonEmpty())), p('file content')),
								contentType: $(c(regex(nonEmpty())), p('application/json')))
						)
					}
					response { status OK() }
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("\"Content-Type\", \"multipart/form-data;boundary=AaB03x\"");
		if (framework == TestFramework.SPOCK) {
			assertThat(test).contains(".param('formParameter', '''\"formParameterValue\"'''");
			assertThat(test).contains(".param('someBooleanParameter', 'true')");
			assertThat(test).contains(".multiPart('file', 'filename.csv', 'file content'.bytes, 'application/json')");
		}
		else {
			assertThat(test).contains(".param(\"formParameter\", \"\\\"formParameterValue\\\"\")");
			assertThat(test).contains(".param(\"someBooleanParameter\", \"true\")");
			if (framework == TestFramework.TESTNG) {
				assertThat(test).contains(
						".multiPart(\"file\", \"filename.csv\", \"file content\".getBytes(), \"application/json\");");
			}
			else {
				assertThat(test).contains(
						".multiPart(\"file\", \"filename.csv\", \"file content\".getBytes(), \"application/json\")");
			}
		}
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_proper_test_code_when_having_multipart_parameters_without_content_type_with(
			String methodBuilderName, TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request {
						method 'PUT'
						url '/multipart'
						headers { contentType('multipart/form-data;boundary=AaB03x') }
						multipart(
							formParameter: $(c(regex('".+"')), p('"formParameterValue"')),
							someBooleanParameter: $(c(regex(anyBoolean())), p('true')),
							file: named(
								name: $(c(regex(nonEmpty())), p('filename.csv')),
								content: $(c(regex(nonEmpty())), p('file content')))
						)
					}
					response { status OK() }
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("\"Content-Type\", \"multipart/form-data;boundary=AaB03x\"");
		if (framework == TestFramework.SPOCK) {
			assertThat(test).contains(".param('formParameter', '''\"formParameterValue\"'''");
			assertThat(test).contains(".param('someBooleanParameter', 'true')");
			assertThat(test).contains(".multiPart('file', 'filename.csv', 'file content'.bytes)");
		}
		else {
			assertThat(test).contains(".param(\"formParameter\", \"\\\"formParameterValue\\\"\")");
			assertThat(test).contains(".param(\"someBooleanParameter\", \"true\")");
			if (framework == TestFramework.TESTNG) {
				assertThat(test).contains(".multiPart(\"file\", \"filename.csv\", \"file content\".getBytes());");
			}
			else {
				assertThat(test).contains(".multiPart(\"file\", \"filename.csv\", \"file content\".getBytes())");
			}
		}
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_test_code_when_having_multipart_parameters_with_byte_array(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request {
						method "PUT"
						url "/multipart"
						headers { contentType('multipart/form-data;boundary=AaB03x') }
						multipart(
							file: named(
								name: value(stub(regex('.+')), test('file')),
								content: value(stub(regex('.+')), test([100, 117, 100, 97] as byte[]))
							)
						)
					}
					response { status 200 }
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("\"Content-Type\", \"multipart/form-data;boundary=AaB03x\"");
		if (framework == TestFramework.SPOCK) {
			assertThat(test).contains(".multiPart('file', 'file', [100, 117, 100, 97] as byte[])");
		}
		else {
			assertThat(test).contains(".multiPart(\"file\", \"file\", new byte[] {100, 117, 100, 97})");
		}
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_proper_test_code_when_having_multipart_parameters_that_use_execute_with(
			String methodBuilderName, TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request {
						method "PUT"
						url "/multipart"
						headers { contentType('multipart/form-data;boundary=AaB03x') }
						multipart(
							formParameter: $(c(regex('".+"')), p('"formParameterValue"')),
							someBooleanParameter: $(c(regex(anyBoolean())), p('true')),
							file: named(
								name: $(c(regex(nonEmpty())), p(execute('toString()'))),
								content: $(c(regex(nonEmpty())), p('file content')))
						)
					}
					response { status OK() }
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("\"Content-Type\", \"multipart/form-data;boundary=AaB03x\"");
		if (framework == TestFramework.SPOCK) {
			assertThat(test).contains(".param('formParameter', '''\"formParameterValue\"'''");
			assertThat(test).contains(".param('someBooleanParameter', 'true')");
			assertThat(test).contains(".multiPart('file', toString(), 'file content'.bytes)");
		}
		else {
			assertThat(test).contains(".param(\"formParameter\", \"\\\"formParameterValue\\\"\")");
			assertThat(test).contains(".param(\"someBooleanParameter\", \"true\")");
			if (framework == TestFramework.TESTNG) {
				assertThat(test).contains(".multiPart(\"file\", toString(), \"file content\".getBytes());");
			}
			else {
				assertThat(test).contains(".multiPart(\"file\", toString(), \"file content\".getBytes())");
			}
		}
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("sevenBuilders")
	void should_generate_proper_test_code_when_having_multipart_parameters_with_named_as_map_with(
			String methodBuilderName, TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request {
						method 'PUT'
						url "/multipart"
						headers { contentType('multipart/form-data;boundary=AaB03x') }
						multipart(
							formParameter: $(c(regex('".+"')), p('"formParameterValue"')),
							someBooleanParameter: $(c(regex(anyBoolean())), p('true')),
							file: named(
								name: $(c(regex(nonEmpty())), p('filename.csv')),
								content: $(c(regex(nonEmpty())), p('file content')))
						)
					}
					response { status OK() }
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(".multiPart");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@Test
	void should_parse_JSON_with_arrays_using_Spock() {
		Contract contractDsl = parseContract(
				"""
						Contract.make {
							request {
								method "GET"
								urlPath('/auth/oauth/check_token') {
									queryParameters {
										parameter 'token': value(
											consumer(regex('^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}')),
											producer('6973b31d-7140-402a-bca6-1cdb954e03a7')
										)
									}
								}
							}
							response {
								status OK()
								body(authorities: [value(consumer('ROLE_ADMIN'), producer(regex('^[a-zA-Z0-9_\\\\- ]+\\$')))])
							}
						}
						""");
		configProperties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(
				"assertThatJson(parsedJson).array(\"['authorities']\").elementWithIndex(0).matches(\"^[a-zA-Z0-9_\\\\- ]+\\$\")");
		SyntaxChecker.tryToCompileGroovy("spock", test);
	}

	@Test
	void should_parse_JSON_with_arrays_using_JUnit() {
		Contract contractDsl = parseContract(
				"""
						Contract.make {
							request {
								method "GET"
								urlPath('/auth/oauth/check_token') {
									queryParameters {
										parameter 'token': value(
											consumer(regex('^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}')),
											producer('6973b31d-7140-402a-bca6-1cdb954e03a7')
										)
									}
								}
							}
							response {
								status OK()
								body(authorities: [value(consumer('ROLE_ADMIN'), producer(regex('^[a-zA-Z0-9_\\\\- ]+\\$')))])
							}
						}
						""");
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(
				"assertThatJson(parsedJson).array(\"['authorities']\").elementWithIndex(0).matches(\"^[a-zA-Z0-9_\\\\- ]+$\")");
		SyntaxChecker.tryToCompileJava("mockmvc", test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_work_with_execution_property_with(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract(
				"""
						Contract.make {
							request { method 'PUT'; url '/fraudcheck' }
							response {
								status OK()
								body(fraudCheckStatus: "OK",
									rejectionReason: $(consumer(null), producer(execute('assertThatRejectionReasonIsNull(\\$it)'))))
							}
						}
						""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).doesNotContain(
				"assertThatJson(parsedJson).field(\"['rejectionReason']\").isEqualTo(\"assertThatRejectionReasonIsNull(\"");
		assertThat(test).contains("assertThatRejectionReasonIsNull(");
	}

	@Test
	void should_generate_proper_test_code_with_map_inside_list() {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'GET'; urlPath '/foos' }
					response {
						status OK()
						body([[id: value(consumer('123'), producer(regex('[0-9]+')))],
							  [id: value(consumer('567'), producer(regex('[0-9]+')))]
						])
						headers { contentType(applicationJsonUtf8()) }
					}
				}
				""");
		configProperties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDsl);
		assertThat(test)
			.contains("assertThatJson(parsedJson).array().elementWithIndex(0).field(\"['id']\").matches(\"[0-9]+\")");
		assertThat(test)
			.contains("assertThatJson(parsedJson).array().elementWithIndex(1).field(\"['id']\").matches(\"[0-9]+\")");
		SyntaxChecker.tryToCompileGroovy("mockmvc", test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_proper_test_code_with_top_level_array_using(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'GET'; urlPath '/api/tags' }
					response {
						status OK()
						body(["Java", "Java8", "Spring", "SpringBoot", "Stream"])
						headers { header('Content-Type': 'application/json;charset=UTF-8') }
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatJson(parsedJson).array().elementWithIndex(0).isEqualTo(\"Java\")");
		assertThat(test).contains("assertThatJson(parsedJson).array().elementWithIndex(1).isEqualTo(\"Java8\")");
		assertThat(test).contains("assertThatJson(parsedJson).array().elementWithIndex(2).isEqualTo(\"Spring\")");
		assertThat(test).contains("assertThatJson(parsedJson).array().elementWithIndex(3).isEqualTo(\"SpringBoot\")");
		assertThat(test).contains("assertThatJson(parsedJson).array().elementWithIndex(4).isEqualTo(\"Stream\")");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_proper_test_code_with_top_level_array_using_with_array_size_check(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		System.setProperty("spring.cloud.contract.verifier.assert.size", "true");
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'GET'; urlPath '/api/tags' }
					response {
						status OK()
						body(['Java', 'Java8', 'Spring', 'SpringBoot', 'Stream'])
						headers { header('Content-Type': 'application/json;charset=UTF-8') }
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatJson(parsedJson).hasSize(5)");
		assertThat(test).contains("assertThatJson(parsedJson).array().elementWithIndex(0).isEqualTo(\"Java\")");
		assertThat(test).contains("assertThatJson(parsedJson).array().elementWithIndex(1).isEqualTo(\"Java8\")");
		assertThat(test).contains("assertThatJson(parsedJson).array().elementWithIndex(2).isEqualTo(\"Spring\")");
		assertThat(test).contains("assertThatJson(parsedJson).array().elementWithIndex(3).isEqualTo(\"SpringBoot\")");
		assertThat(test).contains("assertThatJson(parsedJson).array().elementWithIndex(4).isEqualTo(\"Stream\")");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_proper_test_code_with_top_level_array_or_arrays_using(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'GET'; urlPath '/api/categories' }
					response {
						status OK()
						body([["Programming", "Java"], ["Programming", "Java", "Spring", "Boot"]])
						headers { header('Content-Type': 'application/json;charset=UTF-8') }
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(
				"assertThatJson(parsedJson).array().elementWithIndex(0).elementWithIndex(0).isEqualTo(\"Programming\")");
		assertThat(test)
			.contains("assertThatJson(parsedJson).array().elementWithIndex(0).elementWithIndex(1).isEqualTo(\"Java\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).array().elementWithIndex(1).elementWithIndex(0).isEqualTo(\"Programming\")");
		assertThat(test)
			.contains("assertThatJson(parsedJson).array().elementWithIndex(1).elementWithIndex(1).isEqualTo(\"Java\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).array().elementWithIndex(1).elementWithIndex(2).isEqualTo(\"Spring\")");
		assertThat(test)
			.contains("assertThatJson(parsedJson).array().elementWithIndex(1).elementWithIndex(3).isEqualTo(\"Boot\")");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_proper_test_code_with_top_level_map_of_sets_using(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					description('get map')
					name('assert map response')
					request { method 'GET'; urlPath '/some-path' }
					response {
						status OK()
						body(["key": ["value1", "value2"] as Set])
						headers { header('Content-Type': 'application/json;charset=UTF-8') }
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatJson(parsedJson).array(\"['key']\").hasSize(2)");
		assertThat(test)
			.contains("assertThatJson(parsedJson).array(\"['key']\").elementWithIndex(0).isEqualTo(\"value1\")");
		assertThat(test)
			.contains("assertThatJson(parsedJson).array(\"['key']\").elementWithIndex(1).isEqualTo(\"value2\")");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("threeBuilders")
	void should_generate_async_body_when_async_flag_set_in_response(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'GET'; url '/test' }
					response { status OK(); async() }
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(".when().async()");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("threeBuilders")
	void should_generate_async_body_after_queryParams_when_async_flag_set_in_response_and_queryParams_set_in_request(
			String methodBuilderName, TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request {
						method 'GET'
						url('/test') { queryParameters { parameter("param", "value") } }
					}
					response { status OK(); async() }
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		String strippedTest = test.replace("\n", "").replace(" ", "").replace("\t", "");
		assertThat(strippedTest).contains(".queryParam(\"param\",\"value\").when().async().get(\"/test\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_proper_test_code_with_array_of_primitives_using(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'GET'; urlPath '/api/tags' }
					response {
						status OK()
						body('{"partners":[{"payment_methods":["BANK","CASH"]}]}')
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(
				"assertThatJson(parsedJson).array(\"['partners']\").elementWithIndex(0).array(\"['payment_methods']\").elementWithIndex(0).isEqualTo(\"BANK\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).array(\"['partners']\").elementWithIndex(0).array(\"['payment_methods']\").elementWithIndex(1).isEqualTo(\"CASH\")");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@Test
	void should_not_escape_dollar_in_Spock_regex_tests() {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'GET'; urlPath '/get' }
					response {
						status OK()
						body(code: 9, message: $(consumer('Wrong credentials'), producer(regex('^(?!\\\\s*\\$).+'))))
					}
				}
				""");
		configProperties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['message']\").matches(\"^(?!\\\\s*\\$).+\")");
		SyntaxChecker.tryToCompileGroovy("mockmvc", test, false);
	}

	@Test
	void should_generate_the_regular_expression_for_the_other_side_of_communication() {
		configProperties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(DSL_WITH_ONLY_ONE_SIDE_FOR_DOCS);
		String strippedTest = test.replace("\n", "").stripIndent();
		assertThat(Pattern.matches(".*header\\(\"header\", \"application\\/vnd\\.fraud\\.v1\\+json;.*\"\\).*",
				strippedTest))
			.isTrue();
		assertThat(Pattern.matches(".*body\\('''\\{\"requestElement\":\"[0-9]{5}\"\\}'''\\).*", strippedTest)).isTrue();
		assertThat(Pattern.matches(".*put\\(\"/foo/[0-9]{5}\"\\).*", strippedTest)).isTrue();
		assertThat(strippedTest).contains(
				"response.header(\"Content-Type\") ==~ java.util.regex.Pattern.compile('application/vnd\\\\.fraud\\\\.v1\\\\+json.*')");
		assertThat("application/vnd.fraud.v1+json;charset=UTF-8").matches("application/vnd\\.fraud\\.v1\\+json.*");
		assertThat(strippedTest)
			.contains("assertThatJson(parsedJson).field(\"['responseElement']\").matches(\"[0-9]{7}\")");
		SyntaxChecker.tryToCompileGroovy("spock", test);
	}

	@Test
	void should_execute_custom_method_for_complex_structures_on_the_response_side() {
		Contract contractDsl = parseContract(
				"""
						Contract.make {
							request { method 'GET'; urlPath '/get' }
							response {
								status OK()
								body([fraudCheckStatus: "OK",
									rejectionReason: [title: $(consumer(null), producer(execute('assertThatRejectionReasonIsNull(\\$it)')))]])
							}
						}
						""");
		configProperties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatRejectionReasonIsNull(parsedJson.read(\"\\$.rejectionReason.title\"))");
		SyntaxChecker.tryToCompileGroovy("spock", test);
	}

	@Test
	void should_execute_custom_method_for_more_complex_structures_on_the_response_side_when_using_Spock() {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'GET'; urlPath '/get' }
					response {
						status OK()
						body([
							[name: $(consumer("userName 1"), producer(execute('assertThatUserNameIsNotNull(\\$it)')))],
							[name: $(consumer("userName 2"), producer(execute('assertThatUserNameIsNotNull(\\$it)')))]
						])
					}
				}
				""");
		configProperties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatUserNameIsNotNull(parsedJson.read(\"\\$.[0].name\")");
		assertThat(test).contains("assertThatUserNameIsNotNull(parsedJson.read(\"\\$.[1].name\")");
		SyntaxChecker.tryToCompileGroovy("spock", test);
	}

	@Test
	void should_execute_custom_method_for_more_complex_structures_on_the_response_side_when_using_JUnit() {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'GET'; urlPath '/get' }
					response {
						status OK()
						body([
							[name: $(consumer("userName 1"), producer(execute('assertThatUserNameIsNotNull(\\$it)')))],
							[name: $(consumer("userName 2"), producer(execute('assertThatUserNameIsNotNull(\\$it)')))]
						])
					}
				}
				""");
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatUserNameIsNotNull(parsedJson.read(\"$.[0].name\")");
		assertThat(test).contains("assertThatUserNameIsNotNull(parsedJson.read(\"$.[1].name\")");
	}

	@Test
	void should_execute_custom_method_for_request_headers() {
		Contract contractDsl = parseContract(
				"""
						Contract.make {
							request {
								method 'GET'
								urlPath '/get'
								headers { header('authorization', value(consumer('Bearer token'), producer(execute('getOAuthTokenHeader()')))) }
							}
							response {
								status OK()
								body([fraudCheckStatus: "OK",
									rejectionReason: [title: $(consumer(null), producer(execute('assertThatRejectionReasonIsNull(\\$it)')))]])
							}
						}
						""");
		configProperties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(".header(\"authorization\", getOAuthTokenHeader())");
		SyntaxChecker.tryToCompileGroovy("spock", test);
	}

	@Test
	void should_support_body_matching_in_response() {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'GET'; url '/get' }
					response { status OK(); body(value(stub("HELLO FROM STUB"), server(regex(".*")))) }
				}
				""");
		configProperties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("responseBody ==~ java.util.regex.Pattern.compile('.*')");
		SyntaxChecker.tryToCompileGroovy("spock", test);
	}

	@Test
	void should_support_custom_method_execution_in_response() {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'GET'; url '/get' }
					response { status OK(); body(value(stub("HELLO FROM STUB"), server(execute('foo(\\$it)')))) }
				}
				""");
		configProperties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("foo(responseBody)");
		SyntaxChecker.tryToCompileGroovy("spock", test);
	}

	@Test
	void should_allow_c_p_version_of_consumer_producer() {
		Contract contractDsl = parseContract("""
				Contract.make {
					request {
						method 'GET'
						urlPath '/get'
						headers { header('authorization', $(c('Bearer token'), p(execute('getOAuthTokenHeader()')))) }
					}
					response {
						status OK()
						body([fraudCheckStatus: "OK",
							rejectionReason: [title: $(c(null), p(execute('assertThatRejectionReasonIsNull(\\$it)')))]])
					}
				}
				""");
		configProperties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(".header(\"authorization\", getOAuthTokenHeader())");
		SyntaxChecker.tryToCompileGroovy("spock", test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_allow_easier_way_of_providing_dynamic_values_for(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request {
						method 'GET'
						urlPath '/get'
						body([
							duck: $(regex("[0-9]")), alpha: $(anyAlphaUnicode()), number: $(anyNumber()),
							anInteger: $(anyInteger()), positiveInt: $(positiveInt()), aDouble: $(anyDouble()),
							aBoolean: $(aBoolean()), ip: $(anyIpAddress()), hostname: $(anyHostname()),
							email: $(anyEmail()), url: $(anyUrl()), httpsUrl: $(anyHttpsUrl()),
							uuid: $(anyUuid()), date: $(anyDate()), dateTime: $(anyDateTime()),
							time: $(anyTime()), iso8601WithOffset: $(anyIso8601WithOffset()),
							nonBlankString: $(anyNonBlankString()), nonEmptyString: $(anyNonEmptyString()),
							anyOf: $(anyOf('foo', 'bar'))
						])
						headers { contentType(applicationJson()) }
					}
					response {
						status OK()
						body([
							alpha: $(anyAlphaUnicode()), number: $(anyNumber()), anInteger: $(anyInteger()),
							positiveInt: $(positiveInt()), aDouble: $(anyDouble()), aBoolean: $(aBoolean()),
							ip: $(anyIpAddress()), hostname: $(anyHostname()), email: $(anyEmail()),
							url: $(anyUrl()), httpsUrl: $(anyHttpsUrl()), uuid: $(anyUuid()),
							date: $(anyDate()), dateTime: $(anyDateTime()), time: $(anyTime()),
							iso8601WithOffset: $(anyIso8601WithOffset()), nonBlankString: $(anyNonBlankString()),
							nonEmptyString: $(anyNonEmptyString()), anyOf: $(anyOf('foo', 'bar'))
						])
						headers { contentType(applicationJson()) }
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['aBoolean']\").matches(\"(true|false)\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['alpha']\").matches(\"[\\\\p{L}]*\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).field(\"['hostname']\").matches(\"((http[s]?|ftp):/)/?([^:/\\\\s]+)(:[0-9]{1,5})?\")");
		assertThat(test)
			.contains("assertThatJson(parsedJson).field(\"['number']\").matches(\"-?(\\\\d*\\\\.\\\\d+|\\\\d+)\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['anInteger']\").matches(\"-?(\\\\d+)\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['positiveInt']\").matches(\"([1-9]\\\\d*)\")");
		assertThat(test)
			.contains("assertThatJson(parsedJson).field(\"['aDouble']\").matches(\"-?(\\\\d*\\\\.\\\\d+)\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).field(\"['email']\").matches(\"[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,6}\")");
		assertThat(test).doesNotContain("cursor");
		assertThat(test).doesNotContain("REGEXP>>");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
		String endOfLine = (framework == TestFramework.SPOCK) ? "\\$" : "$";
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['anyOf']\").matches(\"^foo" + endOfLine + "|^bar"
				+ endOfLine + "\")");
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_generate_random_boolean_input_in_generated_test(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request {
						method 'GET'
						urlPath '/get'
						body(["someBooleanInputParam": aBoolean()])
						headers { contentType applicationJson() }
					}
					response {
						status OK()
						body(["someBooleanOutputParam": $(aBoolean())])
						headers { contentType(applicationJson()) }
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		Pattern p = Pattern
			.compile("\\.body\\(('{3}|\")\\{(\"|\\\\\")someBooleanInputParam(\"|\\\\\"):(true|false)\\}('{3}|\")\\)");
		Matcher m = p.matcher(test);
		int count = 0;
		while (m.find()) {
			count++;
		}
		assertThat(count).isEqualTo(1);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_escape_regex_properly_for_content_type(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request {
						method GET()
						url 'get'
						headers { contentType("application/vnd.fraud.v1+json") }
					}
					response {
						status OK()
						headers { contentType("application/vnd.fraud.v1+json") }
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("application/vnd\\\\.fraud\\\\.v1\\\\+json.*");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_resolve_Optional_object_when_used_in_query_parameters(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request {
						method 'GET'
						urlPath('/blacklist') {
							queryParameters {
								parameter 'isActive': value(consumer(optional(regex('(true|false)'))))
								parameter 'limit': value(consumer(optional(regex('([0-9]{1,10})'))))
								parameter 'offset': value(consumer(optional(regex('([0-9]{1,10})'))))
							}
						}
						headers { header 'Content-Type': 'application/json' }
					}
					response { status(200) }
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).doesNotContain("sh.stubborn.contract.spec.internal.OptionalProperty");
		assertThat(test).contains("(([0-9]{1,10}))?");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_resolve_plain_text_properly_via_headers(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'GET'; url("/foo") }
					response {
						status(200)
						body '{"a":1}\\n{"a":2}'
						headers { contentType(textPlain()) }
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).doesNotContain("assertThatJson(parsedJson).field(\"['a']\").isEqualTo(1)");
		if (framework == TestFramework.SPOCK) {
			assertThat(test).contains("{\"a\":1}\\n{\"a\":2}'''");
		}
		else {
			assertThat(test).contains("assertThat(responseBody).isEqualTo(\"{\\\"a\\\":1}\\n{\\\"a\\\":2}");
		}
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("sixBuilders")
	void should_resolve_plain_text_that_happens_to_be_a_valid_json_for(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'GET'; url '/foo' }
					response {
						status OK()
						headers { contentType(applicationJsonUtf8()) }
						body(value(client('true'), server(regex("true|false"))))
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		if (framework == TestFramework.SPOCK && testMode != TestMode.JAXRSCLIENT) {
			assertThat(test).contains("responseBody ==~ java.util.regex.Pattern.compile('true|false')");
		}
		else if (framework == TestFramework.SPOCK && testMode == TestMode.JAXRSCLIENT) {
			assertThat(test).contains("responseBody ==~ java.util.regex.Pattern.compile(\"true|false\")");
		}
		else {
			assertThat(test).contains("assertThat(responseBody).matches(\"true|false\");");
		}
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("springBuilders")
	void should_escape_quotes_properly_using(String methodBuilderName, TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request {
						method 'POST'
						url '/foo'
						body(xyz: 'abc')
						headers { header('Content-Type', 'application/json;charset=UTF-8') }
					}
					response {
						status OK()
						body(bar: $(producer(regex('some value \\"with quote\\"|bar'))))
						headers { header('Content-Type': 'application/json;charset=UTF-8') }
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test)
			.contains("assertThatJson(parsedJson).field(\"['bar']\").matches(\"some value \\\"with quote\\\"|bar\")");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("sixBuilders")
	void should_make_the_execute_method_work_in_a_url_for(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request {
						method 'POST'
						url $(c("foo"), p(execute("toString()")))
					}
					response { status OK() }
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
		assertThat(test).contains("toString()");
		assertThat(test).doesNotContain("\"toString()\"");
		assertThat(test).doesNotContain("'toString()'");
	}

	@ParameterizedTest
	@MethodSource("sixBuilders")
	void should_create_an_assertion_for_an_empty_list_for(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'GET'; url '/api/v1/xxxx' }
					response {
						status OK()
						body([status: '200', list: [], foo: ["bar", "baz"]])
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
		assertThat(test).contains("assertThatJson(parsedJson).array(\"['list']\").isEmpty()");
		assertThat(test).doesNotContain("assertThatJson(parsedJson).array(\"['foo']\").isEmpty()");
	}

	@ParameterizedTest
	@MethodSource("sixBuilders")
	void should_work_properly_when_body_is_an_integer(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method 'GET'; url '/api/v1/xxxx'; body(12000) }
					response { status OK(); body(12000) }
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
		if (framework == TestFramework.SPOCK && testMode == null) {
			assertThat(test).contains("body('''12000''')");
			assertThat(test).contains("responseBody == '12000'");
		}
		else if (framework == TestFramework.SPOCK && testMode == TestMode.JAXRSCLIENT) {
			assertThat(test).contains(".build(\"GET\", entity(\"12000\", \"text/plain\"))");
			assertThat(test).contains("responseBody == \"12000\"");
		}
		else if (testMode == TestMode.JAXRSCLIENT) {
			assertThat(test).contains(".build(\"GET\", entity(\"12000\", \"text/plain\"))");
			assertThat(test).contains("assertThat(responseBody).isEqualTo(\"12000\")");
		}
		else {
			assertThat(test).contains("body(\"12000\")");
			assertThat(test).contains("assertThat(responseBody).isEqualTo(\"12000\");");
		}
	}

	@ParameterizedTest
	@MethodSource("sixBuilders")
	void should_manage_to_reference_request_in_response(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request {
						method 'GET'
						url('/api/v1/xxxx') {
							queryParameters {
								parameter('foo', 'bar')
								parameter('foo', 'bar2')
							}
						}
						headers { header(authorization(), 'secret'); header(authorization(), 'secret2') }
						body(foo: 'bar', baz: 5)
					}
					response {
						status OK()
						headers { header(authorization(), "foo ${fromRequest().header(authorization())} bar") }
						body(
							url: fromRequest().url(), path: fromRequest().path(), pathIndex: fromRequest().path(1),
							param: fromRequest().query('foo'), paramIndex: fromRequest().query('foo', 1),
							authorization: fromRequest().header('Authorization'),
							authorization2: fromRequest().header('Authorization', 1),
							fullBody: fromRequest().body(),
							responseFoo: fromRequest().body('\\$.foo'),
							responseBaz: fromRequest().body('\\$.baz'),
							responseBaz2: "Bla bla ${fromRequest().body('\\$.foo')} bla bla",
							rawUrl: fromRequest().rawUrl(), rawPath: fromRequest().rawPath(),
							rawPathIndex: fromRequest().rawPath(1), rawParam: fromRequest().rawQuery('foo'),
							rawParamIndex: fromRequest().rawQuery('foo', 1),
							rawAuthorization: fromRequest().rawHeader('Authorization'),
							rawAuthorization2: fromRequest().rawHeader('Authorization', 1),
							rawResponseFoo: fromRequest().rawBody('\\$.foo'),
							rawResponseBaz: fromRequest().rawBody('\\$.baz'),
							rawResponseBaz2: "Bla bla ${fromRequest().rawBody('\\$.foo')} bla bla"
						)
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test).doesNotContain("DslProperty");
		assertThat(test).doesNotContain("ERROR: ");
		assertThat(test)
			.contains("assertThatJson(parsedJson).field(\"['url']\").isEqualTo(\"/api/v1/xxxx?foo=bar&foo=bar2\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['path']\").isEqualTo(\"/api/v1/xxxx\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['pathIndex']\").isEqualTo(\"v1\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).field(\"['fullBody']\").isEqualTo(\"{\\\"foo\\\":\\\"bar\\\",\\\"baz\\\":5}\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['paramIndex']\").isEqualTo(\"bar2\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['responseFoo']\").isEqualTo(\"bar\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['authorization']\").isEqualTo(\"secret\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['authorization2']\").isEqualTo(\"secret2\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['responseBaz']\").isEqualTo(5)");
		assertThat(test)
			.contains("assertThatJson(parsedJson).field(\"['responseBaz2']\").isEqualTo(\"Bla bla bar bla bla\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['param']\").isEqualTo(\"bar\")");
		if (framework == TestFramework.SPOCK && testMode == null) {
			assertThat(test).contains("response.header(\"Authorization\") == 'foo secret bar'");
		}
		else if (framework == TestFramework.SPOCK && testMode == TestMode.JAXRSCLIENT) {
			assertThat(test).contains("response.getHeaderString(\"Authorization\") == \"foo secret bar\"");
		}
		else if (testMode == TestMode.JAXRSCLIENT) {
			assertThat(test)
				.contains("assertThat(response.getHeaderString(\"Authorization\")).isEqualTo(\"foo secret bar\");");
		}
		else {
			assertThat(test).contains("assertThat(response.header(\"Authorization\")).isEqualTo(\"foo secret bar\");");
		}
	}

	@ParameterizedTest
	@MethodSource("sixBuilders")
	void should_manage_to_reference_request_in_response_via_WireMock_native_entries(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request {
						method 'GET'
						url('/api/v1/xxxx') {
							queryParameters { parameter('foo', 'bar'); parameter('foo', 'bar2') }
						}
						headers { header(authorization(), 'secret'); header(authorization(), 'secret2') }
						body(foo: "bar", baz: 5)
					}
					response {
						status OK()
						headers { contentType(applicationJson()) }
						body('''
							{
								"responseFoo": "{{{ jsonPath request.body '\\$.foo' }}}",
								"responseBaz": {{{ jsonPath request.body '\\$.baz' }}},
								"responseBaz2": "Bla bla {{{ jsonPath request.body '\\$.foo' }}} bla bla"
							}
						'''.toString())
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		assertThat(test).doesNotContain("DslProperty");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['responseFoo']\").isEqualTo(\"bar\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['responseBaz']\").isEqualTo(5)");
		assertThat(test)
			.contains("assertThatJson(parsedJson).field(\"['responseBaz2']\").isEqualTo(\"Bla bla bar bla bla\")");
	}

	@ParameterizedTest
	@MethodSource("twoBuilders")
	void should_generate_JUnit_assertions_with_cookies(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(CONTRACT_DSL_WITH_COOKIES_VALUE);
		assertThat(test).contains(".cookie(\"cookie-key\", \"cookie-value\")");
		assertThat(test).contains("assertThat(response.cookie(\"cookie-key\")).isNotNull();");
		assertThat(test).contains("assertThat(response.cookie(\"cookie-key\")).isEqualTo(\"new-cookie-value\");");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("twoBuilders")
	void should_generate_JUnit_assertions_with_cookies_pattern(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(CONTRACT_DSL_WITH_COOKIES_PATTERN);
		assertThat(test).doesNotContain(".cookie(\"cookie-key\", \"[A-Za-z]+\")");
		assertThat(test).contains(".cookie(\"cookie-key\", \"");
		assertThat(test).contains("assertThat(response.cookie(\"cookie-key\")).isNotNull();");
		assertThat(test).contains("assertThat(response.cookie(\"cookie-key\")).matches(\"[A-Za-z]+\");");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest
	@MethodSource("twoBuilders")
	void should_not_generate_JUnit_cookie_assertion_with_absent_cookie(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(CONTRACT_DSL_WITH_ABSENT_COOKIES);
		assertThat(test).doesNotContain("cookie");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@Test
	void should_generate_spock_assertions_with_cookies() {
		configProperties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(CONTRACT_DSL_WITH_COOKIES_VALUE);
		assertThat(test).contains(".cookie(\"cookie-key\", \"cookie-value\")");
		assertThat(test).contains("response.cookie(\"cookie-key\") != null");
		assertThat(test).contains("response.cookie(\"cookie-key\") == 'new-cookie-value'");
		SyntaxChecker.tryToCompile("spock", test);
	}

	@Test
	void should_generate_spock_assertions_with_cookies_pattern() {
		configProperties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(CONTRACT_DSL_WITH_COOKIES_PATTERN);
		assertThat(test).doesNotContain(".cookie(\"cookie-key\", \"[A-Za-z]+\")");
		assertThat(test).contains(".cookie(\"cookie-key\", \"");
		assertThat(test).contains("response.cookie(\"cookie-key\") != null");
		assertThat(test).contains("response.cookie(\"cookie-key\") ==~ java.util.regex.Pattern.compile('[A-Za-z]+')");
		SyntaxChecker.tryToCompile("spock", test);
	}

	@Test
	void should_not_generate_spock_cookie_assertion_with_absent_cookie() {
		configProperties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(CONTRACT_DSL_WITH_ABSENT_COOKIES);
		assertThat(test).doesNotContain("cookie");
		SyntaxChecker.tryToCompile("spock", test);
	}

	@ParameterizedTest
	@MethodSource("sixBuilders")
	void should_create_an_assertion_for_an_empty_map_or_Object_for(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					name("method")
					request { method 'GET'; url '/api/v1/xxxx' }
					response {
						status 200
						body([aMap: ["foo": "bar"], anEmptyMap: [:]])
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
		assertThat(test)
			.contains("assertThatJson(parsedJson).field(\"['aMap']\").field(\"['foo']\").isEqualTo(\"bar\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['anEmptyMap']\").isEmpty()");
	}

	@ParameterizedTest
	@MethodSource("sixBuilders")
	void should_have_a_big_decimal_import_for(String methodBuilderName, TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request {
						method 'POST'
						url '/crystals/create'
						headers { header 'Content-Type': 'application/json' }
						body(amount: 200)
						bodyMatchers { jsonPath('\\$.amount', byRegex('^[0-9]{1,3}\\$')) }
					}
					response {
						status 201
						body(amount: fromRequest().body('\\$.amount'), price: 10100.0)
						bodyMatchers {
							jsonPath('\\$.amount', byRegex('^\\\\d*\\$'))
							jsonPath('\\$.price', byEquality())
						}
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
		assertThat(test).contains("$.price\", java.math.BigDecimal.class)).isEqualTo(\"10100.0\")");
	}

	@ParameterizedTest
	@MethodSource("sixBuilders")
	void should_resolve_from_request_evaluation_even_if_there_is_no_response_body(String methodBuilderName,
			TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request {
						method PUT()
						url '/frauds/name'
						body([name: $(anyAlphaUnicode())])
						headers { contentType("application/json") }
					}
					response {
						status OK()
						headers { header(contentType(), "${fromRequest().header(contentType())}") }
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
		assertThat(test).doesNotContain("{{{request.headers.Content-Type.[0]}}}");
	}

	@ParameterizedTest
	@MethodSource("notInProgressBuilders")
	void should_generate_test_methods_for_not_in_progress_contracts(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		List<Contract> contractDsl = List.of(parseContract("""
				Contract.make {
					name("httpContractNotInProgress")
					request { method PUT(); url '/httpContractNotInProgress' }
					response { status OK() }
				}
				"""), parseContract("""
				Contract.make {
					name("httpContractInProgress")
					inProgress()
					request { method PUT(); url '/httpContractInProgress' }
					response { status OK() }
				}
				"""),
				parseContract(
						"""
								Contract.make {
									name("messagingContractNotInProgress")
									input { triggeredBy("toString()") }
									outputMessage { sentTo("messagingContractNotInProgress"); body([type: "messagingContractNotInProgress"]) }
								}
								"""),
				parseContract(
						"""
								Contract.make {
									name("messagingContractInProgress")
									inProgress()
									input { triggeredBy("toString()") }
									outputMessage { sentTo("messagingContractNotInProgress"); body([type: "messagingContractNotInProgress"]) }
								}
								"""));
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
		assertThat(test).contains("httpContractNotInProgress");
		assertThat(test).contains("messagingContractNotInProgress");
		assertThat(test).contains("httpContractInProgress");
		assertThat(test).contains("messagingContractInProgress");
		assertThat(test.split("@Ignore", -1).length - 1 == 2 || test.split("@Disabled", -1).length - 1 == 2
				|| test.split(Pattern.quote("@Test(enabled = false)"), -1).length - 1 == 2)
			.isTrue();
	}

	@ParameterizedTest
	@MethodSource("largeNumbersBuilders")
	void should_work_with_large_numbers(String methodBuilderName, TestFramework framework, TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					label 'storage_object_created'
					input { triggeredBy('toString()') }
					outputMessage {
						sentTo('document_uploads')
						headers { header('objectGeneration', 23094823904823) }
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
		assertThat(test).contains("23094823904823L");
	}

	@ParameterizedTest
	@MethodSource("sixBuilders")
	void should_call_execute_in_headers_instead_of_quoting_it(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract(
				"""
						Contract.make {
							request {
								method PUT()
								url '/frauds/name'
								headers { header(authorization(), value(client(anyNonBlankString()), server(execute("toString()")))) }
							}
							response {
								status OK()
								headers { header(contentType(), "${fromRequest().header(contentType())}") }
							}
						}
						""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
		assertThat(test).doesNotContain("\"toString()\"");
		assertThat(test).doesNotContain("'toString()'");
	}

	@ParameterizedTest
	@MethodSource("sixBuilders")
	void should_allow_using_execute_in_the_request_body(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					description "should migrate spaceship"
					request {
						method POST()
						url('/api/migration')
						headers { accept('application/json'); contentType(applicationJson()) }
						body($(c([id: 4, foo: 5, whatever: "hello"]), p(execute('hashCode()'))))
					}
					response {
						status OK()
						headers { contentType(applicationJson()) }
						body($(c([id: 4, foo: 5, whatever: "hello"]), p(execute('hashCode()'))))
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
		assertThat(test.split("hashCode\\(\\)", -1).length - 1).isEqualTo(2);
	}

	@ParameterizedTest
	@MethodSource("sixBuilders")
	void should_not_double_escape_header_values_containing_quotes_for(String methodBuilderName, TestFramework framework,
			TestMode testMode) {
		Contract contractDsl = parseContract("""
				Contract.make {
					request { method GET(); url '/download' }
					response {
						status OK()
						headers { header('Content-Disposition', 'attachment; filename="test.pdf"') }
						body("file content")
					}
				}
				""");
		applyBuilder(framework, testMode);
		String test = singleTestGenerator(contractDsl);
		if (framework == TestFramework.SPOCK && testMode == null) {
			assertThat(test).contains("'''attachment; filename=\"test.pdf\"'''");
		}
		else if (framework == TestFramework.SPOCK && testMode == TestMode.JAXRSCLIENT) {
			assertThat(test).contains("filename=\\\"test.pdf\\\"");
		}
		else {
			assertThat(test).contains("isEqualTo(\"attachment; filename=\\\"test.pdf\\\"\")");
		}
		assertThat(test).doesNotContain("filename=\\\\\\\"test.pdf\\\\\\\"");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

}
