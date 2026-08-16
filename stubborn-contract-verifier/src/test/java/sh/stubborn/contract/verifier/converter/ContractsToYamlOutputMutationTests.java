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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link ContractsToYaml}'s output-message mapping: inline
 * regex body values and regex/command header values become {@link YamlContract} matchers.
 */
class ContractsToYamlOutputMutationTests {

	private final YamlContractConverter converter = new YamlContractConverter();

	private YamlContract convert(Contract contract) {
		List<YamlContract> yaml = this.converter.convertTo(List.of(contract));
		return yaml.get(0);
	}

	@Test
	void output_message_inline_regex_body_becomes_by_regex_matcher() {
		Contract contract = Contract.make((dsl) -> {
			dsl.input((in) -> in.triggeredBy("trigger()"));
			dsl.outputMessage((out) -> {
				out.sentTo("topic");
				Map<String, Object> body = new LinkedHashMap<>();
				body.put("code", out.regex("[0-9]+"));
				body.put("name", "foo");
				out.body(body);
				out.headers((h) -> h.header("Content-Type", "application/json"));
			});
		});
		YamlContract yaml = convert(contract);
		assertThat(yaml.outputMessage.matchers.body).anySatisfy((bm) -> {
			assertThat(bm.type).isEqualTo(YamlContract.TestMatcherType.by_regex);
			assertThat(bm.value).isEqualTo("[0-9]+");
		});
	}

	@Test
	void output_message_regex_header_becomes_test_header_matcher() {
		Contract contract = Contract.make((dsl) -> {
			dsl.input((in) -> in.triggeredBy("trigger()"));
			dsl.outputMessage((out) -> {
				out.sentTo("topic");
				out.body(Map.of("a", "b"));
				out.headers((h) -> h.header("Code", out.regex("[0-9]+")));
			});
		});
		YamlContract yaml = convert(contract);
		assertThat(yaml.outputMessage.matchers.headers).anySatisfy((hm) -> {
			assertThat(hm.key).isEqualTo("Code");
			assertThat(hm.regex).isEqualTo("[0-9]+");
		});
	}

}
