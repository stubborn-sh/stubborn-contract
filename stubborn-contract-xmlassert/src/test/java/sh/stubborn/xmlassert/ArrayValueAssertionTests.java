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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Targets {@link ArrayValueAssertion}: the primitive vs. non-primitive branch of every
 * {@code isEqualTo}/{@code matches} overload, {@code node} wrapping, {@code hasSize} and
 * the {@code isAssertingAValueInArray} flag.
 */
@SuppressWarnings("NullAway")
class ArrayValueAssertionTests {

	static final String STRING_LIST = """
			<?xml version="1.0" encoding="UTF-8" ?>
			<root>
			    <some_list>name1</some_list>
			    <some_list>name2</some_list>
			</root>""";

	static final String NUMBER_LIST = """
			<?xml version="1.0" encoding="UTF-8" ?>
			<root>
			    <place>
			        <bounding_box>
			            <coordinates>38.995548</coordinates>
			        </bounding_box>
			    </place>
			</root>""";

	static final String AUTHORITIES = """
			<?xml version="1.0" encoding="UTF-8" ?>
			<root>
			    <authorities>ROLE_ADMIN</authorities>
			</root>""";

	static final String BOOL_LIST = "<root><flag>true</flag></root>";

	static final String ERRORS = "<root><errors><property>bank_account_number</property></errors></root>";

	static final String NESTED_NUMBER = "<root><list><num>5</num></list></root>";

	static final String NESTED_BOOL = "<root><list><flag>true</flag></list></root>";

	static final String NESTED_MATCH = "<root><list><val>123</val></list></root>";

	static final String NESTED_NODES = "<root><list><a><b>v</b></a></list></root>";

	// --- primitive (checkingPrimitiveType == true) ---

	@Test
	void primitiveIsEqualToStringUsesTextPredicate() {
		XmlVerifiable verifiable = XmlAssertion.assertThat(STRING_LIST)
			.node("root")
			.array("some_list")
			.isEqualTo("name1");
		assertThat(verifiable.xPath()).isEqualTo("/root/some_list[text()='name1']");
	}

	@Test
	void primitiveIsEqualToNumberUsesNumberPredicate() {
		XmlVerifiable verifiable = XmlAssertion.assertThat(NUMBER_LIST)
			.node("root")
			.node("place")
			.node("bounding_box")
			.array("coordinates")
			.isEqualTo(38.995548);
		assertThat(verifiable.xPath()).isEqualTo("/root/place/bounding_box/coordinates[number()=38.995548]");
	}

	@Test
	void primitiveMatchesUsesTextPredicate() {
		XmlVerifiable verifiable = XmlAssertion.assertThat(AUTHORITIES)
			.node("root")
			.array("authorities")
			.matches("^[a-zA-Z0-9_\\- ]+$");
		assertThat(verifiable.xPath()).isEqualTo("/root/authorities[matches(text(), '^[a-zA-Z0-9_\\- ]+$')]");
	}

	@Test
	void primitiveIsEqualToBooleanUsesTextPredicate() {
		XmlVerifiable verifiable = XmlAssertion.assertThat(BOOL_LIST).node("root").array("flag").isEqualTo(true);
		assertThat(verifiable.xPath()).isEqualTo("/root/flag[text()='true']");
	}

	// --- non-primitive (checkingPrimitiveType == false, after contains) ---

	@Test
	void nonPrimitiveIsEqualToStringDelegatesToSuper() {
		XmlVerifiable verifiable = XmlAssertion.assertThat(ERRORS)
			.node("root")
			.array("errors")
			.contains("property")
			.isEqualTo("bank_account_number");
		assertThat(verifiable.xPath()).isEqualTo("/root/errors[property='bank_account_number']");
	}

	@Test
	void nonPrimitiveIsEqualToNumberDelegatesToSuper() {
		XmlVerifiable verifiable = XmlAssertion.assertThat(NESTED_NUMBER)
			.node("root")
			.array("list")
			.contains("num")
			.isEqualTo(5);
		assertThat(verifiable.xPath()).isEqualTo("/root/list[num=5]");
	}

	@Test
	void nonPrimitiveIsEqualToBooleanDelegatesToSuper() {
		XmlVerifiable verifiable = XmlAssertion.assertThat(NESTED_BOOL)
			.node("root")
			.array("list")
			.contains("flag")
			.isEqualTo(true);
		assertThat(verifiable.xPath()).isEqualTo("/root/list[flag='true']");
	}

	@Test
	void nonPrimitiveMatchesDelegatesToSuper() {
		XmlVerifiable verifiable = XmlAssertion.assertThat(NESTED_MATCH)
			.node("root")
			.array("list")
			.contains("val")
			.matches("[0-9]+");
		assertThat(verifiable.xPath()).isEqualTo("/root/list[matches(val, '[0-9]+')]");
	}

	// --- node wrapping on an array ---

	@Test
	void nodeSingleOnArrayKeepsBuildingXPath() {
		XmlVerifiable verifiable = XmlAssertion.assertThat(NESTED_NODES).node("root").array("list").node("a").node("b");
		assertThat(verifiable.xPath()).isEqualTo("/root/list/a/b");
		assertThat(verifiable.isAssertingAValueInArray()).isTrue();
	}

	@Test
	void nodeVarargsOnArrayKeepsBuildingXPath() {
		XmlVerifiable verifiable = XmlAssertion.assertThat(NESTED_NODES).node("root").array("list").node("a", "b");
		assertThat(verifiable.xPath()).isEqualTo("/root/list/a/b");
	}

	// --- hasSize ---

	@Test
	void hasSizeMatchingReturnsCountVerifiable() {
		XmlVerifiable verifiable = XmlAssertion.assertThat(STRING_LIST).node("root").array("some_list").hasSize(2);
		assertThat(verifiable.xPath()).isEqualTo("count(/root/some_list)");
	}

	@Test
	void hasSizeMismatchThrows() {
		assertThatThrownBy(() -> XmlAssertion.assertThat(STRING_LIST).node("root").array("some_list").hasSize(1))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("has size [2] and not [1]");
	}

	@Test
	void containsReturnsArrayVerifiable() {
		XmlArrayVerifiable array = (XmlArrayVerifiable) XmlAssertion.assertThat(ERRORS).node("root").array("errors");
		XmlVerifiable contained = array.contains("property");
		assertThat(contained.isAssertingAValueInArray()).isTrue();
	}

	@Test
	void primitiveIsEqualToNullBooleanBuildsTextPredicate() {
		// On a primitive array (checkingPrimitiveType == true) a null Boolean flows
		// through
		// isEqualTo(String.valueOf(value)) == isEqualTo("null") and builds a
		// [text()='null']
		// predicate. If the checkingPrimitiveType branch is negated it would instead
		// route to
		// super.isEqualTo(Boolean) -> isNull(), producing a not(boolean(...)) expression.
		XmlVerifiable verifiable = XPathBuilder.builder("<root><x>1</x></root>")
			.node("root")
			.array("x")
			.isEqualTo((Boolean) null);
		assertThat(verifiable.xPath()).isEqualTo("/root/x[text()='null']");
	}

}
