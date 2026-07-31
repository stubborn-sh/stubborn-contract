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

package sh.stubborn.contract.verifier.messaging.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Tests for {@link ContractVerifierObjectMapper}.
 */
class ContractVerifierObjectMapperTests {

	ContractVerifierObjectMapper mapper = new ContractVerifierObjectMapper();

	@Test
	void should_convert_an_object_into_a_json_representation() throws Exception {
		MyClass input = new MyClass("bar");

		String result = this.mapper.writeValueAsString(input);

		then(result).isEqualTo("{\"foo\":\"bar\"}");
	}

	@Test
	void should_convert_bytes_into_a_json_representation() throws Exception {
		String input = "{\"foo\":\"bar\"}";

		String result = this.mapper.writeValueAsString(input.getBytes());

		then(result).isEqualTo("{\"foo\":\"bar\"}");
	}

	@Test
	void should_convert_an_object_into_a_byte_array_representation() throws Exception {
		MyClass input = new MyClass("bar");

		byte[] result = this.mapper.writeValueAsBytes(input);

		then(result).isEqualTo("{\"foo\":\"bar\"}".getBytes());
	}

	@Test
	void should_convert_a_String_into_a_byte_array_representation() throws Exception {
		byte[] result = this.mapper.writeValueAsBytes("{\"foo\":\"bar\"}");

		then(result).isEqualTo("{\"foo\":\"bar\"}".getBytes());
	}

	@Test
	void should_pass_byte_array_as_a_byte_array_representation() throws Exception {
		byte[] result = this.mapper.writeValueAsBytes("{\"foo\":\"bar\"}".getBytes());

		then(result).isEqualTo("{\"foo\":\"bar\"}".getBytes());
	}

	static class MyClass {

		private final String foo;

		MyClass(String foo) {
			this.foo = foo;
		}

		public String getFoo() {
			return this.foo;
		}

	}

}
