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

package sh.stubborn.contract.verifier.compat;

import java.io.File;
import java.util.Collection;
import java.util.Objects;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.RegexProperty;
import sh.stubborn.contract.verifier.dsl.wiremock.WireMockStubStrategy;
import sh.stubborn.contract.verifier.file.ContractMetadata;
import sh.stubborn.contract.verifier.util.ContractVerifierDslConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Backward-compatibility gate for reading <em>legacy Spring Cloud Contract</em> Groovy
 * DSL contracts — the ones authored against
 * {@code org.springframework.cloud.contract.spec.Contract} with
 * {@code value(consumer(...), producer(...))} dynamic values, {@code regex(...)}
 * matchers, GString-interpolated bodies and {@code bodyMatchers} blocks.
 *
 * <p>
 * These are the exact idioms this project promises to keep parsing unchanged while the
 * Groovy runtime is relocated out of the framework-agnostic core. Each test parses an
 * SCC-authored contract and asserts both the resulting {@link Contract} model and the
 * generated WireMock stub, so a regression in either the parse path or the GString→stub
 * pipeline fails loudly.
 *
 * @author Marcin Grzejszczak
 */
class SccLegacyDslCompatibilityTests {

	@TempDir
	File rootFolder;

	private Contract parseSingle(String groovyDsl) {
		Collection<Contract> contracts = ContractVerifierDslConverter.convertAsCollection(this.rootFolder, groovyDsl);
		assertThat(contracts).hasSize(1);
		return contracts.iterator().next();
	}

	private String toClientStubJson(Contract contract) {
		StubMapping stub = new WireMockStubStrategy("Test", new ContractMetadata(null, false, 0, null, contract),
				contract)
			.toWireMockClientStub();
		return Objects.requireNonNull(stub).toString();
	}

	@Test
	void readsAnSccContractAuthoredAgainstTheSpringCloudContractAlias() {
		// The historical SCC package name must keep resolving to make { } .
		Contract contract = parseSingle("""
				org.springframework.cloud.contract.spec.Contract.make {
					request {
						method 'PUT'
						url '/api/v1/users/1'
						headers { contentType(applicationJson()) }
						body([status: 'OK'])
					}
					response {
						status 200
						body([status: 'OK'])
						headers { contentType(applicationJson()) }
					}
				}
				""");

		assertThat(Objects.requireNonNull(contract.getRequest()).getMethod().getClientValue()).isEqualTo("PUT");
		assertThat(contract.getRequest().getUrl().getClientValue()).isEqualTo("/api/v1/users/1");
		assertThat(Objects.requireNonNull(contract.getResponse()).getStatus().getClientValue()).isEqualTo(200);
	}

	@Test
	void readsDynamicValuesAndRegexMatchersFromAMapBody() {
		Contract contract = parseSingle("""
				org.springframework.cloud.contract.spec.Contract.make {
					request {
						method 'POST'
						url '/users'
						headers { contentType(applicationJson()) }
						body([
							id: value(consumer(regex('[0-9]+')), producer('123')),
							name: 'John'
						])
					}
					response {
						status 200
						body([id: '123'])
					}
				}
				""");

		Object idClientValue = extractMapEntryClientValue(contract, "id");
		assertThat(idClientValue).isInstanceOf(RegexProperty.class);
		assertThat(((RegexProperty) idClientValue).pattern()).isEqualTo("[0-9]+");

		// The stub must match the request body against that same regex.
		String stubJson = toClientStubJson(contract);
		assertThat(stubJson).contains("[0-9]+");
	}

