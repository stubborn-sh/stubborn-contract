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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.RecipeSpec;

import static org.openrewrite.java.Assertions.java;

class MigrateJavaPackagesTest implements RewriteTest {

	@Override
	public void defaults(RecipeSpec spec) {
		spec.recipeFromResources("sh.stubborn.contract.migration.RenameJavaPackages")
			.parser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(false).dependsOn("""
					package org.springframework.cloud.contract.stubrunner.spring;
					import java.lang.annotation.*;
					@Retention(RetentionPolicy.RUNTIME)
					@Target(ElementType.TYPE)
					public @interface AutoConfigureStubRunner {
						StubsMode stubsMode() default StubsMode.CLASSPATH;
					}
					""", """
					package org.springframework.cloud.contract.stubrunner;
					public enum StubsMode { CLASSPATH, REMOTE, LOCAL }
					""", """
					package org.springframework.cloud.contract.verifier.config;
					public enum TestFramework { JUNIT5, SPOCK, JUNIT }
					"""));
	}

	@Test
	void renamesStubRunnerImport() {
		rewriteRun(java("""
				import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
				import org.springframework.cloud.contract.stubrunner.StubsMode;

				@AutoConfigureStubRunner(stubsMode = StubsMode.CLASSPATH)
				class MyTest {
				}
				""", """
				import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
				import sh.stubborn.contract.stubrunner.StubsMode;

				@AutoConfigureStubRunner(stubsMode = StubsMode.CLASSPATH)
				class MyTest {
				}
				"""));
	}

	@Test
	void renamesVerifierImport() {
		rewriteRun(java("""
				import org.springframework.cloud.contract.verifier.config.TestFramework;

				class MyTest {
					TestFramework framework = TestFramework.JUNIT5;
				}
				""", """
				import sh.stubborn.contract.verifier.config.TestFramework;

				class MyTest {
					TestFramework framework = TestFramework.JUNIT5;
				}
				"""));
	}

}
