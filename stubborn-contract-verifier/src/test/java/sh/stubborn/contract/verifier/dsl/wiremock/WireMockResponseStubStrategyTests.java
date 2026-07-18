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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import groovy.json.JsonSlurper;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.RegexPatterns;
import sh.stubborn.contract.verifier.file.SingleContractMetadata;
import sh.stubborn.contract.verifier.util.ContentType;
import sh.stubborn.contract.verifier.util.MapConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link WireMockResponseStubStrategy}.
 *
 * @author Marcin Grzejszczak
 */
class WireMockResponseStubStrategyTests {

	private static final Contract PATTERNS_CONTRACT = Contract.make((c) -> {
		c.request((r) -> {
			r.method("GET");
			r.urlPath(r.value(r.regex("/info/[0-9]")), (up) -> up.queryParameters((q) -> {
				q.parameter("limit", r.$(r.consumer(r.equalTo("20")), r.producer(r.equalTo("10"))));
				q.parameter("offset", r.$(r.consumer(r.containing("20")), r.producer(r.equalTo("20"))));
				q.parameter("filter", "email");
				q.parameter("sort", r.equalTo("name"));
				q.parameter("age", r.$(r.consumer(r.notMatching("^\\w*$")), r.producer("99")));
				q.parameter("name", r.$(r.consumer(r.matching("John.*")), r.producer("John.Doe")));
				q.parameter("email", "bob@email.com");
				q.parameter("hello", r.$(r.consumer(r.matching("John.*")), r.producer(r.absent())));
				q.parameter("hello2", r.absent());
			}));
			r.headers((h) -> {
				h.contentType(h.applicationJson());
				h.header("second", "value");
				h.header("third", r.anyAlphaNumeric());
			});
			r.body(Map.of("foo1", r.$(r.consumer(r.regex("[0-9]")), r.producer(1)), "foo2",
					r.$(r.consumer(r.regex("[0-9]"))), "foo3", r.$(r.anyAlphaNumeric()), "foo4",
					r.value(r.regex(RegexPatterns.aDouble())), "foo5", r.value(r.anyDouble()), "foo6", "concrete",
					"duck", 123, "alpha", "abc", "number", 123, "aBoolean", true));
			r.bodyMatchers((bm) -> {
				bm.jsonPath("$.duck", bm.byRegex("[0-9]{3}"));
				bm.jsonPath("$.duck", bm.byEquality());
				bm.jsonPath("$.alpha", bm.byRegex(RegexPatterns.onlyAlphaUnicode()));
				bm.jsonPath("$.alpha", bm.byEquality());
				bm.jsonPath("$.number", bm.byRegex(RegexPatterns.number()));
				bm.jsonPath("$.aBoolean", bm.byRegex(RegexPatterns.anyBoolean()));
			});
		});
		c.response((r) -> {
			r.status(r.OK());
			r.headers((h) -> {
				h.contentType(h.applicationJson());
				h.header("second", "value");
			});
			r.body(Map.of("duck", 123, "alpha", "abc", "number", 123, "aBoolean", true, "date", "2017-01-01",
					"dateTime", "2017-01-01T01:23:45", "time", "01:02:34", "valueWithoutAMatcher", "foo",
					"valueWithTypeMatch", "string", "nullValue", "null"));
			r.bodyMatchers((bm) -> {
				bm.jsonPath("$.duck", bm.byRegex("[0-9]{3}"));
				bm.jsonPath("$.duck", bm.byEquality());
				bm.jsonPath("$.alpha", bm.byRegex(RegexPatterns.onlyAlphaUnicode()));
				bm.jsonPath("$.alpha", bm.byEquality());
				bm.jsonPath("$.number", bm.byRegex(RegexPatterns.number()));
				bm.jsonPath("$.aBoolean", bm.byRegex(RegexPatterns.anyBoolean()));
				bm.jsonPath("$.date", bm.byDate());
				bm.jsonPath("$.dateTime", bm.byTimestamp());
				bm.jsonPath("$.time", bm.byTime());
				bm.jsonPath("$.valueWithTypeMatch", bm.byType());
				bm.jsonPath("$.duck", bm.byCommand("assertThatValueIsANumber($it)"));
				bm.jsonPath("$.nullValue", bm.byNull());
			});
		});
	});

