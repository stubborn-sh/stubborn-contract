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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.yaml.Assertions.yaml;

class MigrateStubRunnerPropertiesTests implements RewriteTest {

	@Override
	public void defaults(RecipeSpec spec) {
		spec.recipeFromResources("sh.stubborn.contract.migration.MigrateStubRunnerProperties");
	}

	@Test
	void renamesStubRunnerKeysInProperties() {
		rewriteRun(properties("""
				spring.cloud.contract.stubrunner.ids=com.example:my-service:+:stubs
				spring.cloud.contract.stubrunner.stubs-mode=LOCAL
				spring.cloud.contract.stubrunner.repository-root=stubs://classpath:/
				""", """
				stubborn.contract.stubrunner.ids=com.example:my-service:+:stubs
				stubborn.contract.stubrunner.stubs-mode=LOCAL
				stubborn.contract.stubrunner.repository-root=stubs://classpath:/
				"""));
	}

	@Test
	void renamesStubRunnerKeysInYaml() {
		// ChangePropertyKey relocates the key in flattened dotted form, which Spring's
		// relaxed binding reads identically to the nested form.
		rewriteRun(yaml("""
				spring:
				  cloud:
				    contract:
				      stubrunner:
				        ids: com.example:my-service:+:stubs
				        stubs-mode: LOCAL
				""", """
				stubborn.contract.stubrunner:
				  ids: com.example:my-service:+:stubs
				  stubs-mode: LOCAL
				"""));
	}

	@Test
	void leavesOtherSpringCloudContractPropertiesUntouched() {
		// Only the stubrunner subset is renamed; verifier/other keys are preserved.
		rewriteRun(properties("""
				spring.cloud.contract.verifier.base-package-for-tests=com.example.contracts
				spring.cloud.contract.stubrunner.ids=com.example:my-service:+:stubs
				""", """
				spring.cloud.contract.verifier.base-package-for-tests=com.example.contracts
				stubborn.contract.stubrunner.ids=com.example:my-service:+:stubs
				"""));
	}

	@Test
	void leavesUnrelatedYamlSubtreeUntouched() {
		rewriteRun(yaml("""
				spring:
				  cloud:
				    contract:
				      verifier:
				        base-package-for-tests: com.example.contracts
				      stubrunner:
				        ids: com.example:my-service:+:stubs
				""", """
				spring:
				  cloud:
				    contract:
				      verifier:
				        base-package-for-tests: com.example.contracts
				stubborn.contract.stubrunner:
				  ids: com.example:my-service:+:stubs
				"""));
	}

}
