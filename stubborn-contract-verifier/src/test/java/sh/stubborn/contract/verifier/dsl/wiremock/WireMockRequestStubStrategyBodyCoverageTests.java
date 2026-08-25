/*
 * Copyright 2013-2026 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.github.tomakehurst.wiremock.matching.RequestPattern;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.DynamicString;
import sh.stubborn.contract.spec.internal.Request;
import sh.stubborn.contract.verifier.dsl.ContractVerifierMetadata;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.util.ContentType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Body-shape coverage for {@link WireMockRequestStubStrategy}.
 * <p>
 * The mutation gate runs over every changed core class, so a change anywhere in the
 * strategy is measured against the whole of it. These exercise the body shapes the
 * existing suites leave untouched — a form-encoded list, an array, and a collection
 * carrying a pattern — which is where the class's unkilled mutants live.
 */
class WireMockRequestStubStrategyBodyCoverageTests {

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
	void form_urlencoded_list_body_is_joined_with_ampersand() {
		// The map form is covered elsewhere; the list form is the other half of the
		// same branch and reaches the collector inside it.
		String json = requestJson((r) -> {
			r.method("POST");
			r.url("/form");
			r.headers((h) -> h.header("Content-Type", "application/x-www-form-urlencoded"));
			r.body(List.of("a=3", "b=4"));
		}, ContentType.FORM);

		assertThat(json).contains("\"equalTo\":\"a=3&b=4\"");
	}

	@Test
	void a_list_body_holding_a_pattern_is_matched_as_a_regex() {
		// Drives the collection branch and the boolean reduce over its elements.
		List<Object> body = new ArrayList<>();
		body.add("plain");
		body.add(java.util.regex.Pattern.compile("[0-9]+"));

		String json = requestJson((r) -> {
			r.method("POST");
			r.url("/re");
			r.body(body);
		}, ContentType.UNKNOWN);

		assertThat(json).contains("\"matches\"");
		assertThat(json).doesNotContain("\"equalTo\"");
	}

	@Test
	void a_list_body_holding_no_pattern_is_not_matched_as_a_regex() {
		// The other side of the reduce: every element answers false.
		List<Object> body = new ArrayList<>();
		body.add("plain");
		body.add("also plain");

		String json = requestJson((r) -> {
			r.method("POST");
			r.url("/re");
			r.body(body);
		}, ContentType.UNKNOWN);

		assertThat(json).contains("\"equalTo\"");
		assertThat(json).doesNotContain("\"matches\"");
	}

	/**
	 * A stand-in for the interpolated string the Groovy DSL produces. It is an interface,
	 * so the GString-shaped paths through the strategy can be reached from Java without a
	 * Groovy contract.
	 */
	private static DynamicString gstring(String[] strings, Object[] values) {
		return new DynamicString() {
			@Override
			public Object[] getValues() {
				return values;
			}

			@Override
			public String[] getStrings() {
				return strings;
			}

			@Override
			public int length() {
				return toString().length();
			}

			@Override
			public char charAt(int index) {
				return toString().charAt(index);
			}

			@Override
			public CharSequence subSequence(int start, int end) {
				return toString().subSequence(start, end);
			}

			@Override
			public String toString() {
				StringBuilder out = new StringBuilder();
				for (int i = 0; i < strings.length; i++) {
					out.append(strings[i]);
					if (i < values.length) {
						out.append(values[i]);
					}
				}
				return out.toString();
			}
		};
	}

	@Test
	void an_interpolated_url_holding_a_pattern_becomes_a_url_regex() {
		String json = requestJson((r) -> {
			r.method("GET");
			r.url(r.$(r.consumer(
					gstring(new String[] { "/id/", "" }, new Object[] { java.util.regex.Pattern.compile("[0-9]+") })),
					r.producer("/id/12")));
		}, ContentType.JSON);

		assertThat(json).contains("urlPattern");
	}

