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
 * Mutation-focused tests for {@link XmlAssertion}'s {@code empty(text)} guard: null,
 * empty and whitespace XML must skip parsing while real XML must parse and be assertable.
 */
@SuppressWarnings("NullAway")
class XmlAssertionEmptyMutationTests {

	@Test
	void whitespace_only_xml_is_treated_as_empty_and_does_not_parse() {
		// if empty() stopped matching whitespace, parsing " " would throw
		XmlVerifiable v = XmlAssertion.assertThat("   ").node("foo");
		assertThat(v.xPath()).isEqualTo("/foo");
	}

	@Test
	void empty_string_xml_is_treated_as_empty() {
		XmlVerifiable v = XmlAssertion.assertThat("").node("foo");
		assertThat(v.xPath()).isEqualTo("/foo");
	}

	@Test
	void real_xml_parses_and_supports_assertions() {
		// unique root name to avoid the static parse CACHE being pre-populated by other
		// tests; if empty() wrongly reported this as empty, no document would be parsed
		// and
		// the isEqualTo check would fail with a null document
		XmlVerifiable v = XmlAssertion.assertThat("<uniqueRootZzz><a>1</a></uniqueRootZzz>")
			.node("uniqueRootZzz")
			.node("a")
			.isEqualTo("1");
		assertThat(v.xPath()).isEqualTo("/uniqueRootZzz[a='1']");
	}

	@Test
	void assert_that_xml_string_delegates_to_assert_that() {
		XmlVerifiable v = XmlAssertion.assertThatXml("<uniqueRootYyy><b>2</b></uniqueRootYyy>")
			.node("uniqueRootYyy")
			.node("b")
			.isEqualTo("2");
		assertThat(v.xPath()).isEqualTo("/uniqueRootYyy[b='2']");
	}

}
