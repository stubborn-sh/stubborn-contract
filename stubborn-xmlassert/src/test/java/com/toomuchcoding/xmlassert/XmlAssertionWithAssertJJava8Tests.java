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

import static com.toomuchcoding.xmlassert.BDDXmlAssertions.then;
import static com.toomuchcoding.xmlassert.XmlAsString.asXml;
import static com.toomuchcoding.xmlassert.XmlAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * @author Marcin Grzejszczak
 */
class XmlAssertionWithAssertJJava8Tests {

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

	@Test
	void shouldConvertXmlWithMapAsRootToMapOfPathToValue() {
		assertThat(new XmlAsString(XML1)).node("some").node("nested").node("anothervalue").isEqualTo(4);
		assertThat(new XmlAsString(XML1)).node("some").node("nested").node("anothervalue");
		assertThat(new XmlAsString(XML1)).node("some")
			.node("nested")
			.node("withattr")
			.withAttribute("id", "a")
			.withAttribute("id2", "b");
		assertThat(new XmlAsString(XML1)).node("some")
			.node("nested")
			.node("withattr")
			.isEqualTo("foo")
			.withAttribute("id", "a")
			.withAttribute("id2", "b");
		then(new XmlAsString(XML1)).node("some").node("nested").node("anothervalue").isEqualTo(4);
		then(new XmlAsString(XML1)).node("some").node("nested").node("anothervalue").isEqualTo(4);
		assertThat(new XmlAsString(XML1)).node("some")
			.node("nested")
			.array("withlist")
			.contains("name")
			.isEqualTo("name1");
		assertThat(new XmlAsString(XML1)).node("some")
			.node("nested")
			.array("withlist")
			.contains("name")
			.isEqualTo("name2");
		assertThat(new XmlAsString(XML1)).node("some")
			.node("nested")
			.array("withlist")
			.contains("name")
			.isEqualTo("name3")
			.withAttribute("id", "10")
			.withAttribute("surname", "kowalski");
		assertThat(new XmlAsString(XML1)).node("some").node("nested").array("withlist").isEqualTo(8);
		assertThat(new XmlAsString(XML1)).node("some").node("nested").node("json").isEqualTo("with \"val'ue");
		assertThat(asXml(XML1)).node("some", "nested", "json").isEqualTo("with \"val'ue");
	}

	@Test
	void shouldGenerateAssertionsForSimpleResponseBody() {
		assertThat(asXml(XML2)).node("root").node("property1").isEqualTo("a");
		assertThat(asXml(XML2)).node("root").node("property2").isEqualTo("b");
	}

	@Test
	void shouldGenerateAssertionsForNullAndBooleanValues() {
		assertThat(asXml(XML3)).node("root").node("property1").isEqualTo("true");
		assertThat(asXml(XML3)).node("root").node("property2").isNull();
		assertThat(asXml(XML3)).node("root").node("property3").isEqualTo(false);
		assertThat(asXml(XML3)).node("root").node("property4").isEqualTo(5);
	}

	@Test
	void shouldGenerateAssertionsForSimpleResponseBodyConstructedFromMapWithList() {
		assertThat(asXml(XML4)).node("root").node("property1").isEqualTo("a");
		assertThat(asXml(XML4)).node("root").array("property2").contains("a").isEqualTo("sth");
		assertThat(asXml(XML4)).node("root").array("property2").contains("b").isEqualTo("sthElse");
	}

	@Test
	void shouldGenerateAssertionsForArrayInsideResponseBodyElement() {
		assertThat(asXml(XML7)).node("root").array("property1").contains("property2").isEqualTo("test1");
		assertThat(asXml(XML7)).node("root").array("property1").contains("property3").isEqualTo("test2");
	}

	@Test
	void shouldGenerateAssertionsForNestedObjectsInResponseBody() {
		assertThat(asXml(XML8)).node("root").node("property2").node("property3").isEqualTo("b");
		assertThat(asXml(XML8)).node("root").node("property1").isEqualTo("a");
	}

	@Test
	void shouldGenerateRegexAssertionsForMapObjectsInResponseBody() {
		assertThat(asXml(XML9)).node("root").node("property2").matches("[0-9]{3}");
		assertThat(asXml(XML9)).node("root").node("property1").isEqualTo("a");
	}

	@Test
	void shouldGenerateEscapedRegexAssertionsForStringObjectsInResponseBody() {
		String xml = "<root><property2>123123</property2></root>";
		assertThat(asXml(xml)).node("root").node("property2").matches("\\d+");
	}

