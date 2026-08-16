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

package sh.stubborn.contract.verifier.wiremock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.Metadata;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.ChunkedDribbleDelay;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Focused mutation-killing tests for the response-merging getters of
 * {@link DefaultWireMockStubPostProcessor}. Every getter picks the value from the
 * metadata-derived stub mapping if present, otherwise from the base stub mapping. Each
 * test asserts the concrete value returned for BOTH branches so that
 * NegateConditionals/NullReturn/EmptyObjectReturn mutants are all killed.
 */
class DefaultWireMockStubPostProcessorMutationTests {

	private static final String STUB_MAPPING = "{\n" + "    \"request\": {\n" + "        \"method\": \"GET\",\n"
			+ "        \"url\": \"/ping\"\n" + "    },\n" + "    \"response\": {\n" + "        \"status\": 200,\n"
			+ "        \"body\": \"pong\"\n" + "    }\n" + "}";

	private final DefaultWireMockStubPostProcessor processor = new DefaultWireMockStubPostProcessor();

	private static ResponseDefinition resp(@Nullable Integer fixedDelay, @Nullable ChunkedDribbleDelay chunked,
			@Nullable String proxyBaseUrl, @Nullable String proxyPrefix, @Nullable Fault fault,
			@Nullable List<String> transformers, @Nullable Parameters transformerParams,
			@Nullable Boolean wasConfigured) {
		return new ResponseDefinition(200, null, "body", null, null, null, null, null, null, fixedDelay, null, chunked,
				proxyBaseUrl, proxyPrefix, fault, transformers, transformerParams, wasConfigured);
	}

	private static StubMapping stub(ResponseDefinition response) {
		StubMapping mapping = new StubMapping();
		mapping.setResponse(response);
		return mapping;
	}

	@Test
	void fixedDelayMilliseconds_prefers_metadata_then_falls_back() {
		StubMapping base = stub(resp(100, null, null, null, null, null, null, true));
		StubMapping meta = stub(resp(500, null, null, null, null, null, null, true));
		then(this.processor.fixedDelayMilliseconds(base, meta)).isEqualTo(500);

		StubMapping metaNull = stub(resp(null, null, null, null, null, null, null, true));
		then(this.processor.fixedDelayMilliseconds(base, metaNull)).isEqualTo(100);
	}

	@Test
	void chunkedDribbleDelay_prefers_metadata_then_falls_back() {
		StubMapping base = stub(resp(null, new ChunkedDribbleDelay(2, 50), null, null, null, null, null, true));
		StubMapping meta = stub(resp(null, new ChunkedDribbleDelay(5, 100), null, null, null, null, null, true));
		then(this.processor.chunkedDribbleDelay(base, meta).getNumberOfChunks()).isEqualTo(5);

		StubMapping metaNull = stub(resp(null, null, null, null, null, null, null, true));
		then(this.processor.chunkedDribbleDelay(base, metaNull).getNumberOfChunks()).isEqualTo(2);
	}

	@Test
	void proxyBaseUrl_prefers_metadata_then_falls_back() {
		StubMapping base = stub(resp(null, null, "http://base", null, null, null, null, true));
		StubMapping meta = stub(resp(null, null, "http://meta", null, null, null, null, true));
		then(this.processor.proxyBaseUrl(base, meta)).isEqualTo("http://meta");

		StubMapping metaNull = stub(resp(null, null, null, null, null, null, null, true));
		then(this.processor.proxyBaseUrl(base, metaNull)).isEqualTo("http://base");
	}

	@Test
	void proxyUrlPrefixToRemove_prefers_metadata_then_falls_back() {
		StubMapping base = stub(resp(null, null, "http://base", "/base", null, null, null, true));
		StubMapping meta = stub(resp(null, null, "http://meta", "/meta", null, null, null, true));
		then(this.processor.proxyUrlPrefixToRemove(base, meta)).isEqualTo("/meta");

		StubMapping metaNull = stub(resp(null, null, "http://base", null, null, null, null, true));
		then(this.processor.proxyUrlPrefixToRemove(base, metaNull)).isEqualTo("/base");
	}

	@Test
	void fault_prefers_metadata_then_falls_back() {
		StubMapping base = stub(resp(null, null, null, null, Fault.MALFORMED_RESPONSE_CHUNK, null, null, true));
		StubMapping meta = stub(resp(null, null, null, null, Fault.EMPTY_RESPONSE, null, null, true));
		then(this.processor.fault(base, meta)).isEqualTo(Fault.EMPTY_RESPONSE);

		StubMapping metaNull = stub(resp(null, null, null, null, null, null, null, true));
		then(this.processor.fault(base, metaNull)).isEqualTo(Fault.MALFORMED_RESPONSE_CHUNK);
	}

