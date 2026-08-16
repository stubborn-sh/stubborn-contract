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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultArtifactVersionWrapperMutationTests {

	@Test
	void shouldDetectSnapshot() {
		DefaultArtifactVersionWrapper snapshot = new DefaultArtifactVersionWrapper(new File("1.0.0.BUILD-SNAPSHOT"));
		assertThat(snapshot.isSnapshot()).isTrue();
		assertThat(snapshot.isNotSnapshot()).isFalse();
	}

	@Test
	void shouldDetectNonSnapshot() {
		DefaultArtifactVersionWrapper release = new DefaultArtifactVersionWrapper(new File("1.0.0.RELEASE"));
		assertThat(release.isSnapshot()).isFalse();
		assertThat(release.isNotSnapshot()).isTrue();
	}

	@Test
	void shouldCompareByMaturity() {
		DefaultArtifactVersionWrapper newer = new DefaultArtifactVersionWrapper(new File("2.0.0.RELEASE"));
		DefaultArtifactVersionWrapper older = new DefaultArtifactVersionWrapper(new File("1.0.0.RELEASE"));
		assertThat(newer.compareTo(older)).isEqualTo(1);
		assertThat(older.compareTo(newer)).isEqualTo(-1);
	}

	@Test
	void shouldProduceDistinctHashCodesForDistinctFiles() {
		assertThat(new DefaultArtifactVersionWrapper(new File("1.0.0.RELEASE")).hashCode())
			.isNotEqualTo(new DefaultArtifactVersionWrapper(new File("2.0.0.RELEASE")).hashCode());
	}

	@Test
	void shouldImplementEquals() {
		DefaultArtifactVersionWrapper one = new DefaultArtifactVersionWrapper(new File("1.0.0.RELEASE"));
		DefaultArtifactVersionWrapper same = new DefaultArtifactVersionWrapper(new File("1.0.0.RELEASE"));
		DefaultArtifactVersionWrapper different = new DefaultArtifactVersionWrapper(new File("2.0.0.RELEASE"));
		assertThat(one.equals(one)).isTrue();
		assertThat(one).isEqualTo(same);
		assertThat(one).hasSameHashCodeAs(same);
		assertThat(one).isNotEqualTo(different);
		assertThat(one).isNotEqualTo(null);
		assertThat(one).isNotEqualTo("1.0.0.RELEASE");
	}

}
