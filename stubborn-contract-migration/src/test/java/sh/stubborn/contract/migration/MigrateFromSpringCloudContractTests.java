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

/**
 * End-to-end migration sample: runs the whole {@code MigrateFromSpringCloudContract}
 * composite over a realistic Spring Cloud Contract project — a Maven build with SCC
 * dependencies, the SCC Maven plugin and BOM, plus an {@code application.properties} that
 * mixes the stub-runner and verifier property families — and asserts every SCC coordinate
 * and property is rewritten to its Stubborn Contract equivalent in a single pass. This is
 * the migration counterpart to the {@code sample-compatibility} interoperability samples.
 *
 * @author Marcin Grzejszczak
 */
class MigrateFromSpringCloudContractTests implements RewriteTest {

	@Override
	public void defaults(RecipeSpec spec) {
		spec.recipeFromResources("sh.stubborn.contract.migration.MigrateFromSpringCloudContract");
	}

	@Test
	void migratesAWholeSpringCloudContractProject() {
		rewriteRun(Assertions.pomXml("""
				<project>
					<groupId>com.example</groupId>
					<artifactId>beer-producer</artifactId>
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
					<dependencies>
						<dependency>
							<groupId>org.springframework.cloud</groupId>
							<artifactId>spring-cloud-starter-contract-verifier</artifactId>
							<scope>test</scope>
						</dependency>
						<dependency>
							<groupId>org.springframework.cloud</groupId>
							<artifactId>spring-cloud-starter-contract-stub-runner</artifactId>
							<scope>test</scope>
						</dependency>
					</dependencies>
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
			assertThat(actual).contains("<artifactId>stubborn-contract-dependencies</artifactId>")
				.contains("<artifactId>stubborn-contract-starter-verifier</artifactId>")
				.contains("<artifactId>stubborn-contract-starter-stub-runner</artifactId>")
				.contains("<artifactId>stubborn-contract-maven-plugin</artifactId>")
				.contains("<groupId>sh.stubborn</groupId>")
				.doesNotContain("spring-cloud-contract");
			return actual;
		})), org.openrewrite.properties.Assertions.properties("""
				spring.cloud.contract.stubrunner.ids=com.example:beer-producer:+:stubs
				spring.cloud.contract.stubrunner.stubs-mode=LOCAL
				spring.cloud.contract.verifier.assert.size=true
				""", """
				stubborn.contract.stubrunner.ids=com.example:beer-producer:+:stubs
				stubborn.contract.stubrunner.stubs-mode=LOCAL
				stubborn.contract.verifier.assert.size=true
				"""));
	}

}
