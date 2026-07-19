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

class DropJUnit4SupportTest implements RewriteTest {

	@Override
	public void defaults(RecipeSpec spec) {
		spec.recipeFromResources("sh.stubborn.contract.migration.DropJUnit4Support")
			.parser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(false).dependsOn("""
					package sh.stubborn.contract.stubrunner.junit;
					public class StubRunnerRule {}
					""", """
					package sh.stubborn.contract.stubrunner.junit5;
					public class StubRunnerExtension {}
					"""));
	}

	@Test
	void migratesStubRunnerRuleToExtension() {
		rewriteRun(java("""
				import sh.stubborn.contract.stubrunner.junit.StubRunnerRule;

				class MyTest {

					StubRunnerRule stubRunner = new StubRunnerRule();

				}
				""", """
				import sh.stubborn.contract.stubrunner.junit5.StubRunnerExtension;

				class MyTest {

					StubRunnerExtension stubRunner = new StubRunnerExtension();

				}
				"""));
	}

}
