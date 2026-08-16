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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sh.stubborn.contract.spec.internal.DslProperty;
import sh.stubborn.contract.spec.internal.DynamicStringImpl;
import sh.stubborn.contract.spec.internal.ExecutionProperty;
import sh.stubborn.contract.spec.internal.FromFileProperty;
import sh.stubborn.contract.spec.internal.Headers;
import sh.stubborn.contract.spec.internal.MatchingStrategy;
import sh.stubborn.contract.spec.internal.NamedProperty;
import sh.stubborn.contract.spec.internal.OptionalProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link ContentUtils}.
 */
@SuppressWarnings({ "unchecked", "rawtypes", "NullAway" })
class ContentUtilsMutationTests {

	private static DynamicStringImpl ds(String literal) {
		return new DynamicStringImpl(new Object[0], new String[] { literal });
	}

	// --- extractValue(DynamicString, ContentType, Function) ---

	@Test
	void extractValue_returns_same_body_when_blank() {
		DynamicStringImpl blank = ds("   ");
		Object result = ContentUtils.extractValue(blank, ContentType.JSON, ContentUtils.GET_TEST_SIDE_FUNCTION);
		assertThat(result).isSameAs(blank);
	}

	@Test
	void extractValue_json_returns_parsed_map_with_server_side() {
		DynamicStringImpl body = new DynamicStringImpl(new Object[] { new DslProperty("cli", "srv") },
				new String[] { "{\"a\":\"", "\"}" });
		Object result = ContentUtils.extractValue(body, ContentType.JSON, ContentUtils.GET_TEST_SIDE_FUNCTION);
		assertThat(result).isInstanceOf(Map.class);
		assertThat((Map) result).containsEntry("a", "srv");
	}

	@Test
	void extractValue_json_returns_parsed_map_with_client_side() {
		DynamicStringImpl body = new DynamicStringImpl(new Object[] { new DslProperty("cli", "srv") },
				new String[] { "{\"a\":\"", "\"}" });
		Object result = ContentUtils.extractValue(body, ContentType.JSON, ContentUtils.GET_STUB_SIDE_FUNCTION);
		assertThat((Map) result).containsEntry("a", "cli");
	}

	@Test
	void extractValue_text_returns_substituted_string() {
		DynamicStringImpl body = new DynamicStringImpl(new Object[] { new DslProperty("cli", "srv") },
				new String[] { "hello ", "!" });
		Object result = ContentUtils.extractValue(body, ContentType.TEXT, ContentUtils.GET_TEST_SIDE_FUNCTION);
		assertThat(result).isEqualTo("hello srv!");
	}

	@Test
	void extractValue_form_returns_substituted_string() {
		DynamicStringImpl body = new DynamicStringImpl(new Object[] { new DslProperty("cli", "srv") },
				new String[] { "a=", "" });
		Object result = ContentUtils.extractValue(body, ContentType.FORM, ContentUtils.GET_TEST_SIDE_FUNCTION);
		assertThat(result).isEqualTo("a=srv");
	}

	@Test
	void extractValue_xml_returns_dynamic_string_impl() {
		DynamicStringImpl body = ds("<root><child>text</child></root>");
		Object result = ContentUtils.extractValue(body, ContentType.XML, ContentUtils.GET_TEST_SIDE_FUNCTION);
		assertThat(result).isInstanceOf(DynamicStringImpl.class);
		assertThat(result.toString()).isEqualTo("<root><child>text</child></root>");
	}

	@Test
	void extractValue_unknown_brute_force_json() {
		DynamicStringImpl body = ds("{\"k\":1}");
		Object result = ContentUtils.extractValue(body, ContentType.UNKNOWN, ContentUtils.GET_TEST_SIDE_FUNCTION);
		assertThat(result).isInstanceOf(Map.class);
		assertThat((Map) result).containsEntry("k", 1);
	}

