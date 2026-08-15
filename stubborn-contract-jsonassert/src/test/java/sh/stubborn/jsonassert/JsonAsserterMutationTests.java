/*
 * Copyright 2013-2025 the original author or authors.
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

package sh.stubborn.jsonassert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.DocumentContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Tests focused on killing surviving / uncovered PIT mutations in {@link JsonAsserter},
 * {@link JsonPathAssert} and the small assertion subtypes. They assert on the built JSON
 * Path strings, the fluent return values and both branches of the conditionals.
 *
 * @author Marcin Grzejszczak
 */
// Deliberately passes null into @NonNull fluent parameters to exercise the
// null -> isNull() dispatch that several mutations live in.
@SuppressWarnings("NullAway")
class JsonAsserterMutationTests {

	private static DocumentContext parse(String json) {
		return com.jayway.jsonpath.JsonPath.parse(json);
	}

	// --- JSON Path building via the builder (kills Null / Negate / dispatch mutators)
	// ---

	@ParameterizedTest
	@MethodSource("provideBuilderJsonPaths")
	void shouldBuildExpectedJsonPath(JsonVerifiable verifiable, String expectedJsonPath) {
		assertThat(verifiable).isNotNull();
		assertThat(verifiable.jsonPath()).isEqualTo(expectedJsonPath);
	}

	static Stream<Arguments> provideBuilderJsonPaths() {
		return Stream.of(
				// array(null) delegates to array()
				Arguments.of(JsonPath.builder().array((Object) null), "$[*]"),
				Arguments.of(JsonPath.builder().array("named"), "$.named[*]"),
				Arguments.of(JsonPath.builder().array(), "$[*]"),
				// contains adds a fake "[*]" section
				Arguments.of(JsonPath.builder().array("errors").contains("property"), "$.errors[*][*]"),
				// field with a single field and with the varargs overload
				Arguments.of(JsonPath.builder().field("a"), "$.a"),
				Arguments.of(JsonPath.builder().field("a", "b", "c"), "$.a.b.c"),
				Arguments.of(JsonPath.builder().elementWithIndex(2), "$[2]"),
				// isEqualTo(String) with null delegates to isNull()
				Arguments.of(JsonPath.builder().field("a").isEqualTo((String) null), "$[?(@.a == null)]"),
				// isEqualTo(Object) dispatch table
				Arguments.of(JsonPath.builder().field("a").isEqualTo((Object) null), "$[?(@.a == null)]"),
				Arguments.of(JsonPath.builder().field("a").isEqualTo((Object) Long.valueOf(5)), "$[?(@.a == 5)]"),
				Arguments.of(JsonPath.builder().field("a").isEqualTo((Object) Boolean.TRUE), "$[?(@.a == true)]"),
				Arguments.of(JsonPath.builder().field("a").isEqualTo((Object) Pattern.compile("[0-9]+")),
						"$[?(@.a =~ /[0-9]+/)]"),
				Arguments.of(JsonPath.builder().field("a").isEqualTo((Object) "x"), "$[?(@.a == 'x')]"),
				// isEqualTo(Number) with null delegates to isNull()
				Arguments.of(JsonPath.builder().field("a").isEqualTo((Number) null), "$[?(@.a == null)]"),
				// isEqualTo(Number) uses numberValue (BigDecimal.toPlainString)
				Arguments.of(JsonPath.builder().field("a").isEqualTo(new BigDecimal("1.50")), "$[?(@.a == 1.50)]"),
				Arguments.of(JsonPath.builder().field("a").isEqualTo(1.12E-12), "$[?(@.a == 0.00000000000112)]"),
				// isEqualTo(Boolean) with null delegates to isNull()
				Arguments.of(JsonPath.builder().field("a").isEqualTo((Boolean) null), "$[?(@.a == null)]"),
				Arguments.of(JsonPath.builder().field("a").isEqualTo(Boolean.FALSE), "$[?(@.a == false)]"),
				// matches(null) delegates to isNull()
				Arguments.of(JsonPath.builder().field("a").matches(null), "$[?(@.a == null)]"),
				Arguments.of(JsonPath.builder().field("a").matches("[a-z]+"), "$[?(@.a =~ /[a-z]+/)]"),
				// isNull() directly
				Arguments.of(JsonPath.builder().field("a").isNull(), "$[?(@.a == null)]"),
				// arrayField true branch (previous section ends with [*] and is not
				// exactly "[*]")
				Arguments.of(JsonPath.builder().array("x").arrayField(), "$.x"),
				// arrayField false branch (previous section is exactly "[*]")
				Arguments.of(JsonPath.builder().array().arrayField(), "$[*]"),
				// array value assertions
				Arguments.of(JsonPath.builder().array("flags").arrayField().isEqualTo(true), "$.flags[?(@ == true)]"),
				Arguments.of(JsonPath.builder().array("names").arrayField().isEqualTo("n1"), "$.names[?(@ == 'n1')]"),
				Arguments.of(JsonPath.builder().array("nums").arrayField().isEqualTo(7), "$.nums[?(@ == 7)]"),
				Arguments.of(JsonPath.builder().array("nums").arrayField().matches("[0-9]+"),
						"$.nums[?(@ =~ /[0-9]+/)]"),
				Arguments.of(JsonPath.builder().array("nums").arrayField().contains(7), "$.nums[?(@ == 7)]"));
	}

