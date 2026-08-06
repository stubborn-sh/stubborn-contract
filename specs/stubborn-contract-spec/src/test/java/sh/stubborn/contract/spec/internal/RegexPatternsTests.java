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

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marcin Grzejszczak
 */
class RegexPatternsTests {

	@ParameterizedTest
	@MethodSource("provideIpAddresses")
	void shouldGenerateARegexForIpAddress(String textToMatch, boolean shouldMatch) {
		assertThat(RegexPatterns.ipAddress().matcher(textToMatch).matches()).isEqualTo(shouldMatch);
	}

	static Stream<Arguments> provideIpAddresses() {
		return Stream.of(Arguments.of("123.123.123.123", true), Arguments.of("a.b.", false));
	}

	@ParameterizedTest
	@MethodSource("provideHostnames")
	void shouldGenerateARegexForHostname(String textToMatch, boolean shouldMatch) {
		assertThat(RegexPatterns.hostname().matcher(textToMatch).matches()).isEqualTo(shouldMatch);
	}

	static Stream<Arguments> provideHostnames() {
		return Stream.of(Arguments.of("https://asd.com", true), Arguments.of("https://asd.com:8080", true),
				Arguments.of("https://localhost", true), Arguments.of("https://localhost:8080", true),
				Arguments.of("https://asd.com/asd", false), Arguments.of("asd.com", false));
	}

	@ParameterizedTest
	@MethodSource("provideEmails")
	void shouldGenerateARegexForEmail(String textToMatch, boolean shouldMatch) {
		assertThat(RegexPatterns.email().matcher(textToMatch).matches()).isEqualTo(shouldMatch);
	}

	static Stream<Arguments> provideEmails() {
		return Stream.of(Arguments.of("asd@asd.com", true), Arguments.of("a.b.", false),
				Arguments.of("asdf@asdf.online", true));
	}

	@ParameterizedTest
	@MethodSource("provideUrls")
	void shouldGenerateARegexForUrl(String textToMatch, boolean shouldMatch) {
		assertThat(RegexPatterns.url().matcher(textToMatch).matches()).isEqualTo(shouldMatch);
	}

	static Stream<Arguments> provideUrls() {
		return Stream.of(Arguments.of("ftp://asd.com:9090/asd/a?a=b", true),
				Arguments.of("http://www.foo.com/blah_blah", true), Arguments.of("ftp://localhost/api", true),
				Arguments.of("http://localhost:8080/api", true), Arguments.of("http://www.foo.com/blah_blah/", true),
				Arguments.of("http://www.foo.com/blah_blah_(wikipedia)", true),
				Arguments.of("http://www.foo.com/blah_blah_(wikipedia)_(again)", true),
				Arguments.of("https://www.example.com/wpstyle/?p=364", true),
				Arguments.of("https://www.example.com/foo/?bar=baz&inga=42&quux", true),
				Arguments.of("http://✪df.ws/123", true), Arguments.of("https://userid:password@example.com:8080", true),
				Arguments.of("https://userid:password@example.com:8080/", true),
				Arguments.of("https://userid@example.com", true), Arguments.of("https://userid@example.com/", true),
				Arguments.of("https://userid@example.com:8080", true),
				Arguments.of("https://userid@example.com:8080/", true),
				Arguments.of("https://userid:password@example.com", true),
				Arguments.of("https://userid:password@example.com/", true), Arguments.of("https://142.42.1.1/", true),
				Arguments.of("https://142.42.1.1:8080/", true), Arguments.of("http://⌘.ws", true),
				Arguments.of("http://⌘.ws/", true), Arguments.of("http://www.foo.com/blah_(wikipedia)#cite-1", true),
				Arguments.of("http://www.foo.com/blah_(wikipedia)_blah#cite-1", true),
				Arguments.of("http://www.foo.com/unicode_(✪)_in_parens", true),
				Arguments.of("http://www.foo.com/(something)?after=parens", true),
				Arguments.of("http://☺.damowmow.com/", true),
				Arguments.of("https://code.google.com/events/#&product=browser", true),
				Arguments.of("https://j.mp", true), Arguments.of("ftp://foo.bar/baz", true),
				Arguments.of("https://foo.bar/?q=Test%20URL-encoded%20stuff", true),
				Arguments.of("https://1224.net/", true), Arguments.of("https://a.b-c.de", true),
				Arguments.of("https://223.255.255.254", true), Arguments.of("foo.com", true),
				Arguments.of("a.b.", false), Arguments.of("http://", false), Arguments.of("http://.", false),
				Arguments.of("http://..", false), Arguments.of("https://../", false), Arguments.of("http://?", false),
				Arguments.of("http://??", false), Arguments.of("https://??/", false), Arguments.of("http://#", false),
				Arguments.of("http://##", false), Arguments.of("http://##/", false),
				Arguments.of("https://foo.bar?q=Spaces should be encoded", false), Arguments.of("//", false),
				Arguments.of("//a", false), Arguments.of("///a", false), Arguments.of("///", false),
				Arguments.of("https:///a", false), Arguments.of("rdar://1234", false), Arguments.of("h://test", false),
				Arguments.of("http:// shouldfail.com", false), Arguments.of(":// should fail", false),
				Arguments.of("https://foo.bar/foo(bar)baz quux", false),
				Arguments.of("https://-error-.invalid/", false), Arguments.of("https://-a.b.co", false),
				Arguments.of("https://a.b-.co", false), Arguments.of("https://1.1.1.1.1", false),
				Arguments.of("https://123.123.123", false), Arguments.of("https://3628126748", false),
				Arguments.of("https://.www.foo.bar/", false), Arguments.of("https://www.foo.bar./", false),
				Arguments.of("https://.www.foo.bar./", false));
	}