	@Test
	void extractValue_unknown_brute_force_xml() {
		DynamicStringImpl body = ds("<a>b</a>");
		Object result = ContentUtils.extractValue(body, ContentType.UNKNOWN, ContentUtils.GET_TEST_SIDE_FUNCTION);
		assertThat(result).isInstanceOf(DynamicStringImpl.class);
		assertThat(result.toString()).isEqualTo("<a>b</a>");
	}

	@Test
	void extractValue_unknown_brute_force_falls_back_to_gstring() {
		DynamicStringImpl body = ds("just plain text not json not xml");
		Object result = ContentUtils.extractValue(body, ContentType.UNKNOWN, ContentUtils.GET_TEST_SIDE_FUNCTION);
		assertThat(result).isInstanceOf(DynamicStringImpl.class);
		assertThat(result.toString()).isEqualTo("just plain text not json not xml");
	}

	@Test
	void extractValue_two_arg_defaults_to_unknown() {
		DynamicStringImpl body = ds("{\"k\":1}");
		Object result = ContentUtils.extractValue(body, ContentUtils.GET_TEST_SIDE_FUNCTION);
		assertThat((Map) result).containsEntry("k", 1);
	}

	// --- getClientContentType ---

	@Test
	void getClientContentType_dynamicString_json() {
		assertThat(ContentUtils.getClientContentType(ds("{\"a\":1}"))).isEqualTo(ContentType.JSON);
	}

	@Test
	void getClientContentType_dynamicString_xml() {
		assertThat(ContentUtils.getClientContentType(ds("<a>1</a>"))).isEqualTo(ContentType.XML);
	}

	@Test
	void getClientContentType_dynamicString_unknown() {
		assertThat(ContentUtils.getClientContentType(ds("not json not xml <<"))).isEqualTo(ContentType.UNKNOWN);
	}

	@Test
	void getClientContentType_string_json_xml_unknown() {
		assertThat(ContentUtils.getClientContentType("{\"a\":1}")).isEqualTo(ContentType.JSON);
		assertThat(ContentUtils.getClientContentType("<a>1</a>")).isEqualTo(ContentType.XML);
		assertThat(ContentUtils.getClientContentType("plain <<< nonsense")).isEqualTo(ContentType.UNKNOWN);
	}

	@Test
	void getClientContentType_object_dispatch_string() {
		assertThat(ContentUtils.getClientContentType((Object) "{\"a\":1}")).isEqualTo(ContentType.JSON);
	}

	@Test
	void getClientContentType_object_dispatch_dynamicString() {
		assertThat(ContentUtils.getClientContentType((Object) ds("<a>1</a>"))).isEqualTo(ContentType.XML);
	}

	@Test
	void getClientContentType_object_dispatch_map() {
		assertThat(ContentUtils.getClientContentType((Object) Map.of("a", 1))).isEqualTo(ContentType.JSON);
	}

	@Test
	void getClientContentType_object_dispatch_list() {
		assertThat(ContentUtils.getClientContentType((Object) List.of("a", "b"))).isEqualTo(ContentType.JSON);
	}

	@Test
	void getClientContentType_object_dispatch_matchingStrategy_unknown() {
		MatchingStrategy strategy = new MatchingStrategy("x", MatchingStrategy.Type.EQUAL_TO);
		assertThat(ContentUtils.getClientContentType((Object) strategy)).isEqualTo(ContentType.UNKNOWN);
	}

	@Test
	void getClientContentType_object_dispatch_fromFileProperty_unknown(@TempDir Path dir) throws Exception {
		File file = dir.resolve("data.txt").toFile();
		Files.writeString(file.toPath(), "hi");
		FromFileProperty prop = new FromFileProperty(file, String.class);
		assertThat(ContentUtils.getClientContentType((Object) prop)).isEqualTo(ContentType.UNKNOWN);
	}

	@Test
	void getClientContentType_object_guess_pojo_json() {
		assertThat(ContentUtils.getClientContentType((Object) Integer.valueOf(5))).isEqualTo(ContentType.JSON);
	}

