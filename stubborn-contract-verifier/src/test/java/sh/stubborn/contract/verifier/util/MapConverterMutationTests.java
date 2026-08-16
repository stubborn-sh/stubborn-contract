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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sh.stubborn.contract.spec.internal.DslProperty;
import sh.stubborn.contract.spec.internal.FromFileProperty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Mutation-focused tests for {@link MapConverter}.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
class MapConverterMutationTests {

	@Test
	void transformToClientValues_resolves_dsl_property_in_map() {
		Map<String, Object> input = Map.of("a", new DslProperty("cli", "srv"));
		Object out = MapConverter.transformToClientValues(input);
		assertThat((Map) out).containsEntry("a", "cli");
	}

	@Test
	void getStubSideValues_returns_client_values() {
		Map<String, Object> input = Map.of("a", new DslProperty("cli", "srv"));
		assertThat((Map) MapConverter.getStubSideValues(input)).containsEntry("a", "cli");
	}

	@Test
	void getTestSideValues_returns_server_values() {
		Map<String, Object> input = Map.of("a", new DslProperty("cli", "srv"));
		assertThat((Map) MapConverter.getTestSideValues(input)).containsEntry("a", "srv");
	}

	@Test
	void transformValues_on_collection_maps_each_element() {
		List<Object> input = List.of(new DslProperty("c1", "s1"), new DslProperty("c2", "s2"));
		Object out = MapConverter.transformToClientValues(input);
		assertThat((List) out).containsExactly("c1", "c2");
	}

	@Test
	void transformValues_parses_json_string_into_map() {
		Object out = MapConverter.getStubSideValues("{\"a\":\"b\"}");
		assertThat((Map) out).containsEntry("a", "b");
	}

	@Test
	void transformValues_parses_json_array_string_into_list() {
		Object out = MapConverter.getStubSideValues("[1,2,3]");
		assertThat((List) out).containsExactly(1, 2, 3);
	}

	@Test
	void transformValues_non_json_string_passthrough() {
		Object out = MapConverter.getStubSideValues("plain text");
		assertThat(out).isEqualTo("plain text");
	}

	@Test
	void json_parsing_function_parses() {
		assertThat(MapConverter.JSON_PARSING_FUNCTION.apply("{\"a\":1}")).isInstanceOf(Map.class);
	}

	@Test
	void json_parsing_function_throws_on_bad_json() {
		assertThatThrownBy(() -> MapConverter.JSON_PARSING_FUNCTION.apply("not json <<"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void identity_function_returns_input() {
		assertThat(MapConverter.IDENTITY.apply("value")).isEqualTo("value");
	}

	@Test
	void getStubSideValues_fromFileProperty_string(@TempDir Path dir) throws Exception {
		File file = dir.resolve("data.txt").toFile();
		Files.writeString(file.toPath(), "file-content");
		FromFileProperty prop = new FromFileProperty(file, String.class);
		assertThat(MapConverter.getStubSideValues(prop)).isEqualTo("file-content");
	}

	@Test
	void getStubSideValues_fromFileProperty_bytes(@TempDir Path dir) throws Exception {
		File file = dir.resolve("data.dat").toFile();
		Files.write(file.toPath(), new byte[] { 1, 2, 3 });
		FromFileProperty prop = new FromFileProperty(file, byte[].class);
		assertThat((byte[]) MapConverter.getStubSideValues(prop)).containsExactly(1, 2, 3);
	}

	@Test
	void getTestSideValuesForText_uses_identity_parsing() {
		Map<String, Object> input = Map.of("a", new DslProperty("cli", "srv"));
		assertThat((Map) MapConverter.getTestSideValuesForText(input)).containsEntry("a", "srv");
	}

	@Test
	void getStubSideValuesForNonBody_and_getTestSideValuesForNonBody() {
		Map<String, Object> input = Map.of("a", new DslProperty("cli", "srv"));
		assertThat((Map) MapConverter.getStubSideValuesForNonBody(input)).containsEntry("a", "cli");
		assertThat((Map) MapConverter.getTestSideValuesForNonBody(input)).containsEntry("a", "srv");
	}

	@Test
	void two_arg_getClientOrServerSideValues_client_and_server() {
		Map<String, Object> input = Map.of("a", new DslProperty("cli", "srv"));
		assertThat((Map) MapConverter.getClientOrServerSideValues(input, true)).containsEntry("a", "cli");
		assertThat((Map) MapConverter.getClientOrServerSideValues(input, false)).containsEntry("a", "srv");
	}

	@Test
	void dynamic_string_value_is_resolved() {
		sh.stubborn.contract.spec.internal.DynamicStringImpl ds = new sh.stubborn.contract.spec.internal.DynamicStringImpl(
				new Object[] { new DslProperty("cli", "srv") }, new String[] { "x=", "" });
		Object out = MapConverter.getClientOrServerSideValues(ds, false);
		assertThat(out.toString()).isEqualTo("x=srv");
	}

	@Test
	void nested_map_is_converted_recursively() {
		Map<String, Object> nested = Map.of("inner", new DslProperty("cli", "srv"));
		Map<String, Object> input = Map.of("outer", nested);
		Object out = MapConverter.transformToClientValues(input);
		assertThat((Map) ((Map) out).get("outer")).containsEntry("inner", "cli");
	}

}
