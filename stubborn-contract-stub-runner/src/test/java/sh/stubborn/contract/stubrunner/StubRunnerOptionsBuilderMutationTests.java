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

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NullAway")
class StubRunnerOptionsBuilderMutationTests {

	private final StubRunnerOptionsBuilder builder = new StubRunnerOptionsBuilder();

	@Test
	void withStubsListReturnsSameBuilderAndRegistersStubs() {
		StubRunnerOptionsBuilder returned = this.builder.withStubs(List.of("group:artifact:1.0.0"));
		assertThat(returned).isSameAs(this.builder);
		assertThat(this.builder.build().getDependencies()).extracting(StubConfiguration::getArtifactId)
			.contains("artifact");
	}

	@Test
	void withStubsModeNullEnumReturnsSameBuilder() {
		assertThat(this.builder.withStubsMode((StubsMode) null)).isSameAs(this.builder);
	}

	@Test
	void withStubsModeNullStringReturnsSameBuilder() {
		assertThat(this.builder.withStubsMode((String) null)).isSameAs(this.builder);
	}

	@Test
	void withStubsModeAppliesGivenMode() {
		assertThat(this.builder.withStubsMode(StubsMode.REMOTE).build().getStubsMode()).isEqualTo(StubsMode.REMOTE);
	}

	@Test
	void withProxyReturnsSameBuilderAndStoresProxy() {
		StubRunnerOptionsBuilder returned = this.builder.withProxy("proxy.example", 8888);
		assertThat(returned).isSameAs(this.builder);
		StubRunnerOptions.StubRunnerProxyOptions proxy = Objects.requireNonNull(this.builder.build().getProxyOptions());
		assertThat(proxy.getProxyHost()).isEqualTo("proxy.example");
		assertThat(proxy.getProxyPort()).isEqualTo(8888);
	}

	@Test
	void portReturnsConcreteMappedValue() {
		StubRunnerOptions options = this.builder.withStubs("group:artifact:1.0.0:stubs:8080").build();
		assertThat(options.port(new StubConfiguration("group:artifact:1.0.0:stubs"))).isEqualTo(8080);
	}

	@Test
	void portReturnsNullForUnmappedConfiguration() {
		StubRunnerOptions options = this.builder.withStubs("group:artifact:1.0.0:stubs:8080").build();
		assertThat(options.port(new StubConfiguration("other:missing:1.0.0:stubs"))).isNull();
	}

	@Test
	void withStubRepositoryRootStringIgnoresNullAndBlank() {
		assertThat(this.builder.withStubRepositoryRoot((String) null).build().getStubRepositoryRoot()).isNull();
		assertThat(this.builder.withStubRepositoryRoot("   ").build().getStubRepositoryRoot()).isNull();
	}

	@Test
	void withStubRepositoryRootStringResolvesResource() {
		StubResource root = this.builder.withStubRepositoryRoot("classpath:logback.xml")
			.build()
			.getStubRepositoryRoot();
		assertThat(root).isNotNull();
		assertThat(root.getDescription()).contains("logback.xml");
	}

	@Test
	void withStubsModeStringAppliesGivenMode() {
		assertThat(this.builder.withStubsMode("REMOTE").build().getStubsMode()).isEqualTo(StubsMode.REMOTE);
	}

	@Test
	void withCommaSeparatedStubsAddsAllDependencies() {
		StubRunnerOptions options = this.builder.withStubs("group:one,group:two").build();
		assertThat(options.getDependencies()).extracting(StubConfiguration::getArtifactId)
			.containsExactlyInAnyOrder("one", "two");
	}

	@Test
	void withPortAppendsPortToLastStub() {
		int port = 12321;
		StubRunnerOptions options = this.builder.withStubs("group:artifact:1.0.0:stubs").withPort(port).build();
		assertThat(options.port(new StubConfiguration("group:artifact:1.0.0:stubs"))).isEqualTo(port);
	}

	@Test
	void withMinAndMaxPortAppliedIndividually() {
		StubRunnerOptions options = this.builder.withMinPort(1111).withMaxPort(2222).build();
		assertThat(options.getMinPortValue()).isEqualTo(1111);
		assertThat(options.getMaxPortValue()).isEqualTo(2222);
	}

}
