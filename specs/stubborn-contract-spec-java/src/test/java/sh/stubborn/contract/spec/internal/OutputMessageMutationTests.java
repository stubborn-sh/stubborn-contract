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
class OutputMessageMutationTests {

	private final OutputMessage message = new OutputMessage();

	@Test
	void sentToFromString() {
		this.message.sentTo("queue");
		assertThat(this.message.getSentTo().getClientValue()).isEqualTo("queue");
	}

	@Test
	void sentToFromDslProperty() {
		this.message.sentTo(new DslProperty<>("queue"));
		assertThat(this.message.getSentTo().getClientValue()).isEqualTo("queue");
	}

	@Test
	void bodyFromObject() {
		this.message.body((Object) "text");
		assertThat(this.message.getBody().getClientValue()).isEqualTo("text");
	}

	@Test
	void bodyFromDslProperty() {
		this.message.body(new DslProperty("c", "s"));
		assertThat(this.message.getBody().getClientValue()).isEqualTo("c");
	}

	@Test
	void assertThatSetsExecutionProperty() {
		this.message.assertThat("verify()");
		assertThat(this.message.getAssertThat().getExecutionCommand()).isEqualTo("verify()");
	}

	@Test
	void valueFromServerDslPropertyPlain() {
		DslProperty property = this.message.value(new ServerDslProperty("s", "c"));
		assertThat(property.getClientValue()).isEqualTo("c");
		assertThat(property.getServerValue()).isEqualTo("s");
	}

	@Test
	void valueFromRegexPropertyKeepsPatternOnServer() {
		DslProperty property = this.message.value(new RegexProperty(Pattern.compile("[0-9]{3}")));
		assertThat(property.getServerValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void dollarServerDslProperty() {
		DslProperty property = this.message.$(new ServerDslProperty("s", "c"));
		assertThat(property.getServerValue()).isEqualTo("s");
	}

	@Test
	void dollarPattern() {
		DslProperty property = this.message.$(Pattern.compile("[0-9]{3}"));
		assertThat(property.getServerValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void dollarRegexProperty() {
		DslProperty property = this.message.$(new RegexProperty(Pattern.compile("[0-9]{3}")));
		assertThat(property.getServerValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void dollarOptionalProperty() {
		DslProperty property = this.message.$(new OptionalProperty("abc"));
		assertThat(property.getServerValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void regexPropertyOverride() {
		RegexProperty property = this.message.regexProperty(Pattern.compile("[0-9]{3}"));
		assertThat(property.getServerValue()).isInstanceOf(Pattern.class);
	}

	@Test
	void settersAndGetters() {
		this.message.setSentTo(new DslProperty<>("q"));
		assertThat(this.message.getSentTo().getClientValue()).isEqualTo("q");
		Headers headers = new Headers();
		this.message.setHeaders(headers);
		assertThat(this.message.getHeaders()).isSameAs(headers);
		Body body = new Body("x");
		this.message.setBody(body);
		assertThat(this.message.getBody()).isSameAs(body);
		this.message.setAssertThat(new ExecutionProperty("a()"));
		assertThat(this.message.getAssertThat().getExecutionCommand()).isEqualTo("a()");
		ResponseBodyMatchers matchers = new ResponseBodyMatchers();
		this.message.setBodyMatchers(matchers);
		assertThat(this.message.getBodyMatchers()).isSameAs(matchers);
		var property = this.message.getProperty();
		assertThat(property).isNotNull();
		this.message.setProperty(property);
		assertThat(this.message.getProperty()).isSameAs(property);
	}

	@Test
	void anyDelegationsReturnServerDslProperties() {
		assertThat(this.message.anyAlphaUnicode().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.message.anyAlphaNumeric().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.message.anyNumber().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.message.anyInteger().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.message.anyPositiveInt().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.message.anyDouble().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.message.anyHex().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.message.aBoolean().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.message.anyIpAddress().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.message.anyHostname().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.message.anyEmail().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.message.anyUrl().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.message.anyHttpsUrl().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.message.anyUuid().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.message.anyDate().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.message.anyDateTime().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.message.anyTime().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.message.anyIso8601WithOffset().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.message.anyNonBlankString().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.message.anyNonEmptyString().getServerValue()).isInstanceOf(Pattern.class);
		assertThat(this.message.anyOf("a", "b").getClientValue()).isIn("a", "b");
	}

	@Test
	void headersConsumerCreatesHeaders() {
		this.message.headers((h) -> h.header("A", "1"));
		assertThat(this.message.getHeaders()).isNotNull();
		assertThat(this.message.getHeaders().getEntries()).extracting(Header::getName).containsExactly("A");
	}

	@Test
	void bodyMatchersConsumer() {
		this.message.bodyMatchers((m) -> m.jsonPath("$.a", m.byEquality()));
		assertThat(this.message.getBodyMatchers().hasMatchers()).isTrue();
	}

	@Test
	void constructorCopiesFields() {
		this.message.sentTo("q");
		this.message.body((Object) "b");
		OutputMessage copy = new OutputMessage(this.message);
		assertThat(copy.getSentTo().getClientValue()).isEqualTo("q");
		assertThat(copy.getBody().getClientValue()).isEqualTo("b");
	}

	@Test
	void equalsHashCode() {
		OutputMessage a = new OutputMessage();
		a.sentTo("q");
		OutputMessage b = new OutputMessage();
		b.sentTo("q");
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a.equals(a)).isTrue();
		assertThat(a.equals(null)).isFalse();
		assertThat(a.equals("x")).isFalse();
	}

	@Test
	void notEqualWhenSentToDiffers() {
		OutputMessage a = new OutputMessage();
		a.sentTo("q1");
		OutputMessage b = new OutputMessage();
		b.sentTo("q2");
		assertThat(a).isNotEqualTo(b);
		assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
	}

	@Test
	void notEqualWhenBodyDiffers() {
		OutputMessage a = new OutputMessage();
		a.body((Object) "b1");
		OutputMessage b = new OutputMessage();
		b.body((Object) "b2");
		assertThat(a).isNotEqualTo(b);
	}

	@Test
	void toStringContainsFields() {
		this.message.sentTo("queue");
		assertThat(this.message.toString()).contains("OutputMessage").contains("sentTo").contains("queue");
	}

}
