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

package sh.stubborn.xmlassert;

import org.junit.jupiter.api.Test;

import static sh.stubborn.xmlassert.XmlAsString.asXml;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Targets {@link XPathAssert}: the success (non-null return) paths of every fluent method
 * and the {@code failWith*} failure paths that raise an {@link AssertionError}.
 */
class XPathAssertTests {

	static final String XML1 = """
			<?xml version="1.0" encoding="UTF-8" ?>
			<some>
			    <nested>
			        <withattr id="a" id2="b">foo</withattr>
			    </nested>
			</some>""";

	static final String XML2 = "<root><property1>a</property1><property2>b</property2></root>";

	static final String XML3 = """
			<?xml version="1.0" encoding="UTF-8" ?>
			<root>
			    <property1>true</property1>
			    <property2 />
			    <property3>false</property3>
			    <property4>5</property4>
			</root>""";

	static final String XML9 = "<root><property1>a</property1><property2>123</property2></root>";

	static final String LIST_XML = """
			<?xml version="1.0" encoding="UTF-8" ?>
			<root>
			    <some_list>name1</some_list>
			    <some_list>name2</some_list>
			</root>""";

	static final String ERRORS = "<root><errors><property>bank_account_number</property></errors></root>";

	// --- success paths ---

	@Test
	void nodeAndIsEqualToStringSucceed() {
		XPathAssert result = XmlAssertions.assertThat(asXml(XML2)).node("root").node("property1").isEqualTo("a");
		assertThat(result).isNotNull();
	}

	@Test
	void nodeVarargsSucceed() {
		XPathAssert result = XmlAssertions.assertThat(asXml(XML1)).node("some", "nested", "withattr").isEqualTo("foo");
		assertThat(result).isNotNull();
	}

	@Test
	void withAttributeSucceeds() {
		XPathAssert result = XmlAssertions.assertThat(asXml(XML1))
			.node("some")
			.node("nested")
			.node("withattr")
			.withAttribute("id", "a")
			.withAttribute("id2", "b");
		assertThat(result).isNotNull();
	}

	@Test
	void arrayHasSizeAndContainsSucceed() {
		XPathAssert size = XmlAssertions.assertThat(asXml(LIST_XML)).node("root").array("some_list").hasSize(2);
		assertThat(size).isNotNull();

		XPathAssert contains = XmlAssertions.assertThat(asXml(ERRORS))
			.node("root")
			.array("errors")
			.contains("property")
			.isEqualTo("bank_account_number");
		assertThat(contains).isNotNull();
	}

	@Test
	void isEqualToNumberSucceeds() {
		XPathAssert result = XmlAssertions.assertThat(asXml(XML3)).node("root").node("property4").isEqualTo(5);
		assertThat(result).isNotNull();
	}

	@Test
	void isEqualToBooleanSucceeds() {
		XPathAssert result = XmlAssertions.assertThat(asXml(XML3)).node("root").node("property3").isEqualTo(false);
		assertThat(result).isNotNull();
	}

	@Test
	void matchesSucceeds() {
		XPathAssert result = XmlAssertions.assertThat(asXml(XML9)).node("root").node("property2").matches("[0-9]{3}");
		assertThat(result).isNotNull();
	}

	@Test
	void isNullSucceeds() {
		XmlAssertions.assertThat(asXml(XML3)).node("root").node("property2").isNull();
	}

	@Test
	void matchesXPathSucceeds() {
		XPathAssert result = XmlAssertions.assertThat(asXml(XML2)).matchesXPath("/root[property1='a']");
		assertThat(result).isNotNull();
	}

	@Test
	void bddThenEntrySucceeds() {
		XPathAssert result = BDDXmlAssertions.then(asXml(XML2)).node("root").node("property1").isEqualTo("a");
		assertThat(result).isNotNull();
	}

	@Test
	void verifiableEntrySucceeds() {
		XmlVerifiable verifiable = XmlAssertion.assertThat(XML2);
		XPathAssert result = XmlAssertions.assertThat(verifiable).node("root").node("property1").isEqualTo("a");
		assertThat(result).isNotNull();
	}

	// --- failure paths ---

	@Test
	void isEqualToStringFailureRaisesAssertionError() {
		assertThatThrownBy(
				() -> XmlAssertions.assertThat(asXml(XML2)).node("root").node("property1").isEqualTo("wrong"))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("Expected XML to match XPath");
	}

	@Test
	void isEqualToNumberFailureRaisesAssertionError() {
		assertThatThrownBy(() -> XmlAssertions.assertThat(asXml(XML3)).node("root").node("property4").isEqualTo(999))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("Expected XML to match XPath");
	}

	@Test
	void isEqualToBooleanFailureRaisesAssertionError() {
		assertThatThrownBy(() -> XmlAssertions.assertThat(asXml(XML3)).node("root").node("property1").isEqualTo(false))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("Expected XML to match XPath");
	}

	@Test
	void matchesFailureRaisesAssertionError() {
		assertThatThrownBy(() -> XmlAssertions.assertThat(asXml(XML2)).node("root").node("property1").matches("[0-9]+"))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("Expected XML to match XPath");
	}

	@Test
	void isNullFailureRaisesAssertionError() {
		assertThatThrownBy(() -> XmlAssertions.assertThat(asXml(XML2)).node("root").node("property1").isNull())
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("Expected XML to match XPath");
	}

	@Test
	void matchesXPathFailureRaisesAssertionError() {
		assertThatThrownBy(
				() -> XmlAssertions.assertThat(asXml(XML2)).matchesXPath("/root/property1[.='non-existing']"))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("to match XPath");
	}

}
