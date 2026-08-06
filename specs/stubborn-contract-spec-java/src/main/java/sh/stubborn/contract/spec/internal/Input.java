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

import java.util.Objects;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * Represents an input for messaging. The input can be a message or some action inside the
 * application.
 *
 * @author Marcin Grzejszczak
 * @author Tim Ysewyn
 * @since 1.0.0
 */
public class Input extends Common implements RegexCreatingProperty<ClientDslProperty> {

	private ClientPatternValueDslProperty property = new ClientPatternValueDslProperty();

	private @Nullable ExecutionProperty triggeredBy;

	private @Nullable ExecutionProperty assertThat;

	/**
	 * Function that needs to be executed to trigger action in the system.
	 * @param triggeredBy method name that triggers the message
	 */
	public void triggeredBy(String triggeredBy) {
		this.triggeredBy = new ExecutionProperty(triggeredBy);
	}

	public DslProperty value(ClientDslProperty client) {
		Object dynamicValue = client.getClientValue();
		Object concreteValue = client.getServerValue();
		if (dynamicValue instanceof RegexProperty) {
			return ((RegexProperty) dynamicValue).dynamicClientConcreteProducer();
		}
		return new DslProperty(dynamicValue, concreteValue);
	}

	public DslProperty value(RegexProperty prop) {
		return value(client(prop));
	}

	public DslProperty $(RegexProperty prop) {
		return value(client(prop));
	}

	public DslProperty $(ClientDslProperty client) {
		return value(client);
	}

	@Override
	public RegexProperty regexProperty(Object object) {
		return new RegexProperty(object).dynamicClientConcreteProducer();
	}

	public void assertThat(String assertThat) {
		this.assertThat = new ExecutionProperty(assertThat);
	}

	public ClientPatternValueDslProperty getProperty() {
		return this.property;
	}

	public void setProperty(ClientPatternValueDslProperty property) {
		this.property = property;
	}

	public @Nullable ExecutionProperty getTriggeredBy() {
		return this.triggeredBy;
	}

	public void setTriggeredBy(@Nullable ExecutionProperty triggeredBy) {
		this.triggeredBy = triggeredBy;
	}

	public @Nullable ExecutionProperty getAssertThat() {
		return this.assertThat;
	}

	public void setAssertThat(@Nullable ExecutionProperty assertThat) {
		this.assertThat = assertThat;
	}

	@Override
	public ClientDslProperty anyAlphaUnicode() {
		return this.property.anyAlphaUnicode();
	}

	@Override
	public ClientDslProperty anyAlphaNumeric() {
		return this.property.anyAlphaNumeric();
	}

	@Override
	public ClientDslProperty anyNumber() {
		return this.property.anyNumber();
	}

	@Override
	public ClientDslProperty anyInteger() {
		return this.property.anyInteger();
	}

	@Override
	public ClientDslProperty anyPositiveInt() {
		return this.property.anyPositiveInt();
	}

	@Override
	public ClientDslProperty anyDouble() {
		return this.property.anyDouble();
	}

	@Override
	public ClientDslProperty anyHex() {
		return this.property.anyHex();
	}

	@Override
	public ClientDslProperty aBoolean() {
		return this.property.aBoolean();
	}

	@Override
	public ClientDslProperty anyIpAddress() {
		return this.property.anyIpAddress();
	}

	@Override
	public ClientDslProperty anyHostname() {
		return this.property.anyHostname();
	}

	@Override
	public ClientDslProperty anyEmail() {
		return this.property.anyEmail();
	}

	@Override
	public ClientDslProperty anyUrl() {
		return this.property.anyUrl();
	}

	@Override
	public ClientDslProperty anyHttpsUrl() {
		return this.property.anyHttpsUrl();
	}

	@Override
	public ClientDslProperty anyUuid() {
		return this.property.anyUuid();
	}

	@Override
	public ClientDslProperty anyDate() {
		return this.property.anyDate();
	}

	@Override
	public ClientDslProperty anyDateTime() {
		return this.property.anyDateTime();
	}

	@Override
	public ClientDslProperty anyTime() {
		return this.property.anyTime();
	}

	@Override
	public ClientDslProperty anyIso8601WithOffset() {
		return this.property.anyIso8601WithOffset();
	}

	@Override
	public ClientDslProperty anyNonBlankString() {
		return this.property.anyNonBlankString();
	}

	@Override
	public ClientDslProperty anyNonEmptyString() {
		return this.property.anyNonEmptyString();
	}

	@Override
	public ClientDslProperty anyOf(String... values) {
		return this.property.anyOf(values);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Input input = (Input) o;
		return Objects.equals(this.triggeredBy, input.triggeredBy) && Objects.equals(this.assertThat, input.assertThat);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.triggeredBy, this.assertThat);
	}

	@Override
	public String toString() {
		return "Input{\n\t" + ", \n\ttriggeredBy=" + this.triggeredBy + ", \n\tassertThat=" + this.assertThat + "} \n\t"
				+ super.toString();
	}

	private static final class ClientPatternValueDslProperty extends PatternValueDslProperty<ClientDslProperty> {

		@Override
		protected ClientDslProperty createProperty(Pattern pattern, @Nullable Object generatedValue) {
			return new ClientDslProperty(pattern, generatedValue);
		}

	}

}