	@Test
	void getClientContentType_map_and_list() {
		assertThat(ContentUtils.getClientContentType(Map.of("a", 1))).isEqualTo(ContentType.JSON);
		assertThat(ContentUtils.getClientContentType(List.of(1, 2))).isEqualTo(ContentType.JSON);
	}

	@Test
	void getClientContentType_withHeaders_header_wins() {
		Headers headers = new Headers();
		headers.contentType("application/json");
		assertThat(ContentUtils.getClientContentType((Object) "plain nonsense <<", headers))
			.isEqualTo(ContentType.JSON);
	}

	@Test
	void getClientContentType_withHeaders_falls_back_to_body() {
		Headers headers = new Headers();
		assertThat(ContentUtils.getClientContentType((Object) "{\"a\":1}", headers)).isEqualTo(ContentType.JSON);
	}

	// --- extractValueForGString ---

	@Test
	void extractValueForGString_substitutes_and_handles_null() {
		DynamicStringImpl body = new DynamicStringImpl(new Object[] { new DslProperty("cli", "srv"), "static" },
				new String[] { "a", "b", "c" });
		DynamicStringImpl result = ContentUtils.extractValueForGString(body, ContentUtils.GET_TEST_SIDE_FUNCTION);
		assertThat(result.toString()).isEqualTo("asrvbstaticc");
	}

	@Test
	void extractValueForGString_null_result_becomes_null_string() {
		DynamicStringImpl body = new DynamicStringImpl(new Object[] { new DslProperty(null, null) },
				new String[] { "x", "y" });
		DynamicStringImpl result = ContentUtils.extractValueForGString(body, ContentUtils.GET_TEST_SIDE_FUNCTION);
		assertThat(result.toString()).isEqualTo("xnully");
	}

	// --- transformJSONStringValue overloads ---

	@Test
	void transformJSONStringValue_pattern() {
		Object out = ContentUtils.transformJSONStringValue(Pattern.compile("[0-9]+"),
				ContentUtils.GET_TEST_SIDE_FUNCTION);
		assertThat(out).isEqualTo("REGEXP>>[0-9]+<<");
	}

	@Test
	void transformJSONStringValue_optional() {
		Object out = ContentUtils.transformJSONStringValue(new OptionalProperty("abc"),
				ContentUtils.GET_TEST_SIDE_FUNCTION);
		assertThat(out).isEqualTo("OPTIONAL>>abc<<");
	}

	@Test
	void transformJSONStringValue_execution() {
		Object out = ContentUtils.transformJSONStringValue(new ExecutionProperty("foo($it)"),
				ContentUtils.GET_TEST_SIDE_FUNCTION);
		assertThat(out).isEqualTo("\"EXECUTION>>foo($it)<<\"");
	}

	@Test
	void transformJSONStringValue_dslProperty_resolves() {
		Object out = ContentUtils.transformJSONStringValue(new DslProperty("cli", "srv"),
				ContentUtils.GET_TEST_SIDE_FUNCTION);
		assertThat(out).isEqualTo("srv");
	}

	@Test
	void transformJSONStringValue_plain_object_passthrough() {
		Object out = ContentUtils.transformJSONStringValue("plain", ContentUtils.GET_TEST_SIDE_FUNCTION);
		assertThat(out).isEqualTo("plain");
	}

	// --- returnParsedObject ---

	@Test
	void returnParsedObject_regex() {
		Object out = ContentUtils.returnParsedObject("REGEXP>>[a-z]+<<");
		assertThat(out).isInstanceOf(Pattern.class);
		assertThat(((Pattern) out).pattern()).isEqualTo("[a-z]+");
	}

	@Test
	void returnParsedObject_execution() {
		Object out = ContentUtils.returnParsedObject("EXECUTION>>doStuff()<<");
		assertThat(out).isInstanceOf(ExecutionProperty.class);
		assertThat(((ExecutionProperty) out).getExecutionCommand()).isEqualTo("doStuff()");
	}

