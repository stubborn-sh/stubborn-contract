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

package sh.stubborn.contract.spec.internal;

import java.util.Map;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Marcin Grzejszczak
 */
class ContractTests {

	@Test
	void shouldWorkForHttp() {
		assertThatCode(() -> Contract.make(c -> {
			c.request(r -> {
				r.url("/foo");
				r.method("PUT");
				r.headers(h -> h.header("foo", "bar"));
				r.body(Map.of("foo", "bar"));
			});
			c.response(resp -> {
				resp.status(200);
				resp.headers(h -> h.header("foo2", "bar"));
				resp.body(Map.of("foo2", "bar"));
			});
		})).doesNotThrowAnyException();
	}

	@Test
	void shouldFailWhenNoMethodIsPresent() {
		assertThatThrownBy(() -> Contract.make(c -> {
			c.request(r -> r.url("/foo"));
			c.response(resp -> resp.status(200));
		})).isInstanceOf(IllegalStateException.class).hasMessageContaining("Method is missing for HTTP contract");
	}

	@Test
	void shouldFailWhenNoUrlIsPresent() {
		assertThatThrownBy(() -> Contract.make(c -> {
			c.request(r -> r.method("GET"));
			c.response(resp -> resp.status(200));
		})).isInstanceOf(IllegalStateException.class).hasMessageContaining("URL is missing for HTTP contract");
	}

	@Test
	void shouldFailWhenNoStatusIsPresent() {
		assertThatThrownBy(() -> Contract.make(c -> {
			c.request(r -> {
				r.url("/foo");
				r.method("GET");
			});
			c.response(resp -> {
			});
		})).isInstanceOf(IllegalStateException.class).hasMessageContaining("Status is missing for HTTP contract");
	}

	@Test
	void shouldSetADescription() {
		// tag::description[]
		sh.stubborn.contract.spec.Contract.make(c -> c.description("""
				given:
					An input
				when:
					Sth happens
				then:
					Output
				"""));
		// end::description[]
	}

	@Test
	void shouldSetAName() {
		// tag::name[]
		sh.stubborn.contract.spec.Contract.make(c -> c.name("some_special_name"));
		// end::name[]
	}

	@Test
	void shouldMarkAContractIgnored() {
		// tag::ignored[]
		sh.stubborn.contract.spec.Contract.make(c -> c.ignored());
		// end::ignored[]
	}

	@Test
	void shouldMarkAContractInProgress() {
		// tag::in_progress[]
		sh.stubborn.contract.spec.Contract.make(c -> c.inProgress());
		// end::in_progress[]
	}

	@Test
	void shouldMakeEqualsAndHashcodeWorkProperlyForUrl() {
		Contract a = Contract.make(c -> c.request(r -> {
			r.method("GET");
			r.url("/1");
		}));
		Contract b = Contract.make(c -> c.request(r -> {
			r.method("GET");
			r.url("/1");
		}));
		assertThat(a).isEqualTo(b);
	}

	@Test
	void shouldMakeEqualsAndHashcodeWorkProperlyForUrlWithConsumerProducer() {
		Contract a = Contract.make(c -> c.request(r -> {
			r.method("GET");
			r.url(r.$(r.c("/1"), r.p("/1")));
		}));
		Contract b = Contract.make(c -> c.request(r -> {
			r.method("GET");
			r.url(r.$(r.c("/1"), r.p("/1")));
		}));
		assertThat(a).isEqualTo(b);
	}

	@Test
	void shouldReturnTrueWhenComparingTwoEqualContractsWithGstring() {
		int index = 1;
		Contract a = Contract.make(c -> {
			c.request(r -> {
				r.method(r.PUT());
				r.headers(h -> h.contentType(h.applicationJson()));
				r.url("/" + index);
			});
			c.response(resp -> resp.status(resp.OK()));
		});
		Contract b = Contract.make(c -> {
			c.request(r -> {
				r.method(r.PUT());
				r.headers(h -> h.contentType(h.applicationJson()));
				r.url("/" + index);
			});
			c.response(resp -> resp.status(resp.OK()));
		});
		assertThat(a.getRequest().getMethod()).isEqualTo(b.getRequest().getMethod());
		assertThat(a.getRequest().getUrl()).isEqualTo(b.getRequest().getUrl());
		assertThat(a.getRequest().getHeaders().getEntries().iterator().next())
			.isEqualTo(b.getRequest().getHeaders().getEntries().iterator().next());
		assertThat(a.getRequest().getHeaders().getEntries()).isEqualTo(b.getRequest().getHeaders().getEntries());
		assertThat(a.getRequest().getHeaders()).isEqualTo(b.getRequest().getHeaders());
		assertThat(a.getRequest()).isEqualTo(b.getRequest());
		assertThat(a.getResponse().getStatus()).isEqualTo(b.getResponse().getStatus());
		assertThat(a.getResponse()).isEqualTo(b.getResponse());
		assertThat(a).isEqualTo(b);
	}

