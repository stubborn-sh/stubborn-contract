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
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.config.TestMode;
import sh.stubborn.contract.verifier.file.ContractMetadata;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.util.ContractVerifierDslConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ModelBuilder} — proves the config + contracts → model mapping
 * populates the class scaffold and that the resulting model renders through
 * {@link JavaPoetTestRenderer}.
 *
 * @author Marcin Grzejszczak
 */
class ModelBuilderTests {

	// @formatter:off
	private static final String CONTRACT = "sh.stubborn.contract.spec.Contract.make {\n"
			+ " request {\n"
			+ "  method 'GET'\n"
			+ "  url '/foo'\n"
			+ " }\n"
			+ " response {\n"
			+ "  status OK()\n"
			+ " }\n"
			+ "}";
	// @formatter:on

	// @formatter:off
	private static final String PLAIN_BODY_CONTRACT = "sh.stubborn.contract.spec.Contract.make {\n"
			+ " request {\n"
			+ "  method 'PUT'\n"
			+ "  url '/foo'\n"
			+ "  headers { contentType(applicationJson()) }\n"
			+ "  body([foo: 'bar'])\n"
			+ " }\n"
			+ " response {\n"
			+ "  status OK()\n"
			+ " }\n"
			+ "}";

	private static final String FILE_BODY_CONTRACT = "sh.stubborn.contract.spec.Contract.make {\n"
			+ " request {\n"
			+ "  method 'PUT'\n"
			+ "  url '/foo'\n"
			+ "  headers { contentType(applicationJson()) }\n"
			+ "  body(file('request.json'))\n"
			+ " }\n"
			+ " response {\n"
			+ "  status OK()\n"
			+ " }\n"
			+ "}";
	// @formatter:on

	@TempDir
	Path tmpDir;

	private final ModelBuilder modelBuilder = new ModelBuilder();

	private final RequestModelBuilder requestModelBuilder = new RequestModelBuilder();

	@Test
	void builds_java_class_scaffold_from_contract() throws IOException {
		Collection<ContractMetadata> contracts = contracts(true);
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.JUNIT5);
		properties.setBaseClassForTests("com.example.BazBar");
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest", "test",
				new File("/tmp").toPath());

		TestClassModel model = this.modelBuilder.build(properties, contracts, "com/example", data);

		assertThat(model.packageName()).isEqualTo("test");
		assertThat(model.className()).isEqualTo("FooTest");
		assertThat(model.baseClass()).isEqualTo("com.example.BazBar");
		assertThat(model.spock()).isFalse();
		assertThat(model.classAnnotations()).extracting(AnnotationModel::type)
			.containsExactly("java.lang.SuppressWarnings");
		assertThat(model.methods()).hasSize(1);
		TestMethodModel method = model.methods().get(0);
		assertThat(method.name()).startsWith("validate_");
		assertThat(method.annotations()).extracting(AnnotationModel::type)
			.containsExactly("org.junit.jupiter.api.Test", "org.junit.jupiter.api.Disabled");
		// the body is captured verbatim from the legacy generator
		assertThat(method.bodyLines()).isNotEmpty();
		assertThat(model.importDeclarations()).isNotEmpty();

		// the model renders to a structurally-correct Java class
		String rendered = new JavaPoetTestRenderer().render(model);
		assertThat(rendered).contains("public class FooTest extends BazBar {").contains("public void " + method.name());
	}

	@Test
	void marks_spock_target_and_omits_java_only_scaffold() throws IOException {
		Collection<ContractMetadata> contracts = contracts(false);
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.SPOCK);
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooSpec", "test",
				new File("/tmp").toPath());

		TestClassModel model = this.modelBuilder.build(properties, contracts, "com/example", data);

		assertThat(model.spock()).isTrue();
		assertThat(model.classAnnotations()).isEmpty();
		assertThat(model.methods()).hasSize(1);
		assertThat(model.methods().get(0).annotations()).extracting(AnnotationModel::type)
			.containsExactly("org.junit.jupiter.api.Test");
	}

	@Test
	void testng_uses_testng_annotation() throws IOException {
		Collection<ContractMetadata> contracts = contracts(false);
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.TESTNG);
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest", "test",
				new File("/tmp").toPath());

		TestClassModel model = this.modelBuilder.build(properties, contracts, "com/example", data);

		AnnotationModel testAnnotation = model.methods().get(0).annotations().get(0);
		assertThat(testAnnotation.type()).isEqualTo("org.testng.annotations.Test");
		assertThat(testAnnotation.memberCode()).isNull();
	}

	@Test
	void testng_ignored_uses_enabled_false_member() throws IOException {
		Collection<ContractMetadata> contracts = contracts(true);
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.TESTNG);
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest", "test",
				new File("/tmp").toPath());

		TestClassModel model = this.modelBuilder.build(properties, contracts, "com/example", data);

		AnnotationModel testAnnotation = model.methods().get(0).annotations().get(0);
		assertThat(model.methods().get(0).annotations()).hasSize(1);
		assertThat(testAnnotation.type()).isEqualTo("org.testng.annotations.Test");
		assertThat(testAnnotation.memberName()).isEqualTo("enabled");
		assertThat(testAnnotation.memberCode()).isEqualTo("false");
	}

	@Test
	void request_model_is_built_for_a_plain_body_contract() throws IOException {
		RequestModel model = requestModelFor(PLAIN_BODY_CONTRACT);

		assertThat(model).as("plain-body contract must flow through the structured path").isNotNull();
		// the .body(...) line is the LAST continuation of the given chain (so it, not a
		// header, receives the trailing `;`)
		assertThat(model.given().continuations()).last(org.assertj.core.api.InstanceOfAssertFactories.STRING)
			.startsWith(".body(");
		assertThat(model.given().render()).last(org.assertj.core.api.InstanceOfAssertFactories.STRING).endsWith(");");
	}

	@Test
	void request_model_is_not_built_for_a_file_body_contract() throws IOException {
		RequestModel model = requestModelFor(FILE_BODY_CONTRACT);

		assertThat(model).as("file-based request body is deferred and must fall back to verbatim").isNull();
	}

	private RequestModel requestModelFor(String contractDsl) throws IOException {
		File file = Files.createTempFile(this.tmpDir, "contract", ".groovy").toFile();
		Files.write(file.toPath(), contractDsl.getBytes());
		// The DSL's file('request.json') is resolved (and validated to exist) at parse
		// time
		// via the context classloader; a matching resource lives at the test classpath
		// root.
		Collection<ContractMetadata> contracts = Collections.singletonList(new ContractMetadata(file.toPath(), false, 1,
				null, ContractVerifierDslConverter.convertAsCollection(new File("/"), file)));
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.JUNIT5);
		properties.setTestMode(TestMode.MOCKMVC);
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest", "test",
				file.toPath());
		GeneratedClassMetaData meta = new GeneratedClassMetaData(properties, contracts, "com/example", data);
		SingleContractMetadata scm = meta.toSingleContractMetadata().iterator().next();
		return this.requestModelBuilder.build(scm, TestFramework.JUNIT5, meta, TestMode.MOCKMVC);
	}

	private Collection<ContractMetadata> contracts(boolean ignored) throws IOException {
		File file = Files.createTempFile(this.tmpDir, "contract", ".groovy").toFile();
		Files.write(file.toPath(), CONTRACT.getBytes());
		return Collections.singletonList(new ContractMetadata(file.toPath(), ignored, 1, null,
				ContractVerifierDslConverter.convertAsCollection(new File("/"), file)));
	}

}
