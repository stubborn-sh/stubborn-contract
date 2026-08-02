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
 * carries a structured {@link ResponseModel} — proves the {@code // then:} status/header
 * assertions are emitted from the model, each terminated with a single {@code ;}, and the
 * verbatim {@code // and:} body block (carried on {@code bodyLines}) follows.
 *
 * @author Marcin Grzejszczak
 */
class ResponseModelRendererTests {

	private final JavaPoetTestRenderer renderer = new JavaPoetTestRenderer();

	@Test
	void renders_structured_then_status_and_headers_then_verbatim_and_body() {
		RequestModel request = new RequestModel(
				new FluentStatement("MockMvcRequestSpecification request = given()", List.of()),
				new FluentStatement("ResponseOptions response = given().spec(request)", List.of(".get(\"/items/1\")")));
		ResponseModel response = new ResponseModel(
				new StatementList(List.of("assertThat(response.statusCode()).isEqualTo(200)",
						"assertThat(response.header(\"X-Reply\")).isEqualTo(\"def\")")));
		TestClassModel model = new TestClassModel("com.example", "ContractTest", null, false, List.of(), List
			.of(new TestMethodModel("validate_contract", List.of(AnnotationModel.marker("org.junit.jupiter.api.Test")),
					List.of("// and:", "DocumentContext parsedJson = JsonPath.parse(response.getBody().asString());",
							"assertThatJson(parsedJson).field(\"['id']\").isEqualTo(1);"),
					request, response)),
				List.of());

		List<String> lines = bodyLines(this.renderer.render(model));

		// // then: label, the status assertion, the header assertion (each ;-terminated),
		// then the verbatim // and: body block
		assertThat(lines).containsSubsequence("// when:", "ResponseOptions response = given().spec(request)",
				".get(\"/items/1\");", "// then:", "assertThat(response.statusCode()).isEqualTo(200);",
				"assertThat(response.header(\"X-Reply\")).isEqualTo(\"def\");", "// and:",
				"DocumentContext parsedJson = JsonPath.parse(response.getBody().asString());",
				"assertThatJson(parsedJson).field(\"['id']\").isEqualTo(1);");
		// the structured then statements carry a single terminator
		assertThat(lines).contains("assertThat(response.statusCode()).isEqualTo(200);")
			.doesNotContain("assertThat(response.statusCode()).isEqualTo(200)")
			.doesNotContain("assertThat(response.statusCode()).isEqualTo(200);;");
	}

	@Test
	void renders_structured_then_status_only_with_no_and_body() {
		RequestModel request = new RequestModel(
				new FluentStatement("MockMvcRequestSpecification request = given()", List.of()), new FluentStatement(
						"ResponseOptions response = given().spec(request)", List.of(".get(\"/resp/exec\")")));
		ResponseModel response = new ResponseModel(
				new StatementList(List.of("assertThat(response.statusCode()).isEqualTo(200)")));
		// a bodyless response leaves no verbatim // and: tail
		TestClassModel model = new TestClassModel("com.example", "ContractTest", null, false, List.of(),
				List.of(new TestMethodModel("validate_contract",
						List.of(AnnotationModel.marker("org.junit.jupiter.api.Test")), List.of(), request, response)),
				List.of());

		List<String> lines = bodyLines(this.renderer.render(model));

		assertThat(lines).containsSubsequence("// then:", "assertThat(response.statusCode()).isEqualTo(200);");
		assertThat(lines).doesNotContain("// and:");
	}

	private List<String> bodyLines(String rendered) {
		return rendered.lines().map(String::trim).filter((line) -> !line.isEmpty()).toList();
	}

}
