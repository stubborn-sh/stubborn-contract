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

package com.toomuchcoding.xmlassert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Marcin Grzejszczak
 */
class XmlAssertionTests {

	static final String XML1 = """
			<?xml version="1.0" encoding="UTF-8" ?>
			<some>
			    <nested>
			        <json>with &quot;val&apos;ue</json>
			        <anothervalue>4</anothervalue>
			        <withattr id="a" id2="b">foo</withattr>
			        <withlist>
			            <name>name1</name>
			        </withlist>
			        <withlist>
			            <name>name2</name>
			        </withlist>
			        <withlist>
			            8
			        </withlist>
			        <withlist>
			            <name id="10" surname="kowalski">name3</name>
			        </withlist>
			    </nested>
			</some>""";

	static final String XML2 = """
			<?xml version="1.0" encoding="UTF-8" ?>
			<root>
			    <property1>a</property1>
			    <property2>b</property2>
			</root>
			""";

	static final String XML3 = """
			<?xml version="1.0" encoding="UTF-8" ?>
			<root>
			    <property1>true</property1>
			    <property2 />
			    <property3>false</property3>
			    <property4>5</property4>
			</root>
			""";

	static final String XML4 = "<root><property1>a</property1><property2><a>sth</a><b>sthElse</b></property2></root>";

	static final String XML7 = """
			<?xml version="1.0" encoding="UTF-8" ?>
			<root>
			    <property1>
			        <property2>test1</property2>
			    </property1>
			    <property1>
			        <property3>test2</property3>
			    </property1>
			</root>
			""";

	static final String XML8 = """
			<?xml version="1.0" encoding="UTF-8" ?>
			<root>
			    <property1>a</property1>
			    <property2>
			        <property3>b</property3>
			    </property2>
			</root>
			""";

	static final String XML9 = "<root><property1>a</property1><property2>123</property2></root>";

	static final String XML10 = "<root><errors><property>bank_account_number</property><message>incorrect_format</message></errors></root>";

	static final String XML11 = """
			<?xml version="1.0" encoding="UTF-8" ?>
			<root>
			    <place>
			        <bounding_box>
			            <coordinates>-77.119759</coordinates>
			            <coordinates>38.995548</coordinates>
			            <coordinates>-76.909393</coordinates>
			            <coordinates>38.791645</coordinates>
			        </bounding_box>
			    </place>
			</root>
			""";

	@ParameterizedTest
	@MethodSource("provideMapAsRootXPaths")
	void shouldConvertXmlWithMapAsRootToMapOfPathToValue(XmlVerifiable verifiable, String expectedXPath) {
		assertThat(verifiable.xPath()).isEqualTo(expectedXPath);
	}

