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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecursiveFilesConverterApplicationMutationTests {

	private static final String SINGLE_CONTRACT = """
			sh.stubborn.contract.spec.Contract.make {
				request {
					method 'GET'
					url '/foo'
				}
				response {
					status OK()
				}
			}""";

	@TempDir
	@Nullable Path tmpDir;

	private Path tmpDir() {
		return Objects.requireNonNull(this.tmpDir);
	}

	@Test
	void main_converts_contracts_when_exactly_five_arguments_are_given() throws Exception {
		File contractsDslDir = Files.createDirectories(tmpDir().resolve("src")).toFile();
		File stubsOutputDir = Files.createDirectories(tmpDir().resolve("out")).toFile();
		Files.writeString(new File(contractsDslDir, "foo.groovy").toPath(), SINGLE_CONTRACT);

		RecursiveFilesConverterApplication.main(new String[] { stubsOutputDir.getAbsolutePath(),
				contractsDslDir.getAbsolutePath(), "", ".*", "false" });

		assertThat(stubsOutputDir.toPath().resolve("foo.json")).exists();
	}

	@Test
	void main_rejects_wrong_number_of_arguments() {
		assertThatThrownBy(() -> RecursiveFilesConverterApplication.main(new String[] { "a", "b", "c", "d" }))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("Invalid number of arguments");
	}

}
