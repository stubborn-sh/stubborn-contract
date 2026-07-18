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
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sh.stubborn.contract.stubrunner.StubsMode;

import static org.assertj.core.api.Assertions.assertThat;

class StubRunnerOptionsBuilderTests {

	private final StubRunnerOptionsBuilder builder = new StubRunnerOptionsBuilder();

	private Map<String, String> savedSystemProps;

	@BeforeEach
	void saveSystemProps() {
		this.savedSystemProps = Map.of();
	}

	@AfterEach
	void restoreSystemProps() {
		clearStubRunnerSystemProps();
	}

	@Test
	void shouldReturnURIOfAResourceFromString() {
		this.builder.withStubRepositoryRoot("classpath:/logback.xml");
		StubRunnerOptions options = this.builder.build();
		assertThat(options.getStubRepositoryRootAsString()).startsWith("file:/").endsWith("logback.xml");
	}

	@Test
	void shouldReturnURIOfAResourceFromResource() {
		this.builder.withStubRepositoryRoot(ResourceResolver.classpathResource("logback.xml"));
		StubRunnerOptions options = this.builder.build();
		assertThat(options.getStubRepositoryRootAsString()).startsWith("file:/").endsWith("logback.xml");
	}

	@Test
	void shouldReturnEmptyStringWhenFileNotFound() {
		this.builder.withStubRepositoryRoot(ResourceResolver.classpathResource("fileThatDoesNotExist.xml"));
		StubRunnerOptions options = this.builder.build();
		assertThat(options.getStubRepositoryRootAsString()).isEqualTo("");
	}

	@Test
	void shouldCreateDependenciesForStub() {
		this.builder.withStubs("foo:bar");
		StubRunnerOptions options = this.builder.build();
		assertThat(options.getDependencies().toString()).isEqualTo("[foo:bar:+:stubs]");
	}

	@Test
	void shouldCreateDependenciesForStubWithSameGroupAndArtifactId() {
		this.builder.withStubs("foo:bar:1.0", "foo:bar:2.0");
		StubRunnerOptions options = this.builder.build();
		assertThat(options.getDependencies().toString()).isEqualTo("[foo:bar:1.0:stubs, foo:bar:2.0:stubs]");
	}

	@Test
	void shouldCreateDependenciesForMultipleStubsWithSameGroup() {
		this.builder.withStubs("foo:bar", "foo:baz", "foo:baz2");
		StubRunnerOptions options = this.builder.build();
		assertThat(options.getDependencies().toString())
			.isEqualTo("[foo:bar:+:stubs, foo:baz:+:stubs, foo:baz2:+:stubs]");
	}

	@Test
	void shouldCreateDependenciesForMultipleStubsWithSameArtifactId() {
		this.builder.withStubs("bar:foo", "baz:foo", "baz2:foo");
		StubRunnerOptions options = this.builder.build();
		assertThat(options.getDependencies().toString())
			.isEqualTo("[bar:foo:+:stubs, baz:foo:+:stubs, baz2:foo:+:stubs]");
	}

	@Test
	void shouldCreateDependenciesForCommaSeparatedStubs() {
		this.builder.withStubs("foo:bar,bar:foo,foo:baz");
		StubRunnerOptions options = this.builder.build();
		assertThat(options.getDependencies()).hasSize(3);
	}

	@Test
	void shouldCreateDependenciesForCommaSeparatedStubsWithSameArtifact() {
		this.builder.withStubs("bar:foo,baz:foo,baz2:foo");
		StubRunnerOptions options = this.builder.build();
		assertThat(options.getDependencies().toString())
			.isEqualTo("[bar:foo:+:stubs, baz:foo:+:stubs, baz2:foo:+:stubs]");
	}

	@Test
	void shouldMapStubsWithPort() {
		this.builder.withStubs("foo:bar:8080");
		StubRunnerOptions options = this.builder.build();
		assertThat(options.getStubIdsToPortMapping().toString()).isEqualTo("[foo:bar:+:stubs:8080]");
	}

	@Test
	void shouldCreateDependenciesWithVersion() {
		this.builder.withStubs("foo:1.2.3:bar");
		StubRunnerOptions options = this.builder.build();
		assertThat(options.getDependencies().toString()).isEqualTo("[foo:1.2.3:bar:stubs]");
	}

	@Test
	void shouldCreateDependenciesWithClassifier() {
		this.builder.withStubs("foo:bar").withStubsClassifier("xxx");
		StubRunnerOptions options = this.builder.build();
		assertThat(options.getDependencies().toString()).isEqualTo("[foo:bar:+:xxx]");
	}

	@Test
	void shouldCreateDependenciesWithEmptyPort() {
		this.builder.withStubs("groupId:artifactId:version:classifier:");
		StubRunnerOptions options = this.builder.build();
		assertThat(options.getDependencies().toString()).isEqualTo("[groupId:artifactId:version:classifier]");
	}

