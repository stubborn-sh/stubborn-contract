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
class InputMutationTests {

	private final Input input = new Input();

	@Test
	void triggeredBySetsExecutionProperty() {
		this.input.triggeredBy("method()");
		assertThat(this.input.getTriggeredBy().getExecutionCommand()).isEqualTo("method()");
	}

	@Test
	void assertThatSetsExecutionProperty() {
		this.input.assertThat("verify()");
		assertThat(this.input.getAssertThat().getExecutionCommand()).isEqualTo("verify()");
	}

	@Test
	void valueFromClientDslPropertyPlain() {
		DslProperty property = this.input.value(new ClientDslProperty("c", "s"));
		assertThat(property.getClientValue()).isEqualTo("c");
		assertThat(property.getServerValue()).isEqualTo("s");
	}

	@Test
	void valueFromRegexProperty() {
		DslProperty property = this.input.value(new RegexProperty(Pattern.compile("[0-9]{3}")));
		assertThat(property.getClientValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void dollarRegexProperty() {
		DslProperty property = this.input.$(new RegexProperty(Pattern.compile("[0-9]{3}")));
		assertThat(property.getClientValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void dollarClientDslProperty() {
		DslProperty property = this.input.$(new ClientDslProperty("c", "s"));
		assertThat(property.getClientValue()).isEqualTo("c");
	}

	@Test
	void regexPropertyOverride() {
		RegexProperty property = this.input.regexProperty(Pattern.compile("[0-9]{3}"));
		assertThat(property.getClientValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void settersAndGetters() {
		this.input.setTriggeredBy(new ExecutionProperty("t()"));
		assertThat(this.input.getTriggeredBy().getExecutionCommand()).isEqualTo("t()");
		this.input.setAssertThat(new ExecutionProperty("a()"));
		assertThat(this.input.getAssertThat().getExecutionCommand()).isEqualTo("a()");
		var property = this.input.getProperty();
		assertThat(property).isNotNull();
		this.input.setProperty(property);
		assertThat(this.input.getProperty()).isSameAs(property);
	}

	@Test
	void anyDelegationsReturnClientDslProperties() {
		assertThat(this.input.anyAlphaUnicode().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.input.anyAlphaNumeric().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.input.anyNumber().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.input.anyInteger().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.input.anyPositiveInt().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.input.anyDouble().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.input.anyHex().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.input.aBoolean().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.input.anyIpAddress().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.input.anyHostname().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.input.anyEmail().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.input.anyUrl().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.input.anyHttpsUrl().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.input.anyUuid().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.input.anyDate().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.input.anyDateTime().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.input.anyTime().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.input.anyIso8601WithOffset().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.input.anyNonBlankString().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.input.anyNonEmptyString().getClientValue()).isInstanceOf(Pattern.class);
		assertThat(this.input.anyOf("a", "b").getServerValue()).isIn("a", "b");
	}

	@Test
	void equalsHashCode() {
		Input a = new Input();
		a.triggeredBy("t()");
		Input b = new Input();
		b.triggeredBy("t()");
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a.equals(a)).isTrue();
		assertThat(a.equals(null)).isFalse();
		assertThat(a.equals("x")).isFalse();
	}

	@Test
	void notEqualWhenTriggeredByDiffers() {
		Input a = new Input();
		a.triggeredBy("t1()");
		Input b = new Input();
		b.triggeredBy("t2()");
		assertThat(a).isNotEqualTo(b);
		assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
	}

	@Test
	void notEqualWhenAssertThatDiffers() {
		Input a = new Input();
		a.assertThat("a1()");
		Input b = new Input();
		b.assertThat("a2()");
		assertThat(a).isNotEqualTo(b);
	}

	@Test
	void toStringContainsFields() {
		this.input.triggeredBy("trigger()");
		assertThat(this.input.toString()).contains("Input").contains("trigger()");
	}

}