	@Test
	void fieldVarargsShouldThrowWhenNoFieldsProvided() {
		assertThatThrownBy(() -> JsonPath.builder().field(new String[0])).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("At least one field name must be provided");
	}

	// --- IteratingOverArray boolean flags on each concrete subtype ---

	@Test
	void iteratingFlagsShouldReflectConcreteAsserterType() {
		// plain FieldAssertion -> all false
		JsonVerifiable field = JsonPath.builder().field("a");
		assertThat(field.isIteratingOverArray()).isFalse();
		assertThat(field.isIteratingOverNamelessArray()).isFalse();
		assertThat(field.isAssertingAValueInArray()).isFalse();

		// ArrayAssertion -> isIteratingOverArray true, others false
		JsonVerifiable array = JsonPath.builder().array("a");
		assertThat(array.isIteratingOverArray()).isTrue();
		assertThat(array.isIteratingOverNamelessArray()).isFalse();
		assertThat(array.isAssertingAValueInArray()).isFalse();

		// ArrayValueAssertion -> isAssertingAValueInArray true, others false
		JsonVerifiable arrayValue = JsonPath.builder().array("a").arrayField();
		assertThat(arrayValue.isAssertingAValueInArray()).isTrue();
		assertThat(arrayValue.isIteratingOverArray()).isFalse();
		assertThat(arrayValue.isIteratingOverNamelessArray()).isFalse();

		// NamelessArrayHavingFieldAssertion (the root) -> isIteratingOverNamelessArray
		// true
		JsonVerifiable root = JsonPath.builder();
		assertThat(root.isIteratingOverNamelessArray()).isTrue();
		assertThat(root.isIteratingOverArray()).isFalse();
		assertThat(root.isAssertingAValueInArray()).isFalse();
	}

	// --- equals / hashCode / toString (constructed directly to isolate the branches) ---

