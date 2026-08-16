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

package sh.stubborn.contract.verifier.file;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link ContractMetadata} and
 * {@link ContractFileScanner.Builder}.
 */
class ContractMetadataAndBuilderMutationTests {

	private static Contract contract(boolean inProgress) {
		Contract contract = Contract.make((dsl) -> {
			dsl.request((r) -> {
				r.method("GET");
				r.url("/foo");
			});
			dsl.response((resp) -> resp.status(200));
		});
		contract.setInProgress(inProgress);
		return contract;
	}

	@Test
	void anyInProgress_true_and_false() {
		ContractMetadata inProgress = new ContractMetadata(Path.of("x.groovy"), false, 1, null, contract(true));
		assertThat(inProgress.anyInProgress()).isTrue();
		ContractMetadata notInProgress = new ContractMetadata(Path.of("x.groovy"), false, 1, null, contract(false));
		assertThat(notInProgress.anyInProgress()).isFalse();
	}

	@Test
	void ignored_flag_from_constructor() {
		ContractMetadata ignored = new ContractMetadata(Path.of("x.groovy"), true, 1, null, contract(false));
		assertThat(ignored.isIgnored()).isTrue();
		assertThat(ignored.getIgnored()).isTrue();
		ContractMetadata notIgnored = new ContractMetadata(Path.of("x.groovy"), false, 1, null, contract(false));
		assertThat(notIgnored.isIgnored()).isFalse();
	}

	@Test
	void forContract_returns_matching_metadata_and_toString() {
		Contract contract = contract(false);
		ContractMetadata metadata = new ContractMetadata(Path.of("x.groovy"), false, 1, null, contract);
		assertThat(metadata.forContract(contract)).isNotNull();
		assertThat(metadata.getConvertedContractWithMetadata()).isNotEmpty();
		assertThat(metadata.toString()).isNotBlank().contains("ContractMetadata");
	}

	@Test
	void builder_methods_are_fluent_and_build_scanner(@org.junit.jupiter.api.io.TempDir Path tmp) {
		File baseDir = tmp.toFile();
		ContractFileScanner.Builder builder = ContractFileScanner.builder();
		assertThat(builder.baseDir(baseDir)).isSameAs(builder);
		assertThat(builder.excluded(Set.of("e/**"))).isSameAs(builder);
		assertThat(builder.ignored(Set.of("i/**"))).isSameAs(builder);
		assertThat(builder.included(Set.of("in/**"))).isSameAs(builder);
		assertThat(builder.includeMatcher("matcher")).isSameAs(builder);
		ContractFileScanner scanner = builder.build();
		assertThat(scanner).isNotNull();
		// a scanner over an empty base dir yields an empty result
		assertThat(scanner.findContractsRecursively()).isEmpty();
	}

	@Test
	void builder_returns_new_builder_instances() {
		assertThat(ContractFileScanner.builder()).isNotNull();
	}

	@Test
	void metadata_exposes_converted_contract_list() {
		Contract contract = contract(false);
		ContractMetadata metadata = new ContractMetadata(Path.of("x.groovy"), false, 2, 7, List.of(contract));
		assertThat(metadata.getConvertedContract()).containsExactly(contract);
		assertThat(metadata.getGroupSize()).isEqualTo(2);
		assertThat(metadata.getOrder()).isEqualTo(7);
	}

}