	@Test
	void should_not_quote_floating_point_numbers() {
		Contract contract = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("/foo");
			});
			c.response((r) -> {
				r.status(200);
				r.body(Map.of("value", 1.5));
			});
		});
		SingleContractMetadata metadata = mock(SingleContractMetadata.class);
		when(metadata.getEvaluatedOutputStubContentType()).thenReturn(ContentType.JSON);
		WireMockResponseStubStrategy subject = new WireMockResponseStubStrategy(contract, metadata) {
			@Override
			Function<String, Object> parsingClosureForContentType() {
				return MapConverter.JSON_PARSING_FUNCTION;
			}
		};
		ResponseDefinition content = subject.buildClientResponseContent();
		assertThat(content.getBody()).isEqualTo("{\"value\":1.5}");
	}

	@Test
	void should_not_quote_generated_numbers() {
		Contract contract = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("/foo");
			});
			c.response((r) -> {
				r.status(200);
				r.body(Map.of("number", r.anyNumber(), "integer", r.anyInteger(), "positiveInt", r.anyPositiveInt(),
						"double", r.anyDouble()));
			});
		});
		SingleContractMetadata metadata = mock(SingleContractMetadata.class);
		when(metadata.getEvaluatedOutputStubContentType()).thenReturn(ContentType.JSON);
		WireMockResponseStubStrategy subject = new WireMockResponseStubStrategy(contract, metadata) {
			@Override
			Function<String, Object> parsingClosureForContentType() {
				return MapConverter.JSON_PARSING_FUNCTION;
			}
		};
		ResponseDefinition content = subject.buildClientResponseContent();
		Map<?, ?> body = (Map<?, ?>) new JsonSlurper().parseText(content.getBody());
		assertThat(body.get("number")).isInstanceOf(Number.class);
		assertThat(body.get("integer")).isInstanceOf(Integer.class);
		assertThat(body.get("positiveInt")).isInstanceOf(Integer.class);
		assertThat(body.get("double")).isInstanceOf(Number.class);
	}

	@Test
	void should_not_quote_numbers_booleans_and_null_inside_arrays() {
		Contract contract = Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("/foo");
			});
			c.response((r) -> {
				r.status(200);
				r.body(Arrays.asList(r.anyPositiveInt(), r.anyInteger(), true, r.anyNumber(), null, "value"));
			});
		});
		SingleContractMetadata metadata = mock(SingleContractMetadata.class);
		when(metadata.getEvaluatedOutputStubContentType()).thenReturn(ContentType.JSON);
		WireMockResponseStubStrategy subject = new WireMockResponseStubStrategy(contract, metadata) {
			@Override
			Function<String, Object> parsingClosureForContentType() {
				return MapConverter.JSON_PARSING_FUNCTION;
			}
		};
		ResponseDefinition content = subject.buildClientResponseContent();
		List<?> body = (List<?>) new JsonSlurper().parseText(content.getBody());
		assertThat(body.get(0)).isInstanceOf(Integer.class);
		assertThat(body.get(1)).isInstanceOf(Integer.class);
		assertThat(body.get(2)).isInstanceOf(Boolean.class);
		assertThat(body.get(3)).isInstanceOf(Number.class);
		assertThat(body.get(4)).isNull();
		assertThat(body.get(5)).isInstanceOf(String.class);
	}

	@Test
	void should_convert_patterns_to_proper_value() {
		WireMockRequestStubStrategy requestSubject = new WireMockRequestStubStrategy(PATTERNS_CONTRACT, null) {
			@Override
			protected ContentType contentType(SingleContractMetadata singleContractMetadata) {
				return ContentType.JSON;
			}
		};
		assertThatCode(requestSubject::buildClientRequestContent).doesNotThrowAnyException();

		WireMockResponseStubStrategy responseSubject = new WireMockResponseStubStrategy(PATTERNS_CONTRACT, null) {
			@Override
			protected ContentType contentType(SingleContractMetadata singleContractMetadata) {
				return ContentType.JSON;
			}

			@Override
			Function<String, Object> parsingClosureForContentType() {
				return MapConverter.JSON_PARSING_FUNCTION;
			}
		};
		assertThatCode(responseSubject::buildClientResponseContent).doesNotThrowAnyException();
	}

}
