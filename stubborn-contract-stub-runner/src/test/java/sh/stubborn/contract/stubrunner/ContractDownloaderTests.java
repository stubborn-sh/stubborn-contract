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
import static org.mockito.Mockito.mock;

class ContractDownloaderTests {

	private final StubDownloader stubDownloader = mock(StubDownloader.class);

	private final StubConfiguration stubConfiguration = new StubConfiguration("");

	private final File file = new File(File.separator + String.join(File.separator, "some", "path", "to", "somewhere"));

	@Test
	void shouldSetInclusionPatternWhenPathStartsWithSeparator() {
		String contractPath = File.separator + String.join(File.separator, "a", "b", "c", "d");
		ContractDownloader downloader = new ContractDownloader(this.stubDownloader, this.stubConfiguration,
				contractPath, "", "", "");
		ContractDownloader.InclusionProperties props = downloader.createNewInclusionProperties(this.file);
		assertThat(props.getIncludedContracts()).startsWith("^")
			.endsWith("$")
			.contains(fileSeparated("/some/path/to/somewhere(/)?.*/a/b/c/d/.*"));
		assertThat(props.getIncludedRootFolderAntPattern()).isEqualTo("**/a/b/c/d/**/");
	}

	@Test
	void shouldSetInclusionPatternWhenPathDoesNotStartWithSeparator() {
		String contractPath = String.join(File.separator, "a", "b", "c", "d");
		ContractDownloader downloader = new ContractDownloader(this.stubDownloader, this.stubConfiguration,
				contractPath, "", "", "");
		ContractDownloader.InclusionProperties props = downloader.createNewInclusionProperties(this.file);
		assertThat(props.getIncludedContracts()).startsWith("^")
			.endsWith("$")
			.contains(fileSeparated("/some/path/to/somewhere(/)?.*/a/b/c/d/.*"));
		assertThat(props.getIncludedRootFolderAntPattern()).isEqualTo("**/a/b/c/d/**/");
	}

	private static String fileSeparated(String s) {
		return s.replace("/", File.separator).replace("\\", "\\\\");
	}

}
