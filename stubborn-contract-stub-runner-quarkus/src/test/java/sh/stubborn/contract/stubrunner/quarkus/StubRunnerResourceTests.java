/*
 * Copyright 2026-present the original author or authors.
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

package sh.stubborn.contract.stubrunner.quarkus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Adapter-level test for {@link StubRunnerResource}. Drives the Quarkus test-resource
 * lifecycle directly (no full Quarkus boot needed to exercise the adapter) against a
 * classpath WireMock stub and asserts a real HTTP call hits the running stub.
 *
 * @author Stubborn Contract
 */
class StubRunnerResourceTests {

	private final StubRunnerResource resource = new StubRunnerResource();

	@AfterEach
	void tearDown() {
		this.resource.stop();
	}

	@Test
	void startsClasspathStubAndServesStubbedBody() throws Exception {
		this.resource.init(Map.of(StubRunnerResource.IDS, "com.example:producer:+:stubs", StubRunnerResource.STUBS_MODE,
				"CLASSPATH"));

		Map<String, String> config = this.resource.start();

		String url = config.get(StubRunnerResource.STUBRUNNER_PREFIX + ".producer.url");
		assertThat(url).as("published base URL for the running stub").isNotNull();
		assertThat(config).containsKey(StubRunnerResource.STUBRUNNER_PREFIX + ".producer.port");
		assertThat(config).containsKey(StubRunnerResource.STUBRUNNER_PREFIX + ".com.example.producer.url");

		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder(URI.create(url + "/hello")).GET().build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).isEqualTo("Hello from Stubborn Contract stub");
	}

	@Test
	void missingIdsInitArgFailsFast() {
		this.resource.init(Map.of(StubRunnerResource.STUBS_MODE, "CLASSPATH"));

		try {
			this.resource.start();
			assertThat(false).as("expected IllegalArgumentException for missing 'ids'").isTrue();
		}
		catch (IllegalArgumentException ex) {
			assertThat(ex).hasMessageContaining(StubRunnerResource.IDS);
		}
	}

}
