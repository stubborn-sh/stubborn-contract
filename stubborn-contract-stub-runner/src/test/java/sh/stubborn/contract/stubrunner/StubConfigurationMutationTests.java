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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NullAway")
class StubConfigurationMutationTests {

	@Test
	void shouldExposeGettersWithConcreteValues() {
		StubConfiguration config = new StubConfiguration("group:artifact:1.2.3:custom");
		assertThat(config.getGroupId()).isEqualTo("group");
		assertThat(config.getArtifactId()).isEqualTo("artifact");
		assertThat(config.getVersion()).isEqualTo("1.2.3");
		assertThat(config.getClassifier()).isEqualTo("custom");
	}

	@Test
	void shouldBeEqualForSameGroupAndArtifact() {
		StubConfiguration one = new StubConfiguration("group:artifact:1.0.0:stubs");
		// version and classifier are intentionally ignored by equals
		StubConfiguration two = new StubConfiguration("group:artifact:2.0.0:other");
		assertThat(one.equals(one)).isTrue();
		assertThat(one).isEqualTo(two);
		assertThat(one).hasSameHashCodeAs(two);
	}

	@Test
	void shouldNotBeEqualForNullAndOtherType() {
		StubConfiguration config = new StubConfiguration("group:artifact:1.0.0");
		assertThat(config).isNotEqualTo(null);
		assertThat(config).isNotEqualTo("group:artifact:1.0.0");
	}

	@Test
	void shouldNotBeEqualForDifferentArtifact() {
		StubConfiguration one = new StubConfiguration("group", "artifact", "1.0.0");
		StubConfiguration two = new StubConfiguration("group", "different", "1.0.0");
		assertThat(one).isNotEqualTo(two);
	}

	@Test
	void shouldNotBeEqualForDifferentGroup() {
		StubConfiguration one = new StubConfiguration("group", "artifact", "1.0.0");
		StubConfiguration two = new StubConfiguration("different", "artifact", "1.0.0");
		assertThat(one).isNotEqualTo(two);
	}

	@Test
	void shouldHandleNullArtifactIdInEquals() {
		StubConfiguration nullArtifact = new StubConfiguration("group", null, "1.0.0");
		StubConfiguration otherNullArtifact = new StubConfiguration("group", null, "1.0.0");
		StubConfiguration nonNullArtifact = new StubConfiguration("group", "artifact", "1.0.0");
		assertThat(nullArtifact).isEqualTo(otherNullArtifact);
		assertThat(nullArtifact).isNotEqualTo(nonNullArtifact);
	}

	@Test
	void shouldHandleNullGroupIdInEquals() {
		StubConfiguration nullGroup = new StubConfiguration(null, "artifact", "1.0.0");
		StubConfiguration otherNullGroup = new StubConfiguration(null, "artifact", "1.0.0");
		StubConfiguration nonNullGroup = new StubConfiguration("group", "artifact", "1.0.0");
		assertThat(nullGroup).isEqualTo(otherNullGroup);
		assertThat(nullGroup).isNotEqualTo(nonNullGroup);
	}

