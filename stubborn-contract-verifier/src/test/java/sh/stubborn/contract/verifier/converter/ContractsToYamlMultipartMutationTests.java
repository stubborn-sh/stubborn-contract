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
 * Mutation-focused tests for {@link ContractsToYaml} multipart mapping: regex params and
 * named file parts become {@link YamlContract} multipart entries and matchers.
 */
class ContractsToYamlMultipartMutationTests {

	private final YamlContractConverter converter = new YamlContractConverter();

	private YamlContract convert(Contract contract) {
		List<YamlContract> yaml = this.converter.convertTo(List.of(contract));
		return yaml.get(0);
	}

	private Contract multipartContract() {
		return Contract.make((dsl) -> {
			dsl.request((r) -> {
				r.method("POST");
				r.url("/multipart");
				r.headers((h) -> h.contentType("multipart/form-data;boundary=AaB03x"));
				Map<String, Object> multipart = new LinkedHashMap<>();
				multipart.put("formParameter", r.$(r.c(r.regex("[a-z]+")), r.p("val")));
				multipart.put("file",
						r.named(r.$(r.c(r.regex("[a-z]+\\.txt")), r.p("f.txt")), r.$(r.c(r.regex(".+")), r.p("data"))));
				r.multipart(multipart);
			});
			dsl.response((resp) -> resp.status(200));
		});
	}

	@Test
	void regex_param_becomes_a_multipart_param_matcher() {
		YamlContract yaml = convert(multipartContract());
		assertThat(yaml.request.multipart).isNotNull();
		assertThat(yaml.request.matchers.multipart.params).anySatisfy((m) -> {
			assertThat(m.key).isEqualTo("formParameter");
			assertThat(m.regex).isEqualTo("[a-z]+");
		});
	}

	@Test
	void named_file_regex_becomes_a_named_multipart_matcher() {
		YamlContract yaml = convert(multipartContract());
		assertThat(yaml.request.matchers.multipart.named).anySatisfy((m) -> {
			assertThat(m.paramName).isEqualTo("file");
			assertThat(m.fileName).isNotNull();
			assertThat(m.fileName.regex).isEqualTo("[a-z]+\\.txt");
		});
	}

	@Test
	void named_file_server_values_are_written_to_multipart_named() {
		YamlContract yaml = convert(multipartContract());
		assertThat(yaml.request.multipart.named).anySatisfy((n) -> {
			assertThat(n.paramName).isEqualTo("file");
			assertThat(n.fileName).isEqualTo("f.txt");
			assertThat(n.fileContent).isEqualTo("data");
		});
	}

}
