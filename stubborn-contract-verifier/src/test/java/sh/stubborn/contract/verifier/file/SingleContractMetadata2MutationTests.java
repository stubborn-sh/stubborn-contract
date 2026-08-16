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

package sh.stubborn.contract.verifier.file;

import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.verifier.util.ContentType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Additional mutation-focused tests for {@link SingleContractMetadata}: the
 * {@code evaluatesToJson} fall-through, case-insensitive Content-Type header lookup and
 * regex-body content-type evaluation via the other side.
 */
class SingleContractMetadata2MutationTests {

	private static SingleContractMetadata single(Contract contract) {
		Path path = Path.of("dir", "x.json");
		ContractMetadata metadata = new ContractMetadata(path, false, 0, null, contract);
		return new SingleContractMetadata(contract, metadata);
	}

	@Test
	void xml_contract_does_not_evaluate_to_json() {
		Contract contract = Contract.make((dsl) -> {
			dsl.request((r) -> {
				r.method("POST");
				r.url("/foo");
				r.body("<root><a>1</a></root>");
				r.headers((h) -> h.contentType("application/xml"));
			});
			dsl.response((resp) -> {
				resp.status(200);
				resp.body("<root><b>2</b></root>");
				resp.headers((h) -> h.contentType("application/xml"));
			});
		});
		SingleContractMetadata scm = single(contract);
		assertThat(scm.isJson()).isFalse();
		assertThat(scm.evaluatesToJson()).isFalse();
	}

	@Test
	void json_body_without_header_still_evaluates_to_json() {
		Contract contract = Contract.make((dsl) -> {
			dsl.request((r) -> {
				r.method("POST");
				r.url("/foo");
				r.body(Map.of("a", "b"));
			});
			dsl.response((resp) -> {
				resp.status(200);
				resp.body(Map.of("c", "d"));
			});
		});
		SingleContractMetadata scm = single(contract);
		assertThat(scm.evaluatesToJson()).isTrue();
	}

	@Test
	void lowercase_content_type_header_is_recognised() {
		Contract contract = Contract.make((dsl) -> {
			dsl.request((r) -> {
				r.method("POST");
				r.url("/foo");
				r.body(Map.of("a", "b"));
				r.headers((h) -> h.header("content-type", "application/json"));
			});
			dsl.response((resp) -> resp.status(200));
		});
		SingleContractMetadata scm = single(contract);
		assertThat(scm.getInputStubContentType()).isEqualTo(ContentType.JSON);
	}

	@Test
	void regex_stub_body_evaluates_content_type_from_server_side() {
		Contract contract = Contract.make((dsl) -> {
			dsl.request((r) -> {
				r.method("POST");
				r.url("/foo");
				r.body(r.value(r.stub(Pattern.compile("\\{.*\\}")), r.test("{\"a\":\"b\"}")));
				r.headers((h) -> h.contentType("application/json"));
			});
			dsl.response((resp) -> resp.status(200));
		});
		SingleContractMetadata scm = single(contract);
		assertThat(scm.getInputTestContentType()).isEqualTo(ContentType.JSON);
	}

}
