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

package sh.stubborn.contract.stubrunner.micronaut;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.stubrunner.StubRunnerOptions;
import sh.stubborn.contract.stubrunner.StubRunnerOptionsBuilder;
import sh.stubborn.contract.stubrunner.StubsMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link StubRunnerSupport} boots a classpath stub and that an HTTP call
 * against the resolved stub URL returns the stubbed body. Runs the pure adapter without
 * booting a Micronaut application context.
 *
 * @author Marcin Grzejszczak
 */
class StubRunnerSupportTests {

	private static final String GROUP_ID = "com.example";

	private static final String ARTIFACT_ID = "stubborn-micronaut-producer";

	private @Nullable StubRunnerSupport support;

	@AfterEach
	void cleanup() throws Exception {
		if (this.support != null) {
			this.support.close();
		}
	}

	@Test
	void shouldStartClasspathStubAndServeStubbedBody() throws Exception {
		this.support = new StubRunnerSupport(classpathOptions()).start();

		URL stubUrl = this.support.stubFinder().findStubUrl(GROUP_ID, ARTIFACT_ID);

		assertThat(stubUrl).isNotNull();
		assertThat(get(stubUrl + "/name")).isEqualTo("stubborn-micronaut");
	}

	@Test
	void shouldExposeRunningStubsAsMicronautProperties() {
		this.support = new StubRunnerSupport(classpathOptions()).start();

		int port = Objects.requireNonNull(this.support.runningStubs().getPort(ARTIFACT_ID));
		String prefix = StubRunnerSupport.STUBRUNNER_PREFIX;

		assertThat(this.support.toProperties())
			.containsEntry(prefix + "." + ARTIFACT_ID + ".port", String.valueOf(port))
			.containsEntry(prefix + "." + ARTIFACT_ID + ".url", "http://localhost:" + port)
			.containsEntry(prefix + "." + GROUP_ID + "." + ARTIFACT_ID + ".port", String.valueOf(port))
			.containsEntry(prefix + "." + GROUP_ID + "." + ARTIFACT_ID + ".url", "http://localhost:" + port);
	}

	private StubRunnerOptions classpathOptions() {
		return new StubRunnerOptionsBuilder().withStubsMode(StubsMode.CLASSPATH)
			.withMinMaxPort(10000, 15000)
			.withStubs(GROUP_ID + ":" + ARTIFACT_ID)
			.build();
	}

	private String get(String url) throws Exception {
		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).isEqualTo(200);
		return response.body();
	}

}
