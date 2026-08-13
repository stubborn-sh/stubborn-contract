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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end resolution of {@code StubsMode.CLASSPATH} stubs laid out exactly the way
 * Spring Cloud Contract publishes them, guarding compatibility between SCC-produced stubs
 * and the Spring-free Stubborn stub runner.
 *
 * <p>
 * The scanned resources live on the test classpath under {@code contracts/},
 * {@code mappings/} and {@code META-INF/} (see {@code src/test/resources}) and cover the
 * layouts a migrating SCC user relies on:
 * <ul>
 * <li>a contract placed <em>directly</em> under {@code contracts/<group>/<artifact>/} —
 * the case that regressed when the Spring {@code AntPathMatcher} was replaced with a Java
 * NIO glob, because {@code **}{@code /*.*} stopped matching files with no intermediate
 * directory;</li>
 * <li>a contract <em>nested</em> under
 * {@code contracts/<group>/<artifact>/contracts/};</li>
 * <li>WireMock mappings under {@code mappings/<group>/<artifact>/};</li>
 * <li>the {@code META-INF/<group>/<artifact>/<version>/contracts/} layout of an installed
 * stubs jar.</li>
 * </ul>
 */
class ClasspathStubResolutionCompatibilityTests {

	private final StubDownloader downloader = Objects.requireNonNull(new ClasspathStubProvider()
		.build(new StubRunnerOptionsBuilder().withStubsMode(StubsMode.CLASSPATH).build()));

	@Test
	void resolvesContractPlacedDirectlyUnderTheArtifactFolder() throws IOException {
		assertUnpacks("com.example", "stubborn-classpath-direct", "shouldReturnGreeting.yml");
	}

	@Test
	void resolvesContractNestedInAContractsSubfolder() throws IOException {
		assertUnpacks("com.example", "stubborn-classpath-nested", "shouldReturnGreeting.yml");
	}

	@Test
	void resolvesWireMockMappingsUnderTheArtifactFolder() throws IOException {
		assertUnpacks("com.example", "stubborn-classpath-mappings", "greeting.json");
	}

	@Test
	void resolvesStubsJarMetaInfVersionedLayout() throws IOException {
		assertUnpacks("com.example", "stubborn-classpath-metainf", "shouldReturnGreeting.yml");
	}

	private void assertUnpacks(String group, String artifact, String expectedFile) throws IOException {
		Map.Entry<StubConfiguration, File> unpacked = this.downloader
			.downloadAndUnpackStubJar(new StubConfiguration(group, artifact, "+"));

		assertThat(unpacked).as("stub for %s:%s was not resolved from the classpath", group, artifact).isNotNull();
		Objects.requireNonNull(unpacked);
		try (Stream<Path> files = Files.walk(unpacked.getValue().toPath())) {
			assertThat(files.map((p) -> p.getFileName().toString())).contains(expectedFile);
		}
	}

}
