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

package sh.stubborn.contract.verifier.converter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.verifier.file.ContractMetadata;

import static org.assertj.core.api.BDDAssertions.then;

class StubGeneratorMutationTests {

	private final StubGenerator<String> generator = new StubGenerator<>() {
		@Override
		public Map<Contract, String> convertContents(String rootName, ContractMetadata content) {
			return new HashMap<>();
		}

		@Override
		public String generateOutputFileNameForInput(String inputFileName) {
			return inputFileName;
		}
	};

	@Test
	void canReadStubMapping_matches_only_the_default_json_extension() {
		then(this.generator.canReadStubMapping(new File("foo.json"))).isTrue();
		then(this.generator.canReadStubMapping(new File("foo.txt"))).isFalse();
	}

	@Test
	void fileExtension_defaults_to_json() {
		then(this.generator.fileExtension()).isEqualTo(".json");
	}

	@Test
	void defaultStubMappingPostProcessing_returns_the_same_stub_mapping() {
		then(this.generator.defaultStubMappingPostProcessing("stub", new Contract())).isEqualTo("stub");
	}

	@Test
	void postProcessStubMapping_returns_default_when_no_processor_is_applicable() {
		Contract contract = new Contract();
		then(this.generator.postProcessStubMapping("original", contract)).isEqualTo("original");
	}

	@Test
	void postProcessStubMapping_applies_matching_processor() {
		Contract contract = new Contract();
		contract.getMetadata().put(MarkerStubPostProcessor.MARKER, true);
		then(this.generator.postProcessStubMapping("original", contract)).isEqualTo(MarkerStubPostProcessor.PROCESSED);
	}

}