	@Test
	void equalsAndHashCodeShouldConsiderBufferAndFieldName() {
		DocumentContext ctx = parse("{}");
		JsonAsserterConfiguration cfg = new JsonAsserterConfiguration();
		LinkedList<String> buf = new LinkedList<>();
		buf.offer("$");
		LinkedList<String> otherBuf = new LinkedList<>();
		otherBuf.offer("$");
		otherBuf.offer(".a");

		ReadyToCheckAsserter base = new ReadyToCheckAsserter(ctx, buf, "x", cfg);
		ReadyToCheckAsserter sameAsBase = new ReadyToCheckAsserter(ctx, buf, "x", cfg);
		ReadyToCheckAsserter differentField = new ReadyToCheckAsserter(ctx, buf, "y", cfg);
		ReadyToCheckAsserter differentBuffer = new ReadyToCheckAsserter(ctx, otherBuf, "x", cfg);
		ReadyToCheckAsserter nullField = new ReadyToCheckAsserter(ctx, buf, null, cfg);
		ReadyToCheckAsserter nullFieldToo = new ReadyToCheckAsserter(ctx, buf, null, cfg);

		// reflexive
		assertThat(base.equals(base)).isTrue();
		// null and different type
		assertThat(base.equals(null)).isFalse();
		assertThat(base.equals("not an asserter")).isFalse();
		// equal buffer + equal field name
		assertThat(base).isEqualTo(sameAsBase);
		assertThat(base.hashCode()).isEqualTo(sameAsBase.hashCode());
		// equal buffer, different field name (reaches the field-name comparison)
		assertThat(base).isNotEqualTo(differentField);
		// different buffer short-circuits before the field-name comparison
		assertThat(base).isNotEqualTo(differentBuffer);
		// null field name on either side
		assertThat(base).isNotEqualTo(nullField);
		assertThat(nullField).isNotEqualTo(base);
		assertThat(nullField).isEqualTo(nullFieldToo);

		// exact hashCode formula kills the Math and null-ternary mutants
		int expectedBufferHash = new LinkedList<>(buf).hashCode();
		assertThat(base.hashCode()).isEqualTo(31 * expectedBufferHash + "x".hashCode());
		assertThat(nullField.hashCode()).isEqualTo(31 * expectedBufferHash);
	}

	@Test
	void toStringShouldContainTheJsonPathBuffer() {
		String rendered = JsonPath.builder().field("a").toString();
		assertThat(rendered).contains("Asserter{").contains("jsonPathBuffer=").contains("$").contains(".a");
	}

	// --- hasSize: normal return, size-mismatch and ignore-exception branches ---

	@Test
	void hasSizeShouldReturnSameAsserterWhenSizeMatches() {
		JsonVerifiable verifiable = JsonAssertion.assertThat("{\"list\":[1,2]}").array("list");
		assertThat(verifiable.hasSize(2)).isSameAs(verifiable);
	}

	@Test
	void hasSizeShouldReturnSameAsserterAndSkipCheckWhenExceptionsIgnored() {
		JsonVerifiable verifiable = JsonAssertion.assertThat("{\"list\":[1,2]}")
			.withoutThrowingException()
			.array("list");
		// wrong size, but exceptions are ignored, so it returns "this" without throwing
		assertThat(verifiable.hasSize(999)).isSameAs(verifiable);
	}

	@Test
	void hasSizeShouldThrowWhenArrayMissing() {
		assertThatThrownBy(() -> JsonAssertion.assertThat("{}").array("missing").hasSize(1))
			.isInstanceOf(RuntimeException.class);
	}

	// --- isEmpty: normal return and ignore-exception branch ---

	@Test
	void isEmptyShouldReturnSameAsserterWhenEmpty() {
		JsonVerifiable verifiable = JsonAssertion.assertThat("{\"c\":[]}").array("c");
		assertThat(verifiable.isEmpty()).isSameAs(verifiable);
	}

	@Test
	void isEmptyShouldReturnSameAsserterAndSkipCheckWhenExceptionsIgnored() {
		JsonVerifiable verifiable = JsonAssertion.assertThat("{\"foo\":[1]}").withoutThrowingException().array("foo");
		assertThat(verifiable.isEmpty()).isSameAs(verifiable);
	}

	// --- check(): ignore-exception branch reached through matchesJsonPath ---

	@Test
	void matchesJsonPathShouldNotThrowWhenExceptionsIgnored() {
		assertThatNoException().isThrownBy(
				() -> JsonAssertion.assertThat("{}").withoutThrowingException().matchesJsonPath("$[?(@.x == 'y')]"));
	}

	// --- isInstanceOf returns the ready-to-check asserter with the current path ---

	@Test
	void isInstanceOfShouldReturnAsserterWithCurrentPath() {
		JsonVerifiable verifiable = JsonAssertion.assertThat("{\"foo\":46}").field("foo").isInstanceOf(Number.class);
		assertThat(verifiable).isNotNull();
		assertThat(verifiable.jsonPath()).isEqualTo("$.foo");
	}

