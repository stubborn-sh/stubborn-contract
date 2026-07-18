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

package sh.stubborn.contract.verifier.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.verifier.util.ContractVerifierUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ContractVerifierDslConverter}.
 *
 * @author Marcin Grzejszczak
 */
class ContractVerifierDslConverterTests {

	private final File singleContract = fileFor("/contract.groovy");

	private final File singleContractJava = fileFor("/contractsToCompile/contract.java");

	private final File singleContractRestJava = fileFor("/contractsToCompile/contract_rest.java");

	private final File multipleContracts = fileFor("/multiple_contracts.groovy");

	private final File invalidContract = fileFor("/contract.yml");

	private final Contract expectedSingleContract = Contract.make((c) -> {
		c.name("contract");
		c.request((r) -> {
			r.method("PUT");
			r.headers((h) -> h.contentType(h.applicationJson()));
			r.body(" { \"status\" : \"OK\" } ");
			r.url("/1");
		});
		c.response((resp) -> {
			resp.status(resp.OK());
			resp.body(" { \"status\" : \"OK\" } ");
			resp.headers((h) -> h.contentType(h.textPlain()));
		});
	});

	private final Contract expectedSingleContractForJava = Contract.make((c) -> {
		c.description("Some description");
		c.name("some name");
		c.priority(8);
		c.ignored();
		c.request((r) -> {
			r.url("/foo", (u) -> u.queryParameters((q) -> {
				q.parameter("a", "b");
				q.parameter("b", "c");
			}));
			r.method(r.PUT());
			r.headers((h) -> {
				h.header("foo", r.value(r.client(r.regex("bar")), r.server("bar")));
				h.header("fooReq", "baz");
			});
			r.body(ContractVerifierUtil.map().entry("foo", "bar"));
			r.bodyMatchers((bm) -> bm.jsonPath("$.foo", bm.byRegex("bar")));
		});
		c.response((resp) -> {
			resp.fixedDelayMilliseconds(1000);
			resp.status(resp.OK());
			resp.headers((h) -> {
				h.header("foo2", resp.value(resp.server(resp.regex("bar")), resp.client("bar")));
				h.header("foo3", resp.value(resp.server(resp.execute("andMeToo($it)")), resp.client("foo33")));
				h.header("fooRes", "baz");
			});
			resp.body(ContractVerifierUtil.map().entry("foo2", "bar").entry("foo3", "baz").entry("nullValue", null));
			resp.bodyMatchers((bm) -> {
				bm.jsonPath("$.foo2", bm.byRegex("bar"));
				bm.jsonPath("$.foo3", bm.byCommand("executeMe($it)"));
				bm.jsonPath("$.nullValue", bm.byNull());
			});
		});
	});

	private final Contract expectedSingleContractForText = Contract.make((c) -> {
		c.request((r) -> {
			r.method("PUT");
			r.headers((h) -> h.contentType(h.applicationJson()));
			r.body(" { \"status\" : \"OK\" } ");
			r.url("/1");
		});
		c.response((resp) -> {
			resp.status(resp.OK());
			resp.body(" { \"status\" : \"OK\" } ");
			resp.headers((h) -> h.contentType(h.textPlain()));
		});
	});

	private List<Contract> expectedMultipleContracts() {
		return IntStream.rangeClosed(1, 2).mapToObj((index) -> Contract.make((c) -> {
			c.name("multiple_contracts_" + (index - 1));
			c.request((r) -> {
				r.method("PUT");
				r.headers((h) -> h.contentType(h.applicationJson()));
				r.body(" { \"status\" : \"OK\" } ");
				r.url("/" + index);
			});
			c.response((resp) -> {
				resp.status(resp.OK());
				resp.body(" { \"status\" : \"OK\" } ");
				resp.headers((h) -> h.contentType(h.textPlain()));
			});
		})).toList();
	}

	private List<Contract> expectedMultipleContractsForText() {
		return IntStream.rangeClosed(1, 2).mapToObj((index) -> Contract.make((c) -> {
			c.request((r) -> {
				r.method("PUT");
				r.headers((h) -> h.contentType(h.applicationJson()));
				r.body(" { \"status\" : \"OK\" } ");
				r.url("/" + index);
			});
			c.response((resp) -> {
				resp.status(resp.OK());
				resp.body(" { \"status\" : \"OK\" } ");
				resp.headers((h) -> h.contentType(h.textPlain()));
			});
		})).toList();
	}

