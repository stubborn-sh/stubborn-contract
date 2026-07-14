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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GitStubDownloaderPropertiesTests {

	@Test
	void shouldParseOnlyUrlAfterProtocolIfItDoesNotStartWithGit() {
		StubResource resource = resource("git://https://foo.com");
		GitStubDownloaderProperties props = new GitStubDownloaderProperties(resource,
				new StubRunnerOptionsBuilder().build());
		assertThat(props.url).isEqualTo(URI.create("https://foo.com"));
	}

	@Test
	void shouldReturnWholeAddressIfItStartsWithGitAtButDoesNotFinishWithDotGit() {
		StubResource resource = resource("git://git@foo.com/foo");
		GitStubDownloaderProperties props = new GitStubDownloaderProperties(resource,
				new StubRunnerOptionsBuilder().build());
		assertThat(props.url).isEqualTo(URI.create("git:git@foo.com/foo"));
	}

	private static StubResource resource(String resourceUri) {
		return new StubResource() {
			@Override
			public URI getURI() {
				return URI.create(resourceUri);
			}

			@Override
			public URL getURL() {
				return null;
			}

			@Override
			public File getFile() {
				return null;
			}

			@Override
			public InputStream getInputStream() {
				return null;
			}

			@Override
			public String getFilename() {
				return null;
			}

			@Override
			public String getDescription() {
				return null;
			}
		};
	}

}
