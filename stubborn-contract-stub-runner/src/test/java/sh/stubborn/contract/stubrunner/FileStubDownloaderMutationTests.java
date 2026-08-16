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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NullAway")
class FileStubDownloaderMutationTests {

	private final FileStubDownloader downloader = new FileStubDownloader();

	@Test
	void protocolAcceptedOnlyForStubsScheme() {
		assertThat(FileStubDownloader.isProtocolAccepted("stubs://file:///tmp")).isTrue();
		assertThat(FileStubDownloader.isProtocolAccepted("http://example.com")).isFalse();
	}

	@Test
	void buildReturnsNullForClasspathMode() {
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withStubsMode(StubsMode.CLASSPATH).build();
		assertThat(this.downloader.build(options)).isNull();
	}

	@Test
	void buildReturnsNullWhenNoRepositoryRoot() {
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withStubsMode(StubsMode.REMOTE).build();
		assertThat(this.downloader.build(options)).isNull();
	}

	@Test
	void buildReturnsNullWhenRootIsNotStubsResource() {
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withStubsMode(StubsMode.REMOTE)
			.withStubRepositoryRoot(ResourceResolver.classpathResource("logback.xml"))
			.build();
		assertThat(this.downloader.build(options)).isNull();
	}

	@Test
	void buildReturnsDownloaderForStubsResource() {
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withStubsMode(StubsMode.REMOTE)
			.withStubRepositoryRoot("stubs://file:///tmp/repo")
			.build();
		assertThat(this.downloader.build(options)).isInstanceOf(StubsStubDownloader.class);
	}

	@Test
	void resolveReturnsNullForNullBlankOrUnacceptedProtocol() {
		assertThat(this.downloader.resolve(null)).isNull();
		assertThat(this.downloader.resolve("   ")).isNull();
		assertThat(this.downloader.resolve("http://example.com")).isNull();
	}

	@Test
	void resolveNonFileStubsLocationKeptAsIs() {
		StubResource resource = this.downloader.resolve("stubs://example.com/repo");
		assertThat(resource).isInstanceOf(StubsResource.class);
		assertThat(resource.getDescription()).isEqualTo("stubs://example.com/repo");
	}

	@Test
	void resolveRelativeFileLocationGetsTripleSlash() {
		StubResource resource = this.downloader.resolve("stubs://file://relative/path");
		assertThat(resource.getDescription()).isEqualTo("stubs://file:///relative/path");
	}

	@Test
	void resolveAbsoluteFileLocationKeptAsIs() {
		StubResource resource = this.downloader.resolve("stubs://file:///absolute/path");
		assertThat(resource.getDescription()).isEqualTo("stubs://file:///absolute/path");
	}

	@Test
	void resolveWindowsPathConvertedToUnix() {
		StubResource resource = this.downloader.resolve("stubs://file://C:\\repo\\stubs");
		assertThat(resource.getDescription()).isEqualTo("stubs://file:///C:/repo/stubs");
	}

}
