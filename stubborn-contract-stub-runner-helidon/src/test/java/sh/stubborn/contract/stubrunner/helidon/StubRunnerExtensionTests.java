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

package sh.stubborn.contract.stubrunner.helidon;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.StubsMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link StubRunnerExtension} boots a classpath stub, exposes it through
 * the injected {@link StubFinder}, and publishes its port as a
 * MicroProfile-Config-visible System property. The stubbed HTTP endpoint is exercised
 * with the JDK {@link HttpClient}, so the test is fully deterministic and does not
 * require booting a Helidon CDI container.
 *
 * @author Marcin Grzejszczak
 */
@StubRunner(ids = "sh.stubborn.test:helidon-producer:0.0.1:stubs", stubsMode = StubsMode.CLASSPATH)
class StubRunnerExtensionTests {

	@Test
	void shouldServeStubbedResponseFromRunningStub(StubFinder stubFinder) throws Exception {
		URL stubUrl = stubFinder.findStubUrl("sh.stubborn.test", "helidon-producer");
		assertThat(stubUrl).isNotNull();

		HttpResponse<String> response = get(stubUrl.toString() + "/hello");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).isEqualTo("{\"greeting\":\"hello from stubborn\"}");
	}

	@Test
	void shouldPublishRunningStubPortAsMicroProfileConfigSystemProperty(StubFinder stubFinder) {
		URL stubUrl = stubFinder.findStubUrl("sh.stubborn.test", "helidon-producer");

		String published = System.getProperty(StubRunnerExtension.STUBRUNNER_PREFIX + ".helidon-producer.port");

		assertThat(published).isNotNull();
		assertThat(Integer.parseInt(published)).isEqualTo(stubUrl.getPort());
	}

	private HttpResponse<String> get(String url) throws Exception {
		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.timeout(Duration.ofSeconds(5))
			.GET()
			.build();
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

}
