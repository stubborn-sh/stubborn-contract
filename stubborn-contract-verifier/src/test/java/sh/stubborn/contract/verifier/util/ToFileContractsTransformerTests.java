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
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sh.stubborn.contract.verifier.converter.YamlContractConverter;

import static org.assertj.core.api.Assertions.assertThat;

class ToFileContractsTransformerTests {

	@TempDir
	Path tmpDir;

	@Test
	void should_store_contracts_as_files() {
		File input = new File("src/test/resources/dsl");
		String fqn = YamlContractConverter.class.getName();
		List<File> files = new ToFileContractsTransformer().storeContractsAsFiles(input.getAbsolutePath(), fqn,
				tmpDir.toFile().getAbsolutePath());
		assertThat(files).hasSize(1);
		assertThat(files.get(0).getName()).endsWith(".yml");
	}

}
