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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.Validated;
import org.openrewrite.config.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Gradle migration recipe is wired correctly: it resolves from the
 * declarative {@code migrate-from-scc.yml}, every referenced OpenRewrite recipe and its
 * options validate (a typo in a recipe id or option name fails to activate here), the
 * expected set of coordinate/plugin changes is present, and the top-level composite runs
 * it. The actual Gradle source transformation is performed by the standard, battle-tested
 * {@code org.openrewrite.gradle.*} recipes; exercising those end-to-end requires the
 * Gradle tooling API (a live Gradle model), which is out of scope for this unit test.
 */
class MigrateGradleDependenciesTests {

	private final Environment environment = Environment.builder().scanRuntimeClasspath().build();

	@Test
	void gradleRecipeResolvesAndValidates() {
		Recipe recipe = this.environment.activateRecipes("sh.stubborn.contract.migration.UpdateGradleDependencies");

		assertThat(recipe.getName()).isEqualTo("sh.stubborn.contract.migration.UpdateGradleDependencies");
		assertThat(recipe.validateAll()).allSatisfy((validated) -> assertThat(validated.isValid()).isTrue());
	}

	@Test
	void gradleRecipeHasExpectedCoordinateAndPluginChanges() {
		Recipe recipe = this.environment.activateRecipes("sh.stubborn.contract.migration.UpdateGradleDependencies");

		List<String> recipeTypes = recipe.getRecipeList().stream().map((r) -> r.getClass().getSimpleName()).toList();

		// 8 dependency coordinate swaps (verifier, stub-runner, 2 starters, wiremock,
		// spec-java, spec-groovy, spec-kotlin) + the BOM + the Gradle plugin id.
		assertThat(recipeTypes).filteredOn("ChangeDependency"::equals).hasSize(8);
		assertThat(recipeTypes).filteredOn("ChangeManagedDependency"::equals).hasSize(1);
		assertThat(recipeTypes).filteredOn("ChangePlugin"::equals).hasSize(1);
	}

	@Test
	void topLevelCompositeIncludesGradleMigration() {
		Recipe composite = this.environment
			.activateRecipes("sh.stubborn.contract.migration.MigrateFromSpringCloudContract");

		assertThat(composite.validateAll()).allSatisfy((validated) -> assertThat(validated.isValid()).isTrue());

		List<String> subRecipeNames = composite.getRecipeList().stream().map(Recipe::getName).toList();
		assertThat(subRecipeNames).contains("sh.stubborn.contract.migration.UpdateGradleDependencies");
	}

}
