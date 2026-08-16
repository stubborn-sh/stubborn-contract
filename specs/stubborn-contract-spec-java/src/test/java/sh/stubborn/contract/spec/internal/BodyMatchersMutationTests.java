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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("NullAway")
class BodyMatchersMutationTests {

	@Test
	void hasMatchersFalseWhenEmptyTrueWhenPopulated() {
		BodyMatchers matchers = new BodyMatchers();
		assertThat(matchers.hasMatchers()).isFalse();
		matchers.jsonPath("$.a", matchers.byDate());
		assertThat(matchers.hasMatchers()).isTrue();
	}

	@Test
	void jsonPathAddsPathBodyMatcher() {
		BodyMatchers matchers = new BodyMatchers();
		matchers.jsonPath("$.a", matchers.byEquality());
		assertThat(matchers.matchers()).hasSize(1);
		assertThat(matchers.matchers().get(0).path()).isEqualTo("$.a");
	}

	@Test
	void xPathAddsPathBodyMatcher() {
		BodyMatchers matchers = new BodyMatchers();
		matchers.xPath("/a/b", matchers.byEquality());
		assertThat(matchers.matchers().get(0).path()).isEqualTo("/a/b");
	}

	@Test
	void byDateUsesDateType() {
		MatchingTypeValue value = new BodyMatchers().byDate();
		assertThat(value.getType()).isEqualTo(MatchingType.DATE);
		assertThat(value.getValue()).isInstanceOf(RegexProperty.class);
	}

	@Test
	void byTimeUsesTimeType() {
		assertThat(new BodyMatchers().byTime().getType()).isEqualTo(MatchingType.TIME);
	}

	@Test
	void byTimestampUsesTimestampType() {
		assertThat(new BodyMatchers().byTimestamp().getType()).isEqualTo(MatchingType.TIMESTAMP);
	}

	@Test
	void byEqualityUsesEqualityType() {
		assertThat(new BodyMatchers().byEquality().getType()).isEqualTo(MatchingType.EQUALITY);
	}

	@Test
	void byRegexStringBuildsRegexMatcher() {
		RegexMatchingTypeValue value = new BodyMatchers().byRegex("[0-9]+");
		assertThat(value.getType()).isEqualTo(MatchingType.REGEX);
		assertThat(((RegexProperty) value.getValue()).pattern()).isEqualTo("[0-9]+");
	}

	@Test
	void byRegexPatternBuildsRegexMatcher() {
		RegexMatchingTypeValue value = new BodyMatchers().byRegex(Pattern.compile("[a-z]+"));
		assertThat(((RegexProperty) value.getValue()).pattern()).isEqualTo("[a-z]+");
	}

	@Test
	void byRegexRegexPropertyBuildsRegexMatcher() {
		RegexProperty regex = new RegexProperty(Pattern.compile("[a-z]+"));
		RegexMatchingTypeValue value = new BodyMatchers().byRegex(regex);
		assertThat(value.getValue()).isSameAs(regex);
	}

	@Test
	void byTypeConsumerBuildsTypeMatcher() {
		MatchingTypeValue value = new BodyMatchers().byType((holder) -> holder.occurrence(2));
		assertThat(value.getType()).isEqualTo(MatchingType.TYPE);
		assertThat(value.getMinTypeOccurrence()).isEqualTo(2);
		assertThat(value.getMaxTypeOccurrence()).isEqualTo(2);
	}

