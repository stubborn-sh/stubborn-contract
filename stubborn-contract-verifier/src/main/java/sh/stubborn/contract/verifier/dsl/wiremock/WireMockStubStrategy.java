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

import java.util.Objects;

import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.jspecify.annotations.Nullable;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.verifier.file.ContractMetadata;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;

/**
 * Converts a {@link ContractMetadata} into a WireMock {@link StubMapping}.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public class WireMockStubStrategy {

	private static final String STEP_START = "Started";

	private static final String STEP_PREFIX = "Step";

	private final WireMockRequestStubStrategy wireMockRequestStubStrategy;

	private final WireMockResponseStubStrategy wireMockResponseStubStrategy;

	private final @Nullable Integer priority;

	private final ContractMetadata contract;

	private final String rootName;

	private final Contract contractDsl;

	public WireMockStubStrategy(String rootName, ContractMetadata contract, Contract contractDsl) {
		this.rootName = rootName;
		this.contract = contract;
		SingleContractMetadata singleContractMetadata = Objects.requireNonNull(contract.forContract(contractDsl));
		this.wireMockRequestStubStrategy = new WireMockRequestStubStrategy(contractDsl, singleContractMetadata);
		this.wireMockResponseStubStrategy = new WireMockResponseStubStrategy(contractDsl, singleContractMetadata);
		this.priority = contractDsl.getPriority();
		this.contractDsl = contractDsl;
	}

	/**
	 * Converts {@link ContractMetadata} to {@link StubMapping}.
	 * @return the built WireMock stub mapping, or {@code null} when it cannot be built
	 */
	public @Nullable StubMapping toWireMockClientStub() {
		StubMapping stubMapping = new StubMapping();
		RequestPattern request = this.wireMockRequestStubStrategy.buildClientRequestContent();
		ResponseDefinition response = this.wireMockResponseStubStrategy.buildClientResponseContent();
		if (this.priority != null) {
			stubMapping.setPriority(this.priority);
		}
		stubMapping.setRequest(request);
		stubMapping.setResponse(response);
		if (request == null || response == null) {
			return null;
		}
		if (this.contractDsl.getIgnored() || this.contract.getIgnored()) {
			return null;
		}
		if (this.contract.getOrder() != null) {
			stubMapping.setScenarioName("Scenario_" + this.rootName);
			stubMapping.setRequiredScenarioState(
					(this.contract.getOrder() == 0) ? STEP_START : STEP_PREFIX + this.contract.getOrder());
			if (this.contract.getOrder() < this.contract.getGroupSize() - 1) {
				stubMapping.setNewScenarioState(STEP_PREFIX + (this.contract.getOrder() + 1));
			}
		}
		return stubMapping;
	}

}
