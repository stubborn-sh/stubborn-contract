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

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.util.ContentType;
import sh.stubborn.contract.verifier.util.MapConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Mutation-focused tests for {@link BaseWireMockStubStrategy#parseBody} value
 * preservation of booleans and numbers inside a JSON list body — asserting the exact
 * serialized values, not just their types.
 */
class BaseWireMockParseBodyMutationTests {

	private static String responseBody(Object body) {
		Contract contract = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("/foo");
			});
			c.response((r) -> {
				r.status(200);
				r.body(body);
			});
		});
		SingleContractMetadata metadata = mock(SingleContractMetadata.class);
		given(metadata.getEvaluatedOutputStubContentType()).willReturn(ContentType.JSON);
		WireMockResponseStubStrategy subject = new WireMockResponseStubStrategy(contract, metadata) {
			@Override
			Function<String, Object> parsingClosureForContentType() {
				return MapConverter.JSON_PARSING_FUNCTION;
			}
		};
		ResponseDefinition content = subject.buildClientResponseContent();
		return content.getBody().replaceAll("\\s+", "");
	}

	@Test
	void boolean_values_in_a_list_keep_their_true_and_false_identity() {
		assertThat(responseBody(Arrays.asList(true, false))).isEqualTo("[true,false]");
	}

	@Test
	void number_values_in_a_list_keep_their_value() {
		assertThat(responseBody(List.of(7, 42))).isEqualTo("[7,42]");
	}

	@Test
	void nested_list_of_primitives_is_recursively_stringified() {
		assertThat(responseBody(List.of(List.of(1, 2), List.of(3)))).isEqualTo("[\"[1,2]\",\"[3]\"]");
	}

}