	@Test
	void shouldCreateDependenciesWithVersionRange() {
		this.builder.withStubs("groupId:artifactId:[,0.0.1]:classifier", "groupId2:artifactId2:[,0.0.2]:classifier2");
		StubRunnerOptions options = this.builder.build();
		assertThat(options.getDependencies().toString())
			.isEqualTo("[groupId:artifactId:[,0.0.1]:classifier, groupId2:artifactId2:[,0.0.2]:classifier2]");
	}

	@Test
	void shouldCreateDependenciesWithVersionRangeWhenSingleOneWasPassed() {
		this.builder.withStubs("groupId:artifactId:[,0.0.1]:classifier");
		StubRunnerOptions options = this.builder.build();
		assertThat(options.getDependencies().toString()).isEqualTo("[groupId:artifactId:[,0.0.1]:classifier]");
	}

	@Test
	void shouldCreateDependenciesWithVersionRangeWhenManyWerePassedInASingleLine() {
		this.builder.withStubs("groupId:artifactId:[,0.0.1]:classifier,groupId2:artifactId2:[,0.0.2]:classifier2");
		StubRunnerOptions options = this.builder.build();
		assertThat(options.getDependencies().toString())
			.isEqualTo("[groupId:artifactId:[,0.0.1]:classifier, groupId2:artifactId2:[,0.0.2]:classifier2]");
	}

	@Test
	void shouldSetAllDependenciesFromOptions() {
		StubResource root = ResourceResolver.classpathResource("root");
		StubRunnerOptionsBuilder b = this.builder.withOptions(new StubRunnerOptions(1, 2, root, StubsMode.LOCAL,
				"classifier", List.of(new StubConfiguration("a:b:c")), Map.of(new StubConfiguration("a:b:c"), 3), "foo",
				"bar", new StubRunnerOptions.StubRunnerProxyOptions("host", 4), true, "consumer", "folder", false, true,
				false, Map.of("foo", "bar"), Foo.class, "server"));
		b.withStubs("foo:bar:baz");
		StubRunnerOptions options = b.build();
		assertThat(options.getMinPortValue()).isEqualTo(1);
		assertThat(options.getMaxPortValue()).isEqualTo(2);
		assertThat(options.getStubRepositoryRoot()).isEqualTo(root);
		assertThat(options.getStubsMode()).isEqualTo(StubsMode.LOCAL);
		assertThat(options.getStubsClassifier()).isEqualTo("classifier");
		assertThat(options.getDependencies()).containsExactly(new StubConfiguration("a:b:c"),
				new StubConfiguration("foo:bar:baz:classifier"));
		assertThat(options.getStubIdsToPortMapping()).containsEntry(new StubConfiguration("a:b:c"), 3);
		assertThat(options.getUsername()).isEqualTo("foo");
		assertThat(options.getPassword()).isEqualTo("bar");
		assertThat(options.getProxyOptions().getProxyHost()).isEqualTo("host");
		assertThat(options.getProxyOptions().getProxyPort()).isEqualTo(4);
		assertThat(options.isStubsPerConsumer()).isTrue();
		assertThat(options.getConsumerName()).isEqualTo("consumer");
		assertThat(options.getMappingsOutputFolder()).isEqualTo("folder");
		assertThat(options.isDeleteStubsAfterTest()).isFalse();
		assertThat(options.isGenerateStubs()).isTrue();
		assertThat(options.isFailOnNoStubs()).isFalse();
		assertThat(options.getProperties()).containsEntry("foo", "bar");
		assertThat(options.getHttpServerStubConfigurer()).isEqualTo(Foo.class);
		assertThat(options.getServerId()).isEqualTo("server");
	}

	@Test
	void shouldNotPrintUsernameAndPassword() {
		StubRunnerOptionsBuilder b = this.builder
			.withOptions(new StubRunnerOptions(1, 2, ResourceResolver.classpathResource("root"), StubsMode.CLASSPATH,
					"classifier", List.of(new StubConfiguration("a:b:c")), Map.of(new StubConfiguration("a:b:c"), 3),
					"username123", "password123", new StubRunnerOptions.StubRunnerProxyOptions("host", 4), true,
					"consumer", "folder", false, true, true, Map.of(), Foo.class, "server"));
		b.withStubs("foo:bar:baz");
		String options = b.build().toString();
		assertThat(options).doesNotContain("username123").doesNotContain("password123").contains("****");
	}

