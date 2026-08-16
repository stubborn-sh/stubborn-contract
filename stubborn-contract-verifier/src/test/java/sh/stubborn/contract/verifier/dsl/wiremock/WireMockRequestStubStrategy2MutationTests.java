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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import com.github.tomakehurst.wiremock.matching.RequestPattern;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.Request;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.util.ContentType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutation-focused tests for {@link WireMockRequestStubStrategy}'s body/url strategy
 * resolution: form bodies, regex bodies, dynamic (GString) urls and matching strategies.
 */
class WireMockRequestStubStrategy2MutationTests {

	private static String requestJson(Consumer<Request> requestConsumer, ContentType contentType) {
		Contract contract = Contract.make((dsl) -> {
			dsl.request(requestConsumer::accept);
			dsl.response((resp) -> resp.status(200));
		});
		SingleContractMetadata meta = Mockito.mock(SingleContractMetadata.class);
		Mockito.lenient().when(meta.getEvaluatedInputStubContentType()).thenReturn(contentType);
		RequestPattern pattern = new WireMockRequestStubStrategy(contract, meta).buildClientRequestContent();
		return pattern.toString().replaceAll("\\s+", "");
	}

	@Test
	void dynamic_gstring_url_with_regex_uses_url_matching() {
		String json = requestJson((r) -> {
			r.method("GET");
			r.url(r.$(r.consumer(r.regex("/[0-9]{2}")), r.producer("/12")));
		}, ContentType.JSON);
		assertThat(json).contains("\"urlPattern\":\"/[0-9]{2}\"");
	}

	@Test
	void form_urlencoded_map_body_is_joined_with_ampersand() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("a", 3);
		body.put("b", 4);
		String json = requestJson((r) -> {
			r.method("POST");
			r.url("/form");
			r.headers((h) -> h.header("Content-Type", "application/x-www-form-urlencoded"));
			r.body(body);
		}, ContentType.FORM);
		assertThat(json).contains("\"equalTo\":\"a=3&b=4\"");
	}

	@Test
	void regex_body_with_unknown_content_type_uses_matching_pattern() {
		String json = requestJson((r) -> {
			r.method("POST");
			r.url("/re");
			r.body(Pattern.compile("[0-9]+"));
		}, ContentType.UNKNOWN);
		assertThat(json).contains("\"matches\":\"[0-9]+\"");
	}

	@Test
	void equal_to_json_matching_strategy_body() {
		String json = requestJson((r) -> {
			r.method("POST");
			r.url("/j");
			r.body(r.equalToJson("{\"name\":\"Jan\"}"));
		}, ContentType.JSON);
		assertThat(json).contains("\"equalToJson\":\"{\\\"name\\\":\\\"Jan\\\"}\"");
	}

	@Test
	void equal_to_xml_matching_strategy_body() {
		String json = requestJson((r) -> {
			r.method("POST");
			r.url("/x");
			r.body(r.equalToXml("<foo><name>Jan</name></foo>"));
		}, ContentType.XML);
		assertThat(json).contains("\"equalToXml\":\"<foo><name>Jan</name></foo>\"");
	}

}
