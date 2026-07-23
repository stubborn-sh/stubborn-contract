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
import java.util.List;

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.file.ContractMetadata;
import sh.stubborn.contract.verifier.util.ContractVerifierDslConverter;
import sh.stubborn.contract.verifier.util.SyntaxChecker;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marcin Grzejszczak
 * @author Tim Ysewyn
 */
class MessagingMethodBodyBuilderTests {

	ContractVerifierConfigProperties properties;

	SingleTestGenerator.GeneratedClassData generatedClassData;

	@BeforeEach
	void setup() throws URISyntaxException {
		properties = new ContractVerifierConfigProperties();
		properties.setAssertJsonSize(true);
		File resourceDir = new File(MessagingMethodBodyBuilderTests.class.getResource(".").toURI());
		properties.setGeneratedTestSourcesDir(resourceDir);
		properties.setGeneratedTestResourcesDir(resourceDir);
		generatedClassData = new SingleTestGenerator.GeneratedClassData("foo", "com.example", resourceDir.toPath());
	}

	private String singleTestGenerator(Contract contractDsl) {
		return new JavaTestGenerator().buildClass(properties, List.of(contractMetadata(contractDsl)), "foo",
				generatedClassData);
	}

	private Contract contractFromGroovy(String groovyDsl) {
		return ContractVerifierDslConverter.convertAsCollection(new File("/"), groovyDsl).iterator().next();
	}

	private ContractMetadata contractMetadata(Contract contractDsl) {
		return new ContractMetadata(new File(".").toPath(), false, 0, null, contractDsl);
	}

	private String messageWithoutTags(String message, String tagName) {
		return message.trim()
			.replace("  ", "\t")
			.replace("\\t", "\t")
			.replace("// tag::" + tagName + "[]\n", "")
			.replace("\n// end::" + tagName + "[]", "");
	}

	@Test
	void should_work_for_triggered_based_messaging_with_Spock() {
		Contract contractDsl = contractFromGroovy("sh.stubborn.contract.spec.Contract.make {\n" + "\tname \"foo\"\n"
				+ "\tlabel 'some_label'\n" + "\tinput {\n" + "\t\ttriggeredBy('bookReturnedTriggered()')\n" + "\t}\n"
				+ "\toutputMessage {\n" + "\t\tsentTo('activemq:output')\n" + "\t\tbody('{ \"bookName\" : \"foo\" }')\n"
				+ "\t\theaders {\n" + "\t\t\theader('BOOK-NAME', 'foo')\n"
				+ "\t\t\tmessagingContentType(applicationJson())\n" + "\t\t}\n" + "\t}\n" + "}");
		properties.setTestFramework(TestFramework.SPOCK);

		String test = singleTestGenerator(contractDsl);

		String expectedMessage = "// tag::trigger_method_test[]\n" + "package com.example\n" + "\n"
				+ "import com.jayway.jsonpath.DocumentContext\n" + "import com.jayway.jsonpath.JsonPath\n"
				+ "import spock.lang.Specification\n"
				+ "import org.springframework.beans.factory.annotation.Autowired\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierObjectMapper\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessage\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging\n" + "\n"
				+ "import static sh.stubborn.contract.verifier.assertion.SpringCloudContractAssertions.assertThat\n"
				+ "import static sh.stubborn.contract.verifier.util.ContractVerifierUtil.*\n"
				+ "import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson\n"
				+ "import static sh.stubborn.contract.verifier.messaging.util.ContractVerifierMessagingUtil.headers\n"
				+ "import static sh.stubborn.contract.verifier.util.ContractVerifierUtil.fileToBytes\n" + "\n"
				+ "@SuppressWarnings(\"rawtypes\")\n" + "class FooSpec extends Specification {\n"
				+ "\t@Autowired ContractVerifierMessaging contractVerifierMessaging\n"
				+ "\t@Autowired ContractVerifierObjectMapper contractVerifierObjectMapper\n" + "\n"
				+ "\tdef validate_foo() throws Exception {\n" + "\t\twhen:\n" + "\t\t\tbookReturnedTriggered()\n" + "\n"
				+ "\t\tthen:\n"
				+ "\t\t\tContractVerifierMessage response = contractVerifierMessaging.receive(\"activemq:output\",\n"
				+ "\t\t\t\t\tcontract(this, \"foo.yml\"))\n" + "\t\t\tresponse != null\n" + "\n" + "\t\tand:\n"
				+ "\t\t\tresponse.getHeader(\"BOOK-NAME\") != null\n"
				+ "\t\t\tresponse.getHeader(\"BOOK-NAME\").toString() == 'foo'\n"
				+ "\t\t\tresponse.getHeader(\"contentType\") != null\n"
				+ "\t\t\tresponse.getHeader(\"contentType\").toString() == 'application/json'\n" + "\n" + "\t\tand:\n"
				+ "\t\t\tDocumentContext parsedJson = JsonPath.parse(contractVerifierObjectMapper.writeValueAsString(response.getPayload()))\n"
				+ "\t\t\tassertThatJson(parsedJson).field(\"['bookName']\").isEqualTo(\"foo\")\n" + "\t}\n" + "\n"
				+ "}\n" + "// end::trigger_method_test[]";
		assertThat(test.trim()).isEqualTo(messageWithoutTags(expectedMessage, "trigger_method_test"));
	}