	@Test
	void returnParsedObject_optional() {
		Object out = ContentUtils.returnParsedObject("OPTIONAL>>abc<<");
		assertThat(out).isInstanceOf(Pattern.class);
		assertThat(((Pattern) out).pattern()).isEqualTo("(abc)?");
	}

	@Test
	void returnParsedObject_plain_string_passthrough() {
		assertThat(ContentUtils.returnParsedObject("just a string")).isEqualTo("just a string");
	}

	@Test
	void returnParsedObject_non_string_passthrough() {
		Integer in = 42;
		assertThat(ContentUtils.returnParsedObject(in)).isSameAs(in);
	}

	// --- convertDslPropsToTemporaryRegexPatterns ---

	@Test
	void convertDslPropsToTemporaryRegexPatterns_replaces_pattern() {
		Map<String, Object> input = Map.of("a", Pattern.compile("[0-9]+"));
		Object out = ContentUtils.convertDslPropsToTemporaryRegexPatterns(input);
		assertThat((Map) out).containsEntry("a", "REGEXP>>[0-9]+<<");
	}

	// --- recognizeContentTypeFromHeader ---

	private static Headers headerWith(String value) {
		Headers headers = new Headers();
		headers.header("Content-Type", value);
		return headers;
	}

	@Test
	void recognizeContentTypeFromHeader_json() {
		assertThat(ContentUtils.recognizeContentTypeFromHeader(headerWith("application/json")))
			.isEqualTo(ContentType.JSON);
	}

	@Test
	void recognizeContentTypeFromHeader_xml() {
		assertThat(ContentUtils.recognizeContentTypeFromHeader(headerWith("application/xml")))
			.isEqualTo(ContentType.XML);
	}

	@Test
	void recognizeContentTypeFromHeader_text() {
		assertThat(ContentUtils.recognizeContentTypeFromHeader(headerWith("text/plain"))).isEqualTo(ContentType.TEXT);
	}

	@Test
	void recognizeContentTypeFromHeader_form() {
		assertThat(ContentUtils.recognizeContentTypeFromHeader(headerWith("application/x-www-form-urlencoded")))
			.isEqualTo(ContentType.FORM);
	}

	@Test
	void recognizeContentTypeFromHeader_octet_stream_unknown() {
		assertThat(ContentUtils.recognizeContentTypeFromHeader(headerWith("application/octet-stream")))
			.isEqualTo(ContentType.UNKNOWN);
	}

	@Test
	void recognizeContentTypeFromHeader_defined_for_other_non_template() {
		assertThat(ContentUtils.recognizeContentTypeFromHeader(headerWith("application/vnd.custom")))
			.isEqualTo(ContentType.DEFINED);
	}

	@Test
	void recognizeContentTypeFromHeader_template_is_unknown() {
		assertThat(ContentUtils.recognizeContentTypeFromHeader(headerWith("{{{request.headers.foo}}}")))
			.isEqualTo(ContentType.UNKNOWN);
	}

	@Test
	void recognizeContentTypeFromHeader_null_headers_unknown() {
		assertThat(ContentUtils.recognizeContentTypeFromHeader((Headers) null)).isEqualTo(ContentType.UNKNOWN);
	}

	@Test
	void recognizeContentTypeFromHeader_empty_content_unknown() {
		assertThat(ContentUtils.recognizeContentTypeFromHeader(headerWith(""))).isEqualTo(ContentType.UNKNOWN);
	}

	@Test
	void recognizeContentTypeFromTestHeader_uses_server_value() {
		Headers headers = new Headers();
		headers.header("Content-Type", new DslProperty("text/plain", "application/json"));
		assertThat(ContentUtils.recognizeContentTypeFromTestHeader(headers)).isEqualTo(ContentType.JSON);
	}

	// --- getEqualsTypeFromContentType ---

