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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marcin Grzejszczak
 */
class WireMockStubMappingTests {

	private static final String STUB_2_1_7 = """

			{"request":{"method":"GET"},"response":{"status":200}}

			""";

	private static final String STUB_2_5_1 = """

			{"id":"77514bd4-a102-4478-a3c0-0fda8b905591","request":{"method":"GET"},"response":{"status":200},"uuid":"77514bd4-a102-4478-a3c0-0fda8b905591"}

			""";

	private static final String STUB_2_5_1_WITH_TRANSFORMER = """

			{"id":"77514bd4-a102-4478-a3c0-0fda8b905591","request":{"method":"GET"},"response":{"status":200,"transformers":["response-template"]},"uuid":"77514bd4-a102-4478-a3c0-0fda8b905591"}

			""";

	@Test
	void should_successfully_parse_a_WireMock_2_1_7_stub() {
		assertThat(WireMockStubMapping.buildFrom(STUB_2_1_7)).isNotNull();
	}

	@Test
	void should_successfully_parse_a_WireMock_2_5_0_stub() {
		assertThat(WireMockStubMapping.buildFrom(STUB_2_5_1)).isNotNull();
	}

	@Test
	void should_successfully_parse_a_WireMock_2_5_0_stub_that_contains_transformers() {
		assertThat(WireMockStubMapping.buildFrom(STUB_2_5_1_WITH_TRANSFORMER)).isNotNull();
	}

}
