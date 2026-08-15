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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.spec.Contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class BatchStubRunnerMutationTests {

	private final StubConfiguration config = new StubConfiguration("group:artifact:1.0.0");

	@Test
	void shouldMergeContractsForTheSameConfiguration() {
		Contract first = mock(Contract.class);
		Contract second = mock(Contract.class);
		StubRunner runnerOne = mock(StubRunner.class);
		StubRunner runnerTwo = mock(StubRunner.class);
		given(runnerOne.getContracts()).willReturn(Map.of(this.config, List.of(first)));
		given(runnerTwo.getContracts()).willReturn(Map.of(this.config, List.of(second)));
		BatchStubRunner batch = new BatchStubRunner(List.of(runnerOne, runnerTwo));

		Map<StubConfiguration, Collection<Contract>> contracts = batch.getContracts();

		assertThat(contracts).containsOnlyKeys(this.config);
		assertThat(contracts.get(this.config)).containsExactlyInAnyOrder(first, second);
	}

	@Test
	void shouldReturnTrueWhenAnyRunnerTriggersByNotationAndLabel() {
		StubRunner runner = mock(StubRunner.class);
		given(runner.trigger("group:artifact", "label")).willReturn(true);
		BatchStubRunner batch = new BatchStubRunner(List.of(runner));
		assertThat(batch.trigger("group:artifact", "label")).isTrue();
	}

	@Test
	void shouldThrowWhenNoRunnerTriggersByNotationAndLabel() {
		StubRunner runner = mock(StubRunner.class);
		given(runner.trigger("group:artifact", "label")).willReturn(false);
		given(runner.labels()).willReturn(Map.of("group:artifact:1.0.0", List.of("other")));
		BatchStubRunner batch = new BatchStubRunner(List.of(runner));
		assertThatIllegalArgumentException().isThrownBy(() -> batch.trigger("group:artifact", "label"))
			.withMessageContaining("No label with name [label]")
			.withMessageContaining("group:artifact");
	}

	@Test
	void shouldReturnTrueWhenAnyRunnerTriggersByLabel() {
		StubRunner runner = mock(StubRunner.class);
		given(runner.trigger("label")).willReturn(true);
		BatchStubRunner batch = new BatchStubRunner(List.of(runner));
		assertThat(batch.trigger("label")).isTrue();
	}

	@Test
	void shouldReturnTrueWhenAnyRunnerTriggersAll() {
		StubRunner runner = mock(StubRunner.class);
		given(runner.trigger()).willReturn(true);
		BatchStubRunner batch = new BatchStubRunner(List.of(runner));
		assertThat(batch.trigger()).isTrue();
	}

	@Test
	void shouldReturnFalseWhenNoRunnerTriggersAll() {
		StubRunner runner = mock(StubRunner.class);
		given(runner.trigger()).willReturn(false);
		BatchStubRunner batch = new BatchStubRunner(List.of(runner));
		assertThat(batch.trigger()).isFalse();
	}

}