	@Test
	void should_convert_file_to_a_list_of_Contracts() {
		List<Contract> contracts = (List<Contract>) ContractVerifierDslConverter.convertAsCollection(new File("/"),
				this.multipleContracts);
		assertThat(contracts).hasSize(2);
		assertThat(contracts.get(0).getName()).isEqualTo("multiple_contracts_0");
		assertThat(contracts.get(0).getRequest().getMethod().getClientValue()).isEqualTo("PUT");
		assertThat(contracts.get(0).getRequest().getUrl().getClientValue().toString()).isEqualTo("/1");
		assertThat(contracts.get(1).getName()).isEqualTo("multiple_contracts_1");
		assertThat(contracts.get(1).getRequest().getMethod().getClientValue()).isEqualTo("PUT");
		assertThat(contracts.get(1).getRequest().getUrl().getClientValue().toString()).isEqualTo("/2");
	}

	@Test
	void should_convert_text_to_a_list_of_Contracts() throws Exception {
		String text = java.nio.file.Files.readString(this.multipleContracts.toPath());
		List<Contract> contracts = (List<Contract>) ContractVerifierDslConverter.convertAsCollection(new File("/"),
				text);
		assertThat(contracts).hasSize(2);
		assertThat(contracts.get(0).getName()).isNull();
		assertThat(contracts.get(0).getRequest().getMethod().getClientValue()).isEqualTo("PUT");
		assertThat(contracts.get(0).getRequest().getUrl().getClientValue().toString()).isEqualTo("/1");
		assertThat(contracts.get(1).getName()).isNull();
		assertThat(contracts.get(1).getRequest().getMethod().getClientValue()).isEqualTo("PUT");
		assertThat(contracts.get(1).getRequest().getUrl().getClientValue().toString()).isEqualTo("/2");
	}

	@Test
	void should_throw_an_exception_when_an_invalid_file_is_parsed() throws Exception {
		String text = java.nio.file.Files.readString(this.invalidContract.toPath());
		assertThatThrownBy(() -> ContractVerifierDslConverter.convertAsCollection(new File("/"), text))
			.isInstanceOf(DslParseException.class);
	}

	@Test
	void should_throw_an_exception_with_file_path_when_an_invalid_file_is_parsed() {
		DslParseException ex = org.assertj.core.api.Assertions.catchThrowableOfType(DslParseException.class,
				() -> ContractVerifierDslConverter.convertAsCollection(new File("/"), this.invalidContract));
		assertThat(ex).isNotNull();
		assertThat(ex.toString()).contains("contract.yml");
	}

	@Test
	void should_throw_an_exception_when_a_non_existent_file_is_parsed() {
		DslParseException ex = org.assertj.core.api.Assertions.catchThrowableOfType(DslParseException.class,
				() -> ContractVerifierDslConverter.convertAsCollection(new File("/"), new File("/foo/bar/baz.foo")));
		assertThat(ex).isNotNull();
		assertThat(ex.getCause()).isInstanceOf(FileNotFoundException.class);
	}

	@Test
	void should_convert_file_to_a_list_of_Contracts_when_theres_only_one_declared_contract() {
		Collection<Contract> contract = ContractVerifierDslConverter.convertAsCollection(new File("/"),
				this.singleContract);
		assertThat(contract).isEqualTo(List.of(this.expectedSingleContract));
	}

	@Test
	void should_convert_file_to_a_list_of_Contracts_when_theres_only_one_declared_java_contract() {
		Collection<Contract> contract = ContractVerifierDslConverter.convertAsCollection(new File("/"),
				this.singleContractJava);
		assertThat(contract).isEqualTo(List.of(this.expectedSingleContract));
	}

	@Test
	void should_convert_file_to_a_list_of_Contracts_for_a_REST_contract_for_docs() {
		Collection<Contract> contract = ContractVerifierDslConverter.convertAsCollection(new File("/"),
				this.singleContractRestJava);
		assertThat(contract).isEqualTo(List.of(this.expectedSingleContractForJava));
	}

	@Test
	void should_convert_text_to_a_list_of_Contracts_when_theres_only_one_declared_contract() throws Exception {
		String text = java.nio.file.Files.readString(this.singleContract.toPath());
		Collection<Contract> contract = ContractVerifierDslConverter.convertAsCollection(new File("/"), text);
		assertThat(contract).isEqualTo(List.of(this.expectedSingleContractForText));
	}

	private static File fileFor(String resource) {
		try {
			return new File(ContractVerifierDslConverterTests.class.getResource(resource).toURI());
		}
		catch (URISyntaxException ex) {
			throw new RuntimeException(ex);
		}
	}

}