	@ParameterizedTest
	@MethodSource("provideHttpsUrls")
	void shouldGenerateARegexForHttpsUrl(String textToMatch, boolean shouldMatch) {
		assertThat(RegexPatterns.httpsUrl().matcher(textToMatch).matches()).isEqualTo(shouldMatch);
	}

	static Stream<Arguments> provideHttpsUrls() {
		return Stream.of(Arguments.of("ftp://asd.com:9090/asd/a?a=b", false),
				Arguments.of("https://foo.com/blah_blah/", true), Arguments.of("https://foo.com/blah_blah", true),
				Arguments.of("http://www.foo.com/blah_blah", false),
				Arguments.of("http://www.foo.com/blah_blah/", false),
				Arguments.of("https://foo.com/blah_blah_(wikipedia)", true),
				Arguments.of("https://foo.com/blah_blah_(wikipedia)_(again)", true),
				Arguments.of("https://www.example.com/wpstyle/?p=364", true),
				Arguments.of("https://www.example.com/foo/?bar=baz&inga=42&quux", true),
				Arguments.of("https://✪df.ws/123", true),
				Arguments.of("https://userid:password@example.com:8080", true),
				Arguments.of("https://userid:password@example.com:8080/", true),
				Arguments.of("https://userid@example.com", true), Arguments.of("https://userid@example.com/", true),
				Arguments.of("https://userid@example.com:8080", true),
				Arguments.of("https://userid@example.com:8080/", true),
				Arguments.of("https://userid:password@example.com", true),
				Arguments.of("https://userid:password@example.com/", true), Arguments.of("https://142.42.1.1/", true),
				Arguments.of("https://142.42.1.1:8080/", true), Arguments.of("https://⌘.ws", true),
				Arguments.of("https://⌘.ws/", true), Arguments.of("https://foo.com/blah_(wikipedia)#cite-1", true),
				Arguments.of("https://foo.com/blah_(wikipedia)_blah#cite-1", true),
				Arguments.of("https://foo.com/unicode_(✪)_in_parens", true),
				Arguments.of("https://foo.com/(something)?after=parens", true),
				Arguments.of("https://☺.damowmow.com/", true),
				Arguments.of("https://code.google.com/events/#&product=browser", true),
				Arguments.of("https://j.mp", true), Arguments.of("ftp://foo.bar/baz", false),
				Arguments.of("https://foo.bar/?q=Test%20URL-encoded%20stuff", true),
				Arguments.of("https://1337.net", true), Arguments.of("https://a.b-c.de", true),
				Arguments.of("https://223.255.255.254", true), Arguments.of("foo.com", false),
				Arguments.of("a.b.", false), Arguments.of("https://", false), Arguments.of("https://.", false),
				Arguments.of("https://..", false), Arguments.of("https://../", false), Arguments.of("https://?", false),
				Arguments.of("https://??", false), Arguments.of("https://??/", false), Arguments.of("https://#", false),
				Arguments.of("https://##", false), Arguments.of("https://##/", false),
				Arguments.of("https://foo.bar?q=Spaces should be encoded", false), Arguments.of("//", false),
				Arguments.of("//a", false), Arguments.of("///a", false), Arguments.of("///", false),
				Arguments.of("https:///a", false), Arguments.of("rdar://1234", false), Arguments.of("h://test", false),
				Arguments.of("https:// shouldfail.com", false), Arguments.of(":// should fail", false),
				Arguments.of("https://foo.bar/foo(bar)baz quux", false),
				Arguments.of("https://-error-.invalid/", false), Arguments.of("https://-a.b.co", false),
				Arguments.of("https://a.b-.co", false), Arguments.of("https://1.1.1.1.1", false),
				Arguments.of("https://123.123.123", false), Arguments.of("https://3628126748", false),
				Arguments.of("https://.www.foo.bar/", false), Arguments.of("https://www.foo.bar./", false),
				Arguments.of("https://.www.foo.bar./", false));
	}

