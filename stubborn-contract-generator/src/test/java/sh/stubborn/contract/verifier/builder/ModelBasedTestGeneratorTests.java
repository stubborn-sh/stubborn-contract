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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.config.FieldInjection;
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.config.TestMode;
import sh.stubborn.contract.verifier.file.ContractMetadata;
import sh.stubborn.contract.verifier.util.ContractVerifierDslConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ModelBasedTestGenerator}. Phase 2 routes the Java targets (JUnit
 * 5, TestNG) through the model + typed-renderer path, while Spock stays on the legacy
 * delegate. The golden-master harness ({@code TestGenerationGoldenMasterTests}) guards
 * the legacy output and {@code ModelBasedScaffoldParityTests} proves the model path
 * reaches normalized + compile parity with it.
 *
 * @author Marcin Grzejszczak
 */
class ModelBasedTestGeneratorTests {

	// @formatter:off
	private static final String CONTRACT = "sh.stubborn.contract.spec.Contract.make {\n"
			+ " request {\n"
			+ "  method 'PUT'\n"
			+ "  url '/foo'\n"
			+ "  headers {\n"
			+ "    contentType(applicationJson())\n"
			+ "  }\n"
			+ "  body([foo: 'bar'])\n"
			+ " }\n"
			+ " response {\n"
			+ "  status OK()\n"
			+ "  headers {\n"
			+ "    contentType(applicationJson())\n"
			+ "  }\n"
			+ "  body([status: 'ok'])\n"
			+ " }\n"
			+ "}";
	// @formatter:on

	@TempDir
	Path tmpDir;