	@Test
	void isInstanceOfShouldThrowForWrongType() {
		assertThatThrownBy(() -> JsonAssertion.assertThat("{\"foo\":46}").field("foo").isInstanceOf(String.class))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("is not assignable from [String]");
	}

	// --- value() returns a ready-to-check asserter ---

	@Test
	void valueShouldReturnAsserterForMatchingArray() {
		JsonVerifiable verifiable = JsonAssertion.assertThat("{\"list\":[\"a\",\"b\"]}")
			.array("list")
			.arrayField()
			.value();
		assertThat(verifiable).isNotNull();
		assertThat(verifiable.jsonPath()).isEqualTo("$.list");
	}

	// --- array value assertions verified against a real document ---

	@Test
	void arrayValueAssertionsShouldMatchRealDocument() {
		String json = "{\"flags\":[true,false], \"names\":[\"n1\",\"n2\"], \"nums\":[7,8]}";
		assertThatNoException()
			.isThrownBy(() -> JsonAssertion.assertThat(json).array("flags").arrayField().isEqualTo(true));
		assertThatNoException()
			.isThrownBy(() -> JsonAssertion.assertThat(json).array("names").arrayField().isEqualTo("n1"));
		assertThatNoException()
			.isThrownBy(() -> JsonAssertion.assertThat(json).array("nums").arrayField().isEqualTo(7));
		assertThatNoException().isThrownBy(() -> JsonAssertion.assertThat(json).array("nums").arrayField().contains(8));
		assertThatNoException()
			.isThrownBy(() -> JsonAssertion.assertThat(json).array("nums").arrayField().matches("[0-9]+"));
	}

	// --- read(): single element unwrapped vs whole array returned ---

	@Test
	void readShouldUnwrapSingleValueAndReturnArrayForMany() {
		String json = "{\"one\":[\"only\"], \"many\":[\"a\",\"b\"]}";
		assertThat(JsonAssertion.assertThat(json).field("one").read(Object.class)).isEqualTo("only");
		assertThat(JsonAssertion.assertThat(json).field("many").read(java.util.List.class)).containsExactly("a", "b");
	}

	// --- JsonPathAssert: success + failure for every wrapper method ---

	@Test
	void jsonPathAssertBuilderMethodsShouldReturnNonNull() {
		DocumentContext ctx = parse("[{\"property1\":\"a\"}]");
		assertThat(JsonAssertions.assertThat(ctx).array()).isNotNull();
		assertThat(JsonAssertions.assertThat(ctx).array().contains("property1")).isNotNull();
		DocumentContext nested = parse("{\"a\":{\"b\":\"c\"}}");
		assertThat(JsonAssertions.assertThat(nested).field("a")).isNotNull();
		assertThat(JsonAssertions.assertThat(nested).field("a", "b")).isNotNull();
		DocumentContext named = parse("{\"list\":[\"x\"]}");
		assertThat(JsonAssertions.assertThat(named).array("list")).isNotNull();
		assertThat(JsonAssertions.assertThat(named).array("list").arrayField()).isNotNull();
	}

	@Test
	void jsonPathAssertIsEqualToStringSuccessAndFailure() {
		DocumentContext ctx = parse("{\"a\":\"b\"}");
		assertThat(JsonAssertions.assertThat(ctx).field("a").isEqualTo("b")).isNotNull();
		assertThatThrownBy(() -> JsonAssertions.assertThat(ctx).field("a").isEqualTo("nope"))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("Expected JSON to match JSON Path");
	}

	@Test
	void jsonPathAssertIsEqualToNumberSuccessAndFailure() {
		DocumentContext ctx = parse("{\"n\":4}");
		assertThat(JsonAssertions.assertThat(ctx).field("n").isEqualTo(4)).isNotNull();
		assertThatThrownBy(() -> JsonAssertions.assertThat(ctx).field("n").isEqualTo(5))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("Expected JSON to match JSON Path");
	}