	@ParameterizedTest
	@MethodSource("provideNumbers")
	void shouldGenerateARegexForANumber(String textToMatch, boolean shouldMatch) {
		assertThat(RegexPatterns.number().matcher(textToMatch).matches()).isEqualTo(shouldMatch);
	}

	static Stream<Arguments> provideNumbers() {
		return Stream.of(Arguments.of("1", true), Arguments.of("1.0", true), Arguments.of("0.1", true),
				Arguments.of(".1", true), Arguments.of("1.", false));
	}

	@ParameterizedTest
	@MethodSource("providePositiveIntegers")
	void shouldGenerateARegexForAPositiveInteger(String textToMatch, boolean shouldMatch) {
		assertThat(RegexPatterns.positiveInt().matcher(textToMatch).matches()).isEqualTo(shouldMatch);
	}

	static Stream<Arguments> providePositiveIntegers() {
		return Stream.of(Arguments.of("1", true), Arguments.of("12345", true), Arguments.of("-1", false),
				Arguments.of("0", false), Arguments.of("1.0", false));
	}

	@ParameterizedTest
	@MethodSource("provideDoubles")
	void shouldGenerateARegexForADouble(String textToMatch, boolean shouldMatch) {
		assertThat(RegexPatterns.aDouble().matcher(textToMatch).matches()).isEqualTo(shouldMatch);
	}

	static Stream<Arguments> provideDoubles() {
		return Stream.of(Arguments.of("1", false), Arguments.of("1.0", true), Arguments.of("0.1", true),
				Arguments.of(".1", true), Arguments.of("1.", false));
	}

	@ParameterizedTest
	@MethodSource("provideUuids")
	void shouldGenerateARegexForAUuid(String textToMatch, boolean shouldMatch) {
		assertThat(RegexPatterns.uuid().matcher(textToMatch).matches()).isEqualTo(shouldMatch);
	}

	static Stream<Arguments> provideUuids() {
		return Stream.of(Arguments.of(UUID.randomUUID().toString(), true),
				Arguments.of(UUID.randomUUID().toString(), true),
				Arguments.of(UUID.randomUUID().toString().toUpperCase(), true),
				Arguments.of(UUID.randomUUID().toString() + "!", false),
				Arguments.of("23e4567-z89b-12z3-j456-426655440000", false), Arguments.of("dog", false),
				Arguments.of("5", false));
	}

