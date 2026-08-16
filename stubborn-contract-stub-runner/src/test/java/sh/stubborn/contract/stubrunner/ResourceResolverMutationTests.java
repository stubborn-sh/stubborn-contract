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

import java.io.IOException;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("NullAway")
class ResourceResolverMutationTests {

	private static final String URL = "http://localhost:1/some/path/contracts.jar?token=abc";

	@Test
	void shouldReturnNullForNullOrBlank() {
		assertThat(ResourceResolver.resource(null)).isNull();
		assertThat(ResourceResolver.resource("")).isNull();
		assertThat(ResourceResolver.resource("   ")).isNull();
	}

	@Test
	void shouldResolveClasspathPrefixToClasspathResource() {
		StubResource resource = Objects.requireNonNull(ResourceResolver.resource("classpath:logback.xml"));
		assertThat(resource).isInstanceOf(ClassPathStubResource.class);
		assertThat(resource.getDescription()).contains("logback.xml");
	}

	@Test
	void shouldResolveSchemelessLocationToClasspathResource() {
		StubResource resource = ResourceResolver.resource("logback.xml");
		assertThat(resource).isInstanceOf(ClassPathStubResource.class);
	}

	@Test
	void plainUrlResourceExposesDescription() {
		assertThat(resolvePlainUrl().getDescription()).isEqualTo("URL [" + URL + "]");
	}

	@Test
	void plainUrlResourceStripsQueryFromFilename() {
		assertThat(resolvePlainUrl().getFilename()).isEqualTo("contracts.jar");
	}

	@Test
	void plainUrlResourceReturnsUrlAndUri() throws IOException {
		StubResource resource = resolvePlainUrl();
		assertThat(resource.getURL().toString()).isEqualTo(URL);
		assertThat(resource.getURI().toString()).isEqualTo(URL);
	}

	@Test
	void plainUrlResourceGetFileFailsForNonFileScheme() {
		assertThatThrownBy(() -> resolvePlainUrl().getFile()).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void plainUrlResourceGetInputStreamOpensConnection() {
		// nothing is listening on port 1, so opening a stream fails with an IOException
		// (a null-return mutant would instead yield null and not throw)
		assertThatThrownBy(() -> resolvePlainUrl().getInputStream()).isInstanceOf(IOException.class);
	}

	private StubResource resolvePlainUrl() {
		return Objects.requireNonNull(ResourceResolver.resource(URL));
	}

}
