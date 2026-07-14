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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ClassPathStubResource}.
 */
class ClassPathStubResourceTest {

	@Test
	void stripsLeadingSlash() {
		ClassPathStubResource r = ResourceResolver.classpathResource("/logback.xml");
		assertThat(r.getPath()).isEqualTo("logback.xml");
	}

	@Test
	void keepsPathWithoutLeadingSlash() {
		ClassPathStubResource r = ResourceResolver.classpathResource("logback.xml");
		assertThat(r.getPath()).isEqualTo("logback.xml");
	}

	@Test
	void getFilenameReturnsLastSegment() {
		ClassPathStubResource r = ResourceResolver.classpathResource("some/path/logback.xml");
		assertThat(r.getFilename()).isEqualTo("logback.xml");
	}

	@Test
	void getDescriptionContainsPath() {
		ClassPathStubResource r = ResourceResolver.classpathResource("logback.xml");
		assertThat(r.getDescription()).contains("logback.xml");
	}

	@Test
	void existsReturnsTrueForPresentResource() {
		ClassPathStubResource r = ResourceResolver.classpathResource("logback.xml");
		assertThat(r.exists()).isTrue();
	}

	@Test
	void existsReturnsFalseForMissingResource() {
		ClassPathStubResource r = ResourceResolver.classpathResource("does-not-exist.xml");
		assertThat(r.exists()).isFalse();
	}

	@Test
	void getURLReturnsValidUrlForPresentResource() throws IOException {
		ClassPathStubResource r = ResourceResolver.classpathResource("logback.xml");
		URL url = r.getURL();
		assertThat(url).isNotNull();
		assertThat(url.toString()).endsWith("logback.xml");
	}

	@Test
	void getURLThrowsFileNotFoundForMissingResource() {
		ClassPathStubResource r = ResourceResolver.classpathResource("does-not-exist.xml");
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(r::getURL)
			.withMessageContaining("does-not-exist.xml");
	}

	@Test
	void getInputStreamOpensStreamForPresentResource() throws IOException {
		ClassPathStubResource r = ResourceResolver.classpathResource("logback.xml");
		try (var is = r.getInputStream()) {
			assertThat(is).isNotNull();
			assertThat(is.read()).isGreaterThanOrEqualTo(0);
		}
	}

	@Test
	void equalsByPath() {
		ClassPathStubResource a = ResourceResolver.classpathResource("logback.xml");
		ClassPathStubResource b = ResourceResolver.classpathResource("logback.xml");
		assertThat(a).isEqualTo(b);
		assertThat(a.hashCode()).isEqualTo(b.hashCode());
	}

	@Test
	void notEqualForDifferentPaths() {
		ClassPathStubResource a = ResourceResolver.classpathResource("logback.xml");
		ClassPathStubResource b = ResourceResolver.classpathResource("other.xml");
		assertThat(a).isNotEqualTo(b);
	}

	@Test
	void leadingSlashNormalizationPreservesEquality() {
		ClassPathStubResource withSlash = ResourceResolver.classpathResource("/logback.xml");
		ClassPathStubResource withoutSlash = ResourceResolver.classpathResource("logback.xml");
		assertThat(withSlash).isEqualTo(withoutSlash);
	}

}
