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
 * Unit tests for {@link JavaPoetTestRenderer} — proves the JavaPoet dependency is wired
 * in and that a {@link TestClassModel} renders to structurally-correct Java.
 *
 * @author Claude Code
 */
class JavaPoetTestRendererTests {

	private final JavaPoetTestRenderer renderer = new JavaPoetTestRenderer();

	@Test
	void renders_class_scaffold_from_model() {
		TestClassModel model = new TestClassModel("com.example", "FooTest", "com.example.base.BaseClass", false,
				List.of(new AnnotationModel("java.lang.SuppressWarnings", "\"rawtypes\"")),
				List.of(new TestMethodModel("validate_foo",
						List.of(AnnotationModel.marker("org.junit.jupiter.api.Test"),
								AnnotationModel.marker("org.junit.jupiter.api.Disabled")),
						List.of())));

		String rendered = this.renderer.render(model);

		assertThat(rendered).contains("package com.example;")
			.contains("import com.example.base.BaseClass;")
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
						List.of(AnnotationModel.marker("org.junit.jupiter.api.Test")), List.of())));

		String rendered = this.renderer.render(model);

		assertThat(rendered).contains("public class NoBaseTest {").doesNotContain("extends");
	}

	@Test
	void renders_method_body_statements() {
		TestClassModel model = new TestClassModel("com.example", "BodyTest", null, false, List.of(),
				List.of(new TestMethodModel("validate_body",
						List.of(AnnotationModel.marker("org.junit.jupiter.api.Test")),
						List.of("int response = call()", "assertThat(response).isEqualTo(200)"))));

		String rendered = this.renderer.render(model);

		assertThat(rendered).contains("int response = call();").contains("assertThat(response).isEqualTo(200);");
	}

	@Test
	void rejects_spock_model() {
		TestClassModel spockModel = new TestClassModel("com.example", "FooSpec", null, true, List.of(), List.of());

		assertThatIllegalArgumentException().isThrownBy(() -> this.renderer.render(spockModel))
			.withMessageContaining("Spock");
	}

}
