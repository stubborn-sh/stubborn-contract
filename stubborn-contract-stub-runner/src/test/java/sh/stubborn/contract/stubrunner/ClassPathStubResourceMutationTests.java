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

class ClassPathStubResourceMutationTests {

	@Test
	void shouldNotBeEqualToOtherTypeOrNull() {
		ClassPathStubResource resource = ResourceResolver.classpathResource("logback.xml");
		assertThat(resource).isNotEqualTo("logback.xml");
		assertThat(resource).isNotEqualTo(null);
	}

	@Test
	void shouldReturnExistingFile() throws IOException {
		ClassPathStubResource resource = ResourceResolver.classpathResource("logback.xml");
		assertThat(resource.getFile().getName()).isEqualTo("logback.xml");
		assertThat(resource.getFile()).exists();
	}

	@Test
	void shouldReturnFilenameWhenSeparatorAtStart() {
		// leading backslash lands the separator at index 0, exercising the sep >= 0
		// boundary
		ClassPathStubResource resource = ResourceResolver.classpathResource("\\logback.xml");
		assertThat(resource.getFilename()).isEqualTo("logback.xml");
	}

	@Test
	void shouldProduceDistinctHashCodesForDistinctPaths() {
		ClassPathStubResource one = ResourceResolver.classpathResource("logback.xml");
		ClassPathStubResource other = ResourceResolver.classpathResource("other/path.xml");
		assertThat(one.hashCode()).isNotEqualTo(other.hashCode());
	}

}
