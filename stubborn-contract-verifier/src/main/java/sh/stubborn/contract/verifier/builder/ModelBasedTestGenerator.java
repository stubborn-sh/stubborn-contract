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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.file.ContractMetadata;

/**
 * {@link SingleTestGenerator} that routes through the model + typed-renderer seam of the
 * test-generation migration (see {@code proposals/test-generation-migration.md}).
 *
 * <p>
 * <strong>Phase 1 behaviour:</strong> the emitted source is still produced by the legacy
 * {@link JavaTestGenerator}; this class only <em>additionally</em> exercises the new
 * pipeline — {@link ModelBuilder} builds a {@link TestClassModel} and, for the Java
 * targets, {@link JavaPoetTestRenderer} renders its scaffold — logging the result at
 * debug level. That proves the model → renderer path runs end-to-end against real
 * contracts without changing any output. As later phases move class scaffold, request,
 * and response generation into the model, this generator's output progressively becomes
 * the model's until the final cutover flips it over entirely and the legacy delegate is
 * removed.
 *
 * <p>
 * Selected only when the {@code stubborn.contract.verifier.model-based-generator} system
 * property is {@code true} (see {@code TestGenerator}); the default remains the legacy
 * generator, so this is inert until explicitly opted into.
 *
 * @author Claude Code
 */
public class ModelBasedTestGenerator implements SingleTestGenerator {

	private static final Logger log = LoggerFactory.getLogger(ModelBasedTestGenerator.class);

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
		exerciseModelPipeline(properties, listOfFiles, generatedClassData);
		return this.delegate.buildClass(properties, listOfFiles, includedDirectoryRelativePath, generatedClassData);
	}

	// Builds the model and, for Java targets, renders its scaffold, logging the result.
	// Any failure is swallowed to debug: the model's coverage is partial during the
	// migration, and this preview must never affect the (legacy-produced) output.
	private void exerciseModelPipeline(ContractVerifierConfigProperties properties,
			Collection<ContractMetadata> listOfFiles, GeneratedClassData generatedClassData) {
		if (!log.isDebugEnabled()) {
			return;
		}
		try {
			TestClassModel model = this.modelBuilder.build(properties, listOfFiles, generatedClassData);
			if (properties.getTestFramework() != TestFramework.SPOCK) {
				log.debug("[test-gen migration] JavaPoet scaffold preview for {}:\n{}", generatedClassData.className,
						this.javaRenderer.render(model));
			}
		}
		catch (RuntimeException ex) {
			log.debug(
					"[test-gen migration] model pipeline preview failed for {} (expected while coverage is partial): {}",
					generatedClassData.className, ex.toString());
		}
	}

}