	@Test
	void readsAGStringInterpolatedJsonBody() {
		// A GString body — the single most important legacy Groovy idiom; the
		// interpolated dynamic value must survive into the stub's request matcher.
		Contract contract = parseSingle("""
				org.springframework.cloud.contract.spec.Contract.make {
					request {
						method 'POST'
						url '/api/v1/xxxx'
						headers { contentType(applicationJson()) }
						body('''{ "id": "${value(consumer(regex('[0-9]{3,}')), producer('123'))}", "name": "John" }''')
					}
					response {
						status 200
						body('''{ "status": "OK" }''')
						headers { contentType(applicationJson()) }
					}
				}
				""");

		String stubJson = toClientStubJson(contract);
		assertThat(stubJson).contains("/api/v1/xxxx");
		// The GString-interpolated regex must appear in the request matching section.
		assertThat(stubJson).contains("[0-9]{3,}");
		// And the literal parts of the GString body must be preserved.
		assertThat(stubJson).contains("name");
	}

	@Test
	void readsAGStringInterpolatedXmlBody() {
		Contract contract = parseSingle(
				"""
						org.springframework.cloud.contract.spec.Contract.make {
							request {
								method 'POST'
								url '/xml'
								headers { header('Content-Type', 'application/xml') }
								body('''<user><id>${value(consumer(regex('[0-9]+')), producer('123'))}</id><name>John</name></user>''')
							}
							response {
								status 200
							}
						}
						""");

		String stubJson = toClientStubJson(contract);
		assertThat(stubJson).contains("[0-9]+");
		assertThat(stubJson).contains("John");
	}

	@Test
	void readsAGStringInterpolatedUrl() {
		Contract contract = parseSingle("""
				org.springframework.cloud.contract.spec.Contract.make {
					request {
						method 'GET'
						url value(consumer(regex('/users/[0-9]+')), producer('/users/42'))
					}
					response {
						status 200
						body('''{ "ok": true }''')
					}
				}
				""");

		String stubJson = toClientStubJson(contract);
		assertThat(stubJson).contains("/users/[0-9]+");
	}

	@Test
	void readsAGStringBodyWithMultipleInterpolatedValues() {
		Contract contract = parseSingle(
				"""
						org.springframework.cloud.contract.spec.Contract.make {
							request {
								method 'POST'
								url '/multi'
								headers { contentType(applicationJson()) }
								body('''{ "id": "${value(consumer(regex('[0-9]+')), producer('1'))}", "code": "${value(consumer(regex('[A-Z]{2}')), producer('AB'))}", "fixed": "x" }''')
							}
							response {
								status 200
							}
						}
						""");

		String stubJson = toClientStubJson(contract);
		assertThat(stubJson).contains("[0-9]+");
		assertThat(stubJson).contains("[A-Z]{2}");
		assertThat(stubJson).contains("fixed");
	}

	@Test
	void readsBodyMatchersFromAnSccContract() {
		Contract contract = parseSingle("""
				org.springframework.cloud.contract.spec.Contract.make {
					request {
						method 'POST'
						url '/check'
						headers { contentType(applicationJson()) }
						body([age: '99'])
						bodyMatchers {
							jsonPath('$.age', byRegex('[0-9]+'))
						}
					}
					response {
						status 200
						body([status: 'OK'])
					}
				}
				""");

		assertThat(Objects.requireNonNull(contract.getRequest()).getBodyMatchers().matchers()).anySatisfy((m) -> {
			assertThat(m.path()).isEqualTo("$.age");
			assertThat(((RegexProperty) m.value()).pattern()).isEqualTo("[0-9]+");
		});
	}

	@SuppressWarnings("unchecked")
	private static Object extractMapEntryClientValue(Contract contract, String key) {
		Object body = Objects.requireNonNull(Objects.requireNonNull(contract.getRequest()).getBody()).getClientValue();
		assertThat(body).isInstanceOf(java.util.Map.class);
		Object entry = ((java.util.Map<String, Object>) body).get(key);
		return (entry instanceof sh.stubborn.contract.spec.internal.DslProperty)
				? ((sh.stubborn.contract.spec.internal.DslProperty<?>) entry).getClientValue() : entry;
	}

}
