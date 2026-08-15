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

package sh.stubborn.contract.wiremock;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserts the return values of the thin WireMock wrappers so the null-return mutations
 * are killed.
 *
 * @author Marcin Grzejszczak
 */
class WireMockCoverageTests {

	@Test
	void buildFromShouldReturnAParsedStubMapping() {
		StubMapping mapping = WireMockStubMapping
			.buildFrom("{\"request\" : { \"method\" : \"GET\" }, \"response\" : { \"status\" : 200 }}");
		assertThat(mapping).isNotNull();
		assertThat(mapping.getRequest().getMethod().getName()).isEqualTo("GET");
		assertThat(mapping.getResponse().getStatus()).isEqualTo(200);
	}

	@Test
	void optionsShouldReturnAFreshConfiguration() {
		assertThat(WireMockSpring.options()).isNotNull();
	}

}
