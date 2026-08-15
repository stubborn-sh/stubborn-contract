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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("NullAway")
class ResourceResolvingStubDownloaderMutationTests {

	private ResourceResolvingStubDownloader downloader(StubRunnerOptions options,
			BiFunction<StubRunnerOptions, StubConfiguration, RepoRoots> roots) {
		Function<StubConfiguration, Pattern> gav = (c) -> Pattern.compile("^(.*)$");
		return new ResourceResolvingStubDownloader(options, roots, gav);
	}

	@Test
	void relativePathPickerReturnsGroupTwoAndThreeForGavPattern() throws IOException {
		ResourceResolvingStubDownloader downloader = downloader(new StubRunnerOptionsBuilder().build(),
				(o, c) -> new RepoRoots());
		StubResource resource = uriResource("/repo/com/example/artifact/1.0.0/stub.json");
		Pattern pattern = Pattern.compile("^(.*)(example)(.*)$");

		assertThat(downloader.relativePathPicker(resource, pattern)).isEqualTo("example/artifact/1.0.0/stub.json");
	}

	@Test
	void relativePathPickerReturnsGroupOneForTwoGroupPattern() throws IOException {
		ResourceResolvingStubDownloader downloader = downloader(new StubRunnerOptionsBuilder().build(),
				(o, c) -> new RepoRoots());
		StubResource resource = uriResource("/repo/artifact/stub.json");
		Pattern pattern = Pattern.compile("^(/repo/)(artifact.*)$");

		assertThat(downloader.relativePathPicker(resource, pattern)).isEqualTo("/repo/");
	}

	@Test
	void relativePathPickerReturnsNullWhenNoMatch() throws IOException {
		ResourceResolvingStubDownloader downloader = downloader(new StubRunnerOptionsBuilder().build(),
				(o, c) -> new RepoRoots());
		StubResource resource = uriResource("/repo/artifact/stub.json");
		Pattern pattern = Pattern.compile("^(will-not-match)$");

		assertThat(downloader.relativePathPicker(resource, pattern)).isNull();
	}

	@Test
	void isDirectoryTrueForDirectoryFalseForFileAndErrors(@TempDir Path temp) throws IOException {
		ResourceResolvingStubDownloader downloader = downloader(new StubRunnerOptionsBuilder().build(),
				(o, c) -> new RepoRoots());
		Path dir = Files.createDirectories(temp.resolve("adir"));
		Path file = Files.writeString(temp.resolve("afile.json"), "{}");

		assertThat(downloader.isDirectory(fileResource(dir.toFile()))).isTrue();
		assertThat(downloader.isDirectory(fileResource(file.toFile()))).isFalse();
		assertThat(downloader.isDirectory(throwingResource())).isFalse();
	}

	@Test
	void downloadFailsWhenNoStubsFoundAndFailOnNoStubs() {
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withFailOnNoStubs(true).build();
		ResourceResolvingStubDownloader downloader = downloader(options,
				(o, c) -> RepoRoots.asList(new RepoRoot("/definitely/not/here/xyz/*.json")));

		assertThatThrownBy(
				() -> downloader.downloadAndUnpackStubJar(new StubConfiguration("com.example:artifact:1.0.0")))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("No stubs were found");
	}

	@Test
	void downloadCopiesMatchingStubsToTemporaryFolder(@TempDir Path temp) throws IOException {
		Path stub = temp.resolve("com.example").resolve("artifact").resolve("mapping.json");
		Files.createDirectories(stub.getParent());
		Files.writeString(stub, "{\"stub\":true}");
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withFailOnNoStubs(false).build();
		ResourceResolvingStubDownloader downloader = new ResourceResolvingStubDownloader(options,
				(o, c) -> RepoRoots.asList(new RepoRoot(temp + "/**/*.json")),
				(c) -> Pattern.compile("^(.*)(com.example)(.*)$"));

		java.util.Map.Entry<StubConfiguration, File> entry = downloader
			.downloadAndUnpackStubJar(new StubConfiguration("com.example:artifact:1.0.0"));

		assertThat(entry).isNotNull();
		assertThat(new File(entry.getValue(), "com.example/artifact/mapping.json")).exists();
	}

	@Test
	void downloadReturnsNullWhenResourcesFoundButNoneMatchPattern(@TempDir Path temp) throws IOException {
		Files.writeString(temp.resolve("stub.json"), "{}");
		StubRunnerOptions options = new StubRunnerOptionsBuilder().withFailOnNoStubs(false).build();
		// a pattern that never matches the resource path -> relative path is null for
		// every
		// resource -> nothing copied -> null result
		ResourceResolvingStubDownloader downloader = new ResourceResolvingStubDownloader(options,
				(o, c) -> RepoRoots.asList(new RepoRoot(temp + "/*.json")),
				(c) -> Pattern.compile("^(will-never-match)$"));

		assertThat(downloader.downloadAndUnpackStubJar(new StubConfiguration("com.example:artifact:1.0.0"))).isNull();
	}

	private static StubResource uriResource(String path) {
		return new StubResource() {
			@Override
			public URI getURI() {
				return URI.create("file://" + path);
			}

			@Override
			public URL getURL() throws IOException {
				return getURI().toURL();
			}

			@Override
			public File getFile() {
				return new File(path);
			}

			@Override
			public InputStream getInputStream() {
				return null;
			}

			@Override
			public String getFilename() {
				return path.substring(path.lastIndexOf('/') + 1);
			}

			@Override
			public String getDescription() {
				return path;
			}
		};
	}

	private static StubResource fileResource(File file) {
		return new StubResource() {
			@Override
			public URI getURI() {
				return file.toURI();
			}

			@Override
			public URL getURL() throws IOException {
				return file.toURI().toURL();
			}

			@Override
			public File getFile() {
				return file;
			}

			@Override
			public InputStream getInputStream() throws IOException {
				return Files.newInputStream(file.toPath());
			}

			@Override
			public String getFilename() {
				return file.getName();
			}

			@Override
			public String getDescription() {
				return file.toString();
			}
		};
	}

	private static StubResource throwingResource() {
		return new StubResource() {
			@Override
			public URI getURI() throws IOException {
				throw new IOException("no uri");
			}

			@Override
			public URL getURL() throws IOException {
				throw new IOException("no url");
			}

			@Override
			public File getFile() throws IOException {
				throw new IOException("no file");
			}

			@Override
			public InputStream getInputStream() throws IOException {
				throw new IOException("no stream");
			}

			@Override
			public String getFilename() {
				return "throwing";
			}

			@Override
			public String getDescription() {
				return "throwing";
			}
		};
	}

}
