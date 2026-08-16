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
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NullAway")
class ClasspathStubProviderMutationTests {

	@Test
	void resolvesFromClassPathResourceRepositoryRoot() {
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withStubsMode(StubsMode.CLASSPATH)
			.withStubRepositoryRoot(
					ResourceResolver.classpathResource("contracts/com.example/stubborn-classpath-direct"))
			.withFailOnNoStubs(false)
			.build();
		StubDownloader downloader = Objects.requireNonNull(new ClasspathStubProvider().build(options));

		// exercises the ClassPathStubResource branch of repoRoot() which uses the
		// resource's
		// own path as the single repository root
		Map.Entry<StubConfiguration, File> entry = downloader
			.downloadAndUnpackStubJar(new StubConfiguration("com.example", "stubborn-classpath-direct", "+"));

		assertThat(entry).isNotNull();
		assertThat(entry.getValue()).exists();
	}

	@Test
	void usesDefaultRootsWhenRepositoryRootIsBlankClassPathResource() {
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withStubsMode(StubsMode.CLASSPATH)
			.withStubRepositoryRoot(ResourceResolver.classpathResource(""))
			.withFailOnNoStubs(false)
			.build();
		StubDownloader downloader = Objects.requireNonNull(new ClasspathStubProvider().build(options));

		// blank classpath path -> falls through to the default
		// META-INF/contracts/mappings
		// roots
		Map.Entry<StubConfiguration, File> entry = downloader
			.downloadAndUnpackStubJar(new StubConfiguration("com.example", "stubborn-classpath-direct", "+"));

		assertThat(entry).isNotNull();
	}

}
