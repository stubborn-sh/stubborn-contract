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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.config.TestFramework;
import sh.stubborn.contract.verifier.file.ContractMetadata;

/**
 * Walks the plugin configuration and parsed contracts into a {@link TestClassModel}.
 *
 * <p>
 * This is where today's builder <em>decisions</em> (which class name, base class, class
 * annotations, which methods) move to, separated from formatting. Phase 1 populates the
 * class scaffold: package/name/base-class, the {@code @SuppressWarnings("rawtypes")}
 * class annotation that the Java targets carry, and one method skeleton per contract with
 * its {@code @Test}/{@code @Disabled} annotations. The structured request/response
 * verification bodies and the real method-name derivation (via {@code NameProvider}) are
 * ported in Phases 2–4.
 *
 * @author Marcin Grzejszczak
 */
class ModelBuilder {

	private static final String JUNIT_JUPITER_TEST = "org.junit.jupiter.api.Test";

	private static final String TESTNG_TEST = "org.testng.annotations.Test";

	TestClassModel build(ContractVerifierConfigProperties properties, Collection<ContractMetadata> listOfFiles,
			SingleTestGenerator.GeneratedClassData generatedClassData) {
		TestFramework framework = properties.getTestFramework();
		boolean spock = framework == TestFramework.SPOCK;

		List<AnnotationModel> classAnnotations = new ArrayList<>();
		if (!spock) {
			classAnnotations.add(new AnnotationModel("java.lang.SuppressWarnings", "\"rawtypes\""));
		}

		List<TestMethodModel> methods = new ArrayList<>();
		int index = 0;
		for (ContractMetadata metadata : listOfFiles) {
			methods.add(methodModel(metadata, framework, index++));
		}

		return new TestClassModel(generatedClassData.classPackage, generatedClassData.className,
				properties.getBaseClassForTests(), spock, classAnnotations, methods);
	}

	private TestMethodModel methodModel(ContractMetadata metadata, TestFramework framework, int index) {
		List<AnnotationModel> annotations = new ArrayList<>();
		annotations.add(AnnotationModel.marker(testAnnotationType(framework)));
		if (metadata.getIgnored()) {
			annotations.add(AnnotationModel.marker(framework.getIgnoreClass()));
		}
		return new TestMethodModel(methodName(metadata, index), annotations, List.of());
	}

	private String testAnnotationType(TestFramework framework) {
		return (framework == TestFramework.TESTNG) ? TESTNG_TEST : JUNIT_JUPITER_TEST;
	}

	private String methodName(ContractMetadata metadata, int index) {
		Path path = metadata.getPath();
		String base = (path.getFileName() != null) ? path.getFileName().toString() : ("contract_" + index);
		int dot = base.lastIndexOf('.');
		if (dot > 0) {
			base = base.substring(0, dot);
		}
		String sanitized = base.replaceAll("[^A-Za-z0-9]", "_");
		if (sanitized.isEmpty()) {
			sanitized = "contract_" + index;
		}
		if (Character.isDigit(sanitized.charAt(0))) {
			sanitized = "_" + sanitized;
		}
		return "validate_" + sanitized;
	}

}