	@Test
	void equalsHashCode() {
		BodyMatchers a = new BodyMatchers();
		a.jsonPath("$.a", a.byEquality());
		BodyMatchers b = new BodyMatchers();
		b.jsonPath("$.a", b.byEquality());
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a.equals(a)).isTrue();
		assertThat(a.equals(null)).isFalse();
		assertThat(a.equals("x")).isFalse();
	}

	@Test
	void notEqualWhenMatchersDiffer() {
		BodyMatchers a = new BodyMatchers();
		a.jsonPath("$.a", a.byEquality());
		BodyMatchers b = new BodyMatchers();
		assertThat(a).isNotEqualTo(b);
		assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
	}

	@Test
	void toStringContainsMatchers() {
		assertThat(new BodyMatchers().toString()).contains("BodyMatchers");
	}

	// --- ResponseBodyMatchers ---

	@Test
	void responseByTypeUsesTypeMatcher() {
		MatchingTypeValue value = new ResponseBodyMatchers().byType();
		assertThat(value.getType()).isEqualTo(MatchingType.TYPE);
	}

	@Test
	void responseByCommandUsesCommandMatcher() {
		MatchingTypeValue value = new ResponseBodyMatchers().byCommand("assertThat($it)");
		assertThat(value.getType()).isEqualTo(MatchingType.COMMAND);
		assertThat(value.getValue()).isInstanceOf(ExecutionProperty.class);
		assertThat(((ExecutionProperty) value.getValue()).getExecutionCommand()).isEqualTo("assertThat($it)");
	}

	@Test
	void responseByNullUsesNullMatcher() {
		MatchingTypeValue value = new ResponseBodyMatchers().byNull();
		assertThat(value.getType()).isEqualTo(MatchingType.NULL);
	}

	// --- PathBodyMatcher ---

	@Test
	void pathBodyMatcherExposesMatchingTypeValueFields() {
		MatchingTypeValue mtv = new MatchingTypeValue(MatchingType.REGEX, "v");
		mtv.setMinTypeOccurrence(1);
		mtv.setMaxTypeOccurrence(5);
		PathBodyMatcher matcher = new PathBodyMatcher("$.a", mtv);
		assertThat(matcher.path()).isEqualTo("$.a");
		assertThat(matcher.matchingType()).isEqualTo(MatchingType.REGEX);
		assertThat(matcher.value()).isEqualTo("v");
		assertThat(matcher.minTypeOccurrence()).isEqualTo(1);
		assertThat(matcher.maxTypeOccurrence()).isEqualTo(5);
	}

	@Test
	void pathBodyMatcherEqualsHashCode() {
		MatchingTypeValue mtv = new MatchingTypeValue(MatchingType.REGEX, "v");
		PathBodyMatcher a = new PathBodyMatcher("$.a", mtv);
		PathBodyMatcher b = new PathBodyMatcher("$.a", mtv);
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a.equals(a)).isTrue();
		assertThat(a.equals(null)).isFalse();
		assertThat(a.equals("x")).isFalse();
	}

	@Test
	void pathBodyMatcherNotEqualWhenPathDiffers() {
		MatchingTypeValue mtv = new MatchingTypeValue(MatchingType.REGEX, "v");
		assertThat(new PathBodyMatcher("$.a", mtv)).isNotEqualTo(new PathBodyMatcher("$.b", mtv));
		assertThat(new PathBodyMatcher("$.a", mtv).hashCode()).isNotEqualTo(new PathBodyMatcher("$.b", mtv).hashCode());
	}

	@Test
	void pathBodyMatcherNotEqualWhenValueDiffers() {
		assertThat(new PathBodyMatcher("$.a", new MatchingTypeValue(MatchingType.REGEX, "v1")))
			.isNotEqualTo(new PathBodyMatcher("$.a", new MatchingTypeValue(MatchingType.REGEX, "v2")));
	}

	@Test
	void pathBodyMatcherToString() {
		PathBodyMatcher matcher = new PathBodyMatcher("$.path", new MatchingTypeValue(MatchingType.REGEX, "v"));
		assertThat(matcher.toString()).contains("PathBodyMatcher").contains("$.path");
	}

	@Test
	void assertNotNullThrowsForNullRegex() {
		BodyMatchers matchers = new BodyMatchers();
		assertThatThrownBy(() -> matchers.byRegex((RegexProperty) null)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("can't be null");
	}

}
