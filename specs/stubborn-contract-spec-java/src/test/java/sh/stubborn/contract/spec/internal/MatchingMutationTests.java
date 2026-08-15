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
class MatchingMutationTests {

	// --- MatchingStrategy ---

	@Test
	void matchingStrategyValueAndType() {
		MatchingStrategy strategy = new MatchingStrategy("v", MatchingStrategy.Type.EQUAL_TO);
		assertThat(strategy.getType()).isEqualTo(MatchingStrategy.Type.EQUAL_TO);
		assertThat(strategy.getClientValue()).isEqualTo("v");
		assertThat(strategy.getJsonCompareMode()).isNull();
	}

	@Test
	void matchingStrategyWithJsonCompareMode() {
		MatchingStrategy strategy = new MatchingStrategy("v", MatchingStrategy.Type.EQUAL_TO_JSON,
				JSONCompareMode.STRICT);
		assertThat(strategy.getJsonCompareMode()).isEqualTo(JSONCompareMode.STRICT);
	}

	@Test
	void matchingStrategyFromDslProperty() {
		MatchingStrategy strategy = new MatchingStrategy(new DslProperty("c", "s"), MatchingStrategy.Type.MATCHING);
		assertThat(strategy.getClientValue()).isEqualTo("c");
		assertThat(strategy.getServerValue()).isEqualTo("s");
		assertThat(strategy.getType()).isEqualTo(MatchingStrategy.Type.MATCHING);
	}

	@Test
	void matchingStrategyFromDslPropertyWithMode() {
		MatchingStrategy strategy = new MatchingStrategy(new DslProperty("c", "s"), MatchingStrategy.Type.EQUAL_TO_JSON,
				JSONCompareMode.LENIENT);
		assertThat(strategy.getJsonCompareMode()).isEqualTo(JSONCompareMode.LENIENT);
	}

	@Test
	void matchingStrategySetters() {
		MatchingStrategy strategy = new MatchingStrategy("v", MatchingStrategy.Type.EQUAL_TO);
		strategy.setType(MatchingStrategy.Type.CONTAINS);
		strategy.setJsonCompareMode(JSONCompareMode.STRICT);
		assertThat(strategy.getType()).isEqualTo(MatchingStrategy.Type.CONTAINS);
		assertThat(strategy.getJsonCompareMode()).isEqualTo(JSONCompareMode.STRICT);
	}

