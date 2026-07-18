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
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.ClientDslProperty;
import sh.stubborn.contract.spec.internal.DslProperty;
import sh.stubborn.contract.spec.internal.ExecutionProperty;
import sh.stubborn.contract.spec.internal.ServerDslProperty;
import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.config.TestMode;
import sh.stubborn.contract.verifier.converter.YamlContractConverter;
import sh.stubborn.contract.verifier.dsl.wiremock.WireMockStubVerifier;
import sh.stubborn.contract.verifier.file.ContractMetadata;
import sh.stubborn.contract.verifier.util.SyntaxChecker;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jakub Kubrynski, codearte.io
 * @author Tim Ysewyn
 */
class YamlMockMvcMethodBodyBuilderTests implements WireMockStubVerifier {

	ContractVerifierConfigProperties properties;

	final GeneratedClassDataForMethod generatedClassDataForMethod = new GeneratedClassDataForMethod(
			new SingleTestGenerator.GeneratedClassData("foo", "bar", new File(".").toPath()), "method");

	final SingleTestGenerator.GeneratedClassData generatedClassData = new SingleTestGenerator.GeneratedClassData("foo",
			"com.example", new File(".").toPath());

	@BeforeEach
	void setup() {
		properties = new ContractVerifierConfigProperties();
		properties.setAssertJsonSize(true);
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
		}.buildClass(properties, List.of(contractMetadata(contractDsl)), "foo", generatedClassData);
	}

	private ContractMetadata contractMetadata(Contract contractDsl) {
		return new ContractMetadata(new File(".").toPath(), false, 0, null, contractDsl);
	}

	static File textToFile(String text) {
		try {
			File temp = Files.createTempFile("yaml", ".yml").toFile();
			Files.writeString(temp.toPath(), text);
			return temp;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static Contract fromYaml(String text) {
		return new YamlContractConverter().convertFrom(textToFile(text)).iterator().next();
	}

	// -------------------------------------------------------------------------
	// Parameter sources
	// -------------------------------------------------------------------------

	static Stream<Arguments> spockMockmvcCustom() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC), Arguments.of("custom", null, TestMode.CUSTOM));
	}

	static Stream<Arguments> spockMockmvcWebclientCustom() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT), Arguments.of("custom", null, TestMode.CUSTOM));
	}

	static Stream<Arguments> spockTestngMockmvcWebclient() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("testng", TestFramework.TESTNG, null), Arguments.of("mockmvc", null, TestMode.MOCKMVC),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT));
	}

	static Stream<Arguments> spockMockmvcWebclient() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT));
	}

	static Stream<Arguments> spockMockmvcWebclientCustomWithBodyString() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null, ".body('''''')"),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC, ".body(\"\")"),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT, ".body(\"\")"),
				Arguments.of("custom", null, TestMode.CUSTOM, ".body(\"\")"));
	}

	static Stream<Arguments> spockMockmvcWebclientAsync() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC));
	}

	private void applyMethodBuilder(TestFramework framework, TestMode mode) {
		if (framework != null) {
			properties.setTestFramework(framework);
		}
		if (mode != null) {
			properties.setTestMode(mode);
		}
	}

	// -------------------------------------------------------------------------
	// Tests
	// -------------------------------------------------------------------------

	@ParameterizedTest(name = "should generate assertions for simple response body with [{0}]")
	@MethodSource("spockMockmvcCustom")
	void shouldGenerateAssertionsForSimpleResponseBody(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "test"
				response:
				  status: 200
				  body:
				    "property1": "a"
				    "property2": "b"
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")")
			.contains("assertThatJson(parsedJson).field(\"['property2']\").isEqualTo(\"b\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should generate assertions for null and boolean values with [{0}]")
	@MethodSource("spockMockmvcCustom")
	void shouldGenerateAssertionsForNullAndBooleanValues(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "test"
				response:
				  status: 200
				  body:
				    "property1": "true"
				    "property2": null
				    "property3": false
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"true\")")
			.contains("assertThatJson(parsedJson).field(\"['property2']\").isNull()")
			.contains("assertThatJson(parsedJson).field(\"['property3']\").isEqualTo(false)");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(
			name = "should generate assertions for simple response body constructed from map with a list with [{0}]")
	@MethodSource("spockMockmvcWebclientCustom")
	void shouldGenerateAssertionsForSimpleResponseBodyWithList(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "test"
				response:
				  status: 200
				  body:
				    "property1": "a"
				    "property2":
				      - "a" : "sth"
				      - "b" : "sthElse"
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")")
			.contains(
					"assertThatJson(parsedJson).array(\"['property2']\").elementWithIndex(0).field(\"['a']\").isEqualTo(\"sth\")")
			.contains(
					"assertThatJson(parsedJson).array(\"['property2']\").elementWithIndex(1).field(\"['b']\").isEqualTo(\"sthElse\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(
			name = "should generate assertions for simple response body constructed from map with a list with [{0}] with array size check")
	@MethodSource("spockMockmvcWebclientCustom")
	void shouldGenerateAssertionsForSimpleResponseBodyWithListAndArraySizeCheck(String methodBuilderName,
			TestFramework framework, TestMode mode) {
		String savedProp = System.getProperty("spring.cloud.contract.verifier.assert.size");
		System.setProperty("spring.cloud.contract.verifier.assert.size", "true");
		try {
			String contract = """
					---
					request:
					  method: "GET"
					  url: "test"
					response:
					  status: 200
					  body:
					    "property1": "a"
					    "property2":
					      - "a" : "sth"
					      - "b" : "sthElse"
					""";
			Contract contractDsl = fromYaml(contract);
			applyMethodBuilder(framework, mode);
			String test = singleTestGenerator(contractDsl);
			assertThat(test).contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")")
				.contains(
						"assertThatJson(parsedJson).array(\"['property2']\").elementWithIndex(0).field(\"['a']\").isEqualTo(\"sth\")")
				.contains(
						"assertThatJson(parsedJson).array(\"['property2']\").elementWithIndex(1).field(\"['b']\").isEqualTo(\"sthElse\")");
			stubMappingIsValidWireMockStub(contractDsl);
			SyntaxChecker.tryToCompile(methodBuilderName, test);
		}
		finally {
			if (savedProp != null) {
				System.setProperty("spring.cloud.contract.verifier.assert.size", savedProp);
			}
			else {
				System.clearProperty("spring.cloud.contract.verifier.assert.size");
			}
		}
	}

	@ParameterizedTest(name = "should generate proper request when body constructed from map with a list [{0}]")
	@MethodSource("spockMockmvcWebclientCustom")
	void shouldGenerateProperRequestWhenBodyConstructedFromMapWithList(String methodBuilderName,
			TestFramework framework, TestMode mode) {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "test"
				  body:
				    items:\s
				      - "HOP"
				response:
				  status: 200
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		String bodyString;
		if ("spock".equals(methodBuilderName)) {
			bodyString = ".body('''{\"items\":[\"HOP\"]}''')";
		}
		else {
			bodyString = ".body(\"{\\\"items\\\":[\\\"HOP\\\"]}\")";
		}
		assertThat(test).contains(bodyString);
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should generate proper request when body constructed from GString with [{0}]")
	@MethodSource("spockMockmvcWebclientCustom")
	void shouldGenerateProperRequestWhenBodyConstructedFromGString(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "test"
				  body: "property1=VAL1"
				response:
				  status: 200
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		String bodyString;
		if ("spock".equals(methodBuilderName)) {
			bodyString = ".body('''property1=VAL1''')";
		}
		else {
			bodyString = ".body(\"property1=VAL1\")";
		}
		assertThat(test).contains(bodyString);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
		stubMappingIsValidWireMockStub(contractDsl);
	}

	@ParameterizedTest(
			name = "should generate assertions for a response body containing map with integers as keys with [{0}]")
	@MethodSource("spockMockmvcWebclientCustom")
	void shouldGenerateAssertionsForResponseBodyWithIntegerKeys(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		// YAML CAN'T HAVE INTEGER KEYS
		String contract = """
				---
				request:
				  method: "GET"
				  url: "test"
				response:
				  status: 200
				  body:
				    property:\s
				      14: 0.0
				      7: 0.0
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['property']\").field(\"['7']\").isEqualTo(0.0)")
			.contains("assertThatJson(parsedJson).field(\"['property']\").field(\"['14']\").isEqualTo(0.0)");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should generate assertions for array in response body with [{0}]")
	@MethodSource("spockMockmvcWebclientCustom")
	void shouldGenerateAssertionsForArrayInResponseBody(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "test"
				response:
				  status: 200
				  body: '
				  [
					{
					 "property1": "a"
					},
					{
					 "property2": "b"
					}
				  ]'
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test)
			.contains(
					"assertThatJson(parsedJson).array().elementWithIndex(0).field(\"['property1']\").isEqualTo(\"a\")")
			.contains(
					"assertThatJson(parsedJson).array().elementWithIndex(1).field(\"['property2']\").isEqualTo(\"b\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should generate assertions for array inside response body element with [{0}]")
	@MethodSource("spockMockmvcWebclientCustom")
	void shouldGenerateAssertionsForArrayInsideResponseBodyElement(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "test"
				response:
				  status: 200
				  body: '
				    {
				      "property1": [
				        { "property2": "test1"},
				        { "property3": "test2"}
				      ]
				    }'
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(
				"assertThatJson(parsedJson).array(\"['property1']\").elementWithIndex(0).field(\"['property2']\").isEqualTo(\"test1\")")
			.contains(
					"assertThatJson(parsedJson).array(\"['property1']\").elementWithIndex(1).field(\"['property3']\").isEqualTo(\"test2\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should generate assertions for nested objects in response body with [{0}]")
	@MethodSource("spockMockmvcWebclientCustom")
	void shouldGenerateAssertionsForNestedObjectsInResponseBody(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "test"
				response:
				  status: 200
				  body: '
				  {
				    "property1": "a",
				    "property2": {"property3": "b"}
				  }'
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test)
			.contains("assertThatJson(parsedJson).field(\"['property2']\").field(\"['property3']\").isEqualTo(\"b\")")
			.contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should generate assertions for a response body containing an empty list with [{0}]")
	@MethodSource("spockTestngMockmvcWebclient")
	void shouldGenerateAssertionsForResponseBodyWithEmptyList(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		String contract = """
				---
				description: Returns an empty collection
				request:
				  method: GET
				  urlPath: /url
				response:
				  status: 200
				  body: []
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatJson(parsedJson).isEmpty()");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should generate assertions for a response body containing an empty map with [{0}]")
	@MethodSource("spockTestngMockmvcWebclient")
	void shouldGenerateAssertionsForResponseBodyWithEmptyMap(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		String contract = """
				---
				description: Returns an empty map
				request:
				  method: GET
				  urlPath: /url
				response:
				  status: 200
				  body: {}
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatJson(parsedJson).isEmpty()");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should generate regex assertions for map objects in response body with [{0}]")
	@MethodSource("spockMockmvcWebclientCustom")
	void shouldGenerateRegexAssertionsForMapObjectsInResponseBody(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "test"
				response:
				  status: 200
				  body:
				    property1: "a"
				    property2: "123"
				  matchers:
				    body:
				      - path: $.property2
				        type: by_regex
				        value: "[0-9]{3}"
				        regexType: as_integer
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("$.property2\", String.class)).matches(\"[0-9]{3}\")")
			.contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should generate a call with an url path and query parameters with [{0}]")
	@MethodSource("spockMockmvcWebclientCustom")
	void shouldGenerateCallWithUrlPathAndQueryParameters(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		String contract = """
				---
				request:
				  method: "GET"
				  urlPath: "/users"
				  queryParameters:
				    'limit': "10"
				    'offset': "20"
				    'filter': "email"
				    'sort': "name"
				    'search': "55"
				    'age': "99"
				    'name': "Denis.Stepanov"
				    'email': "bob@email.com"
				  matchers:
				    queryParameters:
				      - key: limit
				        type: equal_to
				        value: "10"
				      - key: offset
				        type: containing
				        value: "20"
				      - key: sort
				        type: equal_to
				        value: "name"
				      - key: sort
				        type: not_matching
				        value: "^/[0-9]{2}$"
				      - key: age
				        type: not_matching
				        value: '^\\\\w*$'
				      - key: name
				        type: matching
				        value: 'Denis.*'
				      - key: hello
				        type: absent
				response:
				  status: 200
				  body:
				    property1: "a"
				    property2: "b"
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(".queryParam(\"limit\",\"10\")")
			.contains(".queryParam(\"offset\",\"20\")")
			.contains(".queryParam(\"filter\",\"email\")")
			.contains(".queryParam(\"sort\",\"name\")")
			.contains(".queryParam(\"search\",\"55\")")
			.contains(".queryParam(\"age\",\"99\")")
			.contains(".queryParam(\"name\",\"Denis.Stepanov\")")
			.contains(".queryParam(\"email\",\"bob@email.com\")")
			.contains(".get(\"/users\")")
			.contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")")
			.contains("assertThatJson(parsedJson).field(\"['property2']\").isEqualTo(\"b\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(
			name = "should generate a call with an url path and query parameters with url containing a pattern with [{0}]")
	@MethodSource("spockMockmvcWebclientCustom")
	void shouldGenerateCallWithUrlPathAndQueryParametersWithUrlPattern(String methodBuilderName,
			TestFramework framework, TestMode mode) {
		String contract = """
				---
				request:
				  method: "GET"
				  urlPath: "/foo/123456"
				  queryParameters:
				    'limit': "10"
				    'offset': "20"
				    'filter': "email"
				    'sort': "name"
				    'search': "55"
				    'age': "99"
				    'name': "Denis.Stepanov"
				    'email': "bob@email.com"
				  matchers:
				    url:
				      regex: '/foo/[0-9]+'
				    queryParameters:
				      - key: limit
				        type: equal_to
				        value: "10"
				      - key: offset
				        type: containing
				        value: "20"
				      - key: sort
				        type: equal_to
				        value: "name"
				      - key: sort
				        type: not_matching
				        value: "^/[0-9]{2}$"
				      - key: age
				        type: not_matching
				        value: '^\\\\w*$'
				      - key: name
				        type: matching
				        value: 'Denis.*'
				      - key: hello
				        type: absent
				response:
				  status: 200
				  body:
				    property1: "a"
				    property2: "b"
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(".queryParam(\"limit\",\"10\")")
			.contains(".queryParam(\"offset\",\"20\")")
			.contains(".queryParam(\"filter\",\"email\")")
			.contains(".queryParam(\"sort\",\"name\")")
			.contains(".queryParam(\"search\",\"55\")")
			.contains(".queryParam(\"age\",\"99\")")
			.contains(".queryParam(\"name\",\"Denis.Stepanov\")")
			.contains(".queryParam(\"email\",\"bob@email.com\")")
			.contains(".get(\"/foo/123456\")")
			.contains("assertThatJson(parsedJson).field(\"['property1']\").isEqualTo(\"a\")")
			.contains("assertThatJson(parsedJson).field(\"['property2']\").isEqualTo(\"b\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should generate test for empty body with [{0}]")
	@MethodSource("spockMockmvcWebclientCustomWithBodyString")
	void shouldGenerateTestForEmptyBody(String methodBuilderName, TestFramework framework, TestMode mode,
			String bodyString) {
		String contract = """
				---
				request:
				  method: "POST"
				  url: "/ws/payments"
				  body: ""
				response:
				  status: 406
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(bodyString);
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	static Stream<Arguments> stringInResponseBodyVariants() {
		return Stream.of(
				Arguments.of("spock", TestFramework.SPOCK, null, "String responseBody = response.body.asString()",
						"responseBody == 'test'"),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC, "String responseBody = response.getBody().asString();",
						"assertThat(responseBody).isEqualTo(\"test\");"),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT,
						"String responseBody = response.getBody().asString();",
						"assertThat(responseBody).isEqualTo(\"test\");"),
				Arguments.of("custom", null, TestMode.CUSTOM, "String responseBody = response.getBody().asString();",
						"assertThat(responseBody).isEqualTo(\"test\");"));
	}

	@ParameterizedTest(name = "should generate test for String in response body with [{0}]")
	@MethodSource("stringInResponseBodyVariants")
	void shouldGenerateTestForStringInResponseBody(String methodBuilderName, TestFramework framework, TestMode mode,
			String bodyDefinitionString, String bodyEvaluationString) {
		String contract = """
				---
				request:
				  method: "POST"
				  url: "test"
				  body: ""
				response:
				  status: 200
				  body: "test"
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(bodyDefinitionString).contains(bodyEvaluationString);
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	static Stream<Arguments> regexTestForStringInResponseHeaderVariants() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null,
				"response.header(\"Location\") ==~ java.util.regex.Pattern.compile('http://localhost/partners/[0-9]+/users/[0-9]+')"),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC,
						"assertThat(response.header(\"Location\")).matches(\"http://localhost/partners/[0-9]+/users/[0-9]+\");"),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT,
						"assertThat(response.header(\"Location\")).matches(\"http://localhost/partners/[0-9]+/users/[0-9]+\");"),
				Arguments.of("custom", null, TestMode.CUSTOM,
						"assertThat(response.header(\"Location\")).matches(\"http://localhost/partners/[0-9]+/users/[0-9]+\");"));
	}

	@ParameterizedTest(name = "should generate regex test for String in response header with [{0}]")
	@MethodSource("regexTestForStringInResponseHeaderVariants")
	void shouldGenerateRegexTestForStringInResponseHeader(String methodBuilderName, TestFramework framework,
			TestMode mode, String headerEvaluationString) {
		String contract = """
				---
				request:
				  method: "POST"
				  url: "/partners/1000/users"
				  headers:
				    "Content-Type": "application/json"
				  body:
				    first_name: 'John'
				    last_name: 'Smith'
				    personal_id: '12345678901'
				    phone_number: '500500500'
				    invitation_token: '00fec7141bb94793bfe7ae1d0f39bda0'
				    password: 'john'
				  matchers:
				    headers:
				      - key: 'Content-Type'
				        regex: 'application/json.*'
				    url:
				      regex: '/partners/[0-9]+/users'
				response:
				  status: 201
				  headers:
				    "Location": "http://localhost/partners/1000/users/1001"
				  matchers:
				    headers:
				      - key: 'Location'
				        regex: 'http://localhost/partners/[0-9]+/users/[0-9]+'
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(headerEvaluationString);
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should work with more complex stuff and jsonpaths with [{0}]")
	@MethodSource("spockMockmvcWebclientCustom")
	void shouldWorkWithMoreComplexStuffAndJsonpaths(String methodBuilderName, TestFramework framework, TestMode mode) {
		String contract = """
				---
				priority: 10
				request:
				  method: "POST"
				  url: "/validation/client"
				  headers:
				    "Content-Type": "application/json"
				  body:
				    bank_account_number: '0014282912345698765432161182'
				    email: 'foo@bar.com'
				    phone_number: '100299300'
				    personal_id: 'ABC123456'
				  matchers:
				    headers:
				      - key: 'Content-Type'
				        regex: 'application/json.*'
				    url:
				      regex: '/partners/[0-9]+/users'
				response:
				  status: 201
				  body:
				    errors:
				    - property: "bank_account_number"
				      message: "incorrect_format"
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(
				"assertThatJson(parsedJson).array(\"['errors']\").elementWithIndex(0).field(\"['property']\").isEqualTo(\"bank_account_number\")")
			.contains(
					"assertThatJson(parsedJson).array(\"['errors']\").elementWithIndex(0).field(\"['message']\").isEqualTo(\"incorrect_format\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	static Stream<Arguments> executeMethodAssertionStrings() {
		return Stream.of(
				Arguments.of("spock", TestFramework.SPOCK, null,
						List.of("assertThatRejectionReasonIsNull(parsedJson.read(\"\\$.rejectionReason\"))",
								"assertThatLocationIsNull(response.header(\"Location\"))")),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC,
						List.of("assertThatRejectionReasonIsNull(parsedJson.read(\"$.rejectionReason\"))",
								"assertThatLocationIsNull(response.header(\"Location\"))")),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT,
						List.of("assertThatRejectionReasonIsNull(parsedJson.read(\"$.rejectionReason\"))",
								"assertThatLocationIsNull(response.header(\"Location\"))")),
				Arguments.of("custom", null, TestMode.CUSTOM,
						List.of("assertThatRejectionReasonIsNull(parsedJson.read(\"$.rejectionReason\"))",
								"assertThatLocationIsNull(response.header(\"Location\"))")));
	}

	@ParameterizedTest(name = "should make the execute method work with [{0}]")
	@MethodSource("executeMethodAssertionStrings")
	void shouldMakeExecuteMethodWork(String methodBuilderName, TestFramework framework, TestMode mode,
			List<String> assertionStrings) {
		String contract = """
				---
				priority: 10
				request:
				  method: "PUT"
				  url: "/fraudcheck"
				response:
				  status: 201
				  headers:
				    "Location": null
				  body:
				    rejectionReason: "foo"
				  matchers:
				    body:
				      - path: "$.rejectionReason"
				        type: by_command
				        value: assertThatRejectionReasonIsNull($it)
				    headers:
				      - key: "Location"
				        command: assertThatLocationIsNull($it)
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		for (String assertionString : assertionStrings) {
			assertThat(test).contains(assertionString);
		}
	}

	@ParameterizedTest(name = "shouldn't generate unicode escape characters with [{0}]")
	@MethodSource("spockMockmvcWebclientCustom")
	void shouldNotGenerateUnicodeEscapeCharacters(String methodBuilderName, TestFramework framework, TestMode mode) {
		String contract = """
				---
				priority: 10
				request:
				  method: "PUT"
				  url: "/v1/payments/e86df6f693de4b35ae648464c5b0dc09/енев"
				  headers:
				    "Content-Type": "application/json"
				  body:
				    client:
				      first_name: "Пенева"
				      last_name: "Пенева"
				  matchers:
				    headers:
				      - key: 'Content-Type'
				        regex: 'application/json.*'
				        regexType: as_string
				    body:
				      - path: $.first_name
				        type: by_regex
				        value: '[\\\\p{L}]*'
				        regexType: as_string
				      - path: $.last_name
				        type: by_regex
				        value: '[\\\\p{L}]*'
				response:
				  status: 201
				  matchers:
				    headers:
				      - key: 'Content-Type'
				        regex: 'application/json.*'
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).doesNotContain("\\u041f");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	static Stream<Arguments> multipartWithRegexRequestStrings() {
		return Stream.of(
				Arguments.of("spock", TestFramework.SPOCK, null,
						List.of("\"Content-Type\", \"multipart/form-data;boundary=AaB03x\"",
								".param('formParameter', '''\"formParameterValue\"''')",
								".param('someBooleanParameter', 'true')",
								".multiPart('file', 'filename.csv', 'file content'.bytes, 'application/json')")),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC, List.of(
						"\"Content-Type\", \"multipart/form-data;boundary=AaB03x\"",
						".param(\"formParameter\", \"\\\"formParameterValue\\\"\")",
						".param(\"someBooleanParameter\", \"true\")",
						".multiPart(\"file\", \"filename.csv\", \"file content\".getBytes(), \"application/json\")")),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT, List.of(
						"\"Content-Type\", \"multipart/form-data;boundary=AaB03x\"",
						".param(\"formParameter\", \"\\\"formParameterValue\\\"\")",
						".param(\"someBooleanParameter\", \"true\")",
						".multiPart(\"file\", \"filename.csv\", \"file content\".getBytes(), \"application/json\")")));
	}

	@ParameterizedTest(name = "should generate proper test code when having multipart parameters with [{0}]")
	@MethodSource("multipartWithRegexRequestStrings")
	void shouldGenerateProperTestCodeWhenHavingMultipartParameters(String methodBuilderName, TestFramework framework,
			TestMode mode, List<String> requestStrings) {
		String contract = """
				---
				request:
				  method: "PUT"
				  url: "/multipart"
				  headers:
				    "Content-Type": 'multipart/form-data;boundary=AaB03x'
				  multipart:
				    params:\s
				      formParameter: '"formParameterValue"'
				      someBooleanParameter: 'true'
				    named:\s
				      - paramName: "file"
				        fileName: 'filename.csv'
				        fileContent: 'file content'
				        contentType: 'application/json'
				  matchers:
				    headers:
				      - key: 'Content-Type'
				        regex: 'multipart/form-data;boundary=AaB03x.*'
				    multipart:
				      params:
				        - key: formParameter
				          regex: ".+"
				        - key: someBooleanParameter
				          predefined: any_boolean
				      named:
				        - paramName: file
				          fileName:
				            predefined: non_empty
				          fileContent:
				            predefined: non_empty
				          contentType:
				            predefined: non_empty
				response:
				  status: 201
				  matchers:
				    headers:
				      - key: 'Content-Type'
				        regex: 'application/json.*'
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		for (String requestString : requestStrings) {
			assertThat(test).contains(requestString);
		}
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	static Stream<Arguments> multipartWithByteArrayRequestStrings() {
		return Stream.of(
				Arguments.of("spock", TestFramework.SPOCK, null,
						List.of("\"Content-Type\", \"multipart/form-data;boundary=AaB03x\"",
								".multiPart('file', 'filename.csv', 'file content'.bytes, 'application/json')")),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC, List.of(
						"\"Content-Type\", \"multipart/form-data;boundary=AaB03x\"",
						".multiPart(\"file\", \"filename.csv\", \"file content\".getBytes(), \"application/json\")")),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT, List.of(
						"\"Content-Type\", \"multipart/form-data;boundary=AaB03x\"",
						".multiPart(\"file\", \"filename.csv\", \"file content\".getBytes(), \"application/json\")")));
	}

	@ParameterizedTest(name = "should generate test code when having multipart parameters with byte array [{0}]")
	@MethodSource("multipartWithByteArrayRequestStrings")
	void shouldGenerateTestCodeWhenHavingMultipartParametersWithByteArray(String methodBuilderName,
			TestFramework framework, TestMode mode, List<String> requestStrings) {
		String contract = """
				---
				request:
				  method: "PUT"
				  url: "/multipart"
				  headers:
				    "Content-Type": 'multipart/form-data;boundary=AaB03x'
				  multipart:
				    params:\s
				      formParameter: '"formParameterValue"'
				      someBooleanParameter: 'true'
				    named:\s
				      - paramName: "file"
				        fileName: 'filename.csv'
				        fileContent: "file content"
				        contentType: 'application/json'
				  matchers:
				    headers:
				      - key: 'Content-Type'
				        regex: 'multipart/form-data;boundary=AaB03x.*'
				    multipart:
				      params:
				        - key: formParameter
				          regex: ".+"
				        - key: someBooleanParameter
				          predefined: any_boolean
				      named:
				        - paramName: file
				          fileName:
				            predefined: non_empty
				          fileContent:
				            predefined: non_empty
				          contentType:
				            predefined: non_empty
				response:
				  status: 201
				  matchers:
				    headers:
				      - key: 'Content-Type'
				        regex: 'application/json.*'
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		for (String requestString : requestStrings) {
			assertThat(test).contains(requestString);
		}
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	static Stream<Arguments> multipartWithExecuteRequestStrings() {
		return Stream.of(
				Arguments.of("spock", TestFramework.SPOCK, null,
						List.of("\"Content-Type\", \"multipart/form-data;boundary=AaB03x\"",
								".param('formParameter', '''\"formParameterValue\"''')",
								".param('someBooleanParameter', 'true')",
								".multiPart('file', toString(), 'file content'.bytes, 'application/json')")),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC,
						List.of("\"Content-Type\", \"multipart/form-data;boundary=AaB03x\"",
								".param(\"formParameter\", \"\\\"formParameterValue\\\"\")",
								".param(\"someBooleanParameter\", \"true\")",
								".multiPart(\"file\", toString(), \"file content\".getBytes(), \"application/json\")")),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT, List.of(
						"\"Content-Type\", \"multipart/form-data;boundary=AaB03x\"",
						".param(\"formParameter\", \"\\\"formParameterValue\\\"\")",
						".param(\"someBooleanParameter\", \"true\")",
						".multiPart(\"file\", toString(), \"file content\".getBytes(), \"application/json\")")));
	}

	@ParameterizedTest(
			name = "should generate proper test code when having multipart parameters that use execute with [{0}]")
	@MethodSource("multipartWithExecuteRequestStrings")
	void shouldGenerateProperTestCodeWhenHavingMultipartParametersThatUseExecute(String methodBuilderName,
			TestFramework framework, TestMode mode, List<String> requestStrings) {
		String contract = """
				---
				request:
				  method: "PUT"
				  url: "/multipart"
				  headers:
				    "Content-Type": 'multipart/form-data;boundary=AaB03x'
				  multipart:
				    params:\s
				      formParameter: '"formParameterValue"'
				      someBooleanParameter: 'true'
				    named:\s
				      - paramName: "file"
				        fileNameCommand: 'toString()'
				        fileContent: 'file content'
				        contentType: 'application/json'
				  matchers:
				    headers:
				      - key: 'Content-Type'
				        regex: 'multipart/form-data;boundary=AaB03x.*'
				    multipart:
				      params:
				        - key: formParameter
				          regex: ".+"
				          regexType: as_string
				        - key: someBooleanParameter
				          predefined: any_boolean
				          regexType: as_boolean
				      named:
				        - paramName: file
				          fileName:
				            predefined: non_empty
				          fileContent:
				            predefined: non_empty
				          contentType:
				            predefined: non_empty
				response:
				  status: 201
				  matchers:
				    headers:
				      - key: 'Content-Type'
				        regex: 'application/json.*'
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		for (String requestString : requestStrings) {
			assertThat(test).contains(requestString);
		}
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@Test
	void shouldParseJsonWithArraysUsingSpock() {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "/auth/oauth/check_token"
				response:
				  status: 200
				  body:
				    authorities:
				     - ROLE_ADMIN
				  matchers:
				    body:
				      - path: $.authorities[0]
				        type: by_regex
				        value: '^[a-zA-Z0-9_\\- ]+$'
				        regexType: as_string
				""";
		Contract contractDsl = fromYaml(contract);
		properties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(
				"assertThat(parsedJson.read(\"\\$.authorities[0]\", String.class)).matches(\"^[a-zA-Z0-9_\\\\- ]+\\$\")");
		SyntaxChecker.tryToCompileWithoutCompileStatic("spock", test);
	}

	@Test
	void shouldParseJsonWithArraysUsingJUnit() {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "/auth/oauth/check_token"
				response:
				  status: 200
				  body:
				    authorities:
				     - ROLE_ADMIN
				  matchers:
				    body:
				      - path: $.authorities[0]
				        type: by_regex
				        value: '^[a-zA-Z0-9_\\- ]+$'
				""";
		Contract contractDsl = fromYaml(contract);
		properties.setTestFramework(TestFramework.JUNIT5);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(
				"assertThat(parsedJson.read(\"$.authorities[0]\", String.class)).matches(\"^[a-zA-Z0-9_\\\\- ]+$\")");
		SyntaxChecker.tryToCompileJava("mockmvc", test);
	}

	@ParameterizedTest(name = "should work with execution property with [{0}]")
	@MethodSource("spockMockmvcCustom")
	void shouldWorkWithExecutionProperty(String methodBuilderName, TestFramework framework, TestMode mode) {
		String contract = """
				---
				request:
				  method: "PUT"
				  url: "/fraudcheck"
				response:
				  status: 200
				  body:
				    fraudCheckStatus: "OK"
				    rejectionReason: null
				  matchers:
				    body:
				      - path: $.rejectionReason
				        type: by_command
				        value: "assertThatRejectionReasonIsNull($it)"
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).doesNotContain(
				"assertThatJson(parsedJson).field(\"['rejectionReason']\").isEqualTo(\"assertThatRejectionReasonIsNull(\"")
			.contains("assertThatRejectionReasonIsNull(");
	}

	@Test
	void shouldGenerateProperTestCodeWithMapInsideList() {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "/foos"
				response:
				  status: 200
				  body:
				    - id: "123"
				    - id: "567"
				  headers:
				    "Content-Type": "application/json;charset=UTF-8"
				  matchers:
				    body:
				      - path: $[0].id
				        type: by_regex
				        value: "[0-9]+"
				      - path: $[1].id
				        type: by_regex
				        value: "[0-9]+"
				""";
		Contract contractDsl = fromYaml(contract);
		properties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThat(parsedJson.read(\"\\$[0].id\", String.class)).matches(\"[0-9]+\")")
			.contains("assertThat(parsedJson.read(\"\\$[1].id\", String.class)).matches(\"[0-9]+\")");
		SyntaxChecker.tryToCompileWithoutCompileStatic("spock", test);
	}

	@ParameterizedTest(name = "should generate proper test code with top level array using [{0}]")
	@MethodSource("spockMockmvcWebclientCustom")
	void shouldGenerateProperTestCodeWithTopLevelArray(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "/api/tags"
				response:
				  status: 200
				  body:\s
				    - "Java"
				    - "Java8"
				    - "Spring"
				    - "SpringBoot"
				    - "Stream"
				  headers:
				    "Content-Type": "application/json;charset=UTF-8"
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatJson(parsedJson).array().elementWithIndex(0).isEqualTo(\"Java\")")
			.contains("assertThatJson(parsedJson).array().elementWithIndex(1).isEqualTo(\"Java8\")")
			.contains("assertThatJson(parsedJson).array().elementWithIndex(2).isEqualTo(\"Spring\")")
			.contains("assertThatJson(parsedJson).array().elementWithIndex(3).isEqualTo(\"SpringBoot\")")
			.contains("assertThatJson(parsedJson).array().elementWithIndex(4).isEqualTo(\"Stream\")");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should generate proper test code with top level array using [{0}] with array size check")
	@MethodSource("spockMockmvcWebclientCustom")
	void shouldGenerateProperTestCodeWithTopLevelArrayWithArraySizeCheck(String methodBuilderName,
			TestFramework framework, TestMode mode) {
		String savedProp = System.getProperty("spring.cloud.contract.verifier.assert.size");
		System.setProperty("spring.cloud.contract.verifier.assert.size", "true");
		try {
			String contract = """
					---
					request:
					  method: "GET"
					  url: "/api/tags"
					response:
					  status: 200
					  body: ["Java", "Java8", "Spring", "SpringBoot", "Stream"]
					  headers:
					    "Content-Type": "application/json;charset=UTF-8"
					""";
			Contract contractDsl = fromYaml(contract);
			applyMethodBuilder(framework, mode);
			String test = singleTestGenerator(contractDsl);
			assertThat(test).contains("assertThatJson(parsedJson).hasSize(5)")
				.contains("assertThatJson(parsedJson).array().elementWithIndex(0).isEqualTo(\"Java\")")
				.contains("assertThatJson(parsedJson).array().elementWithIndex(1).isEqualTo(\"Java8\")")
				.contains("assertThatJson(parsedJson).array().elementWithIndex(2).isEqualTo(\"Spring\")")
				.contains("assertThatJson(parsedJson).array().elementWithIndex(3).isEqualTo(\"SpringBoot\")")
				.contains("assertThatJson(parsedJson).array().elementWithIndex(4).isEqualTo(\"Stream\")");
			SyntaxChecker.tryToCompile(methodBuilderName, test);
		}
		finally {
			if (savedProp != null) {
				System.setProperty("spring.cloud.contract.verifier.assert.size", savedProp);
			}
			else {
				System.clearProperty("spring.cloud.contract.verifier.assert.size");
			}
		}
	}

	@ParameterizedTest(name = "should generate proper test code with top level array of arrays using [{0}]")
	@MethodSource("spockMockmvcWebclientCustom")
	void shouldGenerateProperTestCodeWithTopLevelArrayOfArrays(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "/api/categories"
				response:
				  status: 200
				  body: [["Programming", "Java"], ["Programming", "Java", "Spring", "Boot"]]
				  headers:
				    "Content-Type": "application/json;charset=UTF-8"
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(
				"assertThatJson(parsedJson).array().elementWithIndex(0).elementWithIndex(0).isEqualTo(\"Programming\")")
			.contains("assertThatJson(parsedJson).array().elementWithIndex(0).elementWithIndex(1).isEqualTo(\"Java\")")
			.contains(
					"assertThatJson(parsedJson).array().elementWithIndex(1).elementWithIndex(0).isEqualTo(\"Programming\")")
			.contains("assertThatJson(parsedJson).array().elementWithIndex(1).elementWithIndex(1).isEqualTo(\"Java\")")
			.contains(
					"assertThatJson(parsedJson).array().elementWithIndex(1).elementWithIndex(2).isEqualTo(\"Spring\")")
			.contains("assertThatJson(parsedJson).array().elementWithIndex(1).elementWithIndex(3).isEqualTo(\"Boot\")");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should generate async body when async flag set in response [{0}]")
	@MethodSource("spockMockmvcWebclientAsync")
	void shouldGenerateAsyncBodyWhenAsyncFlagSetInResponse(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "test"
				response:
				  async: true
				  status: 200
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(".when().async()");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(
			name = "should generate async body after queryParams when async flag set in response and queryParams set in request [{0}]")
	@MethodSource("spockMockmvcWebclientAsync")
	void shouldGenerateAsyncBodyAfterQueryParamsWhenAsyncFlagSet(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "/test"
				  queryParameters:
				    param: value
				response:
				  async: true
				  status: 200
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		String strippedTest = test.replace("\n", "").replace(" ", "").replace("\t", "").strip();
		assertThat(strippedTest).contains(".queryParam(\"param\",\"value\").when().async().get(\"/test\")");
		stubMappingIsValidWireMockStub(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should generate proper test code with array of primitives using [{0}]")
	@MethodSource("spockMockmvcWebclientCustom")
	void shouldGenerateProperTestCodeWithArrayOfPrimitives(String methodBuilderName, TestFramework framework,
			TestMode mode) {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "/api/tags"
				response:
				  status: 200
				  body:
				    - partners:
				        payment_methods:\s
				          - BANK
				          - CASH
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(
				"assertThatJson(parsedJson).array().elementWithIndex(0).field(\"['partners']\").array(\"['payment_methods']\").elementWithIndex(0).isEqualTo(\"BANK\")")
			.contains(
					"assertThatJson(parsedJson).array().elementWithIndex(0).field(\"['partners']\").array(\"['payment_methods']\").elementWithIndex(1).isEqualTo(\"CASH\")");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@Test
	void shouldNotEscapeDollarInSpockRegexTests() {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "/get"
				response:
				  status: 200
				  body:
				    code: 9
				    message: Wrong credentials
				  matchers:
				    body:
				      - path: $.message
				        type: by_regex
				        value: '^(?!\\\\s*$).+'
				""";
		Contract contractDsl = fromYaml(contract);
		properties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDsl);
		assertThat(test)
			.contains("assertThat(parsedJson.read(\"\\$.message\", String.class)).matches(\"^(?!\\\\\\\\s*\\$).+\")");
		SyntaxChecker.tryToCompileGroovy("spock", test, false);
	}

	@Test
	void shouldExecuteCustomMethodForComplexStructuresOnTheResponseSide() {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "/get"
				response:
				  status: 200
				  body:
				    fraudCheckStatus: "OK"
				    rejectionReason:
				      title: null
				  matchers:
				    body:
				      - path: $.rejectionReason.title
				        type: by_command
				        value: "assertThatRejectionReasonIsNull($it)"
				""";
		Contract contractDsl = fromYaml(contract);
		properties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatRejectionReasonIsNull(parsedJson.read(\"\\$.rejectionReason.title\"))");
		SyntaxChecker.tryToCompileGroovy("spock", test);
	}

	@Test
	void shouldExecuteCustomMethodForMoreComplexStructuresOnTheResponseSideWhenUsingSpock() {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "/get"
				response:
				  status: 200
				  body:
				    - name: "userName 1"
				    - name: "userName 2"
				  matchers:
				    body:
				      - path: $[0].name
				        type: by_command
				        value: "assertThatUserNameIsNotNull($it)"
				      - path: $[1].name
				        type: by_command
				        value: "assertThatUserNameIsNotNull($it)"
				""";
		Contract contractDsl = fromYaml(contract);
		properties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatUserNameIsNotNull(parsedJson.read(\"\\$[0].name\"))")
			.contains("assertThatUserNameIsNotNull(parsedJson.read(\"\\$[1].name\"))");
	}

	@Test
	void shouldExecuteCustomMethodForMoreComplexStructuresOnTheResponseSideWhenUsingJUnit() {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "/get"
				response:
				  status: 200
				  body:
				    - name: "userName 1"
				    - name: "userName 2"
				  matchers:
				    body:
				      - path: $[0].name
				        type: by_command
				        value: "assertThatUserNameIsNotNull($it)"
				      - path: $[1].name
				        type: by_command
				        value: "assertThatUserNameIsNotNull($it)"
				""";
		Contract contractDsl = fromYaml(contract);
		properties.setTestFramework(TestFramework.JUNIT5);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("assertThatUserNameIsNotNull(parsedJson.read(\"$[0].name\")")
			.contains("assertThatUserNameIsNotNull(parsedJson.read(\"$[1].name\")");
	}

	@Test
	void shouldExecuteCustomMethodForRequestHeaders() {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "/get"
				  headers:
				    authorization: Bearer token
				  matchers:
				    headers:
				      - key: "authorization"
				        command: "getOAuthTokenHeader()"
				response:
				  status: 200
				""";
		Contract contractDsl = fromYaml(contract);
		properties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(".header(\"authorization\", getOAuthTokenHeader())");
		SyntaxChecker.tryToCompileGroovy("spock", test);
	}

	@Test
	void shouldSupportBodyMatchingInResponse() {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "/get"
				response:
				  status: 200
				  body: "HELLO FROM STUB"
				  matchers:
				    body:
				      - type: by_regex
				        value: ".*"
				""";
		Contract contractDsl = fromYaml(contract);
		properties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("responseBody ==~ java.util.regex.Pattern.compile('.*')");
		SyntaxChecker.tryToCompileGroovy("spock", test);
	}

	@Test
	void shouldSupportCustomMethodExecutionInResponse() {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "/get"
				response:
				  status: 200
				  body: "HELLO FROM STUB"
				  matchers:
				    body:
				      - type: by_command
				        value: "foo($it)"
				""";
		Contract contractDsl = fromYaml(contract);
		properties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains("foo(responseBody)");
		SyntaxChecker.tryToCompileGroovy("spock", test);
	}

	static Stream<Arguments> escapeRegexForContentTypeVariants() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null,
				"response.header(\"Content-Type\") ==~ java.util.regex.Pattern.compile('application.vnd.fraud.v1.json.*')"),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC,
						"assertThat(response.header(\"Content-Type\")).matches(\"application.vnd.fraud.v1.json.*\")"),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT,
						"assertThat(response.header(\"Content-Type\")).matches(\"application.vnd.fraud.v1.json.*\")"),
				Arguments.of("custom", null, TestMode.CUSTOM,
						"assertThat(response.header(\"Content-Type\")).matches(\"application.vnd.fraud.v1.json.*\")"));
	}

	@ParameterizedTest(name = "should escape regex properly for content type [{0}]")
	@MethodSource("escapeRegexForContentTypeVariants")
	void shouldEscapeRegexProperlyForContentType(String methodBuilderName, TestFramework framework, TestMode mode,
			String expectedMatcher) {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "/get"
				  headers:
				    "Content-Type": "application/vnd.fraud.v1+json"
				  matchers:
				    headers:
				      - key: 'Content-Type'
				        regex: 'application.vnd.fraud.v1.json.*'
				response:
				  status: 200
				  headers:
				    "Content-Type": "application/vnd.fraud.v1+json"
				  matchers:
				    headers:
				      - key: 'Content-Type'
				        regex: 'application.vnd.fraud.v1.json.*'
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(expectedMatcher);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	static Stream<Arguments> plainTextProperlyViaHeadersVariants() {
		return Stream.of(
				Arguments.of("spock", TestFramework.SPOCK, null, "responseBody == '''{\"a\":1}\\n{\"a\":2}\\n'''"),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC,
						"assertThat(responseBody).isEqualTo(\"{\\\"a\\\":1}\\n{\\\"a\\\":2}\\n"),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT,
						"assertThat(responseBody).isEqualTo(\"{\\\"a\\\":1}\\n{\\\"a\\\":2}\\n"),
				Arguments.of("custom", null, TestMode.CUSTOM,
						"assertThat(responseBody).isEqualTo(\"{\\\"a\\\":1}\\n{\\\"a\\\":2}\\n"));
	}

	@ParameterizedTest(name = "should resolve plain text properly via headers with [{0}]")
	@MethodSource("plainTextProperlyViaHeadersVariants")
	void shouldResolvePlainTextProperlyViaHeaders(String methodBuilderName, TestFramework framework, TestMode mode,
			String expectedAssertion) {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "/foo"
				response:
				  status: 200
				  body: |
				    {"a":1}
				    {"a":2}
				  headers:
				    "Content-Type": "text/plain"
				  matchers:
				    headers:
				      - key: 'Content-Type'
				        regex: 'text/plain.*'
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).doesNotContain("assertThatJson(parsedJson).field(\"['a']\").isEqualTo(1)")
			.contains(expectedAssertion);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	static Stream<Arguments> plainTextThatHappensToBeValidJsonVariants() {
		return Stream.of(
				Arguments.of("spock", TestFramework.SPOCK, null,
						"responseBody ==~ java.util.regex.Pattern.compile('true|false')"),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC, "assertThat(responseBody).matches(\"true|false\");"),
				Arguments.of("mockmvc-testng", TestFramework.TESTNG, TestMode.MOCKMVC,
						"assertThat(responseBody).matches(\"true|false\");"),
				Arguments.of("jaxrs-spock", TestFramework.SPOCK, TestMode.JAXRSCLIENT,
						"responseBody ==~ java.util.regex.Pattern.compile(\"true|false\")"),
				Arguments.of("jaxrs", TestFramework.JUNIT5, TestMode.JAXRSCLIENT,
						"assertThat(responseBody).matches(\"true|false\");"),
				Arguments.of("custom", null, TestMode.CUSTOM, "assertThat(responseBody).matches(\"true|false\");"));
	}

	@ParameterizedTest(name = "should resolve plain text that happens to be a valid json for [{0}]")
	@MethodSource("plainTextThatHappensToBeValidJsonVariants")
	void shouldResolvePlainTextThatHappensToBeValidJson(String methodBuilderName, TestFramework framework,
			TestMode mode, String expectedContains) {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "/foo"
				response:
				  status: 200
				  body: 'true'
				  headers:
				    "Content-Type": "application/json;charset=utf-8"
				  matchers:
				    body:
				      - type: by_regex
				        value: 'true|false'
				    headers:
				      - key: 'Content-Type'
				        regex: 'application/json;charset=utf-8.*'
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(expectedContains);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@ParameterizedTest(name = "should escape quotes properly using [{0}]")
	@MethodSource("spockMockmvcCustom")
	void shouldEscapeQuotesProperly(String methodBuilderName, TestFramework framework, TestMode mode) {
		String contract = """
				---
				request:
				  method: "POST"
				  url: "/foo"
				  body:
				    xyz: 'abc'
				  headers:
				    "Content-Type": "application/json;charset=utf-8"
				response:
				  status: 200
				  body: '{ "bar": "some value \\u0022with quote\\u0022" }'
				  headers:
				    "Content-Type": "application/json;charset=utf-8"
				  matchers:
				    body:
				      - path: $.bar
				        type: by_regex
				        value: 'some value "with quote"|bar'
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		assertThat(test).contains(".matches(\"some value \\\"with quote\\\"|bar\")");
		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
	}

	static Stream<Arguments> executeMethodInUrlVariants() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC),
				Arguments.of("mockmvc-testng", TestFramework.TESTNG, TestMode.MOCKMVC),
				Arguments.of("jaxrs-spock", TestFramework.SPOCK, TestMode.JAXRSCLIENT),
				Arguments.of("jaxrs", TestFramework.JUNIT5, TestMode.JAXRSCLIENT),
				Arguments.of("custom", null, TestMode.CUSTOM));
	}

	@ParameterizedTest(name = "should make the execute method work in a url for [{0}]")
	@MethodSource("executeMethodInUrlVariants")
	void shouldMakeExecuteMethodWorkInUrl(String methodBuilderName, TestFramework framework, TestMode mode) {
		Contract contractDsl = Contract.make((c) -> {
			c.request((req) -> {
				req.method("POST");
				req.url(new DslProperty<>(new ClientDslProperty("foo"),
						new ServerDslProperty(new ExecutionProperty("toString()"))));
			});
			c.response((resp) -> resp.status(200));
		});
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
		assertThat(test).contains("toString()").doesNotContain("\"toString()\"").doesNotContain("'toString()'");
	}

	static Stream<Arguments> emptyListAssertionVariants() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC),
				Arguments.of("mockmvc-testng", TestFramework.TESTNG, TestMode.MOCKMVC),
				Arguments.of("jaxrs-spock", TestFramework.SPOCK, TestMode.JAXRSCLIENT),
				Arguments.of("jaxrs", TestFramework.JUNIT5, TestMode.JAXRSCLIENT),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT), Arguments.of("custom", null, TestMode.CUSTOM));
	}

	@ParameterizedTest(name = "should create an assertion for an empty list for [{0}]")
	@MethodSource("emptyListAssertionVariants")
	void shouldCreateAnAssertionForAnEmptyList(String methodBuilderName, TestFramework framework, TestMode mode) {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "/api/v1/xxxx"
				response:
				  status: 200
				  body:\s
				    status: '200'
				    list: []
				    foo   :\s
				      - "bar"
				      - "baz"
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
		assertThat(test).contains("assertThatJson(parsedJson).array(\"['list']\").isEmpty()")
			.doesNotContain("assertThatJson(parsedJson).array(\"['foo']\").isEmpty()");
	}

	static Stream<Arguments> bodyIsIntegerVariants() {
		return Stream.of(
				Arguments.of("spock", TestFramework.SPOCK, null, "body('''12000''')", "responseBody == '12000'"),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC, "body(\"12000\")",
						"assertThat(responseBody).isEqualTo(\"12000\");"),
				Arguments.of("mockmvc-testng", TestFramework.TESTNG, TestMode.MOCKMVC, "body(\"12000\")",
						"assertThat(responseBody).isEqualTo(\"12000\");"),
				Arguments.of("jaxrs-spock", TestFramework.SPOCK, TestMode.JAXRSCLIENT,
						".build(\"GET\", entity(\"12000\", \"text/plain\"))", "responseBody == \"12000\""),
				Arguments.of("jaxrs", TestFramework.JUNIT5, TestMode.JAXRSCLIENT,
						".build(\"GET\", entity(\"12000\", \"text/plain\"))",
						"assertThat(responseBody).isEqualTo(\"12000\")"),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT, "body(\"12000\")",
						"assertThat(responseBody).isEqualTo(\"12000\");"),
				Arguments.of("custom", null, TestMode.CUSTOM, "body(\"12000\")",
						"assertThat(responseBody).isEqualTo(\"12000\");"));
	}

	@ParameterizedTest(name = "should work properly when body is an integer [{0}]")
	@MethodSource("bodyIsIntegerVariants")
	void shouldWorkProperlyWhenBodyIsAnInteger(String methodBuilderName, TestFramework framework, TestMode mode,
			String requestContains, String responseContains) {
		String contract = """
				---
				request:
				  method: "GET"
				  url: "/api/v1/xxxx"
				  body: 12000
				response:
				  status: 200
				  body: 12000
				""";
		Contract contractDsl = fromYaml(contract);
		applyMethodBuilder(framework, mode);
		String test = singleTestGenerator(contractDsl);
		SyntaxChecker.tryToCompile(methodBuilderName, test);
		assertThat(test).contains(requestContains).contains(responseContains);
	}

}
