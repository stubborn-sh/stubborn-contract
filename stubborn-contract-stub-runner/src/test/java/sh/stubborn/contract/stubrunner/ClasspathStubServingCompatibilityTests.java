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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.verifier.messaging.noop.NoOpStubMessages;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full HTTP round-trip through the Spring-free stub runner, starting a real WireMock
 * server from {@code StubsMode.CLASSPATH} stubs published exactly the way Spring Cloud
 * Contract installs them ({@code META-INF/<group>/<artifact>/<version>/mappings/*.json}).
 * Where {@link ClasspathStubResolutionCompatibilityTests} proves the stubs are
 * <em>resolved</em>, this proves the whole pipeline — resolve → unpack → register in
 * WireMock → answer an HTTP request — works for SCC-produced stubs, including the
 * {@code response-template} transformer that SCC 5.x stubs rely on.
 */
class ClasspathStubServingCompatibilityTests {

	@Test
	void servesAnInstalledStubsJarOverHttp() throws Exception {
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withStubsMode(StubsMode.CLASSPATH)
			.withStubs("com.example:stubborn-scc-http")
			.build();
		StubRunner runner = firstStubRunner(options);
		try {
			runner.runStubs();
			URL url = runner.findStubUrl("com.example", "stubborn-scc-http");
			Response response = get(url + "/greeting");
			assertThat(response.status()).isEqualTo(200);
			assertThat(response.body()).isEqualTo("hello from the installed stubs jar");
		}
		finally {
			runner.close();
		}
	}

	@Test
	void servesAStubUsingTheSccResponseTemplateTransformer() throws Exception {
		// The stub body is "Hello {{request.query.name}}" with "transformers":
		// ["response-template"] — the SCC 5.x templating that must keep working
		// unmodified.
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withStubsMode(StubsMode.CLASSPATH)
			.withStubs("com.example:stubborn-scc-template")
			.build();
		StubRunner runner = firstStubRunner(options);
		try {
			runner.runStubs();
			URL url = runner.findStubUrl("com.example", "stubborn-scc-template");
			Response response = get(url + "/hello?name=Stubborn");
			assertThat(response.status()).isEqualTo(200);
			assertThat(response.body()).isEqualTo("Hello Stubborn");
		}
		finally {
			runner.close();
		}
	}

	@Test
	void stubPerConsumerCollectsOnlyTheSelectedConsumersStubs() {
		// Two consumers share one producer:
		// stubs-per-consumer/com.example/beer-api-producer/beer-api-consumer-a/... and
		// .../beer-api-consumer-b/... . With stub-per-consumer enabled and consumer "a"
		// selected, only consumer a's mapping is collected for registration.
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withStubPerConsumer(true)
			.withConsumerName("beer-api-consumer-a")
			.build();
		StubRepository repository = new StubRepository(
				new File("src/test/resources/stubs-per-consumer/com.example/beer-api-producer"), new ArrayList<>(),
				options, null);

		List<String> collected = repository.getStubs().stream().map(File::getName).toList();

		assertThat(collected).containsExactly("consumer-a-beer.json");
	}

	private static StubRunner firstStubRunner(StubRunnerOptions options) {
		StubDownloader downloader = Objects.requireNonNull(new ClasspathStubProvider().build(options));
		return new StubRunnerFactory(options, downloader, new NoOpStubMessages()).createStubsFromServiceConfiguration()
			.iterator()
			.next();
	}

	private static Response get(String url) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setRequestMethod("GET");
		try {
			int status = connection.getResponseCode();
			InputStream stream = (status >= 200 && status < 400) ? connection.getInputStream()
					: connection.getErrorStream();
			String body = (stream != null) ? new String(stream.readAllBytes()) : "";
			return new Response(status, body);
		}
		finally {
			connection.disconnect();
		}
	}

	private record Response(int status, String body) {
	}

}
