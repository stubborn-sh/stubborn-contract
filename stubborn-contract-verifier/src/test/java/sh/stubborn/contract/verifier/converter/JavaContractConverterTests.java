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

package sh.stubborn.contract.verifier.converter;

import java.io.File;
import java.util.Collection;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.spec.internal.ExecutionProperty;
import sh.stubborn.contract.spec.internal.FromFileProperty;
import sh.stubborn.contract.spec.internal.MatchingType;
import sh.stubborn.contract.spec.internal.RegexProperty;
import sh.stubborn.contract.spec.internal.Url;
import sh.stubborn.contract.verifier.util.ContractVerifierDslConverter;
import sh.stubborn.contract.verifier.util.MapConverter;

import static org.assertj.core.api.Assertions.assertThat;

class JavaContractConverterTests {

	private static final File javaRestFile = fileFor("/contractsToCompile/contract_rest.java");

	private static final File javaRestWithTagsFile = fileFor("/contractsToCompile/contract_rest_with_tags.java");

	private static final File contractBodyFile = fileFor("/contractsToCompile/contract_rest_from_file.java");

	private static final File contractBodyBytesFile = fileFor("/contractsToCompile/contract_rest_from_pdf.java");

	private static final File docsFile = fileFor("/contractsToCompile/contract_docs_examples.java");

