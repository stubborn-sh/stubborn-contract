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

import java.io.InputStream;
import java.net.URL;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.stubrunner.spring.StubRunnerProperties;
import sh.stubborn.contract.verifier.messaging.noop.NoOpStubMessages;

import org.springframework.cloud.test.TestSocketUtils;

import static org.assertj.core.api.Assertions.assertThat;

class StubRunnerTests {

	private static final int MIN_PORT = TestSocketUtils.findAvailableTcpPort();

	private static final int MAX_PORT = MIN_PORT;

	private static final URL EXPECTED_STUB_URL;

	private static final URL GENERATE_STUBS;

	static {
		try {
			EXPECTED_STUB_URL = new URL("http://localhost:" + MIN_PORT);
			GENERATE_STUBS = StubRunnerTests.class.getResource("/generateStubs/");
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void shouldProvideStubUrlForProvidedGroupIdAndArtifactId() throws Exception {
		Arguments args = argumentsWithProjectDefinition();
		StubRunner runner = new StubRunner(args.getStubRunnerOptions(), args.getRepositoryPath(), args.getStub());
		try {
			runner.runStubs();
			assertThat(runner.findStubUrl("groupId", "artifactId")).isEqualTo(EXPECTED_STUB_URL);
		}
		finally {
			runner.close();
		}
	}

	@Test
	void shouldProvideStubUrlIfOnlyArtifactIdWasPassed() throws Exception {
		Arguments args = argumentsWithProjectDefinition();
		StubRunner runner = new StubRunner(args.getStubRunnerOptions(), args.getRepositoryPath(), args.getStub());
		try {
			runner.runStubs();
			assertThat(runner.findStubUrl(null, "artifactId")).isEqualTo(EXPECTED_STUB_URL);
		}
		finally {
			runner.close();
		}
	}

	@Test
	void shouldGenerateStubsAtRuntime() throws Exception {
		Arguments args = argumentsWithGenerateStubs();
		StubDownloader downloader = new FileStubDownloader().build(args.getStubRunnerOptions());
		StubRunner runner = new StubRunnerFactory(args.getStubRunnerOptions(), downloader, new NoOpStubMessages())
			.createStubsFromServiceConfiguration()
			.iterator()
			.next();
		try {
			runner.runStubs();
			URL url = runner.findStubUrl("groupId2", "artifactId2");
			assertThat(readUrl(url.toString() + "/goodbye")).isEqualTo("Goodbye World!");
		}
		finally {
			runner.close();
		}
	}

	@Test
	void shouldOverrideExistingMappingsWhenGeneratingStubsAtRuntime() throws Exception {
		Arguments args = argumentsWithGenerateStubs();
		StubDownloader downloader = new FileStubDownloader().build(args.getStubRunnerOptions());
		StubRunner runner = new StubRunnerFactory(args.getStubRunnerOptions(), downloader, new NoOpStubMessages())
			.createStubsFromServiceConfiguration()
			.iterator()
			.next();
		try {
			runner.runStubs();
			URL url = runner.findStubUrl("groupId2", "artifactId2");
			assertThat(readUrl(url.toString() + "/hello")).isEqualTo("Hello New World!");
		}
		finally {
			runner.close();
		}
	}

	@Test
	void shouldHandleContractsWithBodyContentsLoadedFromExternalFileWhenGeneratingStubsAtRuntime() throws Exception {
		Arguments args = argumentsWithGenerateStubs();
		StubDownloader downloader = new FileStubDownloader().build(args.getStubRunnerOptions());
		StubRunner runner = new StubRunnerFactory(args.getStubRunnerOptions(), downloader, new NoOpStubMessages())
			.createStubsFromServiceConfiguration()
			.iterator()
			.next();
		try {
			runner.runStubs();
			URL url = runner.findStubUrl("groupId2", "artifactId2");
			assertThat(readUrl(url.toString() + "/goodbye_from_file")).isEqualTo("Goodbye from file!");
		}
		finally {
			runner.close();
		}
	}

	private Arguments argumentsWithProjectDefinition() {
		StubConfiguration stubConfiguration = new StubConfiguration("groupId", "artifactId", "classifier");
		StubRunnerOptions stubRunnerOptions = new StubRunnerOptionsBuilder().withMinMaxPort(MIN_PORT, MAX_PORT).build();
		return new Arguments(stubRunnerOptions, "src/test/resources/repository", stubConfiguration);
	}

	private Arguments argumentsWithGenerateStubs() {
		StubConfiguration stubConfiguration = new StubConfiguration("groupId2", "artifactId2", "classifier2");
		StubRunnerOptions stubRunnerOptions = new StubRunnerOptionsBuilder().withMinMaxPort(MIN_PORT, MAX_PORT)
			.withGenerateStubs(true)
			.withStubs(stubConfiguration.toString())
			.withStubsMode(StubRunnerProperties.StubsMode.REMOTE)
			.withStubRepositoryRoot("stubs://file://" + GENERATE_STUBS.getPath())
			.build();
		return new Arguments(stubRunnerOptions, "src/test/resources/generateStubs", stubConfiguration);
	}

	private static String readUrl(String url) throws Exception {
		try (InputStream in = new URL(url).openStream()) {
			return new String(in.readAllBytes());
		}
	}

}
