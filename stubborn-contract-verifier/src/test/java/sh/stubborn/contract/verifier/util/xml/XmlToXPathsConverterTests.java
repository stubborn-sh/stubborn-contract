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

package sh.stubborn.contract.verifier.util.xml;

import javax.xml.xpath.XPathExpressionException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import sh.stubborn.contract.spec.internal.BodyMatcher;
import sh.stubborn.contract.spec.internal.BodyMatchers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Chris Bono
 * @since 2.1.0
 */
class XmlToXPathsConverterTests {

	static final String NAMED_XML = """
			<ns1:customer xmlns:ns1="http://demo.com/testns">
			      <email>customer@test.com</email>
			    </ns1:customer>
			    """;

	static final String NAMED_DEFAULT_NAMESPACE_XML = """
			<customer xmlns="http://demo.com/testns">
			      <email>customer@test.com</email>
			    </customer>
			    """;

	static final String UNNAMED_XML = """
			<customer>
			      <email>customer@test.com</email>
			    </customer>
			    """;

	static final String ATTRIBUTES_IN_CHILD_XML = """
			<customer first_custom_attribute="first_value">
			      <email second_custom_attribute="second_value" >customer@test.com</email>
			      <address third_custom_attribute="third_value"/>
			    </customer>
			    """;

	static List<Arguments> retrieveValueFromBodyCases() {
		return List.of(
				Arguments.of(XmlToXPathsConverter.retrieveValueFromBody("/customer/email/text()", UNNAMED_XML),
						"customer@test.com"),
				Arguments.of(XmlToXPathsConverter.retrieveValueFromBody("/ns1:customer/email/text()", NAMED_XML),
						"customer@test.com"),
				Arguments.of(XmlToXPathsConverter.retrieveValueFromBody("//email/text()", NAMED_XML),
						"customer@test.com"),
				Arguments.of(XmlToXPathsConverter.retrieveValueFromBody("/customer/email/text()", NAMED_XML), ""),
				Arguments.of(XmlToXPathsConverter.retrieveValueFromBody(
						"/*[local-name()='customer' and namespace-uri()='http://demo.com/testns']/*[local-name()='email']/text()",
						NAMED_DEFAULT_NAMESPACE_XML), "customer@test.com"),
				Arguments.of(XmlToXPathsConverter.retrieveValueFromBody("//*[local-name()='email']/text()",
						NAMED_DEFAULT_NAMESPACE_XML), "customer@test.com"),
				Arguments.of(XmlToXPathsConverter.retrieveValueFromBody(
						"/*[local-name()='customer']/*[local-name()='email']/text()", NAMED_DEFAULT_NAMESPACE_XML),
						"customer@test.com"),
				Arguments.of(XmlToXPathsConverter.retrieveValueFromBody(
						"/*[local-name()='customer' and namespace-uri()='http://demo.com/testns']/email/text()",
						NAMED_DEFAULT_NAMESPACE_XML), ""),
				Arguments.of(XmlToXPathsConverter.retrieveValueFromBody("/customer/email/text()",
						NAMED_DEFAULT_NAMESPACE_XML), ""));
	}

	@ParameterizedTest
	@MethodSource("retrieveValueFromBodyCases")
	void should_generate_expected_value_for_xPath(String value, String expectedValue) {
		assertThat(value).isEqualTo(expectedValue);
	}

	@Test
	void should_throw_exception_when_searching_for_non_existent_namespace() {
		assertThatThrownBy(() -> XmlToXPathsConverter.retrieveValueFromBody("/ns1:customer/email/text()", UNNAMED_XML))
			.isInstanceOf(XPathExpressionException.class)
			.hasMessageContaining("Prefix must resolve to a namespace: ns1");
	}

	static List<Arguments> matchedPathCases() {
		List<BodyMatcher> matchers = XmlToXPathsConverter.mapToMatchers(ATTRIBUTES_IN_CHILD_XML);
		return List.of(Arguments.of(matchers.get(0).path(), "/customer/email/text()"),
				Arguments.of(matchers.get(1).path(), "/customer/@first_custom_attribute"),
				Arguments.of(matchers.get(2).path(), "/customer/email/@second_custom_attribute"),
				Arguments.of(matchers.get(3).path(), "/customer/address/@third_custom_attribute"));
	}

	@ParameterizedTest
	@MethodSource("matchedPathCases")
	void should_generate_matched_path_for_xPath(String value, String expectedValue) {
		assertThat(value).isEqualTo(expectedValue);
	}

	static List<Arguments> removeMatchingXPathsCases() {
		return List.of(Arguments.of("/customer/email/text()", UNNAMED_XML,
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><customer>\n      <email/>\n    </customer>"),
				Arguments.of("/customer/email/text()", NAMED_XML,
						"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><ns1:customer xmlns:ns1=\"http://demo.com/testns\">\n      <email>customer@test.com</email>\n    </ns1:customer>"),
				Arguments.of("/ns1:customer/email/text()", NAMED_XML,
						"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><ns1:customer xmlns:ns1=\"http://demo.com/testns\">\n      <email/>\n    </ns1:customer>"),
				Arguments.of(
						"/*[local-name()='customer' and namespace-uri()='http://demo.com/testns']/*[local-name()='email']/text()",
						NAMED_DEFAULT_NAMESPACE_XML,
						"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><customer xmlns=\"http://demo.com/testns\">\n      <email/>\n    </customer>"));
	}

	@ParameterizedTest
	@MethodSource("removeMatchingXPathsCases")
	void should_remove_elements_for_xPath(String xpath, String xml, String result) {
		BodyMatchers m = new BodyMatchers();
		m.xPath(xpath, m.byEquality());
		assertThat(XmlToXPathsConverter.removeMatchingXPaths(xml, m)).isEqualTo(result);
	}

	@Test
	void should_remove_multiple_elements_when_xpath_matches_them() {
		String test = """
				<root>
				    <childOne>
				        <id>123</id>
				    </childOne>
				    <childTwo>
				        <id>234</id>
				    </childTwo>
				</root>
				""";
		BodyMatchers m = new BodyMatchers();
		m.xPath("/root/*/id/text()", m.byEquality());
		assertThat(XmlToXPathsConverter.removeMatchingXPaths(test, m)).isEqualTo(
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><root>\n    <childOne>\n        <id/>\n    </childOne>\n    <childTwo>\n        <id/>\n    </childTwo>\n</root>");
	}

}
