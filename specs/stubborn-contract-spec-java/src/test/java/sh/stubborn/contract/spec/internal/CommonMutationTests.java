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

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("NullAway")
class CommonMutationTests {

	private final Common common = new Common();

	@Test
	void toDslPropertyWrapsPlainObject() {
		DslProperty property = this.common.toDslProperty((Object) "value");
		assertThat(property.getClientValue()).isEqualTo("value");
		assertThat(property.getServerValue()).isEqualTo("value");
	}

	@Test
	void toDslPropertyReturnsSameDslProperty() {
		DslProperty original = new DslProperty("v");
		assertThat(this.common.toDslProperty(original)).isSameAs(original);
	}

	@Test
	void toDslPropertyFromMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("a", "1");
		DslProperty property = this.common.toDslProperty(map);
		assertThat(property.getClientValue()).isInstanceOf(Map.class);
	}

	@Test
	void toDslPropertyFromList() {
		DslProperty property = this.common.toDslProperty(List.of("a", "b"));
		assertThat(property.getClientValue()).isInstanceOf(List.class);
	}

	@Test
	void convertObjectsFromMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("a", "1");
		map.put("b", "2");
		Map<String, DslProperty> result = this.common.convertObjectsToDslProperties(map);
		assertThat(result).containsOnlyKeys("a", "b");
		assertThat(result.get("a").getClientValue()).isEqualTo("1");
	}

	@Test
	void convertObjectsFromList() {
		var result = this.common.convertObjectsToDslProperties(List.of("x", "y"));
		assertThat(result).hasSize(2);
	}

	@Test
	void valueWrapsObject() {
		assertThat(this.common.value((Object) "v").getClientValue()).isEqualTo("v");
		assertThat(this.common.$((Object) "v").getClientValue()).isEqualTo("v");
	}

	@Test
	void valueReturnsSameDslProperty() {
		DslProperty property = new DslProperty("v");
		assertThat(this.common.value(property)).isSameAs(property);
		assertThat(this.common.$(property)).isSameAs(property);
	}

	@Test
	void valueClientThenServer() {
		DslProperty property = this.common.value(this.common.client("c"), this.common.server("s"));
		assertThat(property.getClientValue()).isEqualTo("c");
		assertThat(property.getServerValue()).isEqualTo("s");
	}

	@Test
	void valueServerThenClient() {
		DslProperty property = this.common.value(this.common.server("s"), this.common.client("c"));
		assertThat(property.getClientValue()).isEqualTo("c");
		assertThat(property.getServerValue()).isEqualTo("s");
	}

	@Test
	void dollarClientServerDelegatesToValue() {
		DslProperty property = this.common.$(this.common.client("c"), this.common.server("s"));
		assertThat(property.getClientValue()).isEqualTo("c");
		assertThat(property.getServerValue()).isEqualTo("s");
	}

	@Test
	void dollarServerClientDelegatesToValue() {
		DslProperty property = this.common.$(this.common.server("s"), this.common.client("c"));
		assertThat(property.getClientValue()).isEqualTo("c");
		assertThat(property.getServerValue()).isEqualTo("s");
	}

	@Test
	void regexFromString() {
		RegexProperty property = this.common.regex("[0-9]+");
		assertThat(property.pattern()).isEqualTo("[0-9]+");
	}

	@Test
	void regexFromPattern() {
		RegexProperty property = this.common.regex(Pattern.compile("[a-z]+"));
		assertThat(property.pattern()).isEqualTo("[a-z]+");
	}

	@Test
	void regexReturnsSameRegexProperty() {
		RegexProperty property = new RegexProperty(Pattern.compile("[0-9]+"));
		assertThat(this.common.regex(property)).isSameAs(property);
	}

	@Test
	void optionalWraps() {
		OptionalProperty property = this.common.optional("v");
		assertThat(property.getValue()).isEqualTo("v");
	}

	@Test
	void regexPropertyWraps() {
		RegexProperty property = this.common.regexProperty(Pattern.compile("[0-9]+"));
		assertThat(property.pattern()).isEqualTo("[0-9]+");
	}

	@Test
	void executeWraps() {
		ExecutionProperty property = this.common.execute("foo($it)");
		assertThat(property.getExecutionCommand()).isEqualTo("foo($it)");
	}

	@Test
	void clientHelpers() {
		assertThat(this.common.client("v").getClientValue()).isEqualTo("v");
		assertThat(this.common.stub("v").getClientValue()).isEqualTo("v");
		assertThat(this.common.consumer("v").getClientValue()).isEqualTo("v");
		assertThat(this.common.c("v").getClientValue()).isEqualTo("v");
	}

	@Test
	void serverHelpers() {
		assertThat(this.common.server("v").getServerValue()).isEqualTo("v");
		assertThat(this.common.p("v").getServerValue()).isEqualTo("v");
		assertThat(this.common.test("v").getServerValue()).isEqualTo("v");
		assertThat(this.common.producer("v").getServerValue()).isEqualTo("v");
	}

	@Test
	void namedWithNameAndValue() {
		NamedProperty property = this.common.named(new DslProperty("n"), new DslProperty("v"));
		assertThat(property.getName().getClientValue()).isEqualTo("n");
		assertThat(property.getValue().getClientValue()).isEqualTo("v");
		assertThat(property.getContentType()).isNull();
	}

	@Test
	void namedWithContentType() {
		NamedProperty property = this.common.named(new DslProperty("n"), new DslProperty("v"), new DslProperty("ct"));
		assertThat(property.getContentType().getClientValue()).isEqualTo("ct");
	}

	@Test
	void namedFromMap() {
		Map<String, DslProperty> map = new LinkedHashMap<>();
		map.put("name", new DslProperty("n"));
		map.put("content", new DslProperty("v"));
		NamedProperty property = this.common.named(map);
		assertThat(property.getName().getClientValue()).isEqualTo("n");
		assertThat(property.getValue().getClientValue()).isEqualTo("v");
	}

	@Test
	void fileReadsResourceContent() {
		FromFileProperty property = this.common.file("mutation-common-file.txt");
		assertThat(property.asString()).isEqualTo("hello mutation world");
	}

	@Test
	void fileWithCharset() {
		FromFileProperty property = this.common.file("mutation-common-file.txt", StandardCharsets.UTF_8);
		assertThat(property.getType()).isEqualTo(String.class);
		assertThat(property.asString()).isEqualTo("hello mutation world");
	}

	@Test
	void fileAsBytes() {
		FromFileProperty property = this.common.fileAsBytes("mutation-common-file.txt");
		assertThat(property.getType()).isEqualTo(byte[].class);
		assertThat(property.asBytes()).isEqualTo("hello mutation world".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void fileMissingThrows() {
		assertThatThrownBy(() -> this.common.file("does-not-exist.txt")).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("is not present");
	}

	@Test
	void regexHelperPatternsDelegateToRegexPatterns() {
		assertThat(this.common.onlyAlphaUnicode().pattern()).isEqualTo(RegexPatterns.ONLY_ALPHA_UNICODE.pattern());
		assertThat(this.common.alphaNumeric().pattern()).isEqualTo(RegexPatterns.ALPHA_NUMERIC.pattern());
		assertThat(this.common.number().pattern()).isEqualTo(RegexPatterns.NUMBER.pattern());
		assertThat(this.common.positiveInt().pattern()).isEqualTo(RegexPatterns.POSITIVE_INT.pattern());
		assertThat(this.common.anyBoolean().pattern()).isEqualTo(RegexPatterns.TRUE_OR_FALSE.pattern());
		assertThat(this.common.anInteger().pattern()).isEqualTo(RegexPatterns.INTEGER.pattern());
		assertThat(this.common.aDouble().pattern()).isEqualTo(RegexPatterns.DOUBLE.pattern());
		assertThat(this.common.ipAddress().pattern()).isEqualTo(RegexPatterns.IP_ADDRESS.pattern());
		assertThat(this.common.hostname().pattern()).isEqualTo(RegexPatterns.HOSTNAME_PATTERN.pattern());
		assertThat(this.common.email().pattern()).isEqualTo(RegexPatterns.EMAIL.pattern());
		assertThat(this.common.url().pattern()).isEqualTo(RegexPatterns.URL.pattern());
		assertThat(this.common.httpsUrl().pattern()).isEqualTo(RegexPatterns.HTTPS_URL.pattern());
		assertThat(this.common.uuid().pattern()).isEqualTo(RegexPatterns.UUID.pattern());
		assertThat(this.common.isoDate().pattern()).isEqualTo(RegexPatterns.ANY_DATE.pattern());
		assertThat(this.common.isoDateTime().pattern()).isEqualTo(RegexPatterns.ANY_DATE_TIME.pattern());
		assertThat(this.common.isoTime().pattern()).isEqualTo(RegexPatterns.ANY_TIME.pattern());
		assertThat(this.common.iso8601WithOffset().pattern()).isEqualTo(RegexPatterns.ISO8601_WITH_OFFSET.pattern());
		assertThat(this.common.nonEmpty().pattern()).isEqualTo(RegexPatterns.NON_EMPTY.pattern());
		assertThat(this.common.nonBlank().pattern()).isEqualTo(RegexPatterns.NON_BLANK.pattern());
	}

	@Test
	void assertThatSidesMatchOptionalWithNullSecondSideDoesNotThrow() {
		this.common.assertThatSidesMatch(new OptionalProperty("abc"), null);
	}

	@Test
	void assertThatSidesMatchOptionalMatching() {
		this.common.assertThatSidesMatch(new OptionalProperty("abc"), "abc");
	}

	@Test
	void assertThatSidesMatchOptionalNotMatchingThrows() {
		assertThatThrownBy(() -> this.common.assertThatSidesMatch(new OptionalProperty("abc"), "xyz"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("is not matched by");
	}

	@Test
	void assertThatSidesMatchPatternFirstStringSecond() {
		this.common.assertThatSidesMatch(Pattern.compile("[0-9]+"), "123");
		assertThatThrownBy(() -> this.common.assertThatSidesMatch(Pattern.compile("[0-9]+"), "abc"))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void assertThatSidesMatchRegexPropertyFirstStringSecond() {
		this.common.assertThatSidesMatch(new RegexProperty(Pattern.compile("[0-9]+")), "123");
		assertThatThrownBy(() -> this.common.assertThatSidesMatch(new RegexProperty(Pattern.compile("[0-9]+")), "abc"))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void assertThatSidesMatchStringFirstPatternSecond() {
		this.common.assertThatSidesMatch("123", Pattern.compile("[0-9]+"));
		assertThatThrownBy(() -> this.common.assertThatSidesMatch("abc", Pattern.compile("[0-9]+")))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void assertThatSidesMatchStringFirstRegexPropertySecond() {
		this.common.assertThatSidesMatch("123", new RegexProperty(Pattern.compile("[0-9]+")));
	}

	@Test
	void assertThatSidesMatchAbsentFirstNonAbsentSecondThrows() {
		MatchingStrategy absent = new MatchingStrategy("x", MatchingStrategy.Type.ABSENT);
		MatchingStrategy equal = new MatchingStrategy("x", MatchingStrategy.Type.EQUAL_TO);
		assertThatThrownBy(() -> this.common.assertThatSidesMatch(absent, equal))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Absent");
	}

	@Test
	void assertThatSidesMatchBothAbsentDoesNotThrow() {
		MatchingStrategy absent = new MatchingStrategy("x", MatchingStrategy.Type.ABSENT);
		MatchingStrategy absent2 = new MatchingStrategy("y", MatchingStrategy.Type.ABSENT);
		this.common.assertThatSidesMatch(absent, absent2);
	}

	@Test
	void assertThatSidesMatchOnlyFirstMatchingStrategyAbsentThrows() {
		MatchingStrategy absent = new MatchingStrategy("x", MatchingStrategy.Type.ABSENT);
		assertThatThrownBy(() -> this.common.assertThatSidesMatch(absent, 42))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void assertThatSidesMatchOnlySecondMatchingStrategyAbsentThrows() {
		MatchingStrategy absent = new MatchingStrategy("x", MatchingStrategy.Type.ABSENT);
		assertThatThrownBy(() -> this.common.assertThatSidesMatch(42, absent))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void assertThatSidesMatchOnlyFirstMatchingStrategyNonAbsentDoesNotThrow() {
		MatchingStrategy equal = new MatchingStrategy("x", MatchingStrategy.Type.EQUAL_TO);
		this.common.assertThatSidesMatch(equal, 42);
	}

}
