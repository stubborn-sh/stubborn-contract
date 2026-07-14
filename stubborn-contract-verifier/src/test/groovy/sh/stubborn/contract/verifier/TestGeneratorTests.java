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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.BDDAssertions;
import org.junit.Test;
import org.mockito.BDDMockito;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.verifier.builder.SingleTestGenerator;
import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.file.ContractFileScanner;
import sh.stubborn.contract.verifier.file.ContractMetadata;

public class TestGeneratorTests {

	@Test
	public void should_throw_exception_when_in_progress_contracts_found() {
		// given:
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setFailOnInProgress(true);
		properties.setContractsDslDir(new File("."));
		SingleTestGenerator singleTestGenerator = BDDMockito.mock(SingleTestGenerator.class);
		FileSaver fileSaver = BDDMockito.mock(FileSaver.class);
		// and:
		Map<Path, List<ContractMetadata>> multimap = new LinkedHashMap<>();
		Path path = new File(".").toPath();
		multimap.computeIfAbsent(path, k -> new ArrayList<>())
			.add(new ContractMetadata(path, false, 0, null, Contract.make(it -> {
				it.inProgress();
				it.request(r -> {
					r.method(r.GET());
					r.url("/foo");
				});
				it.response(r -> {
					r.status(r.OK());
				});
			})));
		ContractFileScanner scanner = new ContractFileScanner(null, null, null, null, null) {
			@Override
			public Map<Path, List<ContractMetadata>> findContractsRecursively() {
				return multimap;
			}
		};
		// and:
		TestGenerator testGenerator = new TestGenerator(properties, singleTestGenerator, fileSaver, scanner);

		// then:
		BDDAssertions.thenThrownBy(() -> {
			// when:
			testGenerator.generateTestClasses("com.example");
		}).isInstanceOf(IllegalStateException.class).hasMessageContaining("In progress contracts found in");
	}

	@Test
	public void should_not_throw_exception_when_in_progress_contracts_found_but_the_fail_on_in_progress_switch_is_off() {
		// given:
		ContractVerifierConfigProperties properties = new ContractVerifierConfigProperties();
		properties.setFailOnInProgress(false);
		properties.setContractsDslDir(new File("."));
		SingleTestGenerator singleTestGenerator = BDDMockito.mock(SingleTestGenerator.class);
		FileSaver fileSaver = BDDMockito.mock(FileSaver.class);
		// and:
		Map<Path, List<ContractMetadata>> multimap = new LinkedHashMap<>();
		Path path = new File(".").toPath();
		multimap.computeIfAbsent(path, k -> new ArrayList<>())
			.add(new ContractMetadata(path, false, 0, null, Contract.make(it -> {
				it.inProgress();
				it.request(r -> {
					r.method(r.GET());
					r.url("/foo");
				});
				it.response(r -> {
					r.status(r.OK());
				});
			})));
		ContractFileScanner scanner = new ContractFileScanner(null, null, null, null, null) {
			@Override
			public Map<Path, List<ContractMetadata>> findContractsRecursively() {
				return multimap;
			}
		};
		// and:
		TestGenerator testGenerator = new TestGenerator(properties, singleTestGenerator, fileSaver, scanner) {
			@Override
			void processAll(Map<Path, List<ContractMetadata>> contracts, String basePackageName) {
			}
		};

		// when:
		testGenerator.generateTestClasses("com.example");

		// then: noExceptionThrown()
	}

}
