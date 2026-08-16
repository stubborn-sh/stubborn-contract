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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NullAway")
class PropertyValueMutationTests {

	// --- OptionalProperty ---

	@Test
	void optionalPatternWrapsValue() {
		OptionalProperty property = new OptionalProperty("abc");
		assertThat(property.value()).isEqualTo("abc");
		assertThat(property.optionalPattern()).isEqualTo("(abc)?");
	}

	@Test
	void optionalValueForNullIsEmpty() {
		OptionalProperty property = new OptionalProperty(null);
		assertThat(property.value()).isEmpty();
		assertThat(property.optionalPattern()).isEqualTo("()?");
	}

	@Test
	void optionalValueUnwrapsClientDslProperty() {
		OptionalProperty property = new OptionalProperty(new ClientDslProperty("cv"));
		assertThat(property.value()).isEqualTo("cv");
	}

	@Test
	void optionalValueUnwrapsServerDslProperty() {
		OptionalProperty property = new OptionalProperty(new ServerDslProperty("sv"));
		assertThat(property.value()).isEqualTo("sv");
	}

	@Test
	void optionalValueUsesPatternForRegex() {
		OptionalProperty property = new OptionalProperty(Pattern.compile("[0-9]+"));
		assertThat(property.value()).isEqualTo("[0-9]+");
	}

	@Test
	void optionalPatternValueCompiles() {
		OptionalProperty property = new OptionalProperty("abc");
		Pattern pattern = property.optionalPatternValue();
		assertThat(pattern.pattern()).isEqualTo("(abc)?");
		assertThat("abc").matches(pattern.pattern());
		assertThat("").matches(pattern.pattern());
	}

	@Test
	void optionalToStringIsPattern() {
		assertThat(new OptionalProperty("abc").toString()).isEqualTo("(abc)?");
	}

	@Test
	void optionalGenerateConcreteValueReturnsString() {
		OptionalProperty property = new OptionalProperty("abc");
		Object generated = property.generateConcreteValue();
		assertThat(generated).isInstanceOf(String.class);
	}

	@Test
	void optionalGetValue() {
		assertThat(new OptionalProperty("abc").getValue()).isEqualTo("abc");
	}

	// --- ExecutionProperty ---

	@Test
	void executionInsertValueReplacesPlaceholder() {
		ExecutionProperty property = new ExecutionProperty("assertThat($it).isEqualTo(1)");
		assertThat(property.insertValue("value")).isEqualTo("assertThat(value).isEqualTo(1)");
	}

	@Test
	void executionInsertValueWithoutPlaceholderIsUnchanged() {
		ExecutionProperty property = new ExecutionProperty("noPlaceholder");
		assertThat(property.insertValue("value")).isEqualTo("noPlaceholder");
	}

	@Test
	void executionGetCommand() {
		assertThat(new ExecutionProperty("cmd").getExecutionCommand()).isEqualTo("cmd");
	}