	@Test
	void getEqualsTypeFromContentType_all_branches() {
		assertThat(ContentUtils.getEqualsTypeFromContentType(ContentType.JSON))
			.isEqualTo(MatchingStrategy.Type.EQUAL_TO_JSON);
		assertThat(ContentUtils.getEqualsTypeFromContentType(ContentType.XML))
			.isEqualTo(MatchingStrategy.Type.EQUAL_TO_XML);
		assertThat(ContentUtils.getEqualsTypeFromContentType(ContentType.TEXT))
			.isEqualTo(MatchingStrategy.Type.EQUAL_TO);
	}

	// --- recognizeContentTypeFromContent overloads ---

	@Test
	void recognizeContentTypeFromContent_dynamicString_json() {
		assertThat(ContentUtils.recognizeContentTypeFromContent(ds("{\"a\":1}"))).isEqualTo(ContentType.JSON);
	}

	@Test
	void recognizeContentTypeFromContent_dynamicString_xml() {
		assertThat(ContentUtils.recognizeContentTypeFromContent(ds("<a>1</a>"))).isEqualTo(ContentType.XML);
	}

	@Test
	void recognizeContentTypeFromContent_dynamicString_unknown() {
		assertThat(ContentUtils.recognizeContentTypeFromContent(ds("plain <<"))).isEqualTo(ContentType.UNKNOWN);
	}

	@Test
	void recognizeContentTypeFromContent_map() {
		assertThat(ContentUtils.recognizeContentTypeFromContent(Map.of("a", 1))).isEqualTo(ContentType.JSON);
	}

	@Test
	void recognizeContentTypeFromContent_bytes() {
		assertThat(ContentUtils.recognizeContentTypeFromContent(new byte[] { 1, 2 })).isEqualTo(ContentType.UNKNOWN);
	}

	@Test
	void recognizeContentTypeFromContent_list() {
		assertThat(ContentUtils.recognizeContentTypeFromContent(List.of(1, 2))).isEqualTo(ContentType.JSON);
	}

	@Test
	void recognizeContentTypeFromContent_string_json_xml_unknown() {
		assertThat(ContentUtils.recognizeContentTypeFromContent("{\"a\":1}")).isEqualTo(ContentType.JSON);
		assertThat(ContentUtils.recognizeContentTypeFromContent("<a>1</a>")).isEqualTo(ContentType.XML);
		assertThat(ContentUtils.recognizeContentTypeFromContent("plain <<")).isEqualTo(ContentType.UNKNOWN);
	}

	@Test
	void recognizeContentTypeFromContent_number() {
		assertThat(ContentUtils.recognizeContentTypeFromContent(42)).isEqualTo(ContentType.TEXT);
	}

	@Test
	void recognizeContentTypeFromContent_object_dispatch_and_default() {
		assertThat(ContentUtils.recognizeContentTypeFromContent((Object) ds("{\"a\":1}"))).isEqualTo(ContentType.JSON);
		assertThat(ContentUtils.recognizeContentTypeFromContent((Object) Map.of("a", 1))).isEqualTo(ContentType.JSON);
		assertThat(ContentUtils.recognizeContentTypeFromContent((Object) new byte[] { 1 }))
			.isEqualTo(ContentType.UNKNOWN);
		assertThat(ContentUtils.recognizeContentTypeFromContent((Object) List.of(1))).isEqualTo(ContentType.JSON);
		assertThat(ContentUtils.recognizeContentTypeFromContent((Object) "{\"a\":1}")).isEqualTo(ContentType.JSON);
		assertThat(ContentUtils.recognizeContentTypeFromContent((Object) Integer.valueOf(7)))
			.isEqualTo(ContentType.TEXT);
		assertThat(ContentUtils.recognizeContentTypeFromContent((Object) Boolean.TRUE)).isEqualTo(ContentType.UNKNOWN);
	}

	@Test
	void recognizeContentTypeFromContent_fromFileProperty_json(@TempDir Path dir) throws Exception {
		File file = dir.resolve("body.json").toFile();
		Files.writeString(file.toPath(), "{\"a\":1}");
		FromFileProperty prop = new FromFileProperty(file, String.class);
		assertThat(ContentUtils.recognizeContentTypeFromContent((Object) prop)).isEqualTo(ContentType.JSON);
	}