	@Test
	void spock_output_matches_legacy_byte_for_byte_via_model_path() throws IOException {
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.SPOCK);
		properties.setBaseClassForTests("com.example.BaseClass");
		Collection<ContractMetadata> contracts = contracts();
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest",
				"com.example", new File("/tmp").toPath());

		String legacy = new JavaTestGenerator().buildClass(properties, contracts, "some/path", data);
		String modelBased = new ModelBasedTestGenerator().buildClass(properties, contracts, "some/path", data);

		// Spock now renders through the Handlebars SpockTestRenderer, not the legacy
		// delegate; the output must still be byte-identical to the legacy generator.
		assertThat(modelBased).isEqualTo(legacy);
	}

	@Test
	void java_targets_route_through_the_model_renderer() throws IOException {
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.JUNIT5);
		Collection<ContractMetadata> contracts = contracts();
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest",
				"com.example", new File("/tmp").toPath());

		String modelBased = new ModelBasedTestGenerator().buildClass(properties, contracts, "some/path", data);

		assertThat(modelBased).contains("package com.example;")
			.contains("public class FooTest")
			.contains("public void validate_")
			.contains("@Test");
	}

	@Test
	void extends_base_class_resolved_from_the_package_convention() throws IOException {
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.JUNIT5);
		properties.setPackageWithBaseClasses("com.example.base");
		Collection<ContractMetadata> contracts = contracts();
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest",
				"com.example", new File("/tmp").toPath());

		String legacy = new JavaTestGenerator().buildClass(properties, contracts, "some/path", data);
		String modelBased = new ModelBasedTestGenerator().buildClass(properties, contracts, "some/path", data);

		// packageWithBaseClasses + "some/path" resolves to com.example.base.SomePathBase;
		// both generators must extend it so the generated test inherits the base setup.
		assertThat(legacy).contains("extends SomePathBase");
		assertThat(modelBased).contains("extends SomePathBase");
	}

	@Test
	void extends_base_class_resolved_from_base_class_for_tests() throws IOException {
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.JUNIT5);
		properties.setBaseClassForTests("com.example.MyBaseClass");
		Collection<ContractMetadata> contracts = contracts();
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest",
				"com.example", new File("/tmp").toPath());

		String modelBased = new ModelBasedTestGenerator().buildClass(properties, contracts, "some/path", data);

		assertThat(modelBased).contains("extends MyBaseClass");
	}

	@Test
	void custom_mode_renders_http_verifier_field_via_model_path() throws IOException {
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.JUNIT5);
		properties.setTestMode(TestMode.CUSTOM);
		Collection<ContractMetadata> contracts = contracts();
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest",
				"com.example", new File("/tmp").toPath());

		String legacy = new JavaTestGenerator().buildClass(properties, contracts, "some/path", data);
		String modelBased = new ModelBasedTestGenerator().buildClass(properties, contracts, "some/path", data);

		// CUSTOM mode declares an @Autowired HttpVerifier collaborator; the model path
		// captures it from the legacy CustomModeFields visitor and must reproduce it byte
		// for byte.
		assertThat(legacy).contains("@Autowired HttpVerifier httpVerifier");
		assertThat(modelBased).contains("@Autowired HttpVerifier httpVerifier").isEqualTo(legacy);
	}

	@Test
	void jakarta_field_injection_swaps_the_annotation_and_import() throws IOException {
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.JUNIT5);
		properties.setTestMode(TestMode.CUSTOM);
		properties.setFieldInjection(FieldInjection.JAKARTA);
		Collection<ContractMetadata> contracts = contracts();
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest",
				"com.example", new File("/tmp").toPath());

		String modelBased = new ModelBasedTestGenerator().buildClass(properties, contracts, "some/path", data);

		assertThat(modelBased).contains("@Inject HttpVerifier httpVerifier")
			.contains("import jakarta.inject.Inject;")
			.doesNotContain("@Autowired")
			.doesNotContain("org.springframework.beans.factory.annotation.Autowired");
	}

	@Test
	void none_field_injection_drops_the_annotation_and_import() throws IOException {
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.JUNIT5);
		properties.setTestMode(TestMode.CUSTOM);
		properties.setFieldInjection(FieldInjection.NONE);
		Collection<ContractMetadata> contracts = contracts();
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest",
				"com.example", new File("/tmp").toPath());

		String modelBased = new ModelBasedTestGenerator().buildClass(properties, contracts, "some/path", data);

		// The collaborator is declared bare; the base class is expected to provide it.
		assertThat(modelBased).contains("HttpVerifier httpVerifier;")
			.doesNotContain("@Autowired")
			.doesNotContain("@Inject")
			.doesNotContain("org.springframework.beans.factory.annotation.Autowired")
			.doesNotContain("jakarta.inject.Inject");
	}

	@Test
	void web_test_client_mode_matches_legacy_via_model_path() throws IOException {
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.JUNIT5);
		properties.setTestMode(TestMode.WEBTESTCLIENT);
		Collection<ContractMetadata> contracts = contracts();
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest",
				"com.example", new File("/tmp").toPath());

		String legacy = new JavaTestGenerator().buildClass(properties, contracts, "some/path", data);
		String modelBased = new ModelBasedTestGenerator().buildClass(properties, contracts, "some/path", data);

		// WebTestClient declares no class-level fields; the model path must reproduce the
		// legacy output byte for byte.
		assertThat(modelBased).isEqualTo(legacy);
	}

	@Test
	void jaxrs_client_field_injected_on_the_delegate_is_captured_by_the_model_path() throws IOException {
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.JUNIT5);
		properties.setTestMode(TestMode.JAXRSCLIENT);
		Collection<ContractMetadata> contracts = contracts();
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest",
				"com.example", new File("/tmp").toPath());

		// JAX-RS gets its WebTarget from the base class in production; the tests supply
		// one
		// through a classBodyBuilder override on the delegate instead. The model path
		// must
		// reproduce whatever the delegate emits, including that injected field.
		JavaTestGenerator delegate = new JavaTestGenerator() {
			@Override
			ClassBodyBuilder classBodyBuilder(BlockBuilder builder, GeneratedClassMetaData metaData,
					SingleMethodBuilder methodBuilder) {
				return super.classBodyBuilder(builder, metaData, methodBuilder).field(new Field() {
					@Override
					public boolean accept() {
						return metaData.configProperties.getTestMode() == TestMode.JAXRSCLIENT;
					}

					@Override
					public Field call() {
						builder.addLine("WebTarget webTarget");
						return this;
					}
				});
			}
		};

		String legacy = delegate.buildClass(properties, contracts, "some/path", data);
		String modelBased = new ModelBasedTestGenerator(delegate, new ModelBuilder(), new JavaPoetTestRenderer())
			.buildClass(properties, contracts, "some/path", data);

		assertThat(legacy).contains("WebTarget webTarget;");
		assertThat(modelBased).contains("WebTarget webTarget;").isEqualTo(legacy);
	}

	@Test
	void spock_renders_groovy_through_the_model_path() throws IOException {
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.SPOCK);
		Collection<ContractMetadata> contracts = contracts();
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest",
				"com.example", new File("/tmp").toPath());

		String modelBased = new ModelBasedTestGenerator().buildClass(properties, contracts, "some/path", data);

		// Groovy/Spock scaffold: "Spec" suffix, extends Specification, def feature
		// methods,
		// no Java-only tokens.
		assertThat(modelBased).contains("class FooTestSpec extends Specification {")
			.contains("def validate_")
			.doesNotContain("public void")
			.doesNotContain("@Test");
	}

	private Collection<ContractMetadata> contracts() throws IOException {
		File file = Files.createTempFile(this.tmpDir, "contract", ".groovy").toFile();
		Files.write(file.toPath(), CONTRACT.getBytes());
		return Collections.singletonList(new ContractMetadata(file.toPath(), false, 1, null,
				ContractVerifierDslConverter.convertAsCollection(new File("/"), file)));
	}

}
