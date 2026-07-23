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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.RegexPatterns;
import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.config.TestMode;
import sh.stubborn.contract.verifier.dsl.wiremock.WireMockStubVerifier;
import sh.stubborn.contract.verifier.file.ContractMetadata;
import sh.stubborn.contract.verifier.util.SyntaxChecker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockMvcMethodBodyBuilderWithMatchersTests implements WireMockStubVerifier {

	private ContractVerifierConfigProperties properties;

	private static final SingleTestGenerator.GeneratedClassData GENERATED_CLASS_DATA = new SingleTestGenerator.GeneratedClassData(
			"foo", "com.example", new File(".").toPath());

	@BeforeEach
	void setup() {
		properties = new ContractVerifierConfigProperties();
		properties.setAssertJsonSize(true);
	}

	private String singleTestGenerator(Contract contractDsl) {
		ContractVerifierConfigProperties props = this.properties;
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
		}.buildClass(props, List.of(contractMetadata(contractDsl)), "foo", GENERATED_CLASS_DATA);
	}

	private ContractMetadata contractMetadata(Contract contractDsl) {
		return new ContractMetadata(new File(".").toPath(), false, 0, null, contractDsl);
	}

	// ---------------------------------------------------------------------------
	// #185: should allow to set dynamic values via stub / test matchers
	// ---------------------------------------------------------------------------

	private static Contract dynamicValueContract() {
		return Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.urlPath("/get");
				r.body(Map.of("duck", 123, "alpha", "abc", "number", 123, "aBoolean", true, "date", "2017-01-01",
						"dateTime", "2017-01-01T01:23:45", "time", "01:02:34", "valueWithoutAMatcher", "foo",
						"valueWithTypeMatch", "string", "key", Map.of("complex.key", "foo")));
				r.bodyMatchers((bm) -> {
					bm.jsonPath("$.duck", bm.byRegex("[0-9]{3}").asInteger());
					bm.jsonPath("$.duck", bm.byEquality());
					bm.jsonPath("$.alpha", bm.byRegex(RegexPatterns.onlyAlphaUnicode()).asString());
					bm.jsonPath("$.alpha", bm.byEquality());
					bm.jsonPath("$.number", bm.byRegex(RegexPatterns.number()).asInteger());
					bm.jsonPath("$.aBoolean", bm.byRegex(RegexPatterns.anyBoolean()).asBooleanType());
					bm.jsonPath("$.date", bm.byDate());
					bm.jsonPath("$.dateTime", bm.byTimestamp());
					bm.jsonPath("$.time", bm.byTime());
					bm.jsonPath("$.['key'].['complex.key']", bm.byEquality());
				});
				r.headers((h) -> h.contentType(h.applicationJson()));
			});
			c.response((res) -> {
				res.status(res.OK());
				Map<String, Object> responseBody = new HashMap<>();
				responseBody.put("duck", 123);
				responseBody.put("alpha", "abc");
				responseBody.put("number", 123);
				responseBody.put("positiveInteger", 1234567890);
				responseBody.put("negativeInteger", -1234567890);
				responseBody.put("positiveDecimalNumber", 123.4567890);
				responseBody.put("negativeDecimalNumber", -123.4567890);
				responseBody.put("aBoolean", true);
				responseBody.put("date", "2017-01-01");
				responseBody.put("dateTime", "2017-01-01T01:23:45");
				responseBody.put("time", "01:02:34");
				responseBody.put("valueWithoutAMatcher", "foo");
				responseBody.put("valueWithTypeMatch", "string");
				responseBody.put("valueWithMin", List.of(1, 2, 3));
				responseBody.put("valueWithMax", List.of(1, 2, 3));
				responseBody.put("valueWithMinMax", List.of(1, 2, 3));
				responseBody.put("valueWithMinEmpty", List.of());
				responseBody.put("valueWithMaxEmpty", List.of());
				responseBody.put("key", Map.of("complex.key", "foo"));
				responseBody.put("nullValue", null);
				res.body(responseBody);
				res.bodyMatchers((bm) -> {
					bm.jsonPath("$.duck", bm.byRegex("[0-9]{3}").asInteger());
					bm.jsonPath("$.duck", bm.byEquality());
					bm.jsonPath("$.alpha", bm.byRegex(RegexPatterns.onlyAlphaUnicode()).asString());
					bm.jsonPath("$.alpha", bm.byEquality());
					bm.jsonPath("$.number", bm.byRegex(RegexPatterns.number()).asInteger());
					bm.jsonPath("$.positiveInteger", bm.byRegex(RegexPatterns.anInteger()).asInteger());
					bm.jsonPath("$.negativeInteger", bm.byRegex(RegexPatterns.anInteger()).asInteger());
					bm.jsonPath("$.positiveDecimalNumber", bm.byRegex(RegexPatterns.aDouble()).asDouble());
					bm.jsonPath("$.negativeDecimalNumber", bm.byRegex(RegexPatterns.aDouble()).asDouble());
					bm.jsonPath("$.aBoolean", bm.byRegex(RegexPatterns.anyBoolean()).asBooleanType());
					bm.jsonPath("$.date", bm.byDate());
					bm.jsonPath("$.dateTime", bm.byTimestamp());
					bm.jsonPath("$.time", bm.byTime());
					bm.jsonPath("$.valueWithTypeMatch", bm.byType());
					bm.jsonPath("$.valueWithMin", bm.byType((t) -> t.minOccurrence(1)));
					bm.jsonPath("$.valueWithMax", bm.byType((t) -> t.maxOccurrence(3)));
					bm.jsonPath("$.valueWithMinMax", bm.byType((t) -> {
						t.minOccurrence(1);
						t.maxOccurrence(3);
					}));
					bm.jsonPath("$.valueWithMinEmpty", bm.byType((t) -> t.minOccurrence(0)));
					bm.jsonPath("$.valueWithMaxEmpty", bm.byType((t) -> t.maxOccurrence(0)));
					bm.jsonPath("$.duck", bm.byCommand("assertThatValueIsANumber($it)"));
					bm.jsonPath("$.['key'].['complex.key']", bm.byEquality());
					bm.jsonPath("$.nullValue", bm.byNull());
				});
				res.headers((h) -> {
					h.contentType(h.applicationJson());
					h.header("Some-Header", res.$(res.c("someValue"), res.p(res.regex("[a-zA-Z]{9}"))));
				});
			});
		});
	}

	static Stream<Arguments> dynamicValuesViaMatchersFrameworks() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null, "\\$"),
				Arguments.of("testng", TestFramework.TESTNG, null, "$"),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC, "$"),
				Arguments.of("jaxrs-spock", TestFramework.SPOCK, TestMode.JAXRSCLIENT, "\\$"),
				Arguments.of("jaxrs", TestFramework.JUNIT5, TestMode.JAXRSCLIENT, "$"),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT, "$"));
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("dynamicValuesViaMatchersFrameworks")
	void should_allow_to_set_dynamic_values_via_stub_test_matchers(String methodBuilderName,
			TestFramework testFramework, TestMode testMode, String rootElement) {
		if (testFramework != null) {
			properties.setTestFramework(testFramework);
		}
		if (testMode != null) {
			properties.setTestMode(testMode);
		}

		String test = singleTestGenerator(dynamicValueContract());

		assertThat(test)
			.contains("assertThat(parsedJson.read(\"" + rootElement + ".duck\", String.class)).matches(\"[0-9]{3}\")");
		assertThat(test)
			.contains("assertThat(parsedJson.read(\"" + rootElement + ".duck\", Integer.class)).isEqualTo(123)");
		assertThat(test).contains(
				"assertThat(parsedJson.read(\"" + rootElement + ".alpha\", String.class)).matches(\"[\\\\p{L}]*\")");
		assertThat(test)
			.contains("assertThat(parsedJson.read(\"" + rootElement + ".alpha\", String.class)).isEqualTo(\"abc\")");
		assertThat(test).contains("assertThat(parsedJson.read(\"" + rootElement
				+ ".number\", String.class)).matches(\"-?(\\\\d*\\\\.\\\\d+|\\\\d+)\")");
		assertThat(test).contains("assertThat(parsedJson.read(\"" + rootElement
				+ ".positiveInteger\", String.class)).matches(\"-?(\\\\d+)\")");
		assertThat(test).contains("assertThat(parsedJson.read(\"" + rootElement
				+ ".negativeInteger\", String.class)).matches(\"-?(\\\\d+)\")");
		assertThat(test).contains("assertThat(parsedJson.read(\"" + rootElement
				+ ".positiveDecimalNumber\", String.class)).matches(\"-?(\\\\d*\\\\.\\\\d+)\")");
		assertThat(test).contains("assertThat(parsedJson.read(\"" + rootElement
				+ ".negativeDecimalNumber\", String.class)).matches(\"-?(\\\\d*\\\\.\\\\d+)\")");
		assertThat(test).contains("assertThat(parsedJson.read(\"" + rootElement
				+ ".aBoolean\", String.class)).matches(\"(true|false)\")");
		assertThat(test).contains("assertThat(parsedJson.read(\"" + rootElement
				+ ".date\", String.class)).matches(\"(\\\\d\\\\d\\\\d\\\\d)-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])\")");
		assertThat(test).contains("assertThat(parsedJson.read(\"" + rootElement
				+ ".dateTime\", String.class)).matches(\"([0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])\")");
		assertThat(test).contains("assertThat(parsedJson.read(\"" + rootElement
				+ ".time\", String.class)).matches(\"(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])\")");
		assertThat(test).contains("assertThat((Object) parsedJson.read(\"" + rootElement
				+ ".valueWithTypeMatch\")).isInstanceOf(java.lang.String.class)");
		assertThat(test).contains("assertThat((Object) parsedJson.read(\"" + rootElement
				+ ".valueWithMin\")).isInstanceOf(java.util.List.class)");
		assertThat(test).contains("assertThat((java.lang.Iterable) parsedJson.read(\"" + rootElement
				+ ".valueWithMin\", java.util.Collection.class)).as(\"" + rootElement
				+ ".valueWithMin\").hasSizeGreaterThanOrEqualTo(1)");
		assertThat(test).contains("assertThat((Object) parsedJson.read(\"" + rootElement
				+ ".valueWithMax\")).isInstanceOf(java.util.List.class)");
		assertThat(test).contains("assertThat((java.lang.Iterable) parsedJson.read(\"" + rootElement
				+ ".valueWithMax\", java.util.Collection.class)).as(\"" + rootElement
				+ ".valueWithMax\").hasSizeLessThanOrEqualTo(3)");
		assertThat(test).contains("assertThat((Object) parsedJson.read(\"" + rootElement
				+ ".valueWithMinMax\")).isInstanceOf(java.util.List.class)");
		assertThat(test).contains("assertThat((java.lang.Iterable) parsedJson.read(\"" + rootElement
				+ ".valueWithMinMax\", java.util.Collection.class)).as(\"" + rootElement
				+ ".valueWithMinMax\").hasSizeBetween(1, 3)");
		assertThat(test).contains("assertThat((Object) parsedJson.read(\"" + rootElement
				+ ".valueWithMinEmpty\")).isInstanceOf(java.util.List.class)");
		assertThat(test).contains("assertThat((java.lang.Iterable) parsedJson.read(\"" + rootElement
				+ ".valueWithMinEmpty\", java.util.Collection.class)).as(\"" + rootElement
				+ ".valueWithMinEmpty\").hasSizeGreaterThanOrEqualTo(0)");
		assertThat(test).contains("assertThat((Object) parsedJson.read(\"" + rootElement
				+ ".valueWithMaxEmpty\")).isInstanceOf(java.util.List.class)");
		assertThat(test).contains("assertThat((java.lang.Iterable) parsedJson.read(\"" + rootElement
				+ ".valueWithMaxEmpty\", java.util.Collection.class)).as(\"" + rootElement
				+ ".valueWithMaxEmpty\").hasSizeLessThanOrEqualTo(0)");
		assertThat(test).contains("assertThatValueIsANumber(parsedJson.read(\"" + rootElement + ".duck\")");
		assertThat(test).contains("assertThat(parsedJson.read(\"" + rootElement
				+ ".['key'].['complex.key']\", String.class)).isEqualTo(\"foo\")");
		assertThat(test).contains("assertThat((Object) parsedJson.read(\"" + rootElement + ".nullValue\")).isNull()");
		assertThat(test).doesNotContain("cursor");

		try {
			SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		}
		catch (Exception classFormatError) {
			String output = classFormatError.getMessage();
			assertThat(output).contains("cannot find symbol");
			assertThat(output).contains("assertThatValueIsANumber");
		}
	}

	// ---------------------------------------------------------------------------
	// #217: should allow complex matchers
	// ---------------------------------------------------------------------------

	private static Contract complexMatchersContract() {
		return Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("person");
			});
			c.response((res) -> {
				res.status(res.OK());
				res.body(Map.of("firstName", "Jane", "lastName", "Doe", "isAlive", true, "address",
						Map.of("postalCode", "98101"), "phoneNumbers",
						List.of(Map.of("type", "home", "number", "999 999-9999")), "gender", Map.of("type", "female"),
						"children", List.of(Map.of("firstName", "Kid", "age", 55))));
				res.bodyMatchers((bm) -> {
					bm.jsonPath("$.phoneNumbers", bm.byType((t) -> {
						t.minOccurrence(0);
						t.maxOccurrence(4);
					}));
					bm.jsonPath("$.phoneNumbers[*].number", bm.byRegex("^[0-9]{3} [0-9]{3}-[0-9]{4}$"));
					bm.jsonPath("$..number", bm.byRegex("^[0-9]{3} [0-9]{3}-[0-9]{4}$"));
				});
				res.headers((h) -> h.contentType("application/json"));
			});
		});
	}

	static Stream<Arguments> complexMatchersFrameworks() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null, "\\$"),
				Arguments.of("testng", TestFramework.TESTNG, null, "$"),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC, "$"),
				Arguments.of("jaxrs-spock", TestFramework.SPOCK, TestMode.JAXRSCLIENT, "\\$"),
				Arguments.of("jaxrs", TestFramework.JUNIT5, TestMode.JAXRSCLIENT, "$"),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT, "$"));
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("complexMatchersFrameworks")
	void should_allow_complex_matchers(String methodBuilderName, TestFramework testFramework, TestMode testMode,
			String rootElement) {
		if (testFramework != null) {
			properties.setTestFramework(testFramework);
		}
		if (testMode != null) {
			properties.setTestMode(testMode);
		}

		String test = singleTestGenerator(complexMatchersContract());

		assertThat(test).contains("assertThat((java.lang.Iterable) parsedJson.read(\"" + rootElement
				+ ".phoneNumbers[*].number\", java.util.Collection.class)).as(\"" + rootElement
				+ ".phoneNumbers[*].number\").allElementsMatch(\"^[0-9]{3} [0-9]{3}-[0-9]{4}" + rootElement + "\")");
		assertThat(test).contains("assertThat((java.lang.Iterable) parsedJson.read(\"" + rootElement
				+ "..number\", java.util.Collection.class)).as(\"" + rootElement
				+ "..number\").allElementsMatch(\"^[0-9]{3} [0-9]{3}-[0-9]{4}" + rootElement + "\")");
		assertThat(test).doesNotContain("cursor");

		try {
			SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		}
		catch (NoClassDefFoundError error) {
			// expected since we're creating an anonymous class
		}
	}

	// ---------------------------------------------------------------------------
	// #217: should use the flattened assertions when jsonpath contains [*]
	// ---------------------------------------------------------------------------

	private static Contract flattenedAssertionsContract() {
		return Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("person");
			});
			c.response((res) -> {
				res.status(res.OK());
				res.body(Map.of("phoneNumbers", Map.of("number", "foo")));
				res.bodyMatchers((bm) -> {
					bm.jsonPath("$.phoneNumbers[*].number", bm.byType((t) -> {
						t.minOccurrence(0);
						t.maxOccurrence(4);
					}));
					bm.jsonPath("$.phoneNumbers[*].number", bm.byType((t) -> t.minOccurrence(0)));
					bm.jsonPath("$.phoneNumbers[*].number", bm.byType((t) -> t.maxOccurrence(4)));
				});
			});
		});
	}

	static Stream<Arguments> flattenedAssertionsFrameworks() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null, "\\$"),
				Arguments.of("testng", TestFramework.TESTNG, null, "$"),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC, "$"),
				Arguments.of("jaxrs-spock", TestFramework.SPOCK, TestMode.JAXRSCLIENT, "\\$"),
				Arguments.of("jaxrs", TestFramework.JUNIT5, TestMode.JAXRSCLIENT, "$"),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT, "$"));
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("flattenedAssertionsFrameworks")
	void should_use_the_flattened_assertions_when_jsonpath_contains_wildcard(String methodBuilderName,
			TestFramework testFramework, TestMode testMode, String rootElement) {
		if (testFramework != null) {
			properties.setTestFramework(testFramework);
		}
		if (testMode != null) {
			properties.setTestMode(testMode);
		}

		String test = singleTestGenerator(flattenedAssertionsContract());

		assertThat(test).contains("assertThat((java.lang.Iterable) parsedJson.read(\"" + rootElement
				+ ".phoneNumbers[*].number\", java.util.Collection.class)).as(\"" + rootElement
				+ ".phoneNumbers[*].number\").hasFlattenedSizeBetween(0, 4)");
		assertThat(test).contains("assertThat((java.lang.Iterable) parsedJson.read(\"" + rootElement
				+ ".phoneNumbers[*].number\", java.util.Collection.class)).as(\"" + rootElement
				+ ".phoneNumbers[*].number\").hasFlattenedSizeGreaterThanOrEqualTo(0)");
		assertThat(test).contains("assertThat((java.lang.Iterable) parsedJson.read(\"" + rootElement
				+ ".phoneNumbers[*].number\", java.util.Collection.class)).as(\"" + rootElement
				+ ".phoneNumbers[*].number\").hasFlattenedSizeLessThanOrEqualTo(4)");
		assertThat(test).doesNotContain("cursor");

		try {
			SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);
		}
		catch (NoClassDefFoundError error) {
			// expected since we're creating an anonymous class
		}
	}

	// ---------------------------------------------------------------------------
	// #217: should allow matcher with command to execute
	// ---------------------------------------------------------------------------

	private static Contract commandMatcherContract() {
		return Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("person");
			});
			c.response((res) -> {
				res.status(res.OK());
				res.body(Map.of("phoneNumbers", Map.of("number", "foo")));
				res.bodyMatchers((bm) -> bm.jsonPath("$.phoneNumbers[*].number", bm.byCommand("foo($it)")));
			});
		});
	}

	static Stream<Arguments> commandMatcherFrameworks() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null, "\\$"),
				Arguments.of("testng", TestFramework.TESTNG, null, "$"),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC, "$"),
				Arguments.of("jaxrs-spock", TestFramework.SPOCK, TestMode.JAXRSCLIENT, "\\$"),
				Arguments.of("jaxrs", TestFramework.JUNIT5, TestMode.JAXRSCLIENT, "$"),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT, "$"));
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("commandMatcherFrameworks")
	void should_allow_matcher_with_command_to_execute(String methodBuilderName, TestFramework testFramework,
			TestMode testMode, String rootElement) {
		if (testFramework != null) {
			properties.setTestFramework(testFramework);
		}
		if (testMode != null) {
			properties.setTestMode(testMode);
		}

		String test = singleTestGenerator(commandMatcherContract());

		assertThat(test).contains("foo(parsedJson.read(\"" + rootElement + ".phoneNumbers[*].number\")");
	}

	// ---------------------------------------------------------------------------
	// #217: should throw exception when command references a non-existing entry
	// ---------------------------------------------------------------------------

	private static Contract nonExistingJsonPathContract() {
		return Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("person");
			});
			c.response((res) -> {
				res.status(res.OK());
				res.body(Map.of("phoneNumbers", Map.of("number", "foo")));
				res.bodyMatchers((bm) -> bm.jsonPath("$.nonExistingPhoneNumbers[*].number", bm.byCommand("foo($it)")));
			});
		});
	}

	static Stream<Arguments> nonExistingJsonPathFrameworks() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null, "\\$"),
				Arguments.of("testng", TestFramework.TESTNG, null, "$"),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC, "$"),
				Arguments.of("jaxrs-spock", TestFramework.SPOCK, TestMode.JAXRSCLIENT, "\\$"),
				Arguments.of("jaxrs", TestFramework.JUNIT5, TestMode.JAXRSCLIENT, "$"),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT, "$"));
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("nonExistingJsonPathFrameworks")
	void should_throw_an_exception_when_command_references_a_non_existing_entry(String methodBuilderName,
			TestFramework testFramework, TestMode testMode, String rootElement) {
		if (testFramework != null) {
			properties.setTestFramework(testFramework);
		}
		if (testMode != null) {
			properties.setTestMode(testMode);
		}

		assertThatThrownBy(() -> singleTestGenerator(nonExistingJsonPathContract()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining(
					"Entry for the provided JSON path <$.nonExistingPhoneNumbers[*].number> doesn't exist in the body");
	}

	// ---------------------------------------------------------------------------
	// #229: should work for matchers and body with json array
	// ---------------------------------------------------------------------------

	private static Contract jsonArrayContract() {
		return Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("/api/v1/xxxx");
				r.body(12000);
			});
			c.response((res) -> {
				res.status(res.OK());
				res.body(List.of(List.of(Map.of("access_token", "123"))));
				res.headers((h) -> h.contentType(h.applicationJson()));
				res.bodyMatchers((bm) -> bm.jsonPath("$[0][0].access_token", bm.byEquality()));
			});
		});
	}

	static Stream<Arguments> jsonArrayFrameworks() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null, "\\$"),
				Arguments.of("testng", TestFramework.TESTNG, null, "$"),
				Arguments.of("mockmvc", null, TestMode.MOCKMVC, "$"),
				Arguments.of("jaxrs-spock", TestFramework.SPOCK, TestMode.JAXRSCLIENT, "\\$"),
				Arguments.of("jaxrs", TestFramework.JUNIT5, TestMode.JAXRSCLIENT, "$"),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT, "$"));
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("jsonArrayFrameworks")
	void should_work_for_matchers_and_body_with_json_array(String methodBuilderName, TestFramework testFramework,
			TestMode testMode, String rootElement) throws Exception {
		if (testFramework != null) {
			properties.setTestFramework(testFramework);
		}
		if (testMode != null) {
			properties.setTestMode(testMode);
		}

		String test = singleTestGenerator(jsonArrayContract());
		SyntaxChecker.tryToCompile(methodBuilderName, test);

		assertThat(test).contains("assertThat(parsedJson.read(\"" + rootElement
				+ "[0][0].access_token\", String.class)).isEqualTo(\"123\")");
	}

	// ---------------------------------------------------------------------------
	// #391: should work for matchers and body with multiline string
	// ---------------------------------------------------------------------------

	private static Contract multilineStringContract() {
		return Contract.make((c) -> {
			c.name("ISSUE 391");
			c.request((r) -> {
				r.method("GET");
				r.urlPath("/item/factsheet?size=2&page=1");
				r.headers((h) -> h.header("accept", "application/...json"));
			});
			c.response((res) -> {
				res.status(res.OK());
				res.body(
						"{\n\t\"items\": [\n\t\t{\n\t\t\t\"id\": \"35309\",\n\t\t\t\"title\": \"lorem ipsum\"\n\t\t}\n\t]\n}");
				res.bodyMatchers((bm) -> {
					bm.jsonPath("$.items[*].id", bm.byRegex(RegexPatterns.nonBlank()));
					bm.jsonPath("$.items[*].title", bm.byRegex(RegexPatterns.nonBlank()));
					bm.jsonPath("$.items[*]", bm.byType((t) -> t.occurrence(2)));
				});
				res.headers((h) -> h.header("content-type", "application/...json;charset=UTF-8"));
			});
		});
	}

	static Stream<Arguments> multilineStringFrameworks() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("testng", TestFramework.TESTNG, null), Arguments.of("mockmvc", null, TestMode.MOCKMVC),
				Arguments.of("jaxrs-spock", TestFramework.SPOCK, TestMode.JAXRSCLIENT),
				Arguments.of("jaxrs", TestFramework.JUNIT5, TestMode.JAXRSCLIENT),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT));
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("multilineStringFrameworks")
	void should_work_for_matchers_and_body_with_multiline_string(String methodBuilderName, TestFramework testFramework,
			TestMode testMode) throws Exception {
		if (testFramework != null) {
			properties.setTestFramework(testFramework);
		}
		if (testMode != null) {
			properties.setTestMode(testMode);
		}

		String test = singleTestGenerator(multilineStringContract());
		SyntaxChecker.tryToCompile(methodBuilderName, test);

		assertThat(test).doesNotContain("assertThatJson(parsedJson).array(\"['items']\").isEmpty()");
	}

	// ---------------------------------------------------------------------------
	// #391: should work for matchers and body with multiline string with map body
	// ---------------------------------------------------------------------------

	private static Contract multilineStringMapBodyContract() {
		return Contract.make((c) -> {
			c.name("ISSUE 391");
			c.request((r) -> {
				r.method("GET");
				r.urlPath("/item/factsheet?size=2&page=1");
				r.headers((h) -> h.header("accept", "application/...json"));
			});
			c.response((res) -> {
				res.status(res.OK());
				res.body(Map.of("items", Map.of("id", "35309", "title", "lorem ipsum")));
				res.bodyMatchers((bm) -> {
					bm.jsonPath("$.items[*].id", bm.byRegex(RegexPatterns.nonBlank()));
					bm.jsonPath("$.items[*].title", bm.byRegex(RegexPatterns.nonBlank()));
					bm.jsonPath("$.items[*]", bm.byType((t) -> {
						t.minOccurrence(2);
						t.maxOccurrence(2);
					}));
				});
				res.headers((h) -> h.header("content-type", "application/...json;charset=UTF-8"));
			});
		});
	}

	static Stream<Arguments> multilineStringMapBodyFrameworks() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("testng", TestFramework.TESTNG, null), Arguments.of("mockmvc", null, TestMode.MOCKMVC),
				Arguments.of("jaxrs-spock", TestFramework.SPOCK, TestMode.JAXRSCLIENT),
				Arguments.of("jaxrs", TestFramework.JUNIT5, TestMode.JAXRSCLIENT),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT));
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("multilineStringMapBodyFrameworks")
	void should_work_for_matchers_and_body_with_multiline_string_with_map_body(String methodBuilderName,
			TestFramework testFramework, TestMode testMode) throws Exception {
		if (testFramework != null) {
			properties.setTestFramework(testFramework);
		}
		if (testMode != null) {
			properties.setTestMode(testMode);
		}

		String test = singleTestGenerator(multilineStringMapBodyContract());
		SyntaxChecker.tryToCompile(methodBuilderName, test);

		assertThat(test).doesNotContain("assertThatJson(parsedJson).array(\"['items']\").isEmpty()");
	}

	// ---------------------------------------------------------------------------
	// #1091: should work for map with array value where matchers cover all array fields
	// ---------------------------------------------------------------------------

	private static Contract pricesArrayContract() {
		return Contract.make((c) -> {
			c.name("ISSUE 1091");
			c.request((r) -> {
				r.method("GET");
				r.url("/test");
				r.headers((h) -> h.contentType(h.applicationJson()));
			});
			c.response((res) -> {
				res.status(res.OK());
				res.body("{\n" + "\"prices\": [\n" + "  {\n" + "    \"country\": \"ES\",\n"
						+ "    \"originalPrice\": \"1500\"\n" + "  }\n" + "]\n" + "}");
				res.bodyMatchers((bm) -> {
					bm.jsonPath("$.prices[0].country", bm.byRegex(RegexPatterns.nonBlank()));
					bm.jsonPath("$.prices[0].originalPrice", bm.byRegex(RegexPatterns.number()));
				});
				res.headers((h) -> h.contentType(h.applicationJsonUtf8()));
			});
		});
	}

	static Stream<Arguments> pricesArrayFrameworks() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("testng", TestFramework.TESTNG, null), Arguments.of("mockmvc", null, TestMode.MOCKMVC),
				Arguments.of("jaxrs-spock", TestFramework.SPOCK, TestMode.JAXRSCLIENT),
				Arguments.of("jaxrs", TestFramework.JUNIT5, TestMode.JAXRSCLIENT),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT));
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("pricesArrayFrameworks")
	void should_work_for_map_with_array_value_where_matchers_cover_all_array_fields(String methodBuilderName,
			TestFramework testFramework, TestMode testMode) throws Exception {
		if (testFramework != null) {
			properties.setTestFramework(testFramework);
		}
		if (testMode != null) {
			properties.setTestMode(testMode);
		}

		String test = singleTestGenerator(pricesArrayContract());
		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);

		assertThat(test).doesNotContain("isEmpty()");
	}

	// ---------------------------------------------------------------------------
	// #1091: should work for array containing map with array value
	// ---------------------------------------------------------------------------

	private static Contract nestedArrayWithPricesContract() {
		return Contract.make((c) -> {
			c.name("ISSUE 1091");
			c.request((r) -> {
				r.method("GET");
				r.url("/test");
				r.headers((h) -> h.contentType(h.applicationJson()));
			});
			c.response((res) -> {
				res.status(res.OK());
				res.body("{\n" + "\"test\": [\n" + "  {\n" + "    \"prices\": [\n" + "      {\n"
						+ "        \"country\": \"ES\",\n" + "        \"originalPrice\": 1500\n" + "      }\n"
						+ "    ]\n" + "  }\n" + "]\n" + "}");
				res.bodyMatchers((bm) -> {
					bm.jsonPath("$.test[0].barcode", bm.byRegex(RegexPatterns.nonBlank()));
					bm.jsonPath("$.test[0].id", bm.byRegex(RegexPatterns.nonBlank()));
					bm.jsonPath("$.test[0].prices[0].country", bm.byRegex(RegexPatterns.nonBlank()));
					bm.jsonPath("$.test[0].prices[0].originalPrice", bm.byRegex(RegexPatterns.nonBlank()));
					bm.jsonPath("$.test[0].prices[?(@.originalPrice==1500)].originalPrice",
							bm.byRegex(RegexPatterns.nonBlank()));
				});
				res.headers((h) -> h.contentType(h.applicationJsonUtf8()));
			});
		});
	}

	static Stream<Arguments> nestedArrayWithPricesFrameworks() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("testng", TestFramework.TESTNG, null), Arguments.of("mockmvc", null, TestMode.MOCKMVC),
				Arguments.of("jaxrs-spock", TestFramework.SPOCK, TestMode.JAXRSCLIENT),
				Arguments.of("jaxrs", TestFramework.JUNIT5, TestMode.JAXRSCLIENT),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT));
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("nestedArrayWithPricesFrameworks")
	void should_work_for_array_containing_map_with_array_value_where_matchers_cover_all_array_fields(
			String methodBuilderName, TestFramework testFramework, TestMode testMode) throws Exception {
		if (testFramework != null) {
			properties.setTestFramework(testFramework);
		}
		if (testMode != null) {
			properties.setTestMode(testMode);
		}

		String test = singleTestGenerator(nestedArrayWithPricesContract());
		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);

		assertThat(test).doesNotContain("isEmpty()");
	}

	// ---------------------------------------------------------------------------
	// #880: should not generate a null statement when there is no content type in the
	// response
	// ---------------------------------------------------------------------------

	private static Contract noContentTypeContract() {
		return Contract.make((c) -> {
			c.description("Should return 200");
			c.request((r) -> {
				r.method(r.POST());
				r.url("/get");
				r.headers((h) -> h.contentType("application/json;charset=UTF-8"));
			});
			c.response((res) -> {
				res.status(res.OK());
				res.body(res.file("getBody.json"));
			});
		});
	}

	static Stream<Arguments> noContentTypeFrameworks() {
		return Stream.of(Arguments.of("spock", TestFramework.SPOCK, null),
				Arguments.of("testng", TestFramework.TESTNG, null), Arguments.of("mockmvc", null, TestMode.MOCKMVC),
				Arguments.of("jaxrs-spock", TestFramework.SPOCK, TestMode.JAXRSCLIENT),
				Arguments.of("jaxrs", TestFramework.JUNIT5, TestMode.JAXRSCLIENT),
				Arguments.of("webclient", null, TestMode.WEBTESTCLIENT));
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("noContentTypeFrameworks")
	void should_not_generate_a_null_statement_when_there_is_no_content_type_in_the_response(String methodBuilderName,
			TestFramework testFramework, TestMode testMode) throws Exception {
		if (testFramework != null) {
			properties.setTestFramework(testFramework);
		}
		if (testMode != null) {
			properties.setTestMode(testMode);
		}

		String test = singleTestGenerator(noContentTypeContract());
		SyntaxChecker.tryToCompileWithoutCompileStatic(methodBuilderName, test);

		assertThat(test).doesNotContain("null");
		assertThat(test).contains(".array(\"['array']\")");
	}

}
