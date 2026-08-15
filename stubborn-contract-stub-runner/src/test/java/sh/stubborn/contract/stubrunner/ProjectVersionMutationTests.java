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
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("NullAway")
class ProjectVersionMutationTests {

	@ParameterizedTest
	@ValueSource(strings = { "1.2.3.RELEASE", "1.2.3-RELEASE", "1.2.3.BUILD-SNAPSHOT", "1.2.3-BUILD-SNAPSHOT",
			"1.2.3.M1", "1.2.3.RC1", "1.2.3.SR1", "Hoxton.RELEASE", "Hoxton-RELEASE", "Hoxton.BUILD-SNAPSHOT",
			"Dysprosium-BUILD-SNAPSHOT" })
	void shouldParseValidVersionsWithoutError(String version) {
		ProjectVersion projectVersion = new ProjectVersion(version);
		assertThat(projectVersion.isMoreMature(new ProjectVersion(version))).isZero();
		assertThat(projectVersion.isSameWithoutSuffix(new ProjectVersion(version))).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "1.2.3", "1.RELEASE", "1.2.RELEASE", "1.2.3.", "1.EXAMPLE", "Hoxton", "1" })
	void shouldRejectMalformedVersions(String version) {
		assertThatThrownBy(() -> new ProjectVersion(version).isMoreMature(new ProjectVersion("1.0.0.RELEASE")))
			.isInstanceOf(IllegalStateException.class);
	}

	@ParameterizedTest
	@CsvSource({ "1.0.0.BUILD-SNAPSHOT, true", "1.0.0.RELEASE, false", "1.0.0.SR1, false", "1.0.0.M1, false" })
	void shouldDetectSnapshot(String version, boolean expected) {
		assertThat(new ProjectVersion(version).isSnapshot()).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({ "1.0.0.RC1, true", "1.0.0.RELEASE, false", "1.0.0.BUILD-SNAPSHOT, false" })
	void shouldDetectRc(String version, boolean expected) {
		assertThat(new ProjectVersion(version).isRc()).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({ "1.0.0.M1, true", "1.0.0.RELEASE, false", "1.0.0.RC1, false" })
	void shouldDetectMilestone(String version, boolean expected) {
		assertThat(new ProjectVersion(version).isMilestone()).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({ "1.0.0.RELEASE, true", "1.0.0.BUILD-SNAPSHOT, false", "1.0.0.M1, false" })
	void shouldDetectRelease(String version, boolean expected) {
		assertThat(new ProjectVersion(version).isRelease()).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({ "1.0.0.SR1, true", "1.0.0.RELEASE, false", "1.0.0.BUILD-SNAPSHOT, false" })
	void shouldDetectServiceRelease(String version, boolean expected) {
		assertThat(new ProjectVersion(version).isServiceRelease()).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({ "1.0.0.RELEASE, true", "1.0.0.SR1, true", "1.0.0.BUILD-SNAPSHOT, false", "1.0.0.M1, false" })
	void shouldDetectReleaseOrServiceRelease(String version, boolean expected) {
		assertThat(new ProjectVersion(version).isReleaseOrServiceRelease()).isEqualTo(expected);
	}

	@Test
	void shouldConsiderHigherMajorMoreMature() {
		assertThat(new ProjectVersion("2.0.0.RELEASE").isMoreMature(new ProjectVersion("1.0.0.RELEASE"))).isEqualTo(1);
		assertThat(new ProjectVersion("1.0.0.RELEASE").isMoreMature(new ProjectVersion("2.0.0.RELEASE"))).isEqualTo(-1);
	}

	@Test
	void shouldConsiderHigherMinorMoreMature() {
		assertThat(new ProjectVersion("1.2.0.RELEASE").isMoreMature(new ProjectVersion("1.1.0.RELEASE"))).isEqualTo(1);
		assertThat(new ProjectVersion("1.1.0.RELEASE").isMoreMature(new ProjectVersion("1.2.0.RELEASE"))).isEqualTo(-1);
	}

	@Test
	void shouldConsiderHigherPatchMoreMature() {
		assertThat(new ProjectVersion("1.0.2.RELEASE").isMoreMature(new ProjectVersion("1.0.1.RELEASE"))).isEqualTo(1);
		assertThat(new ProjectVersion("1.0.1.RELEASE").isMoreMature(new ProjectVersion("1.0.2.RELEASE"))).isEqualTo(-1);
	}

	@Test
	void shouldConsiderHigherReleaseTypeMoreMatureWhenNotBothGa() {
		// milestone(1) more mature than snapshot(0), equal numeric parts -> returns 1
		assertThat(new ProjectVersion("1.0.0.M1").isMoreMature(new ProjectVersion("1.0.0.BUILD-SNAPSHOT")))
			.isEqualTo(1);
		// even though numeric major would say otherwise: RELEASE beats a higher SNAPSHOT
		assertThat(new ProjectVersion("1.0.0.RELEASE").isMoreMature(new ProjectVersion("2.0.0.BUILD-SNAPSHOT")))
			.isEqualTo(1);
	}

	@Test
	void shouldCompareReleaseTypesWhenBothGa() {
		// both GA: SR(4) vs RELEASE(3) -> release type comparison of 1
		assertThat(new ProjectVersion("1.0.0.SR1").isMoreMature(new ProjectVersion("1.0.0.RELEASE"))).isEqualTo(1);
		assertThat(new ProjectVersion("1.0.0.RELEASE").isMoreMature(new ProjectVersion("1.0.0.SR1"))).isEqualTo(-1);
	}

	@Test
	void shouldCompareReleaseTypesForEqualNumericVersions() {
		// milestone(1) vs rc(2) with equal numeric parts -> -1 (exact, so
		// misclassification is caught)
		assertThat(new ProjectVersion("1.0.0.M1").isMoreMature(new ProjectVersion("1.0.0.RC1"))).isEqualTo(-1);
		assertThat(new ProjectVersion("1.0.0.RC1").isMoreMature(new ProjectVersion("1.0.0.RELEASE"))).isEqualTo(-1);
		assertThat(new ProjectVersion("1.0.0.RELEASE").isMoreMature(new ProjectVersion("1.0.0.RELEASE"))).isEqualTo(0);
	}

	@Test
	void shouldCompareReleaseTrainsUsingStringComparison() {
		// non-numeric major falls back to String.compareTo in compare()
		assertThat(new ProjectVersion("Hoxton.RELEASE").isMoreMature(new ProjectVersion("Greenwich.RELEASE")))
			.isGreaterThan(0);
		assertThat(new ProjectVersion("Greenwich.RELEASE").isMoreMature(new ProjectVersion("Hoxton.RELEASE")))
			.isLessThan(0);
		assertThat(new ProjectVersion("Hoxton.RELEASE").isMoreMature(new ProjectVersion("Hoxton.RELEASE"))).isZero();
	}

	@Test
	void shouldParseHyphenSeparatedReleaseTrain() {
		assertThat(new ProjectVersion("Dysprosium-RELEASE").isMoreMature(new ProjectVersion("Dysprosium-RELEASE")))
			.isZero();
	}

	@Test
	void shouldParseHyphenSeparatedReleaseTrainBuildSnapshot() {
		assertThat(new ProjectVersion("Dysprosium-BUILD-SNAPSHOT")
			.isMoreMature(new ProjectVersion("Dysprosium-BUILD-SNAPSHOT"))).isZero();
	}

	@Test
	void shouldParseHyphenSeparatedLibraryVersion() {
		assertThat(new ProjectVersion("1.0.0-RELEASE").isMoreMature(new ProjectVersion("1.0.0-RELEASE"))).isZero();
		assertThat(new ProjectVersion("2.0.0-RELEASE").isMoreMature(new ProjectVersion("1.0.0-RELEASE"))).isEqualTo(1);
	}

	@Test
	void shouldParseHyphenSeparatedLibraryBuildSnapshot() {
		assertThat(new ProjectVersion("1.0.0-BUILD-SNAPSHOT").isMoreMature(new ProjectVersion("1.0.0-BUILD-SNAPSHOT")))
			.isZero();
	}

	@Test
	void shouldParseThreePartDotReleaseTrain() {
		assertThat(new ProjectVersion("Foo.Bar.RELEASE").isMoreMature(new ProjectVersion("Foo.Bar.RELEASE"))).isZero();
	}

	@Test
	void shouldThrowWhenReleaseTrainHasNoSuffix() {
		// single token with no suffix is invalid -> orDefault/noSuffix branch throws
		// IllegalState
		assertThatThrownBy(() -> new ProjectVersion("Hoxton").isMoreMature(new ProjectVersion("Greenwich")))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldThrowWhenNumericLibraryVersionIsMissingPatch() {
		// 1.0.RELEASE -> numeric major with a blank patch -> wrongLibraryVersion ->
		// invalid
		assertThatThrownBy(() -> new ProjectVersion("1.0.RELEASE").isMoreMature(new ProjectVersion("1.0.0.RELEASE")))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldThrowWhenVersionIsNull() {
		assertThatThrownBy(() -> new ProjectVersion(null).isMoreMature(new ProjectVersion("1.0.0.RELEASE")))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldConsiderSameVersionWithoutSuffix() {
		assertThat(new ProjectVersion("1.0.0.RELEASE").isSameWithoutSuffix(new ProjectVersion("1.0.0.BUILD-SNAPSHOT")))
			.isTrue();
		assertThat(new ProjectVersion("1.0.0.RELEASE").isSameWithoutSuffix(new ProjectVersion("1.0.1.RELEASE")))
			.isFalse();
	}

	@Test
	void shouldImplementEquals() {
		ProjectVersion version = new ProjectVersion("1.0.0.RELEASE");
		assertThat(version.equals(version)).isTrue();
		assertThat(version).isEqualTo(new ProjectVersion("1.0.0.RELEASE"));
		assertThat(version).isNotEqualTo(new ProjectVersion("1.0.1.RELEASE"));
		assertThat(version).isNotEqualTo(null);
		assertThat(version).isNotEqualTo("1.0.0.RELEASE");
	}

	@Test
	void shouldImplementHashCode() {
		assertThat(new ProjectVersion("1.0.0.RELEASE")).hasSameHashCodeAs(new ProjectVersion("1.0.0.RELEASE"));
		assertThat(new ProjectVersion("1.0.0.RELEASE").hashCode())
			.isNotEqualTo(new ProjectVersion("1.0.1.RELEASE").hashCode());
	}

	@Test
	void shouldImplementToString() {
		assertThat(new ProjectVersion("1.0.0.RELEASE")).hasToString("1.0.0.RELEASE");
	}

	@Test
	void shouldPreferHigherNumericWhenBothAreGaEvenIfReleaseTypeDiffers() {
		// both GA (SR and RELEASE) -> the numeric comparison decides, not the release
		// type
		assertThat(new ProjectVersion("1.0.0.SR1").isMoreMature(new ProjectVersion("2.0.0.RELEASE"))).isEqualTo(-1);
		assertThat(new ProjectVersion("2.0.0.RELEASE").isMoreMature(new ProjectVersion("1.0.0.SR1"))).isEqualTo(1);
	}

	@Test
	void shouldThrowForHyphenVersionWithInvalidVersionType() {
		// "Foo-Bar": hyphenated but the suffix matches no known release type
		assertThatThrownBy(() -> new ProjectVersion("Foo-Bar").isMoreMature(new ProjectVersion("1.0.0.RELEASE")))
			.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void shouldThrowForBareBuildSnapshotWithoutName() {
		// "BUILD-SNAPSHOT": single hyphen and endsWith BUILD-SNAPSHOT -> not a hyphen
		// train,
		// falls through to the dot parser which rejects a suffix-less version
		assertThatThrownBy(() -> new ProjectVersion("BUILD-SNAPSHOT").isMoreMature(new ProjectVersion("1.0.0.RELEASE")))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldImplementCompareTo() {
		assertThat(new ProjectVersion("1.0.0.RELEASE").compareTo(new ProjectVersion("1.0.0.RELEASE"))).isZero();
		assertThat(new ProjectVersion("1.0.1.RELEASE").compareTo(new ProjectVersion("1.0.0.RELEASE"))).isGreaterThan(0);
		assertThat(new ProjectVersion("1.0.0.RELEASE").compareTo(new ProjectVersion("1.0.1.RELEASE"))).isLessThan(0);
	}

}
