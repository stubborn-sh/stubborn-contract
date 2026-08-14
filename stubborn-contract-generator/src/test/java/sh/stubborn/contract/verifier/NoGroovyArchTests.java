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

package sh.stubborn.contract.verifier;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;

class NoGroovyArchTests {

	private final JavaClasses classes = new ClassFileImporter()
		.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
		.importPackages("sh.stubborn.contract.verifier");

	@Test
	void productionCodeHasNoGroovyDependencies() {
		ArchRuleDefinition.noClasses()
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage("groovy..", "org.codehaus.groovy..", "org.apache.groovy..")
			.as("Test generator must not depend on Groovy — Groovy DSL parsing lives in stubborn-contract-spec-groovy")
			.check(this.classes);
	}

}
