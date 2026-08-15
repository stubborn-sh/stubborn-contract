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
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractDownloaderMutationTests {

	private final StubDownloader stubDownloader = mock(StubDownloader.class);

	private final StubConfiguration stubConfiguration = new StubConfiguration("com.example:foo:1.0.0");

	private ContractDownloader downloader(String contractsPath) {
		return new ContractDownloader(this.stubDownloader, this.stubConfiguration, contractsPath, "com.example", "foo",
				"1.0.0");
	}

	@Test
	void shouldReturnUnpackedContractsLocation(@TempDir Path temp) {
		File unpacked = temp.resolve("unpacked").toFile();
		when(this.stubDownloader.downloadAndUnpackStubJar(this.stubConfiguration))
			.thenReturn(Map.entry(this.stubConfiguration, unpacked));
		assertThat(downloader("").unpackAndDownloadContracts()).isEqualTo(unpacked);
	}

	@Test
	void shouldThrowWhenContractsCannotBeDownloaded() {
		when(this.stubDownloader.downloadAndUnpackStubJar(this.stubConfiguration)).thenReturn(null);
		assertThatThrownBy(() -> downloader("").unpackAndDownloadContracts()).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("failed to be downloaded");
	}

	@Test
	void shouldPickPatternFromGavInPathWithoutContractsSubdir(@TempDir Path temp) throws Exception {
		File contractsDir = temp.resolve("com.example").resolve("foo").resolve("1.0.0").toFile();
		assertThat(contractsDir.mkdirs()).isTrue();
		ContractDownloader.InclusionProperties props = downloader("").createNewInclusionProperties(contractsDir);
		assertThat(props.getIncludedRootFolderAntPattern()).isEqualTo("**/");
		assertThat(props.getIncludedContracts()).startsWith("^")
			.endsWith(".*$")
			.contains(escaped(contractsDir.getAbsolutePath()));
	}

	@Test
	void shouldDetectGavWhenGroupIsDotSeparatedInPath(@TempDir Path temp) throws Exception {
		// path uses a fully dot-joined group+artifact ("com.example.foo") and the
		// version,
		// exercising the dot-separator branch of hasGavInPath
		File contractsDir = temp.resolve("com.example.foo").resolve("1.0.0").toFile();
		assertThat(contractsDir.mkdirs()).isTrue();
		ContractDownloader.InclusionProperties props = downloader("").createNewInclusionProperties(contractsDir);
		assertThat(props.getIncludedRootFolderAntPattern()).isEqualTo("**/");
	}

	@Test
	void shouldPickPatternFromContractsSubdirWhenPresent(@TempDir Path temp) throws Exception {
		File contractsDir = temp.resolve("com.example").resolve("foo").resolve("1.0.0").toFile();
		File contractsSubDir = new File(contractsDir, "contracts");
		assertThat(contractsSubDir.mkdirs()).isTrue();
		ContractDownloader.InclusionProperties props = downloader("").createNewInclusionProperties(contractsDir);
		assertThat(props.getIncludedRootFolderAntPattern()).isEqualTo("**/");
		// the pattern now points at the nested contracts folder
		assertThat(props.getIncludedContracts()).contains(escaped(contractsSubDir.getAbsolutePath()));
	}

	@Test
	void shouldUseGroupAndArtifactBranchWhenGroupPresentButVersionAbsent(@TempDir Path temp) {
		// path contains group/artifact but NOT the version -> hasVersionInPath() must be
		// false so the group-and-artifact branch is taken (not the GAV branch)
		File contractsDir = temp.resolve("com.example").resolve("foo").toFile();
		ContractDownloader.InclusionProperties props = downloader("").createNewInclusionProperties(contractsDir);
		assertThat(props.getIncludedRootFolderAntPattern()).contains("example").contains("foo").endsWith("**/");
	}

	@Test
	void shouldPickPatternFromGroupAndArtifactWhenNoGavInPath(@TempDir Path temp) {
		File contractsDir = temp.resolve("plain").toFile();
		ContractDownloader.InclusionProperties props = downloader("").createNewInclusionProperties(contractsDir);
		assertThat(props.getIncludedRootFolderAntPattern()).startsWith("**")
			.contains("com")
			.contains("example")
			.contains("foo")
			.endsWith("**/");
		assertThat(props.getIncludedContracts()).startsWith("^").endsWith(".*$").contains("foo");
	}

	@Test
	@SuppressWarnings("NullAway")
	void shouldTreatNullContractsPathLikeGroupAndArtifact(@TempDir Path temp) {
		// a null contractsPath must take the group/artifact branch (not the contractsPath
		// branch)
		ContractDownloader downloader = new ContractDownloader(this.stubDownloader, this.stubConfiguration, null,
				"com.example", "foo", "1.0.0");
		File contractsDir = temp.resolve("plain").toFile();
		ContractDownloader.InclusionProperties props = downloader.createNewInclusionProperties(contractsDir);
		assertThat(props.getIncludedRootFolderAntPattern()).contains("foo").endsWith("**/");
	}

	private static String escaped(String path) {
		return path.replace("\\", "\\\\");
	}

}
