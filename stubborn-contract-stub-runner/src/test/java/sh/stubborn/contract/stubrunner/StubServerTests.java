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
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.stubrunner.provider.wiremock.WireMockHttpServerStub;

import org.springframework.cloud.test.TestSocketUtils;

import static org.assertj.core.api.Assertions.assertThat;

class StubServerTests {

	private static final int STUB_SERVER_PORT = TestSocketUtils.findAvailableTcpPort();

	private static final URL EXPECTED_URL;

	static {
		try {
			EXPECTED_URL = new URL("http://localhost:" + STUB_SERVER_PORT);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private final File repository = new File("src/test/resources/repository/mappings/spring/cloud/bye");

	private final StubConfiguration stubConfiguration = new StubConfiguration("a:b");

	@Test
	void shouldRegisterStubMappingsUponServerStart() throws Exception {
		List<File> mappingDescriptors = new StubRepository(this.repository).getStubs();
		StubServer pingStubServer = new StubServer(this.stubConfiguration, mappingDescriptors, List.of(),
				new WireMockHttpServerStub())
			.start(new HttpServerStubConfiguration(new HttpServerStubConfigurer.NoOpHttpServerStubConfigurer(),
					StubRunnerOptions.fromSystemProps(), new StubConfiguration("a:b:c:d"), STUB_SERVER_PORT));
		try {
			assertThat(readUrl("http://localhost:" + pingStubServer.getPort() + "/bye")).isEqualTo("Goodbye world!");
		}
		finally {
			pingStubServer.stop();
		}
	}

	@Test
	void shouldProvideStubServerUrl() throws Exception {
		List<File> mappingDescriptors = new StubRepository(this.repository).getStubs();
		StubServer pingStubServer = new StubServer(this.stubConfiguration, mappingDescriptors, List.of(),
				new WireMockHttpServerStub())
			.start(new HttpServerStubConfiguration(new HttpServerStubConfigurer.NoOpHttpServerStubConfigurer(),
					StubRunnerOptions.fromSystemProps(), new StubConfiguration("a:b:c:d"), STUB_SERVER_PORT));
		try {
			assertThat(pingStubServer.getStubUrl()).isEqualTo(EXPECTED_URL);
		}
		finally {
			pingStubServer.stop();
		}
	}

	private static String readUrl(String url) throws Exception {
		try (InputStream in = new URL(url).openStream()) {
			return new String(in.readAllBytes());
		}
	}

}
