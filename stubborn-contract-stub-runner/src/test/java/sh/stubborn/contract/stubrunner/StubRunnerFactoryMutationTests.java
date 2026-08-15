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

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sh.stubborn.contract.verifier.messaging.noop.NoOpStubMessages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StubRunnerFactoryMutationTests {

	private final StubDownloader stubDownloader = mock(StubDownloader.class);

	private StubRunnerFactory factory(StubRunnerOptions options) {
		return new StubRunnerFactory(options, this.stubDownloader, new NoOpStubMessages<>());
	}

	@Test
	void returnsEmptyWhenNoDependenciesConfigured() {
		StubRunnerOptions options = new StubRunnerOptionsBuilder().build();
		assertThat(factory(options).createStubsFromServiceConfiguration()).isEmpty();
	}

	@Test
	void skipsDependencyWhenDownloaderReturnsNull() {
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withStubs("group:artifact:1.0.0").build();
		when(this.stubDownloader.downloadAndUnpackStubJar(org.mockito.ArgumentMatchers.any())).thenReturn(null);
		assertThat(factory(options).createStubsFromServiceConfiguration()).isEmpty();
	}

	@Test
	void createsStubRunnerWhenDownloaderReturnsEntry(@TempDir Path temp) {
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withStubs("group:artifact:1.0.0")
			.withFailOnNoStubs(false)
			.build();
		StubConfiguration config = new StubConfiguration("group", "artifact", "1.0.0");
		when(this.stubDownloader.downloadAndUnpackStubJar(org.mockito.ArgumentMatchers.any()))
			.thenReturn(Map.entry(config, temp.toFile()));

		Collection<StubRunner> runners = factory(options).createStubsFromServiceConfiguration();

		assertThat(runners).hasSize(1);
	}

}