	@ParameterizedTest
	@MethodSource("provideUuidv4s")
	void shouldGenerateARegexForAUuidv4(String textToMatch, boolean shouldMatch) {
		assertThat(RegexPatterns.uuid4().matcher(textToMatch).matches()).isEqualTo(shouldMatch);
	}

	static Stream<Arguments> provideUuidv4s() {
		return Stream.of(Arguments.of(UUID.randomUUID().toString(), true),
				Arguments.of(UUID.randomUUID().toString(), true),
				Arguments.of(UUID.randomUUID().toString().toUpperCase(), true),
				Arguments.of("123e4567-e89b-42d3-a456-556642440000", true),
				Arguments.of("00000000-0000-4000-8000-000000000000", true),
				Arguments.of("00000000-0000-4000-9000-000000000000", true),
				Arguments.of("00000000-0000-4000-a000-000000000000", true),
				Arguments.of("00000000-0000-4000-b000-000000000000", true),
				Arguments.of("00000000-0000-4000-1000-000000000000", false),
				Arguments.of(UUID.randomUUID().toString() + "!", false),
				Arguments.of("00000000-0000-0000-0000-000000000000", false), Arguments.of("dog", false),
				Arguments.of("5", false));
	}

	@ParameterizedTest
	@MethodSource("provideIsoDates")
	void shouldGenerateARegexWithDateInYyyyMmDdFormat(String textToMatch, boolean shouldMatch) {
		assertThat(RegexPatterns.isoDate().matcher(textToMatch).matches()).isEqualTo(shouldMatch);
	}

	static Stream<Arguments> provideIsoDates() {
		return Stream.of(Arguments.of("2014-03-01", true), Arguments.of("1014-03-01", true),
				Arguments.of("1014-3-01", false), Arguments.of("14-03-01", false), Arguments.of("1014-12-01", true),
				Arguments.of("1014-12-31", true), Arguments.of("1014-12-1", false), Arguments.of("1014-12-32", false),
				Arguments.of("1014-13-31", false), Arguments.of("1014-20-30", false), Arguments.of("5", false));
	}

	@ParameterizedTest
	@MethodSource("provideIsoDateTimes")
	void shouldGenerateARegexWithDatetimeInYyyyMmDdTHhMmSsFormat(String textToMatch, boolean shouldMatch) {
		assertThat(RegexPatterns.isoDateTime().matcher(textToMatch).matches()).isEqualTo(shouldMatch);
	}

	static Stream<Arguments> provideIsoDateTimes() {
		return Stream.of(Arguments.of("2014-03-01T12:23:45", true), Arguments.of("1014-03-01T23:59:59", true),
				Arguments.of("1014-3-01T01:01:01", false), Arguments.of("1014-03-01T00:00:00", true),
				Arguments.of("1014-03-01T00:00:0", false), Arguments.of("1014-03-01T00:0:01", false),
				Arguments.of("1014-03-01T0:01:01", false), Arguments.of("1014-03-0100:01:01", false),
				Arguments.of("14-03-01T12:23:45", false), Arguments.of("1014-12-01T12:23:45", true),
				Arguments.of("1014-12-31T12:23:45", true), Arguments.of("1014-12-1T12:23:45", false),
				Arguments.of("1014-12-32T12:23:45", false), Arguments.of("1014-13-31T12:23:45", false),
				Arguments.of("1014-20-30T12:23:45", false), Arguments.of("1014-20-30T24:23:45", false),
				Arguments.of("1014-20-30T23:60:45", false), Arguments.of("1014-20-30T23:59:60", false));
	}

