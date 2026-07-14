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

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BatchStubRunnerTests {

	private static final String KNOWN_STUB_PATH = "group:knownArtifact";

	private static final String UNKNOWN_STUB_PATH = "group:unknownArtifact";

	private static URL knownUrl() throws Exception {
		return new URL("http://localhost:8080");
	}

	private static List<StubRunner> runners() throws Exception {
		StubRunner runner = mock(StubRunner.class);
		when(runner.findStubUrl("group", "knownArtifact")).thenReturn(knownUrl());
		when(runner.findStubUrl("group:knownArtifact")).thenReturn(knownUrl());
		when(runner.findStubUrl("group:unknownArtifact")).thenThrow(new StubNotFoundException(UNKNOWN_STUB_PATH));
		when(runner.labels()).thenReturn(Map.of("a:b:c", List.of("foo")));
		return List.of(runner);
	}

	@Test
	void shouldProvideStubUrlFromEnclosedStubRunner() throws Exception {
		BatchStubRunner batchStubRunner = new BatchStubRunner(runners());
		assertThat(batchStubRunner.findStubUrl(KNOWN_STUB_PATH)).isEqualTo(knownUrl());
	}

	@Test
	void shouldThrowForUnknownStubPath() throws Exception {
		BatchStubRunner batchStubRunner = new BatchStubRunner(runners());
		assertThatExceptionOfType(StubNotFoundException.class)
			.isThrownBy(() -> batchStubRunner.findStubUrl(UNKNOWN_STUB_PATH));
	}

	@Test
	void shouldThrowWhenTriggeringNonExistentLabel() throws Exception {
		BatchStubRunner batchStubRunner = new BatchStubRunner(runners());
		assertThatIllegalArgumentException().isThrownBy(() -> batchStubRunner.trigger("non existing label"))
			.withMessage(
					"No label with name [non existing label] was found. Here you have the list of dependencies and their labels [Dependency [a:b:c] has labels [foo]]");
	}

}
