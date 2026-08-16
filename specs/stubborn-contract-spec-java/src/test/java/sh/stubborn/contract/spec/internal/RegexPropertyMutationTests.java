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
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("NullAway")
class RegexPropertyMutationTests {

	@Test
	void throwsWhenNeitherSideIsDynamic() {
		assertThatThrownBy(() -> new RegexProperty("notAPattern")).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Neither client not server side is dynamic");
	}

	@Test
	void singleValuePatternConstructorSetsPatternAndDefaultClass() {
		Pattern pattern = Pattern.compile("[0-9]+");
		RegexProperty property = new RegexProperty(pattern);
		assertThat(property.getPattern()).isEqualTo(pattern);
		assertThat(property.pattern()).isEqualTo("[0-9]+");
		assertThat(property.getClazz()).isEqualTo(String.class);
	}

	@Test
	void clientDynamicPatternIsUsed() {
		Pattern pattern = Pattern.compile("[a-z]+");
		RegexProperty property = new RegexProperty(pattern, "concrete");
		assertThat(property.pattern()).isEqualTo("[a-z]+");
	}

	@Test
	void serverDynamicPatternIsUsed() {
		Pattern pattern = Pattern.compile("[a-z]+");
		RegexProperty property = new RegexProperty("concrete", pattern);
		assertThat(property.pattern()).isEqualTo("[a-z]+");
	}

	@Test
	void copiesPatternAndClassFromRegexProperty() {
		RegexProperty source = new RegexProperty(Pattern.compile("[0-9]+")).asInteger();
		RegexProperty property = new RegexProperty(source, "x");
		assertThat(property.pattern()).isEqualTo("[0-9]+");
		assertThat(property.getClazz()).isEqualTo(Integer.class);
	}

	@Test
	void matcherMatchesInput() {
		RegexProperty property = new RegexProperty(Pattern.compile("[0-9]+"));
		assertThat(property.matcher("12345").matches()).isTrue();
		assertThat(property.matcher("abc").matches()).isFalse();
	}

	@Test
	void asIntegerSetsIntegerClass() {
		RegexProperty property = new RegexProperty(Pattern.compile("[0-9]+")).asInteger();
		assertThat(property.getClazz()).isEqualTo(Integer.class);
	}

	@Test
	void asDoubleSetsDoubleClass() {
		assertThat(new RegexProperty(Pattern.compile("[0-9]+")).asDouble().getClazz()).isEqualTo(Double.class);
	}

	@Test
	void asFloatSetsFloatClass() {
		assertThat(new RegexProperty(Pattern.compile("[0-9]+")).asFloat().getClazz()).isEqualTo(Float.class);
	}

	@Test
	void asLongSetsLongClass() {
		assertThat(new RegexProperty(Pattern.compile("[0-9]+")).asLong().getClazz()).isEqualTo(Long.class);
	}

	@Test
	void asShortSetsShortClass() {
		assertThat(new RegexProperty(Pattern.compile("[0-9]+")).asShort().getClazz()).isEqualTo(Short.class);
	}

	@Test
	void asStringSetsStringClass() {
		assertThat(new RegexProperty(Pattern.compile("[0-9]+")).asString().getClazz()).isEqualTo(String.class);
	}

	@Test
	void asBooleanTypeSetsBooleanClass() {
		assertThat(new RegexProperty(Pattern.compile("(true|false)")).asBooleanType().getClazz())
			.isEqualTo(Boolean.class);
	}

	@Test
	void asStringWithCharsetSetsCharset() {
		RegexProperty property = new RegexProperty(Pattern.compile("[0-9]+")).asString(StandardCharsets.ISO_8859_1);
		assertThat(property.charset).isEqualTo(StandardCharsets.ISO_8859_1.name());
	}

	@Test
	void asStringWithCharsetNameSetsCharset() {
		RegexProperty property = new RegexProperty(Pattern.compile("[0-9]+")).asString("US-ASCII");
		assertThat(property.charset).isEqualTo("US-ASCII");
	}

	@Test
	void clazzReturnsRegexPropertyClass() {
		assertThat(new RegexProperty(Pattern.compile("[0-9]+")).clazz()).isEqualTo(RegexProperty.class);
	}

	@Test
	void generateProducesString() {
		RegexProperty property = new RegexProperty(Pattern.compile("[0-9]{3}"));
		Object generated = property.generate();
		assertThat(generated).isInstanceOf(String.class);
		assertThat((String) generated).isNotEmpty();
	}