	@ParameterizedTest
	@MethodSource("provideIsoTimes")
	void shouldGenerateARegexWithTimeInHhMmSsFormat(String textToMatch, boolean shouldMatch) {
		assertThat(RegexPatterns.isoTime().matcher(textToMatch).matches()).isEqualTo(shouldMatch);
	}

	static Stream<Arguments> provideIsoTimes() {
		return Stream.of(Arguments.of("12:23:45", true), Arguments.of("23:59:59", true), Arguments.of("00:00:00", true),
				Arguments.of("00:00:0", false), Arguments.of("00:0:01", false), Arguments.of("0:01:01", false),
				Arguments.of("24:23:45", false), Arguments.of("23:60:45", false), Arguments.of("23:59:60", false));
	}

	@ParameterizedTest
	@MethodSource("provideIso8601WithOffset")
	void shouldGenerateARegexWithIso8601DateTimeWithTimezone(String textToMatch, boolean shouldMatch) {
		assertThat(RegexPatterns.iso8601WithOffset().matcher(textToMatch).matches()).isEqualTo(shouldMatch);
	}

	static Stream<Arguments> provideIso8601WithOffset() {
		return Stream.of(Arguments.of("2014-03-01T12:23:45Z", true), Arguments.of("2014-03-01T12:23:45+01:00", true),
				Arguments.of("2014-03-01T12:23:45.1Z", true), Arguments.of("2014-03-01T12:23:45.12Z", true),
				Arguments.of("2014-03-01T12:23:45.123Z", true), Arguments.of("2014-03-01T12:23:45.1234Z", true),
				Arguments.of("2014-03-01T12:23:45.12345Z", true), Arguments.of("2014-03-01T12:23:45.123456Z", true),
				Arguments.of("2014-03-01T12:23:45.1+01:00", true), Arguments.of("2014-03-01T12:23:45.12+01:00", true),
				Arguments.of("2014-03-01T12:23:45.123+01:00", true),
				Arguments.of("2014-03-01T12:23:45.1234+01:00", true),
				Arguments.of("2014-03-01T12:23:45.12345+01:00", true),
				Arguments.of("2014-03-01T12:23:45.123456+01:00", true), Arguments.of("2014-03-01T12:23:45", false),
				Arguments.of("2014-03-01T12:23:45.123", false));
	}

	@ParameterizedTest
	@MethodSource("provideNonBlankStrings")
	void shouldGenerateARegexForANonBlankString(String textToMatch, boolean shouldMatch) {
		assertThat(RegexPatterns.nonBlank().matcher(textToMatch).matches()).isEqualTo(shouldMatch);
	}

	static Stream<Arguments> provideNonBlankStrings() {
		return Stream.of(Arguments.of("Not Empty", true), Arguments.of("", false), Arguments.of("    ", false),
				Arguments.of("\nFoo\nBar\n", true), Arguments.of("\r\n", false), Arguments.of(" \r\n\t", false));
	}

	@ParameterizedTest
	@MethodSource("provideNonEmptyStrings")
	void shouldGenerateARegexForANonEmptyString(String textToMatch, boolean shouldMatch) {
		assertThat(RegexPatterns.nonEmpty().matcher(textToMatch).matches()).isEqualTo(shouldMatch);
	}

	static Stream<Arguments> provideNonEmptyStrings() {
		return Stream.of(Arguments.of("Not Empty", true), Arguments.of("", false), Arguments.of("  ", true),
				Arguments.of("\nFoo\nBar\n", true), Arguments.of("\r\n", true), Arguments.of(" \r\n\t", true));
	}

	@ParameterizedTest
	@MethodSource("provideAnyOfValues")
	void shouldGenerateARegexForAnEnumeratedValue(String textToMatch, boolean shouldMatch) {
		assertThat(RegexPatterns.anyOf("foo", "bar").matcher(textToMatch).matches()).isEqualTo(shouldMatch);
	}

	static Stream<Arguments> provideAnyOfValues() {
		return Stream.of(Arguments.of("foo", true), Arguments.of("bar", true), Arguments.of("baz", false));
	}

}
