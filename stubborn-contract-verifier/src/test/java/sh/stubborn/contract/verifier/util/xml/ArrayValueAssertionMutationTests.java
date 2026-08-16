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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link ArrayValueAssertion}: primitive vs non-primitive
 * equality/matches paths and node traversal inside an array.
 */
class ArrayValueAssertionMutationTests {

	private static final String XML = "<root><arr><n>1</n></arr></root>";

	private static XmlArrayVerifiable array() {
		return XPathBuilder.builder(XML).node("root").array("arr");
	}

	@Test
	void primitive_is_equal_to_string_uses_text_predicate() {
		assertThat(array().isEqualTo("v").xPath()).isEqualTo("/root/arr[text()='v']");
	}

	@Test
	void primitive_is_equal_to_number_uses_number_predicate() {
		assertThat(array().isEqualTo(5).xPath()).isEqualTo("/root/arr[number()=5]");
	}

	@Test
	void primitive_is_equal_to_boolean_uses_text_predicate() {
		assertThat(array().isEqualTo(Boolean.TRUE).xPath()).isEqualTo("/root/arr[text()='true']");
	}

	@Test
	void primitive_matches_uses_matches_on_text() {
		assertThat(array().matches("[0-9]+").xPath()).isEqualTo("/root/arr[matches(text(), '[0-9]+')]");
	}

	@Test
	void array_is_asserting_a_value_in_array() {
		assertThat(((ArrayValueAssertion) array()).isAssertingAValueInArray()).isTrue();
	}

	@Test
	void node_inside_array_extends_path() {
		XmlVerifiable v = array().node("n");
		assertThat(v.xPath()).isEqualTo("/root/arr/n");
		assertThat(((ArrayValueAssertion) v).isAssertingAValueInArray()).isTrue();
	}

	@Test
	void node_with_default_namespace_inside_array() {
		XmlVerifiable v = array().nodeWithDefaultNamespace("n", "");
		assertThat(v.xPath()).isEqualTo("/root/arr/*[local-name()='n']");
	}

	@Test
	void non_primitive_is_equal_to_number_delegates_to_field_predicate() {
		assertThat(array().contains("n").isEqualTo(5).xPath()).isEqualTo("/root/arr[n=5]");
	}

	@Test
	void non_primitive_is_equal_to_boolean_delegates_to_field_predicate() {
		assertThat(array().contains("n").isEqualTo(Boolean.FALSE).xPath()).isEqualTo("/root/arr[n='false']");
	}

	@Test
	void non_primitive_matches_delegates_to_field_predicate() {
		assertThat(array().contains("n").matches("[0-9]+").xPath()).isEqualTo("/root/arr[matches(n, '[0-9]+')]");
	}

}
