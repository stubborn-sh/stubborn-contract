/*
 * Copyright 2013-present the original author or authors.
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

package sh.stubborn.contract.verifier.config;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.verifier.config.framework.CustomDefinition;
import sh.stubborn.contract.verifier.config.framework.JUnit5Definition;
import sh.stubborn.contract.verifier.config.framework.SpockDefinition;
import sh.stubborn.contract.verifier.config.framework.TestNGDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Mutation-focused tests for {@link TestFramework}, the framework definitions and
 * {@link FieldInjection}.
 */
class TestFrameworkAndInjectionMutationTests {

	@Test
	void spock_definition_values() {
		SpockDefinition def = new SpockDefinition();
		assertThat(def.getClassExtension()).isEqualTo(".groovy");
		assertThat(def.getClassNameSuffix()).isEqualTo("Spec");
		assertThat(def.getIgnoreClass()).isEqualTo("spock.lang.Ignore");
		assertThat(def.getIgnoreAnnotation()).isEqualTo("@Ignore");
	}

	@Test
	void junit5_definition_values() {
		JUnit5Definition def = new JUnit5Definition();
		assertThat(def.getClassExtension()).isEqualTo(".java");
		assertThat(def.getClassNameSuffix()).isEqualTo("Test");
		assertThat(def.getIgnoreClass()).isEqualTo("org.junit.jupiter.api.Disabled");
		assertThat(def.getIgnoreAnnotation()).isEqualTo("@Disabled");
	}

	@Test
	void testng_definition_values() {
		TestNGDefinition def = new TestNGDefinition();
		assertThat(def.getClassExtension()).isEqualTo(".java");
		assertThat(def.getClassNameSuffix()).isEqualTo("Test");
		assertThatThrownBy(def::getIgnoreClass).isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(def::getIgnoreAnnotation).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void custom_definition_values() {
		CustomDefinition def = new CustomDefinition();
		assertThat(def.getClassExtension()).isEmpty();
		assertThat(def.getClassNameSuffix()).isEmpty();
		assertThat(def.getIgnoreClass()).isEmpty();
		assertThat(def.getIgnoreAnnotation()).isEqualTo("@Ignore");
	}

	@Test
	void test_framework_delegates_to_definitions() {
		assertThat(TestFramework.SPOCK.getClassExtension()).isEqualTo(".groovy");
		assertThat(TestFramework.SPOCK.getClassNameSuffix()).isEqualTo("Spec");
		assertThat(TestFramework.SPOCK.getIgnoreClass()).isEqualTo("spock.lang.Ignore");
		assertThat(TestFramework.SPOCK.getIgnoreAnnotation()).isEqualTo("@Ignore");
		assertThat(TestFramework.JUNIT5.getClassExtension()).isEqualTo(".java");
		assertThat(TestFramework.JUNIT5.getClassNameSuffix()).isEqualTo("Test");
		assertThat(TestFramework.JUNIT5.getIgnoreClass()).isEqualTo("org.junit.jupiter.api.Disabled");
		assertThat(TestFramework.JUNIT5.getIgnoreAnnotation()).isEqualTo("@Disabled");
		assertThat(TestFramework.CUSTOM.getClassExtension()).isEmpty();
		assertThat(TestFramework.CUSTOM.getIgnoreAnnotation()).isEqualTo("@Ignore");
		assertThat(TestFramework.TESTNG.getClassExtension()).isEqualTo(".java");
		assertThat(TestFramework.TESTNG.getClassNameSuffix()).isEqualTo("Test");
	}

	@Test
	void field_injection_resolve_auto_against_mode() {
		assertThat(FieldInjection.AUTO.resolve(TestMode.MOCKMVC)).isEqualTo(FieldInjection.SPRING);
		assertThat(FieldInjection.AUTO.resolve(TestMode.WEBTESTCLIENT)).isEqualTo(FieldInjection.SPRING);
		assertThat(FieldInjection.AUTO.resolve(TestMode.EXPLICIT)).isEqualTo(FieldInjection.JAKARTA);
		assertThat(FieldInjection.AUTO.resolve(TestMode.JAXRSCLIENT)).isEqualTo(FieldInjection.JAKARTA);
		assertThat(FieldInjection.AUTO.resolve(TestMode.CUSTOM)).isEqualTo(FieldInjection.JAKARTA);
	}

	@Test
	void field_injection_resolve_non_auto_returns_self() {
		assertThat(FieldInjection.SPRING.resolve(TestMode.EXPLICIT)).isEqualTo(FieldInjection.SPRING);
		assertThat(FieldInjection.JAKARTA.resolve(TestMode.MOCKMVC)).isEqualTo(FieldInjection.JAKARTA);
		assertThat(FieldInjection.NONE.resolve(TestMode.MOCKMVC)).isEqualTo(FieldInjection.NONE);
	}

	@Test
	void field_injection_annotation_prefix() {
		assertThat(FieldInjection.SPRING.annotationPrefix()).isEqualTo("@Autowired ");
		assertThat(FieldInjection.JAKARTA.annotationPrefix()).isEqualTo("@Inject ");
		assertThat(FieldInjection.NONE.annotationPrefix()).isEmpty();
		assertThat(FieldInjection.AUTO.annotationPrefix()).isEmpty();
	}

	@Test
	void field_injection_annotation_import() {
		assertThat(FieldInjection.SPRING.annotationImport())
			.isEqualTo("org.springframework.beans.factory.annotation.Autowired");
		assertThat(FieldInjection.JAKARTA.annotationImport()).isEqualTo("jakarta.inject.Inject");
		assertThat(FieldInjection.NONE.annotationImport()).isNull();
		assertThat(FieldInjection.AUTO.annotationImport()).isNull();
	}

}
