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

import java.util.Collection;

import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.file.ContractMetadata;

/**
 * {@link SingleTestGenerator} that routes through the model + typed-renderer seam of the
 * test-generation migration (see {@code proposals/test-generation-migration.md}).
 *
 * <p>
 * <strong>Phase 2 behaviour:</strong> for the Java targets (JUnit 5, TestNG) the emitted
 * source is produced by the model path — {@link ModelBuilder} builds a
 * {@link TestClassModel} whose scaffold (package, imports, annotations, signatures) is
 * rendered by {@link JavaPoetTestRenderer}, while each method's body is captured verbatim
 * from the legacy {@link JavaTestGenerator} via the escape hatch. Spock stays entirely on
 * the legacy delegate this phase. As later phases move request/response generation into
 * the model, the escape hatch shrinks until the legacy delegate can be removed.
 *
 * <p>
 * Selected only when the {@code stubborn.contract.verifier.model-based-generator} system
 * property is {@code true} (see {@code TestGenerator}); the default remains the legacy
 * generator, so this is inert until explicitly opted into.
 *
 * @author Marcin Grzejszczak
 */
public class ModelBasedTestGenerator implements SingleTestGenerator {

	private final SingleTestGenerator delegate;

	private final ModelBuilder modelBuilder;

	private final JavaPoetTestRenderer javaRenderer;

	public ModelBasedTestGenerator() {
		this(new JavaTestGenerator(), new ModelBuilder(), new JavaPoetTestRenderer());
	}

	ModelBasedTestGenerator(SingleTestGenerator delegate, ModelBuilder modelBuilder,
			JavaPoetTestRenderer javaRenderer) {
		this.delegate = delegate;
		this.modelBuilder = modelBuilder;
		this.javaRenderer = javaRenderer;
	}

	@Override
	public String buildClass(ContractVerifierConfigProperties properties, Collection<ContractMetadata> listOfFiles,
			String includedDirectoryRelativePath, GeneratedClassData generatedClassData) {
		if (properties.getTestFramework() == TestFramework.SPOCK) {
			// Spock cannot be modelled by JavaPoet; stays on the legacy generator.
			return this.delegate.buildClass(properties, listOfFiles, includedDirectoryRelativePath, generatedClassData);
		}
		TestClassModel model = this.modelBuilder.build(properties, listOfFiles, includedDirectoryRelativePath,
				generatedClassData);
		return this.javaRenderer.render(model);
	}

}
