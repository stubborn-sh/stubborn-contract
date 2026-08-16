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

package sh.stubborn.contract.verifier.builder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.BodyMatcher;
import sh.stubborn.contract.spec.internal.MatchingType;
import sh.stubborn.contract.verifier.template.HandlebarsTemplateProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Mutation-focused coverage for the private helper logic of
 * {@link JsonBodyVerificationBuilder} (property-path resolution, tokenizer, value
 * rendering and size-check helpers). These are pure functions reached by reflection so
 * the exact returned value / thrown type can be pinned, killing NegateConditionals,
 * ConditionalsBoundary, Math, EmptyReturn and NullReturn mutants.
 */
class JsonBodyVerificationBuilderMutationTests {

	private final JsonBodyVerificationBuilder builder = newBuilder();

	private static JsonBodyVerificationBuilder newBuilder() {
		HandlebarsTemplateProcessor tp = new HandlebarsTemplateProcessor();
		return new JsonBodyVerificationBuilder(false, tp, tp, new Contract(), Optional.empty(), Function.identity());
	}

	private Object invoke(String name, Class<?>[] types, Object... args) throws Exception {
		Method m = JsonBodyVerificationBuilder.class.getDeclaredMethod(name, types);
		m.setAccessible(true);
		return m.invoke(this.builder, args);
	}

	// --- objectToString: each type suffix must survive (EmptyReturn on the Double arm)
	// ---

	@Test
	void objectToString_long_appends_L() throws Exception {
		assertThat(invoke("objectToString", new Class<?>[] { Object.class }, 5L)).isEqualTo("5L");
	}

	@Test
	void objectToString_double_appends_D() throws Exception {
		assertThat(invoke("objectToString", new Class<?>[] { Object.class }, 1.5D)).isEqualTo("1.5D");
	}

	@Test
	void objectToString_other_is_plain_string() throws Exception {
		assertThat(invoke("objectToString", new Class<?>[] { Object.class }, 7)).isEqualTo("7");
	}

	// --- unquote: single/double quotes stripped, otherwise verbatim ---

	@Test
	void unquote_strips_single_quotes() throws Exception {
		assertThat(invoke("unquote", new Class<?>[] { String.class }, "'abc'")).isEqualTo("abc");
	}

	@Test
	void unquote_strips_double_quotes() throws Exception {
		assertThat(invoke("unquote", new Class<?>[] { String.class }, "\"abc\"")).isEqualTo("abc");
	}

	@Test
	void unquote_leaves_unquoted_verbatim() throws Exception {
		assertThat(invoke("unquote", new Class<?>[] { String.class }, "abc")).isEqualTo("abc");
	}

	// --- resolveNextToken: List branch (L348 boundary) ---

	@Test
	void resolveNextToken_list_returns_element_at_index() throws Exception {
		Object result = invoke("resolveNextToken", new Class<?>[] { Object.class, String.class },
				List.of("a", "b", "c"), "1");
		assertThat(result).isEqualTo("b");
	}

	@Test
	void resolveNextToken_list_index_equal_to_size_throws_illegal_state() {
		assertThatThrownBy(
				() -> invoke("resolveNextToken", new Class<?>[] { Object.class, String.class }, List.of("a"), "1"))
			.isInstanceOf(InvocationTargetException.class)
			.cause()
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("out of bounds");
	}

	@Test
	void resolveNextToken_list_negative_index_throws_illegal_state() {
		assertThatThrownBy(
				() -> invoke("resolveNextToken", new Class<?>[] { Object.class, String.class }, List.of("a"), "-1"))
			.isInstanceOf(InvocationTargetException.class)
			.cause()
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("out of bounds");
	}

	// --- resolveNextToken: array branch (L356 boundary/negate, L359 null return) ---

	@Test
	void resolveNextToken_array_returns_element_at_index() throws Exception {
		Object result = invoke("resolveNextToken", new Class<?>[] { Object.class, String.class },
				new String[] { "x", "y" }, "0");
		assertThat(result).isEqualTo("x");
	}

	@Test
	void resolveNextToken_array_index_equal_to_length_throws_illegal_state() {
		assertThatThrownBy(
				() -> invoke("resolveNextToken", new Class<?>[] { Object.class, String.class }, new int[] { 1 }, "1"))
			.isInstanceOf(InvocationTargetException.class)
			.cause()
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("out of bounds");
	}

	@Test
	void resolveNextToken_array_negative_index_throws_illegal_state() {
		assertThatThrownBy(
				() -> invoke("resolveNextToken", new Class<?>[] { Object.class, String.class }, new int[] { 1 }, "-1"))
			.isInstanceOf(InvocationTargetException.class)
			.cause()
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("out of bounds");
	}

	@Test
	void resolveNextToken_null_current_throws_illegal_state() {
		assertThatThrownBy(() -> invoke("resolveNextToken", new Class<?>[] { Object.class, String.class }, null, "0"))
			.isInstanceOf(InvocationTargetException.class)
			.cause()
			.isInstanceOf(IllegalStateException.class);
	}

