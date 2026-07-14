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
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class StubRepositoryTests {

	private static final File REPOSITORY_LOCATION = new File("src/test/resources/repository");

	@Test
	void shouldRetrieveAllDescriptors() {
		StubRepository repository = new StubRepository(REPOSITORY_LOCATION, List.of(),
				new StubRunnerOptionsBuilder().build(), null);
		assertThat(repository.getStubs()).hasSize(8);
	}

	@Test
	void shouldThrowWhenNoStubsOrContractsPresent() {
		assertThatExceptionOfType(IllegalStateException.class)
			.isThrownBy(() -> new StubRepository(new File("src/test/resources/emptyrepo"), List.of(),
					new StubRunnerOptionsBuilder().build(), null));
	}

	@Test
	void shouldThrowWhenDirectoryWithMappingsIsMissing() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new StubRepository(new File("src/test/resources/nonexistingrepo"), List.of(),
					new StubRunnerOptionsBuilder().build(), null));
	}

	@Test
	void shouldRetrieveOnlyMappingsThatContainConsumerName() {
		StubRepository repository = new StubRepository(REPOSITORY_LOCATION, List.of(),
				new StubRunnerOptionsBuilder().withStubPerConsumer(true).withConsumerName("ping").build(), null);
		assertThat(repository.getStubs()).hasSize(1);
	}

}
