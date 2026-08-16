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

package sh.stubborn.contract.spec.internal;

import java.util.regex.Pattern;

import org.codehaus.groovy.runtime.GStringImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused coverage for {@link GroovyDslPropertyConverter}.
 */
@SuppressWarnings("NullAway")
class GroovyDslPropertyConverterTests {

	private final GroovyDslPropertyConverter converter = new GroovyDslPropertyConverter();

	@Test
	void testSideReturnsTheSameObjectForANonGStringInput() {
		Object input = "plain string";
		assertThat(this.converter.testSide(input)).isSameAs(input);
	}

	@Test
	void testSideReturnsTheSameGStringWhenItHoldsNoRegexProperty() {
		GStringImpl gstring = new GStringImpl(new Object[] { "x" }, new String[] { "a", "b" });
		assertThat(this.converter.testSide(gstring)).isSameAs(gstring);
	}

	@Test
	void testSideConvertsAGStringHoldingARegexPropertyIntoAUrlWithTheGeneratedValue() {
		RegexProperty regexProperty = new RegexProperty(Pattern.compile("[0]"));
		GStringImpl gstring = new GStringImpl(new Object[] { regexProperty }, new String[] { "/foo/", "" });
		Object result = this.converter.testSide(gstring);
		assertThat(result).isInstanceOf(Url.class);
		Url url = (Url) result;
		assertThat(url.getClientValue()).isNotNull();
		assertThat(url.getClientValue().toString()).isEqualTo("/foo/0");
	}

}