	// --- addTokens: tokenizer, including the unclosed-bracket remainder (L412 math,
	// L413) ---

	@SuppressWarnings("unchecked")
	private List<String> tokens(String segment) throws Exception {
		List<String> out = new ArrayList<>();
		invoke("addTokens", new Class<?>[] { String.class, List.class }, segment, out);
		return out;
	}

	@Test
	void addTokens_plain_segment() throws Exception {
		assertThat(tokens("foo")).containsExactly("foo");
	}

	@Test
	void addTokens_bracketed_indexes() throws Exception {
		assertThat(tokens("a[0]b[1]")).containsExactly("a", "0", "b", "1");
	}

	@Test
	void addTokens_leading_bracket_only() throws Exception {
		assertThat(tokens("[0]")).containsExactly("0");
	}

	@Test
	void addTokens_unclosed_bracket_takes_remainder_after_bracket() throws Exception {
		// bracketStart+1 (L412) must skip the '['; a Math mutant would emit "o[bar".
		assertThat(tokens("foo[bar")).containsExactly("foo", "bar");
	}

	// --- minus: substring removal of first occurrence ---

	private String minus(String self, String target) throws Exception {
		return (String) invoke("minus", new Class<?>[] { CharSequence.class, Object.class }, self, target);
	}

	@Test
	void minus_removes_trailing_occurrence() throws Exception {
		assertThat(minus("hello world", "world")).isEqualTo("hello ");
	}

	@Test
	void minus_removes_leading_occurrence_and_keeps_tail() throws Exception {
		assertThat(minus("hello world", "hello ")).isEqualTo("world");
	}

	@Test
	void minus_removes_middle_occurrence() throws Exception {
		assertThat(minus("aXbXc", "X")).isEqualTo("abXc");
	}

	@Test
	void minus_missing_target_returns_unchanged() throws Exception {
		assertThat(minus("abc", "z")).isEqualTo("abc");
	}

	// --- mapOrListBodyReducedToEmpty: both the Map (L136) and List (L141) reductions ---

	private boolean reduced(Object original, Object converted) throws Exception {
		return (boolean) invoke("mapOrListBodyReducedToEmpty", new Class<?>[] { Object.class, Object.class }, original,
				converted);
	}

	@Test
	void reducedToEmpty_true_when_map_emptied() throws Exception {
		Map<String, Object> orig = new LinkedHashMap<>();
		orig.put("a", 1);
		assertThat(reduced(orig, new LinkedHashMap<>())).isTrue();
	}

	@Test
	void reducedToEmpty_true_when_list_emptied() throws Exception {
		assertThat(reduced(List.of("a"), new ArrayList<>())).isTrue();
	}

	@Test
	void reducedToEmpty_false_when_list_kept() throws Exception {
		assertThat(reduced(List.of("a"), List.of("a"))).isFalse();
	}

	@Test
	void reducedToEmpty_false_when_not_map_or_list() throws Exception {
		assertThat(reduced("a", "b")).isFalse();
	}

	// --- sizeCheckMethod: no min/max occurrence returns the bare prefix (L167
	// EmptyReturn) ---

	private BodyMatcher matcher(String path, Integer min, Integer max) {
		return new BodyMatcher() {
			@Override
			public MatchingType matchingType() {
				return MatchingType.TYPE;
			}

			@Override
			public String path() {
				return path;
			}

			@Override
			public Object value() {
				return null;
			}

			@Override
			public Integer minTypeOccurrence() {
				return min;
			}

			@Override
			public Integer maxTypeOccurrence() {
				return max;
			}
		};
	}

	@Test
	void sizeCheckMethod_without_occurrences_returns_bare_prefix() throws Exception {
		Object result = invoke("sizeCheckMethod", new Class<?>[] { BodyMatcher.class, String.class },
				matcher("$.foo", null, null), "\"$.foo\"");
		assertThat(result).isEqualTo("as(\"$.foo\").hasSize");
	}

	@Test
	void sizeCheckMethod_with_min_only() throws Exception {
		Object result = invoke("sizeCheckMethod", new Class<?>[] { BodyMatcher.class, String.class },
				matcher("$.foo", 2, null), "\"$.foo\"");
		assertThat(result).isEqualTo("as(\"$.foo\").hasSizeGreaterThanOrEqualTo(2)");
	}

	@Test
	void sizeCheckMethod_with_max_only() throws Exception {
		Object result = invoke("sizeCheckMethod", new Class<?>[] { BodyMatcher.class, String.class },
				matcher("$.foo", null, 5), "\"$.foo\"");
		assertThat(result).isEqualTo("as(\"$.foo\").hasSizeLessThanOrEqualTo(5)");
	}

	@Test
	void sizeCheckMethod_with_min_and_max() throws Exception {
		Object result = invoke("sizeCheckMethod", new Class<?>[] { BodyMatcher.class, String.class },
				matcher("$.foo", 2, 5), "\"$.foo\"");
		assertThat(result).isEqualTo("as(\"$.foo\").hasSizeBetween(2, 5)");
	}

}