	@Test
	void transformers_prefers_metadata_then_falls_back() {
		StubMapping base = stub(resp(null, null, null, null, null, List.of("base-transformer"), null, true));
		StubMapping meta = stub(resp(null, null, null, null, null, List.of("meta-transformer"), null, true));
		then(this.processor.transformers(base, meta)).containsExactly("meta-transformer");

		StubMapping metaNull = stub(resp(null, null, null, null, null, null, null, true));
		then(this.processor.transformers(base, metaNull)).containsExactly("base-transformer");
	}

	@Test
	void transformerParameters_prefers_metadata_then_falls_back() {
		StubMapping base = stub(resp(null, null, null, null, null, null, Parameters.one("k", "base"), true));
		StubMapping meta = stub(resp(null, null, null, null, null, null, Parameters.one("k", "meta"), true));
		then(this.processor.transformerParameters(base, meta).getString("k")).isEqualTo("meta");

		StubMapping metaNull = stub(resp(null, null, null, null, null, null, null, true));
		then(this.processor.transformerParameters(base, metaNull).getString("k")).isEqualTo("base");
	}

	// Note: WireMock's ResponseDefinition.isFromConfiguredStub() returns FALSE only for a
	// stub that was explicitly NOT configured (wasConfigured == false) and null otherwise
	// (including when wasConfigured == true). The getter therefore only ever yields
	// Boolean.FALSE or null, never Boolean.TRUE.

	@Test
	void wasConfigured_uses_metadata_value_when_metadata_marks_not_configured() {
		// metadata not configured -> isFromConfiguredStub() == FALSE (non-null)
		StubMapping base = stub(resp(null, null, null, null, null, null, null, true));
		StubMapping meta = stub(resp(null, null, null, null, null, null, null, false));
		then(this.processor.wasConfigured(base, meta)).isEqualTo(false);
	}

	@Test
	void wasConfigured_falls_back_to_base_when_metadata_is_configured() {
		// metadata configured -> isFromConfiguredStub() == null -> use base value
		StubMapping meta = stub(resp(null, null, null, null, null, null, null, true));

		StubMapping baseNotConfigured = stub(resp(null, null, null, null, null, null, null, false));
		then(this.processor.wasConfigured(baseNotConfigured, meta)).isEqualTo(false);

		StubMapping baseConfigured = stub(resp(null, null, null, null, null, null, null, true));
		then(this.processor.wasConfigured(baseConfigured, meta)).isNull();
	}

	@Test
	void setMetadata_merges_into_existing_metadata_preserving_existing_keys() {
		StubMapping base = new StubMapping();
		Metadata existing = new Metadata();
		existing.put("existingKey", "existingValue");
		base.setMetadata(existing);
		StubMapping fromMeta = new StubMapping();
		Metadata source = new Metadata();
		source.put("addedKey", "addedValue");
		fromMeta.setMetadata(source);

		this.processor.setMetadata(base, fromMeta);

		then(base.getMetadata().getString("existingKey")).isEqualTo("existingValue");
		then(base.getMetadata().getString("addedKey")).isEqualTo("addedValue");
	}

	@Test
	void setMetadata_creates_new_metadata_when_absent() {
		StubMapping base = new StubMapping();
		StubMapping fromMeta = new StubMapping();
		Metadata source = new Metadata();
		source.put("addedKey", "addedValue");
		fromMeta.setMetadata(source);

		this.processor.setMetadata(base, fromMeta);

		then(base.getMetadata().getString("addedKey")).isEqualTo("addedValue");
	}

	@Test
	void postProcess_uses_stub_mapping_typed_metadata() {
		Contract contract = new Contract();
		Map<String, Object> map = new HashMap<>();
		StubMapping metaMapping = new StubMapping();
		metaMapping.setResponse(new ResponseDefinitionBuilder().withStatus(200).withFixedDelay(777).build());
		map.put("stubMapping", metaMapping);
		contract.getMetadata().put("wiremock", map);
		StubMapping base = StubMapping.buildFrom(STUB_MAPPING);

		StubMapping result = this.processor.postProcess(base, contract);

		then(result.getResponse().getFixedDelayMilliseconds()).isEqualTo(777);
		then(result.getResponse().getBody()).isEqualTo("pong");
	}

}
