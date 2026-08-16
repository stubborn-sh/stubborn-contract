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

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Targets the {@link XmlAsserter} identity/inspection methods (equals, hashCode,
 * toString, isIteratingOverArray, isAssertingAValueInArray, xPath special-case buffer)
 * and the {@code null} handling / dispatch branches of the
 * {@code isEqualTo}/{@code matches} builders.
 */
// Deliberately passes null into @NonNull fluent parameters to exercise the
// null -> isNull() dispatch that several mutations live in.
@SuppressWarnings("NullAway")
class XmlAsserterInternalsTests {

	static final String XML1 = """
			<?xml version="1.0" encoding="UTF-8" ?>
			<some>
			    <nested>
			        <json>with &quot;val&apos;ue</json>
			        <anothervalue>4</anothervalue>
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
			</root>
			""";

	static final String XML9 = "<root><property1>a</property1><property2>123</property2></root>";

	static final String LIST_XML = """
			<?xml version="1.0" encoding="UTF-8" ?>
			<root>
			    <some_list>name1</some_list>
			    <some_list>name2</some_list>
			</root>""";

	private XmlAsserter baseAsserter() {
		return (XmlAsserter) XmlAssertion.assertThat(XML2).node("root");
	}

	@Test
	void equalsShouldCoverEveryBranch() {
		XmlAsserter base = baseAsserter();
		XmlCachedObjects cached = base.cachedObjects;
		XmlAsserterConfiguration cfg = base.xmlAsserterConfiguration;

		FieldAssertion a1 = new FieldAssertion(cached, new LinkedList<String>(List.of("/root")),
				new LinkedList<String>(), "f", cfg);
		FieldAssertion a2 = new FieldAssertion(cached, new LinkedList<String>(List.of("/root")),
				new LinkedList<String>(), "f", cfg);
		FieldAssertion differentBuffer = new FieldAssertion(cached, new LinkedList<String>(List.of("/other")),
				new LinkedList<String>(), "f", cfg);
		FieldAssertion nullName1 = new FieldAssertion(cached, new LinkedList<String>(List.of("/root")),
				new LinkedList<String>(), null, cfg);
		FieldAssertion nullName2 = new FieldAssertion(cached, new LinkedList<String>(List.of("/root")),
				new LinkedList<String>(), null, cfg);
		ArrayValueAssertion differentClass = new ArrayValueAssertion(cached, new LinkedList<String>(List.of("/root")),
				new LinkedList<String>(), "f", cfg);

		assertThat(a1.equals(a1)).isTrue(); // this == o
		assertThat(a1.equals(a2)).isTrue(); // equal buffer + equal non-null fieldName
		assertThat(a1.equals(null)).isFalse(); // o == null
		assertThat(a1.equals("not an asserter")).isFalse(); // different class
		assertThat(a1.equals(differentClass)).isFalse(); // different concrete class
		assertThat(a1.equals(differentBuffer)).isFalse(); // different xPathBuffer
		assertThat(nullName1.equals(nullName2)).isTrue(); // both fieldName null
		assertThat(a1.equals(nullName1)).isFalse(); // non-null vs null fieldName
		assertThat(nullName1.equals(a1)).isFalse(); // null vs non-null fieldName
	}

	@Test
	void hashCodeShouldDependOnBufferAndFieldName() {
		XmlAsserter base = baseAsserter();
		XmlCachedObjects cached = base.cachedObjects;
		XmlAsserterConfiguration cfg = base.xmlAsserterConfiguration;

		int bufferHash = new LinkedList<String>(List.of("/root")).hashCode();

		FieldAssertion withName = new FieldAssertion(cached, new LinkedList<String>(List.of("/root")),
				new LinkedList<String>(), "f", cfg);
		FieldAssertion sameName = new FieldAssertion(cached, new LinkedList<String>(List.of("/root")),
				new LinkedList<String>(), "f", cfg);
		FieldAssertion nullName = new FieldAssertion(cached, new LinkedList<String>(List.of("/root")),
				new LinkedList<String>(), null, cfg);

		assertThat(withName.hashCode()).isEqualTo(31 * bufferHash + "f".hashCode());
		assertThat(nullName.hashCode()).isEqualTo(31 * bufferHash);
		assertThat(withName.hashCode()).isEqualTo(sameName.hashCode());
		assertThat(withName.hashCode()).isNotEqualTo(nullName.hashCode());
	}

	@Test
	void toStringShouldRenderBuffer() {
		XmlAsserter asserter = (XmlAsserter) XmlAssertion.assertThat(XML2).node("root").node("property1");
		assertThat(asserter.toString()).contains("Asserter").contains("xPathBuffer").contains("root");
	}

	@Test
	void iterationFlagsHaveExpectedDefaults() {
		XmlVerifiable field = XmlAssertion.assertThat(XML2).node("root");
		assertThat(field.isIteratingOverArray()).isFalse();
		assertThat(field.isAssertingAValueInArray()).isFalse();

		XmlVerifiable array = XmlAssertion.assertThat(LIST_XML).node("root").array("some_list");
		assertThat(array.isAssertingAValueInArray()).isTrue();
		assertThat(array.isIteratingOverArray()).isFalse();
	}

