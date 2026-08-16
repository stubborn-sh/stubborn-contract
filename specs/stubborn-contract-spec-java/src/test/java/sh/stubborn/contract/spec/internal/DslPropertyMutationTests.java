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
class DslPropertyMutationTests {

	@Test
	void twoArgConstructorKeepsBothSides() {
		DslProperty<String> property = new DslProperty<>("client", "server");
		assertThat(property.getClientValue()).isEqualTo("client");
		assertThat(property.getServerValue()).isEqualTo("server");
	}

	@Test
	void singleArgConstructorUsesSameValueForBothSides() {
		DslProperty<String> property = new DslProperty<>("both");
		assertThat(property.getClientValue()).isEqualTo("both");
		assertThat(property.getServerValue()).isEqualTo("both");
	}

	@Test
	void isSingleValueWhenSidesEqual() {
		assertThat(new DslProperty<>("a", "a").isSingleValue()).isTrue();
	}

	@Test
	void isSingleValueWhenServerNull() {
		assertThat(new DslProperty<>("a", null).isSingleValue()).isTrue();
	}

	@Test
	void isSingleValueWhenClientNull() {
		assertThat(new DslProperty<>(null, "a").isSingleValue()).isTrue();
	}

	@Test
	void isNotSingleValueWhenSidesDiffer() {
		assertThat(new DslProperty<>("a", "b").isSingleValue()).isFalse();
	}

	@Test
	void equalsAndHashCodeForEqualObjects() {
		DslProperty<String> a = new DslProperty<>("c", "s");
		DslProperty<String> b = new DslProperty<>("c", "s");
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
	}

	@Test
	void notEqualWhenClientDiffers() {
		assertThat(new DslProperty<>("c1", "s")).isNotEqualTo(new DslProperty<>("c2", "s"));
		assertThat(new DslProperty<>("c1", "s").hashCode()).isNotEqualTo(new DslProperty<>("c2", "s").hashCode());
	}

	@Test
	void notEqualWhenServerDiffers() {
		assertThat(new DslProperty<>("c", "s1")).isNotEqualTo(new DslProperty<>("c", "s2"));
	}

	@Test
	void equalToItself() {
		DslProperty<String> a = new DslProperty<>("c", "s");
		assertThat(a.equals(a)).isTrue();
	}

	@Test
	void notEqualToNull() {
		assertThat(new DslProperty<>("c", "s").equals(null)).isFalse();
	}

	@Test
	void notEqualToDifferentType() {
		assertThat(new DslProperty<>("c", "s").equals("string")).isFalse();
	}

	@Test
	void patternsComparedByPatternString() {
		DslProperty<Object> a = new DslProperty<>(Pattern.compile("[0-9]+"), "srv");
		DslProperty<Object> b = new DslProperty<>(Pattern.compile("[0-9]+"), "srv");
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
	}

	@Test
	void differentPatternsNotEqual() {
		DslProperty<Object> a = new DslProperty<>(Pattern.compile("[0-9]+"), "srv");
		DslProperty<Object> b = new DslProperty<>(Pattern.compile("[a-z]+"), "srv");
		assertThat(a).isNotEqualTo(b);
	}

	@Test
	void toStringContainsBothValues() {
		DslProperty<String> property = new DslProperty<>("theClient", "theServer");
		assertThat(property.toString()).contains("DslProperty").contains("theClient").contains("theServer");
	}

	@Test
	void clientDslPropertySingleValue() {
		ClientDslProperty property = new ClientDslProperty("v");
		assertThat(property.getClientValue()).isEqualTo("v");
		assertThat(property.getServerValue()).isEqualTo("v");
	}

	@Test
	void clientDslPropertyTwoArgKeepsClientThenServer() {
		ClientDslProperty property = new ClientDslProperty("c", "s");
		assertThat(property.getClientValue()).isEqualTo("c");
		assertThat(property.getServerValue()).isEqualTo("s");
	}

	@Test
	void serverDslPropertySingleValue() {
		ServerDslProperty property = new ServerDslProperty("v");
		assertThat(property.getClientValue()).isEqualTo("v");
		assertThat(property.getServerValue()).isEqualTo("v");
	}

	@Test
	void serverDslPropertyTwoArgSwapsArguments() {
		// ServerDslProperty(server, client) -> super(client, server)
		ServerDslProperty property = new ServerDslProperty("server", "client");
		assertThat(property.getServerValue()).isEqualTo("server");
		assertThat(property.getClientValue()).isEqualTo("client");
	}

	@Test
	void notToEscapePatternSingleValue() {
		Pattern pattern = Pattern.compile("abc");
		NotToEscapePattern property = new NotToEscapePattern(pattern);
		assertThat(property.getClientValue()).isEqualTo(pattern);
		assertThat(property.getServerValue()).isEqualTo(pattern);
	}

	@Test
	void notToEscapePatternTwoValues() {
		Pattern client = Pattern.compile("abc");
		Pattern server = Pattern.compile("def");
		NotToEscapePattern property = new NotToEscapePattern(client, server);
		assertThat(property.getClientValue()).isEqualTo(client);
		assertThat(property.getServerValue()).isEqualTo(server);
	}

}
