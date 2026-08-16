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

package sh.stubborn.contract.verifier.template;

import java.util.Map;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.Request;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for
 * {@link sh.stubborn.contract.verifier.builder.handlebars.HandlebarsJsonPathHelper}
 * exercised through {@link HandlebarsTemplateProcessor#transform}. Focus on the
 * {@code jsonPath request.body} (String context) branch and the test-side model
 * detection.
 */
class HandlebarsJsonPathHelperMutationTests {

	private final HandlebarsTemplateProcessor processor = new HandlebarsTemplateProcessor();

	private static Request request(Object body) {
		Contract contract = Contract.make((dsl) -> dsl.request((r) -> {
			r.method("POST");
			r.url("/foo");
			r.body(body);
			r.headers((h) -> h.header("X-Key", "val"));
		}));
		return contract.getRequest();
	}

	@Test
	void request_body_jsonpath_long_value_gets_L_suffix_via_test_side_model() {
		Request request = request(Map.of("big", 10000000000L));
		String result = this.processor.transform(request, "{{jsonPath request.body '$.big'}}");
		assertThat(result).isEqualTo("10000000000L");
	}

	@Test
	void request_body_jsonpath_string_value_is_returned_as_is() {
		Request request = request(Map.of("name", "John"));
		String result = this.processor.transform(request, "{{jsonPath request.body '$.name'}}");
		assertThat(result).isEqualTo("John");
	}

	@Test
	void request_body_jsonpath_integer_value_has_no_suffix() {
		Request request = request(Map.of("count", 5));
		String result = this.processor.transform(request, "{{jsonPath request.body '$.count'}}");
		assertThat(result).isEqualTo("5");
	}

}
