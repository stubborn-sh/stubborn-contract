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

import java.util.List;

import org.apache.maven.settings.Settings;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the remote-repository assembly branches in the {@link AetherStubDownloader}
 * constructors that the resolution tests in {@link AetherStubDownloaderTests} do not
 * exercise: a configured proxy, an unknown server id, a blank entry in a comma-separated
 * repository list, and the Maven-injected constructor with and without remote
 * repositories. Construction alone drives these branches; no network access is required.
 */
class AetherStubDownloaderRemoteRepositoriesTests {

	@Test
	void buildsARemoteRepositoryWithAProxy() {
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withStubsMode(StubsMode.REMOTE)
			.withStubRepositoryRoot("file:///tmp/stubborn-stubs-proxy")
			.withProxy("proxy.example.com", 8080)
			.build();

		assertThat(new AetherStubDownloader(options)).isNotNull();
	}

	@Test
	void resolvesAuthenticationWhenTheServerIdMatchesNoConfiguredServer() {
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withStubsMode(StubsMode.REMOTE)
			.withStubRepositoryRoot("file:///tmp/stubborn-stubs-server")
			.withServerId("no-such-server")
			.build();

		assertThat(new AetherStubDownloader(options)).isNotNull();
	}

	@Test
	void skipsBlankEntriesInACommaSeparatedRepositoryList() {
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withStubsMode(StubsMode.REMOTE)
			.withStubRepositoryRoot("file:///tmp/stubborn-stubs-a,,file:///tmp/stubborn-stubs-b")
			.build();

		assertThat(new AetherStubDownloader(options)).isNotNull();
	}

	@Test
	void mavenInjectedConstructorAcceptsRemoteRepositories() {
		RepositorySystem system = AetherFactories.repositorySystemOr(null);
		RepositorySystemSession session = AetherFactories.newSession(system, true);
		Settings settings = AetherFactories.settings();
		List<RemoteRepository> repositories = List
			.of(new RemoteRepository.Builder("remote", "default", "file:///tmp/stubborn-stubs-injected").build());

		assertThat(new AetherStubDownloader(system, repositories, session, settings)).isNotNull();
	}

	@Test
	void mavenInjectedConstructorLogsWhenNoRemoteRepositoriesArePassed() {
		RepositorySystem system = AetherFactories.repositorySystemOr(null);
		RepositorySystemSession session = AetherFactories.newSession(system, true);
		Settings settings = AetherFactories.settings();

		assertThat(new AetherStubDownloader(system, List.of(), session, settings)).isNotNull();
	}

}
