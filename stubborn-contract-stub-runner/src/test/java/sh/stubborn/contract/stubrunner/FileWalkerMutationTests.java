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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NullAway")
class FileWalkerMutationTests {

	private Path walk(Path root, StubConfiguration stub) throws IOException {
		FileWalker walker = new FileWalker(stub);
		Files.walkFileTree(root, walker);
		return walker.foundFile;
	}

	@Test
	void shouldFindExactVersionFolder(@TempDir Path temp) throws IOException {
		Path version = Files.createDirectories(temp.resolve("com.example/artifact/1.0.0.RELEASE"));
		Files.createDirectories(temp.resolve("com.example/artifact/2.0.0.RELEASE"));

		Path found = walk(temp, new StubConfiguration("com.example", "artifact", "1.0.0.RELEASE", "stubs"));

		assertThat(found).isEqualTo(version);
	}

	@Test
	void shouldPickLatestReleaseAndIgnoreSnapshots(@TempDir Path temp) throws IOException {
		Path release = Files.createDirectories(temp.resolve("com.example/artifact/1.0.0.RELEASE"));
		Files.createDirectories(temp.resolve("com.example/artifact/2.0.0.BUILD-SNAPSHOT"));

		Path found = walk(temp, new StubConfiguration("com.example", "artifact", "release", "stubs"));

		// snapshots are excluded for a release request, so the RELEASE is the latest
		// match
		assertThat(found).isEqualTo(release);
	}

	@Test
	void shouldReturnNothingWhenNoReleaseVersionMatches(@TempDir Path temp) throws IOException {
		Files.createDirectories(temp.resolve("com.example/artifact/1.0.0.BUILD-SNAPSHOT"));

		Path found = walk(temp, new StubConfiguration("com.example", "artifact", "release", "stubs"));

		assertThat(found).isNull();
	}

	@Test
	void shouldPreferSnapshotOverReleaseForSameLatestVersion(@TempDir Path temp) throws IOException {
		Files.createDirectories(temp.resolve("com.example/artifact/2.0.0.RELEASE"));
		Path snapshot = Files.createDirectories(temp.resolve("com.example/artifact/2.0.0.BUILD-SNAPSHOT"));

		Path found = walk(temp, new StubConfiguration("com.example", "artifact", "+", "stubs"));

		// latest version request with both a RELEASE and a SNAPSHOT of the same version
		// resolves to the SNAPSHOT
		assertThat(found).isEqualTo(snapshot);
	}

}
