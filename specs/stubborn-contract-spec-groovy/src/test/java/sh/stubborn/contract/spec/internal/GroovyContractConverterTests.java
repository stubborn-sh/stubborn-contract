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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import sh.stubborn.contract.spec.Contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Mutation-focused coverage for {@link GroovyContractConverter}.
 */
@SuppressWarnings("NullAway")
class GroovyContractConverterTests {

	private static final String SINGLE_DSL = """
			sh.stubborn.contract.spec.Contract.make {
			    request { method 'GET'; url '/foo' }
			    response { status 200 }
			}
			""";

	private static final String LIST_DSL = """
			[sh.stubborn.contract.spec.Contract.make {
			    request { method 'GET'; url '/a' }
			    response { status 200 }
			},
			sh.stubborn.contract.spec.Contract.make {
			    request { method 'GET'; url '/b' }
			    response { status 200 }
			}]
			""";

	@TempDir
	Path tempDir;

	@Test
	void instanceIsNonNullAndUsable() {
		assertThat(GroovyContractConverter.INSTANCE).isNotNull().isInstanceOf(GroovyContractConverter.class);
	}

	@Test
	void convertAsCollectionFromDslParsesASingleContract() {
		Collection<Contract> contracts = GroovyContractConverter.convertAsCollection(this.tempDir.toFile(), SINGLE_DSL);
		assertThat(contracts).hasSize(1);
		assertThat(contracts.iterator().next()).isInstanceOf(Contract.class);
	}

	@Test
	void convertAsCollectionFromDslParsesAListOfTwoContracts() {
		Collection<Contract> contracts = GroovyContractConverter.convertAsCollection(this.tempDir.toFile(), LIST_DSL);
		assertThat(contracts).hasSize(2);
	}

	@Test
	void convertAsCollectionFromDslThrowsWhenDslDoesNotReturnAContract() {
		assertThatIllegalStateException()
			.isThrownBy(() -> GroovyContractConverter.convertAsCollection(this.tempDir.toFile(), "\"hello\""))
			.withMessageContaining("not returning a Contract or list of Contracts");
	}

	@Test
	void convertAsCollectionFromDslWrapsInvalidGroovy() {
		assertThatThrownBy(
				() -> GroovyContractConverter.convertAsCollection(this.tempDir.toFile(), "this is not valid groovy ]["))
			.isInstanceOf(Exception.class);
	}

	@Test
	void convertAsCollectionFromFileDefaultsNameToFilenameWithoutExtension() throws Exception {
		File dsl = writeFile("mycontract.groovy", SINGLE_DSL);
		Collection<Contract> contracts = GroovyContractConverter.convertAsCollection(dsl);
		assertThat(contracts).hasSize(1);
		assertThat(contracts.iterator().next().getName()).isEqualTo("mycontract");
	}

	@Test
	void convertAsCollectionFromRootFolderAndFileDefaultsName() throws Exception {
		File dsl = writeFile("single.groovy", SINGLE_DSL);
		Collection<Contract> contracts = GroovyContractConverter.convertAsCollection(this.tempDir.toFile(), dsl);
		assertThat(contracts).hasSize(1);
		assertThat(contracts.iterator().next().getName()).isEqualTo("single");
	}

	@Test
	void convertAsCollectionFromFileNamesTwoUnnamedContractsWithIndexSuffixes() throws Exception {
		File dsl = writeFile("multi.groovy", LIST_DSL);
		List<Contract> contracts = new ArrayList<>(GroovyContractConverter.convertAsCollection(dsl));
		assertThat(contracts).hasSize(2);
		assertThat(contracts.get(0).getName()).isEqualTo("multi_0");
		assertThat(contracts.get(1).getName()).isEqualTo("multi_1");
	}

	@Test
	void convertAsCollectionFromFileKeepsAnExplicitlyNamedContract() throws Exception {
		String named = """
				sh.stubborn.contract.spec.Contract.make {
				    name('explicit')
				    request { method 'GET'; url '/foo' }
				    response { status 200 }
				}
				""";
		File dsl = writeFile("named.groovy", named);
		Collection<Contract> contracts = GroovyContractConverter.convertAsCollection(dsl);
		assertThat(contracts.iterator().next().getName()).isEqualTo("explicit");
	}

	@Test
	void convertAsCollectionFromFileReturnsEmptyListWhenFileEvaluatesToNull() throws Exception {
		File dsl = writeFile("nullish.groovy", "null");
		Collection<Contract> contracts = GroovyContractConverter.convertAsCollection(dsl);
		assertThat(contracts).isEmpty();
	}

	@Test
	void convertAsCollectionFromRootFolderAndFileThrowsWhenFileDoesNotReturnAContract() throws Exception {
		File dsl = writeFile("nope.groovy", "\"hello\"");
		assertThatIllegalStateException()
			.isThrownBy(() -> GroovyContractConverter.convertAsCollection(this.tempDir.toFile(), dsl))
			.withMessageContaining("not returning a Contract or list of Contracts");
	}

	@Test
	void convertAsCollectionFromRootFolderAndFileThrowsWhenFileReturnsAListOfNonContracts() throws Exception {
		File dsl = writeFile("nonContractList.groovy", "[\"a\", \"b\"]");
		assertThatIllegalStateException()
			.isThrownBy(() -> GroovyContractConverter.convertAsCollection(this.tempDir.toFile(), dsl))
			.withMessageContaining("not returning a Contract or list of Contracts");
	}

	@Test
	void convertAsCollectionFromRootFolderAndFileWrapsACheckedFailureWithTheFilePath() {
		File dsl = new File(this.tempDir.toFile(), "missing.groovy");
		assertThatIllegalStateException()
			.isThrownBy(() -> GroovyContractConverter.convertAsCollection(this.tempDir.toFile(), dsl))
			.withMessageContaining(dsl.getPath());
	}

	@ParameterizedTest
	@CsvSource({ "a.groovy, true", "a.gvy, true", "a.json, false", "a.txt, false" })
	void isAcceptedMatchesGroovyExtensionsOnly(String name, boolean accepted) {
		assertThat(GroovyContractConverter.INSTANCE.isAccepted(new File(name))).isEqualTo(accepted);
	}

	@Test
	void convertToReturnsTheSameCollectionInstance() {
		Collection<Contract> input = GroovyContractConverter.convertAsCollection(this.tempDir.toFile(), SINGLE_DSL);
		assertThat(GroovyContractConverter.INSTANCE.convertTo(input)).isSameAs(input);
	}

	@Test
	void convertFromDelegatesToConvertAsCollectionFromFile() throws Exception {
		File dsl = writeFile("fromfile.groovy", SINGLE_DSL);
		Collection<Contract> contracts = GroovyContractConverter.INSTANCE.convertFrom(dsl);
		assertThat(contracts).hasSize(1);
		assertThat(contracts.iterator().next().getName()).isEqualTo("fromfile");
	}

	private File writeFile(String name, String content) throws Exception {
		File file = this.tempDir.resolve(name).toFile();
		Files.writeString(file.toPath(), content);
		return file;
	}

}
