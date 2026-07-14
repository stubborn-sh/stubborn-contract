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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ClasspathStubResourceScanner}.
 */
class ClasspathStubResourceScannerTest {

	private final ClasspathStubResourceScanner scanner = new ClasspathStubResourceScanner();

	@Test
	void classpathAllPrefixFindsResourcesOnClasspath() throws IOException {
		List<StubResource> resources = this.scanner.getResources("classpath*:**/logback.xml");
		assertThat(resources).isNotEmpty();
		assertThat(resources).anyMatch((r) -> r.getFilename() != null && r.getFilename().equals("logback.xml"));
	}

	@Test
	void classpathSinglePrefixFindsResourcesOnClasspath() throws IOException {
		List<StubResource> resources = this.scanner.getResources("classpath:**/logback.xml");
		assertThat(resources).isNotEmpty();
	}

	@Test
	void fileSystemGlobFindsFilesInDirectory(@TempDir Path dir) throws IOException {
		Files.createFile(dir.resolve("a.json"));
		Files.createFile(dir.resolve("b.json"));
		Files.createFile(dir.resolve("ignore.txt"));

		List<StubResource> resources = this.scanner.getResources(dir.toString() + "/*.json");

		assertThat(resources).hasSize(2);
		assertThat(resources).allMatch((r) -> r.getFilename() != null && r.getFilename().endsWith(".json"));
	}

	@Test
	void fileSystemWithDoubleSlashProtocolStripped(@TempDir Path dir) throws IOException {
		Files.createFile(dir.resolve("x.json"));

		List<StubResource> resources = this.scanner.getResources("file://" + dir + "/*.json");

		assertThat(resources).hasSize(1);
		assertThat(resources.get(0).getFilename()).isEqualTo("x.json");
	}

	@Test
	void fileSystemWithTripleSlashProtocolStripped(@TempDir Path dir) throws IOException {
		Files.createFile(dir.resolve("y.json"));

		List<StubResource> resources = this.scanner.getResources("file:///" + dir + "/*.json");

		assertThat(resources).hasSize(1);
		assertThat(resources.get(0).getFilename()).isEqualTo("y.json");
	}

	@Test
	void noMatchReturnsEmptyList() throws IOException {
		List<StubResource> resources = this.scanner.getResources("classpath*:**/no-such-file-ever-12345.xyz");
		assertThat(resources).isEmpty();
	}

	@Test
	void exactFileThatExistsReturnsIt(@TempDir Path dir) throws IOException {
		Path file = Files.createFile(dir.resolve("stub.json"));

		List<StubResource> resources = this.scanner.getResources(file.toString());

		assertThat(resources).hasSize(1);
		assertThat(resources.get(0).getFilename()).isEqualTo("stub.json");
	}

	@Test
	void exactFileThatDoesNotExistReturnsEmptyList() throws IOException {
		List<StubResource> resources = this.scanner.getResources("/no/such/path/stub.json");
		assertThat(resources).isEmpty();
	}

}