	@Test
	void should_work_for_triggered_based_messaging_with_JUnit() {
		Contract contractDsl = contractFromGroovy("sh.stubborn.contract.spec.Contract.make {\n" + "\tname \"foo\"\n"
				+ "\tlabel 'some_label'\n" + "\tinput {\n" + "\t\ttriggeredBy('bookReturnedTriggered()')\n" + "\t}\n"
				+ "\toutputMessage {\n" + "\t\tsentTo('activemq:output')\n" + "\t\tbody('{ \"bookName\" : \"foo\" }')\n"
				+ "\t\theaders {\n" + "\t\t\theader('BOOK-NAME', 'foo')\n"
				+ "\t\t\tmessagingContentType(applicationJson())\n" + "\t\t}\n" + "\t}\n" + "}");
		properties.setTestFramework(TestFramework.JUNIT5);

		String test = singleTestGenerator(contractDsl);

		String expectedMessage = "// tag::trigger_method_junit_test[]\n" + "package com.example;\n" + "\n"
				+ "import com.jayway.jsonpath.DocumentContext;\n" + "import com.jayway.jsonpath.JsonPath;\n"
				+ "import org.junit.jupiter.api.Test;\n" + "import org.junit.jupiter.api.extension.ExtendWith;\n"
				+ "import org.springframework.beans.factory.annotation.Autowired;\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierObjectMapper;\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessage;\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;\n" + "\n"
				+ "import static sh.stubborn.contract.verifier.assertion.SpringCloudContractAssertions.assertThat;\n"
				+ "import static sh.stubborn.contract.verifier.util.ContractVerifierUtil.*;\n"
				+ "import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson;\n"
				+ "import static sh.stubborn.contract.verifier.messaging.util.ContractVerifierMessagingUtil.headers;\n"
				+ "import static sh.stubborn.contract.verifier.util.ContractVerifierUtil.fileToBytes;\n" + "\n"
				+ "@SuppressWarnings(\"rawtypes\")\n" + "public class FooTest {\n"
				+ "\t@Autowired ContractVerifierMessaging contractVerifierMessaging;\n"
				+ "\t@Autowired ContractVerifierObjectMapper contractVerifierObjectMapper;\n" + "\n" + "\t@Test\n"
				+ "\tpublic void validate_foo() throws Exception {\n" + "\t\t// when:\n"
				+ "\t\t\tbookReturnedTriggered();\n" + "\n" + "\t\t// then:\n"
				+ "\t\t\tContractVerifierMessage response = contractVerifierMessaging.receive(\"activemq:output\",\n"
				+ "\t\t\t\t\tcontract(this, \"foo.yml\"));\n" + "\t\t\tassertThat(response).isNotNull();\n" + "\n"
				+ "\t\t// and:\n" + "\t\t\tassertThat(response.getHeader(\"BOOK-NAME\")).isNotNull();\n"
				+ "\t\t\tassertThat(response.getHeader(\"BOOK-NAME\").toString()).isEqualTo(\"foo\");\n"
				+ "\t\t\tassertThat(response.getHeader(\"contentType\")).isNotNull();\n"
				+ "\t\t\tassertThat(response.getHeader(\"contentType\").toString()).isEqualTo(\"application/json\");\n"
				+ "\n" + "\t\t// and:\n"
				+ "\t\t\tDocumentContext parsedJson = JsonPath.parse(contractVerifierObjectMapper.writeValueAsString(response.getPayload()));\n"
				+ "\t\t\tassertThatJson(parsedJson).field(\"['bookName']\").isEqualTo(\"foo\");\n" + "\t}\n" + "\n"
				+ "}\n" + "// end::trigger_method_junit_test[]";
		assertThat(test.trim()).isEqualTo(messageWithoutTags(expectedMessage, "trigger_method_junit_test"));
	}

