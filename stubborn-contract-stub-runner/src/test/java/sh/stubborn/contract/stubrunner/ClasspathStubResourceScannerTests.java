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
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link ClasspathStubResourceScanner}.
 */
class ClasspathStubResourceScannerTests {

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
	void doubleStarMatchesFileDirectlyUnderTheDirectory(@TempDir Path dir) throws IOException {
		// Spring Cloud Contract publishes stubs as contracts/<group>/<artifact>/<file>,
		// i.e.
		// the contract can sit directly under the artifact folder. Spring's
		// AntPathMatcher
		// treats the "**" in ".../**/*.*" as zero-or-more directories, so it still
		// matches.
		Path artifact = Files.createDirectories(dir.resolve("contracts/com.example/artifact"));
		Files.createFile(artifact.resolve("shouldSendGreeting.yml"));

		List<StubResource> resources = this.scanner.getResources(dir + "/contracts/com.example/artifact/**/*.*");

		assertThat(resources).hasSize(1);
		assertThat(resources.get(0).getFilename()).isEqualTo("shouldSendGreeting.yml");
	}

	@Test
	void doubleStarMatchesFileNestedUnderTheDirectory(@TempDir Path dir) throws IOException {
		// The other common SCC layout nests the contract in a contracts/ subfolder of the
		// artifact: contracts/<group>/<artifact>/contracts/<file>. Both must resolve.
		Path nested = Files.createDirectories(dir.resolve("contracts/com.example/artifact/contracts"));
		Files.createFile(nested.resolve("shouldSendGreeting.yml"));

		List<StubResource> resources = this.scanner.getResources(dir + "/contracts/com.example/artifact/**/*.*");

		assertThat(resources).hasSize(1);
		assertThat(resources.get(0).getFilename()).isEqualTo("shouldSendGreeting.yml");
	}

	@Test
	void doubleStarMatchesBothDirectAndNestedFilesTogether(@TempDir Path dir) throws IOException {
		Path artifact = Files.createDirectories(dir.resolve("mappings/com.example/artifact"));
		Files.createFile(artifact.resolve("top.json"));
		Path nested = Files.createDirectories(artifact.resolve("mappings"));
		Files.createFile(nested.resolve("deep.json"));

		List<StubResource> resources = this.scanner.getResources(dir + "/mappings/com.example/artifact/**/*.*");

		assertThat(resources).hasSize(2);
		assertThat(resources).extracting(StubResource::getFilename).containsExactlyInAnyOrder("top.json", "deep.json");
	}

	@Test
	void leadingDoubleStarMatchesFileAtTheRoot(@TempDir Path dir) throws IOException {
		Files.createFile(dir.resolve("root.json"));
		Files.createFile(Files.createDirectories(dir.resolve("sub")).resolve("nested.json"));

		List<StubResource> resources = this.scanner.getResources(dir + "/**/*.json");

		assertThat(resources).extracting(StubResource::getFilename)
			.containsExactlyInAnyOrder("root.json", "nested.json");
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

	@Test
	void jarMappingWithSpaceInFilenameResolvesToValidUri(@TempDir Path dir) throws IOException {
		// Regression for #169: a WireMock mapping filename with a character that is legal
		// in a jar entry name but illegal in an unescaped URI (a space) must not blow up
		// StubResource.getURI() and, in turn, the whole @AutoConfigureStubRunner context.
		String entryName = "META-INF/com.example/artifact/mappings/should send greeting.json";
		String body = "{\"request\":{\"method\":\"GET\"}}";
		Path jar = dir.resolve("stubs-with-spaces.jar");
		try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
			// Real stub jars carry directory entries; getResources("META-INF") relies on
			// the
			// "META-INF/" entry existing, and JarOutputStream does not create them
			// implicitly.
			for (String dirEntry : new String[] { "META-INF/", "META-INF/com.example/",
					"META-INF/com.example/artifact/", "META-INF/com.example/artifact/mappings/" }) {
				out.putNextEntry(new JarEntry(dirEntry));
				out.closeEntry();
			}
			out.putNextEntry(new JarEntry(entryName));
			out.write(body.getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
		}

		ClassLoader previous = Thread.currentThread().getContextClassLoader();
		try (URLClassLoader loader = new URLClassLoader(new URL[] { jar.toUri().toURL() }, null)) {
			Thread.currentThread().setContextClassLoader(loader);

			List<StubResource> resources = this.scanner.getResources("classpath*:**/*.json");

			assertThat(resources).extracting(StubResource::getFilename).contains("should send greeting.json");
			StubResource resource = resources.stream()
				.filter((r) -> "should send greeting.json".equals(r.getFilename()))
				.findFirst()
				.orElseThrow();
			assertThatNoException().isThrownBy(resource::getURI);
			URI uri = resource.getURI();
			assertThat(uri.toString()).contains("should%20send%20greeting.json");
			try (InputStream in = Objects.requireNonNull(resource.getInputStream())) {
				assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(body);
			}
		}
		finally {
			Thread.currentThread().setContextClassLoader(previous);
		}
	}

}
