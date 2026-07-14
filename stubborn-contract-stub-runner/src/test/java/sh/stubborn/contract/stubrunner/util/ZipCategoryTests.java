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

package sh.stubborn.contract.stubrunner.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ZipCategoryTests {

	@Test
	void shouldUnzipFileToSpecifiedLocation(@TempDir Path tempDir) throws Exception {
		File zipFile = new File(ZipCategoryTests.class.getClassLoader().getResource("file.zip").toURI());
		File dest = tempDir.toFile();
		ZipCategory.unzipTo(zipFile, dest);
		File unzipped = new File(dest, "file.txt");
		assertThat(unzipped).exists();
		assertThat(Files.readString(unzipped.toPath()).trim()).isEqualTo("test");
	}

	@Test
	void shouldNotAllowMaliciousTraversal(@TempDir Path tempDir) throws Exception {
		File zipFile = new File(
				ZipCategoryTests.class.getClassLoader().getResource("zip/zip-malicious-traversal.zip").toURI());
		assertThatExceptionOfType(Exception.class).isThrownBy(() -> ZipCategory.unzipTo(zipFile, tempDir.toFile()))
			.havingCause()
			.withMessageContaining("is trying to leave the target output directory");
	}

}
