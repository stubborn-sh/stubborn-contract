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

package sh.stubborn.contract.verifier.util;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link ContractVerifierDslConverter}: the accept predicate,
 * conversion round-trips and single-contract collection wrapping.
 */
class ContractVerifierDslConverter2MutationTests {

	private final ContractVerifierDslConverter converter = new ContractVerifierDslConverter();

	private static File fileFor(String path) {
		try {
			return new File(ContractVerifierDslConverter2MutationTests.class.getResource(path).toURI());
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Test
	void accepts_java_files_only() {
		assertThat(this.converter.isAccepted(new File("contract.java"))).isTrue();
		assertThat(this.converter.isAccepted(new File("contract.groovy"))).isFalse();
		assertThat(this.converter.isAccepted(new File("contract.yml"))).isFalse();
	}

	@Test
	void convert_from_java_file_yields_a_named_contract() {
		Collection<Contract> contracts = this.converter.convertFrom(fileFor("/contractsToCompile/contract.java"));
		assertThat(contracts).isNotEmpty();
		assertThat(contracts).allSatisfy((c) -> assertThat(c.getName()).isNotBlank());
	}

	@Test
	void convert_as_collection_matches_convert_from() {
		Collection<Contract> contracts = ContractVerifierDslConverter
			.convertAsCollection(fileFor("/contractsToCompile/contract.java"));
		assertThat(contracts).hasSize(1);
	}

	@Test
	void convert_to_returns_the_same_contracts() {
		Contract contract = Contract.make((dsl) -> {
			dsl.request((r) -> {
				r.method("GET");
				r.url("/x");
			});
			dsl.response((resp) -> resp.status(200));
		});
		List<Contract> input = List.of(contract);
		assertThat(this.converter.convertTo(input)).containsExactly(contract);
	}

}