	private static File fileFor(String resource) {
		try {
			return new File(JavaContractConverterTests.class.getResource(resource).toURI());
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	static Stream<File> contractFiles() {
		return Stream.of(javaRestFile, javaRestWithTagsFile);
	}

	@ParameterizedTest
	@MethodSource("contractFiles")
	void should_convert_java_dsl_with_rest_to_dsl(File contractFile) throws Exception {
		Collection<Contract> contracts = ContractVerifierDslConverter.convertAsCollection(contractFile.getParentFile(),
				contractFile);
		Contract contract = contracts.iterator().next();
		assertThat(contract.getDescription()).isEqualTo("Some description");
		assertThat(contract.getName()).isEqualTo("some name");
		assertThat(contract.getPriority()).isEqualTo(8);
		assertThat(contract.isIgnored()).isTrue();
		Url url = contract.getRequest().getUrl();
		assertThat(url.getClientValue()).isEqualTo("/foo");
		assertThat(url.getQueryParameters().getParameters().get(0).getName()).isEqualTo("a");
		assertThat(url.getQueryParameters().getParameters().get(0).getServerValue()).isEqualTo("b");
		assertThat(url.getQueryParameters().getParameters().get(1).getName()).isEqualTo("b");
		assertThat(url.getQueryParameters().getParameters().get(1).getServerValue()).isEqualTo("c");
		assertThat(contract.getRequest().getMethod().getClientValue()).isEqualTo("PUT");
		assertThat(contract.getRequest().getHeaders().getEntries()).anyMatch((e) -> "foo".equals(e.getName())
				&& ((RegexProperty) e.getClientValue()).pattern().equals("bar") && "bar".equals(e.getServerValue()));
		assertThat(contract.getRequest().getHeaders().getEntries())
			.anyMatch((e) -> "fooReq".equals(e.getName()) && "baz".equals(e.getServerValue()));
		assertThat(MapConverter.getStubSideValues(contract.getRequest().getBody()))
			.isEqualTo(new ObjectMapper().readValue("{\"foo\":\"bar\"}", Object.class));
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(0).path()).isEqualTo("$.foo");
		assertThat(contract.getRequest().getBodyMatchers().matchers().get(0).matchingType())
			.isEqualTo(MatchingType.REGEX);
		assertThat(((RegexProperty) contract.getRequest().getBodyMatchers().matchers().get(0).value()).pattern())
			.isEqualTo("bar");
		assertThat(contract.getResponse().getStatus().getClientValue()).isEqualTo(200);
		assertThat(contract.getResponse().getDelay().getClientValue()).isEqualTo(1000);
		assertThat(contract.getResponse().getHeaders().getEntries()).anyMatch((e) -> "foo2".equals(e.getName())
				&& ((RegexProperty) e.getServerValue()).pattern().equals("bar") && "bar".equals(e.getClientValue()));
		assertThat(contract.getResponse().getHeaders().getEntries())
			.anyMatch((e) -> "foo3".equals(e.getName()) && e.getServerValue() instanceof ExecutionProperty
					&& ((ExecutionProperty) e.getServerValue()).insertValue("foo").equals("andMeToo(foo)"));
		assertThat(contract.getResponse().getHeaders().getEntries())
			.anyMatch((e) -> "fooRes".equals(e.getName()) && "baz".equals(e.getClientValue()));
		assertThat(MapConverter.getStubSideValues(contract.getResponse().getBody())).isEqualTo(
				new ObjectMapper().readValue("{\"foo2\":\"bar\",\"foo3\":\"baz\",\"nullValue\":null}", Object.class));
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(0).path()).isEqualTo("$.foo2");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(0).matchingType())
			.isEqualTo(MatchingType.REGEX);
		assertThat(((RegexProperty) contract.getResponse().getBodyMatchers().matchers().get(0).value()).pattern())
			.isEqualTo("bar");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(1).path()).isEqualTo("$.foo3");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(1).matchingType())
			.isEqualTo(MatchingType.COMMAND);
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(1).value())
			.isEqualTo(new ExecutionProperty("executeMe($it)"));
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(2).path()).isEqualTo("$.nullValue");
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(2).matchingType())
			.isEqualTo(MatchingType.NULL);
		assertThat(contract.getResponse().getBodyMatchers().matchers().get(2).value()).isNull();
	}

	@Test
	void should_convert_java_with_rest_with_body_from_file() throws Exception {
		Collection<Contract> contracts = ContractVerifierDslConverter
			.convertAsCollection(contractBodyFile.getParentFile(), contractBodyFile);
		assertThat(contracts).hasSize(1);
		Contract contract = contracts.iterator().next();
		ObjectMapper mapper = new ObjectMapper();
		assertThat(mapper.readTree(contract.getRequest().getBody().getClientValue().toString()))
			.isEqualTo(mapper.readTree("{ \"hello\" : \"request\" }"));
		assertThat(mapper.readTree(contract.getResponse().getBody().getClientValue().toString()))
			.isEqualTo(mapper.readTree("{ \"hello\" : \"response\" }"));
	}

	@Test
	void should_convert_java_with_rest_with_body_as_bytes() {
		Collection<Contract> contracts = ContractVerifierDslConverter
			.convertAsCollection(contractBodyBytesFile.getParentFile(), contractBodyBytesFile);
		assertThat(contracts).hasSize(1);
		Contract contract = contracts.iterator().next();
		assertThat(contract.getRequest().getBody().getClientValue()).isInstanceOf(FromFileProperty.class);
		assertThat(contract.getResponse().getBody().getClientValue()).isInstanceOf(FromFileProperty.class);
	}

	@Test
	void should_convert_java_with_rest_for_docs() {
		Collection<Contract> contracts = ContractVerifierDslConverter.convertAsCollection(docsFile.getParentFile(),
				docsFile);
		assertThat(contracts).hasSize(1);
	}

	@Test
	void should_work_when_contract_starts_with_package() throws Exception {
		File packageFile = new File(getClass().getResource("/contractsToCompile/package_contract.java").toURI());
		Collection<Contract> contracts = ContractVerifierDslConverter.convertAsCollection(packageFile.getParentFile(),
				packageFile);
		assertThat(contracts).hasSize(1);
	}

	@Test
	void should_work_when_contract_starts_with_package_preceded_by_other_text() throws Exception {
		File contractFile = new File(getClass().getResource("/contractsToCompile/contract.java").toURI());
		Collection<Contract> contracts = ContractVerifierDslConverter.convertAsCollection(contractFile.getParentFile(),
				contractFile);
		assertThat(contracts).hasSize(1);
	}

}
