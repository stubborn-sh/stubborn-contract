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

/**
 * Covers the {@link DirectoryCopyingVisitor} branches the happy-path copy tests in
 * {@link DirectoryCopyingVisitorMutationTests} do not reach: the {@code mappings} special
 * folder, and the recursive delete of a NESTED directory when a special folder is
 * recreated (which drives the inner delete visitor's {@code postVisitDirectory}).
 */
class DirectoryCopyingVisitorTests {

	@Test
	void shouldRecreateMappingsFolderReplacingExistingContent(@TempDir Path temp) throws IOException {
		Path from = Files.createDirectories(temp.resolve("from"));
		Path to = Files.createDirectories(temp.resolve("to"));
		Files.writeString(Files.createDirectories(from.resolve("mappings")).resolve("new.json"), "new");
		// pre-existing "mappings" folder in the target with stale content
		Files.writeString(Files.createDirectories(to.resolve("mappings")).resolve("old.json"), "old");

		Files.walkFileTree(from, new DirectoryCopyingVisitor(from, to));

		assertThat(to.resolve("mappings/old.json")).doesNotExist();
		assertThat(to.resolve("mappings/new.json")).exists();
		assertThat(Files.readString(to.resolve("mappings/new.json"))).isEqualTo("new");
	}

	@Test
	void shouldRecursivelyDeleteNestedContentWhenRecreatingContractsFolder(@TempDir Path temp) throws IOException {
		Path from = Files.createDirectories(temp.resolve("from"));
		Path to = Files.createDirectories(temp.resolve("to"));
		Files.writeString(Files.createDirectories(from.resolve("contracts")).resolve("new.json"), "new");
		// pre-existing "contracts" folder with a NESTED sub-directory that also holds a
		// file; recreating the folder must delete the nested tree first
		Path staleNested = Files.createDirectories(to.resolve("contracts").resolve("nested").resolve("deep"));
		Files.writeString(staleNested.resolve("old.json"), "old");

		Files.walkFileTree(from, new DirectoryCopyingVisitor(from, to));

		assertThat(to.resolve("contracts/nested")).doesNotExist();
		assertThat(to.resolve("contracts/new.json")).exists();
	}

}