	@Test
	void recognizeContentTypeFromContent_fromFileProperty_xml(@TempDir Path dir) throws Exception {
		File file = dir.resolve("body.xml").toFile();
		Files.writeString(file.toPath(), "<a>1</a>");
		FromFileProperty prop = new FromFileProperty(file, String.class);
		assertThat(ContentUtils.recognizeContentTypeFromContent((Object) prop)).isEqualTo(ContentType.XML);
	}

	@Test
	void recognizeContentTypeFromContent_fromFileProperty_string_body(@TempDir Path dir) throws Exception {
		File file = dir.resolve("body.txt").toFile();
		Files.writeString(file.toPath(), "{\"a\":1}");
		FromFileProperty prop = new FromFileProperty(file, String.class);
		assertThat(ContentUtils.recognizeContentTypeFromContent((Object) prop)).isEqualTo(ContentType.JSON);
	}

	@Test
	void recognizeContentTypeFromContent_fromFileProperty_bytes(@TempDir Path dir) throws Exception {
		File file = dir.resolve("body.dat").toFile();
		Files.write(file.toPath(), new byte[] { 1, 2, 3 });
		FromFileProperty prop = new FromFileProperty(file, byte[].class);
		assertThat(ContentUtils.recognizeContentTypeFromContent((Object) prop)).isEqualTo(ContentType.UNKNOWN);
	}

	// --- isJsonType / isXmlType ---

	@Test
	void isJsonType_true_false_and_empty() {
		assertThat(ContentUtils.isJsonType(ds("{\"a\":1}"))).isTrue();
		assertThat(ContentUtils.isJsonType(ds("<a>1</a>"))).isFalse();
		assertThat(ContentUtils.isJsonType(new DynamicStringImpl(new Object[0], new String[] { "" }))).isFalse();
	}

	@Test
	void isJsonType_escapes_non_string_values() {
		DynamicStringImpl body = new DynamicStringImpl(new Object[] { Integer.valueOf(5) },
				new String[] { "{\"a\":", "}" });
		assertThat(ContentUtils.isJsonType(body)).isTrue();
	}

	@Test
	void isXmlType_true_and_false() {
		assertThat(ContentUtils.isXmlType(ds("<a>1</a>"))).isTrue();
		assertThat(ContentUtils.isXmlType(ds("{\"a\":1}"))).isFalse();
	}

	@Test
	void isXmlType_escapes_non_string_values() {
		DynamicStringImpl body = new DynamicStringImpl(new Object[] { Integer.valueOf(5) },
				new String[] { "<a>", "</a>" });
		assertThat(ContentUtils.isXmlType(body)).isTrue();
	}

	@Test
	void namedPropertyValueForJava_execution() {
		NamedProperty prop = named("file.txt", new ExecutionProperty("valFn()"), null);
		assertThat(ContentUtils.namedPropertyValueForJava(prop, "\"", (f) -> "X")).isEqualTo("valFn()");
	}

	// --- recognizeContentTypeFromMatchingStrategy ---

	@Test
	void recognizeContentTypeFromMatchingStrategy_all_branches() {
		assertThat(ContentUtils.recognizeContentTypeFromMatchingStrategy(MatchingStrategy.Type.EQUAL_TO_XML))
			.isEqualTo(ContentType.XML);
		assertThat(ContentUtils.recognizeContentTypeFromMatchingStrategy(MatchingStrategy.Type.EQUAL_TO_JSON))
			.isEqualTo(ContentType.JSON);
		assertThat(ContentUtils.recognizeContentTypeFromMatchingStrategy(MatchingStrategy.Type.EQUAL_TO))
			.isEqualTo(ContentType.UNKNOWN);
	}

	// --- multipart helpers ---

	private static NamedProperty named(Object name, Object value, Object contentType) {
		return new NamedProperty(NamedProperty.asDslProperty(name), NamedProperty.asDslProperty(value),
				NamedProperty.asDslProperty(contentType));
	}

