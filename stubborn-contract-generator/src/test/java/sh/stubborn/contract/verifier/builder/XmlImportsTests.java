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

package sh.stubborn.contract.verifier.builder;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;
import sh.stubborn.contract.verifier.config.ContractVerifierConfigProperties;
import sh.stubborn.contract.verifier.file.ContractMetadata;

import static org.assertj.core.api.Assertions.assertThat;

class XmlImportsTests {

	@Test
	void should_import_only_the_dom_document_and_return_itself() {
		Contract contract = Contract.make((c) -> {
			c.request((r) -> {
				r.method(r.GET());
				r.url("/xml");
			});
			c.response((r) -> {
				r.status(r.OK());
				r.headers((h) -> h.contentType(h.applicationXml()));
				r.body("<root><a>1</a></root>");
			});
		});
		ContractMetadata metadata = new ContractMetadata(new File(".").toPath(), false, 0, null, contract);
		GeneratedClassMetaData classMetaData = new GeneratedClassMetaData(new ContractVerifierConfigProperties(),
				List.of(metadata), "com/example",
				new SingleTestGenerator.GeneratedClassData("FooTest", "com.example", new File(".").toPath()));
		BlockBuilder blockBuilder = new BlockBuilder(" ");
		XmlImports imports = new XmlImports(blockBuilder, classMetaData);

		assertThat(imports.accept()).isTrue();
		assertThat(imports.call()).isSameAs(imports);
		assertThat(blockBuilder.toString().trim()).isEqualTo("import org.w3c.dom.Document");
	}

}
