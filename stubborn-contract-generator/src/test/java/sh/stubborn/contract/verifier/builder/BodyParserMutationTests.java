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

package sh.stubborn.contract.verifier.builder;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.OptionalProperty;
import sh.stubborn.contract.spec.internal.Request;
import sh.stubborn.contract.verifier.file.ContractMetadata;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused coverage for {@link BodyParser}'s form-encoding branches. The Map and
 * List form-body renderers are the survivor lambdas at lines 123 and 130; asserting the
 * exact {@code a=1&b=2} rendering kills the NullReturn mutants on those lambdas (a null
 * lambda result would break the {@code &}-join).
 */
class BodyParserMutationTests {

	private BodyParser parser() {
		return new BodyParser() {
			@Override
			public String byteArrayString() {
				return "";
			}

			@Override
			public String responseAsString() {
				return "";
			}
		};
	}

	private SingleContractMetadata formRequest(Consumer<Request> bodyCustomizer) {
		Contract contract = new Contract();
		contract.request((request) -> {
			request.method("POST");
			request.url("/form");
			request.headers((headers) -> headers.contentType("application/x-www-form-urlencoded"));
			bodyCustomizer.accept(request);
		});
		contract.response((response) -> response.status(200));
		ContractMetadata metadata = new ContractMetadata(new File("form.groovy").toPath(), false, 0, null, contract);
		return new SingleContractMetadata(contract, metadata);
	}

	@Test
	void form_map_body_is_rendered_as_ampersand_joined_pairs() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("a", "1");
		body.put("b", "2");
		SingleContractMetadata metadata = formRequest((request) -> request.body(body));

		String result = parser().requestBodyAsString(metadata);

		assertThat(result).isEqualTo("a=1&b=2");
	}

	@Test
	void form_list_body_is_rendered_as_ampersand_joined_values() {
		SingleContractMetadata metadata = formRequest((request) -> request.body(List.of("a=1", "b=2")));

		String result = parser().requestBodyAsString(metadata);

		assertThat(result).isEqualTo("a=1&b=2");
	}

	@Test
	void json_body_renders_unresolved_optional_property_as_its_regex_string() {
		Contract contract = new Contract();
		contract.request((request) -> {
			request.method("POST");
			request.url("/json");
			request.headers((headers) -> headers.contentType("application/json"));
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("opt", new OptionalProperty("foo"));
			request.body(body);
		});
		contract.response((response) -> response.status(200));
		ContractMetadata metadata = new ContractMetadata(new File("json.groovy").toPath(), false, 0, null, contract);
		SingleContractMetadata scm = new SingleContractMetadata(contract, metadata);

		String result = parser().requestBodyAsString(scm);

		// resolveDynamicValues() must render the OptionalProperty via toString()
		// ("(foo)?")
		// rather than letting Jackson serialize its bean (or a NullReturn mutant emit
		// null).
		assertThat(result).contains("(foo)?");
	}

}
