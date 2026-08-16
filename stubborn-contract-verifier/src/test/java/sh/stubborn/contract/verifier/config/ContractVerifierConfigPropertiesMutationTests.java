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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link ContractVerifierConfigProperties} accessors.
 */
class ContractVerifierConfigPropertiesMutationTests {

	@Test
	void enum_accessors_round_trip() {
		ContractVerifierConfigProperties props = new ContractVerifierConfigProperties();
		props.setTestFramework(TestFramework.SPOCK);
		assertThat(props.getTestFramework()).isEqualTo(TestFramework.SPOCK);
		props.setTestMode(TestMode.JAXRSCLIENT);
		assertThat(props.getTestMode()).isEqualTo(TestMode.JAXRSCLIENT);
		props.setFieldInjection(FieldInjection.JAKARTA);
		assertThat(props.getFieldInjection()).isEqualTo(FieldInjection.JAKARTA);
	}

	@Test
	void string_accessors_round_trip() {
		ContractVerifierConfigProperties props = new ContractVerifierConfigProperties();
		props.setBasePackageForTests("com.example.base");
		assertThat(props.getBasePackageForTests()).isEqualTo("com.example.base");
		props.setBaseClassForTests("com.example.BaseClass");
		assertThat(props.getBaseClassForTests()).isEqualTo("com.example.BaseClass");
		props.setNameSuffixForTests("IT");
		assertThat(props.getNameSuffixForTests()).isEqualTo("IT");
		props.setRuleClassForTests("com.example.Rule");
		assertThat(props.getRuleClassForTests()).isEqualTo("com.example.Rule");
		props.setStubsSuffix("stubs-suffix");
		assertThat(props.getStubsSuffix()).isEqualTo("stubs-suffix");
		props.setIncludedContracts("included.*");
		assertThat(props.getIncludedContracts()).isEqualTo("included.*");
		props.setIncludedRootFolderAntPattern("**/root/");
		assertThat(props.getIncludedRootFolderAntPattern()).isEqualTo("**/root/");
		props.setPackageWithBaseClasses("com.example.bases");
		assertThat(props.getPackageWithBaseClasses()).isEqualTo("com.example.bases");
	}

	@Test
	void list_accessors_round_trip() {
		ContractVerifierConfigProperties props = new ContractVerifierConfigProperties();
		props.setExcludedFiles(List.of("a.groovy"));
		assertThat(props.getExcludedFiles()).containsExactly("a.groovy");
		props.setIncludedFiles(List.of("b.groovy"));
		assertThat(props.getIncludedFiles()).containsExactly("b.groovy");
		props.setIgnoredFiles(List.of("c.groovy"));
		assertThat(props.getIgnoredFiles()).containsExactly("c.groovy");
	}

	@Test
	void array_accessors_round_trip() {
		ContractVerifierConfigProperties props = new ContractVerifierConfigProperties();
		props.setImports(new String[] { "com.example.Import" });
		assertThat(props.getImports()).containsExactly("com.example.Import");
		props.setStaticImports(new String[] { "com.example.staticImport" });
		assertThat(props.getStaticImports()).containsExactly("com.example.staticImport");
	}

	@Test
	void file_accessors_round_trip() {
		ContractVerifierConfigProperties props = new ContractVerifierConfigProperties();
		File dsl = new File("contracts");
		props.setContractsDslDir(dsl);
		assertThat(props.getContractsDslDir()).isEqualTo(dsl);
		File sources = new File("gen-sources");
		props.setGeneratedTestSourcesDir(sources);
		assertThat(props.getGeneratedTestSourcesDir()).isEqualTo(sources);
		File resources = new File("gen-resources");
		props.setGeneratedTestResourcesDir(resources);
		assertThat(props.getGeneratedTestResourcesDir()).isEqualTo(resources);
		File stubs = new File("stubs-out");
		props.setStubsOutputDir(stubs);
		assertThat(props.getStubsOutputDir()).isEqualTo(stubs);
	}

	@Test
	void assert_json_size_round_trip() {
		ContractVerifierConfigProperties props = new ContractVerifierConfigProperties();
		props.setAssertJsonSize(true);
		assertThat(props.getAssertJsonSize()).isTrue();
		props.setAssertJsonSize(false);
		assertThat(props.getAssertJsonSize()).isFalse();
	}

	@Test
	void base_class_mappings_round_trip() {
		ContractVerifierConfigProperties props = new ContractVerifierConfigProperties();
		Map<String, String> mappings = new HashMap<>();
		mappings.put("com.example", "com.example.Base");
		props.setBaseClassMappings(mappings);
		assertThat(props.getBaseClassMappings()).containsEntry("com.example", "com.example.Base");
	}

	@Test
	void exclude_build_folders_round_trip() {
		ContractVerifierConfigProperties props = new ContractVerifierConfigProperties();
		props.setExcludeBuildFolders(true);
		assertThat(props.getExcludeBuildFolders()).isTrue();
		assertThat(props.isExcludeBuildFolders()).isTrue();
		props.setExcludeBuildFolders(false);
		assertThat(props.getExcludeBuildFolders()).isFalse();
		assertThat(props.isExcludeBuildFolders()).isFalse();
	}

	@Test
	void fail_on_in_progress_round_trip() {
		ContractVerifierConfigProperties props = new ContractVerifierConfigProperties();
		props.setFailOnInProgress(true);
		assertThat(props.isFailOnInProgress()).isTrue();
		props.setFailOnInProgress(false);
		assertThat(props.isFailOnInProgress()).isFalse();
	}

}
