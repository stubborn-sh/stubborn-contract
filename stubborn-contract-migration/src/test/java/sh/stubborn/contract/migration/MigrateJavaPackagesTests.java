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
import org.openrewrite.java.Assertions;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

class MigrateJavaPackagesTests implements RewriteTest {

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
					""", """
					package com.toomuchcoding.jsonassert;
					public class JsonAssertion {
						public static Object assertThatJson(String body) { return null; }
					}
					""", """
					package com.toomuchcoding.xmlassert;
					public class XmlAssertion {
						public static Object assertThat(String body) { return null; }
					}
					""", """
					package org.springframework.cloud.contract.verifier.openapivalidation;
					public class VerifyContractsAgainstOpenApi {
					}
					""", """
					package org.springframework.cloud.contract.verifier.converter;
					public class OpenApiContractConverter {
					}
					""", """
					package org.springframework.cloud.contract.verifier.converter;
					public class YamlContractConverter {
					}
					"""));
	}

	@Test
	void renamesStubRunnerImport() {
		rewriteRun(Assertions.java("""
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
		rewriteRun(Assertions.java("""
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

	@Test
	void renamesJsonAssertImport() {
		rewriteRun(Assertions.java("""
				import com.toomuchcoding.jsonassert.JsonAssertion;

				class MyTest {
					Object result = JsonAssertion.assertThatJson("{}");
				}
				""", """
				import sh.stubborn.jsonassert.JsonAssertion;

				class MyTest {
					Object result = JsonAssertion.assertThatJson("{}");
				}
				"""));
	}

	@Test
	void renamesXmlAssertImport() {
		rewriteRun(Assertions.java("""
				import com.toomuchcoding.xmlassert.XmlAssertion;

				class MyTest {
					Object result = XmlAssertion.assertThat("<a/>");
				}
				""", """
				import sh.stubborn.xmlassert.XmlAssertion;

				class MyTest {
					Object result = XmlAssertion.assertThat("<a/>");
				}
				"""));
	}

	@Test
	void renamesOpenApiValidationImportToItsRelocatedPackage() {
		// The openapi-validator's openapivalidation classes moved to
		// sh.stubborn.contract.openapi.validation, not sh.stubborn.contract.verifier.*.
		rewriteRun(Assertions.java("""
				import org.springframework.cloud.contract.verifier.openapivalidation.VerifyContractsAgainstOpenApi;

				class MyTest {
					VerifyContractsAgainstOpenApi verifier = new VerifyContractsAgainstOpenApi();
				}
				""", """
				import sh.stubborn.contract.openapi.validation.VerifyContractsAgainstOpenApi;

				class MyTest {
					VerifyContractsAgainstOpenApi verifier = new VerifyContractsAgainstOpenApi();
				}
				"""));
	}

	@Test
	void renamesOpenApiContractConverterToItsRelocatedPackage() {
		rewriteRun(Assertions.java("""
				import org.springframework.cloud.contract.verifier.converter.OpenApiContractConverter;

				class MyTest {
					OpenApiContractConverter converter = new OpenApiContractConverter();
				}
				""", """
				import sh.stubborn.contract.openapi.converter.OpenApiContractConverter;

				class MyTest {
					OpenApiContractConverter converter = new OpenApiContractConverter();
				}
				"""));
	}

	@Test
	void keepsLibraryConverterInTheVerifierPackage() {
		// Discriminates the openapi redirect above from the broad rename: a genuine
		// library
		// type in the same source package must still go to
		// sh.stubborn.contract.verifier.*.
		rewriteRun(Assertions.java("""
				import org.springframework.cloud.contract.verifier.converter.YamlContractConverter;

				class MyTest {
					YamlContractConverter converter = new YamlContractConverter();
				}
				""", """
				import sh.stubborn.contract.verifier.converter.YamlContractConverter;

				class MyTest {
					YamlContractConverter converter = new YamlContractConverter();
				}
				"""));
	}

}
