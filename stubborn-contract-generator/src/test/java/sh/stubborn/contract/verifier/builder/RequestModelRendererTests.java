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

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JavaPoetTestRenderer} rendering a {@link TestMethodModel} that
 * carries a structured {@link RequestModel} — proves the
 * {@code // given:}/{@code // when:} chains are emitted from the model with the correct
 * {@code ;} placement, and the verbatim {@code // then:} block follows.
 *
 * @author Marcin Grzejszczak
 */
class RequestModelRendererTests {

	private final JavaPoetTestRenderer renderer = new JavaPoetTestRenderer();

	@Test
	void renders_mockmvc_request_model_with_header_and_url() {
		RequestModel request = new RequestModel(
				new FluentStatement("MockMvcRequestSpecification request = given()",
						List.of(".header(\"X-Trace\", \"abc\")")),
				new FluentStatement("ResponseOptions response = given().spec(request)", List.of(".get(\"/items/1\")")));
		TestClassModel model = new TestClassModel("com.example", "ContractTest", null, false, List.of(),
				List.of(new TestMethodModel("validate_contract",
						List.of(AnnotationModel.marker("org.junit.jupiter.api.Test")),
						List.of("// then:", "assertThat(response.statusCode()).isEqualTo(200);"), request)),
				List.of());

		List<String> lines = bodyLines(this.renderer.render(model));

		// given: head + header (only the header carries the ;), then when: head + url
		assertThat(lines).containsSubsequence("// given:", "MockMvcRequestSpecification request = given()",
				".header(\"X-Trace\", \"abc\");", "// when:", "ResponseOptions response = given().spec(request)",
				".get(\"/items/1\");", "// then:", "assertThat(response.statusCode()).isEqualTo(200);");
		// the fluent-chain heads must NOT be terminated
		assertThat(lines).contains("MockMvcRequestSpecification request = given()")
			.contains("ResponseOptions response = given().spec(request)")
			.doesNotContain("MockMvcRequestSpecification request = given();")
			.doesNotContain("ResponseOptions response = given().spec(request);");
	}

	@Test
	void renders_mockmvc_request_model_with_header_and_body() {
		RequestModel request = new RequestModel(
				new FluentStatement("MockMvcRequestSpecification request = given()",
						List.of(".header(\"Content-Type\", \"application/json\")",
								".body(\"{\\\"foo\\\":\\\"bar\\\"}\")")),
				new FluentStatement("ResponseOptions response = given().spec(request)", List.of(".put(\"/foo\")")));
		TestClassModel model = new TestClassModel("com.example", "ContractTest", null, false, List.of(),
				List.of(new TestMethodModel("validate_contract",
						List.of(AnnotationModel.marker("org.junit.jupiter.api.Test")),
						List.of("// then:", "assertThat(response.statusCode()).isEqualTo(200);"), request)),
				List.of());

		List<String> lines = bodyLines(this.renderer.render(model));

		// given: head + header (unterminated) + body (the body, being last, carries the
		// ;)
		assertThat(lines).containsSubsequence("// given:", "MockMvcRequestSpecification request = given()",
				".header(\"Content-Type\", \"application/json\")", ".body(\"{\\\"foo\\\":\\\"bar\\\"}\");", "// when:",
				"ResponseOptions response = given().spec(request)", ".put(\"/foo\");", "// then:",
				"assertThat(response.statusCode()).isEqualTo(200);");
		// the header line must NOT be terminated (only the trailing body line is)
		assertThat(lines).contains(".header(\"Content-Type\", \"application/json\")")
			.doesNotContain(".header(\"Content-Type\", \"application/json\");")
			.doesNotContain(".body(\"{\\\"foo\\\":\\\"bar\\\"}\")");
	}

	@Test
	void renders_explicit_request_model_heads() {
		RequestModel request = new RequestModel(
				new FluentStatement("RequestSpecification request = given()", List.of(".header(\"X-Trace\", \"abc\")")),
				new FluentStatement("Response response = given().spec(request)", List.of(".get(\"/items/1\")")));
		TestClassModel model = new TestClassModel("com.example", "ContractTest", null, false, List.of(),
				List.of(new TestMethodModel("validate_contract",
						List.of(AnnotationModel.marker("org.junit.jupiter.api.Test")), List.of("// then:", "ok();"),
						request)),
				List.of());

		List<String> lines = bodyLines(this.renderer.render(model));

		assertThat(lines).containsSubsequence("// given:", "RequestSpecification request = given()",
				".header(\"X-Trace\", \"abc\");", "// when:", "Response response = given().spec(request)",
				".get(\"/items/1\");", "// then:", "ok();");
	}

	@Test
	void renders_mockmvc_request_model_with_header_then_cookie() {
		RequestModel request = new RequestModel(
				new FluentStatement("MockMvcRequestSpecification request = given()",
						List.of(".header(\"X-Trace\", \"abc\")", ".cookie(\"session\", \"abc123\")")),
				new FluentStatement("ResponseOptions response = given().spec(request)", List.of(".get(\"/items\")")));
		TestClassModel model = new TestClassModel("com.example", "ContractTest", null, false, List.of(),
				List.of(new TestMethodModel("validate_contract",
						List.of(AnnotationModel.marker("org.junit.jupiter.api.Test")),
						List.of("// then:", "assertThat(response.statusCode()).isEqualTo(200);"), request)),
				List.of());

		List<String> lines = bodyLines(this.renderer.render(model));

		// given: head + header (unterminated) + cookie (last continuation carries the ;)
		assertThat(lines).containsSubsequence("// given:", "MockMvcRequestSpecification request = given()",
				".header(\"X-Trace\", \"abc\")", ".cookie(\"session\", \"abc123\");", "// when:",
				"ResponseOptions response = given().spec(request)", ".get(\"/items\");");
		// the .cookie(...) line follows the .header(...) line, and only the cookie (being
		// last) is terminated
		assertThat(lines).containsSubsequence(".header(\"X-Trace\", \"abc\")", ".cookie(\"session\", \"abc123\");")
			.doesNotContain(".header(\"X-Trace\", \"abc\");")
			.doesNotContain(".cookie(\"session\", \"abc123\")");
	}

	@Test
	void renders_mockmvc_request_model_with_query_param_before_url() {
		RequestModel request = new RequestModel(
				new FluentStatement("MockMvcRequestSpecification request = given()", List.of()),
				new FluentStatement("ResponseOptions response = given().spec(request)",
						List.of(".queryParam(\"page\",\"2\")", ".get(\"/items\")")));
		TestClassModel model = new TestClassModel("com.example", "ContractTest", null, false, List.of(),
				List.of(new TestMethodModel("validate_contract",
						List.of(AnnotationModel.marker("org.junit.jupiter.api.Test")),
						List.of("// then:", "assertThat(response.statusCode()).isEqualTo(200);"), request)),
				List.of());

		List<String> lines = bodyLines(this.renderer.render(model));

		// when: head + queryParam (unterminated) + url (the url, being last, carries the
		// ;)
		assertThat(lines).containsSubsequence("// when:", "ResponseOptions response = given().spec(request)",
				".queryParam(\"page\",\"2\")", ".get(\"/items\");");
		// the .queryParam(...) line precedes the url line, and only the url is terminated
		assertThat(lines).doesNotContain(".queryParam(\"page\",\"2\");").doesNotContain(".get(\"/items\")");
	}

	@Test
	void renders_mockmvc_request_model_with_async_between_query_param_and_url() {
		RequestModel request = new RequestModel(
				new FluentStatement("MockMvcRequestSpecification request = given()", List.of()),
				new FluentStatement("ResponseOptions response = given().spec(request)",
						List.of(".queryParam(\"page\",\"2\")", ".when().async()", ".get(\"/async\")")));
		TestClassModel model = new TestClassModel("com.example", "ContractTest", null, false, List.of(),
				List.of(new TestMethodModel("validate_contract",
						List.of(AnnotationModel.marker("org.junit.jupiter.api.Test")),
						List.of("// then:", "assertThat(response.statusCode()).isEqualTo(200);"), request)),
				List.of());

		List<String> lines = bodyLines(this.renderer.render(model));

		// when: head + queryParam (unterminated) + async (unterminated) + url (last,
		// carries the ;) — the async line sits AFTER queryParams and BEFORE the url
		assertThat(lines).containsSubsequence("// when:", "ResponseOptions response = given().spec(request)",
				".queryParam(\"page\",\"2\")", ".when().async()", ".get(\"/async\");");
		// only the terminal url line is terminated
		assertThat(lines).doesNotContain(".queryParam(\"page\",\"2\");")
			.doesNotContain(".when().async();")
			.doesNotContain(".get(\"/async\")");
	}

	@Test
	void renders_mockmvc_request_model_with_multipart_after_body() {
		RequestModel request = new RequestModel(
				new FluentStatement("MockMvcRequestSpecification request = given()",
						List.of(".header(\"Content-Type\", \"multipart/form-data\")", ".body(\"{}\")",
								".param(\"formParameter\", \"formValue\")",
								".multiPart(\"file\", \"filename.csv\", \"file content\".getBytes())")),
				new FluentStatement("ResponseOptions response = given().spec(request)",
						List.of(".put(\"/multipart\")")));
		TestClassModel model = new TestClassModel("com.example", "ContractTest", null, false, List.of(),
				List.of(new TestMethodModel("validate_contract",
						List.of(AnnotationModel.marker("org.junit.jupiter.api.Test")),
						List.of("// then:", "assertThat(response.statusCode()).isEqualTo(200);"), request)),
				List.of());

		List<String> lines = bodyLines(this.renderer.render(model));

		// given: head + header + body + multipart lines (in that order); only the LAST
		// multipart line carries the ; — the body and the .param(...) line do not
		assertThat(lines).containsSubsequence("// given:", "MockMvcRequestSpecification request = given()",
				".header(\"Content-Type\", \"multipart/form-data\")", ".body(\"{}\")",
				".param(\"formParameter\", \"formValue\")",
				".multiPart(\"file\", \"filename.csv\", \"file content\".getBytes());");
		assertThat(lines).contains(".body(\"{}\")")
			.doesNotContain(".body(\"{}\");")
			.doesNotContain(".param(\"formParameter\", \"formValue\");")
			.doesNotContain(".multiPart(\"file\", \"filename.csv\", \"file content\".getBytes())");
	}

	@Test
	void head_only_when_chain_terminates_the_head() {
		RequestModel request = new RequestModel(
				new FluentStatement("MockMvcRequestSpecification request = given()", List.of()),
				new FluentStatement("ResponseOptions response = given().spec(request)", List.of(".get(\"/items/1\")")));
		TestClassModel model = new TestClassModel("com.example", "ContractTest", null, false, List.of(),
				List.of(new TestMethodModel("validate_contract",
						List.of(AnnotationModel.marker("org.junit.jupiter.api.Test")), List.of("// then:", "ok();"),
						request)),
				List.of());

		List<String> lines = bodyLines(this.renderer.render(model));

		// a given chain with only the head gets the ; on the head itself
		assertThat(lines).contains("MockMvcRequestSpecification request = given();");
	}

	private List<String> bodyLines(String rendered) {
		return rendered.lines().map(String::trim).filter((line) -> !line.isEmpty()).toList();
	}

}
