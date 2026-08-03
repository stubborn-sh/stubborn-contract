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

/**
 * Test-only selector that lets the shared body-builder suites run under either generator
 * so their assertions act as a cross-generator parity harness. A test hands its legacy
 * {@link JavaTestGenerator} (often an anonymous subclass with test-specific field hooks)
 * to {@link #wrap(SingleTestGenerator)}; depending on the
 * {@code stubborn.contract.verifier.model-based-generator} system property the call
 * either returns that legacy generator unchanged or wraps it as the delegate of a
 * {@link ModelBasedTestGenerator}. Wrapping preserves the caller's delegate so the
 * not-yet-migrated shapes (Spock, JAX-RS, WebTestClient, messaging) still route through
 * the caller's customized legacy generator, while the migrated Java HTTP path is rendered
 * by JavaPoet.
 *
 * <p>
 * The module's surefire configuration runs the test phase twice — once with the property
 * {@code false} (legacy) and once {@code true} (model) — so every assertion in these
 * suites is checked against both generators. The default when the property is unset
 * mirrors production: the model generator.
 *
 * @author Marcin Grzejszczak
 */
final class GeneratorUnderTest {

	static final String PROPERTY = "stubborn.contract.verifier.model-based-generator";

	private GeneratorUnderTest() {
	}

	static boolean modelBased() {
		return !"false".equalsIgnoreCase(System.getProperty(PROPERTY, "true"));
	}

	/**
	 * Wraps the caller's legacy generator so it runs under the generator selected by the
	 * {@link #PROPERTY} system property.
	 * @param legacy the legacy generator the test would otherwise call directly (may be
	 * an anonymous subclass carrying test-specific field hooks)
	 * @return the same instance for the legacy pass, or a {@link ModelBasedTestGenerator}
	 * delegating to it for the model pass
	 */
	static SingleTestGenerator wrap(SingleTestGenerator legacy) {
		if (!modelBased()) {
			return legacy;
		}
		return new ModelBasedTestGenerator(legacy, new ModelBuilder(), new JavaPoetTestRenderer());
	}

}
