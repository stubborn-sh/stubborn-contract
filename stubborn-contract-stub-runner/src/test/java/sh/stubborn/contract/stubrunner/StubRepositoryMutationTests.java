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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StubRepositoryMutationTests {

	@Test
	void shouldExposePath(@TempDir Path temp) {
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withFailOnNoStubs(false).build();
		StubRepository repository = new StubRepository(temp.toFile(), List.of(), options, null);
		assertThat(repository.getPath()).isEqualTo(temp.toFile());
	}

	@Test
	void shouldCollectStubsAcceptedByCustomHttpServerStub(@TempDir Path temp) throws Exception {
		File custom = temp.resolve("mapping.custom").toFile();
		Files.writeString(custom.toPath(), "{}");
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withFailOnNoStubs(false).build();
		StubRepository repository = new StubRepository(temp.toFile(), List.of(new CustomExtensionServerStub()), options,
				null);
		assertThat(repository.getStubs()).containsExactly(custom);
	}

	@Test
	void shouldNotCollectStubsWhenNoServerStubAcceptsFile(@TempDir Path temp) throws Exception {
		// a non-json file is rejected by the default WireMock stub
		Files.writeString(temp.resolve("mapping.custom").toFile().toPath(), "{}");
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withFailOnNoStubs(false).build();
		StubRepository repository = new StubRepository(temp.toFile(), List.of(), options, null);
		assertThat(repository.getStubs()).isEmpty();
	}

	@Test
	void shouldCollectAllStubsWhenStubsPerConsumerDisabled(@TempDir Path temp) throws Exception {
		Files.writeString(Files.createDirectories(temp.resolve("consumerA")).resolve("a.custom"), "{}");
		Files.writeString(Files.createDirectories(temp.resolve("other")).resolve("b.custom"), "{}");
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withFailOnNoStubs(false).build();
		StubRepository repository = new StubRepository(temp.toFile(), List.of(new CustomExtensionServerStub()), options,
				null);
		assertThat(repository.getStubs()).hasSize(2);
	}

	@Test
	void shouldCollectOnlyConsumerStubsWhenStubsPerConsumerEnabled(@TempDir Path temp) throws Exception {
		File consumerStub = Files
			.writeString(Files.createDirectories(temp.resolve("consumerA")).resolve("a.custom"), "{}")
			.toFile();
		Files.writeString(Files.createDirectories(temp.resolve("other")).resolve("b.custom"), "{}");
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withFailOnNoStubs(false)
			.withStubPerConsumer(true)
			.withConsumerName("consumerA")
			.build();
		StubRepository repository = new StubRepository(temp.toFile(), List.of(new CustomExtensionServerStub()), options,
				null);
		assertThat(repository.getStubs()).containsExactly(consumerStub);
	}

	@Test
	void shouldFailWithConfigurationNotationWhenNoStubsFound(@TempDir Path temp) {
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withFailOnNoStubs(true).build();
		StubConfiguration configuration = new StubConfiguration("com.example:foo:1.0.0");
		assertThatThrownBy(() -> new StubRepository(temp.toFile(), List.of(), options, configuration))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("com.example:foo:1.0.0:stubs");
	}

	@Test
	void shouldFailWhenRepositoryIsNotADirectory(@TempDir Path temp) throws Exception {
		File notADir = Files.writeString(temp.resolve("plain.txt"), "x").toFile();
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withFailOnNoStubs(false).build();
		assertThatThrownBy(() -> new StubRepository(notADir, List.of(), options, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Missing descriptor repository");
	}

	@Test
	void shouldFailWithNullWhenNoStubsFoundAndNoConfiguration(@TempDir Path temp) {
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withFailOnNoStubs(true).build();
		assertThatThrownBy(() -> new StubRepository(temp.toFile(), List.of(), options, null))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("[null]");
	}

	private static final class CustomExtensionServerStub implements HttpServerStub {

		@Override
		public int port() {
			return -1;
		}

		@Override
		public boolean isRunning() {
			return false;
		}

		@Override
		public HttpServerStub start(HttpServerStubConfiguration configuration) {
			return this;
		}

		@Override
		public HttpServerStub stop() {
			return this;
		}

		@Override
		public HttpServerStub registerMappings(Collection<File> stubFiles) {
			return this;
		}

		@Override
		public String registeredMappings() {
			return "[]";
		}

		@Override
		public boolean isAccepted(File file) {
			return file.getName().endsWith(".custom");
		}

	}

}
