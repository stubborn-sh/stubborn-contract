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
import java.util.EnumSet;
import java.util.Set;

import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.config.TestMode;
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
 * This is now the default generator (see {@code TestGenerator}); set the
 * {@code stubborn.contract.verifier.model-based-generator} system property to
 * {@code false} to fall back to the legacy generator.
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

	/**
	 * Test modes whose Java scaffold is fully produced by the model path. The other modes
	 * (JAX-RS client, WebTestClient, CUSTOM) declare class-level fields — e.g. the
	 * {@code WebTarget} for JAX-RS — through the legacy {@code ClassBodyBuilder} field
	 * hooks that the JavaPoet scaffold does not model, so they stay on the legacy
	 * generator until a later migration phase ports them.
	 */
	private static final Set<TestMode> MIGRATED_MODES = EnumSet.of(TestMode.MOCKMVC, TestMode.EXPLICIT);

	@Override
	public String buildClass(ContractVerifierConfigProperties properties, Collection<ContractMetadata> listOfFiles,
			String includedDirectoryRelativePath, GeneratedClassData generatedClassData) {
		GeneratedClassMetaData meta = new GeneratedClassMetaData(properties, listOfFiles, includedDirectoryRelativePath,
				generatedClassData);
		if (!modellable(properties, meta)) {
			// Spock, the not-yet-migrated Java modes (JAX-RS, WebTestClient, CUSTOM) and
			// messaging classes cannot be modelled by JavaPoet yet; they stay
			// byte-identical on the legacy generator.
			return this.delegate.buildClass(properties, listOfFiles, includedDirectoryRelativePath, generatedClassData);
		}
		TestClassModel model = this.modelBuilder.build(properties, listOfFiles, includedDirectoryRelativePath,
				generatedClassData);
		return this.javaRenderer.render(model);
	}

	/**
	 * Whether the model + JavaPoet path can fully produce this class. Only the migrated
	 * HTTP surface qualifies: a non-Spock framework, a migrated HTTP {@link TestMode}
	 * (MockMvc/EXPLICIT), and a class whose contracts are all HTTP. Messaging classes
	 * reference messaging collaborators ({@code contractVerifierMessaging},
	 * {@code contractVerifierObjectMapper}) that only the legacy scaffold declares, so
	 * they stay on the legacy generator.
	 * @param properties the plugin configuration
	 * @param meta the class-level metadata for the contracts of this class
	 * @return {@code true} if the class should be rendered by the model path
	 */
	private static boolean modellable(ContractVerifierConfigProperties properties, GeneratedClassMetaData meta) {
		return properties.getTestFramework() != TestFramework.SPOCK && MIGRATED_MODES.contains(properties.getTestMode())
				&& meta.isAnyHttp() && !meta.isAnyMessaging();
	}

}
