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

package sh.stubborn.contract.verifier.builder;

import java.io.File;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marcin Grzejszczak
 */
class ClassBuilderTests {

	@Test
	void shouldReturnExplicitBaseClassIfProvidedAndNoDefaultPackageForBaseClassesIsProvided() {
		Map<String, String> baseClassMappings = null;
		String packageWithBaseClasses = null;
		String baseClassForTests = "a.b.Class";
		assertThat(new BaseClassProvider().retrieveBaseClass(baseClassMappings, packageWithBaseClasses,
				baseClassForTests, "com/example/foo"))
			.isEqualTo("a.b.Class");
	}

	@Test
	void shouldReturnAClassFromTheGeneratedPathByTakingTwoLastFoldersWhenPackageWithBaseClassesIsProvided() {
		Map<String, String> baseClassMappings = null;
		String packageWithBaseClasses = "com.example.base";
		String baseClassForTests = null;
		String contractRelativeFolder = String.join(File.separator, "com", "example", "some", "superpackage");
		assertThat(new BaseClassProvider().retrieveBaseClass(baseClassMappings, packageWithBaseClasses,
				baseClassForTests, contractRelativeFolder))
			.isEqualTo("com.example.base.SomeSuperpackageBase");
	}

	@Test
	void shouldReturnAClassFromTheGeneratedPathByTakingTwoLastFoldersWhenPackageWithBaseClassesIsProvidedAndContainsInvalidChars() {
		Map<String, String> baseClassMappings = null;
		String packageWithBaseClasses = "com.example.base";
		String baseClassForTests = null;
		String contractRelativeFolder = String.join(File.separator, "com", "example", "beer-api-producer-external",
				"beer-api-consumer");
		assertThat(new BaseClassProvider().retrieveBaseClass(baseClassMappings, packageWithBaseClasses,
				baseClassForTests, contractRelativeFolder))
			.isEqualTo("com.example.base.Beer_api_producer_externalBeer_api_consumerBase");
	}

	@Test
	void shouldReturnAClassFromTheGeneratedPathByTakingASingleFolderWhenPackageWithBaseClassesIsProvidedAndThereAreNotEnoughPackageElements() {
		Map<String, String> baseClassMappings = null;
		String packageWithBaseClasses = "com.example.base";
		String baseClassForTests = null;
		String contractRelativeFolder = "superpackage";
		assertThat(new BaseClassProvider().retrieveBaseClass(baseClassMappings, packageWithBaseClasses,
				baseClassForTests, contractRelativeFolder))
			.isEqualTo("com.example.base.SuperpackageBase");
	}

	@Test
	void shouldReturnAClassFromMappingsRegardlessOfOtherEntriesIfMappingExists() {
		Map<String, String> baseClassMappings = Map.of(".*", "com.example.base.SuperClass");
		String packageWithBaseClasses = "com.example.base";
		String baseClassForTests = null;
		String contractRelativeFolder = "superpackage";
		assertThat(new BaseClassProvider().retrieveBaseClass(baseClassMappings, packageWithBaseClasses,
				baseClassForTests, contractRelativeFolder))
			.isEqualTo("com.example.base.SuperClass");
	}

	@Test
	void shouldMatchBaseClassWhenMappingRegexHasMultipleFolders() {
		Map<String, String> baseClassMappings = Map.of(".*bar.baz.some.*", "com.example.base.SuperClass");
		String packageWithBaseClasses = null;
		String baseClassForTests = null;
		String contractRelativeFolder = String.join(File.separator, "foo/bar/baz/some/package".split("/"));
		assertThat(new BaseClassProvider().retrieveBaseClass(baseClassMappings, packageWithBaseClasses,
				baseClassForTests, contractRelativeFolder))
			.isEqualTo("com.example.base.SuperClass");
	}

	@Test
	void shouldReturnTheFirstMatchingBaseClassWhenProvidedMappingDoesntMatch() {
		Map<String, String> baseClassMappings = Map.of("patternNotMatchingAnything", "com.example.base.SuperClass");
		String packageWithBaseClasses = "com.example.base";
		String baseClassForTests = "a.b.Class";
		String contractRelativeFolder = "superpackage";
		assertThat(new BaseClassProvider().retrieveBaseClass(baseClassMappings, packageWithBaseClasses,
				baseClassForTests, contractRelativeFolder))
			.isEqualTo("com.example.base.SuperpackageBase");
	}

	@Test
	void shouldReturnAClassFromTheGeneratedPathByWhenExternalContractsArePicked() {
		Map<String, String> baseClassMappings = null;
		String packageWithBaseClasses = "foo.Bar";
		String baseClassForTests = null;
		String contractRelativeFolder = String.join(File.separator, "org", "springframework", "cloud", "contract",
				"verifier", "tests", "META_INF", "com.example", "hello_world", "0.1.0_dev.1.uncommitted+d1174dd");
		assertThat(new BaseClassProvider().retrieveBaseClass(baseClassMappings, packageWithBaseClasses,
				baseClassForTests, contractRelativeFolder))
			.isEqualTo("foo.Bar.Hello_world0_1_0_dev_1_uncommitted_d1174ddBase");
	}

}