	@Test
	void should_generate_tests_with_message_headers_containing_regular_expression_for_JUnit() {
		Contract contractDsl = contractFromGroovy("sh.stubborn.contract.spec.Contract.make {\n" + "\tname \"foo\"\n"
				+ "\tlabel 'trigger_event'\n" + "\tinput {\n" + "\t\ttriggeredBy('requestIsCalled()')\n" + "\t}\n"
				+ "\toutputMessage {\n" + "\t\tsentTo 'topic.rateablequote'\n" + "\t\theaders {\n"
				+ "\t\t\theader('processId', value(producer(regex('[0-9]+')), consumer('123')))\n" + "\t\t}\n"
				+ "\t\tbody([\n" + "\t\t\teventId: value(producer(regex('[0-9]+')), consumer('1'))\n" + "\t\t])\n"
				+ "\t}\n" + "}");
		properties.setTestFramework(TestFramework.JUNIT5);

		String test = singleTestGenerator(contractDsl);

		String expectedMessage = "package com.example;\n" + "\n" + "import com.jayway.jsonpath.DocumentContext;\n"
				+ "import com.jayway.jsonpath.JsonPath;\n" + "import org.junit.jupiter.api.Test;\n"
				+ "import org.junit.jupiter.api.extension.ExtendWith;\n"
				+ "import org.springframework.beans.factory.annotation.Autowired;\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierObjectMapper;\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessage;\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;\n" + "\n"
				+ "import static sh.stubborn.contract.verifier.assertion.SpringCloudContractAssertions.assertThat;\n"
				+ "import static sh.stubborn.contract.verifier.util.ContractVerifierUtil.*;\n"
				+ "import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson;\n"
				+ "import static sh.stubborn.contract.verifier.messaging.util.ContractVerifierMessagingUtil.headers;\n"
				+ "import static sh.stubborn.contract.verifier.util.ContractVerifierUtil.fileToBytes;\n" + "\n"
				+ "@SuppressWarnings(\"rawtypes\")\n" + "public class FooTest {\n"
				+ "\t@Autowired ContractVerifierMessaging contractVerifierMessaging;\n"
				+ "\t@Autowired ContractVerifierObjectMapper contractVerifierObjectMapper;\n" + "\n" + "\t@Test\n"
				+ "\tpublic void validate_foo() throws Exception {\n" + "\t\t// when:\n" + "\t\t\trequestIsCalled();\n"
				+ "\n" + "\t\t// then:\n"
				+ "\t\t\tContractVerifierMessage response = contractVerifierMessaging.receive(\"topic.rateablequote\",\n"
				+ "\t\t\t\t\tcontract(this, \"foo.yml\"));\n" + "\t\t\tassertThat(response).isNotNull();\n" + "\n"
				+ "\t\t// and:\n" + "\t\t\tassertThat(response.getHeader(\"processId\")).isNotNull();\n"
				+ "\t\t\tassertThat(response.getHeader(\"processId\").toString()).matches(\"[0-9]+\");\n" + "\n"
				+ "\t\t// and:\n"
				+ "\t\t\tDocumentContext parsedJson = JsonPath.parse(contractVerifierObjectMapper.writeValueAsString(response.getPayload()));\n"
				+ "\t\t\tassertThatJson(parsedJson).field(\"['eventId']\").matches(\"[0-9]+\");\n" + "\t}\n" + "\n"
				+ "}\n";
		assertThat(test.trim()).isEqualTo(messageWithoutTags(expectedMessage, "expectedMsg"));
	}

	@Test
	void should_generate_tests_with_message_headers_containing_regular_expression_with_backslashes_for_JUnit() {
		Contract contractDsl = contractFromGroovy("sh.stubborn.contract.spec.Contract.make {\n" + "\tname \"foo\"\n"
				+ "\tlabel 'trigger_event'\n" + "\tinput {\n" + "\t\ttriggeredBy('requestIsCalled()')\n" + "\t}\n"
				+ "\toutputMessage {\n" + "\t\tsentTo 'topic.rateablequote'\n" + "\t\theaders {\n"
				+ "\t\t\theader('processId', value(producer(regex('\\\\d+')), consumer('123')))\n" + "\t\t}\n"
				+ "\t\tbody([\n" + "\t\t\teventId: value(producer(regex('\\\\d+')), consumer('1'))\n" + "\t\t])\n"
				+ "\t}\n" + "}");
		properties.setTestFramework(TestFramework.JUNIT5);

		String test = singleTestGenerator(contractDsl);

		assertThat(test).contains("assertThat(response.getHeader(\"processId\").toString()).matches(\"\\\\d+\");");
	}

	static Stream<Arguments> shouldAllowEasierWayOfProvidingDynamicValues() {
		return Stream.of(
				Arguments.of("spock",
						(Consumer<ContractVerifierConfigProperties>) (props) -> props
							.setTestFramework(TestFramework.SPOCK),
						"\\$"),
				Arguments.of("junit", (Consumer<ContractVerifierConfigProperties>) (props) -> props
					.setTestFramework(TestFramework.JUNIT5), "$"));
	}

