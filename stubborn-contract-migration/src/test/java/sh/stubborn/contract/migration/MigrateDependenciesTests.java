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
import org.openrewrite.maven.Assertions;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;

class MigrateDependenciesTests implements RewriteTest {

	@Override
	public void defaults(RecipeSpec spec) {
		spec.recipeFromResources("sh.stubborn.contract.migration.UpdateDependencies");
	}

	@Test
	void everyRecipeInTheYamlConfigures() {
		assertRecipesConfigure();
	}

	@Test
	void migratesVerifierDependency() {
		rewriteRun(Assertions.pomXml("""
				<project>
					<groupId>com.example</groupId>
					<artifactId>my-app</artifactId>
					<version>1.0.0</version>
					<dependencies>
						<dependency>
							<groupId>org.springframework.cloud</groupId>
							<artifactId>spring-cloud-contract-verifier</artifactId>
							<version>4.1.0</version>
							<scope>test</scope>
						</dependency>
					</dependencies>
				</project>
				""", (spec) -> spec.after((actual) -> {
			assertThat(actual).contains("<groupId>sh.stubborn</groupId>")
				.contains("<artifactId>stubborn-contract-verifier</artifactId>")
				.doesNotContain("spring-cloud-contract-verifier");
			return actual;
		})));
	}

	@Test
	void migratesStubRunnerDependency() {
		rewriteRun(Assertions.pomXml("""
				<project>
					<groupId>com.example</groupId>
					<artifactId>my-app</artifactId>
					<version>1.0.0</version>
					<dependencies>
						<dependency>
							<groupId>org.springframework.cloud</groupId>
							<artifactId>spring-cloud-contract-stub-runner</artifactId>
							<version>4.1.0</version>
							<scope>test</scope>
						</dependency>
					</dependencies>
				</project>
				""", (spec) -> spec.after((actual) -> {
			assertThat(actual).contains("<groupId>sh.stubborn</groupId>")
				.contains("<artifactId>stubborn-contract-stub-runner</artifactId>")
				.doesNotContain("spring-cloud-contract-stub-runner");
			return actual;
		})));
	}

	@Test
	void migratesConvertersDependency() {
		rewriteRun(Assertions.pomXml("""
				<project>
					<groupId>com.example</groupId>
					<artifactId>my-app</artifactId>
					<version>1.0.0</version>
					<dependencies>
						<dependency>
							<groupId>org.springframework.cloud</groupId>
							<artifactId>spring-cloud-contract-converters</artifactId>
							<version>4.1.0</version>
							<scope>test</scope>
						</dependency>
					</dependencies>
				</project>
				""",
				(spec) -> spec.after((actual) -> assertThat(actual).contains("<groupId>sh.stubborn</groupId>")
					.contains("<artifactId>stubborn-contract-converters</artifactId>")
					.doesNotContain("spring-cloud-contract-converters")
					.actual())));
	}

	@Test
	void migratesSpecJettyStarterAndAssertionDependencies() {
		rewriteRun(
				Assertions.pomXml("""
						<project>
							<groupId>com.example</groupId>
							<artifactId>my-app</artifactId>
							<version>1.0.0</version>
							<dependencies>
								<dependency>
									<groupId>org.springframework.cloud</groupId>
									<artifactId>spring-cloud-contract-spec</artifactId>
									<version>4.1.0</version>
									<scope>test</scope>
								</dependency>
								<dependency>
									<groupId>org.springframework.cloud</groupId>
									<artifactId>spring-cloud-starter-contract-stub-runner-jetty</artifactId>
									<version>4.1.0</version>
									<scope>test</scope>
								</dependency>
								<dependency>
									<groupId>com.toomuchcoding.jsonassert</groupId>
									<artifactId>jsonassert</artifactId>
									<version>0.8.0</version>
									<scope>test</scope>
								</dependency>
								<dependency>
									<groupId>com.toomuchcoding.xmlassert</groupId>
									<artifactId>xmlassert</artifactId>
									<version>0.0.2</version>
									<scope>test</scope>
								</dependency>
							</dependencies>
						</project>
						""",
						(spec) -> spec.after((actual) -> assertThat(actual)
							.contains("<artifactId>stubborn-contract-spec</artifactId>")
							.contains("<artifactId>stubborn-contract-starter-stub-runner-jetty</artifactId>")
							.contains("<artifactId>stubborn-contract-jsonassert</artifactId>")
							.contains("<artifactId>stubborn-contract-xmlassert</artifactId>")
							.doesNotContain("org.springframework.cloud")
							.doesNotContain("com.toomuchcoding")
							.actual())));
	}

	@Test
	void migratesMavenPlugin() {
		// The plugin repin drops the stale version in one cycle and re-resolves it in
		// the next, so this recipe legitimately makes changes across two cycles.
		rewriteRun((spec) -> spec.expectedCyclesThatMakeChanges(2), Assertions.pomXml("""
				<project>
					<groupId>com.example</groupId>
					<artifactId>my-app</artifactId>
					<version>1.0.0</version>
					<build>
						<plugins>
							<plugin>
								<groupId>org.springframework.cloud</groupId>
								<artifactId>spring-cloud-contract-maven-plugin</artifactId>
								<version>4.1.0</version>
							</plugin>
						</plugins>
					</build>
				</project>
				""", (spec) -> spec.after((actual) -> {
			assertThat(actual).contains("<groupId>sh.stubborn</groupId>")
				.contains("<artifactId>stubborn-contract-maven-plugin</artifactId>")
				.doesNotContain("spring-cloud-contract-maven-plugin");
			return actual;
		})));
	}

