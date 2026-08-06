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

package sh.stubborn.contract.verifier.dsl.wiremock;

import java.util.regex.Pattern;

import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.verifier.file.ContractMetadata;

import static org.assertj.core.api.Assertions.assertThat;

public interface WireMockStubVerifier {

	default void stubMappingIsValidWireMockStub(String mappingDefinition) {
		StubMapping stubMapping = WireMockStubMapping.buildFrom(mappingDefinition);
		stubMappingIsValidWireMockStub(stubMapping);
	}

	default void stubMappingIsValidWireMockStub(StubMapping mappingDefinition) {
		if (mappingDefinition.getRequest().getBodyPatterns() != null) {
			mappingDefinition.getRequest()
				.getBodyPatterns()
				.stream()
				.filter((p) -> p instanceof RegexPattern)
				.forEach((p) -> Pattern.compile((String) ((RegexPattern) p).getValue()));
		}
		String definition = mappingDefinition.toString();
		assertThat(definition).doesNotContain("sh.stubborn.contract.spec.internal");
		assertThat(definition).doesNotContain("cursor");
	}

	default void stubMappingIsValidWireMockStub(Contract contractDsl) {
		stubMappingIsValidWireMockStub(
				new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, contractDsl), contractDsl)
					.toWireMockClientStub());
	}

}
