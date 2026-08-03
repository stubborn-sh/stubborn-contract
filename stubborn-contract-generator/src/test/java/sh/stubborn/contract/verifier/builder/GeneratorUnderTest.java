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

/**
 * Test-only helper that runs the shared body-builder suites through the model-based
 * generator. The legacy string-builder scaffold has been removed, so every suite now
 * builds its class via {@link ModelBasedTestGenerator}; byte-parity with the historical
 * legacy output is guarded by the committed golden snapshots (see
 * {@link TestGenerationGoldenMasterTests}).
 *
 * <p>
 * A suite that used to inject extra class-level fields through a
 * {@code JavaTestGenerator#classBodyBuilder} override (e.g. the JAX-RS {@code WebTarget})
 * passes those field declarations to
 * {@link #wrapWithExtraFields(SingleTestGenerator, List)} instead; the model path emits
 * them via {@link ClassScaffoldProducer}.
 *
 * @author Marcin Grzejszczak
 */
final class GeneratorUnderTest {

	private GeneratorUnderTest() {
	}

	/**
	 * Runs the caller's generator through the model path.
	 * @param legacy the method-body engine the model path delegates to
	 * @return a {@link ModelBasedTestGenerator} over the given delegate
	 */
	static SingleTestGenerator wrap(SingleTestGenerator legacy) {
		return new ModelBasedTestGenerator(legacy, new ModelBuilder(), new JavaTestRenderer());
	}

	/**
	 * Runs the caller's generator through the model path, injecting extra class-level
	 * field declarations.
	 * @param legacy the method-body engine the model path delegates to
	 * @param extraFieldLines the additional class-level field declarations to emit (e.g.
	 * {@code WebTarget webTarget} for the JAX-RS cases)
	 * @return a {@link ModelBasedTestGenerator} over the given delegate
	 */
	static SingleTestGenerator wrapWithExtraFields(SingleTestGenerator legacy, List<String> extraFieldLines) {
		return new ModelBasedTestGenerator(legacy, new ModelBuilder(), new JavaTestRenderer(), extraFieldLines);
	}

}
