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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.file.ContractMetadata;
import sh.stubborn.contract.verifier.util.ContractVerifierDslConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ModelBasedTestGenerator}. Phase 1 requires that enabling the
 * model-based generator produces output <em>byte-identical</em> to the legacy generator
 * across the framework matrix — it delegates for output while additionally exercising the
 * new model pipeline. The golden-master harness ({@code TestGenerationGoldenMasterTests})
 * then guards the shape of that output.
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

	@ParameterizedTest
	@EnumSource(value = TestFramework.class, names = "CUSTOM", mode = Mode.EXCLUDE)
	void delegates_output_identically_to_legacy_generator(TestFramework framework) throws IOException {
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(framework);
		properties.setBaseClassForTests("com.example.BaseClass");
		Collection<ContractMetadata> contracts = contracts();
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest",
				"com.example", new File("/tmp").toPath());

		String legacy = new JavaTestGenerator().buildClass(properties, contracts, "some/path", data);
		String modelBased = new ModelBasedTestGenerator().buildClass(properties, contracts, "some/path", data);

		assertThat(modelBased).isEqualTo(legacy);
	}

	@Test
	void uses_the_provided_delegate() throws IOException {
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.JUNIT5);
		Collection<ContractMetadata> contracts = contracts();
		SingleTestGenerator.GeneratedClassData data = new SingleTestGenerator.GeneratedClassData("FooTest",
				"com.example", new File("/tmp").toPath());

		SingleTestGenerator delegate = (props, files, path, classData) -> "DELEGATED";
		ModelBasedTestGenerator generator = new ModelBasedTestGenerator(delegate, new ModelBuilder(),
				new JavaPoetTestRenderer());

		assertThat(generator.buildClass(properties, contracts, "some/path", data)).isEqualTo("DELEGATED");
	}

	private Collection<ContractMetadata> contracts() throws IOException {
		File file = Files.createTempFile(this.tmpDir, "contract", ".groovy").toFile();
		Files.write(file.toPath(), CONTRACT.getBytes());
		return Collections.singletonList(new ContractMetadata(file.toPath(), false, 1, null,
				ContractVerifierDslConverter.convertAsCollection(new File("/"), file)));
	}

}