	@Test
	void xPathReturnsSpecialCaseBufferWhenPresent() {
		XmlVerifiable size = XmlAssertion.assertThat(LIST_XML).node("root").array("some_list").hasSize(2);
		assertThat(size.xPath()).isEqualTo("count(/root/some_list)");
	}

	@Test
	void xPathReturnsRegularBufferWhenNoSpecialCase() {
		XmlVerifiable field = XmlAssertion.assertThat(XML2).node("root").node("property1");
		assertThat(field.xPath()).isEqualTo("/root/property1");
	}

	@Test
	void isEqualToStringNullDelegatesToIsNull() {
		XmlVerifiable verifiable = XmlAssertion.assertThat(XML3)
			.node("root")
			.node("property2")
			.isEqualTo((String) null);
		assertThat(verifiable.xPath()).isEqualTo("not(boolean(/root/property2/text()[1]))");
	}

	@Test
	void isEqualToNumberNullDelegatesToIsNull() {
		XmlVerifiable verifiable = XmlAssertion.assertThat(XML3)
			.node("root")
			.node("property2")
			.isEqualTo((Number) null);
		assertThat(verifiable.xPath()).isEqualTo("not(boolean(/root/property2/text()[1]))");
	}

	@Test
	void isEqualToBooleanNullDelegatesToIsNull() {
		XmlVerifiable verifiable = XmlAssertion.assertThat(XML3)
			.node("root")
			.node("property2")
			.isEqualTo((Boolean) null);
		assertThat(verifiable.xPath()).isEqualTo("not(boolean(/root/property2/text()[1]))");
	}

	@Test
	void isEqualToObjectNullDelegatesToIsNull() {
		XmlVerifiable verifiable = XmlAssertion.assertThat(XML3)
			.node("root")
			.node("property2")
			.isEqualTo((Object) null);
		assertThat(verifiable.xPath()).isEqualTo("not(boolean(/root/property2/text()[1]))");
	}

	@Test
	void matchesNullDelegatesToIsNull() {
		XmlVerifiable verifiable = XmlAssertion.assertThat(XML3).node("root").node("property2").matches(null);
		assertThat(verifiable.xPath()).isEqualTo("not(boolean(/root/property2/text()[1]))");
	}

	@Test
	void isEqualToBooleanNonNullBuildsEquality() {
		XmlVerifiable verifiable = XmlAssertion.assertThat(XML3)
			.node("root")
			.node("property3")
			.isEqualTo(Boolean.FALSE);
		assertThat(verifiable.xPath()).isEqualTo("/root[property3='false']");
	}

	@Test
	void isEqualToObjectDispatchesToNumber() {
		Object value = Integer.valueOf(4);
		XmlVerifiable verifiable = XmlAssertion.assertThat(XML1)
			.node("some")
			.node("nested")
			.node("anothervalue")
			.isEqualTo(value);
		assertThat(verifiable.xPath()).isEqualTo("/some/nested[anothervalue=4]");
	}

	@Test
	void isEqualToObjectDispatchesToBoolean() {
		Object value = Boolean.FALSE;
		XmlVerifiable verifiable = XmlAssertion.assertThat(XML3).node("root").node("property3").isEqualTo(value);
		assertThat(verifiable.xPath()).isEqualTo("/root[property3='false']");
	}

	@Test
	void isEqualToObjectDispatchesToPatternMatches() {
		Object value = Pattern.compile("[0-9]{3}");
		XmlVerifiable verifiable = XmlAssertion.assertThat(XML9).node("root").node("property2").isEqualTo(value);
		assertThat(verifiable.xPath()).isEqualTo("/root[matches(property2, '[0-9]{3}')]");
	}

	@Test
	void isEqualToObjectDispatchesToStringForPlainObject() {
		Object value = "a";
		XmlVerifiable verifiable = XmlAssertion.assertThat(XML2).node("root").node("property1").isEqualTo(value);
		assertThat(verifiable.xPath()).isEqualTo("/root[property1='a']");
	}

	@Test
	void isEqualToShouldThrowWhenXPathNotMatched() {
		assertThatThrownBy(() -> XmlAssertion.assertThat(XML2).node("root").node("property1").isEqualTo("wrong"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("doesn't match the XPath");
	}

	@Test
	void matchesShouldThrowWhenXPathNotMatched() {
		assertThatThrownBy(() -> XmlAssertion.assertThat(XML9).node("root").node("property1").matches("[0-9]+"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("doesn't match the XPath");
	}

	@Test
	void isNullBuildsNotBooleanXPathEvenWhenNodeHasValue() {
		// isNull() builds a not(boolean(...)) XPath which always yields a (non-empty)
		// boolean result, so its own check() never throws - even for a populated node.
		XmlVerifiable verifiable = XmlAssertion.assertThat(XML2).node("root").node("property1").isNull();
		assertThat(verifiable.xPath()).isEqualTo("not(boolean(/root/property1/text()[1]))");
	}

	@Test
	void readReturnsElementTextValue() {
		String value = XPathBuilder.builder(XML2).node("root").node("property1").read();
		assertThat(value).isEqualTo("a");
	}

	@Test
	void readShouldThrowWhenNodeMissing() {
		assertThatThrownBy(() -> XPathBuilder.builder(XML2).node("root").node("missing").read())
			.isInstanceOf(RuntimeException.class);
	}

}
