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

package sh.stubborn.contract.stubrunner.provider.wiremock;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.stubrunner.HttpServerStub;
import sh.stubborn.contract.stubrunner.HttpServerStubConfiguration;
import sh.stubborn.contract.stubrunner.HttpServerStubConfigurer;
import sh.stubborn.contract.stubrunner.StubConfiguration;
import sh.stubborn.contract.stubrunner.StubRunnerOptionsBuilder;

import org.springframework.cloud.test.TestSocketUtils;

import static org.assertj.core.api.Assertions.assertThat;

class WireMockHttpServerStubMutationTests {

	private HttpServerStubConfiguration config() {
		return new HttpServerStubConfiguration(HttpServerStubConfigurer.NoOpHttpServerStubConfigurer.INSTANCE,
				new StubRunnerOptionsBuilder().build(), new StubConfiguration("group:artifact:version"),
				TestSocketUtils.findAvailableTcpPort());
	}

	@Test
	void shouldReportInvalidPortAndNotRunningBeforeStart() {
		WireMockHttpServerStub stub = new WireMockHttpServerStub();
		assertThat(stub.isRunning()).isFalse();
		assertThat(stub.port()).isEqualTo(-1);
		assertThat(stub.httpsPort()).isEqualTo(-1);
	}

	@Test
	void stopOnNotStartedServerReturnsSameInstance() {
		WireMockHttpServerStub stub = new WireMockHttpServerStub();
		assertThat(stub.stop()).isSameAs(stub);
	}

	@Test
	void shouldStartWithHttpsWhenConfigurerEnablesIt() {
		WireMockHttpServerStub stub = new WireMockHttpServerStub();
		HttpServerStubConfiguration configuration = new HttpServerStubConfiguration(new HttpsEnablingConfigurer(),
				new StubRunnerOptionsBuilder().build(), new StubConfiguration("group:artifact:version"),
				TestSocketUtils.findAvailableTcpPort());
		stub.start(configuration);
		try {
			assertThat(stub.isRunning()).isTrue();
			// https path: httpsPort() no longer returns the -1 sentinel and port()
			// reports
			// the https port
			assertThat(stub.httpsPort()).isPositive();
			assertThat(stub.port()).isPositive();
		}
		finally {
			stub.stop();
		}
	}

	private static final class HttpsEnablingConfigurer
			implements HttpServerStubConfigurer<com.github.tomakehurst.wiremock.core.WireMockConfiguration> {

		@Override
		public boolean isAccepted(Object httpStubConfiguration) {
			return httpStubConfiguration instanceof com.github.tomakehurst.wiremock.core.WireMockConfiguration;
		}

		@Override
		public com.github.tomakehurst.wiremock.core.WireMockConfiguration configure(
				com.github.tomakehurst.wiremock.core.WireMockConfiguration httpStubConfiguration,
				HttpServerStubConfiguration httpServerStubConfiguration) {
			return httpStubConfiguration.dynamicHttpsPort();
		}

	}

	@Test
	void shouldListRegisteredMappingsAfterRegisteringRealStub() {
		WireMockHttpServerStub stub = new WireMockHttpServerStub();
		stub.start(config());
		try {
			stub.registerMappings(List.of(new File("src/test/resources/transformers.json")));
			// registering a real mapping populates the registered-mappings JSON (not
			// "[]")
			assertThat(stub.registeredMappings()).contains("/ping");
		}
		finally {
			stub.stop();
		}
	}

	@Test
	void shouldStartServeAndStop() {
		WireMockHttpServerStub stub = new WireMockHttpServerStub();
		HttpServerStub started = stub.start(config());
		try {
			assertThat(started).isSameAs(stub);
			assertThat(stub.isRunning()).isTrue();
			assertThat(stub.port()).isPositive();
			// starting an already-running server is a no-op returning the same instance
			assertThat(stub.start(config())).isSameAs(stub);
			assertThat(stub.reset()).isSameAs(stub);
			assertThat(stub.registeredMappings()).startsWith("[").endsWith("]");
			stub.registerMappings(List.of());
		}
		finally {
			assertThat(stub.stop()).isSameAs(stub);
		}
		assertThat(stub.isRunning()).isFalse();
	}

}
