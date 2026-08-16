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

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link XmlAsserter}: the generic {@code isEqualTo(Object)}
 * dispatch, default-namespace / attribute node building, and the equality/hashCode/string
 * contract.
 */
class XmlAsserterMutationTests {

	private static final String XML = "<root><a>5</a></root>";

	private static XmlVerifiable node() {
		return XPathBuilder.builder(XML).node("root").node("a");
	}

	@Test
	void isEqualTo_object_dispatches_to_number_overload() {
		XmlVerifiable v = node().isEqualTo((Object) Integer.valueOf(5));
		assertThat(v.xPath()).isEqualTo("/root[a=5]");
	}

	@Test
	void isEqualTo_object_dispatches_to_boolean_overload() {
		XmlVerifiable v = node().isEqualTo((Object) Boolean.TRUE);
		assertThat(v.xPath()).isEqualTo("/root[a='true']");
	}

	@Test
	void isEqualTo_object_dispatches_to_pattern_matches() {
		XmlVerifiable v = node().isEqualTo((Object) Pattern.compile("[0-9]+"));
		assertThat(v.xPath()).isEqualTo("/root[matches(a, '[0-9]+')]");
	}

	@Test
	void isEqualTo_object_dispatches_to_string_overload() {
		XmlVerifiable v = node().isEqualTo((Object) "hello");
		assertThat(v.xPath()).isEqualTo("/root[a='hello']");
	}

	@Test
	void isEqualTo_object_null_becomes_is_null() {
		XmlVerifiable v = node().isEqualTo((Object) null);
		assertThat(v.xPath()).isEqualTo("not(boolean(/root/a/text()[1]))");
	}

	@Test
	void isEqualTo_boolean_null_becomes_is_null() {
		XmlVerifiable v = node().isEqualTo((Boolean) null);
		assertThat(v.xPath()).isEqualTo("not(boolean(/root/a/text()[1]))");
	}

	@Test
	void isEqualTo_boolean_value_is_quoted() {
		XmlVerifiable v = node().isEqualTo(Boolean.FALSE);
		assertThat(v.xPath()).isEqualTo("/root[a='false']");
	}

	@Test
	void isEqualTo_string_null_becomes_is_null() {
		XmlVerifiable v = node().isEqualTo((String) null);
		assertThat(v.xPath()).isEqualTo("not(boolean(/root/a/text()[1]))");
	}

	@Test
	void isNull_builds_not_boolean_expression() {
		XmlVerifiable v = node().isNull();
		assertThat(v.xPath()).isEqualTo("not(boolean(/root/a/text()[1]))");
	}

	@Test
	void matches_null_becomes_is_null() {
		XmlVerifiable v = node().matches(null);
		assertThat(v.xPath()).isEqualTo("not(boolean(/root/a/text()[1]))");
	}

	@Test
	void node_with_default_namespace_uses_local_name_and_namespace_uri() {
		XmlVerifiable v = XPathBuilder.builder("<root/>").nodeWithDefaultNamespace("book", "urn:books");
		assertThat(v.xPath()).isEqualTo("/*[local-name()='book' and namespace-uri()='urn:books']");
	}

	@Test
	void node_with_blank_default_namespace_omits_namespace_uri() {
		XmlVerifiable v = XPathBuilder.builder("<root/>").nodeWithDefaultNamespace("book", "");
		assertThat(v.xPath()).isEqualTo("/*[local-name()='book']");
	}

	@Test
	void with_attribute_name_only_uses_at_syntax() {
		XmlVerifiable v = XPathBuilder.builder(XML).node("root").withAttribute("id");
		assertThat(v.xPath()).isEqualTo("/root/@id");
	}

	@Test
	void with_attribute_xmlns_prefix_uses_namespace_axis() {
		XmlVerifiable v = XPathBuilder.builder(XML).node("root").withAttribute("xmlns:foo");
		assertThat(v.xPath()).isEqualTo("/root/namespace::foo");
	}

	@Test
	void base_asserter_is_not_iterating_or_asserting_value_in_array() {
		XmlAsserter v = (XmlAsserter) node();
		assertThat(v.isIteratingOverArray()).isFalse();
		assertThat(v.isAssertingAValueInArray()).isFalse();
	}

	@Test
	void equals_is_reflexive_null_type_and_buffer_and_field_based() {
		XmlVerifiable a = XPathBuilder.builder(XML).node("root").node("a");
		XmlVerifiable aAgain = XPathBuilder.builder(XML).node("root").node("a");
		XmlVerifiable b = XPathBuilder.builder(XML).node("root").node("b");

		assertThat(a).isEqualTo(a);
		assertThat(a).isEqualTo(aAgain);
		assertThat(a).isNotEqualTo(b);
		assertThat(a.equals(null)).isFalse();
		assertThat(a.equals("string")).isFalse();
	}

	@Test
	void hashCode_combines_buffer_and_field_name() {
		XmlAsserter a = (XmlAsserter) XPathBuilder.builder(XML).node("root").node("a");
		int expected = 31 * a.xPathBuffer.hashCode() + a.fieldName.hashCode();
		assertThat(a.hashCode()).isEqualTo(expected);
		assertThat(a.hashCode()).isNotZero();

		XmlAsserter aAgain = (XmlAsserter) XPathBuilder.builder(XML).node("root").node("a");
		assertThat(a.hashCode()).isEqualTo(aAgain.hashCode());
	}

	@Test
	void toString_contains_the_xpath_buffer() {
		XmlAsserter a = (XmlAsserter) XPathBuilder.builder(XML).node("root").node("a");
		assertThat(a.toString()).contains("Asserter{").contains("xPathBuffer=").contains("root");
	}

}
