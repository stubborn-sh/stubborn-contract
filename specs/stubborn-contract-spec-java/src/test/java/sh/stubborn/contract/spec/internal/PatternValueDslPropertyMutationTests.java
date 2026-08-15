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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NullAway")
class PatternValueDslPropertyMutationTests {

	private final TestPatternValueDslProperty property = new TestPatternValueDslProperty();

	private void assertPatternAndGeneratedValue(DslProperty result, Pattern expectedPattern) {
		assertThat(result.getClientValue()).isInstanceOf(Pattern.class);
		assertThat(((Pattern) result.getClientValue()).pattern()).isEqualTo(expectedPattern.pattern());
		assertThat(String.valueOf(result.getServerValue())).matches(expectedPattern.pattern());
	}

	@Test
	void anyAlphaUnicode() {
		assertPatternAndGeneratedValue(this.property.anyAlphaUnicode(), RegexPatterns.ONLY_ALPHA_UNICODE);
	}

	@Test
	void anyAlphaNumeric() {
		assertPatternAndGeneratedValue(this.property.anyAlphaNumeric(), RegexPatterns.ALPHA_NUMERIC);
	}

	@Test
	void anyNumber() {
		assertPatternAndGeneratedValue(this.property.anyNumber(), RegexPatterns.NUMBER);
	}

	@Test
	void anyInteger() {
		assertPatternAndGeneratedValue(this.property.anyInteger(), RegexPatterns.INTEGER);
	}

	@Test
	void anyPositiveInt() {
		assertPatternAndGeneratedValue(this.property.anyPositiveInt(), RegexPatterns.POSITIVE_INT);
	}

	@Test
	void anyDouble() {
		assertPatternAndGeneratedValue(this.property.anyDouble(), RegexPatterns.DOUBLE);
	}

	@Test
	void anyHex() {
		assertPatternAndGeneratedValue(this.property.anyHex(), RegexPatterns.HEX);
	}

	@Test
	void aBoolean() {
		assertPatternAndGeneratedValue(this.property.aBoolean(), RegexPatterns.TRUE_OR_FALSE);
	}

	@Test
	void anyIpAddress() {
		assertPatternAndGeneratedValue(this.property.anyIpAddress(), RegexPatterns.IP_ADDRESS);
	}

	@Test
	void anyHostname() {
		assertPatternAndGeneratedValue(this.property.anyHostname(), RegexPatterns.HOSTNAME_PATTERN);
	}

	@Test
	void anyEmail() {
		assertPatternAndGeneratedValue(this.property.anyEmail(), RegexPatterns.EMAIL);
	}

	@Test
	void anyUrl() {
		assertPatternAndGeneratedValue(this.property.anyUrl(), RegexPatterns.URL);
	}

	@Test
	void anyHttpsUrl() {
		assertPatternAndGeneratedValue(this.property.anyHttpsUrl(), RegexPatterns.HTTPS_URL);
	}

	@Test
	void anyUuid() {
		assertPatternAndGeneratedValue(this.property.anyUuid(), RegexPatterns.UUID);
	}

	@Test
	void anyDate() {
		assertPatternAndGeneratedValue(this.property.anyDate(), RegexPatterns.ANY_DATE);
	}

	@Test
	void anyDateTime() {
		assertPatternAndGeneratedValue(this.property.anyDateTime(), RegexPatterns.ANY_DATE_TIME);
	}

	@Test
	void anyTime() {
		assertPatternAndGeneratedValue(this.property.anyTime(), RegexPatterns.ANY_TIME);
	}

	@Test
	void anyIso8601WithOffset() {
		assertPatternAndGeneratedValue(this.property.anyIso8601WithOffset(), RegexPatterns.ISO8601_WITH_OFFSET);
	}

	@Test
	void anyNonBlankString() {
		assertPatternAndGeneratedValue(this.property.anyNonBlankString(), RegexPatterns.NON_BLANK);
	}

	@Test
	void anyNonEmptyString() {
		assertPatternAndGeneratedValue(this.property.anyNonEmptyString(), RegexPatterns.NON_EMPTY);
	}

	@Test
	void anyOfPicksOneOfValues() {
		DslProperty result = this.property.anyOf("aaa", "bbb");
		assertThat(String.valueOf(result.getServerValue())).isIn("aaa", "bbb");
	}

	private static final class TestPatternValueDslProperty extends PatternValueDslProperty<ClientDslProperty> {

		@Override
		protected ClientDslProperty createProperty(Pattern pattern, @Nullable Object generatedValue) {
			return new ClientDslProperty(pattern, generatedValue);
		}

	}

}
