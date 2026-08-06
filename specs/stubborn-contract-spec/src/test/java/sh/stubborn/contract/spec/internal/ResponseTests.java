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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Marcin Grzejszczak
 */
class ResponseTests {

	@Test
	void shouldThrowExceptionWhenOnResponseSideAValueContainsRegexForClient() {
		assertThatThrownBy(() -> {
			Response response = new Response();
			response.value(response.producer("foo"), response.consumer(response.regex("foo")));
		}).isInstanceOf(IllegalStateException.class);

		assertThatThrownBy(() -> {
			Response response = new Response();
			response.value(response.consumer(response.regex("foo")), response.producer("foo"));
		}).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldSetPropertyWhenUsingDollarConvenienceMethod() {
		Response contract = new Response();
		DslProperty<?> property = contract.$(contract.producer(contract.regex("[0-9]{5}")));
		String generatedValue = (String) property.getClientValue();
		int value = Integer.parseInt(generatedValue);
		assertThat(value).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(99_999);
	}

	@Test
	void shouldSetPropertyWhenUsingDollarConvenienceMethodForDouble() {
		Response contract = new Response();
		DslProperty<?> property = contract.$(contract.producer(contract.regex("[0-9]{5}").asDouble()));
		Object value = property.getClientValue();
		assertThat(value).isInstanceOf(Double.class);
		assertThat((Double) value).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(99_999.0);
	}

	@Test
	void shouldSetPropertyWhenUsingDollarConvenienceMethodForShort() {
		Response contract = new Response();
		DslProperty<?> property = contract.$(contract.producer(contract.regex("[0-9]{1}").asShort()));
		Object value = property.getClientValue();
		assertThat(value).isInstanceOf(Short.class);
		assertThat((Short) value).isGreaterThanOrEqualTo((short) 0).isLessThanOrEqualTo((short) 9);
	}

	@Test
	void shouldSetPropertyWhenUsingDollarConvenienceMethodForLong() {
		Response contract = new Response();
		DslProperty<?> property = contract.$(contract.producer(contract.regex("[0-9]{5}").asLong()));
		Object value = property.getClientValue();
		assertThat(value).isInstanceOf(Long.class);
		assertThat((Long) value).isGreaterThanOrEqualTo(0L).isLessThanOrEqualTo(99_999L);
	}

	@Test
	void shouldSetPropertyWhenUsingDollarConvenienceMethodForInteger() {
		Response contract = new Response();
		DslProperty<?> property = contract.$(contract.producer(contract.regex("[0-9]{5}").asInteger()));
		Object value = property.getClientValue();
		assertThat(value).isInstanceOf(Integer.class);
		assertThat((Integer) value).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(99_999);
	}

	@Test
	void shouldSetPropertyWhenUsingDollarConvenienceMethodForFloat() {
		Response contract = new Response();
		DslProperty<?> property = contract.$(contract.producer(contract.regex("[0-9]{5}").asFloat()));
		Object value = property.getClientValue();
		assertThat(value).isInstanceOf(Float.class);
		assertThat((Float) value).isGreaterThanOrEqualTo(0.0f).isLessThanOrEqualTo(99_999.0f);
	}

}
