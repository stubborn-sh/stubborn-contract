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

package sh.stubborn.contract.verifier.wiremock;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.GroovyContractConverter;
import sh.stubborn.contract.verifier.file.ContractMetadata;

import static org.assertj.core.api.BDDAssertions.then;

class DslToWireMockClientConverterMutationTests {

	private final DslToWireMockClientConverter converter = new DslToWireMockClientConverter();

	@Test
	void named_contracts_in_a_list_use_their_name_for_the_scenario() {
		Collection<Contract> contracts = GroovyContractConverter.convertAsCollection(new File("/"), """
				[
				sh.stubborn.contract.spec.Contract.make {
					name "scenarioOne"
					request {
						method 'GET'
						url '/a'
					}
					response {
						status OK()
					}
				},
				sh.stubborn.contract.spec.Contract.make {
					name "scenarioTwo"
					request {
						method 'GET'
						url '/b'
					}
					response {
						status OK()
					}
				}
				]
				""");
		Path file = Paths.get("scenarios.groovy");
		ContractMetadata metadata = new ContractMetadata(file, false, 2, 0, contracts);

		Map<Contract, String> converted = this.converter.convertContents("rootName", metadata);

		then(converted).hasSize(2);
		then(converted.values()).anyMatch((json) -> json.contains("Scenario_scenarioOne"));
		then(converted.values()).anyMatch((json) -> json.contains("Scenario_scenarioTwo"));
		then(converted.values()).noneMatch((json) -> json.contains("Scenario_rootName"));
	}

}
