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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class StubConfigurationTests {

	@ParameterizedTest
	@CsvSource({
			"group:artifact:version:classifier, group:artifact:version:classifier, artifact, group, classifier, version",
			"group:artifact:version:,               group:artifact:version:,            artifact, group, '',         version",
			"group:artifact:version,                group:artifact:version:stubs,        artifact, group, stubs,      version" })
	void shouldParseAndMatchIvyNotation(String ivy, String expectedNotation, String artifactId, String groupId,
			String classifier, String version) {
		StubConfiguration config = new StubConfiguration(ivy);
		assertThat(config.artifactId).isEqualTo(artifactId);
		assertThat(config.groupId).isEqualTo(groupId);
		assertThat(config.classifier).isEqualTo(classifier);
		assertThat(config.version).isEqualTo(version);
		assertThat(config.toColonSeparatedDependencyNotation()).isEqualTo(expectedNotation);
		assertThat(config.matchesIvyNotation(ivy)).isTrue();
	}

	@ParameterizedTest
	@CsvSource({ "group:artifact:1.0.0.RELEASE:classifier, false", "group:artifact:1.0.0.BUILD-SNAPSHOT:,         true",
			"group:artifact:1.0.0.SNAPSHOT,                true",
			"group:artifact:+:,                             true" })
	void shouldResolveChangingVersion(String ivy, boolean expected) {
		StubConfiguration config = new StubConfiguration(ivy);
		assertThat(config.isVersionChanging()).isEqualTo(expected);
	}

}