	@Test
	void shouldSetAllPropsFromSystemProps() {
		System.setProperty("spring.cloud.contract.stubrunner.port.range.min", "1");
		System.setProperty("spring.cloud.contract.stubrunner.port.range.max", "2");
		System.setProperty("spring.cloud.contract.stubrunner.repository.root", "root");
		System.setProperty("spring.cloud.contract.stubrunner.stubs-mode", "LOCAL");
		System.setProperty("spring.cloud.contract.stubrunner.classifier", "classifier");
		System.setProperty("spring.cloud.contract.stubrunner.ids", "a:b:c,foo:bar:baz:classifier");
		System.setProperty("spring.cloud.contract.stubrunner.username", "foo");
		System.setProperty("spring.cloud.contract.stubrunner.password", "bar");
		System.setProperty("spring.cloud.contract.stubrunner.stubs-per-consumer", "true");
		System.setProperty("spring.cloud.contract.stubrunner.consumer-name", "consumer");
		System.setProperty("spring.cloud.contract.stubrunner.proxy.host", "host");
		System.setProperty("spring.cloud.contract.stubrunner.proxy.port", "4");
		System.setProperty("spring.cloud.contract.stubrunner.mappings-output-folder", "folder");
		System.setProperty("spring.cloud.contract.stubrunner.properties.foo-bar", "bar");
		System.setProperty("spring.cloud.contract.stubrunner.properties.foo-baz", "baz");
		System.setProperty("spring.cloud.contract.stubrunner.properties.bar.bar", "foo");
		System.setProperty("spring.cloud.contract.stubrunner.delete-stubs-after-test", "false");
		System.setProperty("spring.cloud.contract.stubrunner.generate-stubs", "true");
		System.setProperty("spring.cloud.contract.stubrunner.fail-on-no-stubs", "false");
		System.setProperty("spring.cloud.contract.stubrunner.http-server-stub-configurer",
				"sh.stubborn.contract.stubrunner.Foo");
		StubRunnerOptions options = StubRunnerOptions.fromSystemProps();
		assertThat(options.getMinPortValue()).isEqualTo(1);
		assertThat(options.getMaxPortValue()).isEqualTo(2);
		assertThat(options.getStubRepositoryRoot()).isEqualTo(ResourceResolver.classpathResource("root"));
		assertThat(options.getStubsMode()).isEqualTo(StubsMode.LOCAL);
		assertThat(options.getStubsClassifier()).isEqualTo("classifier");
		assertThat(options.getDependencies()).containsExactly(new StubConfiguration("a:b:c"),
				new StubConfiguration("foo:bar:baz:classifier"));
		assertThat(options.getUsername()).isEqualTo("foo");
		assertThat(options.getPassword()).isEqualTo("bar");
		assertThat(options.getProxyOptions().getProxyHost()).isEqualTo("host");
		assertThat(options.getProxyOptions().getProxyPort()).isEqualTo(4);
		assertThat(options.isStubsPerConsumer()).isTrue();
		assertThat(options.isDeleteStubsAfterTest()).isFalse();
		assertThat(options.isGenerateStubs()).isTrue();
		assertThat(options.isFailOnNoStubs()).isFalse();
		assertThat(options.getConsumerName()).isEqualTo("consumer");
		assertThat(options.getMappingsOutputFolder()).isEqualTo("folder");
		assertThat(options.getProperties()).containsEntry("foo-bar", "bar")
			.containsEntry("foo-baz", "baz")
			.containsEntry("bar.bar", "foo");
		assertThat(options.getHttpServerStubConfigurer()).isEqualTo(Foo.class);
	}

	private static void clearStubRunnerSystemProps() {
		for (String key : List.of("spring.cloud.contract.stubrunner.port.range.min",
				"spring.cloud.contract.stubrunner.port.range.max", "spring.cloud.contract.stubrunner.repository.root",
				"spring.cloud.contract.stubrunner.stubs-mode", "spring.cloud.contract.stubrunner.classifier",
				"spring.cloud.contract.stubrunner.ids", "spring.cloud.contract.stubrunner.username",
				"spring.cloud.contract.stubrunner.password", "spring.cloud.contract.stubrunner.stubs-per-consumer",
				"spring.cloud.contract.stubrunner.consumer-name", "spring.cloud.contract.stubrunner.proxy.host",
				"spring.cloud.contract.stubrunner.proxy.port",
				"spring.cloud.contract.stubrunner.mappings-output-folder",
				"spring.cloud.contract.stubrunner.properties.foo-bar",
				"spring.cloud.contract.stubrunner.properties.foo-baz",
				"spring.cloud.contract.stubrunner.properties.bar.bar",
				"spring.cloud.contract.stubrunner.delete-stubs-after-test",
				"spring.cloud.contract.stubrunner.generate-stubs", "spring.cloud.contract.stubrunner.fail-on-no-stubs",
				"spring.cloud.contract.stubrunner.http-server-stub-configurer")) {
			System.clearProperty(key);
		}
	}

}
