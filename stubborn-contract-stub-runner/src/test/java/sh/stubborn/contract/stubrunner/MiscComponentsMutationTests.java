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
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({ "NullAway", "unchecked", "rawtypes" })
class MiscComponentsMutationTests {

	@Test
	void noOpHttpServerStubBehavesAsNoOp() {
		NoOpHttpServerStub stub = new NoOpHttpServerStub();
		assertThat(stub.port()).isEqualTo(-1);
		assertThat(stub.isRunning()).isFalse();
		assertThat(stub.start(null)).isSameAs(stub);
		assertThat(stub.stop()).isSameAs(stub);
		assertThat(stub.registerMappings(List.of())).isSameAs(stub);
		assertThat(stub.registeredMappings()).isEmpty();
		assertThat(stub.isAccepted(new File("anything.json"))).isTrue();
	}

	@Test
	void httpServerStubConfigurationComputesRandomPort() {
		StubConfiguration config = new StubConfiguration("group:artifact:1.0.0:stubs");
		HttpServerStubConfigurer configurer = HttpServerStubConfigurer.NoOpHttpServerStubConfigurer.INSTANCE;
		StubRunnerOptions options = new StubRunnerOptionsBuilder().build();

		HttpServerStubConfiguration nullPort = new HttpServerStubConfiguration(configurer, options, config, null);
		HttpServerStubConfiguration zeroPort = new HttpServerStubConfiguration(configurer, options, config, 0);
		HttpServerStubConfiguration fixedPort = new HttpServerStubConfiguration(configurer, options, config, 8080);

		assertThat(nullPort.isRandomPort()).isTrue();
		assertThat(nullPort.randomPort).isTrue();
		assertThat(zeroPort.isRandomPort()).isTrue();
		assertThat(fixedPort.isRandomPort()).isFalse();
		assertThat(fixedPort.randomPort).isFalse();
		assertThat(fixedPort.toColonSeparatedDependencyNotation()).isEqualTo("group:artifact:1.0.0:stubs");
	}

	@Test
	void noOpConfigurerRejectsAndReturnsConfigurationUnchanged() {
		HttpServerStubConfigurer.NoOpHttpServerStubConfigurer configurer = new HttpServerStubConfigurer.NoOpHttpServerStubConfigurer();
		Object stubConfig = new Object();
		assertThat(configurer.isAccepted(stubConfig)).isFalse();
		assertThat(configurer.configure(stubConfig, null)).isSameAs(stubConfig);
	}

	@Test
	void argumentsToStringExposesFields() {
		Arguments arguments = new Arguments(new StubRunnerOptionsBuilder().build(), "some/path",
				new StubConfiguration("group:artifact:1.0.0"));
		assertThat(arguments.toString()).contains("repositoryPath='some/path'").contains("stub=group:artifact:1.0.0");
	}

	@Test
	void httpServerStubDefaultsAreProvided() {
		HttpServerStub stub = new MinimalHttpServerStub();
		assertThat(stub.httpsPort()).isEqualTo(-1);
		assertThat(stub.reset()).isSameAs(stub);
	}

	@Test
	void stubResourceExistsWhenFilePresent(@TempDir Path temp) throws IOException {
		Path file = temp.resolve("present.txt");
		Files.writeString(file, "x");
		assertThat(new FileBackedResource(file.toFile()).exists()).isTrue();
	}

	@Test
	void stubResourceDoesNotExistWhenFileMissing(@TempDir Path temp) {
		assertThat(new FileBackedResource(temp.resolve("missing.txt").toFile()).exists()).isFalse();
	}

	@Test
	void stubResourceDoesNotExistWhenGetFileThrows() {
		assertThat(new ThrowingResource().exists()).isFalse();
	}

	private static final class MinimalHttpServerStub implements HttpServerStub {

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
			return true;
		}

	}

	private static final class FileBackedResource implements StubResource {

		private final File file;

		FileBackedResource(File file) {
			this.file = file;
		}

		@Override
		public URI getURI() {
			return this.file.toURI();
		}

		@Override
		public URL getURL() throws IOException {
			return this.file.toURI().toURL();
		}

		@Override
		public File getFile() {
			return this.file;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return Files.newInputStream(this.file.toPath());
		}

		@Override
		public String getFilename() {
			return this.file.getName();
		}

		@Override
		public String getDescription() {
			return this.file.toString();
		}

	}

	private static final class ThrowingResource implements StubResource {

		@Override
		public URI getURI() throws IOException {
			throw new IOException("no uri");
		}

		@Override
		public URL getURL() throws IOException {
			throw new IOException("no url");
		}

		@Override
		public File getFile() throws IOException {
			throw new IOException("no file");
		}

		@Override
		public InputStream getInputStream() throws IOException {
			throw new IOException("no stream");
		}

		@Override
		public String getFilename() {
			return "throwing";
		}

		@Override
		public String getDescription() {
			return "throwing";
		}

	}

}
