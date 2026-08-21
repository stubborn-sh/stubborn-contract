/*
 * Copyright 2020-present the original author or authors.
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

package sh.stubborn.contract.migration;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.properties.Assertions;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

/**
 * Verifies that the {@code MigrateVerifierProperties} recipe renames the deprecated
 * {@code spring.cloud.contract.verifier.*} property prefix to its canonical
 * {@code stubborn.contract.verifier.*} equivalent in Spring Boot configuration files, and
 * leaves the {@code stubrunner} subset (handled by {@code MigrateStubRunnerProperties})
 * untouched.
 *
 * @author Marcin Grzejszczak
 */
class MigrateVerifierPropertiesTests implements RewriteTest {

	@Override
	public void defaults(RecipeSpec spec) {
		spec.recipeFromResources("sh.stubborn.contract.migration.MigrateVerifierProperties");
	}

	@DocumentExample
	@Test
	void renamesVerifierKeysInProperties() {
		rewriteRun(Assertions.properties("""
				spring.cloud.contract.verifier.assert.size=true
				spring.cloud.contract.verifier.base-package-for-tests=com.example.contracts
				""", """
				stubborn.contract.verifier.assert.size=true
				stubborn.contract.verifier.base-package-for-tests=com.example.contracts
				"""));
	}

	@Test
	void renamesVerifierKeysInYaml() {
		// ChangePropertyKey relocates the key in flattened dotted form, which Spring's
		// relaxed binding reads identically to the nested form.
		rewriteRun(org.openrewrite.yaml.Assertions.yaml("""
				spring:
				  cloud:
				    contract:
				      verifier:
				        assert:
				          size: true
				""", """
				stubborn.contract.verifier:
				  assert:
				    size: true
				"""));
	}

	@Test
	void leavesStubRunnerPropertiesUntouched() {
		// Only the verifier subset is renamed here; stubrunner keys are handled by the
		// separate MigrateStubRunnerProperties recipe.
		rewriteRun(Assertions.properties("""
				spring.cloud.contract.stubrunner.ids=com.example:my-service:+:stubs
				spring.cloud.contract.verifier.assert.size=true
				""", """
				spring.cloud.contract.stubrunner.ids=com.example:my-service:+:stubs
				stubborn.contract.verifier.assert.size=true
				"""));
	}

}
