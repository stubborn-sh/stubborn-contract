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
class RequestTests {

	@Test
	void shouldThrowExceptionWhenOnRequestSideAValueContainsRegexForServer() {
		assertThatThrownBy(() -> {
			Request contract = new Request();
			contract.value(contract.consumer("foo"), contract.producer(contract.regex("foo")));
		}).isInstanceOf(IllegalStateException.class);

		assertThatThrownBy(() -> {
			Request contract = new Request();
			contract.value(contract.producer(contract.regex("foo")), contract.consumer("foo"));
		}).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldSetPropertyWhenUsingDollarConvenienceMethod() {
		Request contract = new Request();
		DslProperty<?> property = contract.value(contract.consumer(contract.regex("[0-9]{5}")));
		String generatedValue = (String) property.getServerValue();
		int value = Integer.parseInt(generatedValue);
		assertThat(value).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(99_999);
	}

	@Test
	void shouldSetPropertyWhenUsingDollarConvenienceMethodForDouble() {
		Request contract = new Request();
		DslProperty<?> property = contract.value(contract.consumer(contract.regex("[0-9]{5}").asDouble()));
		Object value = property.getServerValue();
		assertThat(value).isInstanceOf(Double.class);
		assertThat((Double) value).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(99_999.0);
	}

	@Test
	void shouldSetPropertyWhenUsingDollarConvenienceMethodForShort() {
		Request contract = new Request();
		DslProperty<?> property = contract.value(contract.consumer(contract.regex("[0-9]{1}").asShort()));
		Object value = property.getServerValue();
		assertThat(value).isInstanceOf(Short.class);
		assertThat((Short) value).isGreaterThanOrEqualTo((short) 0).isLessThanOrEqualTo((short) 9);
	}

	@Test
	void shouldSetPropertyWhenUsingDollarConvenienceMethodForLong() {
		Request contract = new Request();
		DslProperty<?> property = contract.value(contract.consumer(contract.regex("[0-9]{5}").asLong()));
		Object value = property.getServerValue();
		assertThat(value).isInstanceOf(Long.class);
		assertThat((Long) value).isGreaterThanOrEqualTo(0L).isLessThanOrEqualTo(99_999L);
	}

	@Test
	void shouldSetPropertyWhenUsingDollarConvenienceMethodForInteger() {
		Request contract = new Request();
		DslProperty<?> property = contract.value(contract.consumer(contract.regex("[0-9]{5}").asInteger()));
		Object value = property.getServerValue();
		assertThat(value).isInstanceOf(Integer.class);
		assertThat((Integer) value).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(99_999);
	}

	@Test
	void shouldSetPropertyWhenUsingDollarConvenienceMethodForFloat() {
		Request contract = new Request();
		DslProperty<?> property = contract.value(contract.consumer(contract.regex("[0-9]{5}").asFloat()));
		Object value = property.getServerValue();
		assertThat(value).isInstanceOf(Float.class);
		assertThat((Float) value).isGreaterThanOrEqualTo(0.0f).isLessThanOrEqualTo(99_999.0f);
	}

	@Test
	void shouldSetAValueIfOnlyRegexIsPassedForClient() {
		Request request = new Request();
		DslProperty<?> property = request.value(request.consumer(request.regex("[0-9]{5}")));
		int value = Integer.parseInt((String) property.getServerValue());
		assertThat(value).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(99_999);
	}

}
