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
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;

import static org.assertj.core.api.Assertions.assertThat;

class StubServerMutationTests {

	private final StubConfiguration config = new StubConfiguration("group", "artifact", "1.0.0", "stubs");

	private StubServer server(HttpServerStub stub, Collection<Contract> contracts) {
		return new StubServer(this.config, List.of(new File("mapping.json")), contracts, stub);
	}

	@Test
	void shouldReportRunningState() {
		assertThat(server(new FakeHttpServerStub(true, 8080, -1), List.of()).isRunning()).isTrue();
		assertThat(server(new FakeHttpServerStub(false, 8080, -1), List.of()).isRunning()).isFalse();
	}

	@Test
	void shouldReturnHttpPortWhenRunning() {
		assertThat(server(new FakeHttpServerStub(true, 8080, -1), List.of()).getPort()).isEqualTo(8080);
	}

	@Test
	void shouldReturnHttpsPortWhenPresent() {
		assertThat(server(new FakeHttpServerStub(true, 8080, 8443), List.of()).getPort()).isEqualTo(8443);
	}

	@Test
	void shouldReturnMinusOneWhenNotRunning() {
		assertThat(server(new FakeHttpServerStub(false, 8080, -1), List.of()).getPort()).isEqualTo(-1);
	}

	@Test
	void shouldReturnHttpUrlWhenNoHttps() {
		assertThat(server(new FakeHttpServerStub(true, 8080, -1), List.of()).getStubUrl().toString())
			.isEqualTo("http://localhost:8080");
	}

	@Test
	void shouldReturnHttpsUrlWhenHttpsPresent() {
		assertThat(server(new FakeHttpServerStub(true, 8080, 8443), List.of()).getStubUrl().toString())
			.isEqualTo("https://localhost:8443");
	}

	@Test
	void shouldExposeHttpServerStub() {
		FakeHttpServerStub stub = new FakeHttpServerStub(true, 8080, -1);
		assertThat(server(stub, List.of()).httpServerStub()).isSameAs(stub);
	}

	@Test
	void shouldExposeConfigurationAndContracts() {
		Contract contract = new Contract();
		StubServer server = server(new FakeHttpServerStub(true, 8080, -1), List.of(contract));
		assertThat(server.getStubConfiguration()).isEqualTo(this.config);
		assertThat(server.getContracts()).containsExactly(contract);
	}

	@Test
	void shouldImplementEquals() {
		StubServer server = server(new FakeHttpServerStub(true, 8080, -1), List.of());
		StubServer same = server(new FakeHttpServerStub(false, 1, -1), List.of());
		StubServer differentConfig = new StubServer(new StubConfiguration("group", "other", "1.0.0", "stubs"),
				List.of(), List.of(), new FakeHttpServerStub(true, 8080, -1));
		StubServer differentContracts = server(new FakeHttpServerStub(true, 8080, -1), List.of(new Contract()));
		assertThat(server.equals(server)).isTrue();
		assertThat(server).isEqualTo(same);
		assertThat(server).hasSameHashCodeAs(same);
		assertThat(server).isNotEqualTo(differentConfig);
		assertThat(server).isNotEqualTo(differentContracts);
		assertThat(server).isNotEqualTo(null);
		assertThat(server).isNotEqualTo("not a stub server");
	}

	@Test
	void shouldImplementToString() {
		StubServer server = server(new FakeHttpServerStub(true, 8080, -1), List.of());
		assertThat(server.toString()).contains("group:artifact:1.0.0:stubs").contains("mappingsSize=1");
	}

	private static final class FakeHttpServerStub implements HttpServerStub {

		private final boolean running;

		private final int port;

		private final int httpsPort;

		FakeHttpServerStub(boolean running, int port, int httpsPort) {
			this.running = running;
			this.port = port;
			this.httpsPort = httpsPort;
		}

		@Override
		public int port() {
			return this.port;
		}

		@Override
		public int httpsPort() {
			return this.httpsPort;
		}

		@Override
		public boolean isRunning() {
			return this.running;
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

}