	static Stream<Arguments> provideMapAsRootXPaths() {
		return Stream.of(
				Arguments.of(
						XmlAssertion.assertThat(XML1).node("some").node("nested").node("anothervalue").isEqualTo(4),
						"/some/nested[anothervalue=4]"),
				Arguments.of(XmlAssertion.assertThat(XML1).node("some").node("nested").node("anothervalue"),
						"/some/nested/anothervalue"),
				Arguments.of(XmlAssertion.assertThat(XML1)
					.node("some")
					.node("nested")
					.node("withattr")
					.withAttribute("id", "a")
					.withAttribute("id2", "b"), "/some/nested/withattr[@id='a'][@id2='b']"),
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
	@MethodSource("provideSimpleResponseBodyXPaths")
	void shouldGenerateAssertionsForSimpleResponseBody(XmlVerifiable verifiable, String expectedXPath) {
		assertThat(verifiable.xPath()).isEqualTo(expectedXPath);
	}

	static Stream<Arguments> provideSimpleResponseBodyXPaths() {
		return Stream.of(
				Arguments.of(XmlAssertion.assertThat(XML2).node("root").node("property1").isEqualTo("a"),
						"/root[property1='a']"),
				Arguments.of(XmlAssertion.assertThat(XML2).node("root").node("property2").isEqualTo("b"),
						"/root[property2='b']"));
	}

	@ParameterizedTest
	@MethodSource("provideNullAndBooleanXPaths")
	void shouldGenerateAssertionsForNullAndBooleanValues(XmlVerifiable verifiable, String expectedXPath) {
		assertThat(verifiable.xPath()).isEqualTo(expectedXPath);
	}

	static Stream<Arguments> provideNullAndBooleanXPaths() {
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
	@MethodSource("provideMapWithListXPaths")
	void shouldGenerateAssertionsForSimpleResponseBodyConstructedFromMapWithList(XmlVerifiable verifiable,
			String expectedXPath) {
		assertThat(verifiable.xPath()).isEqualTo(expectedXPath);
	}

	static Stream<Arguments> provideMapWithListXPaths() {
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
	@MethodSource("provideArrayInsideBodyXPaths")
	void shouldGenerateAssertionsForArrayInsideResponseBodyElement(XmlVerifiable verifiable, String expectedXPath) {
		assertThat(verifiable.xPath()).isEqualTo(expectedXPath);
	}

	static Stream<Arguments> provideArrayInsideBodyXPaths() {
		return Stream.of(Arguments.of(
				XmlAssertion.assertThat(XML7).node("root").array("property1").contains("property2").isEqualTo("test1"),
				"/root/property1[property2='test1']"),
				Arguments.of(XmlAssertion.assertThat(XML7)
					.node("root")
					.array("property1")
					.contains("property3")
					.isEqualTo("test2"), "/root/property1[property3='test2']"));
	}

	@Test
	void shouldGenerateAssertionsForNestedObjectsInResponseBody() {
		assertThat(
				XmlAssertion.assertThat(XML8).node("root").node("property2").node("property3").isEqualTo("b").xPath())
			.isEqualTo("/root/property2[property3='b']");
		assertThat(XmlAssertion.assertThat(XML8).node("root").node("property1").isEqualTo("a").xPath())
			.isEqualTo("/root[property1='a']");
	}

	@ParameterizedTest
	@MethodSource("provideRegexXPaths")
	void shouldGenerateRegexAssertionsForMapObjectsInResponseBody(XmlVerifiable verifiable, String expectedXPath) {
		assertThat(verifiable.xPath()).isEqualTo(expectedXPath);
	}

	static Stream<Arguments> provideRegexXPaths() {
		return Stream.of(
				Arguments.of(XmlAssertion.assertThat(XML9).node("root").node("property2").matches("[0-9]{3}"),
						"/root[matches(property2, '[0-9]{3}')]"),
				Arguments.of(XmlAssertion.assertThat(XML9).node("root").node("property1").isEqualTo("a"),
						"/root[property1='a']"));
	}

	@Test
	void shouldGenerateEscapedRegexAssertionsForStringObjectsInResponseBody() {
		String xml = "<root><property2>123123</property2></root>";
		XmlVerifiable verifiable = XmlAssertion.assertThat(xml).node("root").node("property2").matches("\\d+");
		assertThat(verifiable.xPath()).isEqualTo("/root[matches(property2, '\\d+')]");
	}

	@ParameterizedTest
	@MethodSource("provideComplexXPaths")
	void shouldWorkWithMoreComplexStuffAndXPaths(XmlVerifiable verifiable, String expectedXPath) {
		assertThat(verifiable.xPath()).isEqualTo(expectedXPath);
	}

	static Stream<Arguments> provideComplexXPaths() {
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
	@MethodSource("provideDoubleArrayXPaths")
	void shouldManageToParseDoubleArray(XmlVerifiable verifiable, String expectedXPath) {
		assertThat(verifiable.xPath()).isEqualTo(expectedXPath);
	}

	static Stream<Arguments> provideDoubleArrayXPaths() {
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

	@Test
	void shouldRunXPathWhenProvidedManually() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <property1>a</property1>
				    <property2>
				        <property3>b</property3>
				    </property2>
				</root>
				""";
		XmlAssertion.assertThat(xml).matchesXPath("/root/property2[property3='b']");
	}

	@Test
	void shouldThrowExceptionWhenXPathIsNotMatched() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <property1>a</property1>
				    <property2>
				        <property3>b</property3>
				    </property2>
				</root>
				""";
		assertThatThrownBy(() -> XmlAssertion.assertThat(xml).matchesXPath("/root/property2[property3='non-existing']"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Parsed XML")
			.hasMessageContaining("doesn't match the XPath");
	}

	@Test
	void shouldNotThrowExceptionWhenXPathIsNotMatchedAndSystemPropOverridesCheck() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <property1>a</property1>
				    <property2>
				        <property3>b</property3>
				    </property2>
				</root>
				""";
		XmlAssertion.assertThat(xml)
			.withoutThrowingException()
			.matchesXPath("/root/property2[property3='non-existing']");
	}

	@Test
	void shouldGenerateEscapedRegexAssertionsForTextWithRegularExpressionValues() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <property1>&apos;&quot;&lt;&gt;[]()</property1>
				</root>""";
		XmlVerifiable verifiable = XmlAssertion.assertThat(xml)
			.node("root")
			.node("property1")
			.matches("'\"<>\\[\\]\\(\\)");
		assertThat(verifiable.xPath()).isEqualTo("/root[matches(property1, concat('',\"'\",'\"<>\\[\\]\\(\\)'))]");
	}

	@Test
	void shouldEscapeRegularExpressionProperly() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <path>/api/12</path>
				    <correlationId>123456</correlationId>
				</root>""";
		XmlVerifiable verifiable = XmlAssertion.assertThatXml(xml).node("root").node("path").matches("^/api/[0-9]{2}$");
		assertThat(verifiable.xPath()).isEqualTo("/root[matches(path, '^/api/[0-9]{2}$')]");
	}

	@Test
	void shouldEscapeSingleQuotesInQuotedString() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <text>text with &apos;quotes&apos; inside</text>
				</root>
				""";
		XmlVerifiable verifiable = XmlAssertion.assertThatXml(xml)
			.node("root")
			.node("text")
			.isEqualTo("text with 'quotes' inside");
		assertThat(verifiable.xPath()).isEqualTo("/root[text=concat('text with ',\"'\",'quotes',\"'\",' inside')]");
	}

	@Test
	void shouldEscapeBracketsInString() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <id>&lt;escape me&gt;</id>
				</root>
				""";
		XmlVerifiable verifiable = XmlAssertion.assertThatXml(xml).node("root").node("id").isEqualTo("<escape me>");
		assertThat(verifiable.xPath()).isEqualTo("/root[id='<escape me>']");
	}

	@Test
	void shouldEscapeDoubleQuotesInQuotedString() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <text>text with &quot;quotes&quot; inside</text>
				</root>
				""";
		XmlVerifiable verifiable = XmlAssertion.assertThatXml(xml)
			.node("root")
			.node("text")
			.isEqualTo("text with \"quotes\" inside");
		assertThat(verifiable.xPath()).isEqualTo("/root[text='text with \"quotes\" inside']");
	}

	@Test
	void shouldResolveValueOfXmlViaXPath() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <element>
				        <some>
				            <nested>
				                <json>with value</json>
				                <anothervalue>4</anothervalue>
				                <withlist>
				                    <name>name1</name>
				                </withlist>
				                <withlist>
				                    <name>name2</name>
				                </withlist>
				                <withlist>
				                    <anothernested>
				                        <name>name3</name>
				                    </anothernested>
				                </withlist>
				            </nested>
				        </some>
				    </element>
				    <element>
				        <someother>
				            <nested>
				                <json>true</json>
				                <anothervalue>4</anothervalue>
				                <withlist>
				                    <name>name1</name>
				                </withlist>
				                <withlist>
				                    <name>name2</name>
				                </withlist>
				                <withlist2>a</withlist2>
				                <withlist2>b</withlist2>
				            </nested>
				        </someother>
				    </element>
				</root>""";
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
	void shouldMatchArrayContainingArrayOfPrimitives() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <first_name>existing</first_name>
				    <elements>
				        <partners>
				            <role>AGENT</role>
				            <payment_methods>BANK</payment_methods>
				            <payment_methods>CASH</payment_methods>
				        </partners>
				    </elements>
				</root>
				""";
		XmlVerifiable verifiable = XmlAssertion.assertThatXml(xml)
			.node("root")
			.array("elements")
			.array("partners")
			.contains("payment_methods")
			.isEqualTo("BANK");
		assertThat(verifiable.xPath()).isEqualTo("/root/elements/partners[payment_methods='BANK']");
	}

	@Test
	void shouldMatchPatternInArray() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <authorities>ROLE_ADMIN</authorities>
				</root>
				""";
		XmlVerifiable verifiable = XmlAssertion.assertThatXml(xml)
			.node("root")
			.array("authorities")
			.matches("^[a-zA-Z0-9_\\- ]+$");
		assertThat(verifiable.xPath()).isEqualTo("/root/authorities[matches(text(), '^[a-zA-Z0-9_\\- ]+$')]");
	}

	@Test
	void shouldManageToParseArrayWithStringValues() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <some_list>name1</some_list>
				    <some_list>name2</some_list>
				</root>""";
		XmlVerifiable v1 = XmlAssertion.assertThat(xml).node("root").array("some_list").isEqualTo("name1");
		XmlVerifiable v2 = XmlAssertion.assertThat(xml).node("root").array("some_list").isEqualTo("name2");
		assertThat(v1.xPath()).isEqualTo("/root/some_list[text()='name1']");
		assertThat(v2.xPath()).isEqualTo("/root/some_list[text()='name2']");
	}

	@Test
	void shouldAllowNestedCallsWithCountingElementsSize() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <some_list>name1</some_list>
				    <some_list>name2</some_list>
				</root>""";
		XmlVerifiable v1 = XmlAssertion.assertThat(xml).node("root").array("some_list").hasSize(2).isEqualTo("name1");
		assertThat(v1.xPath()).isEqualTo("/root/some_list[text()='name1']");
	}

	@Test
	void shouldCountElementsSize() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <some_list>name1</some_list>
				    <some_list>name2</some_list>
				</root>""";
		XmlVerifiable v1 = XmlAssertion.assertThat(xml).node("root").array("some_list").hasSize(2);
		assertThat(v1.xPath()).isEqualTo("count(/root/some_list)");
	}

	@Test
	void shouldThrowExceptionIfSizeIsWrong() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <some_list>name1</some_list>
				    <some_list>name2</some_list>
				</root>""";
		assertThatThrownBy(() -> XmlAssertion.assertThat(xml).node("root").array("some_list").hasSize(1))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("has size [2] and not [1] for XPath <count(/root/some_list)>");
	}

	@Test
	void shouldReturnZeroIfElementIsMissing() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <some_list>name1</some_list>
				    <some_list>name2</some_list>
				</root>""";
		assertThatThrownBy(() -> XmlAssertion.assertThat(xml).node("root").array("foo").hasSize(1))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("has size [0] and not [1] for XPath <count(/root/foo)>");
	}

}