	@Test
	void shouldWorkWithMoreComplexStuffAndXPaths() {
		assertThat(asXml(XML10)).node("root").array("errors").contains("property").isEqualTo("bank_account_number");
		assertThat(asXml(XML10)).node("root").array("errors").contains("message").isEqualTo("incorrect_format");
	}

	@Test
	void shouldManageToParseDoubleArray() {
		assertThat(asXml(XML11)).node("root")
			.node("place")
			.node("bounding_box")
			.array("coordinates")
			.isEqualTo(38.995548);
		assertThat(asXml(XML11)).node("root")
			.node("place")
			.node("bounding_box")
			.array("coordinates")
			.isEqualTo(-77.119759);
		assertThat(asXml(XML11)).node("root")
			.node("place")
			.node("bounding_box")
			.array("coordinates")
			.isEqualTo(-76.909393);
		assertThat(asXml(XML11)).node("root")
			.node("place")
			.node("bounding_box")
			.array("coordinates")
			.isEqualTo(38.791645);
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
		assertThat(asXml(xml)).matchesXPath("/root/property2[property3='b']");
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
		assertThatThrownBy(() -> assertThat(asXml(xml)).matchesXPath("/root/property2[property3='non-existing']"))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("Expected XML [")
			.hasMessageContaining("to match XPath");
	}

	@Test
	void shouldGenerateEscapedRegexAssertionsForTextWithRegularExpressionValues() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <property1>&apos;&quot;&lt;&gt;[]()</property1>
				</root>""";
		assertThat(asXml(xml)).node("root").node("property1").matches("'\"<>\\[\\]\\(\\)");
	}

	@Test
	void shouldEscapeRegularExpressionProperly() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <path>/api/12</path>
				    <correlationId>123456</correlationId>
				</root>""";
		assertThat(asXml(xml)).node("root").node("path").matches("^/api/[0-9]{2}$");
	}

	@Test
	void shouldEscapeSingleQuotesInQuotedString() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <text>text with &apos;quotes&apos; inside</text>
				</root>
				""";
		assertThat(asXml(xml)).node("root").node("text").isEqualTo("text with 'quotes' inside");
	}

	@Test
	void shouldEscapeBracketsInString() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <id>&lt;escape me&gt;</id>
				</root>
				""";
		assertThat(asXml(xml)).node("root").node("id").isEqualTo("<escape me>");
	}

	@Test
	void shouldEscapeDoubleQuotesInQuotedString() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <text>text with &quot;quotes&quot; inside</text>
				</root>
				""";
		assertThat(asXml(xml)).node("root").node("text").isEqualTo("text with \"quotes\" inside");
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
		assertThat(asXml(xml)).node("root")
			.array("elements")
			.array("partners")
			.contains("payment_methods")
			.isEqualTo("BANK");
	}

	@Test
	void shouldMatchPatternInArray() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <authorities>ROLE_ADMIN</authorities>
				</root>
				""";
		assertThat(asXml(xml)).node("root").array("authorities").matches("^[a-zA-Z0-9_\\- ]+$");
	}

	@Test
	void shouldManageToParseArrayWithStringValues() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <some_list>name1</some_list>
				    <some_list>name2</some_list>
				</root>""";
		assertThat(asXml(xml)).node("root").array("some_list").isEqualTo("name1");
		assertThat(asXml(xml)).node("root").array("some_list").isEqualTo("name2");
	}

	@Test
	void shouldAllowNestedCallsWithCountingElementsSize() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <some_list>name1</some_list>
				    <some_list>name2</some_list>
				</root>""";
		assertThat(asXml(xml)).node("root").array("some_list").hasSize(2).isEqualTo("name1");
	}

	@Test
	void shouldCountElementsSize() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <some_list>name1</some_list>
				    <some_list>name2</some_list>
				</root>""";
		assertThat(asXml(xml)).node("root").array("some_list").hasSize(2);
	}

	@Test
	void shouldThrowExceptionIfSizeIsWrong() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8" ?>
				<root>
				    <some_list>name1</some_list>
				    <some_list>name2</some_list>
				</root>""";
		assertThatThrownBy(() -> assertThat(asXml(xml)).node("root").array("some_list").hasSize(1))
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
		assertThatThrownBy(() -> assertThat(asXml(xml)).node("root").array("foo").hasSize(1))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("has size [0] and not [1] for XPath <count(/root/foo)>");
	}

}
