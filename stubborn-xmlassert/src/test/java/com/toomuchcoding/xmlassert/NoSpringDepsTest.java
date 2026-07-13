package com.toomuchcoding.xmlassert;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class NoSpringDepsTest {

	private static final JavaClasses CLASSES = new ClassFileImporter().importPackages("com.toomuchcoding.xmlassert");

	@Test
	void no_spring_imports() {
		ArchRule rule = noClasses().should().dependOnClassesThat().resideInAPackage("org.springframework..");
		rule.check(CLASSES);
	}

}
