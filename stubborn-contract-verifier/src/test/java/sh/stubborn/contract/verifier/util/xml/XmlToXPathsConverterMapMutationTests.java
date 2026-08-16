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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.internal.BodyMatcher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link XmlToXPathsConverter#mapToMatchers}, pinning the
 * exact XPath and value produced for value nodes, attributes, repeated elements, explicit
 * namespaces and default namespaces.
 */
class XmlToXPathsConverterMapMutationTests {

	private static List<String> matchers(String xml) {
		List<String> result = new ArrayList<>();
		for (BodyMatcher m : XmlToXPathsConverter.mapToMatchers(xml)) {
			result.add(m.path() + " = " + m.value());
		}
		return result;
	}

	@Test
	void simple_value_nodes() {
		assertThat(matchers("<root><a>1</a><b>two</b></root>")).containsExactlyInAnyOrder("/root/a/text() = 1",
				"/root/b/text() = two");
	}

	@Test
	void nested_value_node() {
		assertThat(matchers("<root><outer><inner>val</inner></outer></root>"))
			.containsExactly("/root/outer/inner/text() = val");
	}

	@Test
	void attribute_node_produces_at_path() {
		assertThat(matchers("<root><item id=\"7\">text</item></root>"))
			.containsExactlyInAnyOrder("/root/item/text() = text", "/root/item/@id = 7");
	}

	@Test
	void repeated_elements_get_positional_index() {
		assertThat(matchers("<root><item>a</item><item>b</item></root>"))
			.containsExactlyInAnyOrder("/root/item/text() = a", "/root/item[2]/text() = b");
	}

	@Test
	void explicit_namespace_is_kept_and_namespace_attribute_emitted() {
		assertThat(matchers("<ns:root xmlns:ns=\"urn:x\"><ns:child>c</ns:child></ns:root>"))
			.containsExactlyInAnyOrder("/ns:root/ns:child/text() = c", "/ns:root/namespace::ns = urn:x");
	}

	@Test
	void default_namespace_uses_local_name_syntax_and_skips_xmlns_attribute() {
		assertThat(matchers("<root xmlns=\"urn:default\"><child>c</child></root>")).containsExactly(
				"/*[local-name()='root' and namespace-uri()='urn:default']/*[local-name()='child']/text() = c");
	}

	@Test
	void buildXPath_of_null_or_empty_is_empty_string() {
		assertThat(XmlToXPathsConverter.buildXPath(null)).isEmpty();
		assertThat(XmlToXPathsConverter.buildXPath(new ArrayList<>())).isEmpty();
		assertThat(XmlToXPathsConverter.buildXPath(new ArrayList<>(), 2)).isEmpty();
	}

}
