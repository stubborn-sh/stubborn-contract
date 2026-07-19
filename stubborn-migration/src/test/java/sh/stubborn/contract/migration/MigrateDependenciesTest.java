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

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class MigrateDependenciesTest implements RewriteTest {

	@Override
	public void defaults(RecipeSpec spec) {
		spec.recipeFromResources("sh.stubborn.contract.migration.UpdateMavenDependencies");
	}

	@Test
	void migratesVerifierDependency() {
		rewriteRun(pomXml("""
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
				""", spec -> spec.after(actual -> {
			assertThat(actual).contains("<groupId>sh.stubborn</groupId>")
				.contains("<artifactId>stubborn-verifier</artifactId>")
				.doesNotContain("spring-cloud-contract-verifier");
			return actual;
		})));
	}

	@Test
	void migratesStubRunnerDependency() {
		rewriteRun(pomXml("""
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
				""", spec -> spec.after(actual -> {
			assertThat(actual).contains("<groupId>sh.stubborn</groupId>")
				.contains("<artifactId>stubborn-stub-runner</artifactId>")
				.doesNotContain("spring-cloud-contract-stub-runner");
			return actual;
		})));
	}

	@Test
	void migratesMavenPlugin() {
		rewriteRun(pomXml("""
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
				""", spec -> spec.after(actual -> {
			assertThat(actual).contains("<groupId>sh.stubborn</groupId>")
				.contains("<artifactId>stubborn-contract-maven-plugin</artifactId>")
				.doesNotContain("spring-cloud-contract-maven-plugin");
			return actual;
		})));
	}

	@Test
	void migratesBomInDependencyManagement() {
		rewriteRun(pomXml("""
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
				""", spec -> spec.after(actual -> {
			assertThat(actual).contains("<groupId>sh.stubborn</groupId>")
				.contains("<artifactId>stubborn-contract-dependencies</artifactId>")
				.doesNotContain("spring-cloud-contract-dependencies");
			return actual;
		})));
	}

}