	@Test
	void migratesBomInDependencyManagement() {
		rewriteRun(Assertions.pomXml("""
				<project>
					<groupId>com.example</groupId>
					<artifactId>my-app</artifactId>
					<version>1.0.0</version>
					<dependencyManagement>
						<dependencies>
							<dependency>
								<groupId>org.springframework.cloud</groupId>
								<artifactId>spring-cloud-contract-dependencies</artifactId>
								<version>4.1.0</version>
								<type>pom</type>
								<scope>import</scope>
							</dependency>
						</dependencies>
					</dependencyManagement>
				</project>
				""", (spec) -> spec.after((actual) -> {
			assertThat(actual).contains("<groupId>sh.stubborn</groupId>")
				.contains("<artifactId>stubborn-contract-dependencies</artifactId>")
				.doesNotContain("spring-cloud-contract-dependencies");
			return actual;
		})));
	}

	@Test
	void repinsTheMavenPluginWithoutDisturbingOtherPlugins() {
		// The plugin repin drops the stale version in one cycle and re-resolves it in
		// the next, so this recipe legitimately makes changes across two cycles.
		rewriteRun((spec) -> spec.expectedCyclesThatMakeChanges(2), Assertions.pomXml("""
				<project>
					<groupId>com.example</groupId>
					<artifactId>my-app</artifactId>
					<version>1.0.0</version>
					<build>
						<plugins>
							<plugin>
								<groupId>org.apache.maven.plugins</groupId>
								<artifactId>maven-surefire-plugin</artifactId>
								<version>3.5.6</version>
							</plugin>
							<plugin>
								<groupId>org.springframework.cloud</groupId>
								<artifactId>spring-cloud-contract-maven-plugin</artifactId>
								<version>4.1.4</version>
							</plugin>
						</plugins>
					</build>
				</project>
				""",
				// The stale plugin version is dropped and re-resolved, so the XPath doing
				// the dropping has to match this plugin only.
				(spec) -> spec.after((actual) -> assertThat(actual)
					.containsPattern(
							"(?s)<artifactId>maven-surefire-plugin</artifactId>\\s*<version>3\\.5\\.6</version>")
					.containsPattern(
							"(?s)<artifactId>stubborn-contract-maven-plugin</artifactId>\\s*<version>\\d+\\.\\d+\\.\\d+</version>")
					.doesNotContain("4.1.4")
					.doesNotContain("~~")
					.actual())));
	}

	@Test
	void repinsAnExplicitlyPinnedBomVersionToAPublishedRelease() {
		rewriteRun(Assertions.pomXml("""
				<project>
					<groupId>com.example</groupId>
					<artifactId>my-app</artifactId>
					<version>1.0.0</version>
					<dependencyManagement>
						<dependencies>
							<dependency>
								<groupId>org.springframework.cloud</groupId>
								<artifactId>spring-cloud-contract-dependencies</artifactId>
								<version>4.1.4</version>
								<type>pom</type>
								<scope>import</scope>
							</dependency>
						</dependencies>
					</dependencyManagement>
				</project>
				""",
				// sh.stubborn:stubborn-contract-dependencies:4.1.4 was never published;
				// carrying the Spring Cloud Contract version over leaves the pom
				// unresolvable, and OpenRewrite marks the failure up with `~~`.
				(spec) -> spec.after((actual) -> assertThat(actual).doesNotContain("4.1.4")
					.doesNotContain("~~")
					.containsPattern(
							"(?s)<artifactId>stubborn-contract-dependencies</artifactId>\\s*<version>\\d+\\.\\d+\\.\\d+</version>")
					.actual())));
	}

	@Test
	void repinsAnExplicitlyPinnedDependencyAndPluginVersionToAPublishedRelease() {
		// The plugin repin drops the stale version in one cycle and re-resolves it in
		// the next, so this recipe legitimately makes changes across two cycles.
		rewriteRun((spec) -> spec.expectedCyclesThatMakeChanges(2), Assertions.pomXml("""
				<project>
					<groupId>com.example</groupId>
					<artifactId>my-app</artifactId>
					<version>1.0.0</version>
					<dependencies>
						<dependency>
							<groupId>org.springframework.cloud</groupId>
							<artifactId>spring-cloud-contract-verifier</artifactId>
							<version>4.1.4</version>
							<scope>test</scope>
						</dependency>
					</dependencies>
					<build>
						<plugins>
							<plugin>
								<groupId>org.springframework.cloud</groupId>
								<artifactId>spring-cloud-contract-maven-plugin</artifactId>
								<version>4.1.4</version>
							</plugin>
						</plugins>
					</build>
				</project>
				""", (spec) -> spec.after((actual) -> assertThat(actual).doesNotContain("4.1.4")
			.doesNotContain("~~")
			.containsPattern(
					"(?s)<artifactId>stubborn-contract-verifier</artifactId>\\s*<version>\\d+\\.\\d+\\.\\d+</version>")
			.containsPattern(
					"(?s)<artifactId>stubborn-contract-maven-plugin</artifactId>\\s*<version>\\d+\\.\\d+\\.\\d+</version>")
			.actual())));
	}

}
