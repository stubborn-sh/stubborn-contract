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

class DirectoryCopyingVisitorMutationTests {

	@Test
	void shouldCopyPlainFiles(@TempDir Path temp) throws IOException {
		Path from = Files.createDirectories(temp.resolve("from"));
		Path to = Files.createDirectories(temp.resolve("to"));
		Files.writeString(from.resolve("file.txt"), "hello");

		Files.walkFileTree(from, new DirectoryCopyingVisitor(from, to));

		assertThat(to.resolve("file.txt")).exists();
		assertThat(Files.readString(to.resolve("file.txt"))).isEqualTo("hello");
	}

	@Test
	void shouldRecreateContractsFolderReplacingExistingContent(@TempDir Path temp) throws IOException {
		Path from = Files.createDirectories(temp.resolve("from"));
		Path to = Files.createDirectories(temp.resolve("to"));
		Files.writeString(Files.createDirectories(from.resolve("contracts")).resolve("new.json"), "new");
		// pre-existing "contracts" folder in the target with stale content
		Files.writeString(Files.createDirectories(to.resolve("contracts")).resolve("old.json"), "old");

		Files.walkFileTree(from, new DirectoryCopyingVisitor(from, to));

		assertThat(to.resolve("contracts/old.json")).doesNotExist();
		assertThat(to.resolve("contracts/new.json")).exists();
		assertThat(Files.readString(to.resolve("contracts/new.json"))).isEqualTo("new");
	}

	@Test
	void shouldCreateNonExistingNonSpecialSubdirectory(@TempDir Path temp) throws IOException {
		Path from = Files.createDirectories(temp.resolve("from"));
		Path to = Files.createDirectories(temp.resolve("to"));
		// a nested folder that is neither "contracts" nor "mappings" and does not yet
		// exist
		// in the target -> the visitor must create it
		Files.writeString(Files.createDirectories(from.resolve("data")).resolve("payload.json"), "{}");

		Files.walkFileTree(from, new DirectoryCopyingVisitor(from, to));

		assertThat(to.resolve("data")).isDirectory();
		assertThat(to.resolve("data/payload.json")).exists();
	}

	@Test
	void shouldLeaveExistingNonSpecialFolderUntouched(@TempDir Path temp) throws IOException {
		Path from = Files.createDirectories(temp.resolve("from"));
		Path to = Files.createDirectories(temp.resolve("to"));
		Files.writeString(Files.createDirectories(from.resolve("data")).resolve("new.json"), "new");
		// pre-existing non-special folder in the target keeps its content (not recreated)
		Files.writeString(Files.createDirectories(to.resolve("data")).resolve("existing.json"), "existing");

		Files.walkFileTree(from, new DirectoryCopyingVisitor(from, to));

		assertThat(to.resolve("data/existing.json")).exists();
		assertThat(to.resolve("data/new.json")).exists();
	}

	@Test
	void shouldSkipGitFolder(@TempDir Path temp) throws IOException {
		Path from = Files.createDirectories(temp.resolve("from"));
		Path to = Files.createDirectories(temp.resolve("to"));
		Files.writeString(Files.createDirectories(from.resolve(".git")).resolve("HEAD"), "ref");
		Files.writeString(from.resolve("keep.txt"), "keep");

		Files.walkFileTree(from, new DirectoryCopyingVisitor(from, to));

		assertThat(to.resolve("keep.txt")).exists();
		assertThat(to.resolve(".git")).doesNotExist();
	}

}
