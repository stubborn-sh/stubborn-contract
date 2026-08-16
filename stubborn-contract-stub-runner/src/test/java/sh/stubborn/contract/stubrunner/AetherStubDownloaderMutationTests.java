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
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

class AetherStubDownloaderMutationTests {

	@Test
	void localModeConstructsWithoutRemoteRepositories() {
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withStubsMode(StubsMode.LOCAL).build();
		assertThat(new AetherStubDownloader(options)).isNotNull();
	}

	@Test
	void classpathModeIsUnsupported() {
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withStubsMode(StubsMode.CLASSPATH).build();
		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> new AetherStubDownloader(options));
	}

	@Test
	void remoteModeWithoutRepositoriesThrows() {
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withStubsMode(StubsMode.REMOTE).build();
		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> new AetherStubDownloader(options));
	}

	@Test
	void remoteModeWithMultipleRepositoriesAndProxyConstructs() {
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withStubsMode(StubsMode.REMOTE)
			.withStubRepositoryRoot("file:///tmp/repo-a,file:///tmp/repo-b")
			.withProxy("proxy.example", 8888)
			.withUsername("user")
			.withPassword("pass")
			.build();
		assertThat(new AetherStubDownloader(options)).isNotNull();
	}

	@Test
	void mavenPluginConstructorLogsWhenRepositoriesEmpty() {
		AetherStubDownloader downloader = new AetherStubDownloader(mock(RepositorySystem.class), List.of(),
				mock(RepositorySystemSession.class), new Settings());
		assertThat(downloader).isNotNull();
	}

	@Test
	void buildAuthenticationHandlesNullCredentials() {
		AetherStubDownloader downloader = new AetherStubDownloader(mock(RepositorySystem.class),
				List.of(mock(RemoteRepository.class)), mock(RepositorySystemSession.class), new Settings());
		Authentication withCredentials = downloader.buildAuthentication("password", "username");
		assertThat(withCredentials).isNotNull();
		// null credentials produce no authentication (Aether returns null); just ensure
		// the
		// call is exercised without error
		assertThat(downloader.buildAuthentication(null, null)).isNull();
	}

}
