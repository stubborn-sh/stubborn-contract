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

class RunningStubsTests {

	private final RunningStubs runningStubs = new RunningStubs(
			Map.of(new StubConfiguration("group", "artifact", "version", "classifier"), 100));

	@Test
	void shouldGetPortByGroupAndArtifactId() {
		assertThat(this.runningStubs.getPort("group", "artifact")).isEqualTo(100);
	}

	@Test
	void shouldGetPortByArtifactId() {
		assertThat(this.runningStubs.getPort("artifact")).isEqualTo(100);
	}

	@Test
	void shouldGetPortByGroupAndArtifactIdInIvyNotation() {
		assertThat(this.runningStubs.getPort("group:artifact")).isEqualTo(100);
	}

	@Test
	void shouldGetPortByGroupArtifactAndVersionInIvyNotation() {
		assertThat(this.runningStubs.getPort("group:artifact:version")).isEqualTo(100);
	}

	@Test
	void shouldGetPortByFullIvyNotation() {
		assertThat(this.runningStubs.getPort("group:artifact:version:classifier")).isEqualTo(100);
	}

	@Test
	void shouldReturnNullForMissingArtifact() {
		assertThat(this.runningStubs.getPort("missing artifact id")).isNull();
	}

	@Test
	void shouldFindStubByGroupAndArtifactId() {
		assertThat(this.runningStubs.isPresent("group", "artifact")).isTrue();
	}

	@Test
	void shouldFindStubByArtifactId() {
		assertThat(this.runningStubs.isPresent("artifact")).isTrue();
	}

	@Test
	void shouldFindStubByGroupAndArtifactIvyNotation() {
		assertThat(this.runningStubs.isPresent("group:artifact")).isTrue();
	}

	@Test
	void shouldFindStubByGroupArtifactVersionIvyNotation() {
		assertThat(this.runningStubs.isPresent("group:artifact:version")).isTrue();
	}

	@Test
	void shouldFindStubByFullIvyNotation() {
		assertThat(this.runningStubs.isPresent("group:artifact:version:classifier")).isTrue();
	}

	@Test
	void shouldReturnFalseForMissingArtifact() {
		assertThat(this.runningStubs.isPresent("missing artifact id")).isFalse();
	}

}