	@Test
	void matchingStrategyEqualsHashCode() {
		MatchingStrategy a = new MatchingStrategy("v", MatchingStrategy.Type.EQUAL_TO);
		MatchingStrategy b = new MatchingStrategy("v", MatchingStrategy.Type.EQUAL_TO);
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a.equals(a)).isTrue();
		assertThat(a.equals(null)).isFalse();
		assertThat(a.equals("x")).isFalse();
	}

	@Test
	void matchingStrategyNotEqualWhenTypeDiffers() {
		assertThat(new MatchingStrategy("v", MatchingStrategy.Type.EQUAL_TO))
			.isNotEqualTo(new MatchingStrategy("v", MatchingStrategy.Type.CONTAINS));
		assertThat(new MatchingStrategy("v", MatchingStrategy.Type.EQUAL_TO).hashCode())
			.isNotEqualTo(new MatchingStrategy("v", MatchingStrategy.Type.CONTAINS).hashCode());
	}

	@Test
	void matchingStrategyNotEqualWhenValueDiffers() {
		assertThat(new MatchingStrategy("v1", MatchingStrategy.Type.EQUAL_TO))
			.isNotEqualTo(new MatchingStrategy("v2", MatchingStrategy.Type.EQUAL_TO));
	}

	@Test
	void matchingStrategyNotEqualWhenModeDiffers() {
		assertThat(new MatchingStrategy("v", MatchingStrategy.Type.EQUAL_TO_JSON, JSONCompareMode.STRICT))
			.isNotEqualTo(new MatchingStrategy("v", MatchingStrategy.Type.EQUAL_TO_JSON, JSONCompareMode.LENIENT));
	}

	@Test
	void matchingStrategyToString() {
		MatchingStrategy strategy = new MatchingStrategy("v", MatchingStrategy.Type.EQUAL_TO);
		assertThat(strategy.toString()).contains("MatchingStrategy").contains("EQUAL_TO");
	}

	@Test
	void matchingStrategyTypeGetName() {
		assertThat(MatchingStrategy.Type.EQUAL_TO.getName()).isEqualTo("equalTo");
		assertThat(MatchingStrategy.Type.CONTAINS.getName()).isEqualTo("containing");
		assertThat(MatchingStrategy.Type.MATCHING.getName()).isEqualTo("matching");
		assertThat(MatchingStrategy.Type.NOT_MATCHING.getName()).isEqualTo("notMatching");
		assertThat(MatchingStrategy.Type.EQUAL_TO_JSON.getName()).isEqualTo("equalToJson");
		assertThat(MatchingStrategy.Type.EQUAL_TO_XML.getName()).isEqualTo("equalToXml");
		assertThat(MatchingStrategy.Type.ABSENT.getName()).isEqualTo("absent");
		assertThat(MatchingStrategy.Type.BINARY_EQUAL_TO.getName()).isEqualTo("binaryEqualTo");
	}

	// --- MatchingType ---

	@Test
	void matchingTypeRegexRelatedTrueForRegexTypes() {
		assertThat(MatchingType.regexRelated(MatchingType.DATE)).isTrue();
		assertThat(MatchingType.regexRelated(MatchingType.TIME)).isTrue();
		assertThat(MatchingType.regexRelated(MatchingType.TIMESTAMP)).isTrue();
		assertThat(MatchingType.regexRelated(MatchingType.REGEX)).isTrue();
	}

	@Test
	void matchingTypeRegexRelatedFalseForNonRegexTypes() {
		assertThat(MatchingType.regexRelated(MatchingType.EQUALITY)).isFalse();
		assertThat(MatchingType.regexRelated(MatchingType.TYPE)).isFalse();
		assertThat(MatchingType.regexRelated(MatchingType.COMMAND)).isFalse();
		assertThat(MatchingType.regexRelated(MatchingType.NULL)).isFalse();
	}

	// --- MatchingTypeValue ---

	@Test
	void matchingTypeValueFullConstructorAndGetters() {
		MatchingTypeValue value = new MatchingTypeValue(MatchingType.TYPE, "v");
		assertThat(value.getType()).isEqualTo(MatchingType.TYPE);
		assertThat(value.getValue()).isEqualTo("v");
		assertThat(value.getMinTypeOccurrence()).isNull();
		assertThat(value.getMaxTypeOccurrence()).isNull();
	}

	@Test
	void matchingTypeValueSetters() {
		MatchingTypeValue value = new MatchingTypeValue(MatchingType.TYPE, "v");
		value.setType(MatchingType.REGEX);
		value.setValue("v2");
		value.setMinTypeOccurrence(1);
		value.setMaxTypeOccurrence(5);
		assertThat(value.getType()).isEqualTo(MatchingType.REGEX);
		assertThat(value.getValue()).isEqualTo("v2");
		assertThat(value.getMinTypeOccurrence()).isEqualTo(1);
		assertThat(value.getMaxTypeOccurrence()).isEqualTo(5);
	}

	@Test
	void matchingTypeValueEqualsHashCode() {
		MatchingTypeValue a = new MatchingTypeValue(MatchingType.TYPE, "v");
		MatchingTypeValue b = new MatchingTypeValue(MatchingType.TYPE, "v");
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a.equals(a)).isTrue();
		assertThat(a.equals(null)).isFalse();
		assertThat(a.equals("x")).isFalse();
	}

	@Test
	void matchingTypeValueNotEqualWhenTypeDiffers() {
		assertThat(new MatchingTypeValue(MatchingType.TYPE, "v"))
			.isNotEqualTo(new MatchingTypeValue(MatchingType.REGEX, "v"));
		assertThat(new MatchingTypeValue(MatchingType.TYPE, "v").hashCode())
			.isNotEqualTo(new MatchingTypeValue(MatchingType.REGEX, "v").hashCode());
	}

	@Test
	void matchingTypeValueNotEqualWhenValueDiffers() {
		assertThat(new MatchingTypeValue(MatchingType.TYPE, "v1"))
			.isNotEqualTo(new MatchingTypeValue(MatchingType.TYPE, "v2"));
	}

	@Test
	void matchingTypeValueNotEqualWhenMinDiffers() {
		MatchingTypeValue a = new MatchingTypeValue(MatchingType.TYPE, "v");
		a.setMinTypeOccurrence(1);
		MatchingTypeValue b = new MatchingTypeValue(MatchingType.TYPE, "v");
		b.setMinTypeOccurrence(2);
		assertThat(a).isNotEqualTo(b);
	}

	@Test
	void matchingTypeValueNotEqualWhenMaxDiffers() {
		MatchingTypeValue a = new MatchingTypeValue(MatchingType.TYPE, "v");
		a.setMaxTypeOccurrence(1);
		MatchingTypeValue b = new MatchingTypeValue(MatchingType.TYPE, "v");
		b.setMaxTypeOccurrence(2);
		assertThat(a).isNotEqualTo(b);
	}

	@Test
	void matchingTypeValueToString() {
		MatchingTypeValue value = new MatchingTypeValue(MatchingType.TYPE, "theValue");
		assertThat(value.toString()).contains("MatchingTypeValue").contains("TYPE").contains("theValue");
	}

	// --- MatchingTypeValueHolder ---

	@Test
	void holderMinOccurrence() {
		MatchingTypeValue value = new MatchingTypeValueHolder().minOccurrence(3);
		assertThat(value.getType()).isEqualTo(MatchingType.TYPE);
		assertThat(value.getMinTypeOccurrence()).isEqualTo(3);
		assertThat(value.getMaxTypeOccurrence()).isNull();
	}

	@Test
	void holderMaxOccurrence() {
		MatchingTypeValue value = new MatchingTypeValueHolder().maxOccurrence(7);
		assertThat(value.getMaxTypeOccurrence()).isEqualTo(7);
		assertThat(value.getMinTypeOccurrence()).isNull();
	}

	@Test
	void holderOccurrenceSetsBoth() {
		MatchingTypeValue value = new MatchingTypeValueHolder().occurrence(4);
		assertThat(value.getMinTypeOccurrence()).isEqualTo(4);
		assertThat(value.getMaxTypeOccurrence()).isEqualTo(4);
	}

	// --- RegexMatchingTypeValue ---

	@Test
	void regexMatchingTypeValueAsIntegerRetypesRegex() {
		RegexMatchingTypeValue value = new RegexMatchingTypeValue(MatchingType.REGEX,
				new RegexProperty(Pattern.compile("[0-9]+")));
		RegexMatchingTypeValue typed = value.asInteger();
		assertThat(((RegexProperty) typed.getValue()).getClazz()).isEqualTo(Integer.class);
	}

	@Test
	void regexMatchingTypeValueAsAllTypes() {
		assertThat(regexClazz((v) -> v.asDouble())).isEqualTo(Double.class);
		assertThat(regexClazz((v) -> v.asFloat())).isEqualTo(Float.class);
		assertThat(regexClazz((v) -> v.asLong())).isEqualTo(Long.class);
		assertThat(regexClazz((v) -> v.asShort())).isEqualTo(Short.class);
		assertThat(regexClazz((v) -> v.asString())).isEqualTo(String.class);
		assertThat(regexClazz((v) -> v.asBooleanType())).isEqualTo(Boolean.class);
	}

	private Class<?> regexClazz(java.util.function.Function<RegexMatchingTypeValue, RegexMatchingTypeValue> mapper) {
		RegexMatchingTypeValue value = new RegexMatchingTypeValue(MatchingType.REGEX,
				new RegexProperty(Pattern.compile("[0-9]+")));
		return ((RegexProperty) mapper.apply(value).getValue()).getClazz();
	}

	@Test
	void regexMatchingTypeValueThrowsWhenValueNotRegex() {
		RegexMatchingTypeValue value = new RegexMatchingTypeValue(MatchingType.REGEX, "notRegex");
		assertThatThrownBy(value::asInteger).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("has to be a regex");
	}

}
