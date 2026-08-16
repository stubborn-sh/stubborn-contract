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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GitResourceMutationTests {

	private final GitResource resource = new GitResource("file:/tmp/git/repo/contracts.jar");

	@Test
	void shouldReturnDescription() {
		assertThat(this.resource.getDescription()).isEqualTo("file:/tmp/git/repo/contracts.jar");
	}

	@Test
	void shouldReturnUri() throws IOException {
		assertThat(this.resource.getURI().toString()).isEqualTo("file:/tmp/git/repo/contracts.jar");
	}

	@Test
	void shouldReturnUrl() throws IOException {
		assertThat(this.resource.getURL().toString()).isEqualTo("file:/tmp/git/repo/contracts.jar");
	}

	@Test
	void shouldReturnFile() throws IOException {
		assertThat(this.resource.getFile().getPath()).isEqualTo("/tmp/git/repo/contracts.jar");
	}

	@Test
	void shouldReturnFilenameFromSlashPath() {
		assertThat(this.resource.getFilename()).isEqualTo("contracts.jar");
	}

	@Test
	void shouldReturnFullNameWhenNoSeparator() {
		assertThat(new GitResource("contracts.jar").getFilename()).isEqualTo("contracts.jar");
	}

	@Test
	void shouldReturnFilenameFromBackslashPath() {
		assertThat(new GitResource("C:\\git\\contracts.jar").getFilename()).isEqualTo("contracts.jar");
	}

	@Test
	void shouldReturnFilenameWhenSeparatorIsAtStart() {
		// separator at index 0 exercises the sep >= 0 boundary
		assertThat(new GitResource("/leading.jar").getFilename()).isEqualTo("leading.jar");
	}

	@Test
	void shouldReturnNullInputStream() throws IOException {
		assertThat(this.resource.getInputStream()).isNull();
	}

}
