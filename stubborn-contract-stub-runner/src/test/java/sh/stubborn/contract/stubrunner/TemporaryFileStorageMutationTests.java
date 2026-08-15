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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class TemporaryFileStorageMutationTests {

	@Test
	void createTempDirReturnsExistingDirectory() {
		File dir = TemporaryFileStorage.createTempDir("test-prefix");
		try {
			assertThat(dir).exists().isDirectory();
			assertThat(dir.getName()).startsWith("test-prefix-");
		}
		finally {
			dir.delete();
		}
	}

	@Test
	void cleanupDoesNothingWhenDeleteFlagIsFalse(@TempDir Path temp) throws Exception {
		TemporaryFileStorage.files().clear();
		File file = Files.writeString(temp.resolve("keep.txt"), "x").toFile();
		TemporaryFileStorage.add(file);

		TemporaryFileStorage.cleanup(false);

		assertThat(file).exists();
	}

	@Test
	void cleanupDeletesTrackedDirectoryWhenFlagIsTrue(@TempDir Path temp) throws Exception {
		TemporaryFileStorage.files().clear();
		File dir = Files.createDirectories(temp.resolve("stubs")).toFile();
		File nested = Files.writeString(temp.resolve("stubs").resolve("a.json"), "x").toFile();
		TemporaryFileStorage.add(dir);

		TemporaryFileStorage.cleanup(true);

		assertThat(dir).doesNotExist();
		assertThat(nested).doesNotExist();
		assertThat(TemporaryFileStorage.files()).isEmpty();
	}

	@Test
	void cleanupDeletesTrackedPlainFileWhenFlagIsTrue(@TempDir Path temp) throws Exception {
		TemporaryFileStorage.files().clear();
		File plainFile = Files.writeString(temp.resolve("single.txt"), "x").toFile();
		TemporaryFileStorage.add(plainFile);

		TemporaryFileStorage.cleanup(true);

		assertThat(plainFile).doesNotExist();
	}

}