	@Test
	void executionEqualsHashCode() {
		ExecutionProperty a = new ExecutionProperty("cmd");
		ExecutionProperty b = new ExecutionProperty("cmd");
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a.equals(a)).isTrue();
		assertThat(a.equals(null)).isFalse();
		assertThat(a.equals("x")).isFalse();
		assertThat(a).isNotEqualTo(new ExecutionProperty("other"));
		assertThat(a.hashCode()).isNotEqualTo(new ExecutionProperty("other").hashCode());
	}

	@Test
	void executionToStringIsCommand() {
		assertThat(new ExecutionProperty("cmd").toString()).isEqualTo("cmd");
	}

	// --- NamedProperty ---

	@Test
	void namedPropertyTwoArgHasNullContentType() {
		NamedProperty property = new NamedProperty(new DslProperty("n"), new DslProperty("v"));
		assertThat(property.getName().getClientValue()).isEqualTo("n");
		assertThat(property.getValue().getClientValue()).isEqualTo("v");
		assertThat(property.getContentType()).isNull();
	}

	@Test
	void namedPropertyThreeArg() {
		NamedProperty property = new NamedProperty(new DslProperty("n"), new DslProperty("v"), new DslProperty("ct"));
		assertThat(property.getContentType().getClientValue()).isEqualTo("ct");
	}

	@Test
	void namedPropertySetters() {
		NamedProperty property = new NamedProperty(null, null);
		property.setName(new DslProperty("n"));
		property.setValue(new DslProperty("v"));
		property.setContentType(new DslProperty("ct"));
		assertThat(property.getName().getClientValue()).isEqualTo("n");
		assertThat(property.getValue().getClientValue()).isEqualTo("v");
		assertThat(property.getContentType().getClientValue()).isEqualTo("ct");
	}

	@Test
	void namedPropertyAsDslPropertyForNull() {
		assertThat(NamedProperty.asDslProperty(null)).isNull();
	}

	@Test
	void namedPropertyAsDslPropertyReturnsSameDslProperty() {
		DslProperty property = new DslProperty("v");
		assertThat(NamedProperty.asDslProperty(property)).isSameAs(property);
	}

	@Test
	void namedPropertyAsDslPropertyWrapsPlainObject() {
		DslProperty property = NamedProperty.asDslProperty("v");
		assertThat(property.getClientValue()).isEqualTo("v");
	}

	@Test
	void namedPropertyFromMap() {
		Map<String, DslProperty> map = new LinkedHashMap<>();
		map.put("name", new DslProperty("n"));
		map.put("content", new DslProperty("v"));
		map.put("contentType", new DslProperty("ct"));
		NamedProperty property = new NamedProperty(map);
		assertThat(property.getName().getClientValue()).isEqualTo("n");
		assertThat(property.getValue().getClientValue()).isEqualTo("v");
		assertThat(property.getContentType().getClientValue()).isEqualTo("ct");
	}

	@Test
	void namedPropertyEqualsHashCode() {
		NamedProperty a = new NamedProperty(new DslProperty("n"), new DslProperty("v"));
		NamedProperty b = new NamedProperty(new DslProperty("n"), new DslProperty("v"));
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a.equals(a)).isTrue();
		assertThat(a.equals(null)).isFalse();
		assertThat(a.equals("x")).isFalse();
	}

	@Test
	void namedPropertyNotEqualWhenNameDiffers() {
		assertThat(new NamedProperty(new DslProperty("n1"), new DslProperty("v")))
			.isNotEqualTo(new NamedProperty(new DslProperty("n2"), new DslProperty("v")));
		assertThat(new NamedProperty(new DslProperty("n1"), new DslProperty("v")).hashCode())
			.isNotEqualTo(new NamedProperty(new DslProperty("n2"), new DslProperty("v")).hashCode());
	}

	@Test
	void namedPropertyNotEqualWhenValueDiffers() {
		assertThat(new NamedProperty(new DslProperty("n"), new DslProperty("v1")))
			.isNotEqualTo(new NamedProperty(new DslProperty("n"), new DslProperty("v2")));
	}

	@Test
	void namedPropertyNotEqualWhenContentTypeDiffers() {
		assertThat(new NamedProperty(new DslProperty("n"), new DslProperty("v"), new DslProperty("ct1")))
			.isNotEqualTo(new NamedProperty(new DslProperty("n"), new DslProperty("v"), new DslProperty("ct2")));
	}

	@Test
	void namedPropertyToString() {
		NamedProperty property = new NamedProperty(new DslProperty("theName"), new DslProperty("theValue"));
		assertThat(property.toString()).contains("NamedProperty").contains("theName").contains("theValue");
	}

	// --- Multipart ---

	@Test
	void multipartFromValue() {
		Multipart multipart = new Multipart("v");
		assertThat(multipart.getClientValue()).isEqualTo("v");
		assertThat(multipart.getServerValue()).isEqualTo("v");
	}

	@Test
	@SuppressWarnings("unchecked")
	void multipartFromMap() {
		Map<String, DslProperty> map = new LinkedHashMap<>();
		map.put("a", new DslProperty("c", "s"));
		Multipart multipart = new Multipart(map);
		assertThat(multipart.getClientValue()).isInstanceOf(Map.class);
		assertThat((Map<String, Object>) multipart.getClientValue()).containsEntry("a", "c");
		assertThat((Map<String, Object>) multipart.getServerValue()).containsEntry("a", "s");
	}

	@Test
	@SuppressWarnings("unchecked")
	void multipartFromList() {
		Multipart multipart = new Multipart(java.util.List.of(new DslProperty("c", "s")));
		assertThat((java.util.List<Object>) multipart.getClientValue()).containsExactly("c");
		assertThat((java.util.List<Object>) multipart.getServerValue()).containsExactly("s");
	}

	@Test
	void multipartFromDslProperty() {
		Multipart multipart = new Multipart(new DslProperty("c", "s"));
		assertThat(multipart.getClientValue()).isEqualTo("c");
		assertThat(multipart.getServerValue()).isEqualTo("s");
	}

	@Test
	void multipartBuildWithMatchingStrategy() {
		MatchingStrategy strategy = new MatchingStrategy("v", MatchingStrategy.Type.EQUAL_TO);
		Multipart multipart = Multipart.build(strategy);
		assertThat(multipart.getClientValue()).isEqualTo(strategy);
	}

	@Test
	void multipartBuildWithPlainValue() {
		Multipart multipart = Multipart.build("v");
		assertThat(multipart.getClientValue()).isEqualTo("v");
	}

	// --- Body ---

	@Test
	void bodyFromString() {
		Body body = new Body("hello");
		assertThat(body.getClientValue()).isEqualTo("hello");
		assertThat(body.getServerValue()).isEqualTo("hello");
	}

	@Test
	@SuppressWarnings("unchecked")
	void bodyFromMap() {
		Map<String, DslProperty> map = new LinkedHashMap<>();
		map.put("a", new DslProperty("c", "s"));
		Body body = new Body(map);
		assertThat((Map<String, Object>) body.getClientValue()).containsEntry("a", "c");
		assertThat((Map<String, Object>) body.getServerValue()).containsEntry("a", "s");
	}

	@Test
	@SuppressWarnings("unchecked")
	void bodyFromList() {
		Body body = new Body(java.util.List.of(new DslProperty("c", "s")));
		assertThat((java.util.List<Object>) body.getClientValue()).containsExactly("c");
		assertThat((java.util.List<Object>) body.getServerValue()).containsExactly("s");
	}

	@Test
	void bodyFromDslProperty() {
		Body body = new Body(new DslProperty("c", "s"));
		assertThat(body.getClientValue()).isEqualTo("c");
		assertThat(body.getServerValue()).isEqualTo("s");
	}

	@Test
	void bodyFromNumber() {
		Body body = new Body(42);
		assertThat(body.getClientValue()).isEqualTo(42);
	}

	@Test
	void bodyFromMatchingStrategy() {
		MatchingStrategy strategy = new MatchingStrategy("v", MatchingStrategy.Type.EQUAL_TO);
		Body body = new Body(strategy);
		assertThat(body.getClientValue()).isEqualTo(strategy);
		assertThat(body.getServerValue()).isEqualTo(strategy);
	}

}
