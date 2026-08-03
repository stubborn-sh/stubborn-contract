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
	 * Test modes whose Java scaffold is fully produced by the model path. CUSTOM declares
	 * a class-level {@code @Autowired HttpVerifier httpVerifier} field, which the model
	 * path captures from the legacy {@code CustomModeFields} visitor (see
	 * {@link LegacyClassFieldExtractor}). The remaining modes (JAX-RS client,
	 * WebTestClient) declare class-level fields — e.g. the {@code WebTarget} for JAX-RS —
	 * through legacy {@code ClassBodyBuilder} field hooks that the model does not yet
	 * capture, so they stay on the legacy generator until a later migration phase ports
	 * them.
	 */
	private static final Set<TestMode> MIGRATED_MODES = EnumSet.of(TestMode.MOCKMVC, TestMode.EXPLICIT, TestMode.CUSTOM,
			TestMode.WEBTESTCLIENT);

	@Override
	public String buildClass(ContractVerifierConfigProperties properties, Collection<ContractMetadata> listOfFiles,
			String includedDirectoryRelativePath, GeneratedClassData generatedClassData) {
		if (!modellable(properties)) {
			// Spock and the not-yet-migrated Java modes (JAX-RS, WebTestClient) cannot be
			// modelled by JavaPoet yet; they stay byte-identical on the legacy generator.
			return this.delegate.buildClass(properties, listOfFiles, includedDirectoryRelativePath, generatedClassData);
		}
		TestClassModel model = this.modelBuilder.build(properties, listOfFiles, includedDirectoryRelativePath,
				generatedClassData);
		return this.javaRenderer.render(model);
	}

	/**
	 * Whether the model + JavaPoet path can fully produce this class. A non-Spock
	 * framework on a migrated {@link TestMode} (MockMvc/EXPLICIT/CUSTOM) qualifies; this
	 * covers the HTTP shapes, messaging classes and CUSTOM mode, whose class-level
	 * collaborators ({@code contractVerifierMessaging},
	 * {@code contractVerifierObjectMapper}, {@code httpVerifier}) are captured as model
	 * fields. The JAX-RS and WebTestClient modes still need their own field handling, so
	 * they stay on the legacy generator.
	 * @param properties the plugin configuration
	 * @return {@code true} if the class should be rendered by the model path
	 */
	private static boolean modellable(ContractVerifierConfigProperties properties) {
		return properties.getTestFramework() != TestFramework.SPOCK
				&& MIGRATED_MODES.contains(properties.getTestMode());
	}

}
