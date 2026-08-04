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

package sh.stubborn.contract.verifier.plugin;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.Directory;
import org.gradle.api.internal.project.DefaultProject;
import org.gradle.api.plugins.GroovyPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.GroovySourceDirectorySet;
import org.gradle.api.tasks.SourceSet;
import org.gradle.testfixtures.ProjectBuilder;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class ContractVerifierTest {

	@Nullable DefaultProject project;

	private DefaultProject project() {
		return Objects.requireNonNull(this.project);
	}

	@BeforeEach
	void setup() {
		String dateString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
		File testFolder = new File("build/generated-tests/" + getClass().getSimpleName() + "/" + dateString);
		testFolder.mkdirs();
		this.project = (DefaultProject) ProjectBuilder.builder().withProjectDir(testFolder).build();
		this.project.getPlugins().apply(StubbornContractGradlePlugin.class);
	}

	@Test
	@DisplayName("should apply java plugin")
	void shouldApplyJavaPlugin() {
		then(project().getPlugins().hasPlugin(JavaPlugin.class)).isTrue();
	}

	@Test
	@DisplayName("should create contracts extension")
	void shouldCreateContractsExtension() {
		then(project().getExtensions().findByType(ContractVerifierExtension.class)).isNotNull();
	}

	@Test
	@DisplayName("should create a test sourceset with java sources")
	void shouldCreateATestSourcesetWithJavaSources() {
		ContractVerifierExtension extension = project().getExtensions().getByType(ContractVerifierExtension.class);
		Directory projectDir = project().getLayout().getProjectDirectory();
		SourceSet contractTest = Objects.requireNonNull(project().getExtensions().findByType(JavaPluginExtension.class))
			.getSourceSets()
			.getByName("contractTest");

		then(contractTest).isNotNull();
		then(contractTest.getJava().getSrcDirs()).contains(projectDir.dir("src/contractTest/java").getAsFile());
		then(contractTest.getJava().getSrcDirs())
			.contains(extension.getGeneratedTestJavaSourcesDir().get().getAsFile());
		then(contractTest.getResources().getSrcDirs())
			.contains(projectDir.dir("src/contractTest/resources").getAsFile());
		then(contractTest.getResources().getSrcDirs())
			.contains(extension.getGeneratedTestResourcesDir().get().getAsFile());
	}

	@Test
	@DisplayName("should create a test sourceset with groovy sources, if the groovy plugin is present")
	void shouldCreateATestSourcesetWithGroovySources() {
		project().getPlugins().apply(GroovyPlugin.class);
		ContractVerifierExtension extension = project().getExtensions().getByType(ContractVerifierExtension.class);
		Directory projectDir = project().getLayout().getProjectDirectory();
		SourceSet contractTest = project().getExtensions()
			.getByType(JavaPluginExtension.class)
			.getSourceSets()
			.getByName("contractTest");
		GroovySourceDirectorySet groovySources = contractTest.getExtensions().getByType(GroovySourceDirectorySet.class);

		then(contractTest).isNotNull();
		then(contractTest.getJava().getSrcDirs()).contains(projectDir.dir("src/contractTest/java").getAsFile());
		then(contractTest.getJava().getSrcDirs())
			.contains(extension.getGeneratedTestJavaSourcesDir().get().getAsFile());
		then(groovySources.getSrcDirs()).contains(projectDir.dir("src/contractTest/groovy").getAsFile());
		then(groovySources.getSrcDirs()).contains(extension.getGeneratedTestGroovySourcesDir().get().getAsFile());
		then(contractTest.getResources().getSrcDirs())
			.contains(projectDir.dir("src/contractTest/resources").getAsFile());
		then(contractTest.getResources().getSrcDirs())
			.contains(extension.getGeneratedTestResourcesDir().get().getAsFile());
	}

	@Test
	@DisplayName("should setup dependency configurations")
	void shouldSetupDependencyConfigurations() {
		Configuration contractTestCompileOnly = project().getConfigurations().getByName("contractTestCompileOnly");
		Configuration contractTestImplementation = project().getConfigurations()
			.getByName("contractTestImplementation");
		Configuration contractTestRuntimeOnly = project().getConfigurations().getByName("contractTestRuntimeOnly");

		then(contractTestCompileOnly).isNotNull();
		then(contractTestCompileOnly.getExtendsFrom())
			.contains(project().getConfigurations().getByName("testCompileOnly"));
		then(contractTestImplementation).isNotNull();
		then(contractTestImplementation.getExtendsFrom())
			.contains(project().getConfigurations().getByName("testImplementation"));
		then(contractTestRuntimeOnly).isNotNull();
		then(contractTestRuntimeOnly.getExtendsFrom())
			.contains(project().getConfigurations().getByName("testRuntimeOnly"));
	}

	@Test
	@DisplayName("should create contract test task")
	void shouldCreateContractTestTask() {
		then(project().getTasks().named("contractTest").get()).isNotNull();
	}

	@Test
	@DisplayName("should create generateContractTests task")
	void shouldCreateGenerateContractTestsTask() {
		then(project().getTasks().named("generateContractTests").get()).isNotNull();
	}

	@Test
	@DisplayName("should configure generateContractTests task as a dependency of the compileContractTestJava task")
	void shouldConfigureGenerateContractTestsTaskAsDependencyOfCompileContractTestJava() {
		then(project().getTasks().getByName("compileContractTestJava").getDependsOn())
			.contains(project().getTasks().named("generateContractTests"));
		then(project().getTasks().findByName("compileContractTestGroovy")).isNull();
	}

	@Test
	@DisplayName("should configure generateContractTests task as a dependency of the compileContractTestGroovy task")
	void shouldConfigureGenerateContractTestsTaskAsDependencyOfCompileContractTestGroovy() {
		project().getPlugins().apply(GroovyPlugin.class);

		then(project().getTasks().getByName("compileContractTestJava").getDependsOn())
			.contains(project().getTasks().named("generateContractTests"));
		then(project().getTasks().getByName("compileContractTestGroovy").getDependsOn())
			.contains(project().getTasks().named("generateContractTests"));
	}

	@Test
	@DisplayName("should create generateClientStubs task")
	void shouldCreateGenerateClientStubsTask() {
		then(project().getTasks().named("generateClientStubs").get()).isNotNull();
	}

	@Test
	@DisplayName("should create verifierStubsJar task")
	void shouldCreateVerifierStubsJarTask() {
		then(project().getTasks().named("verifierStubsJar").get()).isNotNull();
	}

	@Test
	@DisplayName("should configure generateClientStubs task as a dependency of the verifierStubsJar task")
	void shouldConfigureGenerateClientStubsTaskAsDependencyOfVerifierStubsJar() {
		then(project().getTasks().getByName("verifierStubsJar").getDependsOn())
			.contains(project().getTasks().named("generateClientStubs"));
	}

	@Test
	@DisplayName("should configure generateClientStubs task as a dependency of the publishStubsToScm task")
	void shouldConfigureGenerateClientStubsTaskAsDependencyOfPublishStubsToScm() {
		then(project().getTasks().getByName("publishStubsToScm").getDependsOn())
			.contains(project().getTasks().named("generateClientStubs"));
	}

	@Test
	@DisplayName("should create copyContracts task")
	void shouldCreateCopyContractsTask() {
		then(project().getTasks().named("copyContracts").get()).isNotNull();
	}

	@Test
	@DisplayName("should configure copyContracts task as a dependency of the verifierStubsJar task")
	void shouldConfigureCopyContractsTaskAsDependencyOfVerifierStubsJar() {
		then(project().getTasks().getByName("verifierStubsJar").getDependsOn())
			.contains(project().getTasks().named("generateClientStubs"));
	}

	@Test
	@DisplayName("should compile")
	void shouldCompile() {
		project().getPlugins().apply(StubbornContractGradlePlugin.class);
		ContractVerifierExtension extension = Objects
			.requireNonNull(project().getExtensions().findByType(ContractVerifierExtension.class));

		// tag::package_with_base_classes[]
		extension.setPackageWithBaseClasses("com.example.base");
		// end::package_with_base_classes[]

		// tag::base_class_mappings[]
		extension.setBaseClassForTests("com.example.FooBase");
		extension.baseClassMappings((mappings) -> {
			mappings.baseClassMapping(".*/com/.*", "com.example.ComBase");
			mappings.baseClassMapping(Map.of(".*/bar/.*", "com.example.BarBase"));
		});
		// end::base_class_mappings[]

		then(extension).isNotNull();
	}

	@Test
	@DisplayName("should property merge scm repository settings for publishing stubs to scm")
	void shouldPropertyMergeScmRepositorySettingsForPublishingStubsToScm() {
		project().getPlugins().apply(StubbornContractGradlePlugin.class);
		ContractVerifierExtension extension = Objects
			.requireNonNull(project().getExtensions().findByType(ContractVerifierExtension.class));
		PublishStubsToScmTask task = (PublishStubsToScmTask) Objects
			.requireNonNull(project().getTasks().findByName(PublishStubsToScmTask.TASK_NAME));

		ContractVerifierExtension.ContractRepository extensionRepository = extension.getContractRepository();
		extensionRepository.getRepositoryUrl().set("https://git.example.com");
		extensionRepository.getUsername().set("username");
		extensionRepository.getPassword().set("password");
		extensionRepository.getProxyHost().set("host");
		extensionRepository.getProxyPort().set(8080);

		then(task.getContractRepository().getRepositoryUrl().get()).isEqualTo("https://git.example.com");
		then(task.getContractRepository().getUsername().get()).isEqualTo("username");
		then(task.getContractRepository().getPassword().get()).isEqualTo("password");
		then(task.getContractRepository().getProxyHost().get()).isEqualTo("host");
		then(task.getContractRepository().getProxyPort().get()).isEqualTo(8080);

		ContractVerifierExtension.ContractRepository publishRepository = extension.getPublishStubsToScm()
			.getContractRepository();
		publishRepository.getRepositoryUrl().set("https://git2.example.com");
		publishRepository.getUsername().set("username2");
		publishRepository.getPassword().set("password2");
		publishRepository.getProxyHost().set("host2");
		publishRepository.getProxyPort().set(8081);

		then(task.getContractRepository().getRepositoryUrl().get()).isEqualTo("https://git2.example.com");
		then(task.getContractRepository().getUsername().get()).isEqualTo("username2");
		then(task.getContractRepository().getPassword().get()).isEqualTo("password2");
		then(task.getContractRepository().getProxyHost().get()).isEqualTo("host2");
		then(task.getContractRepository().getProxyPort().get()).isEqualTo(8081);
	}

}
