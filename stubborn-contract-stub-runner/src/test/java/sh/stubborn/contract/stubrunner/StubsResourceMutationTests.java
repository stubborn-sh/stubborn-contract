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

class StubsResourceMutationTests {

	private final StubsResource resource = new StubsResource("file:/tmp/stubs/foo/bar-1.0.0.jar");

	@Test
	void shouldReturnDescription() {
		assertThat(this.resource.getDescription()).isEqualTo("file:/tmp/stubs/foo/bar-1.0.0.jar");
	}

	@Test
	void shouldReturnUri() throws IOException {
		assertThat(this.resource.getURI().toString()).isEqualTo("file:/tmp/stubs/foo/bar-1.0.0.jar");
	}

	@Test
	void shouldReturnUrl() throws IOException {
		assertThat(this.resource.getURL().toString()).isEqualTo("file:/tmp/stubs/foo/bar-1.0.0.jar");
	}

	@Test
	void shouldReturnFile() throws IOException {
		assertThat(this.resource.getFile().getPath()).isEqualTo("/tmp/stubs/foo/bar-1.0.0.jar");
	}

	@Test
	void shouldReturnFilenameFromSlashSeparatedPath() {
		assertThat(this.resource.getFilename()).isEqualTo("bar-1.0.0.jar");
	}

	@Test
	void shouldReturnFullNameWhenNoSeparator() {
		assertThat(new StubsResource("bar-1.0.0.jar").getFilename()).isEqualTo("bar-1.0.0.jar");
	}

	@Test
	void shouldReturnFilenameFromBackslashSeparatedPath() {
		assertThat(new StubsResource("C:\\stubs\\bar-1.0.0.jar").getFilename()).isEqualTo("bar-1.0.0.jar");
	}

	@Test
	void shouldReturnFilenameWhenSeparatorIsAtStart() {
		// separator at index 0 exercises the sep >= 0 boundary
		assertThat(new StubsResource("/leading.jar").getFilename()).isEqualTo("leading.jar");
	}

	@Test
	void shouldProduceDistinctHashCodesForDistinctLocations() {
		assertThat(new StubsResource("a.jar").hashCode()).isNotEqualTo(new StubsResource("b.jar").hashCode());
	}

	@Test
	void shouldReturnNullInputStream() throws IOException {
		assertThat(this.resource.getInputStream()).isNull();
	}

	@Test
	void shouldImplementEquals() {
		StubsResource same = new StubsResource("file:/tmp/stubs/foo/bar-1.0.0.jar");
		StubsResource different = new StubsResource("file:/tmp/stubs/foo/other.jar");
		assertThat(this.resource.equals(this.resource)).isTrue();
		assertThat(this.resource).isEqualTo(same);
		assertThat(this.resource).hasSameHashCodeAs(same);
		assertThat(this.resource).isNotEqualTo(different);
		assertThat(this.resource).isNotEqualTo(null);
		assertThat(this.resource).isNotEqualTo("file:/tmp/stubs/foo/bar-1.0.0.jar");
	}

}
