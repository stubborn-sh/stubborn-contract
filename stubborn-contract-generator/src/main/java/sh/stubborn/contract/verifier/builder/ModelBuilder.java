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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.config.TestMode;
import sh.stubborn.contract.verifier.file.ContractMetadata;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;

/**
 * Walks the plugin configuration and parsed contracts into a {@link TestClassModel}.
 *
 * <p>
 * This is where today's builder <em>decisions</em> (which class name, base class, class
 * annotations, which methods) live, separated from formatting. Phase 2 populates the full
 * class scaffold: package/name/base-class, class-level annotations (the
 * {@code @SuppressWarnings("rawtypes")} the Java targets carry, plus JUnit 5's
 * {@code @TestMethodOrder} when a contract carries an order), one method per contract
 * with its {@code @Test}/{@code @Disabled}/{@code @Test(enabled = false)} annotations,
 * the real method-name derivation (via {@link NameProvider}) and the verbatim method body
 * captured from the legacy generator by {@link LegacyMethodBodyExtractor}. The
 * class-level import set is captured from the legacy generator so the renderer can merge
 * it in.
 *
 * @author Marcin Grzejszczak
 */
class ModelBuilder {

	private static final String JUNIT_JUPITER_TEST = "org.junit.jupiter.api.Test";

	private static final String JUNIT_JUPITER_DISABLED = "org.junit.jupiter.api.Disabled";

	private static final String JUNIT_JUPITER_TEST_METHOD_ORDER = "org.junit.jupiter.api.TestMethodOrder";

	private static final String TESTNG_TEST = "org.testng.annotations.Test";

	private final NameProvider nameProvider = new NameProvider();

	private final LegacyMethodBodyExtractor bodyExtractor = new LegacyMethodBodyExtractor();

	private final RequestModelBuilder requestModelBuilder = new RequestModelBuilder();

	TestClassModel build(ContractVerifierConfigProperties properties, Collection<ContractMetadata> listOfFiles,
			String includedDirectoryRelativePath, SingleTestGenerator.GeneratedClassData generatedClassData) {
		TestFramework framework = properties.getTestFramework();
		boolean spock = framework == TestFramework.SPOCK;

		GeneratedClassMetaData meta = new GeneratedClassMetaData(properties, listOfFiles, includedDirectoryRelativePath,
				generatedClassData);

		List<AnnotationModel> classAnnotations = new ArrayList<>();
		if (!spock) {
			classAnnotations.add(new AnnotationModel("java.lang.SuppressWarnings", "\"rawtypes\""));
		}
		if (framework == TestFramework.JUNIT5 && hasOrder(listOfFiles)) {
			// Mirrors JUnit5OrderClassAnnotation. Kept short so it matches the legacy
			// output; the MethodOrderer import is supplied by the merged import set.
			classAnnotations.add(
					AnnotationModel.member(JUNIT_JUPITER_TEST_METHOD_ORDER, "value", "MethodOrderer.MethodName.class"));
		}

		TestMode mode = properties.getTestMode();
		List<TestMethodModel> methods = new ArrayList<>();
		for (SingleContractMetadata scm : meta.toSingleContractMetadata()) {
			methods.add(methodModel(scm, framework, meta, mode));
		}

		List<String> imports = importDeclarations(properties, listOfFiles, includedDirectoryRelativePath,
				generatedClassData);

		return new TestClassModel(generatedClassData.classPackage, generatedClassData.className,
				properties.getBaseClassForTests(), spock, classAnnotations, methods, imports);
	}

	private TestMethodModel methodModel(SingleContractMetadata scm, TestFramework framework,
			GeneratedClassMetaData meta, TestMode mode) {
		List<AnnotationModel> annotations = new ArrayList<>();
		boolean ignored = isIgnored(scm);
		if (framework == TestFramework.TESTNG) {
			// TestNG disables via a named member on @Test, not a separate annotation.
			annotations.add(ignored ? AnnotationModel.member(TESTNG_TEST, "enabled", "false")
					: AnnotationModel.marker(TESTNG_TEST));
		}
		else {
			annotations.add(AnnotationModel.marker(JUNIT_JUPITER_TEST));
			if (ignored) {
				annotations.add(AnnotationModel.marker(JUNIT_JUPITER_DISABLED));
			}
		}
		// When the request portion is eligible for the structured path, emit it from the
		// model and capture only the verbatim // then: block; otherwise fall back to the
		// whole body verbatim from the legacy generator.
		RequestModel request = this.requestModelBuilder.build(scm, framework, meta, mode);
		List<String> bodyLines = (request != null) ? this.bodyExtractor.responseBodyLines(meta, scm)
				: this.bodyExtractor.bodyLines(meta, scm);
		return new TestMethodModel(this.nameProvider.methodName(scm), annotations, bodyLines, request);
	}

	// Mirrors JUnit5IgnoreMethodAnnotation#accept: a contract is treated as ignored when
	// the contract or its metadata is ignored, or the contract is in progress.
	private boolean isIgnored(SingleContractMetadata scm) {
		return scm.getContractMetadata().isIgnored() || scm.getContract().isIgnored()
				|| scm.getContract().isInProgress();
	}

	// Mirrors JUnit5OrderClassAnnotation#accept.
	private boolean hasOrder(Collection<ContractMetadata> listOfFiles) {
		return listOfFiles.stream().anyMatch((file) -> file.getOrder() != null);
	}

	// Captures the legacy generator's class-level import set (both `import` and `import
	// static`), in order, so the renderer can merge it with JavaPoet's own imports.
	private List<String> importDeclarations(ContractVerifierConfigProperties properties,
			Collection<ContractMetadata> listOfFiles, String includedDirectoryRelativePath,
			SingleTestGenerator.GeneratedClassData generatedClassData) {
		String legacy = new JavaTestGenerator().buildClass(properties, listOfFiles, includedDirectoryRelativePath,
				generatedClassData);
		List<String> imports = new ArrayList<>();
		for (String line : legacy.lines().toList()) {
			String trimmed = line.trim();
			if (trimmed.startsWith("import ")) {
				imports.add(trimmed);
			}
			else if (trimmed.startsWith("class ") || trimmed.startsWith("public ") || trimmed.startsWith("@")) {
				break;
			}
		}
		return imports;
	}

}