	@Test
	void an_interpolated_url_holding_no_pattern_stays_a_plain_url() {
		String json = requestJson((r) -> {
			r.method("GET");
			r.url(r.$(r.consumer(gstring(new String[] { "/id/", "" }, new Object[] { "12" })), r.producer("/id/12")));
		}, ContentType.JSON);

		assertThat(json).contains("\"url\":\"/id/12\"");
	}

	@Test
	void an_interpolated_body_is_flattened_to_its_string_value() {
		String json = requestJson((r) -> {
			r.method("POST");
			r.url("/g");
			r.body(gstring(new String[] { "hello ", "" }, new Object[] { "world" }));
		}, ContentType.TEXT);

		assertThat(json).contains("\"equalTo\":\"helloworld\"");
		assertThat(json).doesNotContain("\"matches\"");
	}

	@Test
	void an_interpolated_body_holding_a_pattern_is_matched_as_a_regex() {
		String json = requestJson((r) -> {
			r.method("POST");
			r.url("/g");
			r.body(gstring(new String[] { "", "" }, new Object[] { java.util.regex.Pattern.compile("[0-9]+") }));
		}, ContentType.UNKNOWN);

		assertThat(json).contains("\"matches\"");
		assertThat(json).doesNotContain("\"equalTo\"");
	}

	@Test
	void a_contract_carrying_verifier_metadata_installs_the_extra_matcher() {
		Contract contract = Contract.make((dsl) -> {
			dsl.metadata(Map.of(ContractVerifierMetadata.METADATA_KEY, Map.of("tool", "custom")));
			dsl.request((r) -> {
				r.method("POST");
				r.url("/meta");
				r.body("plain");
			});
			dsl.response((resp) -> resp.status(200));
		});
		SingleContractMetadata meta = Mockito.mock(SingleContractMetadata.class);
		Mockito.lenient().when(meta.getEvaluatedInputStubContentType()).thenReturn(ContentType.TEXT);

		String json = new WireMockRequestStubStrategy(contract, meta).buildClientRequestContent()
			.toString()
			.replaceAll("\\s+", "");

		assertThat(json).contains(SpringCloudContractRequestMatcher.NAME);
		assertThat(json).doesNotContain("\"equalTo\":\"plain\"");
	}

	@Test
	void a_contract_with_no_tool_in_its_metadata_still_matches_on_the_body() {
		Contract contract = Contract.make((dsl) -> {
			dsl.metadata(Map.of(ContractVerifierMetadata.METADATA_KEY, Map.of()));
			dsl.request((r) -> {
				r.method("POST");
				r.url("/meta");
				r.body("plain");
			});
			dsl.response((resp) -> resp.status(200));
		});
		SingleContractMetadata meta = Mockito.mock(SingleContractMetadata.class);
		Mockito.lenient().when(meta.getEvaluatedInputStubContentType()).thenReturn(ContentType.TEXT);

		String json = new WireMockRequestStubStrategy(contract, meta).buildClientRequestContent()
			.toString()
			.replaceAll("\\s+", "");

		assertThat(json).contains(SpringCloudContractRequestMatcher.NAME);
		assertThat(json).contains("\"equalTo\":\"plain\"");
	}

	@Test
	void a_regex_property_body_is_matched_as_a_regex() {
		// A regex property takes its own branch, distinct from a bare Pattern.
		String json = requestJson((r) -> {
			r.method("POST");
			r.url("/re");
			r.body(r.regex("[0-9]+"));
		}, ContentType.UNKNOWN);

		assertThat(json).contains("\"matches\":\"[0-9]+\"");
		assertThat(json).doesNotContain("\"equalTo\"");
	}

	@Test
	void an_interpolated_body_resolves_a_dsl_property_to_its_client_value() {
		// The value inside the interpolation is a consumer/producer pair, so the
		// client side is what has to survive into the stub.
		String json = requestJson((r) -> {
			r.method("POST");
			r.url("/g");
			r.body(gstring(new String[] { "id=", "" }, new Object[] { r.$(r.consumer("42"), r.producer("99")) }));
		}, ContentType.TEXT);

		assertThat(json).contains("\"equalTo\":\"id=42\"");
		assertThat(json).doesNotContain("99");
	}

}