	@ParameterizedTest
	@MethodSource("shouldAllowEasierWayOfProvidingDynamicValues")
	void should_allow_easier_way_of_providing_dynamic_values(String methodBuilderName,
			Consumer<ContractVerifierConfigProperties> methodBuilder, String endOfLineRegExSymbol) {
		Contract contractDsl = contractFromGroovy("sh.stubborn.contract.spec.Contract.make {\n" + "\tname \"foo\"\n"
				+ "\tlabel 'trigger_event'\n" + "\tinput {\n" + "\t\ttriggeredBy('toString()')\n" + "\t}\n"
				+ "\toutputMessage {\n" + "\t\tsentTo 'topic.rateablequote'\n" + "\t\tbody([\n"
				+ "\t\t\talpha            : $(anyAlphaUnicode()),\n" + "\t\t\tnumber           : $(anyNumber()),\n"
				+ "\t\t\tanInteger        : $(anyInteger()),\n" + "\t\t\tpositiveInt      : $(anyPositiveInt()),\n"
				+ "\t\t\taDouble          : $(anyDouble()),\n" + "\t\t\taBoolean         : $(aBoolean()),\n"
				+ "\t\t\tip               : $(anyIpAddress()),\n" + "\t\t\thostname         : $(anyHostname()),\n"
				+ "\t\t\temail            : $(anyEmail()),\n" + "\t\t\turl              : $(anyUrl()),\n"
				+ "\t\t\thttpsUrl         : $(anyHttpsUrl()),\n" + "\t\t\tuuid             : $(anyUuid()),\n"
				+ "\t\t\tdate             : $(anyDate()),\n" + "\t\t\tdateTime         : $(anyDateTime()),\n"
				+ "\t\t\ttime             : $(anyTime()),\n" + "\t\t\tiso8601WithOffset: $(anyIso8601WithOffset()),\n"
				+ "\t\t\tnonBlankString   : $(anyNonBlankString()),\n"
				+ "\t\t\tnonEmptyString   : $(anyNonEmptyString()),\n"
				+ "\t\t\tanyOf            : $(anyOf('foo', 'bar'))\n" + "\t\t])\n" + "\t}\n" + "}");
		methodBuilder.accept(properties);

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
		assertThat(test).contains(
				"assertThatJson(parsedJson).field(\"['ip']\").matches(\"([01]?\\\\d\\\\d?|2[0-4]\\\\d|25[0-5])\\\\.([01]?\\\\d\\\\d?|2[0-4]\\\\d|25[0-5])\\\\.([01]?\\\\d\\\\d?|2[0-4]\\\\d|25[0-5])\\\\.([01]?\\\\d\\\\d?|2[0-4]\\\\d|25[0-5])\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).field(\"['uuid']\").matches(\"[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).field(\"['date']\").matches(\"(\\\\d\\\\d\\\\d\\\\d)-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])");
		assertThat(test).contains(
				"assertThatJson(parsedJson).field(\"['dateTime']\").matches(\"([0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])");
		assertThat(test).contains(
				"assertThatJson(parsedJson).field(\"['time']\").matches(\"(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).field(\"['iso8601WithOffset']\").matches(\"([0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])(\\\\.\\\\d+)?(Z|[+-][01]\\\\d:[0-5]\\\\d)\")");
		assertThat(test).contains(
				"assertThatJson(parsedJson).field(\"['nonBlankString']\").matches(\"^\\\\s*\\\\S[\\\\S\\\\s]*\")");
		assertThat(test)
			.contains("assertThatJson(parsedJson).field(\"['nonEmptyString']\").matches(\"[\\\\S\\\\s]+\")");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['anyOf']\").matches(\"^foo"
				+ endOfLineRegExSymbol + "|^bar" + endOfLineRegExSymbol + "\")");
		assertThat(test).doesNotContain("cursor");
		assertThat(test).doesNotContain("REGEXP>>");
		SyntaxChecker.tryToCompile(methodBuilderName, test);
	}

	@Test
	void should_generate_tests_with_message_headers_containing_regular_expression_with_escapes_for_JUnit() {
		Contract contractDsl = contractFromGroovy("sh.stubborn.contract.spec.Contract.make {\n" + "\tname \"foo\"\n"
				+ "\tlabel 'trigger_event'\n" + "\tinput {\n" + "\t\ttriggeredBy('requestIsCalled()')\n" + "\t}\n"
				+ "\toutputMessage {\n" + "\t\tsentTo 'topic.rateablequote'\n" + "\t\theaders {\n"
				+ "\t\t\theader('processId', value(producer(regex(nonEmpty())), consumer('123')))\n" + "\t\t}\n"
				+ "\t\tbody([\n" + "\t\t\teventId: value(producer(regex(nonEmpty())), consumer('1'))\n" + "\t\t])\n"
				+ "\t}\n" + "}");
		properties.setTestFramework(TestFramework.JUNIT5);

		String test = singleTestGenerator(contractDsl);

		assertThat(test)
			.contains("ContractVerifierMessage response = contractVerifierMessaging.receive(\"topic.rateablequote\"");
		assertThat(test).contains("contract(this, \"foo.yml\"))");
		assertThat(test).contains("assertThat(response).isNotNull()");
		assertThat(test).contains("assertThat(response.getHeader(\"processId\")).isNotNull()");
		assertThat(test)
			.contains("assertThat(response.getHeader(\"processId\").toString()).matches(\"[\\\\S\\\\s]+\")");
		assertThat(test).contains(
				"DocumentContext parsedJson = JsonPath.parse(contractVerifierObjectMapper.writeValueAsString(response.getPayload()))");
		assertThat(test).contains("assertThatJson(parsedJson).field(\"['eventId']\").matches(\"[\\\\S\\\\s]+\")");
	}

	@Test
	void should_generate_tests_with_message_headers_containing_regular_expression_for_Spock() {
		Contract contractDsl = contractFromGroovy("sh.stubborn.contract.spec.Contract.make {\n" + "\tname \"foo\"\n"
				+ "\tlabel 'trigger_event'\n" + "\tinput {\n" + "\t\ttriggeredBy('requestIsCalled()')\n" + "\t}\n"
				+ "\toutputMessage {\n" + "\t\tsentTo 'topic.rateablequote'\n" + "\t\theaders {\n"
				+ "\t\t\theader('processId', value(producer(regex('[0-9]+')), consumer('123')))\n" + "\t\t}\n"
				+ "\t\tbody([\n" + "\t\t\teventId: value(producer(regex('[0-9]+')), consumer('1'))\n" + "\t\t])\n"
				+ "\t}\n" + "}");
		properties.setTestFramework(TestFramework.SPOCK);

		String test = singleTestGenerator(contractDsl);

		String expectedMessage = "package com.example\n" + "\n" + "import com.jayway.jsonpath.DocumentContext\n"
				+ "import com.jayway.jsonpath.JsonPath\n" + "import spock.lang.Specification\n"
				+ "import org.springframework.beans.factory.annotation.Autowired\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierObjectMapper\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessage\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging\n" + "\n"
				+ "import static sh.stubborn.contract.verifier.assertion.SpringCloudContractAssertions.assertThat\n"
				+ "import static sh.stubborn.contract.verifier.util.ContractVerifierUtil.*\n"
				+ "import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson\n"
				+ "import static sh.stubborn.contract.verifier.messaging.util.ContractVerifierMessagingUtil.headers\n"
				+ "import static sh.stubborn.contract.verifier.util.ContractVerifierUtil.fileToBytes\n" + "\n"
				+ "@SuppressWarnings(\"rawtypes\")\n" + "class FooSpec extends Specification {\n"
				+ "\t@Autowired ContractVerifierMessaging contractVerifierMessaging\n"
				+ "\t@Autowired ContractVerifierObjectMapper contractVerifierObjectMapper\n" + "\n"
				+ "\tdef validate_foo() throws Exception {\n" + "\t\twhen:\n" + "\t\t\trequestIsCalled()\n" + "\n"
				+ "\t\tthen:\n"
				+ "\t\t\tContractVerifierMessage response = contractVerifierMessaging.receive(\"topic.rateablequote\",\n"
				+ "\t\t\t\t\tcontract(this, \"foo.yml\"))\n" + "\t\t\tresponse != null\n" + "\n" + "\t\tand:\n"
				+ "\t\t\tresponse.getHeader(\"processId\") != null\n"
				+ "\t\t\tresponse.getHeader(\"processId\").toString() ==~ java.util.regex.Pattern.compile('[0-9]+')\n"
				+ "\n" + "\t\tand:\n"
				+ "\t\t\tDocumentContext parsedJson = JsonPath.parse(contractVerifierObjectMapper.writeValueAsString(response.getPayload()))\n"
				+ "\t\t\tassertThatJson(parsedJson).field(\"['eventId']\").matches(\"[0-9]+\")\n" + "\t}\n" + "\n"
				+ "}\n";
		assertThat(test.trim()).isEqualTo(messageWithoutTags(expectedMessage, "expectedMsg"));
	}

	@Test
	void should_generate_tests_with_message_headers_containing_regular_expression_with_escapes_for_Spock() {
		Contract contractDsl = contractFromGroovy("sh.stubborn.contract.spec.Contract.make {\n" + "\tname \"foo\"\n"
				+ "\tlabel 'trigger_event'\n" + "\tinput {\n" + "\t\ttriggeredBy('requestIsCalled()')\n" + "\t}\n"
				+ "\toutputMessage {\n" + "\t\tsentTo 'topic.rateablequote'\n" + "\t\theaders {\n"
				+ "\t\t\theader('processId', value(producer(regex(nonEmpty())), consumer('123')))\n" + "\t\t}\n"
				+ "\t\tbody([\n" + "\t\t\teventId: value(producer(regex(nonEmpty())), consumer('1'))\n" + "\t\t])\n"
				+ "\t}\n" + "}");
		properties.setTestFramework(TestFramework.SPOCK);

		String test = singleTestGenerator(contractDsl);

		String expectedMessage = "package com.example\n" + "\n" + "import com.jayway.jsonpath.DocumentContext\n"
				+ "import com.jayway.jsonpath.JsonPath\n" + "import spock.lang.Specification\n"
				+ "import org.springframework.beans.factory.annotation.Autowired\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierObjectMapper\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessage\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging\n" + "\n"
				+ "import static sh.stubborn.contract.verifier.assertion.SpringCloudContractAssertions.assertThat\n"
				+ "import static sh.stubborn.contract.verifier.util.ContractVerifierUtil.*\n"
				+ "import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson\n"
				+ "import static sh.stubborn.contract.verifier.messaging.util.ContractVerifierMessagingUtil.headers\n"
				+ "import static sh.stubborn.contract.verifier.util.ContractVerifierUtil.fileToBytes\n" + "\n"
				+ "@SuppressWarnings(\"rawtypes\")\n" + "class FooSpec extends Specification {\n"
				+ "\t@Autowired ContractVerifierMessaging contractVerifierMessaging\n"
				+ "\t@Autowired ContractVerifierObjectMapper contractVerifierObjectMapper\n" + "\n"
				+ "\tdef validate_foo() throws Exception {\n" + "\t\twhen:\n" + "\t\t\trequestIsCalled()\n" + "\n"
				+ "\t\tthen:\n"
				+ "\t\t\tContractVerifierMessage response = contractVerifierMessaging.receive(\"topic.rateablequote\",\n"
				+ "\t\t\t\t\tcontract(this, \"foo.yml\"))\n" + "\t\t\tresponse != null\n" + "\n" + "\t\tand:\n"
				+ "\t\t\tresponse.getHeader(\"processId\") != null\n"
				+ "\t\t\tresponse.getHeader(\"processId\").toString() ==~ java.util.regex.Pattern.compile('[\\\\S\\\\s]+')\n"
				+ "\n" + "\t\tand:\n"
				+ "\t\t\tDocumentContext parsedJson = JsonPath.parse(contractVerifierObjectMapper.writeValueAsString(response.getPayload()))\n"
				+ "\t\t\tassertThatJson(parsedJson).field(\"['eventId']\").matches(\"[\\\\S\\\\s]+\")\n" + "\t}\n"
				+ "\n" + "}\n";
		assertThat(test.trim()).isEqualTo(messageWithoutTags(expectedMessage, "expectedMsg"));
	}

	@Test
	void should_generate_tests_with_sentTo_having_a_method_execution_for_Spock() {
		Contract contractDsl = contractFromGroovy("sh.stubborn.contract.spec.Contract.make {\n" + "\tname \"foo\"\n"
				+ "\tlabel 'trigger_event'\n" + "\tinput {\n" + "\t\ttriggeredBy('requestIsCalled()')\n" + "\t}\n"
				+ "\toutputMessage {\n"
				+ "\t\tsentTo $(producer(execute(\"toString()\")), consumer('topic.rateablequote'))\n"
				+ "\t\theaders {\n" + "\t\t\theader('processId', value(producer(regex('[0-9]+')), consumer('123')))\n"
				+ "\t\t}\n" + "\t\tbody([\n" + "\t\t\teventId: value(producer(regex('[0-9]+')), consumer('1'))\n"
				+ "\t\t])\n" + "\t}\n" + "}");
		properties.setTestFramework(TestFramework.SPOCK);

		String test = singleTestGenerator(contractDsl);

		String expectedMessage = "package com.example\n" + "\n" + "import com.jayway.jsonpath.DocumentContext\n"
				+ "import com.jayway.jsonpath.JsonPath\n" + "import spock.lang.Specification\n"
				+ "import org.springframework.beans.factory.annotation.Autowired\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierObjectMapper\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessage\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging\n" + "\n"
				+ "import static sh.stubborn.contract.verifier.assertion.SpringCloudContractAssertions.assertThat\n"
				+ "import static sh.stubborn.contract.verifier.util.ContractVerifierUtil.*\n"
				+ "import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson\n"
				+ "import static sh.stubborn.contract.verifier.messaging.util.ContractVerifierMessagingUtil.headers\n"
				+ "import static sh.stubborn.contract.verifier.util.ContractVerifierUtil.fileToBytes\n" + "\n"
				+ "@SuppressWarnings(\"rawtypes\")\n" + "class FooSpec extends Specification {\n"
				+ "\t@Autowired ContractVerifierMessaging contractVerifierMessaging\n"
				+ "\t@Autowired ContractVerifierObjectMapper contractVerifierObjectMapper\n" + "\n"
				+ "\tdef validate_foo() throws Exception {\n" + "\t\twhen:\n" + "\t\t\trequestIsCalled()\n" + "\n"
				+ "\t\tthen:\n"
				+ "\t\t\tContractVerifierMessage response = contractVerifierMessaging.receive(toString(),\n"
				+ "\t\t\t\t\tcontract(this, \"foo.yml\"))\n" + "\t\t\tresponse != null\n" + "\n" + "\t\tand:\n"
				+ "\t\t\tresponse.getHeader(\"processId\") != null\n"
				+ "\t\t\tresponse.getHeader(\"processId\").toString() ==~ java.util.regex.Pattern.compile('[0-9]+')\n"
				+ "\n" + "\t\tand:\n"
				+ "\t\t\tDocumentContext parsedJson = JsonPath.parse(contractVerifierObjectMapper.writeValueAsString(response.getPayload()))\n"
				+ "\t\t\tassertThatJson(parsedJson).field(\"['eventId']\").matches(\"[0-9]+\")\n" + "\t}\n" + "\n"
				+ "}\n";
		assertThat(test.trim()).isEqualTo(messageWithoutTags(expectedMessage, "expectedMsg"));
	}

	@Test
	void should_generate_tests_with_sentTo_having_a_method_execution_for_JUnit() {
		Contract contractDsl = contractFromGroovy("sh.stubborn.contract.spec.Contract.make {\n" + "\tname \"foo\"\n"
				+ "\tlabel 'trigger_event'\n" + "\tinput {\n" + "\t\ttriggeredBy('requestIsCalled()')\n" + "\t}\n"
				+ "\toutputMessage {\n"
				+ "\t\tsentTo $(producer(execute(\"toString()\")), consumer('topic.rateablequote'))\n"
				+ "\t\theaders {\n" + "\t\t\theader('processId', value(producer(regex('[0-9]+')), consumer('123')))\n"
				+ "\t\t}\n" + "\t\tbody([\n" + "\t\t\teventId: value(producer(regex('[0-9]+')), consumer('1'))\n"
				+ "\t\t])\n" + "\t}\n" + "}");
		properties.setTestFramework(TestFramework.JUNIT5);

		String test = singleTestGenerator(contractDsl);

		String expectedMessage = "package com.example;\n" + "\n" + "import com.jayway.jsonpath.DocumentContext;\n"
				+ "import com.jayway.jsonpath.JsonPath;\n" + "import org.junit.jupiter.api.Test;\n"
				+ "import org.junit.jupiter.api.extension.ExtendWith;\n"
				+ "import org.springframework.beans.factory.annotation.Autowired;\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierObjectMapper;\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessage;\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;\n" + "\n"
				+ "import static sh.stubborn.contract.verifier.assertion.SpringCloudContractAssertions.assertThat;\n"
				+ "import static sh.stubborn.contract.verifier.util.ContractVerifierUtil.*;\n"
				+ "import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson;\n"
				+ "import static sh.stubborn.contract.verifier.messaging.util.ContractVerifierMessagingUtil.headers;\n"
				+ "import static sh.stubborn.contract.verifier.util.ContractVerifierUtil.fileToBytes;\n" + "\n"
				+ "@SuppressWarnings(\"rawtypes\")\n" + "public class FooTest {\n"
				+ "\t@Autowired ContractVerifierMessaging contractVerifierMessaging;\n"
				+ "\t@Autowired ContractVerifierObjectMapper contractVerifierObjectMapper;\n" + "\n" + "\t@Test\n"
				+ "\tpublic void validate_foo() throws Exception {\n" + "\t\t// when:\n" + "\t\t\trequestIsCalled();\n"
				+ "\n" + "\t\t// then:\n"
				+ "\t\t\tContractVerifierMessage response = contractVerifierMessaging.receive(toString(),\n"
				+ "\t\t\t\t\tcontract(this, \"foo.yml\"));\n" + "\t\t\tassertThat(response).isNotNull();\n" + "\n"
				+ "\t\t// and:\n" + "\t\t\tassertThat(response.getHeader(\"processId\")).isNotNull();\n"
				+ "\t\t\tassertThat(response.getHeader(\"processId\").toString()).matches(\"[0-9]+\");\n" + "\n"
				+ "\t\t// and:\n"
				+ "\t\t\tDocumentContext parsedJson = JsonPath.parse(contractVerifierObjectMapper.writeValueAsString(response.getPayload()));\n"
				+ "\t\t\tassertThatJson(parsedJson).field(\"['eventId']\").matches(\"[0-9]+\");\n" + "\t}\n" + "\n"
				+ "}\n";
		assertThat(test.trim()).isEqualTo(messageWithoutTags(expectedMessage, "expectedMsg"));
	}

	static Stream<Arguments> shouldGenerateTestsWithMessageHeadersContainingRegexWhichCompile() {
		String spockExpected = "package com.example\n" + "\n" + "import com.jayway.jsonpath.DocumentContext\n"
				+ "import com.jayway.jsonpath.JsonPath\n" + "import spock.lang.Specification\n"
				+ "import org.springframework.beans.factory.annotation.Autowired\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierObjectMapper\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessage\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging\n" + "\n"
				+ "import static sh.stubborn.contract.verifier.assertion.SpringCloudContractAssertions.assertThat\n"
				+ "import static sh.stubborn.contract.verifier.util.ContractVerifierUtil.*\n"
				+ "import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson\n"
				+ "import static sh.stubborn.contract.verifier.messaging.util.ContractVerifierMessagingUtil.headers\n"
				+ "import static sh.stubborn.contract.verifier.util.ContractVerifierUtil.fileToBytes\n" + "\n"
				+ "@SuppressWarnings(\"rawtypes\")\n" + "class FooSpec extends Specification {\n"
				+ "\t@Autowired ContractVerifierMessaging contractVerifierMessaging\n"
				+ "\t@Autowired ContractVerifierObjectMapper contractVerifierObjectMapper\n" + "\n"
				+ "\tdef validate_foo() throws Exception {\n" + "\t\twhen:\n" + "\t\t\tfoo()\n" + "\n" + "\t\tthen:\n"
				+ "\t\t\tContractVerifierMessage response = contractVerifierMessaging.receive(\"messageExchange\",\n"
				+ "\t\t\t\t\tcontract(this, \"foo.yml\"))\n" + "\t\t\tresponse != null\n" + "\n" + "\t\tand:\n"
				+ "\t\t\tresponse.getHeader(\"Authorization\") != null\n"
				+ "\t\t\tresponse.getHeader(\"Authorization\").toString() ==~ java.util.regex.Pattern.compile('Bearer [A-Za-z0-9\\\\-\\\\._~\\\\+\\\\/]+=*')\n"
				+ "\n" + "\t\tand:\n"
				+ "\t\t\tDocumentContext parsedJson = JsonPath.parse(contractVerifierObjectMapper.writeValueAsString(response.getPayload()))\n"
				+ "\t\t\tassertThatJson(parsedJson).field(\"['field']\").isEqualTo(\"value\")\n" + "\t}\n" + "\n"
				+ "}\n";
		String junitExpected = "package com.example;\n" + "\n" + "import com.jayway.jsonpath.DocumentContext;\n"
				+ "import com.jayway.jsonpath.JsonPath;\n" + "import org.junit.jupiter.api.Test;\n"
				+ "import org.junit.jupiter.api.extension.ExtendWith;\n"
				+ "import org.springframework.beans.factory.annotation.Autowired;\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierObjectMapper;\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessage;\n"
				+ "import sh.stubborn.contract.verifier.messaging.internal.ContractVerifierMessaging;\n" + "\n"
				+ "import static sh.stubborn.contract.verifier.assertion.SpringCloudContractAssertions.assertThat;\n"
				+ "import static sh.stubborn.contract.verifier.util.ContractVerifierUtil.*;\n"
				+ "import static com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson;\n"
				+ "import static sh.stubborn.contract.verifier.messaging.util.ContractVerifierMessagingUtil.headers;\n"
				+ "import static sh.stubborn.contract.verifier.util.ContractVerifierUtil.fileToBytes;\n" + "\n"
				+ "@SuppressWarnings(\"rawtypes\")\n" + "public class FooTest {\n"
				+ "\t@Autowired ContractVerifierMessaging contractVerifierMessaging;\n"
				+ "\t@Autowired ContractVerifierObjectMapper contractVerifierObjectMapper;\n" + "\n" + "\t@Test\n"
				+ "\tpublic void validate_foo() throws Exception {\n" + "\t\t// when:\n" + "\t\t\tfoo();\n" + "\n"
				+ "\t\t// then:\n"
				+ "\t\t\tContractVerifierMessage response = contractVerifierMessaging.receive(\"messageExchange\",\n"
				+ "\t\t\t\t\tcontract(this, \"foo.yml\"));\n" + "\t\t\tassertThat(response).isNotNull();\n" + "\n"
				+ "\t\t// and:\n" + "\t\t\tassertThat(response.getHeader(\"Authorization\")).isNotNull();\n"
				+ "\t\t\tassertThat(response.getHeader(\"Authorization\").toString()).matches(\"Bearer [A-Za-z0-9\\\\-\\\\._~\\\\+\\\\/]+=*\");\n"
				+ "\n" + "\t\t// and:\n"
				+ "\t\t\tDocumentContext parsedJson = JsonPath.parse(contractVerifierObjectMapper.writeValueAsString(response.getPayload()));\n"
				+ "\t\t\tassertThatJson(parsedJson).field(\"['field']\").isEqualTo(\"value\");\n" + "\t}\n" + "\n"
				+ "}\n";
		return Stream.of(
				Arguments.of("spock",
						(Consumer<ContractVerifierConfigProperties>) (props) -> props
							.setTestFramework(TestFramework.SPOCK),
						spockExpected),
				Arguments.of("junit", (Consumer<ContractVerifierConfigProperties>) (props) -> props
					.setTestFramework(TestFramework.JUNIT5), junitExpected));
	}

	@ParameterizedTest
	@MethodSource("shouldGenerateTestsWithMessageHeadersContainingRegexWhichCompile")
	void should_generate_tests_with_message_headers_containing_regular_expression_which_compile(
			String methodBuilderName, Consumer<ContractVerifierConfigProperties> methodBuilder, String expectedTest) {
		Contract contractDsl = contractFromGroovy("sh.stubborn.contract.spec.Contract.make {\n" + "\tname \"foo\"\n"
				+ "\tlabel 'shouldPublishMessage'\n" + "\tinput {\n" + "\t\ttriggeredBy('foo()')\n" + "\t}\n"
				+ "\toutputMessage {\n" + "\t\tsentTo('messageExchange')\n" + "\t\tbody([\n"
				+ "\t\t\t\"field\": \"value\"\n" + "\t\t])\n" + "\t\theaders {\n"
				+ "\t\t\theader('Authorization', value(regex('Bearer [A-Za-z0-9\\\\-\\\\._~\\\\+\\\\/]+=*')))\n"
				+ "\t\t}\n" + "\t}\n" + "}");
		methodBuilder.accept(properties);

		String test = singleTestGenerator(contractDsl);

		assertThat(test).doesNotContain("cursor");
		assertThat(test).doesNotContain("REGEXP>>");
		assertThat(test.trim()).isEqualTo(expectedTest.trim());
	}

}
