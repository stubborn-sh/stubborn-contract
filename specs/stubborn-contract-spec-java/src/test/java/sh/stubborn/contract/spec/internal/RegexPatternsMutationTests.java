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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NullAway")
class RegexPatternsMutationTests {

	@Test
	void onlyAlphaUnicode() {
		RegexProperty property = RegexPatterns.onlyAlphaUnicode();
		assertThat(property.pattern()).isEqualTo(RegexPatterns.ONLY_ALPHA_UNICODE.pattern());
		assertThat(property.getClazz()).isEqualTo(String.class);
		assertThat("abc").matches(property.pattern());
	}

	@Test
	void alphaNumeric() {
		RegexProperty property = RegexPatterns.alphaNumeric();
		assertThat(property.pattern()).isEqualTo(RegexPatterns.ALPHA_NUMERIC.pattern());
		assertThat("abc123").matches(property.pattern());
		assertThat("!!!").doesNotMatch(property.pattern());
	}

	@Test
	void number() {
		RegexProperty property = RegexPatterns.number();
		assertThat(property.pattern()).isEqualTo(RegexPatterns.NUMBER.pattern());
		assertThat(property.getClazz()).isEqualTo(Double.class);
		assertThat("123").matches(property.pattern());
	}

	@Test
	void positiveInt() {
		RegexProperty property = RegexPatterns.positiveInt();
		assertThat(property.pattern()).isEqualTo(RegexPatterns.POSITIVE_INT.pattern());
		assertThat(property.getClazz()).isEqualTo(Integer.class);
		assertThat("123").matches(property.pattern());
		assertThat("0").doesNotMatch(property.pattern());
	}

	@Test
	void anyBoolean() {
		RegexProperty property = RegexPatterns.anyBoolean();
		assertThat(property.pattern()).isEqualTo(RegexPatterns.TRUE_OR_FALSE.pattern());
		assertThat(property.getClazz()).isEqualTo(Boolean.class);
		assertThat("true").matches(property.pattern());
		assertThat("false").matches(property.pattern());
	}

	@Test
	void anInteger() {
		RegexProperty property = RegexPatterns.anInteger();
		assertThat(property.pattern()).isEqualTo(RegexPatterns.INTEGER.pattern());
		assertThat(property.getClazz()).isEqualTo(Integer.class);
		assertThat("-45").matches(property.pattern());
	}

	@Test
	void aDouble() {
		RegexProperty property = RegexPatterns.aDouble();
		assertThat(property.pattern()).isEqualTo(RegexPatterns.DOUBLE.pattern());
		assertThat(property.getClazz()).isEqualTo(Double.class);
		assertThat("1.5").matches(property.pattern());
	}

	@Test
	void ipAddress() {
		RegexProperty property = RegexPatterns.ipAddress();
		assertThat(property.pattern()).isEqualTo(RegexPatterns.IP_ADDRESS.pattern());
		assertThat("192.168.0.1").matches(property.pattern());
	}

	@Test
	void hostname() {
		RegexProperty property = RegexPatterns.hostname();
		assertThat(property.pattern()).isEqualTo(RegexPatterns.HOSTNAME_PATTERN.pattern());
		assertThat("https://foo.com").matches(property.pattern());
	}

	@Test
	void email() {
		RegexProperty property = RegexPatterns.email();
		assertThat(property.pattern()).isEqualTo(RegexPatterns.EMAIL.pattern());
		assertThat("foo@bar.com").matches(property.pattern());
	}

	@Test
	void url() {
		RegexProperty property = RegexPatterns.url();
		assertThat(property.pattern()).isEqualTo(RegexPatterns.URL.pattern());
	}

	@Test
	void httpsUrl() {
		RegexProperty property = RegexPatterns.httpsUrl();
		assertThat(property.pattern()).isEqualTo(RegexPatterns.HTTPS_URL.pattern());
	}

	@Test
	void uuid() {
		RegexProperty property = RegexPatterns.uuid();
		assertThat(property.pattern()).isEqualTo(RegexPatterns.UUID.pattern());
		assertThat("9f6e4b5c-1a2b-3c4d-5e6f-7a8b9c0d1e2f").matches(property.pattern());
	}

	@Test
	void uuid4() {
		RegexProperty property = RegexPatterns.uuid4();
		assertThat(property.pattern()).isEqualTo(RegexPatterns.UUID4.pattern());
	}

	@Test
	void isoDate() {
		RegexProperty property = RegexPatterns.isoDate();
		assertThat(property.pattern()).isEqualTo(RegexPatterns.ANY_DATE.pattern());
		assertThat("2020-01-02").matches(property.pattern());
	}

	@Test
	void isoDateTime() {
		RegexProperty property = RegexPatterns.isoDateTime();
		assertThat(property.pattern()).isEqualTo(RegexPatterns.ANY_DATE_TIME.pattern());
		assertThat("2020-01-02T12:23:34").matches(property.pattern());
	}

	@Test
	void isoTime() {
		RegexProperty property = RegexPatterns.isoTime();
		assertThat(property.pattern()).isEqualTo(RegexPatterns.ANY_TIME.pattern());
		assertThat("12:23:34").matches(property.pattern());
	}

	@Test
	void iso8601WithOffset() {
		RegexProperty property = RegexPatterns.iso8601WithOffset();
		assertThat(property.pattern()).isEqualTo(RegexPatterns.ISO8601_WITH_OFFSET.pattern());
		assertThat("2020-01-02T12:23:34.123Z").matches(property.pattern());
	}

	@Test
	void nonEmpty() {
		RegexProperty property = RegexPatterns.nonEmpty();
		assertThat(property.pattern()).isEqualTo(RegexPatterns.NON_EMPTY.pattern());
		assertThat("x").matches(property.pattern());
	}

	@Test
	void nonBlank() {
		RegexProperty property = RegexPatterns.nonBlank();
		assertThat(property.pattern()).isEqualTo(RegexPatterns.NON_BLANK.pattern());
	}

	@Test
	void anyOfBuildsAlternation() {
		Pattern pattern = RegexPatterns.anyOf("a", "b");
		assertThat("a").matches(pattern.pattern());
		assertThat("b").matches(pattern.pattern());
		assertThat("c").doesNotMatch(pattern.pattern());
	}

	@Test
	void multipartParamContainsNameAndValue() {
		String result = RegexPatterns.multipartParam("field", "value");
		assertThat(result).contains("name=\"field\"").contains("value");
	}

	@Test
	void multipartFileWithStringContentType() {
		String result = RegexPatterns.multipartFile("field", "file.txt", "content", "text/plain");
		assertThat(result).contains("name=\"field\"")
			.contains("filename=\"file.txt\"")
			.contains("content")
			.contains("text/plain");
	}

	@Test
	void multipartFileWithNullContentTypeUsesWildcard() {
		String result = RegexPatterns.multipartFile("field", "file.txt", "content", null);
		assertThat(result).contains("Content-Type: .*");
	}

	@Test
	void multipartFileWithRegexPropertyContentType() {
		RegexProperty regex = new RegexProperty(Pattern.compile("text/.*"));
		String result = RegexPatterns.multipartFile("field", "file.txt", "content", regex);
		assertThat(result).contains("text/.*");
	}

	@Test
	void constructorLogsWarningWithoutFailing() {
		assertThat(new RegexPatterns()).isNotNull();
	}

}