	@Test
	void generateProducesInteger() {
		RegexProperty property = new RegexProperty(Pattern.compile("[1-9][0-9]{2}")).asInteger();
		assertThat(property.generate()).isInstanceOf(Integer.class);
	}

	@Test
	void generateProducesLong() {
		RegexProperty property = new RegexProperty(Pattern.compile("[1-9][0-9]{2}")).asLong();
		assertThat(property.generate()).isInstanceOf(Long.class);
	}

	@Test
	void generateProducesShort() {
		RegexProperty property = new RegexProperty(Pattern.compile("[1-9][0-9]{2}")).asShort();
		assertThat(property.generate()).isInstanceOf(Short.class);
	}

	@Test
	void generateProducesDouble() {
		RegexProperty property = new RegexProperty(Pattern.compile("[1-9][0-9]\\.[0-9]{2}")).asDouble();
		assertThat(property.generate()).isInstanceOf(Double.class);
	}

	@Test
	void generateProducesFloat() {
		RegexProperty property = new RegexProperty(Pattern.compile("[1-9][0-9]\\.[0-9]{2}")).asFloat();
		assertThat(property.generate()).isInstanceOf(Float.class);
	}

	@Test
	void generateProducesBoolean() {
		RegexProperty property = new RegexProperty(Pattern.compile("(true|false)")).asBooleanType();
		assertThat(property.generate()).isInstanceOf(Boolean.class);
	}

	@Test
	void generateConcreteValueDelegatesToGenerate() {
		RegexProperty property = new RegexProperty(Pattern.compile("[0-9]{3}"));
		assertThat(property.generateConcreteValue()).isInstanceOf(String.class);
		assertThat((String) property.generateConcreteValue()).isNotEmpty();
	}

	@Test
	void generateAndEscapeReturnsNumberForNumericClass() {
		RegexProperty property = new RegexProperty(Pattern.compile("[1-9][0-9]{2}")).asInteger();
		assertThat(property.generateAndEscapeJavaStringIfNeeded()).isInstanceOf(Integer.class);
	}

	@Test
	void generateAndEscapeReturnsStringForStringClass() {
		RegexProperty property = new RegexProperty(Pattern.compile("[a-z]{3}")).asString();
		assertThat(property.generateAndEscapeJavaStringIfNeeded()).isInstanceOf(String.class);
	}

	@Test
	void dynamicClientConcreteProducerKeepsPatternOnClient() {
		RegexProperty property = new RegexProperty(Pattern.compile("[0-9]{3}")).dynamicClientConcreteProducer();
		assertThat(property.getClientValue()).isInstanceOf(Pattern.class);
		assertThat(property.pattern()).isEqualTo("[0-9]{3}");
	}

	@Test
	void concreteClientDynamicProducerKeepsPatternOnServer() {
		RegexProperty property = new RegexProperty(Pattern.compile("[0-9]{3}")).concreteClientDynamicProducer();
		assertThat(property.getServerValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void concreteClientEscapedDynamicProducerKeepsPatternOnServer() {
		RegexProperty property = new RegexProperty(Pattern.compile("[0-9]{3}")).concreteClientEscapedDynamicProducer();
		assertThat(property.getServerValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void dynamicClientEscapedConcreteProducerKeepsPatternOnClient() {
		RegexProperty property = new RegexProperty(Pattern.compile("[0-9]{3}")).dynamicClientEscapedConcreteProducer();
		assertThat(property.getClientValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void equalObjectsAreEqual() {
		RegexProperty a = new RegexProperty(Pattern.compile("[0-9]+"));
		RegexProperty b = new RegexProperty(Pattern.compile("[0-9]+"));
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
	}

	@Test
	void differentPatternNotEqual() {
		assertThat(new RegexProperty(Pattern.compile("[0-9]+")))
			.isNotEqualTo(new RegexProperty(Pattern.compile("[a-z]+")));
	}

	@Test
	void differentClassNotEqual() {
		RegexProperty a = new RegexProperty(Pattern.compile("[0-9]+")).asInteger();
		RegexProperty b = new RegexProperty(Pattern.compile("[0-9]+")).asLong();
		assertThat(a).isNotEqualTo(b);
		assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
	}

	@Test
	void equalToItselfAndNotToNullOrOtherType() {
		RegexProperty a = new RegexProperty(Pattern.compile("[0-9]+"));
		assertThat(a.equals(a)).isTrue();
		assertThat(a.equals(null)).isFalse();
		assertThat(a.equals("x")).isFalse();
	}

	@Test
	void toStringReturnsPattern() {
		assertThat(new RegexProperty(Pattern.compile("[0-9]+")).toString()).isEqualTo("[0-9]+");
	}

}
