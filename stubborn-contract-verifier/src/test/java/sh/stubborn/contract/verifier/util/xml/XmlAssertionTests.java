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

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XmlAssertionTests {

	private static final String XML1 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "    <some>\n"
			+ "        <nested>\n" + "            <json>with &quot;val&apos;ue</json>\n"
			+ "            <anothervalue>4</anothervalue>\n"
			+ "            <withattr id=\"a\" id2=\"b\">foo</withattr>\n" + "            <withlist>\n"
			+ "                <name>name1</name>\n" + "            </withlist>\n" + "            <withlist>\n"
			+ "                <name>name2</name>\n" + "            </withlist>\n" + "            <withlist>\n"
			+ "                8\n" + "            </withlist>\n" + "            <withlist>\n"
			+ "                <name id=\"10\" surname=\"kowalski\">name3</name>\n" + "            </withlist>\n"
			+ "        </nested>\n" + "    </some>";

	private static final String XML2 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "    <root>\n"
			+ "        <property1>a</property1>\n" + "        <property2>b</property2>\n" + "    </root>\n";

	private static final String XML3 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "    <root>\n"
			+ "        <property1>true</property1>\n" + "        <property2 />\n"
			+ "        <property3>false</property3>\n" + "        <property4>5</property4>\n" + "    </root>\n";

	// xml4 built from MarkupBuilder in Groovy => equivalent inline XML:
	private static final String XML4 = "<root><property1>a</property1><property2><a>sth</a><b>sthElse</b></property2></root>";

	private static final String XML7 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "    <root>\n"
			+ "        <property1>\n" + "            <property2>test1</property2>\n" + "        </property1>\n"
			+ "        <property1>\n" + "            <property3>test2</property3>\n" + "        </property1>\n"
			+ "    </root>\n";

	private static final String XML8 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "    <root>\n"
			+ "        <property1>a</property1>\n" + "        <property2>\n" + "            <property3>b</property3>\n"
			+ "        </property2>\n" + "    </root>\n";

	// xml9 built from MarkupBuilder in Groovy => equivalent inline XML:
	private static final String XML9 = "<root><property1>a</property1><property2>123</property2></root>";

	// xml10 built from MarkupBuilder in Groovy => equivalent inline XML:
	private static final String XML10 = "<root><errors><property>bank_account_number</property><message>incorrect_format</message></errors></root>";

	private static final String XML11 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "    <root>\n"
			+ "        <place>\n" + "            <bounding_box>\n"
			+ "                <coordinates>-77.119759</coordinates>\n"
			+ "                <coordinates>38.995548</coordinates>\n"
			+ "                <coordinates>-76.909393</coordinates>\n"
			+ "                <coordinates>38.791645</coordinates>\n" + "            </bounding_box>\n"
			+ "        </place>\n" + "    </root>\n";

	static Stream<Arguments> convertXmlWithMapAsRoot() {
		return Stream.of(
				Arguments.of(
						XmlAssertion.assertThat(XML1).node("some").node("nested").node("anothervalue").isEqualTo(4),
						"/some/nested[anothervalue=4]"),
				Arguments.of(XmlAssertion.assertThat(XML1).node("some").node("nested").node("anothervalue"),
						"/some/nested/anothervalue"),
				Arguments.of(XmlAssertion.assertThat(XML1).node("some").text(), "/some/text()"),
				Arguments.of(XmlAssertion.assertThat(XML1)
					.node("some")
					.node("nested")
					.node("withattr")
					.withAttribute("id", "a")
					.withAttribute("id2", "b"), "/some/nested/withattr[@id='a'][@id2='b']"),
				Arguments.of(
						XmlAssertion.assertThat(XML1).node("some").node("nested").node("withattr").withAttribute("id"),
						"/some/nested/withattr/@id"),
				Arguments.of(XmlAssertion.assertThat(XML1)
					.node("some")
					.node("nested")
					.node("withattr")
					.isEqualTo("foo")
					.withAttribute("id", "a")
					.withAttribute("id2", "b"), "/some/nested[withattr='foo']/withattr[@id='a'][@id2='b']"),
				Arguments.of(
						XmlAssertion.assertThatXml(XML1).node("some").node("nested").node("anothervalue").isEqualTo(4),
						"/some/nested[anothervalue=4]"),
				Arguments.of(XmlAssertion.assertThat(XML1)
					.node("some")
					.node("nested")
					.array("withlist")
					.contains("name")
					.isEqualTo("name1"), "/some/nested/withlist[name='name1']"),
				Arguments.of(XmlAssertion.assertThat(XML1)
					.node("some")
					.node("nested")
					.array("withlist")
					.contains("name")
					.isEqualTo("name2"), "/some/nested/withlist[name='name2']"),
				Arguments.of(
						XmlAssertion.assertThat(XML1)
							.node("some")
							.node("nested")
							.array("withlist")
							.contains("name")
							.isEqualTo("name3")
							.withAttribute("id", "10")
							.withAttribute("surname", "kowalski"),
						"/some/nested/withlist[name='name3']/name[@id='10'][@surname='kowalski']"),
				Arguments.of(XmlAssertion.assertThat(XML1).node("some").node("nested").array("withlist").isEqualTo(8),
						"/some/nested/withlist[number()=8]"),
				Arguments.of(XmlAssertion.assertThat(XML1)
					.node("some")
					.node("nested")
					.node("json")
					.isEqualTo("with \"val'ue"), "/some/nested[json=concat('with \"val',\"'\",'ue')]"),
				Arguments.of(XmlAssertion.assertThat(XML1).node("some", "nested", "json").isEqualTo("with \"val'ue"),
						"/some/nested[json=concat('with \"val',\"'\",'ue')]"));
	}

	@ParameterizedTest
	@MethodSource("convertXmlWithMapAsRoot")
	void should_convert_an_xml_with_a_map_as_root_to_a_map_of_path_to_value(XmlVerifiable verifiable,
			String expectedXPath) {
		assertThat(verifiable.xPath()).isEqualTo(expectedXPath);
	}

	static Stream<Arguments> simpleResponseBody() {
		return Stream.of(
				Arguments.of(XmlAssertion.assertThat(XML2).node("root").node("property1").isEqualTo("a"),
						"/root[property1='a']"),
				Arguments.of(XmlAssertion.assertThat(XML2).node("root").node("property2").isEqualTo("b"),
						"/root[property2='b']"));
	}

	@ParameterizedTest
	@MethodSource("simpleResponseBody")
	void should_generate_assertions_for_simple_response_body(XmlVerifiable verifiable, String expectedXPath) {
		assertThat(verifiable.xPath()).isEqualTo(expectedXPath);
	}

	static Stream<Arguments> nullAndBooleanValues() {
		return Stream.of(
				Arguments.of(XmlAssertion.assertThat(XML3).node("root").node("property1").isEqualTo("true"),
						"/root[property1='true']"),
				Arguments.of(XmlAssertion.assertThat(XML3).node("root").node("property2").isNull(),
						"not(boolean(/root/property2/text()[1]))"),
				Arguments.of(XmlAssertion.assertThat(XML3).node("root").node("property3").isEqualTo(false),
						"/root[property3='false']"),
				Arguments.of(XmlAssertion.assertThat(XML3).node("root").node("property4").isEqualTo(5),
						"/root[property4=5]"));
	}

	@ParameterizedTest
	@MethodSource("nullAndBooleanValues")
	void should_generate_assertions_for_null_and_boolean_values(XmlVerifiable verifiable, String expectedXPath) {
		assertThat(verifiable.xPath()).isEqualTo(expectedXPath);
	}

	static Stream<Arguments> simpleResponseBodyFromMapWithList() {
		return Stream.of(
				Arguments.of(XmlAssertion.assertThat(XML4).node("root").node("property1").isEqualTo("a"),
						"/root[property1='a']"),
				Arguments.of(
						XmlAssertion.assertThat(XML4).node("root").array("property2").contains("a").isEqualTo("sth"),
						"/root/property2[a='sth']"),
				Arguments.of(XmlAssertion.assertThat(XML4)
					.node("root")
					.array("property2")
					.contains("b")
					.isEqualTo("sthElse"), "/root/property2[b='sthElse']"));
	}

	@ParameterizedTest
	@MethodSource("simpleResponseBodyFromMapWithList")
	void should_generate_assertions_for_simple_response_body_constructed_from_map_with_a_list(XmlVerifiable verifiable,
			String expectedXPath) {
		assertThat(verifiable.xPath()).isEqualTo(expectedXPath);
	}

	static Stream<Arguments> arrayInsideResponseBodyElement() {
		return Stream.of(Arguments.of(
				XmlAssertion.assertThat(XML7).node("root").array("property1").contains("property2").isEqualTo("test1"),
				"/root/property1[property2='test1']"),
				Arguments.of(XmlAssertion.assertThat(XML7)
					.node("root")
					.array("property1")
					.contains("property3")
					.isEqualTo("test2"), "/root/property1[property3='test2']"));
	}

	@ParameterizedTest
	@MethodSource("arrayInsideResponseBodyElement")
	void should_generate_assertions_for_array_inside_response_body_element(XmlVerifiable verifiable,
			String expectedXPath) {
		assertThat(verifiable.xPath()).isEqualTo(expectedXPath);
	}

	static Stream<Arguments> nestedObjectsInResponseBody() {
		return Stream.of(
				Arguments.of(
						XmlAssertion.assertThat(XML8).node("root").node("property2").node("property3").isEqualTo("b"),
						"/root/property2[property3='b']"),
				Arguments.of(XmlAssertion.assertThat(XML8).node("root").node("property1").isEqualTo("a"),
						"/root[property1='a']"));
	}

	@ParameterizedTest
	@MethodSource("nestedObjectsInResponseBody")
	void should_generate_assertions_for_nested_objects_in_response_body(XmlVerifiable verifiable,
			String expectedXPath) {
		assertThat(verifiable.xPath()).isEqualTo(expectedXPath);
	}

	static Stream<Arguments> regexAssertionsForMapObjects() {
		return Stream.of(
				Arguments.of(XmlAssertion.assertThat(XML9).node("root").node("property2").matches("[0-9]{3}"),
						"/root[matches(property2, '[0-9]{3}')]"),
				Arguments.of(XmlAssertion.assertThat(XML9).node("root").node("property1").isEqualTo("a"),
						"/root[property1='a']"));
	}

	@ParameterizedTest
	@MethodSource("regexAssertionsForMapObjects")
	void should_generate_regex_assertions_for_map_objects_in_response_body(XmlVerifiable verifiable,
			String expectedXPath) {
		assertThat(verifiable.xPath()).isEqualTo(expectedXPath);
	}

	@Test
	void should_generate_escaped_regex_assertions_for_string_objects_in_response_body() {
		String xml = "<root><property2>123123</property2></root>";
		XmlVerifiable verifiable = XmlAssertion.assertThat(xml).node("root").node("property2").matches("\\d+");
		assertThat(verifiable.xPath()).isEqualTo("/root[matches(property2, '\\d+')]");
	}

	static Stream<Arguments> complexXpaths() {
		return Stream.of(
				Arguments.of(XmlAssertion.assertThat(XML10)
					.node("root")
					.array("errors")
					.contains("property")
					.isEqualTo("bank_account_number"), "/root/errors[property='bank_account_number']"),
				Arguments.of(XmlAssertion.assertThat(XML10)
					.node("root")
					.array("errors")
					.contains("message")
					.isEqualTo("incorrect_format"), "/root/errors[message='incorrect_format']"));
	}

	@ParameterizedTest
	@MethodSource("complexXpaths")
	void should_work_with_more_complex_stuff_and_xpaths(XmlVerifiable verifiable, String expectedXPath) {
		assertThat(verifiable.xPath()).isEqualTo(expectedXPath);
	}

	static Stream<Arguments> doubleArray() {
		return Stream.of(
				Arguments.of(XmlAssertion.assertThat(XML11)
					.node("root")
					.node("place")
					.node("bounding_box")
					.array("coordinates")
					.isEqualTo(38.995548), "/root/place/bounding_box/coordinates[number()=38.995548]"),
				Arguments.of(XmlAssertion.assertThat(XML11)
					.node("root")
					.node("place")
					.node("bounding_box")
					.array("coordinates")
					.isEqualTo(-77.119759), "/root/place/bounding_box/coordinates[number()=-77.119759]"),
				Arguments.of(XmlAssertion.assertThat(XML11)
					.node("root")
					.node("place")
					.node("bounding_box")
					.array("coordinates")
					.isEqualTo(-76.909393), "/root/place/bounding_box/coordinates[number()=-76.909393]"),
				Arguments.of(XmlAssertion.assertThat(XML11)
					.node("root")
					.node("place")
					.node("bounding_box")
					.array("coordinates")
					.isEqualTo(38.791645), "/root/place/bounding_box/coordinates[number()=38.791645]"));
	}

	@ParameterizedTest
	@MethodSource("doubleArray")
	void should_manage_to_parse_a_double_array(XmlVerifiable verifiable, String expectedXPath) {
		assertThat(verifiable.xPath()).isEqualTo(expectedXPath);
	}

	@Test
	void should_run_xpath_when_provided_manually() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "    <root>\n"
				+ "        <property1>a</property1>\n" + "        <property2>\n"
				+ "            <property3>b</property3>\n" + "        </property2>\n" + "    </root>\n";
		String xPath = "/root/property2[property3='b']";
		XmlAssertion.assertThat(xml).matchesXPath(xPath);
	}

	@Test
	void should_throw_exception_when_xpath_is_not_matched() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "    <root>\n"
				+ "        <property1>a</property1>\n" + "        <property2>\n"
				+ "            <property3>b</property3>\n" + "        </property2>\n" + "    </root>\n";
		String xPath = "/root/property2[property3='non-existing']";
		assertThatThrownBy(() -> XmlAssertion.assertThat(xml).matchesXPath(xPath))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Parsed XML")
			.hasMessageContaining("doesn't match the XPath");
	}

	@Test
	void should_not_throw_exception_when_json_path_is_not_matched_and_system_prop_overrides_the_check() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "    <root>\n"
				+ "        <property1>a</property1>\n" + "        <property2>\n"
				+ "            <property3>b</property3>\n" + "        </property2>\n" + "    </root>\n";
		String xPath = "/root/property2[property3='non-existing']";
		// should not throw
		XmlAssertion.assertThat(xml).withoutThrowingException().matchesXPath(xPath);
	}

	@Test
	void should_generate_escaped_regex_assertions_for_text_with_regular_expression_values() {
		// '"<>[]()
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "        <root>\n"
				+ "            <property1>&apos;&quot;&lt;&gt;[]()</property1>\n" + "        </root>";
		XmlVerifiable verifiable = XmlAssertion.assertThat(xml)
			.node("root")
			.node("property1")
			.matches("'\"<>\\[\\]\\(\\)");
		assertThat(verifiable.xPath()).isEqualTo("/root[matches(property1, concat('',\"'\",'\"<>\\[\\]\\(\\)'))]");
	}

	@Test
	void should_escape_regular_expression_properly() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "            <root>\n"
				+ "                <path>/api/12</path>\n" + "                <correlationId>123456</correlationId>\n"
				+ "            </root>";
		XmlVerifiable verifiable = XmlAssertion.assertThatXml(xml).node("root").node("path").matches("^/api/[0-9]{2}$");
		assertThat(verifiable.xPath()).isEqualTo("/root[matches(path, '^/api/[0-9]{2}$')]");
	}

	@Test
	void should_escape_single_quotes_in_a_quoted_string() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "        <root>\n"
				+ "            <text>text with &apos;quotes&apos; inside</text>\n" + "        </root>\n";
		XmlVerifiable verifiable = XmlAssertion.assertThatXml(xml)
			.node("root")
			.node("text")
			.isEqualTo("text with 'quotes' inside");
		assertThat(verifiable.xPath()).isEqualTo("/root[text=concat('text with ',\"'\",'quotes',\"'\",' inside')]");
	}

	@Test
	void should_escape_brackets_in_a_string() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "        <root>\n"
				+ "            <id>&lt;escape me&gt;</id>\n" + "        </root>\n";
		XmlVerifiable verifiable = XmlAssertion.assertThatXml(xml).node("root").node("id").isEqualTo("<escape me>");
		assertThat(verifiable.xPath()).isEqualTo("/root[id='<escape me>']");
	}

	@Test
	void should_escape_double_quotes_in_a_quoted_string() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "        <root>\n"
				+ "            <text>text with &quot;quotes&quot; inside</text>\n" + "        </root>\n";
		XmlVerifiable verifiable = XmlAssertion.assertThatXml(xml)
			.node("root")
			.node("text")
			.isEqualTo("text with \"quotes\" inside");
		assertThat(verifiable.xPath()).isEqualTo("/root[text='text with \"quotes\" inside']");
	}

	@Test
	void should_resolve_the_value_of_xml_via_xpath() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "    <root>\n" + "        <element>\n"
				+ "            <some>\n" + "                <nested>\n"
				+ "                    <json>with value</json>\n"
				+ "                    <anothervalue>4</anothervalue>\n" + "                    <withlist>\n"
				+ "                        <name>name1</name>\n" + "                    </withlist>\n"
				+ "                    <withlist>\n" + "                        <name>name2</name>\n"
				+ "                    </withlist>\n" + "                    <withlist>\n"
				+ "                        <anothernested>\n" + "                            <name>name3</name>\n"
				+ "                        </anothernested>\n" + "                    </withlist>\n"
				+ "                </nested>\n" + "            </some>\n" + "        </element>\n"
				+ "        <element>\n" + "            <someother>\n" + "                <nested>\n"
				+ "                    <json>true</json>\n" + "                    <anothervalue>4</anothervalue>\n"
				+ "                    <withlist>\n" + "                        <name>name1</name>\n"
				+ "                    </withlist>\n" + "                    <withlist>\n"
				+ "                        <name>name2</name>\n" + "                    </withlist>\n"
				+ "                    <withlist2>a</withlist2>\n" + "                    <withlist2>b</withlist2>\n"
				+ "                </nested>\n" + "            </someother>\n" + "        </element>\n" + "    </root>";
		assertThat(
				XPathBuilder.builder(xml).node("root").array("element").node("some").node("nested").node("json").read())
			.isEqualTo("with value");
		assertThat(XPathBuilder.builder(xml)
			.node("root")
			.array("element")
			.node("some")
			.node("nested")
			.node("anothervalue")
			.read()).isEqualTo("4");
		assertThat(XmlAssertion.assertThat(xml)
			.node("root")
			.array("element")
			.node("someother")
			.node("nested")
			.node("json")
			.read()).isEqualTo(Boolean.TRUE.toString());
	}

	@Test
	void should_match_array_containing_an_array_of_primitives() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "    <root>\n"
				+ "        <first_name>existing</first_name>\n" + "        <elements>\n" + "            <partners>\n"
				+ "                <role>AGENT</role>\n" + "                <payment_methods>BANK</payment_methods>\n"
				+ "                <payment_methods>CASH</payment_methods>\n" + "            </partners>\n"
				+ "        </elements>\n" + "    </root>\n";
		XmlVerifiable verifiable = XmlAssertion.assertThatXml(xml)
			.node("root")
			.array("elements")
			.array("partners")
			.contains("payment_methods")
			.isEqualTo("BANK");
		assertThat(verifiable.xPath()).isEqualTo("/root/elements/partners[payment_methods='BANK']");
	}

	@Test
	void should_match_pattern_in_array() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "    <root>\n"
				+ "        <authorities>ROLE_ADMIN</authorities>\n" + "    </root>\n";
		XmlVerifiable verifiable = XmlAssertion.assertThatXml(xml)
			.node("root")
			.array("authorities")
			.matches("^[a-zA-Z0-9_\\- ]+$");
		assertThat(verifiable.xPath()).isEqualTo("/root/authorities[matches(text(), '^[a-zA-Z0-9_\\- ]+$')]");
	}

	@Test
	void should_manage_to_parse_array_with_string_values() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "    <root>\n"
				+ "        <some_list>name1</some_list>\n" + "        <some_list>name2</some_list>\n" + "    </root>";
		XmlVerifiable v1 = XmlAssertion.assertThat(xml).node("root").array("some_list").isEqualTo("name1");
		XmlVerifiable v2 = XmlAssertion.assertThat(xml).node("root").array("some_list").isEqualTo("name2");
		assertThat(v1.xPath()).isEqualTo("/root/some_list[text()='name1']");
		assertThat(v2.xPath()).isEqualTo("/root/some_list[text()='name2']");
	}

	@Test
	void should_allow_nested_calls_with_counting_the_elements_size() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "    <root>\n"
				+ "        <some_list>name1</some_list>\n" + "        <some_list>name2</some_list>\n" + "    </root>";
		XmlVerifiable v1 = XmlAssertion.assertThat(xml).node("root").array("some_list").hasSize(2).isEqualTo("name1");
		assertThat(v1.xPath()).isEqualTo("/root/some_list[text()='name1']");
	}

	@Test
	void should_count_the_elements_size() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "    <root>\n"
				+ "        <some_list>name1</some_list>\n" + "        <some_list>name2</some_list>\n" + "    </root>";
		XmlVerifiable v1 = XmlAssertion.assertThat(xml).node("root").array("some_list").hasSize(2);
		assertThat(v1.xPath()).isEqualTo("count(/root/some_list)");
	}

	@Test
	void should_throw_exception_if_size_is_wrong() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "    <root>\n"
				+ "        <some_list>name1</some_list>\n" + "        <some_list>name2</some_list>\n" + "    </root>";
		assertThatThrownBy(() -> XmlAssertion.assertThat(xml).node("root").array("some_list").hasSize(1))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("has size [2] and not [1] for XPath <count(/root/some_list)>");
	}

	@Test
	void should_return_0_if_element_is_missing() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "    <root>\n"
				+ "        <some_list>name1</some_list>\n" + "        <some_list>name2</some_list>\n" + "    </root>";
		assertThatThrownBy(() -> XmlAssertion.assertThat(xml).node("root").array("foo").hasSize(1))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("has size [0] and not [1] for XPath <count(/root/foo)>");
	}

}
