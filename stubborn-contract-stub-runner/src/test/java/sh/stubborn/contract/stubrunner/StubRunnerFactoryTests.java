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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sh.stubborn.contract.verifier.messaging.noop.NoOpStubMessages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class StubRunnerFactoryTests {

	private static final String MAPPING = """
			{
			  "request": {
			    "method": "GET",
			    "url": "/hello"
			  },
			  "response": {
			    "status": 200,
			    "body": "Hello world!",
			    "headers": {
			      "Content-Type": "text/plain"
			    }
			  }
			}
			""";

	@TempDir
	@Nullable Path tempDir;

	private String stubs = "a:b,c:d";

	private StubDownloader downloader;

	private StubRunnerOptions stubRunnerOptions;

	private StubRunnerFactory factory;

	@BeforeEach
	void setUp() throws Exception {
		this.downloader = mock(StubDownloader.class);
		this.stubRunnerOptions = new StubRunnerOptionsBuilder()
			.withStubRepositoryRoot(Objects.requireNonNull(this.tempDir).toFile().getAbsolutePath())
			.withStubs(this.stubs)
			.build();
		this.factory = new StubRunnerFactory(this.stubRunnerOptions, this.downloader, new NoOpStubMessages());
	}

	@Test
	void shouldDownloadStubDefinitionsManyTimes() throws Exception {
		Files.createDirectory(Objects.requireNonNull(this.tempDir).resolve("mappings"));
		Files.writeString(Objects.requireNonNull(this.tempDir).resolve("hello.json"), MAPPING);
		given(this.downloader.downloadAndUnpackStubJar(any()))
			.willReturn(new AbstractMap.SimpleEntry<>(new StubConfiguration("a:b"),
					Objects.requireNonNull(this.tempDir).toFile()))
			.willReturn(new AbstractMap.SimpleEntry<>(new StubConfiguration("c:d"),
					Objects.requireNonNull(this.tempDir).toFile()));

		Collection<StubRunner> stubRunners = collectOnlyPresentValues(
				this.factory.createStubsFromServiceConfiguration());

		assertThat(stubRunners).hasSize(2);
	}

	private List<StubRunner> collectOnlyPresentValues(Collection<StubRunner> stubRunners) {
		return stubRunners.stream().filter((r) -> r != null).collect(Collectors.toList());
	}

}
