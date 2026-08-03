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
import java.util.List;

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
 * Unit tests for {@link ModelBasedTestGenerator}. Every target (JUnit 5, TestNG, Spock)
 * is rendered through the model + typed-renderer path. Byte-parity with the historical
 * legacy output is guarded by the golden-master harness
 * ({@code TestGenerationGoldenMasterTests}); these tests assert the structural contract
 * of the model output.
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

		String modelBased = new ModelBasedTestGenerator().buildClass(properties, contracts, "some/path", data);

		// Spock renders through the Handlebars SpockTestRenderer; byte-parity with the
		// historical legacy output is guarded by the committed golden snapshots (see
		// TestGenerationGoldenMasterTests).
		assertThat(modelBased).contains("class FooTestSpec extends BaseClass").contains("def validate_");
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
	void mockmvc_structured_request_matches_legacy_byte_for_byte() throws IOException {
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.JUNIT5);
		Collection<ContractMetadata> contracts = contracts();
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest",
				"com.example", new File("/tmp").toPath());

		String modelBased = new ModelBasedTestGenerator().buildClass(properties, contracts, "some/path", data);

		// This contract (headers + body, no matchers) is eligible for the structured
		// request/response path; its byte-for-byte indentation is guarded by the
		// committed
		// golden snapshots (see TestGenerationGoldenMasterTests).
		assertThat(modelBased).contains("public class FooTest")
			.contains("assertThat(response.statusCode()).isEqualTo(200);");
	}

	@Test
	void extends_base_class_resolved_from_the_package_convention() throws IOException {
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.JUNIT5);
		properties.setPackageWithBaseClasses("com.example.base");
		Collection<ContractMetadata> contracts = contracts();
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest",
				"com.example", new File("/tmp").toPath());

		String modelBased = new ModelBasedTestGenerator().buildClass(properties, contracts, "some/path", data);

		// packageWithBaseClasses + "some/path" resolves to com.example.base.SomePathBase;
		// the model path must extend it so the generated test inherits the base setup.
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

		String modelBased = new ModelBasedTestGenerator().buildClass(properties, contracts, "some/path", data);

		// CUSTOM is not a Spring-based mode, so the AUTO default injects the HttpVerifier
		// collaborator with jakarta.inject's @Inject; the model path emits it from the
		// CustomModeFields visitor via ClassScaffoldProducer.
		assertThat(modelBased).contains("@Inject HttpVerifier httpVerifier");
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

		String modelBased = new ModelBasedTestGenerator().buildClass(properties, contracts, "some/path", data);

		// WebTestClient declares no class-level fields; byte-parity with the historical
		// legacy output is guarded by the committed golden snapshots.
		assertThat(modelBased).contains("public class FooTest").contains("@Test").doesNotContain("WebTarget");
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
		// through the model-side extra-field hook instead. The model path emits the
		// injected field via ClassScaffoldProducer.
		String modelBased = new ModelBasedTestGenerator(new JavaTestGenerator(), new ModelBuilder(),
				new JavaPoetTestRenderer(), List.of("WebTarget webTarget"))
			.buildClass(properties, contracts, "some/path", data);

		assertThat(modelBased).contains("WebTarget webTarget;");
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
