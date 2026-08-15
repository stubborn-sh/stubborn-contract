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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NullAway")
class ClasspathStubResourceScannerMutationTests {

	private final ClasspathStubResourceScanner scanner = new ClasspathStubResourceScanner();

	@Test
	void fileResourceExposesConcreteAccessors(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("stub.json");
		Files.writeString(file, "{\"x\":1}");

		List<StubResource> resources = this.scanner.getResources(dir + "/*.json");
		assertThat(resources).hasSize(1);
		StubResource resource = resources.get(0);

		assertThat(resource.getFilename()).isEqualTo("stub.json");
		assertThat(resource.getFile()).isEqualTo(file.toFile());
		assertThat(resource.getFile().getName()).isEqualTo("stub.json");
		assertThat(resource.getURL().toString()).endsWith("stub.json");
		assertThat(resource.getURI()).isEqualTo(file.toUri());
		assertThat(resource.getDescription()).isEqualTo("file [" + file.toAbsolutePath() + "]");
		assertThat(resource.exists()).isTrue();
		try (InputStream is = resource.getInputStream()) {
			assertThat(new String(is.readAllBytes())).isEqualTo("{\"x\":1}");
		}
	}

	@Test
	void fileResourceExistsReflectsUnderlyingFile(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("present.json");
		Files.writeString(file, "{}");
		StubResource resource = this.scanner.getResources(dir + "/*.json").get(0);
		assertThat(resource.exists()).isTrue();
		Files.delete(file);
		assertThat(resource.exists()).isFalse();
	}

	@Test
	void fileColonSinglePrefixIsStripped(@TempDir Path dir) throws IOException {
		Files.createFile(dir.resolve("single.json"));
		List<StubResource> resources = this.scanner.getResources("file:" + dir + "/*.json");
		assertThat(resources).hasSize(1);
		assertThat(resources.get(0).getFilename()).isEqualTo("single.json");
	}

	@Test
	void singleCharWildcardMatchesOnlySingleCharNames(@TempDir Path dir) throws IOException {
		Files.createFile(dir.resolve("a.json"));
		Files.createFile(dir.resolve("ab.json"));

		List<StubResource> resources = this.scanner.getResources(dir + "/?.json");

		assertThat(resources).extracting(StubResource::getFilename).containsExactly("a.json");
	}

	@Test
	void patternWithBothStarAndQuestionUsesEarliestWildcard(@TempDir Path dir) throws IOException {
		Files.createFile(dir.resolve("ab.json"));

		// pattern contains both '?' and '*'; firstWildcard() must pick the earliest so
		// the
		// base directory is computed from the correct position
		List<StubResource> resources = this.scanner.getResources(dir + "/a?*.json");

		assertThat(resources).extracting(StubResource::getFilename).containsExactly("ab.json");
	}

	@Test
	void jarBackedResourceExposesConcreteAccessors() throws IOException {
		// scanning the classpath for a file that lives inside dependency jars exercises
		// the
		// jar-scanning path (scanJar/urlToResource) and its anonymous StubResource
		List<StubResource> resources = this.scanner.getResources("classpath*:META-INF/MANIFEST.MF");
		StubResource jarResource = resources.stream()
			.filter((r) -> r.getDescription().startsWith("URL [jar:"))
			.findFirst()
			.orElseThrow(() -> new AssertionError("expected at least one jar-backed MANIFEST.MF resource"));

		assertThat(jarResource.getFilename()).isEqualTo("MANIFEST.MF");
		assertThat(jarResource.getURL().toString()).startsWith("jar:").endsWith("META-INF/MANIFEST.MF");
		assertThat(jarResource.getURI().toString()).startsWith("jar:").endsWith("META-INF/MANIFEST.MF");
		assertThat(jarResource.getDescription()).startsWith("URL [jar:").contains("META-INF/MANIFEST.MF");
		assertThat(jarResource.exists()).isTrue();
		try (java.io.InputStream is = jarResource.getInputStream()) {
			assertThat(is.readAllBytes()).isNotEmpty();
		}
	}

}
