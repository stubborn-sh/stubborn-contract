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

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link ContractsToYaml} via
 * {@link YamlContractConverter#convertTo}. Assertions pin the produced
 * {@link YamlContract} fields for URL regex matchers, body matchers, response body and
 * output messages.
 */
class ContractsToYamlMutationTests {

	private final YamlContractConverter converter = new YamlContractConverter();

	private YamlContract convert(Contract contract) {
		List<YamlContract> yaml = this.converter.convertTo(List.of(contract));
		return yaml.get(0);
	}

	@Test
	void url_regex_matcher_is_written_to_matchers_url() {
		Contract contract = Contract.make((dsl) -> {
			dsl.request((r) -> {
				r.method("GET");
				r.url(r.value(r.stub(Pattern.compile("/id/[0-9]+")), r.test("/id/1")));
			});
			dsl.response((resp) -> resp.status(200));
		});
		YamlContract yaml = convert(contract);
		assertThat(yaml.request.matchers.url).isNotNull();
		assertThat(yaml.request.matchers.url.regex).isEqualTo("/id/[0-9]+");
	}

	@Test
	void plain_url_leaves_matchers_url_null() {
		Contract contract = Contract.make((dsl) -> {
			dsl.request((r) -> {
				r.method("GET");
				r.url("/plain");
			});
			dsl.response((resp) -> resp.status(200));
		});
		YamlContract yaml = convert(contract);
		assertThat(yaml.request.matchers.url).isNull();
	}

	@Test
	void request_body_json_path_matcher_is_written() {
		Contract contract = Contract.make((dsl) -> {
			dsl.request((r) -> {
				r.method("POST");
				r.url("/foo");
				r.body(Map.of("name", "x"));
				r.headers((h) -> h.contentType("application/json"));
				r.bodyMatchers((m) -> m.jsonPath("$.name", m.byRegex("[a-z]+")));
			});
			dsl.response((resp) -> resp.status(200));
		});
		YamlContract yaml = convert(contract);
		assertThat(yaml.request.matchers.body).anySatisfy((bm) -> {
			assertThat(bm.path).isEqualTo("$.name");
			assertThat(bm.type).isEqualTo(YamlContract.StubMatcherType.by_regex);
		});
	}

	@Test
	void response_body_is_written() {
		Contract contract = Contract.make((dsl) -> {
			dsl.request((r) -> {
				r.method("GET");
				r.url("/foo");
			});
			dsl.response((resp) -> {
				resp.status(200);
				resp.body(Map.of("c", "d"));
				resp.headers((h) -> h.contentType("application/json"));
			});
		});
		YamlContract yaml = convert(contract);
		assertThat(yaml.response.body).isNotNull();
		assertThat(yaml.response.status).isEqualTo(200);
	}

	@Test
	void output_message_is_written() {
		Contract contract = Contract.make((dsl) -> {
			dsl.input((in) -> in.triggeredBy("createBook()"));
			dsl.outputMessage((out) -> {
				out.sentTo("topic");
				out.body(Map.of("a", "b"));
				out.headers((h) -> h.header("Content-Type", "application/json"));
			});
		});
		YamlContract yaml = convert(contract);
		assertThat(yaml.outputMessage).isNotNull();
		assertThat(yaml.outputMessage.sentTo).isEqualTo("topic");
		assertThat(yaml.outputMessage.body).isNotNull();
		assertThat(yaml.input.triggeredBy).isEqualTo("createBook()");
	}

}