	@Test
	void getGroovyMultipartFileParameterContent_string_value() {
		NamedProperty prop = named("file.txt", "content", "text/plain");
		String out = ContentUtils.getGroovyMultipartFileParameterContent("field", prop, (f) -> "FROMFILE");
		assertThat(out).isEqualTo("'field', 'file.txt', 'content'.bytes, 'text/plain'");
	}

	@Test
	void getJavaMultipartFileParameterContent_string_value() {
		NamedProperty prop = named("file.txt", "content", "text/plain");
		String out = ContentUtils.getJavaMultipartFileParameterContent("field", prop, (f) -> "FROMFILE");
		assertThat(out).isEqualTo("\"field\", \"file.txt\", \"content\".getBytes(), \"text/plain\"");
	}

	@Test
	void getGroovyMultipartFileParameterContent_no_content_type() {
		NamedProperty prop = named("file.txt", "content", null);
		String out = ContentUtils.getGroovyMultipartFileParameterContent("field", prop, (f) -> "FROMFILE");
		assertThat(out).isEqualTo("'field', 'file.txt', 'content'.bytes");
	}

	@Test
	void namedPropertyName_execution_value() {
		NamedProperty prop = named(new ExecutionProperty("nameFn()"), "content", null);
		assertThat(ContentUtils.namedPropertyName(prop, "'")).isEqualTo("nameFn()");
	}

	@Test
	void namedPropertyName_quoted_value() {
		NamedProperty prop = named("na\"me", "content", null);
		assertThat(ContentUtils.namedPropertyName(prop, "\"")).isEqualTo("\"na\\\"me\"");
	}

	@Test
	void namedContentTypeNameIfPresent_null_returns_empty() {
		NamedProperty prop = named("file.txt", "content", null);
		assertThat(ContentUtils.namedContentTypeNameIfPresent(prop, "'")).isEmpty();
	}

	@Test
	void namedContentTypeNameIfPresent_execution() {
		NamedProperty prop = named("file.txt", "content", new ExecutionProperty("ctFn()"));
		assertThat(ContentUtils.namedContentTypeNameIfPresent(prop, "'")).isEqualTo(", ctFn()");
	}

	@Test
	void namedPropertyValueForGroovy_execution() {
		NamedProperty prop = named("file.txt", new ExecutionProperty("valFn()"), null);
		assertThat(ContentUtils.namedPropertyValueForGroovy(prop, "'", (f) -> "FROMFILE")).isEqualTo("valFn()");
	}

	@Test
	void namedPropertyValueForGroovy_byte_array() {
		NamedProperty prop = named("file.txt", new byte[] { 1, 2, 3 }, null);
		assertThat(ContentUtils.namedPropertyValueForGroovy(prop, "'", (f) -> "FROMFILE"))
			.isEqualTo("[1, 2, 3] as byte[]");
	}

	@Test
	void namedPropertyValueForJava_byte_array() {
		NamedProperty prop = named("file.txt", new byte[] { 1, 2, 3 }, null);
		assertThat(ContentUtils.namedPropertyValueForJava(prop, "\"", (f) -> "FROMFILE"))
			.isEqualTo("new byte[] {1, 2, 3}");
	}

	@Test
	void namedPropertyValueForGroovy_fromFile_byte(@TempDir Path dir) throws Exception {
		File file = dir.resolve("f.dat").toFile();
		Files.write(file.toPath(), new byte[] { 9 });
		FromFileProperty prop = new FromFileProperty(file, byte[].class);
		NamedProperty named = named("f.dat", prop, null);
		assertThat(ContentUtils.namedPropertyValueForGroovy(named, "'", (f) -> "FROMFILE_BYTES"))
			.isEqualTo("FROMFILE_BYTES");
	}

	@Test
	void namedPropertyValueForGroovy_fromFile_string(@TempDir Path dir) throws Exception {
		File file = dir.resolve("f.txt").toFile();
		Files.write(file.toPath(), new byte[] { 10, 20 });
		FromFileProperty prop = new FromFileProperty(file, String.class);
		NamedProperty named = named("f.txt", prop, null);
		assertThat(ContentUtils.namedPropertyValueForGroovy(named, "'", (f) -> "FROMFILE_BYTES"))
			.isEqualTo("[10, 20] as byte[]");
	}

