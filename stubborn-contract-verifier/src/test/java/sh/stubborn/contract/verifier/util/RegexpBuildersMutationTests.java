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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.internal.DslProperty;
import sh.stubborn.contract.spec.internal.DynamicStringImpl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link RegexpBuilders}.
 */
class RegexpBuildersMutationTests {

	private static final String WS = "/\\s*/";

	@Test
	void stubSide_object_escapes_plain_string() {
		assertThat(RegexpBuilders.buildGStringRegexpForStubSide((Object) "abc")).isEqualTo("abc");
	}

	@Test
	void stubSide_pattern_returns_pattern() {
		assertThat(RegexpBuilders.buildGStringRegexpForStubSide((Object) Pattern.compile("[0-9]+")))
			.isEqualTo("[0-9]+");
	}

	@Test
	void stubSide_dslProperty_uses_client_value() {
		assertThat(RegexpBuilders.buildGStringRegexpForStubSide((Object) new DslProperty("cli", "srv")))
			.isEqualTo("cli");
	}

	@Test
	void stubSide_dynamicString_combines_values_and_strings() {
		DynamicStringImpl ds = new DynamicStringImpl(new Object[] { Pattern.compile("[0-9]+") },
				new String[] { "a", "b" });
		assertThat(RegexpBuilders.buildGStringRegexpForStubSide((Object) ds)).isEqualTo("a[0-9]+b");
	}

	@Test
	void testSide_object_doubles_backslashes() {
		assertThat(RegexpBuilders.buildGStringRegexpForTestSide((Object) "a\\b")).isEqualTo("a\\\\b");
	}

	@Test
	void testSide_dynamicString() {
		DynamicStringImpl ds = new DynamicStringImpl(new Object[] { "x" }, new String[] { "a", "b" });
		assertThat(RegexpBuilders.buildGStringRegexpForTestSide(ds)).isEqualTo("axb");
	}

	@Test
	void escapeSpecialRegexChars_plain_passthrough() {
		assertThat(RegexpBuilders.escapeSpecialRegexChars("abc")).isEqualTo("abc");
	}

	@Test
	void escapeSpecialRegexChars_escapes_dot() {
		assertThat(RegexpBuilders.escapeSpecialRegexChars("a.b")).contains("\\").contains("a").contains("b");
	}

	@Test
	void buildJSONRegexpMatchString() {
		assertThat(RegexpBuilders.buildJSONRegexpMatchString("a")).isEqualTo(WS + "\"a\"" + WS);
	}

	@Test
	void buildJSONRegexpMatchStringOptionalQuotes() {
		assertThat(RegexpBuilders.buildJSONRegexpMatchStringOptionalQuotes("b")).isEqualTo(WS + "\"?b\"?" + WS);
	}

	@Test
	void buildJSONRegexpMatch_object() {
		assertThat(RegexpBuilders.buildJSONRegexpMatch((Object) "b")).isEqualTo(WS + "\"?b\"?" + WS);
	}

	@Test
	void buildJSONRegexpMatch_pattern() {
		assertThat(RegexpBuilders.buildJSONRegexpMatch(Pattern.compile("[0-9]+"))).isEqualTo(WS + "\"?[0-9]+\"?" + WS);
	}

	@Test
	void buildJSONRegexpMatch_entry() {
		Map.Entry<String, Object> entry = Map.entry("a", "b");
		String expected = WS + "\"a\"" + WS + ":" + WS + "\"?b\"?" + WS;
		assertThat(RegexpBuilders.buildJSONRegexpMatch(entry)).isEqualTo(expected);
	}

	@Test
	void buildJSONRegexpMatch_map() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("a", "b");
		String entry = WS + "\"a\"" + WS + ":" + WS + "\"?b\"?" + WS;
		String expected = WS + "\\{" + entry + "\\}" + WS;
		assertThat(RegexpBuilders.buildJSONRegexpMatch(map)).isEqualTo(expected);
	}

	@Test
	void buildJSONRegexpMatch_list() {
		List<Object> list = List.of("b");
		String expected = WS + "\\[" + WS + "\"?b\"?" + WS + "\\]" + WS;
		assertThat(RegexpBuilders.buildJSONRegexpMatch(list)).isEqualTo(expected);
	}

	@Test
	void buildJSONRegexpMatch_dynamicString_json_map() {
		DynamicStringImpl ds = new DynamicStringImpl(new Object[0], new String[] { "{\"a\":\"b\"}" });
		String result = RegexpBuilders.buildJSONRegexpMatch(ds);
		assertThat(result).startsWith(WS + "\"?").endsWith("\"?" + WS).contains("a=b");
	}

}
