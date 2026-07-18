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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.RegexPatterns;
import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.config.TestMode;
import sh.stubborn.contract.verifier.file.ContractMetadata;
import sh.stubborn.contract.verifier.util.SyntaxChecker;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests used for the documentation.
 *
 * @author Marcin Grzejszczak
 */
class ContractHttpDocsTests {

	private ContractVerifierConfigProperties properties;

	private final SingleTestGenerator.GeneratedClassData generatedClassData = new SingleTestGenerator.GeneratedClassData(
			"foo", "com.example", new File(".").toPath());

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

	// tag::optionals[]
	private static final Contract OPTIONALS = Contract.make((c) -> {
		c.priority(1);
		c.name("optionals");
		c.request((r) -> {
			r.method("POST");
			r.url("/users/password");
			r.headers((h) -> h.contentType(h.applicationJson()));
			LinkedHashMap<String, Object> body = new LinkedHashMap<>();
			body.put("email", r.$(r.consumer(r.optional(r.regex(RegexPatterns.email()))), r.producer("abc@abc.com")));
			body.put("callback_url",
					r.$(r.consumer(r.regex(RegexPatterns.hostname())), r.producer("https://partners.com")));
			r.body(body);
		});
		c.response((r) -> {
			r.status(404);
			r.headers((h) -> h.header("Content-Type", "application/json"));
			r.body(Map.of("code", r.value(r.consumer("123123"), r.producer(r.optional("123123")))));
		});
	});

	// end::optionals[]

	@Test
	void should_convert_dsl_with_optionals_to_proper_spock_test() {
		properties.setTestFramework(TestFramework.SPOCK);
		String test = singleTestGenerator(OPTIONALS);

		String expectedTest = """
				// tag::optionals_test[]
				package com.example

				import com.jayway.jsonpath.DocumentContext
				import com.jayway.jsonpath.JsonPath
				import spock.lang.Specification
				import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification
				import io.restassured.response.ResponseOptions

				import static sh.stubborn.contract.verifier.assertion.SpringCloudContractAssertions.assertThat
				import static sh.stubborn.contract.verifier.util.ContractVerifierUtil.*
				import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson
				import static io.restassured.module.mockmvc.RestAssuredMockMvc.*

				@SuppressWarnings("rawtypes")
				class FooSpec extends Specification {

				\tdef validate_optionals() throws Exception {
				\t\tgiven:
				\t\t\tMockMvcRequestSpecification request = given()
				\t\t\t\t\t.header("Content-Type", "application/json")
				\t\t\t\t\t.body('''{"email":"abc@abc.com","callback_url":"https://partners.com"}''')

				\t\twhen:
				\t\t\tResponseOptions response = given().spec(request)
				\t\t\t\t\t.post("/users/password")

				\t\tthen:
				\t\t\tresponse.statusCode() == 404
				\t\t\tresponse.header("Content-Type") == 'application/json'

				\t\tand:
				\t\t\tDocumentContext parsedJson = JsonPath.parse(response.body.asString())
				\t\t\tassertThatJson(parsedJson).field("['code']").matches("(123123)?")
				\t}

				}
				// end::optionals_test[]
				""";

		String normalizedExpected = expectedTest.trim()
			.replace("  ", "\t")
			.replace("\\t", "\t")
			.replace("// tag::optionals_test[]\n", "")
			.replace("\n// end::optionals_test[]", "");

		assertThat(test.trim().replace("\\t", "\t")).isEqualTo(normalizedExpected);
		SyntaxChecker.tryToCompile("spock", test);
	}

}
