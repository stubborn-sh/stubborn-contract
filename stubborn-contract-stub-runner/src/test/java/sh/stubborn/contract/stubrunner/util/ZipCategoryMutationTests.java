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

package sh.stubborn.contract.stubrunner.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZipCategoryMutationTests {

	private File buildZip(Path dir) throws Exception {
		File zip = dir.resolve("stubs.zip").toFile();
		try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
			out.putNextEntry(new ZipEntry("nested/"));
			out.closeEntry();
			writeEntry(out, "nested/file.txt", "hello");
			writeEntry(out, "root.txt", "world");
		}
		return zip;
	}

	private void writeEntry(ZipOutputStream out, String name, String content) throws Exception {
		out.putNextEntry(new ZipEntry(name));
		out.write(content.getBytes());
		out.closeEntry();
	}

	@Test
	void unzipsFilesAndDirectoriesToDestination(@TempDir Path dir) throws Exception {
		File zip = buildZip(dir);
		File destination = Files.createDirectories(dir.resolve("out")).toFile();

		Collection<File> unzipped = ZipCategory.unzipTo(zip, destination);

		assertThat(new File(destination, "nested/file.txt")).exists();
		assertThat(Files.readString(new File(destination, "nested/file.txt").toPath())).isEqualTo("hello");
		assertThat(new File(destination, "root.txt")).exists();
		assertThat(new File(destination, "nested")).isDirectory();
		assertThat(unzipped).isNotEmpty();
	}

	@Test
	@SuppressWarnings("NullAway")
	void fallsBackToParentDirectoryWhenDestinationIsNull(@TempDir Path dir) throws Exception {
		Path sub = Files.createDirectories(dir.resolve("archive"));
		File zip = buildZip(sub);

		ZipCategory.unzipTo(zip, null);

		// destination defaults to the zip file's parent directory
		assertThat(new File(sub.toFile(), "root.txt")).exists();
		assertThat(new File(sub.toFile(), "nested/file.txt")).exists();
	}

	@Test
	void createsIntermediateDirectoriesForNestedEntries(@TempDir Path dir) throws Exception {
		File zip = dir.resolve("deep.zip").toFile();
		try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
			// no explicit directory entries, so the parent folders must be created on the
			// fly
			writeEntry(out, "a/b/c/deep.txt", "deep");
		}
		File destination = Files.createDirectories(dir.resolve("out")).toFile();

		ZipCategory.unzipTo(zip, destination);

		assertThat(new File(destination, "a/b/c/deep.txt")).exists();
		assertThat(Files.readString(new File(destination, "a/b/c/deep.txt").toPath())).isEqualTo("deep");
	}

	@Test
	void rejectsEntriesThatEscapeTheTargetDirectory(@TempDir Path dir) throws Exception {
		File zip = dir.resolve("evil.zip").toFile();
		try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
			writeEntry(out, "../escaped.txt", "boom");
		}
		File destination = Files.createDirectories(dir.resolve("out")).toFile();

		assertThatThrownBy(() -> ZipCategory.unzipTo(zip, destination)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Cannot unzip archive");
	}

	@Test
	void rejectsDestinationThatIsNotADirectory(@TempDir Path dir) throws Exception {
		File zip = buildZip(dir);
		File notADir = Files.writeString(dir.resolve("plain.txt"), "x").toFile();

		assertThatThrownBy(() -> ZipCategory.unzipTo(zip, notADir)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("has to be a directory");
	}

	@Test
	@SuppressWarnings("NullAway")
	void throwsWhenArchiveCannotBeRead(@TempDir Path dir) throws Exception {
		File missing = dir.resolve("does-not-exist.zip").toFile();
		File destination = Files.createDirectories(dir.resolve("out")).toFile();

		assertThatThrownBy(() -> ZipCategory.unzipTo(missing, destination)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Cannot unzip archive");
	}

}
