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
import java.util.List;
import java.util.Set;

import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
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
 * rendered by {@link JavaTestRenderer}, while each method's body is captured verbatim
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
 * @since 1.0.0
 */
public class ModelBasedTestGenerator implements SingleTestGenerator {

	private final SingleTestGenerator delegate;

	private final ModelBuilder modelBuilder;

	private final JavaTestRenderer javaRenderer;

	private final SpockTestRenderer spockRenderer;

	private final List<String> extraFieldLines;

	public ModelBasedTestGenerator() {
		this(new JavaTestGenerator(), new ModelBuilder(), new JavaTestRenderer(), new SpockTestRenderer(), List.of());
	}

	ModelBasedTestGenerator(SingleTestGenerator delegate, ModelBuilder modelBuilder, JavaTestRenderer javaRenderer) {
		this(delegate, modelBuilder, javaRenderer, new SpockTestRenderer(), List.of());
	}

	ModelBasedTestGenerator(SingleTestGenerator delegate, ModelBuilder modelBuilder, JavaTestRenderer javaRenderer,
			List<String> extraFieldLines) {
		this(delegate, modelBuilder, javaRenderer, new SpockTestRenderer(), extraFieldLines);
	}

	ModelBasedTestGenerator(SingleTestGenerator delegate, ModelBuilder modelBuilder, JavaTestRenderer javaRenderer,
			SpockTestRenderer spockRenderer, List<String> extraFieldLines) {
		this.delegate = delegate;
		this.modelBuilder = modelBuilder;
		this.javaRenderer = javaRenderer;
		this.spockRenderer = spockRenderer;
		this.extraFieldLines = extraFieldLines;
	}

	/**
	 * Test modes whose scaffold is fully produced by the model path — every mode. The
	 * class-level fields the non-MockMvc shapes declare (the messaging collaborators, the
	 * CUSTOM-mode {@code httpVerifier}, the JAX-RS {@code WebTarget}) are produced from
	 * the same {@link Field} visitors by {@link ClassScaffoldProducer}, so no mode needs
	 * bespoke field handling. Both the Java targets (via {@link JavaTestRenderer}) and
	 * the Groovy/Spock target (via {@link SpockTestRenderer}) are rendered from the
	 * model.
	 */
	private static final Set<TestMode> MIGRATED_MODES = EnumSet.of(TestMode.MOCKMVC, TestMode.EXPLICIT, TestMode.CUSTOM,
			TestMode.WEBTESTCLIENT, TestMode.JAXRSCLIENT);

	@Override
	public String buildClass(ContractVerifierConfigProperties properties, Collection<ContractMetadata> listOfFiles,
			String includedDirectoryRelativePath, GeneratedClassData generatedClassData) {
		if (!modellable(properties)) {
			return this.delegate.buildClass(properties, listOfFiles, includedDirectoryRelativePath, generatedClassData);
		}
		TestClassModel model = this.modelBuilder.build(properties, listOfFiles, includedDirectoryRelativePath,
				generatedClassData, this.extraFieldLines);
		// Both targets render through Handlebars templates: the Java targets via
		// JavaTestRenderer, the Groovy/Spock target via SpockTestRenderer.
		return model.spock() ? this.spockRenderer.render(model) : this.javaRenderer.render(model);
	}

	/**
	 * Whether the model path can fully produce this class. Every migrated
	 * {@link TestMode} qualifies, for both the Java targets and Spock — the class-level
	 * collaborators ({@code contractVerifierMessaging},
	 * {@code contractVerifierObjectMapper}, {@code httpVerifier}, {@code webTarget}) are
	 * captured as model fields from the legacy output, and the framework selects the
	 * renderer.
	 * @param properties the plugin configuration
	 * @return {@code true} if the class should be rendered by the model path
	 */
	private static boolean modellable(ContractVerifierConfigProperties properties) {
		return MIGRATED_MODES.contains(properties.getTestMode());
	}

}