	@Test
	void namedPropertyValueForJava_fromFile_byte(@TempDir Path dir) throws Exception {
		File file = dir.resolve("f.dat").toFile();
		Files.write(file.toPath(), new byte[] { 9 });
		FromFileProperty prop = new FromFileProperty(file, byte[].class);
		NamedProperty named = named("f.dat", prop, null);
		assertThat(ContentUtils.namedPropertyValueForJava(named, "\"", (f) -> "FROMFILE_BYTES"))
			.isEqualTo("FROMFILE_BYTES");
	}

	@Test
	void namedPropertyValueForJava_fromFile_string(@TempDir Path dir) throws Exception {
		File file = dir.resolve("f.txt").toFile();
		Files.write(file.toPath(), new byte[] { 10, 20 });
		FromFileProperty prop = new FromFileProperty(file, String.class);
		NamedProperty named = named("f.txt", prop, null);
		assertThat(ContentUtils.namedPropertyValueForJava(named, "\"", (f) -> "FROMFILE_BYTES"))
			.isEqualTo("new byte[] {10, 20}");
	}

	@Test
	void namedPropertyValueForJava_string_getBytes() {
		NamedProperty prop = named("file.txt", "abc", null);
		assertThat(ContentUtils.namedPropertyValueForJava(prop, "\"", (f) -> "X")).isEqualTo("\"abc\".getBytes()");
	}

	// --- joinBytes indirectly asserts single-byte has no separator ---

	@Test
	void joinBytes_single_byte_no_leading_separator() {
		NamedProperty prop = named("file.txt", new byte[] { 7 }, null);
		assertThat(ContentUtils.namedPropertyValueForGroovy(prop, "'", (f) -> "X")).isEqualTo("[7] as byte[]");
	}

	// --- evaluateClientSideContentType / evaluateServerSideContentType ---

	@Test
	void evaluateClientSideContentType_header_wins() {
		Headers headers = new Headers();
		headers.header("Content-Type", new DslProperty("application/json", "application/xml"));
		assertThat(ContentUtils.evaluateClientSideContentType(headers, "plain <<")).isEqualTo(ContentType.JSON);
	}

	@Test
	void evaluateClientSideContentType_falls_back_to_body() {
		Headers headers = new Headers();
		assertThat(ContentUtils.evaluateClientSideContentType(headers, "{\"a\":1}")).isEqualTo(ContentType.JSON);
	}

	@Test
	void evaluateServerSideContentType_header_wins() {
		Headers headers = new Headers();
		headers.header("Content-Type", new DslProperty("application/xml", "application/json"));
		assertThat(ContentUtils.evaluateServerSideContentType(headers, "plain <<")).isEqualTo(ContentType.JSON);
	}

	@Test
	void evaluateServerSideContentType_falls_back_to_body() {
		Headers headers = new Headers();
		assertThat(ContentUtils.evaluateServerSideContentType(headers, "<a>1</a>")).isEqualTo(ContentType.XML);
	}

	@Test
	void transformXml_escapes_special_characters() {
		DynamicStringImpl body = ds("<a>a &amp; b</a>");
		Object result = ContentUtils.extractValue(body, ContentType.XML, ContentUtils.GET_TEST_SIDE_FUNCTION);
		assertThat(result.toString()).contains("&amp;");
	}

	@Test
	void bytes_roundtrip_utf8_marker() {
		// guards against MATH mutation altering escapeJson path indirectly
		byte[] data = "hi".getBytes(StandardCharsets.UTF_8);
		NamedProperty prop = named("file.txt", data, null);
		assertThat(ContentUtils.namedPropertyValueForJava(prop, "\"", (f) -> "X")).isEqualTo("new byte[] {104, 105}");
	}

}
