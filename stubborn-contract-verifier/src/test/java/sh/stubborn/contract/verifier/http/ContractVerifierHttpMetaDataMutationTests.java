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

package sh.stubborn.contract.verifier.http;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.verifier.http.ContractVerifierHttpMetaData.Protocol;
import sh.stubborn.contract.verifier.http.ContractVerifierHttpMetaData.Scheme;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link ContractVerifierHttpMetaData}.
 */
class ContractVerifierHttpMetaDataMutationTests {

	@Test
	void defaults_are_http_and_http_1_1() {
		ContractVerifierHttpMetaData metaData = new ContractVerifierHttpMetaData();
		assertThat(metaData.getScheme()).isEqualTo(Scheme.HTTP);
		assertThat(metaData.getProtocol()).isEqualTo(Protocol.HTTP_1_1);
	}

	@Test
	void key_and_description() {
		ContractVerifierHttpMetaData metaData = new ContractVerifierHttpMetaData();
		assertThat(metaData.key()).isEqualTo("verifierHttp");
		assertThat(ContractVerifierHttpMetaData.METADATA_KEY).isEqualTo("verifierHttp");
		assertThat(metaData.description()).isEqualTo("Metadata entries used by the framework");
	}

	@Test
	void setters_update_scheme_and_protocol() {
		ContractVerifierHttpMetaData metaData = new ContractVerifierHttpMetaData();
		metaData.setScheme(Scheme.HTTPS);
		assertThat(metaData.getScheme()).isEqualTo(Scheme.HTTPS);
		metaData.setProtocol(Protocol.HTTP_2);
		assertThat(metaData.getProtocol()).isEqualTo(Protocol.HTTP_2);
	}

	@Test
	void string_setters_parse_values() {
		ContractVerifierHttpMetaData metaData = new ContractVerifierHttpMetaData();
		metaData.setScheme("https");
		assertThat(metaData.getScheme()).isEqualTo(Scheme.HTTPS);
		metaData.setProtocol("h2");
		assertThat(metaData.getProtocol()).isEqualTo(Protocol.HTTP_2);
	}

	@Test
	void scheme_fromString_case_insensitive_and_null() {
		assertThat(Scheme.fromString("HTTP")).isEqualTo(Scheme.HTTP);
		assertThat(Scheme.fromString("https")).isEqualTo(Scheme.HTTPS);
		assertThat(Scheme.fromString("nope")).isNull();
	}

	@Test
	void protocol_fromString_and_toString() {
		assertThat(Protocol.fromString("http/1.0")).isEqualTo(Protocol.HTTP_1_0);
		assertThat(Protocol.fromString("HTTP/1.1")).isEqualTo(Protocol.HTTP_1_1);
		assertThat(Protocol.fromString("quic")).isEqualTo(Protocol.QUIC);
		assertThat(Protocol.fromString("unknown")).isNull();
		assertThat(Protocol.HTTP_2.toString()).isEqualTo("h2");
		assertThat(Protocol.H2_PRIOR_KNOWLEDGE.toString()).isEqualTo("h2_prior_knowledge");
	}

	@Test
	void fromMetadata_returns_stored_and_default() {
		ContractVerifierHttpMetaData stored = new ContractVerifierHttpMetaData();
		stored.setScheme(Scheme.HTTPS);
		Map<String, Object> metadata = new HashMap<>();
		metadata.put(ContractVerifierHttpMetaData.METADATA_KEY, stored);
		ContractVerifierHttpMetaData result = ContractVerifierHttpMetaData.fromMetadata(metadata);
		assertThat(result.getScheme()).isEqualTo(Scheme.HTTPS);
		ContractVerifierHttpMetaData def = ContractVerifierHttpMetaData.fromMetadata(new HashMap<>());
		assertThat(def.getScheme()).isEqualTo(Scheme.HTTP);
	}

}