	@ParameterizedTest
	@CsvSource({ "artifact, true", "other, false" })
	void shouldMatchGroupIdAndArtifactForArtifactOnly(String ivy, boolean expected) {
		StubConfiguration config = new StubConfiguration("group:artifact:1.0.0");
		assertThat(config.groupIdAndArtifactMatches(ivy)).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({ "group:artifact, true", "other:artifact, false", "group:other, false" })
	void shouldMatchGroupIdAndArtifactForGroupAndArtifact(String ivy, boolean expected) {
		StubConfiguration config = new StubConfiguration("group:artifact:1.0.0");
		assertThat(config.groupIdAndArtifactMatches(ivy)).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({ "artifact, true", "other, false", "group:artifact, true", "group:other, false",
			"other:artifact, false", "group:artifact:1.0.0, true", "group:artifact:9.9.9, false",
			"group:artifact:+, true", "group:artifact:1.0.0:stubs, true", "group:artifact:1.0.0:custom, false",
			"group:artifact:1.0.0:+, false" })
	void shouldMatchIvyNotation(String ivy, boolean expected) {
		StubConfiguration config = new StubConfiguration("group:artifact:1.0.0:stubs");
		assertThat(config.matchesIvyNotation(ivy)).isEqualTo(expected);
	}

	@Test
	void shouldMatchIvyNotationWithDefaultClassifierAlias() {
		StubConfiguration config = new StubConfiguration("group:artifact:1.0.0:custom");
		// stubs is the DEFAULT_CLASSIFIER alias, always accepted
		assertThat(config.matchesIvyNotation("group:artifact:1.0.0:stubs")).isTrue();
		assertThat(config.matchesIvyNotation("group:artifact:1.0.0:custom")).isTrue();
		assertThat(config.matchesIvyNotation("group:artifact:1.0.0:nope")).isFalse();
	}

	@Test
	void shouldReturnEmptyNotationWhenNotDefined() {
		assertThat(new StubConfiguration("", "", "1.0.0").toColonSeparatedDependencyNotation()).isEmpty();
		assertThat(new StubConfiguration("group", "", "1.0.0").toColonSeparatedDependencyNotation()).isEmpty();
		assertThat(new StubConfiguration(null, "artifact", "1.0.0").toColonSeparatedDependencyNotation()).isEmpty();
		assertThat(new StubConfiguration("group", null, "1.0.0").toColonSeparatedDependencyNotation()).isEmpty();
	}

	@Test
	void shouldReturnFullNotationWhenDefined() {
		assertThat(new StubConfiguration("group:artifact:1.0.0:stubs").toColonSeparatedDependencyNotation())
			.isEqualTo("group:artifact:1.0.0:stubs");
	}

	@Test
	void shouldProduceEqualHashCodesForNullFields() {
		assertThat(new StubConfiguration(null, "artifact", "1.0.0"))
			.hasSameHashCodeAs(new StubConfiguration(null, "artifact", "2.0.0"));
		assertThat(new StubConfiguration("group", null, "1.0.0"))
			.hasSameHashCodeAs(new StubConfiguration("group", null, "2.0.0"));
	}

	@Test
	void shouldLeaveAllPartsEmptyForSingleTokenPath() {
		StubConfiguration config = new StubConfiguration("onlyartifact");
		assertThat(config.getGroupId()).isEmpty();
		assertThat(config.getArtifactId()).isEmpty();
		assertThat(config.getVersion()).isEmpty();
		assertThat(config.getClassifier()).isEmpty();
		assertThat(config.toColonSeparatedDependencyNotation()).isEmpty();
	}

	@Test
	void shouldDefaultVersionAndClassifierForTwoTokenPath() {
		StubConfiguration config = new StubConfiguration("group:artifact");
		assertThat(config.getVersion()).isEqualTo("+");
		assertThat(config.getClassifier()).isEqualTo("stubs");
	}

	@Test
	void shouldUseProvidedDefaultClassifierWhenClassifierAbsent() {
		StubConfiguration config = new StubConfiguration("group:artifact:1.0.0", "custom-default");
		assertThat(config.getClassifier()).isEqualTo("custom-default");
	}

	@Test
	void shouldIgnoreExtraTokensBeyondClassifier() {
		StubConfiguration config = new StubConfiguration("group:artifact:1.0.0:stubs:extra");
		assertThat(config.getGroupId()).isEqualTo("group");
		assertThat(config.getArtifactId()).isEqualTo("artifact");
		assertThat(config.getVersion()).isEqualTo("1.0.0");
		assertThat(config.getClassifier()).isEqualTo("stubs");
	}

	@ParameterizedTest
	@CsvSource({ "1.0.0.RELEASE, false", "1.0.0.BUILD-SNAPSHOT, true", "+, true", "1.0.0-SNAPSHOT, true" })
	void shouldDetectChangingVersion(String version, boolean changing) {
		assertThat(new StubConfiguration("group", "artifact", version).isVersionChanging()).isEqualTo(changing);
	}

}
