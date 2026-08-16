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

package sh.stubborn.contract.verifier.template;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.Request;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link HandlebarsTemplateProcessor} and
 * {@link TestSideRequestTemplateModel}.
 */
class TemplateProcessorMutationTests {

	private final HandlebarsTemplateProcessor processor = new HandlebarsTemplateProcessor();

	private static Request request(String url, Object body) {
		Contract contract = Contract.make((dsl) -> dsl.request((r) -> {
			r.method("POST");
			r.url(url);
			if (body != null) {
				r.body(body);
			}
			r.headers((h) -> h.header("X-Key", "val"));
		}));
		return contract.getRequest();
	}

	// --- delegating template token methods ---

	@Test
	void opening_and_closing_tokens() {
		assertThat(this.processor.openingTemplate()).isEqualTo("{{");
		assertThat(this.processor.closingTemplate()).isEqualTo("}}");
		assertThat(this.processor.escapedOpeningTemplate()).isEqualTo("{{{");
		assertThat(this.processor.escapedClosingTemplate()).isEqualTo("}}}");
	}

	@Test
	void unescaped_helpers() {
		assertThat(this.processor.url()).isEqualTo("{{request.url}}");
		assertThat(this.processor.query("k")).isEqualTo("{{request.query.k.[0]}}");
		assertThat(this.processor.query("k", 2)).isEqualTo("{{request.query.k.[2]}}");
		assertThat(this.processor.path()).isEqualTo("{{request.path}}");
		assertThat(this.processor.path(1)).isEqualTo("{{request.path.[1]}}");
		assertThat(this.processor.header("h")).isEqualTo("{{request.headers.h.[0]}}");
		assertThat(this.processor.header("h", 2)).isEqualTo("{{request.headers.h.[2]}}");
		assertThat(this.processor.cookie("c")).isEqualTo("{{request.cookies.c}}");
		assertThat(this.processor.body()).isEqualTo("{{request.body}}");
		assertThat(this.processor.body("$.a")).isEqualTo("{{jsonPath request.body '$.a'}}");
	}

	@Test
	void escaped_helpers() {
		assertThat(this.processor.escapedUrl()).isEqualTo("{{{request.url}}}");
		assertThat(this.processor.escapedQuery("k")).isEqualTo("{{{request.query.k.[0]}}}");
		assertThat(this.processor.escapedQuery("k", 2)).isEqualTo("{{{request.query.k.[2]}}}");
		assertThat(this.processor.escapedPath()).isEqualTo("{{{request.path}}}");
		assertThat(this.processor.escapedPath(1)).isEqualTo("{{{request.path.[1]}}}");
		assertThat(this.processor.escapedHeader("h")).isEqualTo("{{{request.headers.h.[0]}}}");
		assertThat(this.processor.escapedHeader("h", 2)).isEqualTo("{{{request.headers.h.[2]}}}");
		assertThat(this.processor.escapedCookie("c")).isEqualTo("{{{request.cookies.c}}}");
		assertThat(this.processor.escapedBody()).isEqualTo("{{{escapejsonbody}}}");
		assertThat(this.processor.escapedBody("$.a")).isEqualTo("{{{jsonPath request.body '$.a'}}}");
	}

	@Test
	void startsWith_template_variants() {
		assertThat(this.processor.startsWithTemplate("{{x}}")).isTrue();
		assertThat(this.processor.startsWithTemplate("abc")).isFalse();
		assertThat(this.processor.startsWithEscapedTemplate("{{{x}}}")).isTrue();
		assertThat(this.processor.startsWithEscapedTemplate("abc")).isFalse();
	}

	// --- containsTemplateEntry ---

	@Test
	void containsTemplateEntry_true_for_plain_template() {
		assertThat(this.processor.containsTemplateEntry("body {{x}} end")).isTrue();
	}

	@Test
	void containsTemplateEntry_true_for_escaped_template() {
		assertThat(this.processor.containsTemplateEntry("body {{{x}}} end")).isTrue();
	}

	@Test
	void containsTemplateEntry_false_without_closing() {
		assertThat(this.processor.containsTemplateEntry("body {{x end")).isFalse();
	}

	@Test
	void containsTemplateEntry_false_for_plain_text() {
		assertThat(this.processor.containsTemplateEntry("no templates here")).isFalse();
	}

	// --- containsJsonPathTemplateEntry ---

