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

package sh.stubborn.contract.stubrunner;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sh.stubborn.contract.spec.Contract;

import static org.assertj.core.api.Assertions.assertThat;

class MappingGeneratorMutationTests {

	private static Contract httpContract() {
		return Contract.make((c) -> {
			c.request((r) -> {
				r.method("GET");
				r.url("/greeting");
			});
			c.response((r) -> {
				r.status(200);
				r.body("hello");
			});
		});
	}

	@Test
	void shouldGenerateMappingStrippingTheFileExtension(@TempDir Path folder) throws Exception {
		File contractFile = new File("shouldReturnGreeting.groovy");

		Collection<Path> mappings = MappingGenerator.toMappings(contractFile, List.of(httpContract()), folder.toFile());

		assertThat(mappings).hasSize(1);
		Path mapping = mappings.iterator().next();
		assertThat(mapping).exists();
		// the ".groovy" extension is stripped, base name kept, hash + extension appended
		assertThat(mapping.getFileName().toString()).startsWith("shouldReturnGreeting_").endsWith(".json");
		assertThat(Files.readString(mapping)).contains("/greeting");
	}

	@Test
	void shouldGenerateMappingWhenFilenameHasNoExtension(@TempDir Path folder) throws Exception {
		File contractFile = new File("shouldReturnGreeting");

		Collection<Path> mappings = MappingGenerator.toMappings(contractFile, List.of(httpContract()), folder.toFile());

		assertThat(mappings).hasSize(1);
		assertThat(mappings.iterator().next().getFileName().toString()).startsWith("shouldReturnGreeting_");
	}

}
