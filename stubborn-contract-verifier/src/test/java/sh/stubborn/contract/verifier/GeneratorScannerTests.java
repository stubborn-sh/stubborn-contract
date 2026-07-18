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

package sh.stubborn.contract.verifier;

import java.io.File;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sh.stubborn.contract.verifier.builder.SingleTestGenerator;
import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.config.TestFramework;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneratorScannerTests {

	@Mock
	private SingleTestGenerator classGenerator;

	@Mock
	private FileSaver fileSaver;

	@Test
	void should_find_all_json_files_and_generate_6_classes_for_them() throws URISyntaxException {
		when(classGenerator.buildClass(any(), any(), any(), any())).thenReturn("qwerty");
		File resource = new File(getClass().getResource("/directory/with/stubs/stubsRepositoryIndicator").toURI());
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setContractsDslDir(resource.getParentFile());
		TestGenerator testGenerator = new TestGenerator(properties, classGenerator, fileSaver);
		testGenerator.generateTestClasses("sh.stubborn.contract.verifier");
		verify(classGenerator, times(6)).buildClass(any(), any(), any(), any());
	}

	@Test
	void should_create_class_with_full_package() throws URISyntaxException {
		when(classGenerator.buildClass(any(), any(), any(), any())).thenReturn("spec");
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.SPOCK);
		properties.setContractsDslDir(new File(getClass().getResource("/directory/with/stubs/package").toURI()));
		TestGenerator testGenerator = new TestGenerator(properties, classGenerator, fileSaver);
		testGenerator.generateTestClasses("sh.stubborn.contract.verifier");
		verify(classGenerator).buildClass(any(), any(), any(), argThat(it -> "exceptionsSpec".equals(it.className)
				&& "sh.stubborn.contract.verifier".equals(it.classPackage)));
		verify(classGenerator).buildClass(any(), any(), any(), argThat(it -> "exceptionsSpec".equals(it.className)
				&& "sh.stubborn.contract.verifier.v1".equals(it.classPackage)));
		verify(classGenerator).buildClass(any(), any(), any(), argThat(it -> "exceptionsSpec".equals(it.className)
				&& "sh.stubborn.contract.verifier.v2".equals(it.classPackage)));
	}

	@Test
	void should_create_class_with_name_with_hyphen() throws URISyntaxException {
		when(classGenerator.buildClass(any(), any(), any(), any())).thenReturn("spec");
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setTestFramework(TestFramework.SPOCK);
		properties.setContractsDslDir(new File(getClass().getResource("/directory/with/name-with-hyphen").toURI()));
		TestGenerator testGenerator = new TestGenerator(properties, classGenerator, fileSaver);
		testGenerator.generateTestClasses("sh.stubborn.contract.verifier");
		verify(classGenerator).buildClass(any(), any(), any(), argThat(it -> "car_rentalSpec".equals(it.className)
				&& "sh.stubborn.contract.verifier".equals(it.classPackage)));
	}

}
