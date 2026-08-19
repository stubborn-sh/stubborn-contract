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
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;
import org.openrewrite.gradle.ChangeManagedDependency;
import org.openrewrite.gradle.plugins.ChangePlugin;
import org.openrewrite.java.dependencies.ChangeDependency;
import org.openrewrite.maven.ChangeManagedDependencyGroupIdAndArtifactId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Gradle half of {@code UpdateDependencies} is wired correctly: it
 * resolves from the declarative {@code migrate-from-scc.yml}, every referenced
 * OpenRewrite recipe and its options validate (a typo in a recipe id or option name fails
 * to activate here), the expected set of coordinate/plugin changes is present, and the
 * top-level composite runs it. The actual Gradle source transformation is performed by
 * the standard, battle-tested {@code org.openrewrite.gradle.*} and
 * {@code org.openrewrite.java.dependencies.*} recipes; exercising those end-to-end
 * requires the Gradle tooling API (a live Gradle model), which is out of scope for this
 * unit test.
 */
class MigrateGradleDependenciesTests {

	private final Environment environment = Environment.builder().scanRuntimeClasspath().build();

	@Test
	void gradleRecipeHasExpectedCoordinateAndPluginChanges() {
		Recipe recipe = this.environment.activateRecipes("sh.stubborn.contract.migration.UpdateDependencies");

		List<String> recipeTypes = flatten(recipe).map((r) -> r.getClass().getSimpleName()).toList();

		// 13 build-tool agnostic coordinate swaps (verifier, stub-runner, 3 starters,
		// wiremock, converters, spec, spec-java, spec-groovy, spec-kotlin, jsonassert,
		// xmlassert) + both BOM variants + the Gradle plugin id.
		assertThat(recipeTypes).filteredOn("ChangeDependency"::equals).hasSize(13);
		assertThat(recipeTypes).filteredOn("ChangeManagedDependency"::equals).hasSize(1);
		assertThat(recipeTypes).filteredOn("ChangeManagedDependencyGroupIdAndArtifactId"::equals).hasSize(1);
		assertThat(recipeTypes).filteredOn("ChangePlugin"::equals).hasSize(1);
	}

	@Test
	void gradleRecipeRepinsEveryCoordinateToAPublishedRelease() {
		Recipe recipe = this.environment.activateRecipes("sh.stubborn.contract.migration.UpdateDependencies");

		// An unset newVersion carries the Spring Cloud Contract version over onto the
		// sh.stubborn coordinate, producing a GAV that was never published.
		assertThat(flatten(recipe)).allSatisfy((subRecipe) -> {
			if (subRecipe instanceof ChangeDependency changeDependency) {
				assertThat(changeDependency.getNewVersion()).isEqualTo("latest.release");
			}
			else if (subRecipe instanceof ChangeManagedDependency changeManagedDependency) {
				assertThat(changeManagedDependency.getNewVersion()).isEqualTo("latest.release");
			}
			else if (subRecipe instanceof ChangeManagedDependencyGroupIdAndArtifactId changeManagedGav) {
				assertThat(changeManagedGav.getNewVersion()).isEqualTo("latest.release");
			}
			else if (subRecipe instanceof ChangePlugin changePlugin) {
				assertThat(changePlugin.getNewVersion()).isEqualTo("latest.release");
			}
		});
	}

	private static Stream<Recipe> flatten(Recipe recipe) {
		return Stream.concat(Stream.of(recipe), recipe.getRecipeList().stream().flatMap((r) -> flatten(r)));
	}

	@Test
	void topLevelCompositeWiresEverySubRecipe() {
		Recipe composite = this.environment
			.activateRecipes("sh.stubborn.contract.migration.MigrateFromSpringCloudContract");

		assertThat(composite.validateAll()).allSatisfy((validated) -> assertThat(validated.isValid()).isTrue());

		// Lock the exact composition so dropping or renaming any migration step
		// (coordinates, Java packages, both property families, JUnit 4) fails here.
		List<String> subRecipeNames = composite.getRecipeList().stream().map(Recipe::getName).toList();
		assertThat(subRecipeNames).containsExactlyInAnyOrder("sh.stubborn.contract.migration.UpdateDependencies",
				"sh.stubborn.contract.migration.RenameJavaPackages",
				"sh.stubborn.contract.migration.MigrateStubRunnerProperties",
				"sh.stubborn.contract.migration.MigrateVerifierProperties",
				"sh.stubborn.contract.migration.DropJUnit4Support");
	}

}