	@Test
	void shouldReturnFalseWhenComparingTwoUnequalContractsWithGstring() {
		int index = 1;
		Contract a = Contract.make(c -> {
			c.request(r -> {
				r.method(r.PUT());
				r.headers(h -> h.contentType(h.applicationJson()));
				r.url("/" + index);
			});
			c.response(resp -> resp.status(resp.OK()));
		});
		int index2 = 2;
		Contract b = Contract.make(c -> {
			c.request(r -> {
				r.method(r.PUT());
				r.headers(h -> h.contentType(h.applicationJson()));
				r.url("/" + index2);
			});
			c.response(resp -> resp.status(resp.OK()));
		});
		assertThat(a).isNotEqualTo(b);
	}

	@Test
	void shouldReturnTrueWhenComparingTwoEqualComplexContracts() {
		Contract a = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("/path");
				r.headers(h -> {
					h.header("Accept", r.$(r.consumer(r.regex("text/.*")), r.producer("text/plain")));
					h.header("X-Custom-Header", r.$(r.consumer(r.regex("^.*2134.*$")), r.producer("121345")));
				});
			});
			c.response(resp -> {
				resp.status(resp.OK());
				resp.body(Map.of("id", Map.of("value", "132"), "surname", "Kowalsky", "name", "Jan", "created",
						"2014-02-02 12:23:43"));
				resp.headers(h -> h.header("Content-Type", "text/plain"));
			});
		});
		Contract b = Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("/path");
				r.headers(h -> {
					h.header("Accept", r.$(r.consumer(r.regex("text/.*")), r.producer("text/plain")));
					h.header("X-Custom-Header", r.$(r.consumer(r.regex("^.*2134.*$")), r.producer("121345")));
				});
			});
			c.response(resp -> {
				resp.status(resp.OK());
				resp.body(Map.of("id", Map.of("value", "132"), "surname", "Kowalsky", "name", "Jan", "created",
						"2014-02-02 12:23:43"));
				resp.headers(h -> h.header("Content-Type", "text/plain"));
			});
		});
		assertThat(a.getRequest().getMethod()).isEqualTo(b.getRequest().getMethod());
		assertThat(a.getRequest().getUrl()).isEqualTo(b.getRequest().getUrl());
		assertThat(a.getRequest().getHeaders().getEntries().iterator().next())
			.isEqualTo(b.getRequest().getHeaders().getEntries().iterator().next());
		assertThat(a.getRequest().getHeaders().getEntries().stream().reduce((x, y) -> y).orElseThrow())
			.isEqualTo(b.getRequest().getHeaders().getEntries().stream().reduce((x, y) -> y).orElseThrow());
		assertThat(a.getRequest().getHeaders().getEntries()).isEqualTo(b.getRequest().getHeaders().getEntries());
		assertThat(a.getRequest().getHeaders()).isEqualTo(b.getRequest().getHeaders());
		assertThat(a.getRequest().getBody()).isEqualTo(b.getRequest().getBody());
		assertThat(a.getRequest()).isEqualTo(b.getRequest());
		assertThat(a.getResponse().getStatus()).isEqualTo(b.getResponse().getStatus());
		assertThat(a.getResponse().getHeaders().getEntries().iterator().next())
			.isEqualTo(b.getResponse().getHeaders().getEntries().iterator().next());
		assertThat(a.getResponse().getBody()).isEqualTo(b.getResponse().getBody());
		assertThat(a.getResponse()).isEqualTo(b.getResponse());
		assertThat(a).isEqualTo(b);
	}

	@Test
	void shouldWorkWithOptionalAndNullValueOfAField() {
		Contract contract = Contract.make(c -> {
			c.description("Creating user");
			c.name("Create user");
			c.request(r -> {
				r.method("POST");
				r.url("/api/user");
				r.body(Map.of("address",
						r.$(r.consumer(r.optional(r.regex(r.alphaNumeric()))), r.producer((Object) null)), "name",
						r.$(r.consumer(r.optional(r.regex(r.alphaNumeric()))), r.producer(""))));
				r.headers(h -> h.contentType(h.applicationJson()));
			});
			c.response(resp -> resp.status(201));
		});
		assertThat(contract).isNotNull();
	}

	@Test
	void shouldFailWhenRegexDoNotMatchTheConcreteValue() {
		assertThatThrownBy(() -> Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("/any");
			});
			c.response(resp -> {
				resp.status(resp.OK());
				resp.body(
						Map.of("time", resp.$(resp.p(resp.regex(resp.iso8601WithOffset())), resp.c("thisIsNotADate"))));
			});
		})).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldWorkFineWhenDealingWithAnyOf() {
		assertThatCode(() -> Contract.make(c -> {
			c.request(r -> {
				r.method("GET");
				r.url("/any");
				r.body(Map.of("foo",
						r.$(r.consumer(r.optional(r.anyOf("WORKS", "MIGHTY", "DESPAIR"))), r.producer("DESPAIR"))));
			});
			c.response(resp -> resp.status(resp.OK()));
		})).doesNotThrowAnyException();
	}

}
