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

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GitStubDownloaderPropertiesMutationTests {

	private GitStubDownloaderProperties properties(Map<String, String> props) {
		StubResource resource = new GitResource("git://https://example.com/repo.git");
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withProperties(props).build();
		return new GitStubDownloaderProperties(resource, options);
	}

	@Test
	void shouldDefaultBranchToMaster() {
		assertThat(properties(Map.of()).branch).isEqualTo("master");
	}

	@Test
	void shouldUseConfiguredBranch() {
		assertThat(properties(Map.of("git.branch", "develop")).branch).isEqualTo("develop");
	}

	@Test
	void shouldDefaultEnsureGitSuffixToTrue() {
		assertThat(properties(Map.of()).ensureGitSuffix).isTrue();
	}

	@Test
	void shouldUseConfiguredEnsureGitSuffix() {
		assertThat(properties(Map.of("git.ensure-git-suffix", "false")).ensureGitSuffix).isFalse();
	}

	@Test
	void shouldUseConfiguredCredentials() {
		GitStubDownloaderProperties props = properties(Map.of("git.username", "user", "git.password", "secret"));
		assertThat(props.username).isEqualTo("user");
		assertThat(props.password).isEqualTo("secret");
	}

	@Test
	void shouldFallBackToOptionCredentialsWhenGitPropertiesAbsent() {
		StubResource resource = new GitResource("git://https://example.com/repo.git");
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withUsername("rootuser")
			.withPassword("rootpass")
			.build();
		GitStubDownloaderProperties props = new GitStubDownloaderProperties(resource, options);
		assertThat(props.username).isEqualTo("rootuser");
		assertThat(props.password).isEqualTo("rootpass");
	}

	@Test
	void blankGitPropertiesFallBackToDefaultsAndOptions() {
		StubResource resource = new GitResource("git://https://example.com/repo.git");
		// blank git.* properties must be treated as absent (isBlank branch), falling back
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withUsername("optuser")
			.withPassword("optpass")
			.withProperties(Map.of("git.username", "  ", "git.password", "  ", "git.branch", "  ",
					"git.ensure-git-suffix", "  "))
			.build();
		GitStubDownloaderProperties props = new GitStubDownloaderProperties(resource, options);
		assertThat(props.username).isEqualTo("optuser");
		assertThat(props.password).isEqualTo("optpass");
		assertThat(props.branch).isEqualTo("master");
		assertThat(props.ensureGitSuffix).isTrue();
	}

	@Test
	void shouldKeepFullAddressForGitAtUrl() {
		StubResource resource = new GitResource("git://git@github.com/foo/bar.git");
		GitStubDownloaderProperties props = new GitStubDownloaderProperties(resource,
				new StubRunnerOptionsBuilder().build());
		assertThat(props.url.toString()).isEqualTo("git:git@github.com/foo/bar.git");
	}

}
