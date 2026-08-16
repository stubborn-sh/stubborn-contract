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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.verifier.util.ContentType;
import sh.stubborn.contract.verifier.util.NamesUtil;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link SingleContractMetadata}.
 */
class SingleContractMetadataMutationTests {

	private static SingleContractMetadata single(Contract contract, Path path, boolean ignored) {
		ContractMetadata metadata = new ContractMetadata(path, ignored, 0, null, contract);
		return new SingleContractMetadata(contract, metadata);
	}

	private static Contract jsonHttpContract() {
		return Contract.make((dsl) -> {
			dsl.request((r) -> {
				r.method("POST");
				r.url("/foo");
				r.body(Map.of("a", "b"));
				r.headers((h) -> h.contentType("application/json"));
			});
			dsl.response((resp) -> {
				resp.status(200);
				resp.body(Map.of("c", "d"));
				resp.headers((h) -> h.contentType("application/json"));
			});
		});
	}

	private static Contract xmlHttpContract() {
		return Contract.make((dsl) -> {
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
	}

	@Test
	void json_contract_flags() {
		SingleContractMetadata scm = single(jsonHttpContract(), Path.of("dir", "should_work.json"), false);
		assertThat(scm.isJson()).isTrue();
		assertThat(scm.isXml()).isFalse();
		assertThat(scm.evaluatesToJson()).isTrue();
		assertThat(scm.isHttp()).isTrue();
		assertThat(scm.isMessaging()).isFalse();
		assertThat(scm.isInProgress()).isFalse();
		assertThat(scm.isIgnored()).isFalse();
	}

	@Test
	void xml_contract_flags() {
		SingleContractMetadata scm = single(xmlHttpContract(), Path.of("dir", "should_work.xml"), false);
		assertThat(scm.isXml()).isTrue();
		assertThat(scm.isJson()).isFalse();
	}

	@Test
	void content_types_are_evaluated() {
		SingleContractMetadata scm = single(jsonHttpContract(), Path.of("dir", "should_work.json"), false);
		assertThat(scm.getInputTestContentType()).isEqualTo(ContentType.JSON);
		assertThat(scm.getOutputTestContentType()).isEqualTo(ContentType.JSON);
		assertThat(scm.getInputStubContentType()).isEqualTo(ContentType.JSON);
		assertThat(scm.getEvaluatedInputStubContentType()).isEqualTo(ContentType.JSON);
		assertThat(scm.getEvaluatedOutputStubContentType()).isEqualTo(ContentType.JSON);
		assertThat(scm.getEvaluatedOutputTestContentType()).isEqualTo(ContentType.JSON);
		assertThat(scm.getDefinedInputTestContentType()).isEqualTo("application/json");
		assertThat(scm.getDefinedOutputTestContentType()).contains("application/json");
		assertThat(scm.getDefinedInputStubContentType()).contains("application/json");
		assertThat(scm.getDefinedOutputStubContentType()).contains("application/json");
	}

	@Test
	void defined_content_types_empty_when_no_header() {
		Contract contract = Contract.make((dsl) -> {
			dsl.request((r) -> {
				r.method("GET");
				r.url("/foo");
			});
			dsl.response((resp) -> resp.status(200));
		});
		SingleContractMetadata scm = single(contract, Path.of("dir", "x.json"), false);
		assertThat(scm.getDefinedInputTestContentType()).isEmpty();
		assertThat(scm.getDefinedOutputTestContentType()).isEmpty();
		assertThat(scm.getInputStubContentType()).isEqualTo(ContentType.UNKNOWN);
		assertThat(scm.getOutputTestContentType()).isEqualTo(ContentType.UNKNOWN);
	}

	@Test
	void messaging_contract_is_not_http() {
		Contract contract = Contract.make((dsl) -> {
			dsl.input((in) -> in.triggeredBy("bookReturnedTriggered()"));
			dsl.outputMessage((out) -> {
				out.sentTo("output");
				out.body(Map.of("a", "b"));
			});
		});
		SingleContractMetadata scm = single(contract, Path.of("dir", "msg.json"), false);
		assertThat(scm.isHttp()).isFalse();
		assertThat(scm.isMessaging()).isTrue();
		assertThat(scm.getOutputTestContentType()).isEqualTo(ContentType.JSON);
	}

	@Test
	void ignored_from_contract() {
		Contract contract = jsonHttpContract();
		contract.setIgnored(true);
		SingleContractMetadata scm = single(contract, Path.of("dir", "x.json"), false);
		assertThat(scm.isIgnored()).isTrue();
	}

	@Test
	void ignored_from_metadata() {
		SingleContractMetadata scm = single(jsonHttpContract(), Path.of("dir", "x.json"), true);
		assertThat(scm.isIgnored()).isTrue();
	}

	@Test
	void in_progress_flag() {
		Contract contract = jsonHttpContract();
		contract.setInProgress(true);
		SingleContractMetadata scm = single(contract, Path.of("dir", "x.json"), false);
		assertThat(scm.isInProgress()).isTrue();
	}

	@Test
	void method_name_from_contract_name() {
		Contract contract = jsonHttpContract();
		contract.name("my custom name");
		SingleContractMetadata scm = single(contract, Path.of("dir", "file_name.json"), false);
		String expected = NamesUtil.camelCase(NamesUtil.convertIllegalPackageChars("my custom name"));
		assertThat(scm.methodName()).isEqualTo(expected);
		assertThat(scm.methodName()).isNotEmpty();
		// second call returns cached value
		assertThat(scm.methodName()).isEqualTo(expected);
	}

	@Test
	void method_name_from_file_name_when_no_contract_name() {
		SingleContractMetadata scm = single(jsonHttpContract(), Path.of("dir", "should_return_ok.json"), false);
		String expected = NamesUtil.camelCase(NamesUtil.convertIllegalMethodNameChars("should_return_ok"));
		assertThat(scm.methodName()).isEqualTo(expected);
		assertThat(scm.methodName()).isNotEmpty();
	}

	@Test
	void method_name_scenario_appends_index() {
		Contract first = jsonHttpContract();
		Contract second = xmlHttpContract();
		Path path = Path.of("dir", "scenario.json");
		ContractMetadata metadata = new ContractMetadata(path, false, 0, null, List.of(first, second));
		SingleContractMetadata secondMeta = new SingleContractMetadata(second, metadata);
		String base = NamesUtil.camelCase(NamesUtil.convertIllegalMethodNameChars("scenario"));
		assertThat(secondMeta.methodName()).isEqualTo(base + "_1");
	}

	@Test
	void getters_expose_state() {
		Contract contract = jsonHttpContract();
		Path path = Path.of("dir", "x.json");
		ContractMetadata metadata = new ContractMetadata(path, false, 0, null, contract);
		SingleContractMetadata scm = new SingleContractMetadata(contract, metadata);
		assertThat(scm.getContract()).isSameAs(contract);
		assertThat(scm.getContractMetadata()).isSameAs(metadata);
		assertThat(scm.getAllContracts()).containsExactly(contract);
	}

}
