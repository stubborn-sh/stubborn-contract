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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link JavaTestRenderer} — proves a {@link TestClassModel} renders to
 * structurally-correct Java through the Handlebars templates.
 *
 * @author Marcin Grzejszczak
 */
class JavaTestRendererTests {

	private final JavaTestRenderer renderer = new JavaTestRenderer();

	@Test
	void renders_class_scaffold_from_model() {
		TestClassModel model = new TestClassModel("com.example", "FooTest", "com.example.base.BaseClass", false,
				List.of(new AnnotationModel("java.lang.SuppressWarnings", "\"rawtypes\"")),
				List.of(new TestMethodModel("validate_foo",
						List.of(AnnotationModel.marker("org.junit.jupiter.api.Test"),
								AnnotationModel.marker("org.junit.jupiter.api.Disabled")),
						List.of())),
				List.of("import org.junit.jupiter.api.Test;", "import org.junit.jupiter.api.Disabled;"));

		String rendered = this.renderer.render(model);

		// Imports come solely from the model's import list; the renderer no longer infers
		// them from referenced types (the base class is emitted by its simple name).
		assertThat(rendered).contains("package com.example;")
			.contains("import org.junit.jupiter.api.Test;")
			.contains("import org.junit.jupiter.api.Disabled;")
			.contains("@SuppressWarnings(\"rawtypes\")")
			.contains("public class FooTest extends BaseClass {")
			.contains("@Test")
			.contains("@Disabled")
			.contains("public void validate_foo() throws Exception {");
	}

	@Test
	void renders_without_base_class_when_absent() {
		TestClassModel model = new TestClassModel("com.example", "NoBaseTest", null, false, List.of(),
				List.of(new TestMethodModel("validate_it",
						List.of(AnnotationModel.marker("org.junit.jupiter.api.Test")), List.of())),
				List.of());

		String rendered = this.renderer.render(model);

		assertThat(rendered).contains("public class NoBaseTest {").doesNotContain("extends");
	}

	@Test
	void renders_named_annotation_member() {
		TestClassModel model = new TestClassModel("com.example", "DisabledTest", null, false, List.of(),
				List.of(new TestMethodModel("validate_it",
						List.of(AnnotationModel.member("org.testng.annotations.Test", "enabled", "false")), List.of())),
				List.of());

		String rendered = this.renderer.render(model);

		assertThat(rendered).contains("@Test(enabled = false)");
	}

	@Test
	void emits_body_lines_verbatim_without_adding_semicolons() {
		TestClassModel model = new TestClassModel("com.example", "BodyTest", null, false, List.of(),
				List.of(new TestMethodModel("validate_body",
						List.of(AnnotationModel.marker("org.junit.jupiter.api.Test")),
						List.of("// given:", "int response = call();", "", "assertThat(response).isEqualTo(200);"))),
				List.of());

		String rendered = this.renderer.render(model);

		// verbatim: no stray extra semicolons, comments and lines preserved as-is
		assertThat(rendered).contains("// given:")
			.contains("int response = call();")
			.contains("assertThat(response).isEqualTo(200);")
			.doesNotContain("call();;")
			.doesNotContain("isEqualTo(200);;");
	}

	@Test
	void keeps_dollar_and_braces_in_body_literal() {
		TestClassModel model = new TestClassModel("com.example", "TemplateTest", null, false, List.of(),
				List.of(new TestMethodModel("validate_body",
						List.of(AnnotationModel.marker("org.junit.jupiter.api.Test")),
						List.of("String s = \"${json.unit.core}\" + \"$foo\" + \"{a}\";"))),
				List.of());

		String rendered = this.renderer.render(model);

		assertThat(rendered).contains("String s = \"${json.unit.core}\" + \"$foo\" + \"{a}\";");
	}

	@Test
	void renders_the_model_import_block_grouping_static_imports() {
		TestClassModel model = new TestClassModel("com.example", "MergeTest", null, false, List.of(),
				List.of(new TestMethodModel("validate_it",
						List.of(AnnotationModel.marker("org.junit.jupiter.api.Test")), List.of())),
				List.of("import org.junit.jupiter.api.Test;", "import com.jayway.jsonpath.JsonPath;",
						"import static sh.stubborn.jsonassert.JsonAssertion.assertThatJson;"));

		String rendered = this.renderer.render(model);

		assertThat(rendered).contains("import org.junit.jupiter.api.Test;")
			.contains("import com.jayway.jsonpath.JsonPath;")
			.contains("import static sh.stubborn.jsonassert.JsonAssertion.assertThatJson;");
	}

	@Test
	void renders_class_level_fields_before_methods() {
		TestClassModel model = new TestClassModel("com.example", "MsgTest", null, false, List.of(),
				List.of("@Autowired ContractVerifierMessaging contractVerifierMessaging;",
						"@Autowired ContractVerifierObjectMapper contractVerifierObjectMapper;"),
				List.of(new TestMethodModel("validate_it",
						List.of(AnnotationModel.marker("org.junit.jupiter.api.Test")), List.of())),
				List.of());

		String rendered = this.renderer.render(model);

		assertThat(rendered).contains("\t@Autowired ContractVerifierMessaging contractVerifierMessaging;")
			.contains("\t@Autowired ContractVerifierObjectMapper contractVerifierObjectMapper;");
		// fields are declared before the first method
		assertThat(rendered.indexOf("contractVerifierMessaging")).isLessThan(rendered.indexOf("validate_it"));
	}

	@Test
	void rejects_spock_model() {
		TestClassModel spockModel = new TestClassModel("com.example", "FooSpec", null, true, List.of(), List.of(),
				List.of());

		assertThatIllegalArgumentException().isThrownBy(() -> this.renderer.render(spockModel))
			.withMessageContaining("Spock");
	}

}
