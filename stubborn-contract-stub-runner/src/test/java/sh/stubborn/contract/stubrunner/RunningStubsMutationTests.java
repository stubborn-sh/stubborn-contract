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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NullAway")
class RunningStubsMutationTests {

	private final StubConfiguration config = new StubConfiguration("group", "artifact", "1.0.0", "stubs");

	private final RunningStubs runningStubs = new RunningStubs(Map.of(this.config, 8080));

	@Test
	void shouldReturnConcretePortByGroupAndArtifact() {
		assertThat(this.runningStubs.getPort("group", "artifact")).isEqualTo(8080);
		assertThat(this.runningStubs.getPort("group", "missing")).isNull();
	}

	@Test
	void shouldExposeAllServices() {
		assertThat(this.runningStubs.getAllServices()).containsExactly(this.config);
	}

	@Test
	void shouldExposeAllServiceNames() {
		assertThat(this.runningStubs.getAllServicesNames()).containsExactly("artifact");
	}

	@Test
	void shouldExposeIvyToPortMapping() {
		assertThat(this.runningStubs.toIvyToPortMapping())
			.containsExactly(Map.entry("group:artifact:1.0.0:stubs", 8080));
	}

	@Test
	void shouldFilterNegativeAndNullPorts() {
		Map<StubConfiguration, Integer> map = new LinkedHashMap<>();
		map.put(this.config, 8080);
		map.put(new StubConfiguration("group", "negative", "1.0.0", "stubs"), -1);
		RunningStubs stubs = new RunningStubs(map);
		assertThat(stubs.validNamesAndPorts()).containsExactly(Map.entry(this.config, 8080));
	}

	@Test
	void shouldMergeMultipleRunningStubs() {
		StubConfiguration other = new StubConfiguration("group", "second", "1.0.0", "stubs");
		RunningStubs first = new RunningStubs(Map.of(this.config, 8080));
		RunningStubs second = new RunningStubs(Map.of(other, 9090));
		RunningStubs merged = new RunningStubs(List.of(first, second));
		assertThat(merged.getPort("artifact")).isEqualTo(8080);
		assertThat(merged.getPort("second")).isEqualTo(9090);
	}

	@Test
	void shouldImplementEquals() {
		RunningStubs same = new RunningStubs(Map.of(this.config, 8080));
		RunningStubs differentPort = new RunningStubs(Map.of(this.config, 9090));
		assertThat(this.runningStubs.equals(this.runningStubs)).isTrue();
		assertThat(this.runningStubs).isEqualTo(same);
		assertThat(this.runningStubs).hasSameHashCodeAs(same);
		assertThat(this.runningStubs).isNotEqualTo(differentPort);
		assertThat(this.runningStubs).isNotEqualTo(null);
		assertThat(this.runningStubs).isNotEqualTo("not a running stubs");
	}

	@Test
	void shouldImplementToString() {
		assertThat(this.runningStubs.toString()).contains("group:artifact:1.0.0:stubs").contains("8080");
	}

	@Test
	void shouldReportPresenceByArtifactAndByGroupArtifact() {
		assertThat(this.runningStubs.isPresent("artifact")).isTrue();
		assertThat(this.runningStubs.isPresent("missing")).isFalse();
		assertThat(this.runningStubs.isPresent("group", "artifact")).isTrue();
		assertThat(this.runningStubs.isPresent("group", "missing")).isFalse();
	}

	@Test
	void shouldReturnNullPortAndEntryForMissingArtifact() {
		assertThat(this.runningStubs.getPort("missing")).isNull();
		assertThat(this.runningStubs.getEntry("missing")).isNull();
		assertThat(this.runningStubs.getEntry("artifact")).isNotNull();
	}

	@Test
	void shouldExcludeNullPortsFromValidNamesAndPorts() {
		Map<StubConfiguration, Integer> map = new LinkedHashMap<>();
		map.put(this.config, 8080);
		map.put(new StubConfiguration("group", "nullport", "1.0.0", "stubs"), null);
		RunningStubs stubs = new RunningStubs(map);
		assertThat(stubs.validNamesAndPorts()).containsExactly(Map.entry(this.config, 8080));
	}

}