	@Test
	void containsJsonPathTemplateEntry_variants() {
		assertThat(this.processor.containsJsonPathTemplateEntry("{{jsonpath this '$.a'}}")).isTrue();
		assertThat(this.processor.containsJsonPathTemplateEntry("{{jsonPath request.body '$.a'}}")).isTrue();
		assertThat(this.processor.containsJsonPathTemplateEntry("{{{jsonpath this '$.a'}}}")).isTrue();
		assertThat(this.processor.containsJsonPathTemplateEntry("{{{jsonPath request.body '$.a'}}}")).isTrue();
		assertThat(this.processor.containsJsonPathTemplateEntry("{{request.body}}")).isFalse();
		assertThat(this.processor.containsJsonPathTemplateEntry("plain")).isFalse();
	}

	// --- jsonPathFromTemplateEntry ---

	@Test
	void jsonPathFromTemplateEntry_extracts_group() {
		assertThat(this.processor.jsonPathFromTemplateEntry("{{jsonpath this '$.legacy'}}")).isEqualTo("$.legacy");
		assertThat(this.processor.jsonPathFromTemplateEntry("{{jsonPath request.body '$.field'}}"))
			.isEqualTo("$.field");
		assertThat(this.processor.jsonPathFromTemplateEntry("{{{jsonpath this '$.esc'}}}")).isEqualTo("$.esc");
		assertThat(this.processor.jsonPathFromTemplateEntry("{{{jsonPath request.body '$.escNew'}}}"))
			.isEqualTo("$.escNew");
	}

	@Test
	void jsonPathFromTemplateEntry_empty_when_no_entry() {
		assertThat(this.processor.jsonPathFromTemplateEntry("plain text")).isEmpty();
	}

	// --- transform ---

	@Test
	void transform_plain_content_unchanged() {
		Request request = request("/foo", Map.of("name", "John"));
		assertThat(this.processor.transform(request, "Hello World")).isEqualTo("Hello World");
	}

	@Test
	void transform_jsonpath_resolves_value_from_body() {
		Request request = request("/foo", Map.of("name", "John"));
		String result = this.processor.transform(request, "{{{jsonPath request.body '$.name'}}}");
		assertThat(result).isEqualTo("John");
	}

	@Test
	void transform_legacy_jsonpath_resolves_value_from_body() {
		Request request = request("/foo", Map.of("name", "John"));
		String result = this.processor.transform(request, "{{{jsonpath this '$.name'}}}");
		assertThat(result).isEqualTo("John");
	}

	@Test
	void transform_jsonpath_long_value_gets_L_suffix() {
		Request request = request("/foo", Map.of("big", 10000000000L));
		String result = this.processor.transform(request, "{{{jsonpath this '$.big'}}}");
		assertThat(result).isEqualTo("10000000000L");
	}

	// --- TestSideRequestTemplateModel ---

	@Test
	void model_exposes_url_path_headers_and_body() {
		Request request = request("/foo/bar", Map.of("name", "John"));
		TestSideRequestTemplateModel model = TestSideRequestTemplateModel.from(request);
		assertThat(model.getUrl()).isEqualTo("/foo/bar");
		assertThat(model.getPath().toString()).isEqualTo("/foo/bar");
		assertThat(model.getPath()).containsExactly("foo", "bar");
		assertThat(model.getHeaders()).containsKey("X-Key");
		assertThat(model.getHeaders().get("X-Key")).containsExactly("val");
		assertThat(model.getBody()).isEqualTo("{\"name\":\"John\"}");
		assertThat(model.getEscapedBody()).contains("name").contains("John");
		assertThat(model.getQuery()).isEmpty();
	}

	@Test
	void model_null_body_yields_null_body() {
		Request request = request("/foo", null);
		TestSideRequestTemplateModel model = TestSideRequestTemplateModel.from(request);
		assertThat(model.getBody()).isNull();
	}

	@Test
	void model_string_body_is_raw() {
		Request request = request("/foo", "just a string body");
		TestSideRequestTemplateModel model = TestSideRequestTemplateModel.from(request);
		assertThat(model.getBody()).isEqualTo("just a string body");
	}

	@Test
	void model_with_query_parameters_builds_full_url() {
		Contract contract = Contract.make((dsl) -> dsl.request((r) -> {
			r.method("GET");
			r.urlPath("/foo", (up) -> up.queryParameters((q) -> q.parameter("a", "b")));
		}));
		TestSideRequestTemplateModel model = TestSideRequestTemplateModel.from(contract.getRequest());
		assertThat(model.getUrl()).isEqualTo("/foo?a=b");
		assertThat(model.getQuery()).containsKey("a");
		assertThat((List<Object>) model.getQuery().get("a")).containsExactly("b");
	}

	@Test
	void path_toString_joins_with_slash() {
		Request request = request("/a/b/c", null);
		TestSideRequestTemplateModel model = TestSideRequestTemplateModel.from(request);
		assertThat(model.getPath().toString()).isEqualTo("/a/b/c");
	}

}
