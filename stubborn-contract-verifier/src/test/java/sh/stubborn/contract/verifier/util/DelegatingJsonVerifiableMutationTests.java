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

package sh.stubborn.contract.verifier.util;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import sh.stubborn.jsonassert.JsonAssertion;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused direct tests for {@link DelegatingJsonVerifiable}: the buffer method
 * string, equality/hashCode contract, matcher compilation and array value handling.
 */
class DelegatingJsonVerifiableMutationTests {

	private static DelegatingJsonVerifiable root(String json) {
		return new DelegatingJsonVerifiable(JsonAssertion.assertThat(json).withoutThrowingException());
	}

	@Test
	void field_varargs_chains_each_field() {
		MethodBufferingJsonVerifiable v = root("{\"a\":{\"b\":\"c\"}}").field("a", "b");
		assertThat(v.method()).isEqualTo(".field(\"['a']\").field(\"['b']\")");
		assertThat(v.jsonPath()).isEqualTo("$.['a'].['b']");
	}

	@Test
	void keyBeforeChecking_returns_delegate_json_path() {
		MethodBufferingJsonVerifiable v = root("{\"a\":\"b\"}").field("a");
		assertThat(v.keyBeforeChecking()).isEqualTo("$.['a']");
	}

	@Test
	void isInstanceOf_appends_fully_qualified_class_name() {
		MethodBufferingJsonVerifiable v = root("{\"a\":\"b\"}").field("a").isInstanceOf(String.class);
		assertThat(v.method()).isEqualTo(".field(\"['a']\").isInstanceOf(java.lang.String)");
	}

	@Test
	void value_marks_the_verifiable_as_finished() {
		MethodBufferingJsonVerifiable v = root("{\"a\":\"b\"}").field("a").value();
		assertThat(v).isInstanceOf(FinishedDelegatingJsonVerifiable.class);
		assertThat(v.method()).isEqualTo(".field(\"['a']\")");
	}

	@Test
	void matches_with_valid_regex_stores_compiled_pattern() {
		MethodBufferingJsonVerifiable v = root("{\"a\":\"123\"}").field("a").matches("[0-9]+");
		assertThat(v.method()).isEqualTo(".field(\"['a']\").matches(\"[0-9]+\")");
		assertThat(v.valueBeforeChecking()).isInstanceOf(Pattern.class);
		assertThat(((Pattern) v.valueBeforeChecking()).pattern()).isEqualTo("[0-9]+");
	}

	@Test
	void matches_with_invalid_regex_falls_back_to_raw_string() {
		MethodBufferingJsonVerifiable v = root("{\"a\":\"x\"}").field("a").matches("[unclosed");
		assertThat(v.valueBeforeChecking()).isInstanceOf(String.class).isEqualTo("[unclosed");
	}

	@Test
	void contains_on_array_value_appends_value_call() {
		MethodBufferingJsonVerifiable arr = root("{\"names\":[\"a\"]}").array("names").arrayField();
		assertThat(arr.isAssertingAValueInArray()).isTrue();
		MethodBufferingJsonVerifiable contained = arr.contains("a");
		assertThat(contained.method()).isEqualTo(".array(\"['names']\").arrayField().contains(\"a\").value()");
	}

	@Test
	void contains_outside_array_value_has_no_value_call() {
		MethodBufferingJsonVerifiable field = root("{\"a\":\"b\"}").field("a");
		assertThat(field.isAssertingAValueInArray()).isFalse();
		MethodBufferingJsonVerifiable contained = field.contains("b");
		assertThat(contained.method()).isEqualTo(".field(\"['a']\").contains(\"b\")");
	}

	@Test
	void assertsConcreteValue_true_only_when_field_or_array_present() {
		MethodBufferingJsonVerifiable withField = root("{\"a\":\"b\"}").field("a").isEqualTo("b");
		assertThat(withField.assertsConcreteValue()).isTrue();

		MethodBufferingJsonVerifiable rootEquality = root("{}").isEqualTo("b");
		assertThat(rootEquality.assertsConcreteValue()).isFalse();
	}

	@Test
	void read_delegates_to_underlying_verifiable() {
		MethodBufferingJsonVerifiable v = root("{\"a\":\"b\"}").field("a");
		assertThat(v.read(String.class)).isEqualTo("b");
	}

	@Test
	void equals_reflexive_null_and_type_and_buffer_based() {
		MethodBufferingJsonVerifiable a = root("{\"a\":\"b\"}").field("a");
		MethodBufferingJsonVerifiable aAgain = root("{\"a\":\"b\"}").field("a");
		MethodBufferingJsonVerifiable b = root("{\"b\":\"c\"}").field("b");

		assertThat(a).isEqualTo(a);
		assertThat(a).isEqualTo(aAgain);
		assertThat(a).isNotEqualTo(b);
		assertThat(a.equals(null)).isFalse();
		assertThat(a.equals("not a verifiable")).isFalse();
	}

	@Test
	void hashCode_is_31_times_buffer_string_hash() {
		DelegatingJsonVerifiable v = (DelegatingJsonVerifiable) root("{\"a\":\"b\"}").field("a");
		int expected = 31 * v.methodsBuffer.toString().hashCode();
		assertThat(v.hashCode()).isEqualTo(expected);
		assertThat(v.hashCode()).isNotZero();
	}

	@Test
	void toString_contains_buffer_and_delegate() {
		DelegatingJsonVerifiable v = (DelegatingJsonVerifiable) root("{\"a\":\"b\"}").field("a");
		assertThat(v.toString()).contains("DelegatingJsonVerifiable{")
			.contains("methodsBuffer=")
			.contains(".field(\"['a']\")");
	}

	@Test
	void withoutThrowingException_returns_a_verifiable() {
		DelegatingJsonVerifiable v = (DelegatingJsonVerifiable) root("{\"a\":\"b\"}").field("a");
		assertThat(v.withoutThrowingException()).isNotNull();
	}

}