	@Test
	void jsonPathAssertIsEqualToBooleanSuccessAndFailure() {
		DocumentContext ctx = parse("{\"b\":true}");
		assertThat(JsonAssertions.assertThat(ctx).field("b").isEqualTo(true)).isNotNull();
		assertThatThrownBy(() -> JsonAssertions.assertThat(ctx).field("b").isEqualTo(false))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("Expected JSON to match JSON Path");
	}

	@Test
	void jsonPathAssertMatchesSuccessAndFailure() {
		DocumentContext ctx = parse("{\"s\":\"abc\"}");
		assertThat(JsonAssertions.assertThat(ctx).field("s").matches("[a-z]+")).isNotNull();
		assertThatThrownBy(() -> JsonAssertions.assertThat(ctx).field("s").matches("[0-9]+"))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("Expected JSON to match JSON Path");
	}

	@Test
	void jsonPathAssertIsInstanceOfSuccessAndFailure() {
		DocumentContext ctx = parse("{\"foo\":46}");
		assertThat(JsonAssertions.assertThat(ctx).field("foo").isInstanceOf(Number.class)).isNotNull();
		assertThatThrownBy(() -> JsonAssertions.assertThat(ctx).field("foo").isInstanceOf(String.class))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("is not assignable from [String]");
	}

	@Test
	void jsonPathAssertValueSuccessAndFailure() {
		DocumentContext present = parse("{\"list\":[\"a\",\"b\"]}");
		assertThat(JsonAssertions.assertThat(present).array("list").arrayField().value()).isNotNull();
		DocumentContext empty = parse("{\"empty\":[]}");
		assertThatThrownBy(() -> JsonAssertions.assertThat(empty).array("empty").arrayField().value())
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("Expected JSON to match JSON Path");
	}

	@Test
	void jsonPathAssertIsNullSuccessAndFailure() {
		DocumentContext nullValue = parse("{\"a\":null}");
		assertThatNoException().isThrownBy(() -> JsonAssertions.assertThat(nullValue).field("a").isNull());
		DocumentContext present = parse("{\"a\":\"b\"}");
		assertThatThrownBy(() -> JsonAssertions.assertThat(present).field("a").isNull())
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("Expected JSON to match JSON Path");
	}

	@Test
	void jsonPathAssertMatchesJsonPathSuccessAndFailure() {
		DocumentContext ctx = parse("{\"a\":\"b\"}");
		assertThat(JsonAssertions.assertThat(ctx).matchesJsonPath("$[?(@.a == 'b')]")).isNotNull();
		assertThatThrownBy(() -> JsonAssertions.assertThat(ctx).matchesJsonPath("$[?(@.a == 'c')]"))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("Expected JSON to match JSON Path");
	}

	@Test
	void jsonPathAssertIsEmptySuccessAndFailure() {
		DocumentContext ctx = parse("{\"coordinates\":[], \"foo\":[\"bar\"]}");
		assertThat(JsonAssertions.assertThat(ctx).array("coordinates").isEmpty()).isNotNull();
		assertThatThrownBy(() -> JsonAssertions.assertThat(ctx).array("foo").isEmpty())
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("to be empty");
	}

	// --- JsonVerifiable-based entry points on the assertion factories ---

	@Test
	void jsonAssertionsAndBddShouldAcceptJsonVerifiable() {
		JsonVerifiable verifiable = JsonAssertion.assertThat("{\"a\":\"b\"}");
		assertThat(JsonAssertions.assertThat(verifiable)).isNotNull();
		assertThat(BDDJsonAssertions.then(verifiable)).isNotNull();
		assertThatNoException().isThrownBy(() -> JsonAssertions.assertThat(verifiable).field("a").isEqualTo("b"));
		assertThatNoException().isThrownBy(() -> BDDJsonAssertions.then(verifiable).field("a").isEqualTo("b"));
	}

	@Test
	void jsonAssertionsAndBddShouldAcceptDocumentContext() {
		DocumentContext ctx = parse("{\"a\":\"b\"}");
		assertThat(JsonAssertions.assertThat(ctx)).isNotNull();
		assertThat(BDDJsonAssertions.then(ctx)).isNotNull();
		assertThatNoException().isThrownBy(() -> BDDJsonAssertions.then(ctx).field("a").isEqualTo("b"));
	}

}
